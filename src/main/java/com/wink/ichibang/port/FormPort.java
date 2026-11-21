package com.wink.ichibang.port;

import com.wink.ichibang.domain.PostType;

/**
 * 신청글 도메인이 신청서(form, BE-3)에 요청하는 기능.
 * BE-3의 ItemService가 이 인터페이스를 구현하면 된다.
 * 구현이 생기기 전까지는 stub/StubFormPort가 대신한다.
 */
public interface FormPort {

    /**
     * POST-01 ↔ FORM-01: 신청글 등록 직후 기본 활동 항목을 만든다.
     * STUDY: 웹 기초·프론트엔드·백엔드 스터디 3개 (APPLY)
     * EVENT: 신청글 제목을 이름으로 한 PARTICIPATE 1개 (제안)
     *
     * @return 만든 항목 수
     */
    int createDefaultItems(Long postId, PostType type, String postTitle);

    /** 신청글의 활동 항목 수 (게시 가능 여부, 상세의 itemCount) */
    int countItems(Long postId);
}
