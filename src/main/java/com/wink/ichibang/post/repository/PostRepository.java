package com.wink.ichibang.post.repository;

import com.wink.ichibang.post.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * 삭제된 글은 Post의 @SQLRestriction으로 모든 조회에서 자동 제외.
 * 정렬은 Pageable로 받는다 (최신순 = id 내림차순).
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /** 부원 목록: 시작일이 지난 글 전부 (모집 중 + 마감) */
    @Query("select p from Post p where p.opensAt <= :now")
    Page<Post> findVisible(@Param("now") LocalDateTime now, Pageable pageable);

    /** 모집 중: 게시 상태이고 시작일 ~ 마감일 사이 (부원 '모집 중만 보기', 운영진 '모집중' 탭) */
    @Query("""
            select p from Post p
            where p.status = com.wink.ichibang.post.domain.PostStatus.OPEN
              and p.opensAt <= :now
              and p.closesAt >= :now
            """)
    Page<Post> findOpen(@Param("now") LocalDateTime now, Pageable pageable);

    /** 마감: 수동 마감 또는 마감일이 지난 글 (운영진 '마감' 탭) */
    @Query("""
            select p from Post p
            where p.status = com.wink.ichibang.post.domain.PostStatus.CLOSED
               or p.closesAt < :now
            """)
    Page<Post> findClosed(@Param("now") LocalDateTime now, Pageable pageable);
}
