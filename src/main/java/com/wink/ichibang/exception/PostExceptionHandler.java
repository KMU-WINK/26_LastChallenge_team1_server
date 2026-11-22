package com.wink.ichibang.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * post 패키지의 컨트롤러에서 난 예외만 처리한다.
 * 팀 공통 예외 처리기가 생기면 그쪽으로 합친다.
 */
@RestControllerAdvice(basePackages = "com.wink.activity.post")
public class PostExceptionHandler {

    /** 도메인 규칙 위반 (POST_NOT_FOUND, POST_NOT_DRAFT ...) */
    @ExceptionHandler(PostException.class)
    public ResponseEntity<PostErrorResponse> handlePostException(PostException e) {
        PostErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getStatus()).body(PostErrorResponse.of(code, e.getDetail()));
    }

    /** @Valid @RequestBody 검증 실패 → 필드별 사유를 detail에 담는다 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<PostErrorResponse> handleInvalidBody(MethodArgumentNotValidException e) {
        List<FieldErrorDetail> fields = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        return badRequest(fields);
    }

    /** 쿼리 파라미터 검증 실패·누락, 잘못된 enum 값, 깨진 JSON */
    @ExceptionHandler({
            HandlerMethodValidationException.class,
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<PostErrorResponse> handleBadRequest(Exception e) {
        return badRequest(null);
    }

    private ResponseEntity<PostErrorResponse> badRequest(Object detail) {
        PostErrorCode code = PostErrorCode.INVALID_INPUT;
        return ResponseEntity.status(code.getStatus()).body(PostErrorResponse.of(code, detail));
    }

    public record FieldErrorDetail(String field, String reason) {
    }
}
