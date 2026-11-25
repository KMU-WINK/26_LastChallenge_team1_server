package com.wink.ichibang.form.exception;

import com.wink.ichibang.form.exception.FormErrorCode;
import com.wink.ichibang.form.exception.FormException;
import com.wink.ichibang.post.exception.ErrorResponse;
import com.wink.ichibang.post.exception.FieldErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * form 패키지 컨트롤러에서 난 오류만 처리한다.
 * 응답 모양은 post·submission과 같은 { errorCode, message, fieldErrors }.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.wink.ichibang.form")
public class FormExceptionHandler {

    @ExceptionHandler(FormException.class)
    public ResponseEntity<ErrorResponse> handleForm(FormException e) {
        return respond(e.getErrorCode(), List.of());
    }

    /** FORM-04 Body 검증 실패 (status 누락) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException e) {
        List<FieldErrorResponse> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                .toList();
        return respond(FormErrorCode.VALIDATION_FAILED, fieldErrors);
    }

    /** page·size 범위 위반 */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParamValidation(HandlerMethodValidationException e) {
        List<FieldErrorResponse> fieldErrors = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldErrorResponse(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return respond(FormErrorCode.VALIDATION_FAILED, fieldErrors);
    }

    /** 잘못된 status 쿼리 값, 숫자가 아닌 postId 등 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return respond(FormErrorCode.VALIDATION_FAILED,
                List.of(new FieldErrorResponse(e.getName(), "값을 확인해주세요")));
    }

    /** FORM-04 Body의 status가 SUBMITTED·COMPLETED가 아님, JSON 문법 오류 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return respond(FormErrorCode.VALIDATION_FAILED,
                List.of(new FieldErrorResponse("status", "상태 값을 확인해주세요")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        // 지원하지 않는 메서드·Content-Type 같은 스프링 표준 오류는 원래 상태 코드를 유지한다
        if (e instanceof org.springframework.web.ErrorResponse standard) {
            return ResponseEntity.status(standard.getStatusCode()).build();
        }
        log.error("신청서·제출자 관리 처리 중 예상하지 못한 오류", e);
        return respond(FormErrorCode.INTERNAL_ERROR, List.of());
    }

    private ResponseEntity<ErrorResponse> respond(FormErrorCode code, List<FieldErrorResponse> fieldErrors) {
        return ResponseEntity.status(code.getStatus())
                .body(new ErrorResponse(code.name(), code.getMessage(), fieldErrors));
    }
}
