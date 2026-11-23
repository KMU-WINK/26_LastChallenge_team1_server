package com.wink.ichibang.post.repository;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface PostRepository extends JpaRepository<Post, Long> {

    // ===== 공개 목록 (부원에게 보이는 글: 게시됨 + 시작 일시 지남) =====
    // type이 null이면 종류 필터 없이 전체. 정렬은 Pageable로 받는다 (PostSort).
    // 삭제된 글은 Post의 @SQLRestriction으로 자동 제외.

    /** '모집중' 탭: 신청 중인 글 */
    @Query("""
            select p from Post p
            where p.status = com.wink.ichibang.post.domain.PostStatus.OPEN
              and p.opensAt <= :now
              and p.closesAt >= :now
            """)
    Page<Post> findVisibleOpen(@Param("now") LocalDateTime now,
                               Pageable pageable);

    /** '마감' 탭: 수동 마감 + 마감 일시가 지난 글 */
    @Query("""
            select p from Post p
            where p.status <> com.wink.ichibang.post.domain.PostStatus.DRAFT
              and p.opensAt <= :now
              and (p.status = com.wink.ichibang.post.domain.PostStatus.CLOSED or p.closesAt < :now)
            """)
    Page<Post> findVisibleClosed(@Param("now") LocalDateTime now,
                                 Pageable pageable);

    // ===== 운영진 목록 (계산된 상태 기준 필터) =====

    /** status=DRAFT */
    Page<Post> findByStatus(PostStatus status, Pageable pageable);

    /** status=OPEN: 게시됐고 아직 마감 전 (공개 대기 포함) */
    @Query("""
            select p from Post p
            where p.status = com.wink.ichibang.post.domain.PostStatus.OPEN
              and p.closesAt >= :now
            """)
    Page<Post> findOpen(@Param("now") LocalDateTime now, Pageable pageable);

    /** status=CLOSED: 수동 마감 + 마감 일시가 지난 글 */
    @Query("""
            select p from Post p
            where p.status = com.wink.ichibang.post.domain.PostStatus.CLOSED
               or (p.status = com.wink.ichibang.post.domain.PostStatus.OPEN and p.closesAt < :now)
            """)
    Page<Post> findClosed(@Param("now") LocalDateTime now, Pageable pageable);
}
