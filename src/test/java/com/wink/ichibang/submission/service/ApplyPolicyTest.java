package com.wink.ichibang.submission.service;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.repository.PostRepository;
import com.wink.ichibang.submission.exception.SubmissionErrorCode;
import com.wink.ichibang.submission.exception.SubmissionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** SUB-02 제출 검증 */
@ExtendWith(MockitoExtension.class)
class ApplyPolicyTest {

    private static final LocalDate TODAY = LocalDate.now(Post.KST);

    @Mock
    PostRepository postRepository;
    @InjectMocks
    ApplyPolicy applyPolicy;

    private void assertCode(Post post, SubmissionErrorCode expected) {
        when(postRepository.findById(100L)).thenReturn(Optional.ofNullable(post));
        assertThatThrownBy(() -> applyPolicy.requireApplicable(100L))
                .isInstanceOfSatisfying(SubmissionException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    @Test
    void 모집_중이면_글을_돌려준다() {
        Post post = SubmissionTestFixtures.openPost();
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));

        assertThat(applyPolicy.requireApplicable(100L)).isSameAs(post);
    }

    @Test
    void 종료일_당일은_신청할_수_있다() {
        Post post = SubmissionTestFixtures.post(TODAY.minusDays(3), TODAY);
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));

        assertThat(applyPolicy.requireApplicable(100L)).isSameAs(post);
    }

    @Test
    void 종료일이_지나면_APPLY_POST_NOT_OPEN() {
        assertCode(SubmissionTestFixtures.post(TODAY.minusDays(3), TODAY.minusDays(1)),
                SubmissionErrorCode.APPLY_POST_NOT_OPEN);
    }

    @Test
    void 수동_마감이면_APPLY_POST_NOT_OPEN() {
        Post post = SubmissionTestFixtures.openPost();
        post.close(TODAY);
        assertCode(post, SubmissionErrorCode.APPLY_POST_NOT_OPEN);
    }

    @Test
    void 시작일_전이면_존재를_숨겨_POST_NOT_FOUND() {
        assertCode(SubmissionTestFixtures.post(TODAY.plusDays(1), TODAY.plusDays(5)),
                SubmissionErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 없는_글이면_POST_NOT_FOUND() {
        assertCode(null, SubmissionErrorCode.POST_NOT_FOUND);
    }
}
