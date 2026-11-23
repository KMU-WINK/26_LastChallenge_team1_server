package com.wink.ichibang.post.service;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.dto.request.PostSort;
import com.wink.ichibang.post.dto.response.PostDetailResponse;
import com.wink.ichibang.post.dto.response.PostPageResponse;
import com.wink.ichibang.post.dto.response.PostSummaryResponse;
import com.wink.ichibang.post.exception.PostErrorCode;
import com.wink.ichibang.post.exception.PostException;
import com.wink.ichibang.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 부원·비회원용 조회 + 다른 도메인(BE-2)에 제공하는 메서드 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    /**
     * 공개 목록: 모집중/마감 탭 + 종류 필터 + 정렬.
     * status는 OPEN(모집중) 또는 CLOSED(마감)만 받는다.
     */
    public PostPageResponse<PostSummaryResponse> getPosts(PostStatus status,
                                                          PostSort sort, int page, int size) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        Pageable pageable = PageRequest.of(page, size, sort.toSort());

        Page<Post> posts = switch (status) {
            case OPEN -> postRepository.findVisibleOpen(now, pageable);
            case CLOSED -> postRepository.findVisibleClosed(now, pageable);
            case DRAFT -> throw new PostException(PostErrorCode.INVALID_INPUT); // 부원은 임시 저장 글을 볼 수 없음
        };

        return PostPageResponse.from(posts.map(post -> PostSummaryResponse.of(post, now)));
    }

    /** 공개 상세: 임시 저장·공개 대기 글은 404로 숨긴다 */
    public PostDetailResponse getPost(Long postId) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        return PostDetailResponse.of(getVisiblePost(postId, now), now);
    }

    // ===== 다른 도메인에 제공 =====

    /** BE-2: 지금 신청·취소가 가능한 신청글인가 */
    public boolean isSubmittable(Long postId) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        return postRepository.findById(postId)
                .map(post -> post.isSubmittable(now))
                .orElse(false);
    }

    /** BE-2: 부원에게 보이는 신청글의 계산된 상태. 안 보이는 글이면 POST_NOT_FOUND */
    public PostStatus getEffectiveStatus(Long postId) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        return getVisiblePost(postId, now).effectiveStatus(now);
    }

    private Post getVisiblePost(Long postId, LocalDateTime now) {
        return postRepository.findById(postId)
                .filter(post -> post.isVisible(now))
                .orElseThrow(() -> new PostException(PostErrorCode.POST_NOT_FOUND));
    }
}
