package com.wink.ichibang.global.time;

import java.time.ZoneId;

/** 서버 시간 기준. 신청 기간·제출 일시 등 날짜 계산은 모두 KST로 한다 */
public final class TimeZones {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private TimeZones() {
    }
}
