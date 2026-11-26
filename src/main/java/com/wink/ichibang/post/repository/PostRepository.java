package com.wink.ichibang.post.repository;

import com.wink.ichibang.post.domain.Post;
import com.wink.ichibang.post.domain.PostStatus;
import com.wink.ichibang.post.domain.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

/**
 * 삭제된 글은 Post의 @SQLRestriction으로 모든 조회에서 빠진다.
 * "모집 중" = 수동 마감 아님(status = OPEN) AND 종료일이 오늘 이후(end_date >= today).
 * 정렬은 호출하는 쪽 Pageable에서 정한다 (최신순 = id 내림차순).
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /** POST-04 부원 목록: 시작일이 된 글만. recruiting=true면 모집 중만 */
    @Query("""
            select p from Post p
              and (:type is null or p.type = :type)
              and (:recruiting = false or (p.status = :open and p.endDate >= :today))
            """)
    Page<Post> findForMember(@Param("today") LocalDate today,
                             @Param("type") PostType type,
                             @Param("recruiting") boolean recruiting,
                             @Param("open") PostStatus open,
                             Pageable pageable);

    /**
     * POST-07 운영진 목록: 시작 전 글 포함.
     * openOnly: 모집 중 탭 (시작 전 글도 여기 들어간다)
     * closedOnly: 마감 탭 (수동 마감 또는 종료일 지남)
     * keyword: 제목 부분 일치, null이면 조건 없음
     */
    @Query("""
            select p from Post p
            where (:type is null or p.type = :type)
              and (:keyword is null or p.title like concat('%', :keyword, '%'))
              and (:openOnly = false or (p.status = :open and p.endDate >= :today))
              and (:closedOnly = false or (p.status <> :open or p.endDate < :today))
            """)
    Page<Post> findForAdmin(@Param("today") LocalDate today,
                            @Param("type") PostType type,
                            @Param("keyword") String keyword,
                            @Param("openOnly") boolean openOnly,
                            @Param("closedOnly") boolean closedOnly,
                            @Param("open") PostStatus open,
                            Pageable pageable);
}
