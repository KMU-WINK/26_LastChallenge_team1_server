package com.wink.ichibang.service;

import com.wink.ichibang.domain.Post;
import com.wink.ichibang.domain.PostStatus;
import com.wink.ichibang.dto.request.PostCreateRequest;
import com.wink.ichibang.dto.request.PostUpdateRequest;
import com.wink.ichibang.dto.response.AdminPostDetailResponse;
import com.wink.ichibang.dto.response.AdminPostSummaryResponse;
import com.wink.ichibang.dto.response.PostIdResponse;
import com.wink.ichibang.dto.response.PostPageResponse;
import com.wink.ichibang.exception.PostErrorCode;
import com.wink.ichibang.exception.PostException;
import com.wink.ichibang.port.FormPort;
import com.wink.ichibang.port.SubmissionPort;
import com.wink.ichibang.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/** 운영진용 조회·등록·수정·삭제·게시·마감 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminPostService {

    private final PostRepository postRepository;
    private final FormPort formPort;
    private final SubmissionPort submissionPort;

    /** P-3 */
    @Transactional(readOnly = true)
    public PostPageResponse<AdminPostSummaryResponse> getPosts(PostStatus status, int page, int size) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Post> posts;
        if (status == null) {
            posts = postRepository.findAll(pageable);
        } else {
            posts = switch (status) {
                case DRAFT -> postRepository.findByStatus(PostStatus.DRAFT, pageable);
                case OPEN -> postRepository.findOpen(now, pageable);
                case CLOSED -> postRepository.findClosed(now, pageable);
            };
        }

        // 한 페이지 최대 50건이라 글마다 제출 수를 세도 부담이 작다
        return PostPageResponse.from(posts.map(post ->
                AdminPostSummaryResponse.of(post, submissionPort.countActiveSubmissions(post.getId()), now)));
    }

    /** P-4 */
    @Transactional(readOnly = true)
    public AdminPostDetailResponse getPost(Long postId) {
        return toDetail(findPost(postId), LocalDateTime.now(Post.KST));
    }

    /** P-5 (POST-01): 임시 저장 + 기본 활동 항목 */
    public PostIdResponse create(PostCreateRequest request, Long memberId) {
        Post post = Post.createDraft(
                request.term(),
                request.type(),
                request.title(),
                request.description(),
                request.opensAt(),
                request.closesAt(),
                memberId
        );
        postRepository.save(post);

        int itemCount = formPort.createDefaultItems(post.getId(), post.getType(), post.getTitle());
        return PostIdResponse.created(post, itemCount);
    }

    /** P-6 (POST-06): 검사 순서 = 임시 저장인가 → 항목이 있는가 → 마감 전인가 */
    public PostIdResponse publish(Long postId) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        Post post = findPost(postId);

        if (post.getStatus() != PostStatus.DRAFT) {
            throw new PostException(PostErrorCode.POST_NOT_DRAFT);
        }
        if (formPort.countItems(postId) == 0) {
            throw new PostException(PostErrorCode.POST_FORM_EMPTY);
        }
        post.publish(now);
        return PostIdResponse.published(post, now);
    }

    /** P-7 (POST-06 제안) */
    public PostIdResponse close(Long postId) {
        Post post = findPost(postId);
        post.close(LocalDateTime.now(Post.KST));
        return PostIdResponse.closed(post);
    }

    /** P-8 (POST-02): 보낸 필드만 수정. 하나라도 막히면 예외 → 트랜잭션 전체 롤백 */
    public AdminPostDetailResponse update(Long postId, PostUpdateRequest request) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        Post post = findPost(postId);

        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new PostException(PostErrorCode.INVALID_INPUT);
            }
            post.changeTitle(request.title());
        }
        if (request.description() != null) {
            post.changeDescription(request.description());
        }
        if (request.term() != null) {
            post.changeTerm(request.term());
        }
        if (request.type() != null) {
            post.changeType(request.type());
        }
        if (request.opensAt() != null || request.closesAt() != null) {
            post.changePeriod(request.opensAt(), request.closesAt(), now);
        }

        postRepository.flush(); // UPDATE를 먼저 실행해 updatedAt을 응답에 반영
        return toDetail(post, now);
    }

    /** P-9 (POST-03): 제출이 있으면 confirm=true로 다시 호출해야 삭제 */
    public void delete(Long postId, boolean confirm) {
        Post post = findPost(postId);
        long submissionCount = submissionPort.countActiveSubmissions(postId);

        if (submissionCount > 0 && !confirm) {
            throw new PostException(PostErrorCode.POST_HAS_SUBMISSIONS,
                    Map.of("submissionCount", submissionCount));
        }
        post.softDelete(LocalDateTime.now(Post.KST));
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.POST_NOT_FOUND));
    }

    private AdminPostDetailResponse toDetail(Post post, LocalDateTime now) {
        return AdminPostDetailResponse.of(
                post,
                formPort.countItems(post.getId()),
                submissionPort.countActiveSubmissions(post.getId()),
                now
        );
    }
}
