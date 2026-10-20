package com.cafe.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT("COMMON_001", "잘못된 입력값입니다.", 400),
    INTERNAL_ERROR("COMMON_002", "서버 내부 오류가 발생했습니다.", 500),

    // 인증
    DUPLICATE_USER_ID("AUTH_001",  "이미 사용 중인 아이디입니다.", 409),
    DUPLICATE_EMAIL("AUTH_002",  "이미 사용 중인 이메일입니다.", 409),
    INVALID_CREDENTIALS("AUTH_003",  "유효하지 않은 아이디나 비밀번호입니다.", 401),
    INVALID_TOKEN("AUTH_004",  "유효하지 않은 토큰입니다.", 401);

    private final String code;
    private final String message;
    private final int status;
}
