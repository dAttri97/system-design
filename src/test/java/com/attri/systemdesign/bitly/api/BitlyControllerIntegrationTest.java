package com.attri.systemdesign.bitly.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class BitlyControllerIntegrationTest {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	void shortenAndRedirect() throws Exception {
		mockMvc.perform(post("/urls")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"long_url":"https://www.example.com/very/long/path"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.short_url").exists());

		String response = mockMvc.perform(post("/urls")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"long_url":"https://www.example.com/redirect-target"}
						"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String shortUrl = response.replaceAll(".*\"short_url\"\\s*:\\s*\"([^\"]+)\".*", "$1");
		String shortCode = shortUrl.substring(shortUrl.lastIndexOf('/') + 1);

		mockMvc.perform(get("/" + shortCode))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://www.example.com/redirect-target"));
	}

	@Test
	void customAliasConflictReturns409() throws Exception {
		mockMvc.perform(post("/urls")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"long_url":"https://www.example.com/a","custom_alias":"launch-day"}
						"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/urls")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"long_url":"https://www.example.com/b","custom_alias":"launch-day"}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Alias unavailable: launch-day"));
	}

	@Test
	void invalidUrlReturns400() throws Exception {
		mockMvc.perform(post("/urls")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"long_url":"not-valid"}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unknownShortCodeReturns404() throws Exception {
		mockMvc.perform(get("/does-not-exist"))
				.andExpect(status().isNotFound());
	}

	@Test
	void expiredShortCodeReturns410() throws Exception {
		Instant expired = Instant.now().minus(1, ChronoUnit.HOURS);

		mockMvc.perform(post("/urls")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"long_url":"https://www.example.com/expired","custom_alias":"expired-link","expiration_date":"%s"}
						""".formatted(expired)))
				.andExpect(status().isBadRequest());

		Instant futureExpiry = Instant.now().plus(1, ChronoUnit.SECONDS);

		mockMvc.perform(post("/urls")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"long_url":"https://www.example.com/will-expire","custom_alias":"soon-expired","expiration_date":"%s"}
						""".formatted(futureExpiry)))
				.andExpect(status().isCreated());

		Thread.sleep(1100);

		mockMvc.perform(get("/soon-expired"))
				.andExpect(status().isGone());
	}
}
