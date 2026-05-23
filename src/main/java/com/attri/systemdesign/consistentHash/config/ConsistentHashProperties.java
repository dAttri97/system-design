package com.attri.systemdesign.consistenthash.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consistent-hash")
public class ConsistentHashProperties {

	private boolean enabled = true;
	private int virtualNodesPerServer = 150;
	private Nodes nodes = new Nodes();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getVirtualNodesPerServer() {
		return virtualNodesPerServer;
	}

	public void setVirtualNodesPerServer(int virtualNodesPerServer) {
		this.virtualNodesPerServer = virtualNodesPerServer;
	}

	public Nodes getNodes() {
		return nodes;
	}

	public void setNodes(Nodes nodes) {
		this.nodes = nodes;
	}

	public static class Nodes {
		private String path = "classpath:consistent-hash/nodes.yml";

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}
	}
}
