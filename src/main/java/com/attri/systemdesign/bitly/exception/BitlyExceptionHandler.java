package com.attri.systemdesign.bitly.exception;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice(basePackages = "com.attri.systemdesign.bitly")
public class BitlyExceptionHandler {

	@ExceptionHandler(InvalidUrlException.class)
	public ResponseEntity<?> handleInvalidUrl(InvalidUrlException ex, HttpServletRequest request) {
		return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(AliasConflictException.class)
	public ResponseEntity<?> handleAliasConflict(AliasConflictException ex, HttpServletRequest request) {
		return errorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(UrlNotFoundException.class)
	public ResponseEntity<?> handleNotFound(UrlNotFoundException ex, HttpServletRequest request) {
		return errorResponse(HttpStatus.NOT_FOUND, "Link not found", request);
	}

	@ExceptionHandler(UrlExpiredException.class)
	public ResponseEntity<?> handleExpired(UrlExpiredException ex, HttpServletRequest request) {
		return errorResponse(HttpStatus.GONE, "This link has expired", request);
	}

	private ResponseEntity<?> errorResponse(HttpStatus status, String message, HttpServletRequest request) {
		if (acceptsHtml(request)) {
			String body = """
					<!DOCTYPE html>
					<html lang="en">
					<head>
					  <meta charset="UTF-8">
					  <meta name="viewport" content="width=device-width, initial-scale=1.0">
					  <title>%s</title>
					  <style>
					    body { font-family: system-ui, sans-serif; max-width: 480px; margin: 4rem auto; padding: 0 1rem; color: #1a1a1a; }
					    h1 { font-size: 1.5rem; margin-bottom: 0.5rem; }
					    p { color: #555; line-height: 1.5; }
					    a { color: #2563eb; }
					  </style>
					</head>
					<body>
					  <h1>%s</h1>
					  <p>%s</p>
					  <p><a href="/">Create a new short link</a></p>
					</body>
					</html>
					""".formatted(status.getReasonPhrase(), status.getReasonPhrase(), message);
			return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML).body(body);
		}

		return ResponseEntity.status(status).body(Map.of("error", message));
	}

	private boolean acceptsHtml(HttpServletRequest request) {
		String accept = request.getHeader(HttpHeaders.ACCEPT);
		return accept != null && accept.contains(MediaType.TEXT_HTML_VALUE);
	}
}
