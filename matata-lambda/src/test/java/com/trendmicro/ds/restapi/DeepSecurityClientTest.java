package com.trendmicro.ds.restapi;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.trendmicro.ds.platform.rest.object.ListEventBasedTasksResponse;
import com.trendmicro.ds.platform.rest.object.alerts.ListAlertsResponse;
import com.trendmicro.ds.platform.rest.object.monitoring.JVMUsageElement;
import com.trendmicro.ds.platform.rest.object.monitoring.JVMUsageListing;
import com.trendmicro.ds.platform.rest.object.proxies.ListProxiesResponse;
import com.trendmicro.ds.platform.rest.object.util.HostStatusSummaryElement;
import com.trendmicro.ds.platform.rest.object.util.StatusSummaryElement;

public class DeepSecurityClientTest {

	@Test
	public void testConnection() throws Exception {
		ListEventBasedTasksResponse listEventBasedTasks;
		ListProxiesResponse listProxies;
		try (DeepSecurityClient dsClient = getDSClient()) {
			dsClient.disableTrustManager();
			//			listEventBasedTasks = dsClient.listEventBasedTasks();

			//			listProxies = dsClient.listProxies();
			//			ListAlertsResponse listAlerts = dsClient.listAlerts();
			//			System.out.println(listAlerts.getAlerts().get(0).getName());
			//			dsClient.listInventories();

			StatusSummaryElement statusSummary = dsClient.getStatusSummary();
			HostStatusSummaryElement hostStatusSummary = statusSummary.getHostStatusSummary();
			System.out.println(hostStatusSummary.getCriticalHosts() + "/" + hostStatusSummary.getWarningHosts() + "/" + hostStatusSummary.getOnlineHosts() + "/" + hostStatusSummary.getUnmanageHosts());
		}
		//		for (EventBasedTask t : listEventBasedTasks.getTasks()) {
		//			System.out.println(t.getName());
		//		}
		//		for (Proxy p : listProxies.getProxies()) {
		//			System.out.println(p.getName());
		//		}
	}

	@Test
	public void testJackson() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
		mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
		mapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, true);
		mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
		mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
		//		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		//		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

		mapper.setSerializationInclusion(Include.NON_NULL);
		//		mapper.registerModule(new JaxbAnnotationModule());

		String content = "{\"ListAlertsResponse\":{\"alerts\":[{\"id\":8,\"typeID\":99,\"name\":\"MemoryWarningThresholdExceeded\",\"description\":\"Thememorywarningthresholdhasbeenexceeded.Formoreinformationaboutthisandotheralerts,visittheDeepSecurityHelpCenterat:https://help.deepsecurity.trendmicro.com/\",\"dismissible\":false,\"severity\":\"warning\",\"managerNodeID\":1,\"timeRaised\":1517735113164,\"timeChanged\":1517735113164,\"targets\":[{\"urn\":\"urn:tmds:core::0:manager-node/1\"}]}]}}";
		ListAlertsResponse readValue = mapper.readValue(content, ListAlertsResponse.class);
		System.out.println(readValue.getAlerts().get(0).getName());
	}

	@Test
	public void testGetJvmUsage() throws Exception {
		try (DeepSecurityClient dsClient = getDSClient()) {
			JVMUsageListing jvmUsage = dsClient.listJvmUsage(null, null, null);
			List<JVMUsageElement> jvmOrderedList = jvmUsage.getUsages().stream().sorted((a, b) -> b.getTime().compareTo(a.getTime())).collect(Collectors.toList());
			System.out.println(jvmOrderedList.get(1).getNativeCPUPercent());
			System.out.println(jvmOrderedList.get(0).getNativeMemoryUsed());
		}
	}

	private DeepSecurityClient getDSClient() throws IOException {
		Properties prop = new Properties();
		prop.load(this.getClass().getResourceAsStream("dsm.properties"));
		DeepSecurityClient dsClient = new DeepSecurityClient(prop.getProperty("dsm.url"), prop.getProperty("dsm.user"), prop.getProperty("dsm.password"), null);
		dsClient.disableTrustManager();
		return dsClient;
	}
}
