package com.wink.ichibang.post.service;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.dto.request.PostCreateRequest;
import com.wink.ichibang.post.dto.request.PostUpdateRequest;
import com.wink.ichibang.post.dto.response.AdminPostDetailResponse;
import com.wink.ichibang.post.dto.response.AdminPostSummaryResponse;
import com.wink.ichibang.post.dto.response.PostIdResponse;
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
import java.util.Map;

/** 운영진용 조회·등록·수정·삭제·게시·마감 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminPostService {

    private final PostRepository postRepository;
    private final SubmissionPort submissionPort;

    /** 운영진 목록 (임시 저장 포함) */
    @Transactional(readOnly = true)
    public PostPageResponse<AdminPostSummaryResponse> getPosts(PostStatus status, int page, int size) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

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

        // 한 페이지 최대 50건이라 글마다 신청자 수를 세도 부담이 작다
        return PostPageResponse.from(posts.map(post ->
                AdminPostSummaryResponse.of(post, submissionPort.countActiveSubmissions(post.getId()), now)));
    }

    /** 운영진 상세 */
    @Transactional(readOnly = true)
    public AdminPostDetailResponse getPost(Long postId) {
        return toDetail(findPost(postId), LocalDateTime.now(Post.KST));
    }

    /** 등록: 항상 임시 저장 */
    public PostIdResponse create(PostCreateRequest request, Long memberId) {
        Post post = Post.createDraft(
                request.title(),
                request.summary(),
                request.opensAt(),
                request.closesAt(),
                memberId
        );
        postRepository.save(post);
        return PostIdResponse.created(post);
    }

    /** 게시: 임시 저장인가 → 마감 전인가 (Post.publish 안에서 검사) */
    public PostIdResponse publish(Long postId) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        Post post = findPost(postId);
        post.publish(now);
        return PostIdResponse.published(post, now);
    }

    /** 수동 마감 */
    public PostIdResponse close(Long postId) {
        Post post = findPost(postId);
        post.close(LocalDateTime.now(Post.KST));
        return PostIdResponse.closed(post);
    }

    /** 수정: 보낸 필드만. 하나라도 막히면 예외 → 트랜잭션 전체 롤백 */
    public AdminPostDetailResponse update(Long postId, PostUpdateRequest request) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        Post post = findPost(postId);

        if (request.title() != null) {
            requireNotBlank(request.title());
            post.changeTitle(request.title());
        }
        if (request.summary() != null) {
            requireNotBlank(request.summary());
            post.changeSummary(request.summary());
        }
        if (request.opensAt() != null || request.closesAt() != null) {
            post.changePeriod(request.opensAt(), request.closesAt(), now);
        }

        postRepository.flush(); // UPDATE를 먼저 실행해 updatedAt을 응답에 반영
        return toDetail(post, now);
    }

    /** 삭제: 신청자가 있으면 confirm=true로 다시 호출해야 삭제 */
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

    private void requireNotBlank(String value) {
        if (value.isBlank()) {
            throw new PostException(PostErrorCode.INVALID_INPUT);
        }
    }

    private AdminPostDetailResponse toDetail(Post post, LocalDateTime now) {
        return AdminPostDetailResponse.of(post, submissionPort.countActiveSubmissions(post.getId()), now);
    }
}
