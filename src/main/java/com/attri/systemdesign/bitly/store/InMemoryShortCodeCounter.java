package com.attri.systemdesign.bitly.store;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "bitly.store.type", havingValue = "memory", matchIfMissing = true)
public class InMemoryShortCodeCounter implements ShortCodeCounter {

	private final AtomicLong counter = new AtomicLong(0);

	@Override
	public long nextValue() {
		return counter.incrementAndGet();
	}
}
