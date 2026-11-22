package com.wink.ichibang.service;

import com.wink.ichibang.domain.Post;
import com.wink.ichibang.domain.PostStatus;
import com.wink.ichibang.dto.response.PostDetailResponse;
import com.wink.ichibang.dto.response.PostPageResponse;
import com.wink.ichibang.dto.response.PostSummaryResponse;
import com.wink.ichibang.exception.PostErrorCode;
import com.wink.ichibang.exception.PostException;
import com.wink.ichibang.port.FormPort;
import com.wink.ichibang.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 부원·비회원용 조회 + 다른 도메인(BE-2)에 제공하는 메서드 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final FormPort formPort;

    /** P-1 (POST-04) */
    public PostPageResponse<PostSummaryResponse> getPosts(int page, int size) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        return PostPageResponse.from(
                postRepository.findVisible(now, PageRequest.of(page, size))
                        .map(post -> PostSummaryResponse.of(post, now))
        );
    }

    /** P-2 (POST-05): 임시 저장·공개 대기 글은 404로 숨긴다 */
    public PostDetailResponse getPost(Long postId) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        Post post = getVisiblePost(postId, now);
        return PostDetailResponse.of(post, formPort.countItems(postId), now);
    }

    // ===== 다른 도메인에 제공 (명세서 1장 접점 표) =====

    /** BE-2 SUB-02: 지금 제출·취소가 가능한 신청글인가 */
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
