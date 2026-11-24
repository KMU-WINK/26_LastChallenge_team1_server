package com.wink.ichibang.post.dto;

/**
 * 성공 응답 껍데기 { "data": ... }.
 * 명세는 data 안쪽만 적었다(공통 규칙 3절). 공통 규칙의 실제 모양을 받으면 이 클래스만 맞춘다.
 */
public record ApiResponse<T>(T data) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data);
    }
}
