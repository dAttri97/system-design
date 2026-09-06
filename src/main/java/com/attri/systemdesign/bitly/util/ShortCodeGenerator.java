package com.attri.systemdesign.bitly.util;

import org.springframework.stereotype.Component;

import com.attri.systemdesign.bitly.store.ShortCodeCounter;

@Component
public class ShortCodeGenerator {

	private final ShortCodeCounter counter;

	public ShortCodeGenerator(ShortCodeCounter counter) {
		this.counter = counter;
	}

	public String generate() {
		return Base62Encoder.encode(counter.nextValue());
	}
}
