package com.attri.systemdesign.ratelimiter.web;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import com.attri.systemdesign.ratelimiter.domain.RateLimitRequestContext;

@Component
public class RateLimitRequestContextFactory {

	public RateLimitRequestContext from(HttpServletRequest request) {
		return new RateLimitRequestContext(
				request.getMethod(),
				resolvePath(request),
				request.getHeader("X-User-Id"),
				resolveClientIp(request),
				request.getHeader("X-Api-Key"));
	}

	private String resolvePath(HttpServletRequest request) {
		String servletPath = request.getServletPath();
		String pathInfo = request.getPathInfo();
		if (servletPath != null && !servletPath.isEmpty()) {
			return pathInfo != null ? servletPath + pathInfo : servletPath;
		}
		String uri = request.getRequestURI();
		String contextPath = request.getContextPath();
		if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
			uri = uri.substring(contextPath.length());
		}
		return uri.isEmpty() ? "/" : uri;
	}

	private String resolveClientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			int commaIndex = forwardedFor.indexOf(',');
			return commaIndex >= 0 ? forwardedFor.substring(0, commaIndex).trim() : forwardedFor.trim();
		}
		return request.getRemoteAddr();
	}
}
