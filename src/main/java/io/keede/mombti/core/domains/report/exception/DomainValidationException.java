package io.keede.mombti.core.domains.report.exception;

import java.util.List;

/**
 * 웹 검증을 우회한 입력을 도메인에서 다시 막을 때 쓴다. 웹 계층이 BindingResult로 바꾼다.
 * 메시지에는 code와 field만 담는다 (사용자 입력값 금지). {@code arguments}는 화면 안내 문구용이다.
 */
public class DomainValidationException extends RuntimeException {

    private final String code;
    private final String field;
    private final List<Object> arguments;

    public DomainValidationException(String code, String field, Object... arguments) {
        super("code=" + code + ", field=" + field);
        this.code = code;
        this.field = field;
        this.arguments = List.of(arguments);
    }

    public String code() {
        return code;
    }

    public String field() {
        return field;
    }

    public List<Object> arguments() {
        return arguments;
    }
}
