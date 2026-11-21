package com.wink.ichibang.repository;

import com.wink.ichibang.domain.Post;
import com.wink.ichibang.domain.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * P-1 공개 목록: 게시됐고 시작 일시가 지난 글.
     * 신청 중인 글 먼저(마감 임박 순), 그 뒤 마감된 글(최근 마감 순).
     * 삭제된 글은 Post의 @SQLRestriction으로 자동 제외.
     */
    @Query(value = """
            select p from Post p
            where p.status <> com.wink.ichibang.domain.PostStatus.DRAFT
              and p.opensAt <= :now
            order by
              case when p.status = com.wink.ichibang.domain.PostStatus.OPEN
                        and p.closesAt >= :now then 0 else 1 end,
              case when p.status = com.wink.ichibang.domain.PostStatus.OPEN
                        and p.closesAt >= :now then p.closesAt end asc,
              p.closesAt desc
            """,
            countQuery = """
            select count(p) from Post p
            where p.status <> com.wink.ichibang.domain.PostStatus.DRAFT
              and p.opensAt <= :now
            """)
    Page<Post> findVisible(@Param("now") LocalDateTime now, Pageable pageable);

    // ===== P-3 운영진 목록 (계산된 상태 기준 필터) =====

    /** status=DRAFT */
    Page<Post> findByStatus(PostStatus status, Pageable pageable);

    /** status=OPEN: 게시됐고 아직 마감 전 (공개 대기 포함) */
    @Query("""
            select p from Post p
            where p.status = com.wink.ichibang.domain.PostStatus.OPEN
              and p.closesAt >= :now
            """)
    Page<Post> findOpen(@Param("now") LocalDateTime now, Pageable pageable);

    /** status=CLOSED: 수동 마감 + 마감 일시가 지난 글 */
    @Query("""
            select p from Post p
            where p.status = com.wink.ichibang.domain.PostStatus.CLOSED
               or (p.status = com.wink.ichibang.domain.PostStatus.OPEN and p.closesAt < :now)
            """)
    Page<Post> findClosed(@Param("now") LocalDateTime now, Pageable pageable);
}
