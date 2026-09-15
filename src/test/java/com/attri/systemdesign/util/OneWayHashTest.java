package com.attri.systemdesign.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OneWayHashTest {

    @Test
    void producesTheKnownSha256DigestForText() {
        assertThat(OneWayHash.hash("hello"))
                .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    void serializesMapsCanonicallyBeforeHashing() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("amount", 100);
        first.put("account", "alice");
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("account", "alice");
        second.put("amount", 100);

        assertThat(OneWayHash.hash(first)).isEqualTo(OneWayHash.hash(second));
    }

    @Test
    void producesAFixedWidthAlphanumericBase62Hash() {
        String hash = OneWayHash.hashBase62("hello");

        assertThat(hash).hasSize(43).matches("[0-9A-Za-z]{43}");
        assertThat(OneWayHash.hashBase62("hello")).isEqualTo(hash);
        assertThat(OneWayHash.hashBase62("world")).isNotEqualTo(hash);
    }

    @Test
    void serializesObjectsCanonicallyBeforeBase62Hashing() {
        assertThat(OneWayHash.hashBase62(Map.of("account", "alice", "amount", 100)))
                .isEqualTo(OneWayHash.hashBase62(Map.of("amount", 100, "account", "alice")));
    }
}
