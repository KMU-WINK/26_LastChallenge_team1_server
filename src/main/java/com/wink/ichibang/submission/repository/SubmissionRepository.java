package com.wink.ichibang.submission.repository;

import com.wink.ichibang.submission.domain.Submission;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    boolean existsByPostIdAndMemberId(Long postId, Long memberId);

    Optional<Submission> findByPostIdAndMemberId(Long postId, Long memberId);

    /**
     * 수정·취소용. 제출 행을 SELECT ... FOR UPDATE로 잠그고 읽는다.
     * 같은 부원의 수정·취소가 겹치거나 운영진의 참여 완료 처리와 겹쳐도 한 줄로 처리되고,
     * 나중 요청은 앞 요청이 끝난 뒤의 최신 상태를 읽는다. 호출하는 쪽에 트랜잭션이 있어야 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Submission> findForUpdateByPostIdAndMemberId(Long postId, Long memberId);

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
