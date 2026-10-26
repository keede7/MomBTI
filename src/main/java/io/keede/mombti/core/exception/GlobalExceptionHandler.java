package io.keede.mombti.core.exception;

import io.keede.mombti.web.support.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 오류 페이지 (docs/ARCHITECTURE.md 에러 처리). 로그에는 requestId와 예외 타입만 남긴다.
 * 예외 메시지와 스택트레이스에는 입력값이 섞일 수 있으므로 기록하지 않는다 (ADR-003).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ModelAndView notFound() {
        return page(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView methodNotAllowed() {
        return page(ErrorCode.METHOD_NOT_ALLOWED);
    }

    /** E-29: SimulationInvariantException 포함 모든 계산 예외. */
    @ExceptionHandler(Exception.class)
    public ModelAndView internalError(Exception exception, HttpServletRequest request) {
        String requestId = RequestIdFilter.requestId(request);
        log.error("requestId={} exception={}", requestId, exception.getClass().getName());
        return page(ErrorCode.INTERNAL_ERROR).addObject("requestId", requestId);
    }

    private static ModelAndView page(ErrorCode errorCode) {
        return new ModelAndView(errorCode.view(), errorCode.status());
    }
}
