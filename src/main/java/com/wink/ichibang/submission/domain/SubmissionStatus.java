package com.wink.ichibang.submission.domain;

/** 제출 상태. 취소한 제출은 행이 삭제되므로 상태가 없다 (SUB-03) */
public enum SubmissionStatus {
    SUBMITTED,  // 제출됨
    COMPLETED   // 참여 완료 (운영진 처리, FORM-04). 부원은 수정·취소할 수 없다
}
