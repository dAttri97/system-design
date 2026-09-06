package com.attri.systemdesign.bitly.store;

import java.util.Optional;

import com.attri.systemdesign.bitly.domain.UrlMapping;

public interface UrlCache {

	void put(UrlMapping mapping);

	Optional<UrlMapping> get(String shortCode);

	void invalidate(String shortCode);
}
