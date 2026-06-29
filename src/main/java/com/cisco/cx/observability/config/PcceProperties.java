package com.cisco.cx.observability.config;

import jakarta.validation.Valid;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pcce")
public class PcceProperties {

    @Valid
    private Queries queries = new Queries();

    @Valid
    private List<ComponentTarget> components = defaultComponents();

    @Valid
    private Security security = new Security();

    @Valid
    private Ssl ssl = new Ssl();

    @Valid
    private Audit audit = new Audit();

    @Valid
    private Operations operations = new Operations();

    @Valid
    private Performance performance = new Performance();

    @Valid
    private PcceApi pcceApi = new PcceApi();
    private CvpApi cvpApi = new CvpApi();

    @Valid
    private CucmAxl cucmAxl = new CucmAxl();

    @Valid
    private AgentProvisioning agentProvisioning = new AgentProvisioning();

    @Valid
    private Notifications notifications = new Notifications();

    @Valid
    private Grafana grafana = new Grafana();

    @Valid
    private Finesse finesse = new Finesse();

    @Valid
    private Jmx jmx = new Jmx();

    @Valid
    private AppDynamics appDynamics = new AppDynamics();

    @Valid
    private LiveData liveData = new LiveData();

    public Queries getQueries() {
        return queries;
    }

    public void setQueries(Queries queries) {
        this.queries = queries;
    }

    public List<ComponentTarget> getComponents() {
        return components;
    }

    public void setComponents(List<ComponentTarget> components) {
        this.components = components;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Ssl getSsl() {
        return ssl;
    }

    public void setSsl(Ssl ssl) {
        this.ssl = ssl;
    }

    public Audit getAudit() {
        return audit;
    }

    public void setAudit(Audit audit) {
        this.audit = audit;
    }

    public Operations getOperations() {
        return operations;
    }

    public void setOperations(Operations operations) {
        this.operations = operations;
    }

    public Performance getPerformance() {
        return performance;
    }

    public void setPerformance(Performance performance) {
        this.performance = performance;
    }

    public PcceApi getPcceApi() {
        return pcceApi;
    }

    public void setPcceApi(PcceApi pcceApi) {
        this.pcceApi = pcceApi;
    }

    public CvpApi getCvpApi() {
        return cvpApi;
    }

    public void setCvpApi(CvpApi cvpApi) {
        this.cvpApi = cvpApi;
    }

    public CucmAxl getCucmAxl() {
        return cucmAxl;
    }

    public void setCucmAxl(CucmAxl cucmAxl) {
        this.cucmAxl = cucmAxl;
    }

    public AgentProvisioning getAgentProvisioning() {
        return agentProvisioning;
    }

    public void setAgentProvisioning(AgentProvisioning agentProvisioning) {
        this.agentProvisioning = agentProvisioning;
    }

    public Notifications getNotifications() {
        return notifications;
    }

    public void setNotifications(Notifications notifications) {
        this.notifications = notifications;
    }

    public Grafana getGrafana() {
        return grafana;
    }

    public void setGrafana(Grafana grafana) {
        this.grafana = grafana;
    }

    public Finesse getFinesse() {
        return finesse;
    }

    public void setFinesse(Finesse finesse) {
        this.finesse = finesse;
    }

    public Jmx getJmx() {
        return jmx;
    }

    public void setJmx(Jmx jmx) {
        this.jmx = jmx;
    }

    public AppDynamics getAppDynamics() {
        return appDynamics;
    }

    public void setAppDynamics(AppDynamics appDynamics) {
        this.appDynamics = appDynamics;
    }

    public LiveData getLiveData() {
        return liveData;
    }

    public void setLiveData(LiveData liveData) {
        this.liveData = liveData;
    }

    private static List<ComponentTarget> defaultComponents() {
        List<ComponentTarget> targets = new ArrayList<>();
        for (ComponentName name : ComponentName.values()) {
            ComponentTarget target = new ComponentTarget();
            target.setName(name);
            target.setEnabled(false);
            targets.add(target);
        }
        return targets;
    }

    public static class Queries {
        private String callMetrics = DefaultSql.CALL_METRICS;
        private boolean callMetricsTcdFallbackEnabled = true;
        private String callMetricsTcdFallback = DefaultSql.CALL_METRICS_TCD_FALLBACK;
        private String agentStats = DefaultSql.AGENT_STATS;
        private String agentStatsTcd = DefaultSql.AGENT_STATS_TCD;
        private String callTypeMetrics = DefaultSql.CALL_TYPE_METRICS;
        private String droppedCalls = DefaultSql.DROPPED_CALLS;
        private boolean droppedCallsEnabled = true;
        private String dispositionBreakdown = DefaultSql.DISPOSITION_BREAKDOWN;
        private String ivrContainment = DefaultSql.IVR_CONTAINMENT;

        public String getCallMetrics() {
            return callMetrics;
        }

        public void setCallMetrics(String callMetrics) {
            this.callMetrics = callMetrics;
        }

        public boolean isCallMetricsTcdFallbackEnabled() {
            return callMetricsTcdFallbackEnabled;
        }

        public void setCallMetricsTcdFallbackEnabled(boolean callMetricsTcdFallbackEnabled) {
            this.callMetricsTcdFallbackEnabled = callMetricsTcdFallbackEnabled;
        }

        public String getCallMetricsTcdFallback() {
            return callMetricsTcdFallback;
        }

        public void setCallMetricsTcdFallback(String callMetricsTcdFallback) {
            this.callMetricsTcdFallback = callMetricsTcdFallback;
        }

        public String getAgentStats() {
            return agentStats;
        }

        public void setAgentStats(String agentStats) {
            this.agentStats = agentStats;
        }

        public String getAgentStatsTcd() {
            return agentStatsTcd;
        }

        public void setAgentStatsTcd(String agentStatsTcd) {
            this.agentStatsTcd = agentStatsTcd;
        }

        public String getCallTypeMetrics() {
            return callTypeMetrics;
        }

        public void setCallTypeMetrics(String callTypeMetrics) {
            this.callTypeMetrics = callTypeMetrics;
        }

        public String getDroppedCalls() {
            return droppedCalls;
        }

        public void setDroppedCalls(String droppedCalls) {
            this.droppedCalls = droppedCalls;
        }

        public boolean isDroppedCallsEnabled() {
            return droppedCallsEnabled;
        }

        public void setDroppedCallsEnabled(boolean droppedCallsEnabled) {
            this.droppedCallsEnabled = droppedCallsEnabled;
        }

        public String getDispositionBreakdown() {
            return dispositionBreakdown;
        }

        public void setDispositionBreakdown(String dispositionBreakdown) {
            this.dispositionBreakdown = dispositionBreakdown;
        }

        public String getIvrContainment() {
            return ivrContainment;
        }

        public void setIvrContainment(String ivrContainment) {
            this.ivrContainment = ivrContainment;
        }
    }

    public static class ComponentTarget {
        private ComponentName name;
        private String displayName;
        private String side;
        private String site;
        private String tier;
        private boolean enabled;
        private ProbeType probe = ProbeType.TCP;
        private String host;
        private int port;
        private String url;
        private Duration timeout = Duration.ofSeconds(3);
        private boolean trustAllCertificates;
        private int expectedStatusMin = 200;
        private int expectedStatusMax = 499;

        public ComponentName getName() {
            return name;
        }

        public void setName(ComponentName name) {
            this.name = name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getSide() {
            return side;
        }

        public void setSide(String side) {
            this.side = side;
        }

        public String getSite() {
            return site;
        }

        public void setSite(String site) {
            this.site = site;
        }

        public String getTier() {
            return tier;
        }

        public void setTier(String tier) {
            this.tier = tier;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public ProbeType getProbe() {
            return probe;
        }

        public void setProbe(ProbeType probe) {
            this.probe = probe;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public boolean isTrustAllCertificates() {
            return trustAllCertificates;
        }

        public void setTrustAllCertificates(boolean trustAllCertificates) {
            this.trustAllCertificates = trustAllCertificates;
        }

        public int getExpectedStatusMin() {
            return expectedStatusMin;
        }

        public void setExpectedStatusMin(int expectedStatusMin) {
            this.expectedStatusMin = expectedStatusMin;
        }

        public int getExpectedStatusMax() {
            return expectedStatusMax;
        }

        public void setExpectedStatusMax(int expectedStatusMax) {
            this.expectedStatusMax = expectedStatusMax;
        }
    }

    public enum ProbeType {
        HOST,
        TCP,
        HTTP,
        JDBC_AW,
        JDBC_HDS,
        JDBC_CVP_REPORTING
    }

    public enum ComponentName {
        ICM_Router,
        ICM_Logger,
        ICM_Distributor_AW,
        HDS,
        Live_Data,
        CVP_CallServer,
        CVP_ReportingServer,
        CVP_VXMLServer,
        CVP_OAMP,
        VVB,
        CTI_Server,
        PG_CUCM,
        PG_CVP,
        PG_ECE,
        VoIP_Gateway,
        SIP_Proxy,
        Load_Balancer,
        Finesse,
        MediaSense,
        CUIC,
        CUCM_Publisher,
        CUCM_Subscriber,
        IM_P,
        ECE,
        Eleveo_QM,
        Eleveo_Recording,
        Eleveo_WFM,
        Database_SQL,
        Domain_Controller,
        DNS,
        NTP
    }

    public enum AppRole {
        ADMIN,
        WORKFORCE_MANAGER,
        SUPERVISOR,
        AGENT,
        VIEWER
    }

    public enum Permission {
        CALL_METRICS_READ,
        AGENT_STATS_READ,
        DROPPED_CALLS_READ,
        IVR_METRICS_READ,
        COMPONENT_STATUS_READ,
        OPERATIONS_READ,
        SOLUTION_ADMIN
    }

    public enum AlertSeverity {
        INFO,
        WARNING,
        CRITICAL
    }

    public static class PcceApi {
        private boolean enabled;
        private String baseUrl;
        private String username;
        private String password;
        private ApiAuthMode authMode = ApiAuthMode.BASIC;
        private String bearerToken;
        private String apiKeyHeaderName;
        private String apiKey;
        private Duration timeout = Duration.ofSeconds(10);
        private boolean trustAllCertificates;
        private List<ApiMonitor> monitors = defaultApiMonitors();
        private List<ApiAction> actions = defaultApiActions();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public ApiAuthMode getAuthMode() {
            return authMode;
        }

        public void setAuthMode(ApiAuthMode authMode) {
            this.authMode = authMode;
        }

        public String getBearerToken() {
            return bearerToken;
        }

        public void setBearerToken(String bearerToken) {
            this.bearerToken = bearerToken;
        }

        public String getApiKeyHeaderName() {
            return apiKeyHeaderName;
        }

        public void setApiKeyHeaderName(String apiKeyHeaderName) {
            this.apiKeyHeaderName = apiKeyHeaderName;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public boolean isTrustAllCertificates() {
            return trustAllCertificates;
        }

        public void setTrustAllCertificates(boolean trustAllCertificates) {
            this.trustAllCertificates = trustAllCertificates;
        }

        public List<ApiMonitor> getMonitors() {
            return monitors;
        }

        public void setMonitors(List<ApiMonitor> monitors) {
            this.monitors = monitors;
        }

        public List<ApiAction> getActions() {
            return actions;
        }

        public void setActions(List<ApiAction> actions) {
            this.actions = actions;
        }

        private static List<ApiMonitor> defaultApiMonitors() {
            return List.of(
                    monitor("Users", "User Configuration and Management", "/unifiedconfig/config/user"),
                    monitor("Agent Teams", "Team Configuration and Management", "/unifiedconfig/config/agentteam"),
                    monitor("Skill Groups", "Skill Group Management", "/unifiedconfig/config/skillgroup"),
                    monitor("Dialed Numbers", "Call Configuration and Management", "/unifiedconfig/config/dialednumber"),
                    monitor("Call Types", "Call Configuration and Management", "/unifiedconfig/config/calltype"),
                    monitor("Machine Inventory", "System Configuration", "/unifiedconfig/config/machineinventory"),
                    monitor("Business Hours", "System Configuration", "/unifiedconfig/config/businesshour"));
        }

        private static ApiMonitor monitor(String name, String category, String path) {
            ApiMonitor monitor = new ApiMonitor();
            monitor.setName(name);
            monitor.setCategory(category);
            monitor.setMethod("GET");
            monitor.setPath(path);
            monitor.setExpectedStatusMax(499);
            monitor.setEnabled(true);
            return monitor;
        }

        private static List<ApiAction> defaultApiActions() {
            return List.of(
                    action("users.list", "User Configuration and Management", "List users", "GET", "/unifiedconfig/config/user", false),
                    action("teams.list", "Team Configuration and Management", "List agent teams", "GET", "/unifiedconfig/config/agentteam", false),
                    action("skills.list", "Skill Group Management", "List skill groups", "GET", "/unifiedconfig/config/skillgroup", false),
                    action("skill.create", "Skill Group Management", "Create skill group", "POST", "/unifiedconfig/config/skillgroup", true),
                    action("skill.update", "Skill Group Management", "Update skill group", "PUT", "/unifiedconfig/config/skillgroup/{id}", true),
                    action("agents.list", "Agents, Skills, Attributes, and Teams", "List agents", "GET", "/unifiedconfig/config/agent", false),
                    action("agent.get", "Agents, Skills, Attributes, and Teams", "Get agent by ID", "GET", "/unifiedconfig/config/agent/{id}", false),
                    action("agent.create", "Agents, Skills, Attributes, and Teams", "Create agent", "POST", "/unifiedconfig/config/agent", true),
                    action("agent.update", "Agents, Skills, Attributes, and Teams", "Update agent", "PUT", "/unifiedconfig/config/agent/{id}", true),
                    action("supervisor.create", "Agents, Skills, Attributes, and Teams", "Create supervisor", "POST", "/unifiedconfig/config/supervisor", true),
                    action("supervisor.update", "Agents, Skills, Attributes, and Teams", "Update supervisor", "PUT", "/unifiedconfig/config/supervisor/{id}", true),
                    action("deskSettings.list", "Agents, Skills, Attributes, and Teams", "List agent desk settings", "GET", "/unifiedconfig/config/agentdesksettings", false),
                    action("attributes.list", "Agents, Skills, Attributes, and Teams", "List attributes", "GET", "/unifiedconfig/config/attribute", false),
                    action("dialedNumbers.list", "Call Configuration and Management", "List dialed numbers", "GET", "/unifiedconfig/config/dialednumber", false),
                    action("callTypes.list", "Call Configuration and Management", "List call types", "GET", "/unifiedconfig/config/calltype", false),
                    action("labels.list", "Call Configuration and Management", "List labels", "GET", "/unifiedconfig/config/label", false),
                    action("eccVariables.list", "Call Configuration and Management", "List ECC variables", "GET", "/unifiedconfig/config/expandedcallvariable", false),
                    action("campaigns.list", "Outbound Option", "List campaigns", "GET", "/unifiedconfig/config/campaign", false),
                    action("dncImportRules.list", "Outbound Option", "List DNC import rules", "GET", "/unifiedconfig/config/dncimportrule", false),
                    action("businessHours.list", "System Configuration", "List business hours", "GET", "/unifiedconfig/config/businesshour", false),
                    action("capacityInfo.get", "System Configuration", "Get capacity info", "GET", "/unifiedconfig/config/capacityinfo", false),
                    action("deploymentType.get", "System Configuration", "Get deployment type", "GET", "/unifiedconfig/config/deploymenttype", false),
                    action("machineInventory.list", "System Configuration", "List machine inventory", "GET", "/unifiedconfig/config/machineinventory", false),
                    action("cvpReportingServer.get", "System Configuration", "Get CVP Reporting Server", "GET", "/unifiedconfig/config/cvpreportingserver", false),
                    action("businessHours.update", "System Configuration", "Update business hours", "PUT", "/unifiedconfig/config/businesshour/{id}", true),
                    action("user.update", "User Configuration and Management", "Update user", "PUT", "/unifiedconfig/config/user/{id}", true),
                    action("agentTeam.update", "Team Configuration and Management", "Update agent team", "PUT", "/unifiedconfig/config/agentteam/{id}", true));
        }

        private static ApiAction action(String id, String category, String name, String method, String path, boolean adminOnly) {
            ApiAction action = new ApiAction();
            action.setId(id);
            action.setCategory(category);
            action.setName(name);
            action.setMethod(method);
            action.setPath(path);
            action.setAdminOnly(adminOnly);
            action.setEnabled(!adminOnly);
            return action;
        }
    }

    public static class CvpApi extends PcceApi {
        public CvpApi() {
            setMonitors(List.of(
                    monitor("CVP Diagnostics", "Diagnostics and Health", "/cvp/diag"),
                    monitor("VXML Applications", "VXML Application Management", "/cvp/api/applications"),
                    monitor("Media Files", "Media File Management", "/cvp/api/media"),
                    monitor("Syslog Configuration", "Operations Configuration", "/cvp/api/syslog"),
                    monitor("SNMP Configuration", "Operations Configuration", "/cvp/api/snmp"),
                    monitor("VVB Services", "VVB and Voice Browser", "/cvp/api/vvb")));
            setActions(List.of(
                    action("cvp.diagnostics.get", "Diagnostics and Health", "Get CVP diagnostics", "GET", "/cvp/diag", false, true),
                    action("cvp.apps.list", "VXML Application Management", "List VXML applications", "GET", "/cvp/api/applications", false, true),
                    action("cvp.app.get", "VXML Application Management", "Get VXML application", "GET", "/cvp/api/applications/{name}", false, true),
                    action("cvp.app.deploy", "VXML Application Management", "Deploy or update VXML application", "POST", "/cvp/api/applications/{name}", true, false),
                    action("cvp.app.delete", "VXML Application Management", "Delete VXML application", "DELETE", "/cvp/api/applications/{name}", true, false),
                    action("cvp.media.list", "Media File Management", "List media files", "GET", "/cvp/api/media", false, true),
                    action("cvp.media.upload", "Media File Management", "Upload media metadata/file reference", "POST", "/cvp/api/media", true, false),
                    action("cvp.syslog.get", "Operations Configuration", "Get Syslog configuration", "GET", "/cvp/api/syslog", false, true),
                    action("cvp.syslog.update", "Operations Configuration", "Update Syslog configuration", "PUT", "/cvp/api/syslog", true, false),
                    action("cvp.snmp.get", "Operations Configuration", "Get SNMP configuration", "GET", "/cvp/api/snmp", false, true),
                    action("cvp.snmp.update", "Operations Configuration", "Update SNMP configuration", "PUT", "/cvp/api/snmp", true, false),
                    action("cvp.vvb.list", "VVB and Voice Browser", "List VVB services", "GET", "/cvp/api/vvb", false, true)));
        }

        private static ApiMonitor monitor(String name, String category, String path) {
            ApiMonitor monitor = new ApiMonitor();
            monitor.setName(name);
            monitor.setCategory(category);
            monitor.setMethod("GET");
            monitor.setPath(path);
            monitor.setExpectedStatusMax(499);
            monitor.setEnabled(true);
            return monitor;
        }

        private static ApiAction action(String id, String category, String name, String method, String path, boolean adminOnly, boolean enabled) {
            ApiAction action = new ApiAction();
            action.setId(id);
            action.setCategory(category);
            action.setName(name);
            action.setMethod(method);
            action.setPath(path);
            action.setAdminOnly(adminOnly);
            action.setEnabled(enabled);
            return action;
        }
    }

    public static class ApiAction {
        private String id;
        private String category;
        private String name;
        private boolean enabled;
        private boolean adminOnly;
        private String method = "GET";
        private String path;
        private String contentType = "application/json";

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isAdminOnly() {
            return adminOnly;
        }

        public void setAdminOnly(boolean adminOnly) {
            this.adminOnly = adminOnly;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }
    }

    public enum ApiAuthMode {
        NONE,
        BASIC,
        BEARER,
        API_KEY
    }

    public static class CucmAxl {
        private boolean enabled;
        private String host;
        private String username;
        private String password;
        private String version = "15.0";
        private Duration timeout = Duration.ofSeconds(15);
        private boolean trustAllCertificates;
        private int dnStart = 50000;
        private int dnEnd = 50999;
        private String devicePool;
        private String phoneTemplate;
        private String commonPhoneProfile;
        private String location = "Hub_None";
        private String securityProfile;
        private String sipProfile;
        private String routePartition;
        private String lineCss;
        private String serviceProfile;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public boolean isTrustAllCertificates() {
            return trustAllCertificates;
        }

        public void setTrustAllCertificates(boolean trustAllCertificates) {
            this.trustAllCertificates = trustAllCertificates;
        }

        public int getDnStart() {
            return dnStart;
        }

        public void setDnStart(int dnStart) {
            this.dnStart = dnStart;
        }

        public int getDnEnd() {
            return dnEnd;
        }

        public void setDnEnd(int dnEnd) {
            this.dnEnd = dnEnd;
        }

        public String getDevicePool() {
            return devicePool;
        }

        public void setDevicePool(String devicePool) {
            this.devicePool = devicePool;
        }

        public String getPhoneTemplate() {
            return phoneTemplate;
        }

        public void setPhoneTemplate(String phoneTemplate) {
            this.phoneTemplate = phoneTemplate;
        }

        public String getCommonPhoneProfile() {
            return commonPhoneProfile;
        }

        public void setCommonPhoneProfile(String commonPhoneProfile) {
            this.commonPhoneProfile = commonPhoneProfile;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getSecurityProfile() {
            return securityProfile;
        }

        public void setSecurityProfile(String securityProfile) {
            this.securityProfile = securityProfile;
        }

        public String getSipProfile() {
            return sipProfile;
        }

        public void setSipProfile(String sipProfile) {
            this.sipProfile = sipProfile;
        }

        public String getRoutePartition() {
            return routePartition;
        }

        public void setRoutePartition(String routePartition) {
            this.routePartition = routePartition;
        }

        public String getLineCss() {
            return lineCss;
        }

        public void setLineCss(String lineCss) {
            this.lineCss = lineCss;
        }

        public String getServiceProfile() {
            return serviceProfile;
        }

        public void setServiceProfile(String serviceProfile) {
            this.serviceProfile = serviceProfile;
        }
    }

    public static class AgentProvisioning {
        private boolean executionEnabled;
        private String identityDomain = "dev.mdi";
        private int resultsPerPage = 200;
        private boolean defaultDryRun = true;

        public boolean isExecutionEnabled() {
            return executionEnabled;
        }

        public void setExecutionEnabled(boolean executionEnabled) {
            this.executionEnabled = executionEnabled;
        }

        public String getIdentityDomain() {
            return identityDomain;
        }

        public void setIdentityDomain(String identityDomain) {
            this.identityDomain = identityDomain;
        }

        public int getResultsPerPage() {
            return resultsPerPage;
        }

        public void setResultsPerPage(int resultsPerPage) {
            this.resultsPerPage = resultsPerPage;
        }

        public boolean isDefaultDryRun() {
            return defaultDryRun;
        }

        public void setDefaultDryRun(boolean defaultDryRun) {
            this.defaultDryRun = defaultDryRun;
        }
    }

    public static class ApiMonitor {
        private String name;
        private String category;
        private boolean enabled;
        private String method = "GET";
        private String path;
        private int expectedStatusMin = 200;
        private int expectedStatusMax = 499;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getExpectedStatusMin() {
            return expectedStatusMin;
        }

        public void setExpectedStatusMin(int expectedStatusMin) {
            this.expectedStatusMin = expectedStatusMin;
        }

        public int getExpectedStatusMax() {
            return expectedStatusMax;
        }

        public void setExpectedStatusMax(int expectedStatusMax) {
            this.expectedStatusMax = expectedStatusMax;
        }
    }

    public static class Notifications {
        private boolean webhookEnabled;
        private String webhookUrl;
        private boolean smtpEnabled;
        private String smtpFrom = "cx-observability@localhost";
        private List<String> smtpRecipients = List.of();
        private String smtpSubjectPrefix = "[PCCE Observability]";
        private boolean smsEnabled;
        private String smsUrl;
        private String smsAuthorization;
        private String smsUserAgent = "D-Tech / v1.6";
        private List<String> smsRecipients = List.of();
        private AlertSeverity smsMinimumSeverity = AlertSeverity.CRITICAL;
        private int smsMaxAlertsPerAssessment = 5;
        private AlertSeverity minimumSeverity = AlertSeverity.CRITICAL;
        private Duration timeout = Duration.ofSeconds(10);

        public boolean isWebhookEnabled() {
            return webhookEnabled;
        }

        public void setWebhookEnabled(boolean webhookEnabled) {
            this.webhookEnabled = webhookEnabled;
        }

        public String getWebhookUrl() {
            return webhookUrl;
        }

        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }

        public boolean isSmtpEnabled() {
            return smtpEnabled;
        }

        public void setSmtpEnabled(boolean smtpEnabled) {
            this.smtpEnabled = smtpEnabled;
        }

        public String getSmtpFrom() {
            return smtpFrom;
        }

        public void setSmtpFrom(String smtpFrom) {
            this.smtpFrom = smtpFrom;
        }

        public List<String> getSmtpRecipients() {
            return smtpRecipients;
        }

        public void setSmtpRecipients(List<String> smtpRecipients) {
            this.smtpRecipients = smtpRecipients;
        }

        public String getSmtpSubjectPrefix() {
            return smtpSubjectPrefix;
        }

        public void setSmtpSubjectPrefix(String smtpSubjectPrefix) {
            this.smtpSubjectPrefix = smtpSubjectPrefix;
        }

        public boolean isSmsEnabled() {
            return smsEnabled;
        }

        public void setSmsEnabled(boolean smsEnabled) {
            this.smsEnabled = smsEnabled;
        }

        public String getSmsUrl() {
            return smsUrl;
        }

        public void setSmsUrl(String smsUrl) {
            this.smsUrl = smsUrl;
        }

        public String getSmsAuthorization() {
            return smsAuthorization;
        }

        public void setSmsAuthorization(String smsAuthorization) {
            this.smsAuthorization = smsAuthorization;
        }

        public String getSmsUserAgent() {
            return smsUserAgent;
        }

        public void setSmsUserAgent(String smsUserAgent) {
            this.smsUserAgent = smsUserAgent;
        }

        public List<String> getSmsRecipients() {
            return smsRecipients;
        }

        public void setSmsRecipients(List<String> smsRecipients) {
            this.smsRecipients = smsRecipients;
        }

        public AlertSeverity getSmsMinimumSeverity() {
            return smsMinimumSeverity;
        }

        public void setSmsMinimumSeverity(AlertSeverity smsMinimumSeverity) {
            this.smsMinimumSeverity = smsMinimumSeverity;
        }

        public int getSmsMaxAlertsPerAssessment() {
            return smsMaxAlertsPerAssessment;
        }

        public void setSmsMaxAlertsPerAssessment(int smsMaxAlertsPerAssessment) {
            this.smsMaxAlertsPerAssessment = smsMaxAlertsPerAssessment;
        }

        public AlertSeverity getMinimumSeverity() {
            return minimumSeverity;
        }

        public void setMinimumSeverity(AlertSeverity minimumSeverity) {
            this.minimumSeverity = minimumSeverity;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }

    public static class Grafana {
        private boolean enabled;
        private List<GrafanaDashboard> dashboards = List.of();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<GrafanaDashboard> getDashboards() {
            return dashboards;
        }

        public void setDashboards(List<GrafanaDashboard> dashboards) {
            this.dashboards = dashboards;
        }
    }

    public static class GrafanaDashboard {
        private String name;
        private String area;
        private String url;
        private boolean enabled = true;
        private String description;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArea() {
            return area;
        }

        public void setArea(String area) {
            this.area = area;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class Finesse {
        private boolean enabled;
        private String baseUrl;
        private String username;
        private String password;
        private Duration timeout = Duration.ofSeconds(10);
        private boolean trustAllCertificates;
        private List<String> userIds = List.of();
        private List<String> teamIds = List.of();
        private List<String> queueIds = List.of();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public boolean isTrustAllCertificates() {
            return trustAllCertificates;
        }

        public void setTrustAllCertificates(boolean trustAllCertificates) {
            this.trustAllCertificates = trustAllCertificates;
        }

        public List<String> getUserIds() {
            return userIds;
        }

        public void setUserIds(List<String> userIds) {
            this.userIds = userIds;
        }

        public List<String> getTeamIds() {
            return teamIds;
        }

        public void setTeamIds(List<String> teamIds) {
            this.teamIds = teamIds;
        }

        public List<String> getQueueIds() {
            return queueIds;
        }

        public void setQueueIds(List<String> queueIds) {
            this.queueIds = queueIds;
        }
    }

    public static class Jmx {
        private boolean enabled;
        private String username;
        private String password;
        private Duration timeout = Duration.ofSeconds(10);
        private boolean trustStoreConfigured;
        private List<JmxTarget> targets = List.of();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public boolean isTrustStoreConfigured() {
            return trustStoreConfigured;
        }

        public void setTrustStoreConfigured(boolean trustStoreConfigured) {
            this.trustStoreConfigured = trustStoreConfigured;
        }

        public List<JmxTarget> getTargets() {
            return targets;
        }

        public void setTargets(List<JmxTarget> targets) {
            this.targets = targets;
        }
    }

    public static class JmxTarget {
        private String name;
        private String component;
        private String serviceUrl;
        private boolean enabled = true;
        private List<String> mbeans = List.of(
                "java.lang:type=Memory",
                "java.lang:type=Threading",
                "java.lang:type=OperatingSystem");

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getComponent() {
            return component;
        }

        public void setComponent(String component) {
            this.component = component;
        }

        public String getServiceUrl() {
            return serviceUrl;
        }

        public void setServiceUrl(String serviceUrl) {
            this.serviceUrl = serviceUrl;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getMbeans() {
            return mbeans;
        }

        public void setMbeans(List<String> mbeans) {
            this.mbeans = mbeans;
        }
    }

    public static class AppDynamics {
        private boolean enabled;
        private String controllerUrl;
        private String accountName;
        private String username;
        private String password;
        private String applicationName;
        private Duration timeout = Duration.ofSeconds(10);
        private boolean trustAllCertificates;
        private List<GrafanaDashboard> dashboards = List.of();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getControllerUrl() {
            return controllerUrl;
        }

        public void setControllerUrl(String controllerUrl) {
            this.controllerUrl = controllerUrl;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getApplicationName() {
            return applicationName;
        }

        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public boolean isTrustAllCertificates() {
            return trustAllCertificates;
        }

        public void setTrustAllCertificates(boolean trustAllCertificates) {
            this.trustAllCertificates = trustAllCertificates;
        }

        public List<GrafanaDashboard> getDashboards() {
            return dashboards;
        }

        public void setDashboards(List<GrafanaDashboard> dashboards) {
            this.dashboards = dashboards;
        }
    }

    public static class LiveData {
        private boolean enabled;
        private String host;
        private int port = 443;
        private int websocketPort = 443;
        private String tokenPath = "livedata/token/new";
        private String username;
        private String password;
        private Duration timeout = Duration.ofSeconds(10);
        private boolean trustAllCertificates;
        private List<String> stockReports = List.of(
                "Agent Real Time",
                "Agent Skill Group Real Time",
                "Call Type Real Time",
                "Skill Group Real Time");

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getWebsocketPort() {
            return websocketPort;
        }

        public void setWebsocketPort(int websocketPort) {
            this.websocketPort = websocketPort;
        }

        public String getTokenPath() {
            return tokenPath;
        }

        public void setTokenPath(String tokenPath) {
            this.tokenPath = tokenPath;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public boolean isTrustAllCertificates() {
            return trustAllCertificates;
        }

        public void setTrustAllCertificates(boolean trustAllCertificates) {
            this.trustAllCertificates = trustAllCertificates;
        }

        public List<String> getStockReports() {
            return stockReports;
        }

        public void setStockReports(List<String> stockReports) {
            this.stockReports = stockReports;
        }
    }

    public static class Operations {
        private boolean scheduledAssessmentEnabled = true;
        private long assessmentFixedDelayMs = 300000;
        private int componentLatencyWarningMs = 1500;
        private int componentLatencyCriticalMs = 5000;
        private int droppedCallsWarningThreshold = 10;
        private int droppedCallsCriticalThreshold = 25;
        private double serviceLevelWarningPct = 80.0;
        private double serviceLevelCriticalPct = 70.0;
        private boolean maintenanceMode;
        private String maintenanceReason;
        private int auditRetention = 500;

        public boolean isScheduledAssessmentEnabled() {
            return scheduledAssessmentEnabled;
        }

        public void setScheduledAssessmentEnabled(boolean scheduledAssessmentEnabled) {
            this.scheduledAssessmentEnabled = scheduledAssessmentEnabled;
        }

        public long getAssessmentFixedDelayMs() {
            return assessmentFixedDelayMs;
        }

        public void setAssessmentFixedDelayMs(long assessmentFixedDelayMs) {
            this.assessmentFixedDelayMs = assessmentFixedDelayMs;
        }

        public int getComponentLatencyWarningMs() {
            return componentLatencyWarningMs;
        }

        public void setComponentLatencyWarningMs(int componentLatencyWarningMs) {
            this.componentLatencyWarningMs = componentLatencyWarningMs;
        }

        public int getComponentLatencyCriticalMs() {
            return componentLatencyCriticalMs;
        }

        public void setComponentLatencyCriticalMs(int componentLatencyCriticalMs) {
            this.componentLatencyCriticalMs = componentLatencyCriticalMs;
        }

        public int getDroppedCallsWarningThreshold() {
            return droppedCallsWarningThreshold;
        }

        public void setDroppedCallsWarningThreshold(int droppedCallsWarningThreshold) {
            this.droppedCallsWarningThreshold = droppedCallsWarningThreshold;
        }

        public int getDroppedCallsCriticalThreshold() {
            return droppedCallsCriticalThreshold;
        }

        public void setDroppedCallsCriticalThreshold(int droppedCallsCriticalThreshold) {
            this.droppedCallsCriticalThreshold = droppedCallsCriticalThreshold;
        }

        public double getServiceLevelWarningPct() {
            return serviceLevelWarningPct;
        }

        public void setServiceLevelWarningPct(double serviceLevelWarningPct) {
            this.serviceLevelWarningPct = serviceLevelWarningPct;
        }

        public double getServiceLevelCriticalPct() {
            return serviceLevelCriticalPct;
        }

        public void setServiceLevelCriticalPct(double serviceLevelCriticalPct) {
            this.serviceLevelCriticalPct = serviceLevelCriticalPct;
        }

        public boolean isMaintenanceMode() {
            return maintenanceMode;
        }

        public void setMaintenanceMode(boolean maintenanceMode) {
            this.maintenanceMode = maintenanceMode;
        }

        public String getMaintenanceReason() {
            return maintenanceReason;
        }

        public void setMaintenanceReason(String maintenanceReason) {
            this.maintenanceReason = maintenanceReason;
        }

        public int getAuditRetention() {
            return auditRetention;
        }

        public void setAuditRetention(int auditRetention) {
            this.auditRetention = auditRetention;
        }
    }

    public static class Performance {
        private int jdbcQueryTimeoutSeconds = 30;
        private long slowQueryWarningMs = 2000;
        private long slowRequestWarningMs = 3000;

        public int getJdbcQueryTimeoutSeconds() {
            return jdbcQueryTimeoutSeconds;
        }

        public void setJdbcQueryTimeoutSeconds(int jdbcQueryTimeoutSeconds) {
            this.jdbcQueryTimeoutSeconds = jdbcQueryTimeoutSeconds;
        }

        public long getSlowQueryWarningMs() {
            return slowQueryWarningMs;
        }

        public void setSlowQueryWarningMs(long slowQueryWarningMs) {
            this.slowQueryWarningMs = slowQueryWarningMs;
        }

        public long getSlowRequestWarningMs() {
            return slowRequestWarningMs;
        }

        public void setSlowRequestWarningMs(long slowRequestWarningMs) {
            this.slowRequestWarningMs = slowRequestWarningMs;
        }
    }

    public static class Ssl {
        private String trustStorePath;
        private String trustStorePassword = "changeit";
        private boolean verifyHostname = true;

        public String getTrustStorePath() {
            return trustStorePath;
        }

        public void setTrustStorePath(String trustStorePath) {
            this.trustStorePath = trustStorePath;
        }

        public String getTrustStorePassword() {
            return trustStorePassword;
        }

        public void setTrustStorePassword(String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
        }

        public boolean isVerifyHostname() {
            return verifyHostname;
        }

        public void setVerifyHostname(boolean verifyHostname) {
            this.verifyHostname = verifyHostname;
        }
    }

    public static class Audit {
        private int retentionDays = 90;

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }
    }

    public static class Security {
        private List<AppUser> users = defaultUsers();
        private Map<AppRole, List<Permission>> rolePermissions = defaultRolePermissions();
        private Ldap ldap = new Ldap();
        private RateLimit rateLimit = new RateLimit();

        public List<AppUser> getUsers() {
            return users;
        }

        public void setUsers(List<AppUser> users) {
            this.users = users;
        }

        public Map<AppRole, List<Permission>> getRolePermissions() {
            return rolePermissions;
        }

        public void setRolePermissions(Map<AppRole, List<Permission>> rolePermissions) {
            this.rolePermissions = rolePermissions;
        }

        public Ldap getLdap() {
            return ldap;
        }

        public void setLdap(Ldap ldap) {
            this.ldap = ldap;
        }

        public RateLimit getRateLimit() {
            return rateLimit;
        }

        public void setRateLimit(RateLimit rateLimit) {
            this.rateLimit = rateLimit;
        }

        public static class RateLimit {
            private boolean enabled = true;
            private int maxRequestsPerMinute = 120;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public int getMaxRequestsPerMinute() {
                return maxRequestsPerMinute;
            }

            public void setMaxRequestsPerMinute(int maxRequestsPerMinute) {
                this.maxRequestsPerMinute = maxRequestsPerMinute;
            }
        }

        private static List<AppUser> defaultUsers() {
            AppUser admin = new AppUser();
            admin.setUsername("admin");
            admin.setPassword("{noop}change-me");
            admin.setDisplayName("Solution Administrator");
            admin.setRoles(List.of(AppRole.ADMIN));
            admin.setEnabled(true);
            return List.of(admin);
        }

        private static Map<AppRole, List<Permission>> defaultRolePermissions() {
            Map<AppRole, List<Permission>> permissions = new EnumMap<>(AppRole.class);
            permissions.put(AppRole.ADMIN, List.of(Permission.values()));
            permissions.put(AppRole.WORKFORCE_MANAGER, List.of(
                    Permission.CALL_METRICS_READ,
                    Permission.AGENT_STATS_READ,
                    Permission.DROPPED_CALLS_READ,
                    Permission.IVR_METRICS_READ,
                    Permission.COMPONENT_STATUS_READ,
                    Permission.OPERATIONS_READ));
            permissions.put(AppRole.SUPERVISOR, List.of(
                    Permission.CALL_METRICS_READ,
                    Permission.AGENT_STATS_READ,
                    Permission.DROPPED_CALLS_READ,
                    Permission.COMPONENT_STATUS_READ,
                    Permission.OPERATIONS_READ));
            permissions.put(AppRole.AGENT, List.of(Permission.AGENT_STATS_READ));
            permissions.put(AppRole.VIEWER, List.of(
                    Permission.CALL_METRICS_READ,
                    Permission.DROPPED_CALLS_READ,
                    Permission.IVR_METRICS_READ,
                    Permission.COMPONENT_STATUS_READ,
                    Permission.OPERATIONS_READ));
            return permissions;
        }
    }

    public static class Ldap {
        private boolean enabled;
        private String userSearchBase;
        private String userSearchFilter = "(sAMAccountName={0})";
        private String groupSearchBase;
        private String adminGroup;
        private String workforceManagerGroup;
        private String supervisorGroup;
        private String viewerGroup;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUserSearchBase() {
            return userSearchBase;
        }

        public void setUserSearchBase(String userSearchBase) {
            this.userSearchBase = userSearchBase;
        }

        public String getUserSearchFilter() {
            return userSearchFilter;
        }

        public void setUserSearchFilter(String userSearchFilter) {
            this.userSearchFilter = userSearchFilter;
        }

        public String getGroupSearchBase() {
            return groupSearchBase;
        }

        public void setGroupSearchBase(String groupSearchBase) {
            this.groupSearchBase = groupSearchBase;
        }

        public String getAdminGroup() {
            return adminGroup;
        }

        public void setAdminGroup(String adminGroup) {
            this.adminGroup = adminGroup;
        }

        public String getWorkforceManagerGroup() {
            return workforceManagerGroup;
        }

        public void setWorkforceManagerGroup(String workforceManagerGroup) {
            this.workforceManagerGroup = workforceManagerGroup;
        }

        public String getSupervisorGroup() {
            return supervisorGroup;
        }

        public void setSupervisorGroup(String supervisorGroup) {
            this.supervisorGroup = supervisorGroup;
        }

        public String getViewerGroup() {
            return viewerGroup;
        }

        public void setViewerGroup(String viewerGroup) {
            this.viewerGroup = viewerGroup;
        }
    }

    public static class AppUser {
        private String username;
        private String password;
        private String displayName;
        private String agentId;
        private boolean enabled = true;
        private List<AppRole> roles = List.of(AppRole.VIEWER);
        private List<String> allowedTeams = List.of();
        private List<Permission> extraPermissions = List.of();

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<AppRole> getRoles() {
            return roles;
        }

        public void setRoles(List<AppRole> roles) {
            this.roles = roles;
        }

        public List<String> getAllowedTeams() {
            return allowedTeams;
        }

        public void setAllowedTeams(List<String> allowedTeams) {
            this.allowedTeams = allowedTeams;
        }

        public List<Permission> getExtraPermissions() {
            return extraPermissions;
        }

        public void setExtraPermissions(List<Permission> extraPermissions) {
            this.extraPermissions = extraPermissions;
        }
    }

    public static class DefaultSql {
        static final String CALL_METRICS = """
                SELECT
                    CAST(sgi.DateTime AS date) AS [date],
                    DATEPART(hour, sgi.DateTime) AS [hour],
                    sg.EnterpriseName AS skill_group,
                    SUM(COALESCE(sgi.CallsOffered, 0)) AS calls_offered,
                    SUM(COALESCE(sgi.CallsHandled, 0)) AS calls_handled,
                    SUM(COALESCE(sgi.AbandonRingCalls, 0) + COALESCE(sgi.AbandonHoldCalls, 0)) AS calls_abandoned,
                    CAST(100.0 * SUM(COALESCE(sgi.ServiceLevelCalls, 0))
                        / NULLIF(SUM(COALESCE(sgi.ServiceLevelCallsOffered, 0)), 0) AS decimal(9,2)) AS service_level_pct,
                    CAST(SUM(COALESCE(sgi.HandledCallsTalkTime, 0) + COALESCE(sgi.WorkReadyTime, 0))
                        / NULLIF(SUM(COALESCE(sgi.CallsHandled, 0)), 0) AS decimal(18,2)) AS avg_handle_time,
                    CAST(SUM(COALESCE(sgi.HandledCallsTalkTime, 0)) / NULLIF(SUM(COALESCE(sgi.CallsHandled, 0)), 0) AS decimal(18,2)) AS avg_talk_time,
                    CAST(0 AS decimal(18,2)) AS avg_hold_time,
                    CAST(SUM(COALESCE(sgi.WorkReadyTime, 0)) / NULLIF(SUM(COALESCE(sgi.CallsHandled, 0)), 0) AS decimal(18,2)) AS avg_wrap_time,
                    CAST(SUM(COALESCE(sgi.AnswerWaitTime, 0)) / NULLIF(SUM(COALESCE(sgi.CallsHandled, 0)), 0) AS decimal(18,2)) AS avg_speed_answer,
                    CAST(SUM(COALESCE(sgi.AnswerWaitTime, 0)) / NULLIF(SUM(COALESCE(sgi.CallsOffered, 0)), 0) AS decimal(18,2)) AS avg_queue_time,
                    CAST(0 AS decimal(18,2)) AS max_queue_time,
                    CAST(NULL AS decimal(9,2)) AS transfer_rate,
                    CAST(NULL AS decimal(9,2)) AS first_call_resolution,
                    CAST(NULL AS decimal(9,2)) AS ivr_containment_rate,
                    CAST(NULL AS decimal(9,2)) AS csat_score
                FROM t_Skill_Group_Interval sgi
                JOIN t_Skill_Group sg ON sg.SkillTargetID = sgi.SkillTargetID
                WHERE sgi.DateTime >= ? AND sgi.DateTime < ?
                  AND (? IS NULL OR sg.EnterpriseName = ? OR CAST(sg.SkillTargetID AS varchar(50)) = ?)
                GROUP BY CAST(sgi.DateTime AS date), DATEPART(hour, sgi.DateTime), sg.EnterpriseName
                ORDER BY [date], [hour], skill_group
                """;

        static final String CALL_METRICS_TCD_FALLBACK = """
                SELECT
                    CAST(tcd.DateTime AS date) AS [date],
                    DATEPART(hour, tcd.DateTime) AS [hour],
                    COALESCE(sg.EnterpriseName, 'SkillTarget ' + CAST(tcd.SkillGroupSkillTargetID AS varchar(50)), 'UNMAPPED') AS skill_group,
                    COUNT_BIG(*) AS calls_offered,
                    SUM(CASE WHEN COALESCE(tcd.AgentSkillTargetID, 0) > 0 THEN 1 ELSE 0 END) AS calls_handled,
                    SUM(CASE WHEN COALESCE(tcd.AgentSkillTargetID, 0) = 0 THEN 1 ELSE 0 END) AS calls_abandoned,
                    CAST(NULL AS decimal(9,2)) AS service_level_pct,
                    CAST(NULL AS decimal(18,2)) AS avg_handle_time,
                    CAST(NULL AS decimal(18,2)) AS avg_talk_time,
                    CAST(NULL AS decimal(18,2)) AS avg_hold_time,
                    CAST(NULL AS decimal(18,2)) AS avg_wrap_time,
                    CAST(NULL AS decimal(18,2)) AS avg_speed_answer,
                    CAST(NULL AS decimal(18,2)) AS avg_queue_time,
                    CAST(NULL AS decimal(18,2)) AS max_queue_time,
                    CAST(NULL AS decimal(9,2)) AS transfer_rate,
                    CAST(NULL AS decimal(9,2)) AS first_call_resolution,
                    CAST(NULL AS decimal(9,2)) AS ivr_containment_rate,
                    CAST(NULL AS decimal(9,2)) AS csat_score
                FROM t_Termination_Call_Detail tcd
                LEFT JOIN t_Skill_Group sg ON sg.SkillTargetID = tcd.SkillGroupSkillTargetID
                WHERE tcd.DateTime >= ?
                  AND tcd.DateTime < ?
                  AND (? IS NULL OR sg.EnterpriseName = ? OR CAST(tcd.SkillGroupSkillTargetID AS varchar(50)) = ?)
                GROUP BY
                    CAST(tcd.DateTime AS date),
                    DATEPART(hour, tcd.DateTime),
                    COALESCE(sg.EnterpriseName, 'SkillTarget ' + CAST(tcd.SkillGroupSkillTargetID AS varchar(50)), 'UNMAPPED')
                ORDER BY [date], [hour], skill_group
                """;

        static final String AGENT_STATS = """
                SELECT
                    CAST(? AS date) AS [date],
                    p.FirstName + ' ' + p.LastName AS agent_name,
                    p.LoginName AS agent_id,
                    at.EnterpriseName AS team,
                    CAST(NULL AS varchar(255)) AS skill_group,
                    'offline' AS status,
                    CAST(0 AS bigint) AS calls_handled,
                    CAST(0 AS decimal(18,2)) AS avg_handle_time,
                    CAST(0 AS decimal(18,2)) AS avg_talk_time,
                    CAST(0 AS decimal(18,2)) AS avg_hold_time,
                    CAST(0 AS decimal(18,2)) AS avg_wrap_time,
                    CAST(NULL AS decimal(9,2)) AS occupancy_pct,
                    CAST(NULL AS decimal(9,2)) AS adherence_pct,
                    CAST(0 AS bigint) AS transfers,
                    CAST(0 AS decimal(18,2)) AS login_duration_min,
                    CAST(0 AS decimal(18,2)) AS not_ready_time_min
                FROM t_Agent a
                JOIN t_Person p ON p.PersonID = a.PersonID
                LEFT JOIN t_Agent_Team_Member atm ON atm.SkillTargetID = a.SkillTargetID
                LEFT JOIN t_Agent_Team at ON at.AgentTeamID = atm.AgentTeamID
                WHERE (? IS NULL OR p.LoginName = ? OR CAST(a.SkillTargetID AS varchar(50)) = ?)
                  AND (? IS NULL OR at.EnterpriseName = ?)
                ORDER BY [date], agent_name
                """;

        static final String AGENT_STATS_TCD = """
                SELECT
                    CAST(tcd.DateTime AS date) AS [date],
                    COALESCE(p.FirstName + ' ' + p.LastName, 'Agent ' + CAST(tcd.AgentSkillTargetID AS varchar(50))) AS agent_name,
                    COALESCE(p.LoginName, CAST(tcd.AgentSkillTargetID AS varchar(50))) AS agent_id,
                    at.EnterpriseName AS team,
                    COALESCE(sg.EnterpriseName, 'SkillTarget ' + CAST(tcd.SkillGroupSkillTargetID AS varchar(50)), 'UNMAPPED') AS skill_group,
                    'offline' AS status,
                    COUNT_BIG(*) AS calls_handled,
                    CAST(NULL AS decimal(18,2)) AS avg_handle_time,
                    CAST(NULL AS decimal(18,2)) AS avg_talk_time,
                    CAST(NULL AS decimal(18,2)) AS avg_hold_time,
                    CAST(NULL AS decimal(18,2)) AS avg_wrap_time,
                    CAST(NULL AS decimal(9,2)) AS occupancy_pct,
                    CAST(NULL AS decimal(9,2)) AS adherence_pct,
                    CAST(0 AS bigint) AS transfers,
                    CAST(NULL AS decimal(18,2)) AS login_duration_min,
                    CAST(NULL AS decimal(18,2)) AS not_ready_time_min
                FROM t_Termination_Call_Detail tcd
                LEFT JOIN t_Agent a ON a.SkillTargetID = tcd.AgentSkillTargetID
                LEFT JOIN t_Person p ON p.PersonID = a.PersonID
                LEFT JOIN t_Agent_Team_Member atm ON atm.SkillTargetID = a.SkillTargetID
                LEFT JOIN t_Agent_Team at ON at.AgentTeamID = atm.AgentTeamID
                LEFT JOIN t_Skill_Group sg ON sg.SkillTargetID = tcd.SkillGroupSkillTargetID
                WHERE tcd.DateTime >= ? AND tcd.DateTime < ?
                  AND COALESCE(tcd.AgentSkillTargetID, 0) > 0
                  AND (? IS NULL OR p.LoginName = ? OR CAST(tcd.AgentSkillTargetID AS varchar(50)) = ?)
                  AND (? IS NULL OR at.EnterpriseName = ?)
                GROUP BY
                    CAST(tcd.DateTime AS date),
                    COALESCE(p.FirstName + ' ' + p.LastName, 'Agent ' + CAST(tcd.AgentSkillTargetID AS varchar(50))),
                    COALESCE(p.LoginName, CAST(tcd.AgentSkillTargetID AS varchar(50))),
                    at.EnterpriseName,
                    COALESCE(sg.EnterpriseName, 'SkillTarget ' + CAST(tcd.SkillGroupSkillTargetID AS varchar(50)), 'UNMAPPED')
                ORDER BY [date], agent_name
                """;

        static final String CALL_TYPE_METRICS = """
                SELECT
                    CAST(tcd.DateTime AS date) AS [date],
                    DATEPART(hour, tcd.DateTime) AS [hour],
                    COALESCE(ct.EnterpriseName, 'CallType ' + CAST(tcd.CallTypeID AS varchar(50)), 'UNMAPPED') AS call_type,
                    COALESCE(sg.EnterpriseName, 'SkillTarget ' + CAST(tcd.SkillGroupSkillTargetID AS varchar(50)), 'UNMAPPED') AS skill_group,
                    COUNT_BIG(*) AS calls,
                    SUM(CASE WHEN COALESCE(tcd.AgentSkillTargetID, 0) > 0 THEN 1 ELSE 0 END) AS handled_calls
                FROM t_Termination_Call_Detail tcd
                LEFT JOIN t_Call_Type ct ON ct.CallTypeID = tcd.CallTypeID
                LEFT JOIN t_Skill_Group sg ON sg.SkillTargetID = tcd.SkillGroupSkillTargetID
                WHERE tcd.DateTime >= ? AND tcd.DateTime < ?
                  AND (? IS NULL OR ct.EnterpriseName = ? OR CAST(tcd.CallTypeID AS varchar(50)) = ?)
                  AND (? IS NULL OR sg.EnterpriseName = ? OR CAST(tcd.SkillGroupSkillTargetID AS varchar(50)) = ?)
                GROUP BY
                    CAST(tcd.DateTime AS date),
                    DATEPART(hour, tcd.DateTime),
                    COALESCE(ct.EnterpriseName, 'CallType ' + CAST(tcd.CallTypeID AS varchar(50)), 'UNMAPPED'),
                    COALESCE(sg.EnterpriseName, 'SkillTarget ' + CAST(tcd.SkillGroupSkillTargetID AS varchar(50)), 'UNMAPPED')
                ORDER BY [date], [hour], call_type, skill_group
                """;

        static final String DROPPED_CALLS = """
                SELECT
                    CAST(tcd.DateTime AS date) AS [date],
                    DATEPART(hour, tcd.DateTime) AS [hour],
                    COALESCE(sg.EnterpriseName, 'SkillTarget ' + CAST(tcd.SkillGroupSkillTargetID AS varchar(50)), 'UNMAPPED') AS skill_group,
                    COUNT_BIG(*) AS dropped_calls,
                    MAX(tcd.DateTime) AS last_drop_time
                FROM t_Termination_Call_Detail tcd
                LEFT JOIN t_Skill_Group sg ON sg.SkillTargetID = tcd.SkillGroupSkillTargetID
                WHERE tcd.DateTime >= ?
                  AND tcd.DateTime < ?
                  AND COALESCE(tcd.CallDisposition, 0) IN (1, 2, 3, 4, 5, 6, 7, 10, 19, 26, 27, 35, 36, 37, 39, 40, 41, 42, 52)
                  AND (? IS NULL OR sg.EnterpriseName = ? OR CAST(sg.SkillTargetID AS varchar(50)) = ?)
                GROUP BY CAST(tcd.DateTime AS date), DATEPART(hour, tcd.DateTime), COALESCE(sg.EnterpriseName, 'SkillTarget ' + CAST(tcd.SkillGroupSkillTargetID AS varchar(50)), 'UNMAPPED')
                ORDER BY [date], [hour], skill_group
                """;

        static final String DISPOSITION_BREAKDOWN = """
                SELECT
                    tcd.CallDisposition AS disposition_code,
                    COUNT_BIG(*) AS calls
                FROM t_Termination_Call_Detail tcd
                WHERE tcd.DateTime >= ? AND tcd.DateTime < ?
                GROUP BY tcd.CallDisposition
                ORDER BY calls DESC
                """;

        static final String IVR_CONTAINMENT = """
                SELECT
                    DATE(c.startdatetime) AS call_date,
                    CAST(TO_CHAR(c.startdatetime, '%H') AS INTEGER) AS call_hour,
                    CASE
                        WHEN COUNT(*) = 0 THEN 0
                        ELSE CAST(100.0 * SUM(CASE WHEN vs.causeid NOT IN (2, 18, 1001, 1044) THEN 1 ELSE 0 END) / COUNT(*) AS DECIMAL(9,2))
                    END AS ivr_containment_rate
                FROM call c
                JOIN vxmlsession vs
                  ON c.callguid = vs.callguid
                 AND c.callstartdate = vs.callstartdate
                WHERE c.startdatetime >= ?
                  AND c.startdatetime < ?
                GROUP BY DATE(c.startdatetime), CAST(TO_CHAR(c.startdatetime, '%H') AS INTEGER)
                ORDER BY call_date, call_hour
                """;

        public static final String CVP_IVR_NODES = """
                SELECT FIRST 500
                    c.callguid AS call_id,
                    c.startdatetime AS call_start_time,
                    c.enddatetime AS call_end_time,
                    c.ani AS caller_number,
                    vs.appname AS app_name,
                    ((c.enddatetime - c.startdatetime)::interval second(6) to second) AS duration,
                    vxmlelementflag.name AS flag,
                    vs.causeid AS call_disposition_id,
                    CASE vs.causeid
                        WHEN 0 THEN 'None'
                        WHEN 1 THEN 'Normal Completion'
                        WHEN 13 THEN 'Called Party Disconnected'
                        WHEN 18 THEN 'Error'
                        WHEN 1001 THEN 'HangUp'
                        WHEN 20 THEN 'Post Call Answer'
                        WHEN 29 THEN 'Whisper Start'
                        WHEN 30 THEN 'Whisper Done'
                        WHEN 1044 THEN 'Error CVP No Session Error'
                        WHEN 2 THEN 'Call Abandoned'
                        ELSE CAST(vs.causeid AS varchar(10))
                    END AS call_disposition_flag_desc
                FROM call c
                JOIN vxmlsession vs
                  ON c.callguid = vs.callguid
                 AND c.callstartdate = vs.callstartdate
                JOIN vxmlelement
                  ON vs.sessionid = vxmlelement.sessionid
                JOIN vxmlelementflag
                  ON vxmlelement.elementid = vxmlelementflag.elementid
                WHERE c.startdatetime >= ?
                  AND c.startdatetime < ?
                  AND vxmlelement.elementtypeid = 9
                  AND (? IS NULL OR UPPER(vs.appname) LIKE '%' || UPPER(?) || '%')
                ORDER BY c.startdatetime DESC
                """;
    }
}
