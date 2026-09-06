package com.attri.systemdesign.bitly.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.attri.systemdesign.bitly.config.BitlyProperties;
import com.attri.systemdesign.bitly.exception.UrlNotFoundException;
import com.attri.systemdesign.bitly.service.RedirectService;
import com.attri.systemdesign.bitly.util.UrlValidator;

@Controller
@ConditionalOnProperty(name = "bitly.enabled", havingValue = "true", matchIfMissing = true)
public class RedirectController {

	private final RedirectService redirectService;
	private final BitlyProperties properties;

	public RedirectController(RedirectService redirectService, BitlyProperties properties) {
		this.redirectService = redirectService;
		this.properties = properties;
	}

	@GetMapping("/{shortCode}")
	public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
		if (!UrlValidator.isValidShortCode(shortCode) || properties.getReservedPaths().contains(shortCode)) {
			throw new UrlNotFoundException(shortCode);
		}

		String longUrl = redirectService.resolveLongUrl(shortCode);
		return ResponseEntity.status(HttpStatus.FOUND)
				.header(HttpHeaders.LOCATION, longUrl)
				.build();
	}
}
