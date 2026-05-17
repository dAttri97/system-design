package com.attri.systemdesign.ratelimiter.rule;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {

	private static final Pattern DURATION = Pattern.compile("^(\\d+)([smhdw])$");

	private DurationParser() {
	}

	public static Duration parse(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Duration must not be blank");
		}
		Matcher matcher = DURATION.matcher(value.trim().toLowerCase());
		if (!matcher.matches()) {
			return Duration.parse("PT" + value.trim().toUpperCase());
		}
		long amount = Long.parseLong(matcher.group(1));
		return switch (matcher.group(2)) {
			case "s" -> Duration.ofSeconds(amount);
			case "m" -> Duration.ofMinutes(amount);
			case "h" -> Duration.ofHours(amount);
			case "d" -> Duration.ofDays(amount);
			case "w" -> Duration.ofDays(amount * 7);
			default -> throw new IllegalArgumentException("Unsupported duration: " + value);
		};
	}
}
