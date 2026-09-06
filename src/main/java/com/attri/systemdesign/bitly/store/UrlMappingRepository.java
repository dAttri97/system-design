package com.attri.systemdesign.bitly.store;

import java.time.Instant;
import java.util.Optional;

import com.attri.systemdesign.bitly.domain.UrlMapping;

public interface UrlMappingRepository {

	UrlMapping save(UrlMapping mapping);

	Optional<UrlMapping> findByShortCode(String shortCode);

	boolean existsByShortCode(String shortCode);

	int deleteExpiredBefore(Instant cutoff);
}
