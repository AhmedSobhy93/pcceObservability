package com.cisco.cx.observability.feature.usermgmt.web;

import com.cisco.cx.observability.config.PcceProperties;
import com.cisco.cx.observability.feature.operations.domain.ProductionReadinessItem;
import com.cisco.cx.observability.feature.monitoring.domain.SchemaColumn;
import com.cisco.cx.observability.feature.monitoring.domain.SchemaTable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/diagnostics")
@PreAuthorize("hasAuthority('PERM_SOLUTION_ADMIN')")
public class AdminDiagnosticsController {

    private final DataSource awDataSource;
    private final DataSource hdsDataSource;
    private final DataSource cvpReportingDataSource;
    private final PcceProperties pcceProperties;

    public AdminDiagnosticsController(
            @Qualifier("awDataSource") DataSource awDataSource,
            @Qualifier("hdsDataSource") DataSource hdsDataSource,
            @Qualifier("cvpReportingDataSource") DataSource cvpReportingDataSource,
            PcceProperties pcceProperties) {
        this.awDataSource = awDataSource;
        this.hdsDataSource = hdsDataSource;
        this.cvpReportingDataSource = cvpReportingDataSource;
        this.pcceProperties = pcceProperties;
    }

    @GetMapping("/config-readiness")
    public List<ProductionReadinessItem> configReadiness() {
        PcceProperties.PcceApi pcceApi = pcceProperties.getPcceApi();
        PcceProperties.Finesse finesse = pcceProperties.getFinesse();
        PcceProperties.Notifications notifications = pcceProperties.getNotifications();
        PcceProperties.Ldap ldap = pcceProperties.getSecurity().getLdap();
        PcceProperties.Jmx jmx = pcceProperties.getJmx();
        PcceProperties.LiveData liveData = pcceProperties.getLiveData();
        PcceProperties.AppDynamics appDynamics = pcceProperties.getAppDynamics();
        PcceProperties.Ssl ssl = pcceProperties.getSsl();
        PcceProperties.Security.RateLimit rateLimit = pcceProperties.getSecurity().getRateLimit();
        List<ProductionReadinessItem> items = new ArrayList<>();
        items.add(item("TLS Trust",
                ssl != null && ssl.isVerifyHostname(),
                ssl != null && hasText(ssl.getTrustStorePath())
                        ? "Custom trust store configured"
                        : "Using JVM default trust store",
                "Import Cisco/PCCE/Finesse/CVP issuing CA certificates into a JKS/PKCS12 trust store and set PCCE_SSL_TRUST_STORE. Keep PCCE_SSL_VERIFY_HOSTNAME=true in production."));
        items.add(item("Rate Limiting",
                rateLimit != null && rateLimit.isEnabled() && rateLimit.getMaxRequestsPerMinute() > 0,
                rateLimit != null && rateLimit.isEnabled()
                        ? "Enabled at " + rateLimit.getMaxRequestsPerMinute() + " requests/minute/client"
                        : "Disabled",
                "Keep PCCE_RATE_LIMIT_ENABLED=true and tune PCCE_RATE_LIMIT_RPM for NOC wallboards and admin users."));
        items.add(item("PCCE API",
                pcceApi != null && pcceApi.isEnabled() && hasText(pcceApi.getBaseUrl()) && hasText(pcceApi.getUsername()),
                pcceApi != null && pcceApi.isEnabled() ? "PCCE API enabled" : "PCCE API disabled",
                "Set PCCE_API_ENABLED=true, PCCE_API_BASE_URL, PCCE_API_USERNAME, and PCCE_API_PASSWORD."));
        items.add(item("Finesse API",
                finesse != null && finesse.isEnabled() && hasText(finesse.getBaseUrl()) && hasText(finesse.getUsername()),
                finesse != null && finesse.isEnabled() ? "Finesse API enabled" : "Finesse API disabled",
                "Set FINESSE_ENABLED=true, FINESSE_BASE_URL, FINESSE_USERNAME, and FINESSE_PASSWORD."));
        items.add(item("LDAP / AD",
                ldap != null && (!ldap.isEnabled() || hasText(ldap.getUserSearchFilter())),
                ldap != null && ldap.isEnabled() ? "LDAP enabled" : "LDAP disabled; local users only",
                "For dual auth set APP_LDAP_ENABLED=true plus APP_LDAP_URLS, APP_LDAP_BASE, manager DN/password, and role groups."));
        items.add(item("SMTP Alerts",
                notifications != null && (!notifications.isSmtpEnabled() || hasText(notifications.getSmtpFrom()) && hasItems(notifications.getSmtpRecipients())),
                notifications != null && notifications.isSmtpEnabled() ? "SMTP enabled" : "SMTP disabled",
                "Set PCCE_SMTP_ENABLED=true, sender, recipients, host, and auth/TLS settings."));
        items.add(item("SMS Alerts",
                notifications != null && (!notifications.isSmsEnabled()
                        || hasText(notifications.getSmsUrl()) && hasText(notifications.getSmsAuthorization()) && hasItems(notifications.getSmsRecipients())),
                notifications != null && notifications.isSmsEnabled() ? "SMS enabled" : "SMS disabled",
                "Set PCCE_SMS_ENABLED=true, D-Tech SMS URL, Basic Authorization, User-Agent, and recipients."));
        items.add(item("Remote Server Metrics",
                false,
                "Collector not built into app",
                "Use SNMP, WMI/WinRM, Prometheus exporter, SIEM, or installed agent for CPU/memory/disk/services."));
        items.add(item("Secure CVP JMX",
                jmx != null && (!jmx.isEnabled() || jmx.isTrustStoreConfigured() && hasText(jmx.getUsername()) && hasAny(jmx.getTargets())),
                jmx != null && jmx.isEnabled() ? "JMX enabled" : "JMX disabled",
                "Follow Cisco secure CVP JMX/OAMP certificate setup, then set PCCE_JMX_* targets and credentials."));
        items.add(item("PCCE Live Data",
                liveData != null && (!liveData.isEnabled() || hasText(liveData.getHost()) && hasText(liveData.getUsername())),
                liveData != null && liveData.isEnabled() ? "Live Data enabled" : "Live Data disabled",
                "Use CUIC Live Data host, port 443, WebSocket port 443, token path livedata/token/new, and Live Data user."));
        items.add(item("AppDynamics",
                appDynamics != null && (!appDynamics.isEnabled()
                        || hasText(appDynamics.getControllerUrl()) && hasText(appDynamics.getApplicationName())),
                appDynamics != null && appDynamics.isEnabled() ? "AppDynamics enabled" : "AppDynamics disabled",
                "Use existing node agents for telemetry; configure controller URL, application name, account and read-only API user."));
        return items;
    }

    @GetMapping("/{source}/tables")
    public List<SchemaTable> tables(
            @PathVariable String source,
            @RequestParam(defaultValue = "%") String namePattern) throws SQLException {
        try (Connection connection = dataSource(source).getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<SchemaTable> tables = new ArrayList<>();
            try (ResultSet rs = metaData.getTables(connection.getCatalog(), null, namePattern, new String[]{"TABLE", "VIEW"})) {
                while (rs.next() && tables.size() < 500) {
                    tables.add(new SchemaTable(
                            rs.getString("TABLE_CAT"),
                            rs.getString("TABLE_SCHEM"),
                            rs.getString("TABLE_NAME"),
                            rs.getString("TABLE_TYPE")));
                }
            }
            return tables;
        }
    }

    @GetMapping("/{source}/tables/{tableName}/columns")
    public List<SchemaColumn> columns(
            @PathVariable String source,
            @PathVariable String tableName) throws SQLException {
        try (Connection connection = dataSource(source).getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<SchemaColumn> columns = new ArrayList<>();
            try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, tableName, "%")) {
                while (rs.next()) {
                    columns.add(new SchemaColumn(
                            rs.getString("TABLE_NAME"),
                            rs.getString("COLUMN_NAME"),
                            rs.getString("TYPE_NAME"),
                            rs.getInt("COLUMN_SIZE"),
                            rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable));
                }
            }
            return columns;
        }
    }

    @GetMapping("/{source}/columns")
    public List<SchemaColumn> searchColumns(
            @PathVariable String source,
            @RequestParam(defaultValue = "%") String tablePattern,
            @RequestParam(defaultValue = "%") String columnPattern) throws SQLException {
        try (Connection connection = dataSource(source).getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<SchemaColumn> columns = new ArrayList<>();
            try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, tablePattern, columnPattern)) {
                while (rs.next() && columns.size() < 1000) {
                    columns.add(new SchemaColumn(
                            rs.getString("TABLE_NAME"),
                            rs.getString("COLUMN_NAME"),
                            rs.getString("TYPE_NAME"),
                            rs.getInt("COLUMN_SIZE"),
                            rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable));
                }
            }
            return columns;
        }
    }

    private DataSource dataSource(String source) {
        return switch (source.toLowerCase()) {
            case "aw" -> awDataSource;
            case "hds" -> hdsDataSource;
            case "cvp", "cvp-reporting", "cvp_reporting" -> cvpReportingDataSource;
            default -> throw new IllegalArgumentException("source must be one of: aw, hds, cvp-reporting");
        };
    }

    private ProductionReadinessItem item(String area, boolean ready, String finding, String recommendation) {
        return new ProductionReadinessItem(area, ready, finding, recommendation);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean hasItems(List<String> values) {
        return values != null && !values.isEmpty();
    }

    private boolean hasAny(List<?> values) {
        return values != null && !values.isEmpty();
    }
}
