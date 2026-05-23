package com.attri.systemdesign.consistenthash.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class ConsistentHashControllerIntegrationTest {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	void locateReturnsBootstrapNode() throws Exception {
		mockMvc.perform(get("/api/consistent-hash/locate").param("key", "demo-key"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nodeId").exists())
				.andExpect(jsonPath("$.ringVersion").isNumber());
	}

	@Test
	void nodesEndpointListsBootstrapCluster() throws Exception {
		mockMvc.perform(get("/api/consistent-hash/nodes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nodes[0]").value("cache-01"))
				.andExpect(jsonPath("$.virtualNodesPerServer").value(150));
	}
}
