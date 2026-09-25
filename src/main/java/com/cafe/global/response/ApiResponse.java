package com.cafe.global.response;

import com.cafe.global.error.ErrorCode;
import com.cafe.global.error.ErrorResponse;
import org.springframework.http.ResponseEntity;

public record ApiResponse<T>(boolean success, T data, ErrorResponse error) {

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data){
        return ResponseEntity.ok(new ApiResponse<>(true, data, null));
    }

    public static ResponseEntity<ApiResponse<Void>> fail(ErrorCode code){
        return ResponseEntity.status(code.getStatus())
                             .body(new ApiResponse<>(false, null, new ErrorResponse(code.getCode(), code.getMessage())));
    }
}
