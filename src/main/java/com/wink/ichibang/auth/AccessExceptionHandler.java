package com.wink.ichibang.auth;

import com.wink.ichibang.post.exception.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 401 / 403 응답. 오류 본문은 post·submission과 같은 { errorCode, message, fieldErrors }.
 *
 * HIGHEST_PRECEDENCE: Post·Submission·Form 핸들러에는 Exception 전체를 받는 메서드가 있어서,
 * 순서를 정하지 않으면 AccessException이 그쪽으로 빠져 500이 될 수 있다.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class AccessExceptionHandler {

    @ExceptionHandler(AccessException.class)
    public ResponseEntity<ErrorResponse> handle(AccessException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorResponse(e.getCode(), e.getMessage(), List.of()));
    }
}
