package com.attri.systemdesign.bitly.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.attri.systemdesign.bitly.api.dto.CreateUrlRequest;
import com.attri.systemdesign.bitly.api.dto.CreateUrlResponse;
import com.attri.systemdesign.bitly.service.UrlShortenerService;

@RestController
@ConditionalOnProperty(name = "bitly.enabled", havingValue = "true", matchIfMissing = true)
public class UrlController {

	private final UrlShortenerService urlShortenerService;

	public UrlController(UrlShortenerService urlShortenerService) {
		this.urlShortenerService = urlShortenerService;
	}

	@PostMapping("/urls")
	public ResponseEntity<CreateUrlResponse> createShortUrl(@RequestBody CreateUrlRequest request) {
		if (request == null || request.longUrl() == null || request.longUrl().isBlank()) {
			throw new com.attri.systemdesign.bitly.exception.InvalidUrlException("long_url is required");
		}

		String shortUrl = urlShortenerService.shorten(
				request.longUrl(),
				request.customAlias(),
				request.expirationDate());

		return ResponseEntity.status(HttpStatus.CREATED).body(new CreateUrlResponse(shortUrl));
	}
}
