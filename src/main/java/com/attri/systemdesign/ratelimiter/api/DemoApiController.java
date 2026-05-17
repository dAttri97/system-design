package com.attri.systemdesign.ratelimiter.api;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DemoApiController {

	@PostMapping("/posts")
	public ResponseEntity<Map<String, String>> createPost(@RequestBody(required = false) Map<String, Object> body) {
		return ResponseEntity.ok(Map.of("status", "created"));
	}

	@PostMapping("/accounts")
	public ResponseEntity<Map<String, String>> createAccount() {
		return ResponseEntity.ok(Map.of("status", "account_created"));
	}

	@PostMapping("/auth/login")
	public ResponseEntity<Map<String, String>> login() {
		return ResponseEntity.ok(Map.of("status", "authenticated"));
	}
}
