package com.attri.systemdesign.bitly.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateUrlResponse(@JsonProperty("short_url") String shortUrl) {
}
