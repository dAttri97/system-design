package com.attri.systemdesign.ratelimiter.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class RateLimitFilterIntegrationTest {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private RateLimitFilter rateLimitFilter;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(rateLimitFilter).build();
	}

	@Test
	void throttlesWritePostsPerUser() throws Exception {
		for (int attempt = 0; attempt < 2; attempt++) {
			mockMvc.perform(post("/api/posts")
							.header("X-User-Id", "integration-user")
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"text\":\"hello\"}"))
					.andExpect(status().isOk());
		}

		mockMvc.perform(post("/api/posts")
						.header("X-User-Id", "integration-user")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"text\":\"hello\"}"))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string(RateLimitResponseWriter.HEADER_LIMIT, "2"))
				.andExpect(header().exists(RateLimitResponseWriter.HEADER_RETRY_AFTER))
				.andExpect(jsonPath("$.error").value("rate_limit_exceeded"));
	}
}
