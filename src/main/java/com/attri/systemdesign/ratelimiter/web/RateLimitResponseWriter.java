package com.attri.systemdesign.ratelimiter.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.attri.systemdesign.ratelimiter.domain.RateLimitVerdict;

@Component
public class RateLimitResponseWriter {

	public static final String HEADER_LIMIT = "X-Ratelimit-Limit";
	public static final String HEADER_REMAINING = "X-Ratelimit-Remaining";
	public static final String HEADER_RETRY_AFTER = "X-Ratelimit-Retry-After";

	public void writeHeaders(HttpServletResponse response, RateLimitVerdict verdict) {
		if (verdict.limit() != Long.MAX_VALUE) {
			response.setHeader(HEADER_LIMIT, String.valueOf(verdict.limit()));
			response.setHeader(HEADER_REMAINING, String.valueOf(Math.max(0, verdict.remaining())));
		}
		if (!verdict.allowed() && verdict.retryAfterSeconds() > 0) {
			response.setHeader(HEADER_RETRY_AFTER, String.valueOf(verdict.retryAfterSeconds()));
		}
	}

	public void writeTooManyRequests(HttpServletResponse response, RateLimitVerdict verdict) throws IOException {
		writeHeaders(response, verdict);
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		String body = """
				{"error":"rate_limit_exceeded","message":"Too many requests. Retry after %d seconds.","rule":"%s"}
				""".formatted(verdict.retryAfterSeconds(), verdict.ruleName());
		response.getWriter().write(body);
	}
}
