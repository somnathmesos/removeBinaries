package com.mesosphere.sdk.nifi.scheduler;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NifiApiClient {
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	private static final int pollThreshold = 5;
	private static final int pollSleep = 5000;
	private String taskName;
	private String frameworkHost;
	private Integer frameworkWebPort;
	private URL baseUrl;
	
	public NifiApiClient(String taskName, String frameworkHost, Integer frameworkWebPort) {
		super();
		this.taskName = taskName;
		this.frameworkHost = frameworkHost;
		this.frameworkWebPort = frameworkWebPort;
		try {
			this.baseUrl = new URL("http", this.taskName + "." + this.frameworkHost, this.frameworkWebPort, "/nifi-api");
			logger.info(String.format("Base URL is: %s", this.baseUrl.toString()));
		} catch (MalformedURLException e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}
	}
	
	public NifiApiClient(String baseUrl) {
		try {
			this.baseUrl = new URL("http", baseUrl, "/nifi-api");
			logger.info(String.format("Base URL is: %s", this.baseUrl.toString()));
		} catch (MalformedURLException e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}
	}	

	public void removeDisconnectedNodes() {
		List<String> disconnectedNodes = getDisconnectedNodes();
		if (disconnectedNodes.isEmpty()) {
			logger.info("There is no disconnected node in the Nifi cluster.");
		} else {
			disconnectedNodes.forEach(nodeId -> {
				removeNode(nodeId);
			});
		}
	}
	
	public List<String> getDisconnectedNodes() {
		return getNodes("DISCONNECTED");
	}
	
	private List<String> getNodes(final String status) {
		List<String> nodes = new ArrayList<String>();
		Optional<WebTarget> resource = getResource("controller/cluster");
		resource.ifPresent(target -> {
			logger.info(String.format("Full URL for Nifi nodes retrieval: %s", target.getUri()));
			Boolean continuePoll = Boolean.TRUE;
			int pollCount = 1;
			do {
				logger.info(String.format("Polling sequence: %d", pollCount));
				Response response = target.request(MediaType.APPLICATION_JSON).get();
				logger.info(String.format("The Nifi nodes retrieval response status is: %d", response.getStatus()));
				continuePoll = !((response.getStatus() == Response.Status.OK.getStatusCode())
								|| (pollCount >= pollThreshold));
				if (response.getStatus() == Response.Status.OK.getStatusCode()) {
					String responseStr = response.readEntity(String.class);
					logger.info(String.format("The Nifi nodes details are: %s", responseStr));
					JSONObject json = new JSONObject(responseStr);
					JSONArray jsonArray = (JSONArray)((JSONObject)json.get("cluster")).get("nodes");
					jsonArray.forEach(jsonObject -> {
						JSONObject nodeJson = (JSONObject)jsonObject;
						String statusVal = nodeJson.getString("status");
						if (status.equalsIgnoreCase(statusVal)) {
							String nodeId = nodeJson.getString("nodeId");
							nodes.add(nodeId);
							logger.info(String.format("The %s nodeId is: %s", status, nodeId));
						}
					});
				}
				pollCount = pollCount + 1;
				try {
					Thread.sleep(pollSleep);
				} catch (InterruptedException e) {
					logger.error("Poll sleep is interrupted.");
				}
			} while (continuePoll);
		});
		return nodes;
	}
	
	private void removeNode(String nodeId) {
		Optional<WebTarget> resource = getResource("controller/cluster/nodes/" + nodeId);
		resource.ifPresent(target -> {
			logger.info(String.format("Full URL for Nifi node removal: %s", target.getUri()));
			Boolean continuePoll = Boolean.TRUE;
			int pollCount = 1;
			do {
				logger.info(String.format("Polling sequence: %d", pollCount));
				int status = target.request(MediaType.APPLICATION_JSON).delete().getStatus();
				logger.info(String.format("The Nifi node removal response status is: %d", status));
				continuePoll = !((status == Response.Status.OK.getStatusCode())
						|| (pollCount >= pollThreshold));				
				if (status == Response.Status.OK.getStatusCode()) {
					logger.info(String.format("The node %s has been sucessfully removed.", nodeId));
				} else {
					logger.info(String.format("There is a problem to remove the node %s.", nodeId));
				}
				pollCount = pollCount + 1;
				try {
					Thread.sleep(pollSleep);
				} catch (InterruptedException e) {
					logger.error("Poll sleep is interrupted.");
				}
			} while (continuePoll);				
		});	
	}
	
	private Optional<WebTarget> getResource(String path) {
		WebTarget resource = null;
		try {
			Client client = ClientBuilder.newClient();
			resource = client.target(this.baseUrl.toURI())
							.path(path);
		} catch (URISyntaxException e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}
		return Optional.of(resource);
	}
}
