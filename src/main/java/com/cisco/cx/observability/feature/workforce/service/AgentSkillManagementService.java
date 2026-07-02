package com.cisco.cx.observability.feature.workforce.service;

import com.cisco.cx.observability.config.PcceProperties;
import com.cisco.cx.observability.feature.workforce.domain.AgentProvisioningPlan;
import com.cisco.cx.observability.feature.workforce.domain.ProvisioningCatalog;
import com.cisco.cx.observability.feature.workforce.domain.ProvisioningOption;
import com.cisco.cx.observability.feature.workforce.domain.ProvisioningStepResult;
import com.cisco.cx.observability.feature.workforce.web.AgentProvisioningRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentSkillManagementService {
    private static final Pattern TOTAL_RESULTS = Pattern.compile("<totalResults>(\\d+)</totalResults>", Pattern.CASE_INSENSITIVE);

    private final PcceProperties properties;
    private final SSLSocketFactory sslSocketFactory;
    private final HostnameVerifier hostnameVerifier;

    public AgentSkillManagementService(
            PcceProperties properties,
            SSLSocketFactory sslSocketFactory,
            HostnameVerifier hostnameVerifier) {
        this.properties = properties;
        this.sslSocketFactory = sslSocketFactory;
        this.hostnameVerifier = hostnameVerifier;
    }

    public ProvisioningCatalog catalog() {
        List<String> warnings = new ArrayList<>();
        PcceProperties.PcceApi pcceApi = properties.getPcceApi();
        PcceProperties.CucmAxl cucmAxl = properties.getCucmAxl();
        if (pcceApi == null || !pcceApi.isEnabled() || !StringUtils.hasText(pcceApi.getBaseUrl())) {
            warnings.add("PCCE API catalog is disabled or pcce.pcce-api.base-url is not configured.");
        }
        if (cucmAxl == null || !cucmAxl.isEnabled() || !StringUtils.hasText(cucmAxl.getHost())) {
            warnings.add("CUCM AXL is disabled or pcce.cucm-axl.host is not configured.");
        }
        return new ProvisioningCatalog(
                pcceApi != null && pcceApi.isEnabled(),
                cucmAxl != null && cucmAxl.isEnabled(),
                properties.getAgentProvisioning().isExecutionEnabled(),
                identityDomain(),
                safeOptions("agent", "agent", warnings),
                safeOptions("agentteam", "agentTeam", warnings),
                safeOptions("skillgroup", "skillGroup", warnings),
                safeOptions("agentdesksettings", "agentDeskSettings", warnings),
                warnings);
    }

    public AgentProvisioningPlan plan(AgentProvisioningRequest request) {
        return buildPlan(request, true, false);
    }

    public AgentProvisioningPlan execute(AgentProvisioningRequest request, boolean dryRun) {
        return buildPlan(request, dryRun || !properties.getAgentProvisioning().isExecutionEnabled(), true);
    }

    private AgentProvisioningPlan buildPlan(AgentProvisioningRequest request, boolean dryRun, boolean executeRequested) {
        List<String> warnings = new ArrayList<>();
        String baseUsername = normalizeBaseUsername(request.baseUsername(), request.mail(), request.agentId());
        String cucmUserId = stripDomain(baseUsername);
        String pcceUserName = baseUsername.contains("@") ? baseUsername : baseUsername + "@" + identityDomain();
        String displayName = displayName(request);
        String dn = StringUtils.hasText(request.dn()) ? request.dn().trim() : "AUTO";
        String deviceName = "AUTO".equalsIgnoreCase(dn) ? "CSF{allocatedDn}" : "CSF" + dn;
        List<ProvisioningStepResult> steps = new ArrayList<>();

        steps.add(step(1, "CUCM AXL", "Authenticate and read CUCM version", "POST", axlUrl(),
                dryRun, () -> axl("getCCMVersion", getCcmVersionSoap()), getCcmVersionSoap()));
        steps.add(step(2, "CUCM AXL", "Find next free DN", "POST", axlUrl(),
                dryRun || !"AUTO".equalsIgnoreCase(dn), () -> axl("executeSQLQuery", freeDnSoap()), freeDnSoap()));
        if ("new".equalsIgnoreCase(request.userMode())) {
            steps.add(step(3, "CUCM AXL", "Create local CUCM end user", "POST", axlUrl(),
                    dryRun, () -> axl("addUser", addUserSoap(cucmUserId, displayName, request)), addUserSoap(cucmUserId, displayName, request)));
        } else {
            steps.add(skipped(3, "CUCM AXL", "Use existing LDAP/SSO CUCM user", axlUrl(), "Existing user mode; addUser is not required."));
        }
        steps.add(step(4, "CUCM AXL", "Create line/DN", "POST", axlUrl(),
                dryRun || "AUTO".equalsIgnoreCase(dn), () -> axl("addLine", addLineSoap(dn)), addLineSoap(dn)));
        steps.add(step(5, "CUCM AXL", "Create CSF/Jabber phone", "POST", axlUrl(),
                dryRun || "AUTO".equalsIgnoreCase(dn), () -> axl("addPhone", addPhoneSoap(deviceName, dn, displayName)), addPhoneSoap(deviceName, dn, displayName)));
        steps.add(step(6, "CUCM AXL", "Associate device and service profile", "POST", axlUrl(),
                dryRun || "AUTO".equalsIgnoreCase(dn), () -> axl("updateUser", updateUserSoap(cucmUserId, deviceName, dn)), updateUserSoap(cucmUserId, deviceName, dn)));

        ProvisioningCatalog catalog = catalog();
        Optional<ProvisioningOption> existingAgent = catalog.agents().stream()
                .filter(agent -> equalsAny(agent.name(), pcceUserName, cucmUserId, request.agentId())
                        || equalsAny(agent.id(), request.agentId(), cucmUserId))
                .findFirst();
        Optional<ProvisioningOption> team = findByName(catalog.teams(), request.teamName());
        List<ProvisioningOption> skills = request.skillGroupNames() == null ? List.of()
                : request.skillGroupNames().stream()
                        .map(name -> findByName(catalog.skillGroups(), name))
                        .flatMap(Optional::stream)
                        .toList();
        Optional<ProvisioningOption> desk = findByName(catalog.deskSettings(), request.deskSettingsName());

        if (StringUtils.hasText(request.teamName()) && team.isEmpty()) {
            warnings.add("Team was not found in PCCE catalog: " + request.teamName());
        }
        if (request.skillGroupNames() != null && skills.size() < request.skillGroupNames().size()) {
            warnings.add("One or more selected skill groups were not found in PCCE catalog.");
        }

        String agentPayload = pcceAgentPayload(request, pcceUserName, displayName, existingAgent, team, skills, desk);
        String target = existingAgent.map(ProvisioningOption::refUrl)
                .filter(StringUtils::hasText)
                .map(this::pcceUrl)
                .orElse(pcceUrl("/unifiedconfig/config/agent"));
        String method = existingAgent.isPresent() ? "PUT" : "POST";
        steps.add(step(7, "PCCE API", existingAgent.isPresent() ? "Update PCCE agent/supervisor" : "Create PCCE agent/supervisor",
                method, target, dryRun, () -> pcce(method, target, agentPayload), agentPayload));
        steps.add(step(8, "PCCE API", "Verify agent after provisioning", "GET",
                pcceUrl("/unifiedconfig/config/agent"), dryRun, () -> pcce("GET", pcceUrl("/unifiedconfig/config/agent"), null), ""));

        if (executeRequested && !properties.getAgentProvisioning().isExecutionEnabled()) {
            warnings.add("Execution was requested but AGENT_PROVISIONING_EXECUTION_ENABLED is false; all write steps stayed in dry-run mode.");
        }
        if ("AUTO".equalsIgnoreCase(dn)) {
            warnings.add("DN allocation is shown as AUTO. Execute the free-DN step first or provide a DN before live addLine/addPhone.");
        }
        return new AgentProvisioningPlan(cucmUserId, pcceUserName, displayName, dn, deviceName,
                "new".equalsIgnoreCase(request.userMode()) ? "new" : "existing",
                "supervisor".equalsIgnoreCase(request.agentType()) ? "supervisor" : "agent",
                dryRun,
                properties.getAgentProvisioning().isExecutionEnabled(),
                steps,
                warnings);
    }

    private List<ProvisioningOption> safeOptions(String resource, String element, List<String> warnings) {
        try {
            return pcceCatalog(resource, element);
        } catch (Exception ex) {
            warnings.add("Could not load PCCE " + resource + ": " + ex.getMessage());
            return List.of();
        }
    }

    private List<ProvisioningOption> pcceCatalog(String resource, String element) throws IOException {
        PcceProperties.PcceApi api = properties.getPcceApi();
        if (api == null || !api.isEnabled() || !StringUtils.hasText(api.getBaseUrl())) {
            return List.of();
        }
        int pageSize = Math.max(25, properties.getAgentProvisioning().getResultsPerPage());
        int start = 0;
        int total = Integer.MAX_VALUE;
        List<ProvisioningOption> rows = new ArrayList<>();
        while (start < total) {
            String target = pcceUrl("/unifiedconfig/config/" + resource + "?startIndex=" + start + "&resultsPerPage=" + pageSize);
            HttpResponse response = pcce("GET", target, null);
            if (response.statusCode() >= 400) {
                throw new IOException("HTTP " + response.statusCode() + " " + snippet(response.body(), 160));
            }
            total = totalResults(response.body()).orElse(start + pageSize);
            List<ProvisioningOption> parsed = parseOptions(response.body(), element);
            if (parsed.isEmpty()) {
                break;
            }
            rows.addAll(parsed);
            start += pageSize;
        }
        return rows.stream()
                .sorted(Comparator.comparing(option -> String.valueOf(option.name()), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<ProvisioningOption> parseOptions(String xml, String element) {
        Pattern pattern = Pattern.compile("<" + element + "\\b[^>]*>(.*?)</" + element + ">", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xml == null ? "" : xml);
        List<ProvisioningOption> rows = new ArrayList<>();
        while (matcher.find()) {
            String block = matcher.group(1);
            String refUrl = tag(block, "refURL");
            String id = idFromRef(refUrl);
            String name = firstText(block, "name", "userName", "loginName", "enterpriseName", "id");
            String changeStamp = tag(block, "changeStamp");
            String detail = firstText(block, "description", "firstName", "lastName", "peripheralNumber");
            rows.add(new ProvisioningOption(id, name, refUrl, element, changeStamp, detail));
        }
        return rows;
    }

    private ProvisioningStepResult step(
            int order,
            String system,
            String action,
            String method,
            String target,
            boolean dryRun,
            StepCall call,
            String payload) {
        if (dryRun || !isEnabledForSystem(system)) {
            return skipped(order, system, action, target, dryRun ? "Dry run only." : system + " integration is disabled.");
        }
        long start = System.nanoTime();
        try {
            HttpResponse response = call.call();
            String status = response.statusCode() < 400 ? "OK" : "FAILED";
            return new ProvisioningStepResult(order, system, action, method, target, status, response.statusCode(),
                    elapsedMs(start), snippet(response.body(), 260), snippet(payload, 900));
        } catch (Exception ex) {
            return new ProvisioningStepResult(order, system, action, method, target, "FAILED", 0, elapsedMs(start),
                    ex.getMessage(), snippet(payload, 900));
        }
    }

    private boolean isEnabledForSystem(String system) {
        if (system.startsWith("CUCM")) {
            PcceProperties.CucmAxl axl = properties.getCucmAxl();
            return axl != null && axl.isEnabled();
        }
        if (system.startsWith("PCCE")) {
            PcceProperties.PcceApi api = properties.getPcceApi();
            return api != null && api.isEnabled();
        }
        return true;
    }

    private ProvisioningStepResult skipped(int order, String system, String action, String target, String detail) {
        return new ProvisioningStepResult(order, system, action, "N/A", target, "SKIPPED", 0, 0, detail, "");
    }

    private HttpResponse axl(String soapAction, String body) throws IOException {
        PcceProperties.CucmAxl axl = properties.getCucmAxl();
        HttpURLConnection connection = open(axlUrl(), "POST", "text/xml; charset=utf-8", axl.getTimeout());
        connection.setRequestProperty("SOAPAction", "CUCM:DB ver=" + axl.getVersion() + " " + soapAction);
        basic(connection, axl.getUsername(), axl.getPassword());
        writeBody(connection, body);
        return new HttpResponse(connection.getResponseCode(), readBody(connection));
    }

    private HttpResponse pcce(String method, String target, String body) throws IOException {
        PcceProperties.PcceApi api = properties.getPcceApi();
        HttpURLConnection connection = open(target, method, body == null ? null : "application/xml", api.getTimeout());
        switch (api.getAuthMode() == null ? PcceProperties.ApiAuthMode.BASIC : api.getAuthMode()) {
            case NONE -> {
            }
            case BASIC -> basic(connection, api.getUsername(), api.getPassword());
            case BEARER -> connection.setRequestProperty("Authorization", "Bearer " + nullToEmpty(api.getBearerToken()));
            case API_KEY -> {
                if (StringUtils.hasText(api.getApiKeyHeaderName())) {
                    connection.setRequestProperty(api.getApiKeyHeaderName(), nullToEmpty(api.getApiKey()));
                }
            }
        }
        if (body != null) {
            writeBody(connection, body);
        }
        return new HttpResponse(connection.getResponseCode(), readBody(connection));
    }

    private HttpURLConnection open(String target, String method, String contentType, Duration timeout) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(target).openConnection();
        if (connection instanceof HttpsURLConnection httpsConnection) {
            httpsConnection.setSSLSocketFactory(sslSocketFactory);
            httpsConnection.setHostnameVerifier(hostnameVerifier);
        }
        connection.setRequestMethod(method);
        connection.setConnectTimeout((int) timeout.toMillis());
        connection.setReadTimeout((int) timeout.toMillis());
        connection.setRequestProperty("Accept", "application/xml, text/xml, application/json, text/plain, */*; q=0.1");
        if (StringUtils.hasText(contentType)) {
            connection.setRequestProperty("Content-Type", contentType);
            connection.setDoOutput(true);
        }
        return connection;
    }

    private void writeBody(HttpURLConnection connection, String body) throws IOException {
        connection.setDoOutput(true);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(nullToEmpty(body).getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readBody(HttpURLConnection connection) throws IOException {
        InputStream stream = connection.getResponseCode() >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            return "";
        }
        try (InputStream inputStream = stream) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void basic(HttpURLConnection connection, String username, String password) {
        if (!StringUtils.hasText(username) || password == null) {
            return;
        }
        String token = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        connection.setRequestProperty("Authorization", "Basic " + token);
    }

    private String getCcmVersionSoap() {
        return axlEnvelope("<getCCMVersion><processNodeName></processNodeName></getCCMVersion>");
    }

    private String freeDnSoap() {
        PcceProperties.CucmAxl axl = properties.getCucmAxl();
        return axlEnvelope("<executeSQLQuery><sql>SELECT dnorpattern FROM numplan WHERE dnorpattern &gt;= '" + axl.getDnStart()
                + "' AND dnorpattern &lt;= '" + axl.getDnEnd() + "' AND tkpatternusage = 2</sql></executeSQLQuery>");
    }

    private String addUserSoap(String userId, String displayName, AgentProvisioningRequest request) {
        return axlEnvelope("<addUser><user><userid>" + xml(userId) + "</userid><firstName>" + xml(request.firstName())
                + "</firstName><lastName>" + xml(request.lastName()) + "</lastName><displayName>" + xml(displayName)
                + "</displayName><mailid>" + xml(request.mail()) + "</mailid></user></addUser>");
    }

    private String addLineSoap(String dn) {
        PcceProperties.CucmAxl axl = properties.getCucmAxl();
        return axlEnvelope("<addLine><line><pattern>" + xml(dn) + "</pattern>" + nameElement("routePartitionName", axl.getRoutePartition())
                + nameElement("shareLineAppearanceCssName", axl.getLineCss()) + "</line></addLine>");
    }

    private String addPhoneSoap(String deviceName, String dn, String displayName) {
        PcceProperties.CucmAxl axl = properties.getCucmAxl();
        return axlEnvelope("<addPhone><phone><name>" + xml(deviceName) + "</name><description>" + xml(displayName)
                + "</description><product>Cisco Unified Client Services Framework</product><class>Phone</class><protocol>SIP</protocol>"
                + nameElement("devicePoolName", axl.getDevicePool())
                + nameElement("commonPhoneConfigName", axl.getCommonPhoneProfile())
                + nameElement("locationName", axl.getLocation())
                + nameElement("securityProfileName", axl.getSecurityProfile())
                + nameElement("sipProfileName", axl.getSipProfile())
                + "<lines><line><index>1</index><dirn><pattern>" + xml(dn) + "</pattern>"
                + nameElement("routePartitionName", axl.getRoutePartition()) + "</dirn></line></lines></phone></addPhone>");
    }

    private String updateUserSoap(String userId, String deviceName, String dn) {
        PcceProperties.CucmAxl axl = properties.getCucmAxl();
        return axlEnvelope("<updateUser><userid>" + xml(userId) + "</userid><associatedDevices><device>" + xml(deviceName)
                + "</device></associatedDevices><primaryExtension><pattern>" + xml(dn) + "</pattern>"
                + nameElement("routePartitionName", axl.getRoutePartition()) + "</primaryExtension>"
                + nameElement("serviceProfile", axl.getServiceProfile()) + "</updateUser>");
    }

    private String pcceAgentPayload(
            AgentProvisioningRequest request,
            String pcceUserName,
            String displayName,
            Optional<ProvisioningOption> existingAgent,
            Optional<ProvisioningOption> team,
            List<ProvisioningOption> skills,
            Optional<ProvisioningOption> desk) {
        String root = "supervisor".equalsIgnoreCase(request.agentType()) ? "supervisor" : "agent";
        StringBuilder builder = new StringBuilder();
        builder.append("<").append(root).append(">");
        existingAgent.map(ProvisioningOption::changeStamp).filter(StringUtils::hasText)
                .ifPresent(changeStamp -> builder.append("<changeStamp>").append(xml(changeStamp)).append("</changeStamp>"));
        builder.append("<userName>").append(xml(pcceUserName)).append("</userName>")
                .append("<firstName>").append(xml(request.firstName())).append("</firstName>")
                .append("<lastName>").append(xml(request.lastName())).append("</lastName>")
                .append("<description>").append(xml(displayName)).append("</description>")
                .append("<agentId>").append(xml(request.agentId())).append("</agentId>");
        team.ifPresent(option -> builder.append("<agentTeam><refURL>").append(xml(option.refUrl())).append("</refURL></agentTeam>"));
        desk.ifPresent(option -> builder.append("<agentDeskSettings><refURL>").append(xml(option.refUrl())).append("</refURL></agentDeskSettings>"));
        if (!skills.isEmpty()) {
            builder.append("<skillGroups>");
            for (ProvisioningOption skill : skills) {
                builder.append("<skillGroup><refURL>").append(xml(skill.refUrl())).append("</refURL>");
                if (request.proficiency() != null) {
                    builder.append("<proficiency>").append(request.proficiency()).append("</proficiency>");
                }
                builder.append("</skillGroup>");
            }
            builder.append("</skillGroups>");
        }
        builder.append("</").append(root).append(">");
        return builder.toString();
    }

    private String axlEnvelope(String body) {
        String namespacedBody = body
                .replaceFirst("^<([A-Za-z0-9_]+)", "<axl:$1")
                .replaceFirst("</([A-Za-z0-9_]+)>$", "</axl:$1>");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:axl=\"http://www.cisco.com/AXL/API/" + xml(properties.getCucmAxl().getVersion()) + "\">"
                + "<soapenv:Body>" + namespacedBody + "</soapenv:Body></soapenv:Envelope>";
    }

    private String nameElement(String name, String value) {
        return StringUtils.hasText(value) ? "<" + name + ">" + xml(value) + "</" + name + ">" : "";
    }

    private Optional<ProvisioningOption> findByName(List<ProvisioningOption> options, String name) {
        if (!StringUtils.hasText(name)) {
            return Optional.empty();
        }
        return options.stream()
                .filter(option -> equalsAny(option.name(), name) || equalsAny(option.id(), name))
                .findFirst();
    }

    private boolean equalsAny(String value, String... candidates) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate) && normalized.equals(candidate.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeBaseUsername(String baseUsername, String mail, String agentId) {
        if (StringUtils.hasText(baseUsername)) {
            return baseUsername.trim();
        }
        if (StringUtils.hasText(mail)) {
            return mail.trim();
        }
        return StringUtils.hasText(agentId) ? agentId.trim() : "agent.user";
    }

    private String displayName(AgentProvisioningRequest request) {
        if (StringUtils.hasText(request.displayName())) {
            return request.displayName().trim();
        }
        return (nullToEmpty(request.firstName()) + " " + nullToEmpty(request.lastName())).trim();
    }

    private String stripDomain(String value) {
        return nullToEmpty(value).replaceAll("@.*$", "");
    }

    private String identityDomain() {
        return StringUtils.hasText(properties.getAgentProvisioning().getIdentityDomain())
                ? properties.getAgentProvisioning().getIdentityDomain()
                : "dev.mdi";
    }

    private String axlUrl() {
        PcceProperties.CucmAxl axl = properties.getCucmAxl();
        if (axl == null || !StringUtils.hasText(axl.getHost())) {
            return "";
        }
        String host = axl.getHost().replaceAll("^https?://", "").replaceAll("/+$", "");
        return "https://" + host + ":8443/axl/";
    }

    private String pcceUrl(String path) {
        PcceProperties.PcceApi api = properties.getPcceApi();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (api == null || !StringUtils.hasText(api.getBaseUrl())) {
            return path;
        }
        return URI.create(api.getBaseUrl().replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "")).toString();
    }

    private Optional<Integer> totalResults(String xml) {
        Matcher matcher = TOTAL_RESULTS.matcher(nullToEmpty(xml));
        return matcher.find() ? Optional.of(Integer.parseInt(matcher.group(1))) : Optional.empty();
    }

    private String firstText(String block, String... tags) {
        for (String tag : tags) {
            String value = tag(block, tag);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String tag(String xml, String tag) {
        Matcher matcher = Pattern.compile("<" + tag + "\\b[^>]*>(.*?)</" + tag + ">", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                .matcher(nullToEmpty(xml));
        return matcher.find() ? matcher.group(1).replaceAll("<[^>]+>", "").trim() : "";
    }

    private String idFromRef(String refUrl) {
        if (!StringUtils.hasText(refUrl)) {
            return "";
        }
        int index = refUrl.lastIndexOf('/');
        return index >= 0 ? refUrl.substring(index + 1) : refUrl;
    }

    private String xml(String value) {
        return nullToEmpty(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String snippet(String value, int limit) {
        String text = nullToEmpty(value).replaceAll("\\s+", " ").trim();
        return text.length() > limit ? text.substring(0, limit) + "..." : text;
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    @FunctionalInterface
    private interface StepCall {
        HttpResponse call() throws IOException;
    }

    private record HttpResponse(int statusCode, String body) {
    }
}
