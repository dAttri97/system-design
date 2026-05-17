package com.attri.systemdesign.ratelimiter.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.attri.systemdesign.ratelimiter.config.RateLimiterProperties;
import com.attri.systemdesign.ratelimiter.domain.RateLimitRequestContext;
import com.attri.systemdesign.ratelimiter.domain.RateLimitVerdict;
import com.attri.systemdesign.ratelimiter.service.RateLimiterService;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnProperty(name = "rate-limiter.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

	private final RateLimiterService rateLimiterService;
	private final RateLimitRequestContextFactory contextFactory;
	private final RateLimitResponseWriter responseWriter;

	public RateLimitFilter(
			RateLimiterService rateLimiterService,
			RateLimitRequestContextFactory contextFactory,
			RateLimitResponseWriter responseWriter) {
		this.rateLimiterService = rateLimiterService;
		this.contextFactory = contextFactory;
		this.responseWriter = responseWriter;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			return true;
		}
		String path = request.getRequestURI();
		return path.startsWith("/actuator") || path.startsWith("/error");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		RateLimitRequestContext context = contextFactory.from(request);
		RateLimitVerdict verdict = rateLimiterService.check(context);

		responseWriter.writeHeaders(response, verdict);
		if (!verdict.allowed()) {
			responseWriter.writeTooManyRequests(response, verdict);
			return;
		}

		filterChain.doFilter(request, response);
	}
}
