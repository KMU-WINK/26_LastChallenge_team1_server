package com.wink.ichibang.post.service;

import com.wink.ichibang.post.domain.DisplayStatus;
import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.Question;
import com.wink.ichibang.post.dto.request.AdminPostFilter;
import com.wink.ichibang.post.dto.request.PostCreateRequest;
import com.wink.ichibang.post.dto.request.PostUpdateRequest;
import com.wink.ichibang.post.dto.request.QuestionRequest;
import com.wink.ichibang.post.dto.response.AdminPostDetailResponse;
import com.wink.ichibang.post.dto.response.AdminPostSummaryResponse;
import com.wink.ichibang.post.dto.response.PostCloseResponse;
import com.wink.ichibang.post.dto.response.PostCreateResponse;
import com.wink.ichibang.post.dto.response.PostDeleteResponse;
import com.wink.ichibang.post.dto.response.PostPageResponse;
import com.wink.ichibang.post.exception.PostErrorCode;
import com.wink.ichibang.post.exception.PostException;
import com.wink.ichibang.post.port.SubmissionPort;
import com.wink.ichibang.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 운영진용: 목록·상세, 작성(POST-01·FORM-01), 수정(POST-02), 삭제(POST-03), 마감(POST-06) */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminPostService {

    private final PostRepository postRepository;
    private final SubmissionPort submissionPort;

    /** POST-04 운영진 목록 (전체 / 모집중 / 마감 탭) */
    @Transactional(readOnly = true)
    public PostPageResponse<AdminPostSummaryResponse> getPosts(AdminPostFilter filter, int page, int size) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        Page<Post> posts = switch (filter) {
            case ALL -> postRepository.findAll(pageable);
            case OPEN -> postRepository.findOpen(now, pageable);
            case CLOSED -> postRepository.findClosed(now, pageable);
        };

        // 한 페이지 최대 50건이라 글마다 제출자 수를 세도 부담이 작다
        return PostPageResponse.from(posts.map(post ->
                AdminPostSummaryResponse.of(post, submissionPort.countSubmissions(post.getId()), now)));
    }

    /** 운영진 상세 (수정 화면용, 질문 포함) */
    @Transactional(readOnly = true)
    public AdminPostDetailResponse getPost(Long postId) {
        return toDetail(findPost(postId), LocalDateTime.now(Post.KST));
    }

    /** POST-01 작성·게시 + FORM-01 질문. 하나라도 실패하면 아무것도 저장되지 않는다 */
    public PostCreateResponse create(PostCreateRequest request, Long memberId) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        List<Question> questions = toQuestions(request.questions());

        Post post = Post.create(
                request.title(),
                request.summary(),
                request.description(),
                request.opensOn(),
                request.closesOn(),
                memberId,
                now
        );
        post.addQuestions(questions);
        postRepository.save(post);   // 질문·선택지도 cascade로 함께 저장

        return PostCreateResponse.of(post, now);
    }

    /** POST-02 수정. 보낸 항목만 바꾸고, questions를 보내면 질문 전체를 교체한다 */
    public AdminPostDetailResponse update(Long postId, PostUpdateRequest request) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        Post post = findPost(postId);
        post.requireEditable(now);   // 마감된 글은 409 POST_CLOSED

        if (request.title() != null) {
            requireNotBlank(request.title());
            post.changeTitle(request.title());
        }
        if (request.summary() != null) {
            post.changeSummary(request.summary());
        }
        if (request.description() != null) {
            requireNotBlank(request.description());
            post.changeDescription(request.description());
        }
        if (request.opensOn() != null || request.closesOn() != null) {
            post.changePeriod(request.opensOn(), request.closesOn(), now);
        }
        if (request.questions() != null) {
            replaceQuestions(post, request.questions());
        }

        post.touch();
        postRepository.flush();
        return toDetail(post, now);
    }

    /** POST-03 삭제. 제출자가 있으면 삭제하지 않고 마감한다 */
    public PostDeleteResponse delete(Long postId) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        Post post = findPost(postId);
        long submissionCount = submissionPort.countSubmissions(postId);

        if (submissionCount == 0) {
            post.softDelete(now);
            return PostDeleteResponse.deleted(postId);
        }
        if (post.displayStatus(now) == DisplayStatus.CLOSED) {
            throw new PostException(PostErrorCode.POST_HAS_SUBMISSIONS,
                    Map.of("submissionCount", submissionCount));
        }
        post.close(now);
        return PostDeleteResponse.closed(postId, submissionCount);
    }

    /** POST-06 수동 마감 */
    public PostCloseResponse close(Long postId) {
        Post post = findPost(postId);
        post.close(LocalDateTime.now(Post.KST));
        return PostCloseResponse.of(postId);
    }

    // ===== 내부 =====

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.POST_NOT_FOUND));
    }

    /** FORM-01: 질문이 하나도 없으면 FORM_EMPTY, 선택지 규칙은 Question.create()에서 검사 */
    private List<Question> toQuestions(List<QuestionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new PostException(PostErrorCode.FORM_EMPTY);
        }
        return requests.stream()
                .map(q -> Question.create(q.title(), q.type(), q.options()))
                .toList();
    }

    /** 제출자가 있으면 FORM_LOCKED. 새 질문을 먼저 검증한 뒤에 기존 질문을 지운다 */
    private void replaceQuestions(Post post, List<QuestionRequest> requests) {
        if (submissionPort.countSubmissions(post.getId()) > 0) {
            throw new PostException(PostErrorCode.FORM_LOCKED);
        }
        List<Question> newQuestions = toQuestions(requests);

        post.clearQuestions();
        // 기존 질문 DELETE를 먼저 실행해야 (post_id, order_no) 유니크 키에 걸리지 않는다
        postRepository.flush();
        post.addQuestions(newQuestions);
    }

    private void requireNotBlank(String value) {
        if (value.isBlank()) {
            throw new PostException(PostErrorCode.INVALID_INPUT);
        }
    }

    private AdminPostDetailResponse toDetail(Post post, LocalDateTime now) {
        return AdminPostDetailResponse.of(post, submissionPort.countSubmissions(post.getId()), now);
    }
}
