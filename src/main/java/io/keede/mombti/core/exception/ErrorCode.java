package io.keede.mombti.core.exception;

import org.springframework.http.HttpStatus;

/** 오류 페이지로 가는 오류 (E-28, E-29). 입력 검증 오류는 폼 재렌더로 처리하므로 여기 없다. */
public enum ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "error/4xx"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "error/4xx"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "error/5xx");

    private final HttpStatus status;
    private final String view;

    ErrorCode(HttpStatus status, String view) {
        this.status = status;
        this.view = view;
    }

    public HttpStatus status() {
        return status;
    }

    public String view() {
        return view;
    }
}
