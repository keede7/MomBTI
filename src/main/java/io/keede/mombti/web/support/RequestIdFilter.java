package io.keede.mombti.web.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 요청마다 requestId를 만들어 MDC, 요청 속성, 응답 헤더에 둔다. 에러 로그와 5xx 화면이 이 값만 쓴다. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String KEY = "requestId";
    public static final String HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        request.setAttribute(KEY, requestId);
        response.setHeader(HEADER, requestId);
        MDC.put(KEY, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(KEY);
        }
    }

    /** 필터를 거치지 않은 요청이면 "unknown". */
    public static String requestId(HttpServletRequest request) {
        return request.getAttribute(KEY) instanceof String requestId ? requestId : "unknown";
    }
}
