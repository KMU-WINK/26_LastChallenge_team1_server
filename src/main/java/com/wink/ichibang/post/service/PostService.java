package com.wink.ichibang.post.service;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.domain.PostType;
import com.wink.ichibang.post.dto.response.PageResponse;
import com.wink.ichibang.post.dto.response.PostDetailResponse;
import com.wink.ichibang.post.dto.response.PostListItemResponse;
import com.wink.ichibang.post.exception.PostErrorCode;
import com.wink.ichibang.post.exception.PostException;
import com.wink.ichibang.post.port.SubmissionPort;
import com.wink.ichibang.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** 부원·비로그인용 신청글 조회 (POST-04, POST-05) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final SubmissionPort submissionPort;

    /** POST-04. 시작 전·삭제 글 제외, 마감 글 포함, 최신순 */
    public PageResponse<PostListItemResponse> getPosts(boolean recruiting, PostType type, int page, int size) {
        LocalDate today = today();
        Page<Post> posts = postRepository.findForMember(
                today, type, recruiting, PostStatus.OPEN, latestFirst(page, size));
        return PageResponse.of(posts, (post, no) -> PostListItemResponse.of(post, no, today));
    }

    /**
     * POST-05. 시작 전·삭제 글은 404.
     * @param memberId 로그인한 부원 ID, 비로그인이면 null → mySubmission null
     */
    public PostDetailResponse getPost(Long postId, Long memberId) {
        LocalDate today = today();
        Post post = postRepository.findById(postId)
                .filter(p -> p.isStartedOn(today))
                .orElseThrow(() -> new PostException(PostErrorCode.POST_NOT_FOUND));

        SubmissionPort.MySubmission mySubmission = memberId == null ? null
                : submissionPort.findMySubmission(postId, memberId).orElse(null);
        return PostDetailResponse.of(post, today, mySubmission);
    }

    static PageRequest latestFirst(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    }

    static LocalDate today() {
        return LocalDate.now(Post.KST);
    }
}
