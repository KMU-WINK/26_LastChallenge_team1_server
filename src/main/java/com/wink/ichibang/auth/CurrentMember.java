package com.wink.ichibang.auth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 로그인한 부원 ID 꺼내기.
 * TODO(인증): 로그인 기능이 생기기 전까지 헤더로 흉내 낸다.
 *   X-Member-Id: 부원 ID (없으면 401)
 *   X-Member-Role: ADMIN 이면 운영진 (아니면 403)
 * 로그인 연동 후에는 getUserPrincipal() / isUserInRole("ADMIN") 방식으로 되돌린다.
 */
@Component
@RequiredArgsConstructor
public class CurrentMember {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";
    private static final String ROLE_HEADER = "X-Member-Role";

    private final HttpServletRequest request;

    public long requireId() {
        String raw = request.getHeader(MEMBER_ID_HEADER);
        if (raw == null || raw.isBlank()) {
            throw new AccessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        try {
            long id = Long.parseLong(raw.trim());
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException e) {
            throw new AccessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인 정보를 확인할 수 없습니다.");
        }
    }

    public long requireAdminId() {
        long id = requireId();
        if (!"ADMIN".equalsIgnoreCase(request.getHeader(ROLE_HEADER))) {
            throw new AccessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "운영진만 사용할 수 있습니다.");
        }
        return id;
    }
}