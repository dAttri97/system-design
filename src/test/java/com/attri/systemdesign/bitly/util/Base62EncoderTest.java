package com.attri.systemdesign.bitly.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Base62EncoderTest {

	@Test
	void encodesZero() {
		assertEquals("0", Base62Encoder.encode(0));
	}

	@Test
	void encodesOneBillion() {
		assertEquals("15ftgG", Base62Encoder.encode(1_000_000_000L));
	}
}
