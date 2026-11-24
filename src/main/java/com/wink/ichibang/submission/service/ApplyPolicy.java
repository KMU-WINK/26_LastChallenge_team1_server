package com.wink.ichibang.submission.service;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.repository.PostRepository;
import com.wink.ichibang.submission.exception.SubmissionErrorCode;
import com.wink.ichibang.submission.exception.SubmissionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * SUB-02 제출 검증: 모집 중이고 신청 기간 안일 때만 신청서 조회·제출·수정·취소를 허용한다.
 * 별도 API 없이 각 기능이 호출한다. 신청서 조회(FORM-02)도 이걸 쓴다.
 * 서버 시간(KST) 기준이며, 마감 계산은 Post#statusOn (POST-06)을 따른다.
 * 호출하는 쪽에 트랜잭션이 있어야 반환된 Post의 질문을 읽을 수 있다.
 */
@Component
@RequiredArgsConstructor
public class ApplyPolicy {

    private final PostRepository postRepository;

    /**
     * @return 신청할 수 있는 글
     * @throws SubmissionException 없거나 삭제되거나 시작 전인 글은 POST_NOT_FOUND(404),
     *                             마감된 글(수동·종료일 경과)은 APPLY_POST_NOT_OPEN(400)
     */
    public Post requireApplicable(Long postId) {
        LocalDate today = LocalDate.now(Post.KST);
        Post post = postRepository.findById(postId)
                .filter(p -> p.isStartedOn(today))
                .orElseThrow(() -> new SubmissionException(SubmissionErrorCode.POST_NOT_FOUND));
        if (post.statusOn(today) != PostStatus.OPEN) {
            throw new SubmissionException(SubmissionErrorCode.APPLY_POST_NOT_OPEN);
        }
        return post;
    }
}
