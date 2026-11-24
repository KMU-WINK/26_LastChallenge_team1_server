package com.wink.ichibang.post.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * post 패키지 컨트롤러에서 난 오류만 처리한다.
 * 팀 공통 핸들러(global)가 생기면 그쪽으로 옮긴다.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.wink.ichibang.post")
public class PostExceptionHandler {

    /** 도메인 규칙 위반 (INVALID_PERIOD, POST_CLOSED, POST_HAS_SUBMISSIONS ...) */
    @ExceptionHandler(PostException.class)
    public ResponseEntity<ErrorResponse> handlePost(PostException e) {
        PostErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code, e.getFieldErrors()));
    }

    /** @Valid 요청 Body 검증 실패 → fieldErrors에 칸별 사유 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException e) {
        List<FieldErrorResponse> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return validationFailed(fieldErrors);
    }

    /** @RequestParam의 @Min·@Max 등 검증 실패 (page, size) */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParamValidation(HandlerMethodValidationException e) {
        List<FieldErrorResponse> fieldErrors = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldErrorResponse(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return validationFailed(fieldErrors);
    }

    /** 잘못된 enum·숫자 쿼리 값 (type=ABC, page=x) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return validationFailed(List.of(new FieldErrorResponse(e.getName(), "값을 확인해주세요")));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e) {
        return validationFailed(List.of(new FieldErrorResponse(e.getParameterName(), "필수 값이에요")));
    }

    /** JSON 문법 오류, 잘못된 enum·날짜 형식 (type: "ABC", startDate: "2026.11.22") */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return validationFailed(List.of(new FieldErrorResponse("body", "요청 형식을 확인해주세요")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("신청글 처리 중 예상하지 못한 오류", e);
        PostErrorCode code = PostErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code));
    }

    private ResponseEntity<ErrorResponse> validationFailed(List<FieldErrorResponse> fieldErrors) {
        PostErrorCode code = PostErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code, fieldErrors));
    }

    private FieldErrorResponse toFieldError(FieldError error) {
        return new FieldErrorResponse(error.getField(), error.getDefaultMessage());
    }
}
