package com.attri.systemdesign.consistenthash.config;

import java.util.List;

public class ConsistentHashNodesDocument {

	private List<NodeEntry> nodes = List.of();

	public List<NodeEntry> getNodes() {
		return nodes;
	}

	public void setNodes(List<NodeEntry> nodes) {
		this.nodes = nodes;
	}

	public static class NodeEntry {
		private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}
}
