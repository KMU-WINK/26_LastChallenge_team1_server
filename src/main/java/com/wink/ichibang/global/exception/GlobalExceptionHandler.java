package com.wink.ichibang.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;

import java.util.List;

/**
 * 모든 컨트롤러의 오류를 { errorCode, message, fieldErrors } 한 가지 모양으로 바꾼다.
 * 도메인별 핸들러를 따로 두지 않는다. 새 도메인 오류는 {@link ErrorCode}에 코드만 추가하고
 * {@link BusinessException}으로 던지면 된다.
 *
 * <p>맨 아래 handleUnexpected가 Exception 전체를 받으므로, 클라이언트 잘못(4xx)은
 * 반드시 위에 개별 핸들러를 둔다. 없으면 500과 ERROR 로그가 된다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 도메인 규칙 위반 (POST_CLOSED, DUPLICATE_SUBMISSION, UNAUTHORIZED ...) */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        if (code.getStatus().is5xxServerError()) {
            log.error("비즈니스 예외 code={}", code, e);
        }
        return respond(code, e.getFieldErrors());
    }

    /** @Valid 요청 Body 검증 실패 → fieldErrors에 칸별 사유 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException e) {
        List<FieldErrorResponse> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return respond(ErrorCode.VALIDATION_FAILED, fieldErrors);
    }

    /** @RequestParam·@PathVariable의 @Min·@Max 등 검증 실패 (page, size) */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParamValidation(HandlerMethodValidationException e) {
        List<FieldErrorResponse> fieldErrors = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldErrorResponse(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return respond(ErrorCode.VALIDATION_FAILED, fieldErrors);
    }

    /** 잘못된 enum·숫자 쿼리·경로 값 (type=ABC, page=x, postId=abc) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return respond(ErrorCode.VALIDATION_FAILED, List.of(new FieldErrorResponse(e.getName(), "값을 확인해주세요")));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e) {
        return respond(ErrorCode.VALIDATION_FAILED,
                List.of(new FieldErrorResponse(e.getParameterName(), "필수 값이에요")));
    }

    /**
     * JSON 문법 오류, 잘못된 enum·날짜 형식 (type: "ABC", startDate: "2026.11.22").
     * 어느 칸이 틀렸는지 알 수 있으면 그 칸 이름을, 아니면 body를 내려준다.
     * 예외 메시지에는 요청 본문이 그대로 들어 있어 응답·로그에 옮기지 않는다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return respond(ErrorCode.VALIDATION_FAILED,
                List.of(new FieldErrorResponse(fieldOf(e), "요청 형식을 확인해주세요")));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return respond(ErrorCode.PATH_NOT_FOUND, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        // 위에서 따로 다루지 않은 스프링 표준 4xx 오류는 원래 상태 코드를 유지한다 (본문 모양은 같다)
        if (e instanceof org.springframework.web.ErrorResponse standard && standard.getStatusCode().is4xxClientError()) {
            ErrorCode code = ErrorCode.INVALID_REQUEST;
            return ResponseEntity.status(standard.getStatusCode())
                    .body(new ErrorResponse(code.name(), code.getMessage(), List.of()));
        }
        log.error("처리되지 않은 예외", e);
        return respond(ErrorCode.INTERNAL_ERROR, List.of());
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode code, List<FieldErrorResponse> fieldErrors) {
        return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code, fieldErrors));
    }

    private FieldErrorResponse toFieldError(FieldError error) {
        return new FieldErrorResponse(error.getField(), error.getDefaultMessage());
    }

    /** Jackson이 알려주는 JSON 경로 (예: questions[0].type). 알 수 없으면 body */
    private static String fieldOf(HttpMessageNotReadableException e) {
        if (!(e.getCause() instanceof JacksonException jackson) || jackson.getPath().isEmpty()) {
            return "body";
        }
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference ref : jackson.getPath()) {
            if (ref.getPropertyName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(ref.getPropertyName());
            } else if (ref.getIndex() >= 0) {
                path.append('[').append(ref.getIndex()).append(']');
            }
        }
        return path.isEmpty() ? "body" : path.toString();
    }
}
