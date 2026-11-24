package com.wink.ichibang.submission.exception;

import com.wink.ichibang.post.exception.ErrorResponse;
import com.wink.ichibang.post.exception.FieldErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * submission 패키지 컨트롤러에서 난 오류만 처리한다. 응답 모양은 post의 ErrorResponse를 그대로 쓴다.
 * 팀 공통 핸들러(global)가 생기면 PostExceptionHandler와 함께 그쪽으로 옮긴다.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.wink.ichibang.submission")
public class SubmissionExceptionHandler {

    @ExceptionHandler(SubmissionException.class)
    public ResponseEntity<ErrorResponse> handleSubmission(SubmissionException e) {
        return respond(e.getErrorCode(), e.getFieldErrors());
    }

    /** @Valid 요청 Body 검증 실패 (예: answers[0].questionId 누락) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException e) {
        List<FieldErrorResponse> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                .toList();
        return respond(SubmissionErrorCode.VALIDATION_FAILED, fieldErrors);
    }

    /** postId가 숫자가 아님 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return respond(SubmissionErrorCode.VALIDATION_FAILED,
                List.of(new FieldErrorResponse(e.getName(), "값을 확인해주세요")));
    }

    /** JSON 문법 오류, 숫자 칸에 문자 등 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return respond(SubmissionErrorCode.VALIDATION_FAILED,
                List.of(new FieldErrorResponse("body", "요청 형식을 확인해주세요")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        // 지원하지 않는 Content-Type(415) 같은 스프링 표준 오류는 원래 상태 코드를 유지한다
        if (e instanceof org.springframework.web.ErrorResponse standard) {
            return ResponseEntity.status(standard.getStatusCode()).build();
        }
        log.error("제출 처리 중 예상하지 못한 오류", e);
        return respond(SubmissionErrorCode.INTERNAL_ERROR, List.of());
    }

    private ResponseEntity<ErrorResponse> respond(SubmissionErrorCode code, List<FieldErrorResponse> fieldErrors) {
        return ResponseEntity.status(code.getStatus())
                .body(new ErrorResponse(code.name(), code.getMessage(), fieldErrors));
    }
}
