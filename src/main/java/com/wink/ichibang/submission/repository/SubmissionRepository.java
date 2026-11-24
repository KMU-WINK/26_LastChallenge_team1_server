package com.wink.ichibang.submission.repository;

import com.wink.ichibang.submission.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    boolean existsByPostIdAndMemberId(Long postId, Long memberId);

    Optional<Submission> findByPostIdAndMemberId(Long postId, Long memberId);

    /** 취소한 제출은 행이 없으므로 이 숫자 = 제출됨 + 참여 완료 */
    long countByPostId(Long postId);

    /** 제출이 없는 글은 결과에 나오지 않는다 */
    @Query("""
            select new com.wink.ichibang.submission.repository.SubmissionRepository$PostCount(s.postId, count(s))
            from Submission s
            where s.postId in :postIds
            group by s.postId
            """)
    List<PostCount> countGroupedByPostId(@Param("postIds") Collection<Long> postIds);

    record PostCount(Long postId, long count) {
    }
}
