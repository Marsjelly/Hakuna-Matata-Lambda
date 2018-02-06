// Copyright (C) 2016 Trend Micro Inc. All rights reserved.

package com.trendmicro.ds.restapi;

import java.net.ConnectException;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.providers.jaxb.json.JsonMapProvider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.trendmicro.ds.appcontrol.rest.api.IApplicationControlPolicyAPI;
import com.trendmicro.ds.appcontrol.rest.api.IRulesetAPI;
import com.trendmicro.ds.appcontrol.rest.api.ISoftwareInventoryAPI;
import com.trendmicro.ds.appcontrol.rest.api.ITrustedUpdateModeAPI;
import com.trendmicro.ds.appcontrol.rest.object.CreateInventoryRequest;
import com.trendmicro.ds.appcontrol.rest.object.CreateInventoryResponse;
import com.trendmicro.ds.appcontrol.rest.object.CreateRulesetRequest;
import com.trendmicro.ds.appcontrol.rest.object.CreateRulesetResponse;
import com.trendmicro.ds.appcontrol.rest.object.DescribeApplicationControlPolicyResponse;
import com.trendmicro.ds.appcontrol.rest.object.DescribeInventoryResponse;
import com.trendmicro.ds.appcontrol.rest.object.DescribeRulesetResponse;
import com.trendmicro.ds.appcontrol.rest.object.DescribeTrustedUpdateModeResponse;
import com.trendmicro.ds.appcontrol.rest.object.GetInventoryResponse;
import com.trendmicro.ds.appcontrol.rest.object.ListInventoriesResponse;
import com.trendmicro.ds.appcontrol.rest.object.ListRulesetsResponse;
import com.trendmicro.ds.appcontrol.rest.object.ModifyApplicationControlPolicyRequest;
import com.trendmicro.ds.appcontrol.rest.object.ModifyTrustedUpdateModeRequest;
import com.trendmicro.ds.network.rest.api.IHostSSLConfigurationAPI;
import com.trendmicro.ds.network.rest.object.sslconfiguration.CreateSSLConfigurationRequest;
import com.trendmicro.ds.network.rest.object.sslconfiguration.CreateSSLConfigurationResponse;
import com.trendmicro.ds.network.rest.object.sslconfiguration.DescribeSSLConfigurationResponse;
import com.trendmicro.ds.network.rest.object.sslconfiguration.ListSSLConfigurationsResponse;
import com.trendmicro.ds.network.rest.object.sslconfiguration.ModifySSLConfigurationRequest;
import com.trendmicro.ds.network.rest.object.sslconfiguration.ModifySSLConfigurationResponse;
import com.trendmicro.ds.network.rest.object.sslconfiguration.SSLConfiguration;
import com.trendmicro.ds.platform.rest.api.IAlertAPI;
import com.trendmicro.ds.platform.rest.api.IAuthenticationAPI;
import com.trendmicro.ds.platform.rest.api.IEventBasedTaskAPI;
import com.trendmicro.ds.platform.rest.api.IGeneralAPI;
import com.trendmicro.ds.platform.rest.api.IManagerInfoAPI;
import com.trendmicro.ds.platform.rest.api.IMonitoringAPI;
import com.trendmicro.ds.platform.rest.api.IProxyAPI;
import com.trendmicro.ds.platform.rest.object.CreateEventBasedTaskRequest;
import com.trendmicro.ds.platform.rest.object.CreateEventBasedTaskResponse;
import com.trendmicro.ds.platform.rest.object.DSCredentials;
import com.trendmicro.ds.platform.rest.object.ListEventBasedTasksResponse;
import com.trendmicro.ds.platform.rest.object.alerts.ListAlertsResponse;
import com.trendmicro.ds.platform.rest.object.monitoring.TenantDatabaseUsageListing;
import com.trendmicro.ds.platform.rest.object.monitoring.TenantHostProtectionListing;
import com.trendmicro.ds.platform.rest.object.proxies.ListProxiesResponse;
import com.trendmicro.ds.platform.rest.object.util.StatusSummaryElement;

/**
 * Sample client helper to simplify access to the Deep Security REST API.
 */
@SuppressWarnings("deprecation")
public class DeepSecurityClient implements AutoCloseable {
	private static final int LOCK_TIMEOUT = 2000;

	private final ResteasyClientBuilder clientBuilder;

	private final Lock clientLock = new ReentrantLock();
	private ResteasyClient client;

	private final Lock targetLock = new ReentrantLock();
	private ResteasyWebTarget target;

	private final Lock sessionIDLock = new ReentrantLock();
	private String sessionID = null;

	private final String baseUrl;
	private final String username;
	private final String password;
	private final String account;

	static {
		/** RESTEasy client framework initialization that only needs to be done once */
		RegisterBuiltin.register(ResteasyProviderFactory.getInstance());
	}

	/**
	 * Construct a client to the Deep Security REST API.
	 * <p>
	 * It is a very good idea to use {@code try-with-resources} to manage your client instance and
	 * ensure it is closed properly when you're done with it. For example:
	 * <pre>
	 * try (DeepSecurityClient client = new DeepSecurityClient(url, username, password, account)) {
	 *     client.ping();
	 * }</pre>
	 * will open a client session, check that the Deep Security Manager is active and responding to
	 * requests, and close the client session.
	 * </p>
	 *
	 * @param baseUrl Base URL for the Deep Security REST API.
	 * @param username User name to use for authentication.
	 * @param password Password to use for authentication.
	 * @param account Tenant account name; required when logging in to a tenant account.
	 */
	public DeepSecurityClient(String baseUrl, String username, String password, String account) {
		//		this.clientBuilder = new ResteasyClientBuilder().register(getJSONProvider());
		//		this.clientBuilder = new ResteasyClientBuilder().register(getJsonMapprovider());

		this.clientBuilder = new ResteasyClientBuilder();
		this.baseUrl = baseUrl;
		this.username = username;
		this.password = password;
		this.account = account;
	}

	/**
	 * Gracefully log out and shut down the client.
	 * <p>
	 * If you are using {@code try-with-resources} to manage your client (a very good idea), this will
	 * be called automatically when the {@code try} block completes.
	 * </p>
	 */
	@Override
	public void close() {
		withLock(clientLock, () -> {
			if (client != null) {
				withLock(sessionIDLock, () -> {
					if (sessionID != null) {
						clientFor(IAuthenticationAPI.class).endSession(sessionID);
						sessionID = null;
					}
					return null;
				});

				getClient().close();
				client = null;
			}
			return null;
		});
	}

	/**
	 * Disable the trust manager protecting communications over the network.
	 * <p>
	 * You should <strong>only</strong> use this in a secure development environment, and <strong>never</strong>
	 * use it over an untrusted network. Disabling the trust manager will turn off the validation process that
	 * checks whether the server you are connecting to is the one you expect, and can result in your connection
	 * being hijacked, your credentials stolen, and general catastrophe. Use with great caution!
	 * </p>
	 */
	public void disableTrustManager() {
		withLock(clientLock, () -> {
			if (client != null) {
				throw new IllegalStateException("The client has already been built. If you need to turn off trust management (generally a bad idea), then do it before making any other calls.");
			}
			clientBuilder.disableTrustManager();
			return null;
		});
	}

	/**
	 * Ping the server to see if we can establish a session.
	 */
	public void ping() throws Exception {
		withSession((s) -> {
			IGeneralAPI generalClient = clientFor(IGeneralAPI.class);
			generalClient.getApiVersion();
			return "ok";
		});
	}

	public ListAlertsResponse listAlerts() throws Exception {
		return withSession((s) -> {
			IAlertAPI alertClient = clientFor(IAlertAPI.class);
			return alertClient.listAlerts(s, null, null, null, null);
		});
	}

	public StatusSummaryElement getStatusSummary() throws Exception {
		return withSession((s) -> {
			IManagerInfoAPI managerInfoClient = clientFor(IManagerInfoAPI.class);
			return managerInfoClient.getStatusSummary(s);
		});
	}

	/**
	 * List host protection information.
	 *
	 * @param tenantName
	 *     the name of the tenant to retrieve the host protection information for.
	 * @param tenantID
	 *      the ID of the tenant to retrieve the host protection information for.
	 * If neither tenantName nor tID are specified, host protection information for all tenants is retrieved.
	 * If both are specified, tID is used and tenantName is ignored.
	 * @param from
	 *     The date from which to gather the usage. If not set, then a time of one hour ago
	 *     is used.
	 * @param to
	 *     The date up to which to gather the usage. If not set, the current server time is used.
	 *
	 * @return The host usage statistics for all tenants
	 */
	public TenantHostProtectionListing listHostProtection(String tenantName, Integer tenantID, Date from, Date to) throws Exception {
		return withSession((s) -> {
			IMonitoringAPI monitoringClient = clientFor(IMonitoringAPI.class);
			return monitoringClient.listHostProtection(tenantName, tenantID, from, to, s);
		});
	}

	/**
	 * List high-level database usage information.
	 *
	 * @param tenantName
	 *     the name of the tenant to retrieve the usage information for.
	 * @param tenantID
	 *      the ID of the tenant to retrieve the usage information for.
	 * If neither tenantName nor tID are specified, usage information for all tenants is retrieved.
	 * If both are specified, tID is used and tenantName is ignored.
	 * @param from
	 *     The date from which to list the usages. If not set, then a time of one hour ago
	 *     is used.
	 * @param to
	 *     The date up to which to list the usages. If not set, the current server time is used.
	 *
	 * @return The requested database usage statistics.
	 */
	public TenantDatabaseUsageListing listDatabaseUsage(String tenantName, Integer tenantID, Date from, Date to) throws Exception {
		return withSession((s) -> {
			IMonitoringAPI monitoringClient = clientFor(IMonitoringAPI.class);
			return monitoringClient.listDatabaseUsage(tenantName, tenantID, from, to, s);
		});
	}

	/**
	 * List event-based tasks.
	 */
	public ListEventBasedTasksResponse listEventBasedTasks() throws Exception {
		return withSession((sID) -> {
			IEventBasedTaskAPI ebtClient = clientFor(IEventBasedTaskAPI.class);
			return ebtClient.listTasks(sID);
		});
	}

	/**
	 * Create a new event-based task.
	 *
	 * @param request a {@link CreateEventBasedTaskRequest} describing the details of the event-based task.
	 *
	 * @return a {@link CreateEventBasedTaskResponse} carrying the ID of the created task.
	 */
	public CreateEventBasedTaskResponse createEventBasedTask(CreateEventBasedTaskRequest createEventBasedTaskRequest) throws Exception {
		return withSession((sId) -> {
			IEventBasedTaskAPI ebtClient = clientFor(IEventBasedTaskAPI.class);
			return ebtClient.createTask(sId, createEventBasedTaskRequest);
		});
	}

	/**
	 * Delete an event-based task.
	 *
	 * @param taskID the task ID.
	 */
	public void deleteEventBasedTask(int taskID) throws Exception {
		withSession((sID) -> {
			IEventBasedTaskAPI ebtClient = clientFor(IEventBasedTaskAPI.class);
			ebtClient.deleteTask(sID, taskID);
			return null;
		});
	}

	/**
	 * Retrieve a list of rulesets.
	 * <p>
	 * Note that this will retrieve at most 5000 rulesets. To page through results, use {@link #listRulesets(Long, Operator, int)}.
	 * </p>
	 *
	 * @return a list of rulesets.
	 */
	public ListRulesetsResponse listRulesets() throws Exception {
		return withSession((sID) -> {
			IRulesetAPI rulesetClient = clientFor(IRulesetAPI.class);

			return rulesetClient.listRulesets(sID, null, null, null);
		});
	}

	/**
	 * Retrieve a list of rulesets.
	 *
	 * @param rulesetID (optional) used to define the starting point for the query. Combine with {@code rulesetIDOp} to page through results.
	 * @param rulesetIDOp (optional, required if {@code rulesetID} is specified) Currently supported operations include gt(greater than), ge(greater than or equal to), eq(eqaul to), lt(less than), and le(less than or equal to).
	 * @param maxItems (optional) the number of items to retrieve. The maximum value for this parameter is controlled by the "Maximum number of items to retrieve from database" setting on the administrator account, which defaults to 5000.
	 *
	 * @return a list of rulesets.
	 */
	public ListRulesetsResponse listRulesets(Long rulesetID, Operator rulesetIDOp, int count) throws Exception {
		return withSession((sID) -> {
			IRulesetAPI rulesetClient = clientFor(IRulesetAPI.class);

			return rulesetClient.listRulesets(sID, rulesetID, rulesetIDOp.value, Integer.valueOf(count));
		});
	}

	/**
	 * Create a new ruleset.
	 *
	 * @param request a {@code CreateRulesetRequest} object describing the ruleset.
	 *
	 * @return a {@code CreateRulesetResponse} containing the ID of the created ruleset.
	 */
	public CreateRulesetResponse createRuleset(CreateRulesetRequest request) throws Exception {
		return withSession((sId) -> {
			IRulesetAPI rulesetClient = clientFor(IRulesetAPI.class);

			return rulesetClient.createRuleset(sId, request);
		});
	}

	/**
	 * Gets the contents of one ruleset.
	 * <p>
	 * Note that this will retrieve at most 5000 rules. To page through results, use {@link #describeRuleset(Long, Long, Operator, int)}.
	 * </p>
	 *
	 * @param rulesetID the ruleset ID.
	 *
	 * @return the ruleset contents.
	 */
	public DescribeRulesetResponse describeRuleset(Long rulesetID) throws Exception {
		return withSession((sID) -> {
			IRulesetAPI rulesetClient = clientFor(IRulesetAPI.class);

			return rulesetClient.describeRuleset(sID, rulesetID, null, null, null);
		});
	}

	/**
	 * Gets the contents of one ruleset.
	 *
	 * @param rulesetID the ruleset ID.
	 * @param ruleID (optional) used to define the starting point for the query. Combine with {@code ruleIDOp} to page through results.
	 * @param ruleIDOp (optional, required if {@code ruleID} is specified) Currently supported operations include gt(greater than), ge(greater than or equal to), eq(eqaul to), lt(less than), and le(less than or equal to).
	 * @param maxItems (optional) the number of items to retrieve. The maximum value for this parameter is controlled by the "Maximum number of items to retrieve from database" setting on the administrator account, which defaults to 5000.
	 *
	 * @return the ruleset contents.
	 */
	public DescribeRulesetResponse describeRuleset(Long rulesetID, Long ruleID, Operator ruleIDOp, int count) throws Exception {
		return withSession((sID) -> {
			IRulesetAPI rulesetClient = clientFor(IRulesetAPI.class);

			return rulesetClient.describeRuleset(sID, rulesetID, ruleID, ruleIDOp.value, Integer.valueOf(count));
		});
	}

	/**
	 * Deletes one ruleset from the Deep Security Manager.
	 *
	 * @param rulesetID the ruleset ID.
	 */
	public void deleteRuleset(Long rulesetID) throws Exception {
		withSession((sID) -> {
			IRulesetAPI rulesetClient = clientFor(IRulesetAPI.class);
			rulesetClient.deleteRuleset(sID, rulesetID);
			return null;
		});
	}

	/**
	 * Retrieve the application control settings for a policy.
	 *
	 * @param policyID the policy ID.
	 *
	 * @return a DescribeApplicationControlPolicyResponse containing the application control settings for the policy.
	 */
	public DescribeApplicationControlPolicyResponse describeApplicationControlPolicy(Integer policyID) throws Exception {
		return withSession((sID) -> {
			IApplicationControlPolicyAPI policyClient = clientFor(IApplicationControlPolicyAPI.class);
			return policyClient.describeApplicationControlPolicy(sID, policyID);
		});
	}

	/**
	 * Modify the application control portion of a policy.
	 *
	 * @param request a {@link ModifyApplicationControlPolicyRequest} describing the details of the policy.
	 */
	public void modifyApplicationControlPolicy(Integer policyID, ModifyApplicationControlPolicyRequest request) throws Exception {
		withSession((sID) -> {
			IApplicationControlPolicyAPI policyClient = clientFor(IApplicationControlPolicyAPI.class);
			policyClient.modifyApplicationControlPolicy(sID, policyID, request);
			return null;
		});
	}

	/**
	 * Retrieve the list of all inventory metadata.
	 * <p>
	 * Note that this will retrieve at most 5000 items. To page through results, use {@link #listInventories(Long, Operator, int)}.
	 * </p>
	 *
	 * @return a {@link ListInventoriesResponse} containing the list of all software inventory metadata.
	 */
	public ListInventoriesResponse listInventories() throws Exception {
		return withSession((sID) -> {
			ISoftwareInventoryAPI softwareInventoryClient = clientFor(ISoftwareInventoryAPI.class);
			return softwareInventoryClient.listInventories(sID, null, null, null);
		});
	}

	/**
	 * Retrieve the list of all inventory metadata.
	 *
	 * @param inventoryID (optional) used to define the starting point for the query. Combine with {@code inventoryIDOp} to page through results.
	 * @param inventoryIDOp (optional, required if {@code inventoryID} is specified) Currently supported operations include gt(greater than), ge(greater than or equal to), eq(eqaul to), lt(less than), and le(less than or equal to).
	 * @param maxItems (optional) the number of items to retrieve. The maximum value for this parameter is controlled by the "Maximum number of items to retrieve from database" setting on the administrator account, which defaults to 5000.
	 *
	 * @return a {@link ListInventoriesResponse} containing the list of all software inventory metadata.
	 */
	public ListInventoriesResponse listInventories(Long inventoryID, Operator inventoryIDOp, int count) throws Exception {
		return withSession((sID) -> {
			ISoftwareInventoryAPI softwareInventoryClient = clientFor(ISoftwareInventoryAPI.class);
			return softwareInventoryClient.listInventories(sID, inventoryID, inventoryIDOp.value, Integer.valueOf(count));
		});
	}

	/**
	 * Create an inventory of a host.
	 *
	 * @param request a {@link CreateInventoryRequest} describing the inventory action to take.
	 *
	 * @return a {@link CreateInventoryResponse} carrying the ID of the created inventory list.
	 */
	public CreateInventoryResponse createInventory(CreateInventoryRequest createInventoryRequest) throws Exception {
		return withSession((sID) -> {
			ISoftwareInventoryAPI softwareInventoryClient = clientFor(ISoftwareInventoryAPI.class);
			return softwareInventoryClient.createInventory(sID, createInventoryRequest);
		});
	}

	/**
	 * Retrieve inventory list metadata.
	 *
	 * @param inventoryID the inventory list ID.
	 *
	 * @return a {@link DescribeInventoryResponse} containing metadata about the inventory list.
	 */
	public DescribeInventoryResponse describeInventory(Long inventoryID) throws Exception {
		return withSession((sID) -> {
			ISoftwareInventoryAPI softwareInventoryClient = clientFor(ISoftwareInventoryAPI.class);
			return softwareInventoryClient.describeInventory(sID, inventoryID);
		});
	}

	/**
	 * Retrieve inventory list details.
	 * <p>
	 * Note that this will retrieve at most 5000 inventory items. To page through results, use {@link #getInventory(Long, Long, Operator, int)}.
	 * </p>
	 *
	 * @param inventoryID the inventory list ID.
	 *
	 * @return the detailed inventory listing of all applications detected in the inventory scan.
	 */
	public GetInventoryResponse getInventory(Long inventoryID) throws Exception {
		return withSession((sID) -> {
			ISoftwareInventoryAPI softwareInventoryClient = clientFor(ISoftwareInventoryAPI.class);
			return softwareInventoryClient.getInventory(sID, inventoryID, null, null, null);
		});
	}

	/**
	 * Retrieve inventory list details.
	 *
	 * @param inventoryID the inventory list ID.
	 * @param itemID (optional) used to define the starting point for the query. Combine with {@code itemIDOp} to page through results.
	 * @param itemIDOp (optional, required if {@code itemID} is specified) Currently supported operations include gt(greater than), ge(greater than or equal to), eq(eqaul to), lt(less than), and le(less than or equal to).
	 * @param maxItems (optional) the number of items to retrieve. The maximum value for this parameter is controlled by the "Maximum number of items to retrieve from database" setting on the administrator account, which defaults to 5000.
	 *
	 * @return the detailed inventory listing of all applications detected in the inventory scan.
	 */
	public GetInventoryResponse getInventory(Long inventoryID, Long itemID, Operator itemIDOp, int count) throws Exception {
		return withSession((sID) -> {
			ISoftwareInventoryAPI softwareInventoryClient = clientFor(ISoftwareInventoryAPI.class);
			return softwareInventoryClient.getInventory(sID, inventoryID, itemID, itemIDOp.value, Integer.valueOf(count));
		});
	}

	/**
	 * Delete an inventory list.
	 *
	 * @param inventoryID the inventory list ID.
	 */
	public void deleteInventory(Long inventoryID) throws Exception {
		withSession((sID) -> {
			ISoftwareInventoryAPI softwareInventoryClient = clientFor(ISoftwareInventoryAPI.class);
			softwareInventoryClient.deleteInventory(sID, inventoryID);
			return null;
		});
	}

	/**
	 * Get the settings for trusted update mode on a host.
	 *
	 * @param hostID the host ID.
	 *
	 * @return the settings for trusted update mode on the host.
	 */
	public DescribeTrustedUpdateModeResponse describeTrustedUpdateMode(Integer hostID) throws Exception {
		return withSession((sID) -> {
			ITrustedUpdateModeAPI trustedUpdateModeClient = clientFor(ITrustedUpdateModeAPI.class);
			return trustedUpdateModeClient.describeTrustedUpdateMode(sID, hostID);
		});
	}

	/**
	 * Modify the settings for trusted update mode on a host.
	 *
	 * @param hostID the host ID.
	 * @param request a {@link ModifyTrustedUpdateModeRequest} capturing the requested new state of trusted update mode on the host.
	 */
	public void modifyTrustedUpdateMode(Integer hostID, ModifyTrustedUpdateModeRequest request) throws Exception {
		withSession((sID) -> {
			ITrustedUpdateModeAPI trustedUpdateModeClient = clientFor(ITrustedUpdateModeAPI.class);
			trustedUpdateModeClient.modifyTrustedUpdateMode(sID, hostID, request);
			return null;
		});
	}

	/**
	 * Create a new SSL configuration.
	 *
	 * @param hostID 		the ID of the host for which the new SSL configuration is being created
	 * @param request 		a {@linkplain CreateSSLConfigurationRequest} describing the SSL configuration details
	 *
	 * @return 				a {@linkplain CreateSSLConfigurationResponse} containing the created SSL configuration details
	 */
	public CreateSSLConfigurationResponse createSSLConfiguration(Integer hostID, CreateSSLConfigurationRequest request) throws Exception {
		return withSession((sID) -> {
			IHostSSLConfigurationAPI sslClient = clientFor(IHostSSLConfigurationAPI.class);
			return sslClient.createSSLConfiguration(sID, hostID, request);
		});
	}

	/**
	 * List SSL configurations.
	 *
	 * @param hostID 				the ID of the host for which the SSL configurations are being listed
	 * @return 						a {@linkplain ListSSLConfigurationResponse} containing a list of SSL configurations
	 */
	public ListSSLConfigurationsResponse listSSLConfigurations(Integer hostID) throws Exception {
		return listSSLConfigurations(hostID, null, null, null);
	}

	/**
	 * List SSL configurations.
	 *
	 * @param hostID 				the ID of the host for which the SSL configurations are being listed
	 * @param sslConfigurationID 	(optional) used to define the starting point for the query.
	 * 								Combine with {@code sslConfigurationIDOp} to page through results.
	 * @param sslConfigurationIDOp 	(optional, required if {@code sslConfigurationID} is specified) Currently supported operations are:
	 * 								<ul>
	 * 									<li><b>gt</b> (greater than)</li>
	 * 									<li><b>ge</b> (greater than or equal to)</li>
	 * 									<li><b>eq</b> (equal to)</li>
	 * 									<li><b>lt</b> (less than)</li>
	 * 									<li><b>le</b> (less than or equal to)</li>
	 * 								</ul>
	 * @param maxItems (optional) 	the maximum number of items to retrieve. The maximum value for this parameter is controlled by the
	 * 								"Maximum number of items to retrieve from database" setting on the administrator account, which defaults to 5000.
	 *
	 * @return 						a {@linkplain ListSSLConfigurationResponse} containing a list of SSL configurations
	 */
	public ListSSLConfigurationsResponse listSSLConfigurations(Integer hostID, Integer sslConfigurationID, String sslConfigurationIDOp, Integer maxItems) throws Exception {
		return withSession((sID) -> {
			IHostSSLConfigurationAPI sslClient = clientFor(IHostSSLConfigurationAPI.class);
			return sslClient.listSSLConfigurations(sID, hostID, sslConfigurationID, sslConfigurationIDOp, maxItems);
		});
	}

	/**
	 * Get details of an SSL configuration.
	 *
	 * @param hostID 				the ID of the host to which the SSL configuration belongs
	 * @param sslConfigurationID 	the SSL configuration ID
	 *
	 * @return						an {@linkplain SSLConfiguration} object with the SSL configuration details
	 */
	public DescribeSSLConfigurationResponse describeSSLConfiguration(Integer hostID, Integer sslConfigurationID) throws Exception {
		return withSession((sID) -> {
			IHostSSLConfigurationAPI sslClient = clientFor(IHostSSLConfigurationAPI.class);
			return sslClient.describeSSLConfiguration(sID, hostID, sslConfigurationID);
		});
	}

	/**
	 * Modify an SSL configuration.
	 *
	 * @param hostID 				the ID of the host to which the SSL configuration belongs
	 * @param sslConfigurationID 	the SSL configuration ID
	 * @param request 				the {@linkplain ModifySSLConfigurationRequest} containing the updated SSL configuration details
	 *
	 * @return						a {@linkplain ModifySSLConfigurationResponse} containing the updated SSL configuration
	 */
	public ModifySSLConfigurationResponse modifySSLConfiguration(Integer hostID, Integer sslConfigurationID, ModifySSLConfigurationRequest request) throws Exception {
		return withSession((sID) -> {
			IHostSSLConfigurationAPI sslClient = clientFor(IHostSSLConfigurationAPI.class);
			return sslClient.modifySSLConfiguration(sID, hostID, sslConfigurationID, request);
		});
	}

	/**
	 * Delete an SSL configuration.
	 *
	 * @param hostID 		the ID of the host to which the SSL configuration belongs
	 */
	public void deleteSSLConfiguration(Integer hostID, Integer sslConfigurationID) throws Exception {
		withSession((sID) -> {
			IHostSSLConfigurationAPI sslClient = clientFor(IHostSSLConfigurationAPI.class);
			sslClient.deleteSSLConfiguration(sID, hostID, sslConfigurationID);
			return null;
		});
	}

	/**
	 * List proxies.
	 *
	 * @return a {@link ListProxiesResponse} with the proxy details.
	 */
	public ListProxiesResponse listProxies() throws Exception {
		return listProxies(null, null, null);
	}

	/**
	 * List proxies.
	 *
	 * @param id (optional) used to define the starting point for the query. Combine with {@code op} to page through results.
	 * @param op (optional, required if {@code id} is specified) Currently supported operations are:
	 * 							<ul>
	 * 								<li><b>gt</b> (greater than)</li>
	 * 								<li><b>ge</b> (greater than or equal to)</li>
	 * 								<li><b>eq</b> (equal to)</li>
	 * 								<li><b>lt</b> (less than)</li>
	 * 								<li><b>le</b> (less than or equal to)</li>
	 * 							</ul>
	 * @param maxItems (optional) the number of items to retrieve. The maximum value for this parameter is controlled by the "Maximum number of items to retrieve from database" setting on the administrator account, which defaults to 5000.
	 *
	 * @return a {@link ListProxiesResponse} with the proxy details.
	 */
	public ListProxiesResponse listProxies(Integer id, String op, Integer maxItems) throws Exception {
		return withSession((s) -> {
			IProxyAPI proxyClient = clientFor(IProxyAPI.class);
			return proxyClient.listProxies(s, id, op, maxItems);
		});
	}

	/**
	 * Get a client for a specified interface class.
	 *
	 * @param interfaceClass the interface class to implement
	 *
	 * @return a client proxying the specified interface to the REST API.
	 */
	protected <T> T clientFor(Class<T> interfaceClass) {
		return withLock(targetLock, () -> {
			if (target == null) {
				target = getClient().target(baseUrl);
			}
			return target.proxy(interfaceClass);
		});
	}

	/**
	 * Perform an API function within a session.
	 * <p>
	 * A session is created if required; the session is left open in anticipation of future calls.
	 * </p>
	 *
	 * @param function the API function to perform; will be passed the session ID.
	 * @return the return value from the API function
	 */
	protected <R, E extends Exception> R withSession(DeepSecurityClient.ExceptionalFunction<String, R, E> function) throws Exception {
		try {
			return withLock(sessionIDLock, () -> {
				if (sessionID == null) {
					IAuthenticationAPI authClient = clientFor(IAuthenticationAPI.class);

					DSCredentials credentials = new DSCredentials();
					credentials.setUserName(username);
					credentials.setPassword(password);
					credentials.setTenantName(account);

					sessionID = authClient.login(credentials);
				}

				return function.apply(sessionID);
			});
		} catch (Exception e) {
			if (e.getCause() != null && e.getCause() instanceof ConnectException) {
				throw new ClientException(e.getCause().getMessage(), e);
			}

			throw e;
		}
	}

	/**
	 * Custom JSON provider with Deep Security Manager-specific serialization/deserialization settings.
	 */
	//	private ResteasyJackson2Provider getJSONProvider() {
	//		ObjectMapper mapper = new ObjectMapper();
	//		mapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
	//		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
	//		mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
	//		mapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, true);
	//		mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
	//		mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
	//		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	//		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	//
	//		mapper.setSerializationInclusion(Include.NON_NULL);
	//		mapper.registerModule(new JaxbAnnotationModule());
	//		
	//		ResteasyJackson2Provider provider = new ResteasyJackson2Provider();
	//		provider.setMapper(mapper);
	//
	//		return provider;
	//	}

	private JsonMapProvider getJsonMapprovider() {
		return new JsonMapProvider();
	}

	/**
	 * Get the client that we'll use to execute REST requests.
	 *
	 * You should never need to call this method; it's used by the {@link #clientFor(Class)} and {@link #close()} methods.
	 */
	private ResteasyClient getClient() {
		return withLock(clientLock, () -> {
			if (client == null) {
				client = clientBuilder.build();
			}
			return client;
		});
	}

	/**
	 * Perform an operation while holding a lock.
	 *
	 * @param l the lock instance to try to lock
	 * @param c the ExceptionalCallable to invoke when we have acquired the lock
	 *
	 * @return the result of the ExceptionalCallable.
	 *
	 * @throws E if the ExceptionalCallable throws an exception.
	 * @throws IllegalStateException if we can't acquire the lock in LOCK_TIMEOUT milliseconds.
	 */
	private <R, E extends Exception> R withLock(Lock l, ExceptionalCallable<R, E> c) throws E {
		try {
			if (l.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
				try {
					return c.call();
				} finally {
					l.unlock();
				}
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException("Could not obtain lock", e);
		}

		throw new IllegalStateException("Could not obtain lock");
	}

	/**
	 * Implementers are Function objects that throw some kind of exception.
	 *
	 * @param <T> the type of the input to the function
	 * @param <R> the type of the result of the function
	 * @param <E> the type of exception that may be thrown
	 *
	 * @see {@link Function}
	 */
	@FunctionalInterface
	protected static interface ExceptionalFunction<T, R, E extends Exception> {
		R apply(T t) throws E;
	}

	/**
	 * Implementers are Callable objects that throw some kind of exception.
	 *
	 * @param <R> the type of the result of the callable
	 * @param <E> the type of exception that may be thrown
	 *
	 * @see {@link Callable}
	 */
	@FunctionalInterface
	protected static interface ExceptionalCallable<R, E extends Exception> {
		R call() throws E;
	}

	/**
	 * Exception thrown when we have a problem connecting to the Deep Security Manager.
	 *
	 * Note: Not all exceptions get wrapped; we will extend the list as we encounter new
	 * cases that we can simplify for you.
	 */
	public static class ClientException extends Exception {
		private static final long serialVersionUID = 1L;

		public ClientException(String message, Throwable e) {
			super(message, e);
		}
	}

	/**
	 * Operator types for use when comparing values.
	 */
	public static enum Operator {
		GREATER_THAN("gt"),
		GREATER_THAN_OR_EQUAL_TO("ge"),
		LESS_THAN("lt"),
		LESS_THAN_OR_EQUAL_TO("le"),
		EQUALS("eq");

		public final String value;

		private Operator(String value) {
			this.value = value;
		}
	}

}