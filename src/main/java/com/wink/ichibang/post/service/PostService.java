package com.wink.ichibang.post.service;

import com.wink.ichibang.post.domain.DisplayStatus;
import com.wink.ichibang.post.domain.Post;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 부원·비회원용 조회 (POST-04, POST-05) + 제출 담당에게 제공하는 메서드 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    /** POST-04 부원 목록. openOnly=true면 모집 중인 글만 */
    public PostPageResponse<PostSummaryResponse> getPosts(boolean openOnly, int page, int size) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        Page<Post> posts = openOnly
                ? postRepository.findOpen(now, pageable)
                : postRepository.findVisible(now, pageable);

        return PostPageResponse.from(posts.map(post -> PostSummaryResponse.of(post, now)));
    }

    /** POST-05 부원 상세. 모집 전·삭제된 글은 404 */
    public PostDetailResponse getPost(Long postId) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        return PostDetailResponse.of(getVisiblePost(postId, now), now);
    }

    // ===== 제출 담당에게 제공 =====

    /** 지금 신청할 수 있는 신청글인가 */
    public boolean isSubmittable(Long postId) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        return postRepository.findById(postId)
                .map(post -> post.isSubmittable(now))
                .orElse(false);
    }

    /** 부원에게 보이는 신청글의 현재 상태. 안 보이는 글이면 POST_NOT_FOUND */
    public DisplayStatus getDisplayStatus(Long postId) {
        LocalDateTime now = LocalDateTime.now(Post.KST);
        return getVisiblePost(postId, now).displayStatus(now);
    }

    private Post getVisiblePost(Long postId, LocalDateTime now) {
        return postRepository.findById(postId)
                .filter(post -> post.isVisible(now))
                .orElseThrow(() -> new PostException(PostErrorCode.POST_NOT_FOUND));
    }
}
