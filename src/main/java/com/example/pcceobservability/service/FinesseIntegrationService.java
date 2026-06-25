package com.example.pcceobservability.service;

import com.example.pcceobservability.config.PcceProperties;
import com.example.pcceobservability.model.FinesseEndpointResult;
import com.example.pcceobservability.model.IntegrationCapability;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FinesseIntegrationService {

    private static final Pattern FINESSE_USER_ID_PATTERN = Pattern.compile("<id>\\s*([^<]+?)\\s*</id>", Pattern.CASE_INSENSITIVE);
    private static final Pattern FINESSE_USER_BLOCK_PATTERN = Pattern.compile("<User\\b[^>]*>[\\s\\S]*?</User>", Pattern.CASE_INSENSITIVE);
    private static final int CACHE_TTL_SECONDS = 30;

    private final PcceProperties pcceProperties;
    private final JdbcTemplate awJdbcTemplate;
    private final SSLSocketFactory sslSocketFactory;
    private final HostnameVerifier hostnameVerifier;
    private volatile List<FinesseEndpointResult> cachedAgents = List.of();
    private volatile Instant cachedAgentsAt = Instant.EPOCH;
    private volatile List<FinesseEndpointResult> cachedDialogs = List.of();
    private volatile Instant cachedDialogsAt = Instant.EPOCH;
    private final AtomicBoolean agentsRefreshInProgress = new AtomicBoolean(false);
    private final AtomicBoolean dialogsRefreshInProgress = new AtomicBoolean(false);

    public FinesseIntegrationService(
            PcceProperties pcceProperties,
            @Qualifier("awJdbcTemplate") JdbcTemplate awJdbcTemplate,
            SSLSocketFactory sslSocketFactory,
            HostnameVerifier hostnameVerifier) {
        this.pcceProperties = pcceProperties;
        this.awJdbcTemplate = awJdbcTemplate;
        this.sslSocketFactory = sslSocketFactory;
        this.hostnameVerifier = hostnameVerifier;
    }

    public List<IntegrationCapability> capabilities() {
        return List.of(
                new IntegrationCapability("Finesse", "SystemInfo", "GET /finesse/api/SystemInfo", status(), "Validate Finesse desktop API and service health"),
                new IntegrationCapability("Finesse", "User directory", "GET /finesse/api/Users", status(), "Discover Finesse-visible users from Finesse and AW login IDs"),
                new IntegrationCapability("Finesse", "Live agent state", "GET /finesse/api/User/{id}", status(), "Use AW LoginName/Finesse login ID, not only numeric SkillTargetID"),
                new IntegrationCapability("Finesse", "Agent dialogs", "GET /finesse/api/User/{id}/Dialogs", status(), "Track active calls, held calls, participants, and dialog lifecycle"),
                new IntegrationCapability("Finesse", "Teams", "GET /finesse/api/Team/{id}", status(), "Configure pcce.finesse.team-ids for supervisor/team monitoring"),
                new IntegrationCapability("Finesse", "Queues", "GET /finesse/api/Queue/{id}", status(), "Use when exposed by your deployment; otherwise keep queue KPIs from AW/HDS/CUIC"));
    }

    public List<FinesseEndpointResult> system() {
        return List.of(request("SystemInfo", "/finesse/api/SystemInfo"));
    }

    public List<FinesseEndpointResult> agents() {
        if (fresh(cachedAgentsAt) && !cachedAgents.isEmpty()) {
            return cachedAgents;
        }
        if (!agentsRefreshInProgress.compareAndSet(false, true)) {
            return withRefreshInProgress(cachedAgents, "Finesse agent refresh already running; returning cached agent state.");
        }
        try {
            List<FinesseEndpointResult> results = new ArrayList<>();
            FinesseEndpointResult directory = request("Users Directory", "/finesse/api/Users");
            results.add(directory);
            Set<String> userIds = new LinkedHashSet<>(userIdsFromDirectory(directory.body()));
            userIds.addAll(configuredUserIds());
            results.addAll(userIds.parallelStream()
                    .limit(25)
                    .map(userId -> request("User " + userId, "/finesse/api/User/" + encodePath(userId)))
                    .toList());
            cachedAgents = List.copyOf(results);
            cachedAgentsAt = Instant.now();
            return results;
        } finally {
            agentsRefreshInProgress.set(false);
        }
    }

    public List<FinesseEndpointResult> dialogs() {
        if (fresh(cachedDialogsAt) && !cachedDialogs.isEmpty()) {
            return cachedDialogs;
        }
        if (!dialogsRefreshInProgress.compareAndSet(false, true)) {
            return withRefreshInProgress(cachedDialogs, "Finesse dialog refresh already running; returning cached dialogs.");
        }
        try {
            if (!fresh(cachedAgentsAt) || cachedAgents.isEmpty()) {
                agents();
            }
            Set<String> userIds = activeFinesseUserIds(cachedAgents);
            if (userIds.isEmpty()) {
                cachedDialogs = List.of(new FinesseEndpointResult("Dialogs", "GET", target("/finesse/api/User/{id}/Dialogs"),
                        204, 0, "No active TALKING/RESERVED/HOLD agents discovered in cached Finesse user state.", Instant.now()));
                cachedDialogsAt = Instant.now();
                return cachedDialogs;
            }
            List<FinesseEndpointResult> results = userIds.parallelStream()
                    .limit(12)
                    .map(userId -> request("Dialogs " + userId, "/finesse/api/User/" + encodePath(userId) + "/Dialogs"))
                    .toList();
            cachedDialogs = List.copyOf(results);
            cachedDialogsAt = Instant.now();
            return results;
        } finally {
            dialogsRefreshInProgress.set(false);
        }
    }

    public List<FinesseEndpointResult> teams() {
        List<String> teamIds = filtered(pcceProperties.getFinesse().getTeamIds());
        if (teamIds.isEmpty()) {
            return List.of(request("Teams Directory", "/finesse/api/Teams"));
        }
        return teamIds.parallelStream()
                .map(teamId -> request("Team " + teamId, "/finesse/api/Team/" + encodePath(teamId)))
                .toList();
    }

    public List<FinesseEndpointResult> queues() {
        List<String> queueIds = filtered(pcceProperties.getFinesse().getQueueIds());
        if (queueIds.isEmpty()) {
            return List.of(request("Queues Directory", "/finesse/api/Queues"));
        }
        return queueIds.parallelStream()
                .map(queueId -> request("Queue " + queueId, "/finesse/api/Queue/" + encodePath(queueId)))
                .toList();
    }

    private FinesseEndpointResult request(String name, String path) {
        PcceProperties.Finesse finesse = pcceProperties.getFinesse();
        Instant checkedAt = Instant.now();
        long start = System.nanoTime();
        String target = target(path);
        if (finesse == null || !finesse.isEnabled()) {
            return new FinesseEndpointResult(name, "GET", target, 0, elapsedMs(start),
                    "Finesse integration disabled. Set pcce.finesse.enabled=true and configure credentials.", checkedAt);
        }
        if (!StringUtils.hasText(finesse.getBaseUrl())) {
            return new FinesseEndpointResult(name, "GET", target, 0, elapsedMs(start),
                    "pcce.finesse.base-url is required.", checkedAt);
        }
        try {
            HttpURLConnection connection = open(finesse, target);
            int statusCode = connection.getResponseCode();
            return new FinesseEndpointResult(name, "GET", target, statusCode, elapsedMs(start),
                    readBody(connection, statusCode), checkedAt);
        } catch (IOException ex) {
            return new FinesseEndpointResult(name, "GET", target, 0, elapsedMs(start), ex.getMessage(), checkedAt);
        }
    }

    private HttpURLConnection open(PcceProperties.Finesse finesse, String target) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(target).openConnection();
        if (connection instanceof HttpsURLConnection httpsConnection) {
            httpsConnection.setSSLSocketFactory(sslSocketFactory);
            httpsConnection.setHostnameVerifier(hostnameVerifier);
        }
        int timeoutMs = (int) (finesse.getTimeout() == null ? 10000 : finesse.getTimeout().toMillis());
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/xml, application/json;q=0.9, text/xml;q=0.8");
        if (StringUtils.hasText(finesse.getUsername()) && finesse.getPassword() != null) {
            String token = Base64.getEncoder().encodeToString((finesse.getUsername() + ":" + finesse.getPassword())
                    .getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic " + token);
        }
        return connection;
    }

    private String readBody(HttpURLConnection connection, int statusCode) throws IOException {
        InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            return "";
        }
        try (InputStream inputStream = stream) {
            String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            if (statusCode == 401 || statusCode == 403 || body.toLowerCase().contains("access denied")) {
                return "Access denied by Finesse. Verify username/password, user role, API permissions, and that the endpoint is enabled on the Finesse node.";
            }
            return body.length() > 5000 ? body.substring(0, 5000) + "\n...truncated..." : body;
        }
    }

    private List<String> configuredUserIds() {
        Set<String> userIds = new LinkedHashSet<>(filtered(pcceProperties.getFinesse().getUserIds()));
        userIds.addAll(awFinesseUserIds());
        for (PcceProperties.AppUser user : pcceProperties.getSecurity().getUsers()) {
            if (StringUtils.hasText(user.getAgentId())) {
                userIds.add(user.getAgentId().trim());
            }
        }
        return new ArrayList<>(userIds);
    }

    private List<String> awFinesseUserIds() {
        try {
            return awJdbcTemplate.query("""
                    SELECT TOP 500
                        COALESCE(p.LoginName, CAST(a.SkillTargetID AS varchar(50))) AS user_id
                    FROM t_Agent a
                    LEFT JOIN t_Person p ON p.PersonID = a.PersonID
                    WHERE COALESCE(p.LoginName, CAST(a.SkillTargetID AS varchar(50))) IS NOT NULL
                    ORDER BY user_id
                    """, (rs, rowNum) -> rs.getString("user_id")).stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<String> userIdsFromDirectory(String body) {
        if (!StringUtils.hasText(body)) {
            return List.of();
        }
        Matcher matcher = FINESSE_USER_ID_PATTERN.matcher(body);
        List<String> ids = new ArrayList<>();
        while (matcher.find()) {
            String id = matcher.group(1);
            if (StringUtils.hasText(id)) {
                ids.add(id.trim());
            }
        }
        return ids;
    }

    private Set<String> activeFinesseUserIds(List<FinesseEndpointResult> agentResults) {
        Set<String> ids = new LinkedHashSet<>();
        for (FinesseEndpointResult result : agentResults == null ? List.<FinesseEndpointResult>of() : agentResults) {
            String body = result.body();
            Matcher matcher = FINESSE_USER_BLOCK_PATTERN.matcher(body == null ? "" : body);
            if (!matcher.find() && StringUtils.hasText(body)) {
                collectActiveUserId(body, ids);
                continue;
            }
            matcher.reset();
            while (matcher.find()) {
                collectActiveUserId(matcher.group(), ids);
            }
        }
        return ids;
    }

    private void collectActiveUserId(String userXml, Set<String> ids) {
        String state = xmlTag(userXml, "state").toUpperCase();
        if (!Set.of("TALKING", "RESERVED", "HOLD", "ACTIVE").contains(state)) {
            return;
        }
        String id = xmlTag(userXml, "id");
        if (StringUtils.hasText(id)) {
            ids.add(id.trim());
        }
    }

    private String xmlTag(String body, String tagName) {
        Matcher matcher = Pattern.compile("<" + tagName + "\\b[^>]*>\\s*([^<]+?)\\s*</" + tagName + ">",
                Pattern.CASE_INSENSITIVE).matcher(body == null ? "" : body);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private List<FinesseEndpointResult> withRefreshInProgress(List<FinesseEndpointResult> cached, String message) {
        if (cached != null && !cached.isEmpty()) {
            List<FinesseEndpointResult> results = new ArrayList<>(cached);
            results.add(new FinesseEndpointResult("Refresh Status", "GET", "cache", 202, 0, message, Instant.now()));
            return results;
        }
        return List.of(new FinesseEndpointResult("Refresh Status", "GET", "cache", 202, 0, message, Instant.now()));
    }

    private List<String> filtered(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(value -> !"null".equalsIgnoreCase(value))
                .toList();
    }

    private String target(String path) {
        PcceProperties.Finesse finesse = pcceProperties.getFinesse();
        if (finesse == null || !StringUtils.hasText(finesse.getBaseUrl())) {
            return path;
        }
        return URI.create(finesse.getBaseUrl().replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "")).toString();
    }

    private String encodePath(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String status() {
        PcceProperties.Finesse finesse = pcceProperties.getFinesse();
        return finesse != null && finesse.isEnabled() ? "Configured" : "Config required";
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private boolean fresh(Instant cachedAt) {
        return cachedAt != null && cachedAt.plusSeconds(CACHE_TTL_SECONDS).isAfter(Instant.now());
    }
}
