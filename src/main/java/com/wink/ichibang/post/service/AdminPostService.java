package com.wink.ichibang.post.service;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.domain.PostType;
import com.wink.ichibang.post.domain.QuestionDraft;
import com.wink.ichibang.post.dto.request.PostCreateRequest;
import com.wink.ichibang.post.dto.request.PostUpdateRequest;
import com.wink.ichibang.post.dto.response.AdminPostDetailResponse;
import com.wink.ichibang.post.dto.response.AdminPostListItemResponse;
import com.wink.ichibang.global.response.PageResponse;
import com.wink.ichibang.post.dto.response.PostIdStatusResponse;
import com.wink.ichibang.global.exception.ErrorCode;
import com.wink.ichibang.global.exception.BusinessException;
import com.wink.ichibang.post.port.SubmissionPort;
import com.wink.ichibang.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.wink.ichibang.post.service.PostService.latestFirst;
import static com.wink.ichibang.post.service.PostService.today;
import static com.wink.ichibang.post.service.QuestionDraftMapper.blankToNull;

/** 운영진용 신청글 관리 (POST-01, 02, 03, 06, 07 + FORM-01) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPostService {

    private final PostRepository postRepository;
    private final SubmissionPort submissionPort;

    /** POST-01 작성. 질문까지 한 트랜잭션으로 저장 */
    @Transactional
    public PostIdStatusResponse create(PostCreateRequest request, Long adminId) {
        List<QuestionDraft> drafts = QuestionDraftMapper.fromCreate(request.questions());
        Post post = Post.create(
                request.type(),
                request.title(),
                blankToNull(request.summary()),
                request.content(),
                request.startDate(),
                request.endDate(),
                adminId,
                drafts);
        postRepository.save(post);
        return PostIdStatusResponse.of(post.getId(), post.statusOn(today()));
    }

    /**
     * POST-07 목록. 시작 전 글도 보이고 모집 중 탭에 들어간다.
     * @param status null이면 전체
     */
    public PageResponse<AdminPostListItemResponse> getPosts(PostStatus status, PostType type, String keyword,
                                                            int page, int size) {
        LocalDate today = today();
        String trimmed = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<Post> posts = postRepository.findForAdmin(
                today, type, trimmed,
                status == PostStatus.OPEN,
                status == PostStatus.CLOSED,
                PostStatus.OPEN,
                latestFirst(page, size));

        Map<Long, Long> counts = submissionPort.countByPostIds(
                posts.getContent().stream().map(Post::getId).toList());
        return PageResponse.of(posts, (post, no) ->
                AdminPostListItemResponse.of(post, no, today, counts.getOrDefault(post.getId(), 0L)));
    }

    /** POST-02 수정 화면용 조회. 마감 글도 조회는 된다 */
    public AdminPostDetailResponse getPost(Long postId) {
        Post post = findPost(postId);
        return AdminPostDetailResponse.of(post, today(), submissionPort.countByPostId(postId));
    }

    /**
     * POST-02 수정.
     * - 마감 글 → 409 POST_CLOSED
     * - 구분 변경 → 400 VALIDATION_FAILED (미지원)
     * - 제출이 있는데 질문·선택지(순서 포함)가 바뀜 → 409 POST_HAS_SUBMISSIONS. 기본 정보만 바뀌면 허용
     */
    @Transactional
    public PostIdStatusResponse update(Long postId, PostUpdateRequest request) {
        LocalDate today = today();
        Post post = findPost(postId);
        post.requireOpen(today);

        if (request.type() != post.getType()) {
            throw BusinessException.field(ErrorCode.VALIDATION_FAILED, "type", "구분은 바꿀 수 없어요");
        }

        List<QuestionDraft> drafts = QuestionDraftMapper.fromUpdate(request.questions());
        if (post.isFormChanged(drafts) && submissionPort.countByPostId(postId) > 0) {
            throw new BusinessException(ErrorCode.POST_HAS_SUBMISSIONS);
        }

        post.updateBasicInfo(
                request.title(),
                blankToNull(request.summary()),
                request.content(),
                request.startDate(),
                request.endDate());
        post.mergeQuestions(drafts);
        return PostIdStatusResponse.of(post.getId(), post.statusOn(today));
    }

    /** POST-03 삭제. 제출 0건일 때만 소프트 삭제, 아니면 409 */
    @Transactional
    public void delete(Long postId) {
        Post post = findPost(postId);
        if (submissionPort.countByPostId(postId) > 0) {
            throw new BusinessException(ErrorCode.POST_HAS_SUBMISSIONS);
        }
        post.softDelete();
    }

    /** POST-06 수동 마감. 이미 마감(수동·자동)이면 409 POST_CLOSED */
    @Transactional
    public PostIdStatusResponse close(Long postId) {
        LocalDate today = today();
        Post post = findPost(postId);
        post.close(today);
        return PostIdStatusResponse.of(post.getId(), PostStatus.CLOSED);
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }
}
