package com.example.pcceobservability.config;

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
    private Operations operations = new Operations();

    @Valid
    private Performance performance = new Performance();

    @Valid
    private PcceApi pcceApi = new PcceApi();

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
        private String agentStats = DefaultSql.AGENT_STATS;
        private String droppedCalls = DefaultSql.DROPPED_CALLS;
        private String dispositionBreakdown = DefaultSql.DISPOSITION_BREAKDOWN;
        private String ivrContainment = DefaultSql.IVR_CONTAINMENT;

        public String getCallMetrics() {
            return callMetrics;
        }

        public void setCallMetrics(String callMetrics) {
            this.callMetrics = callMetrics;
        }

        public String getAgentStats() {
            return agentStats;
        }

        public void setAgentStats(String agentStats) {
            this.agentStats = agentStats;
        }

        public String getDroppedCalls() {
            return droppedCalls;
        }

        public void setDroppedCalls(String droppedCalls) {
            this.droppedCalls = droppedCalls;
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
        private boolean enabled;
        private ProbeType probe = ProbeType.TCP;
        private String host;
        private int port;
        private String url;
        private Duration timeout = Duration.ofSeconds(3);
        private boolean trustAllCertificates;

        public ComponentName getName() {
            return name;
        }

        public void setName(ComponentName name) {
            this.name = name;
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
    }

    public enum ProbeType {
        TCP,
        HTTP,
        JDBC_AW,
        JDBC_HDS,
        JDBC_CVP_REPORTING
    }

    public enum ComponentName {
        ICM_Router,
        ICM_Logger,
        CVP_CallServer,
        CVP_ReportingServer,
        CTI_Server,
        PG_CUCM,
        PG_CVP,
        VoIP_Gateway,
        Finesse,
        MediaSense,
        CUIC
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
        private Duration timeout = Duration.ofSeconds(10);
        private boolean trustAllCertificates;
        private List<ApiMonitor> monitors = defaultApiMonitors();

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

        public List<ApiMonitor> getMonitors() {
            return monitors;
        }

        public void setMonitors(List<ApiMonitor> monitors) {
            this.monitors = monitors;
        }

        private static List<ApiMonitor> defaultApiMonitors() {
            return List.of(
                    monitor("Users", "User Configuration and Management", "/unifiedconfig/config/user"),
                    monitor("Agent Teams", "Team Configuration and Management", "/unifiedconfig/config/agentteam"),
                    monitor("Skill Groups", "Skill Group Management", "/unifiedconfig/config/skillgroup"),
                    monitor("Dialed Numbers", "Call Configuration and Management", "/unifiedconfig/config/dialednumber"),
                    monitor("Call Types", "Call Configuration and Management", "/unifiedconfig/config/calltype"),
                    monitor("Business Hours", "System Configuration", "/unifiedconfig/config/businesshour"));
        }

        private static ApiMonitor monitor(String name, String category, String path) {
            ApiMonitor monitor = new ApiMonitor();
            monitor.setName(name);
            monitor.setCategory(category);
            monitor.setMethod("GET");
            monitor.setPath(path);
            monitor.setExpectedStatusMax(499);
            monitor.setEnabled(false);
            return monitor;
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

    public static class Security {
        private List<AppUser> users = defaultUsers();
        private Map<AppRole, List<Permission>> rolePermissions = defaultRolePermissions();

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

    static class DefaultSql {
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
                  AND (? IS NULL OR sg.EnterpriseName = ?)
                GROUP BY CAST(sgi.DateTime AS date), DATEPART(hour, sgi.DateTime), sg.EnterpriseName
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
                WHERE ? IS NOT NULL
                  AND (? IS NULL OR p.LoginName = ?)
                  AND (? IS NULL OR at.EnterpriseName = ?)
                ORDER BY [date], agent_name
                """;

        static final String DROPPED_CALLS = """
                SELECT
                    CAST(tcd.DateTime AS date) AS [date],
                    DATEPART(hour, tcd.DateTime) AS [hour],
                    COALESCE(sg.EnterpriseName, 'UNKNOWN') AS skill_group,
                    COUNT_BIG(*) AS dropped_calls,
                    MAX(tcd.DateTime) AS last_drop_time
                FROM t_Termination_Call_Detail tcd
                LEFT JOIN t_Skill_Group sg ON sg.SkillTargetID = tcd.SkillGroupSkillTargetID
                WHERE tcd.DateTime >= ? AND tcd.DateTime < ?
                  AND tcd.CallDisposition IN (1, 2, 3, 4, 5, 6, 7, 10, 19)
                  AND (? IS NULL OR sg.EnterpriseName = ?)
                GROUP BY CAST(tcd.DateTime AS date), DATEPART(hour, tcd.DateTime), COALESCE(sg.EnterpriseName, 'UNKNOWN')
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
                    TODAY AS call_date,
                    0 AS call_hour,
                    CAST(NULL AS DECIMAL(9,2)) AS ivr_containment_rate
                FROM systables
                WHERE tabid < 0
                  AND CURRENT >= ?
                  AND CURRENT >= ?
                """;
    }
}
