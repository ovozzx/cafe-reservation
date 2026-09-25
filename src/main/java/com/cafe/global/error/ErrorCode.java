package com.cafe.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT("COMMON_001", "잘못된 입력값입니다.", 400),
    INTERNAL_ERROR("COMMON_002", "서버 내부 오류가 발생했습니다.", 500);

    private final String code;
    private final String message;
    private final int status;
}
