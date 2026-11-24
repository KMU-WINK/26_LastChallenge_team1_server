package com.wink.ichibang.auth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentMember {
    private final HttpServletRequest request;

    public long requireId() {
        if (request.getUserPrincipal() == null) {
            throw new AccessException(
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "로그인이 필요합니다."
            );
        }

        try {
            long id = Long.parseLong(request.getUserPrincipal().getName());

            if (id <= 0) {
                throw new NumberFormatException("회원 ID가 올바르지 않습니다.");
            }

            return id;
        } catch (NumberFormatException e) {
            throw new AccessException(
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "로그인 정보를 확인할 수 없습니다."
            );
        }
    }

    public long requireAdminId() {
        long id = requireId();

        if (!request.isUserInRole("ADMIN")) {
            throw new AccessException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "운영진만 사용할 수 있습니다."
            );
        }

        return id;
    }
}