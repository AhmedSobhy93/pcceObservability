const state = {
    summary: null,
    calls: [],
    callTypes: [],
    agents: [],
    drops: [],
    ivr: [],
    cvpIvrNodes: [],
    components: [],
    serverMetrics: [],
    assessment: null,
    user: null,
    queryPerformance: [],
    logs: [],
    pcceApiStatus: [],
    pcceCapabilities: [],
    pcceFunctions: [],
    pcceActions: [],
    rtmtCapabilities: [],
    spogCapabilities: [],
    smtpCapabilities: [],
    spogOps: [],
    serverLogTargets: [],
    grafanaDashboards: [],
    cuicReports: [],
    integrationCapabilities: [],
    finesseCapabilities: [],
    finesseSystem: [],
    finesseAgents: [],
    finesseDialogs: [],
    finesseTeams: [],
    finesseQueues: [],
    callFlow: [],
    skillGroups: [],
    callTypeOptions: [],
    agentOptions: [],
    adminUsers: [],
    adminRoles: [],
    adminComponents: [],
    notificationSettings: null
};

const pages = {
    overview: ["Dashboard Overview", "Cisco PCCE 12.6.2 - Real-time Contact Center Analytics"],
    business: ["Business Metrics", "Service level, handle time, IVR containment, and contact center KPIs"],
    agents: ["Agent Performance", "Workforce team and agent productivity view"],
    calls: ["Call Analytics", "Dropped calls, queue behavior, and skill group distribution"],
    system: ["System Health", "PCCE, CVP, CUIC, Finesse, PG, CTI, and gateway status"],
    eleveo: ["Eleveo QM & Recording", "Grafana monitoring for quality management and recording platforms"],
    integration: ["PCCE Integration", "AW/HDS SQL Server and CVP Informix connectivity"],
    smtp: ["Alerts", "Webhook, SMTP, SMS thresholds, escalation, and notification readiness"],
    spog: ["SPOG Operations", "Single pane operations, graceful actions, and log collection"],
    admin: ["Admin", "Users, role permissions, and runtime configuration"],
    app: ["Spring Boot App", "Operational alerts, readiness checks, and support status"],
    plan: ["Project Plan", "Production hardening and banking support checklist"]
};

const colors = ["#2ed3c2", "#3d82f6", "#f4a51c", "#8d6cf7", "#24e0a4", "#ff626c"];
const planState = { view: "tasks", topic: "ALL", collapsed: new Set() };
const permissionCatalog = [
    "CALL_METRICS_READ",
    "AGENT_STATS_READ",
    "DROPPED_CALLS_READ",
    "IVR_METRICS_READ",
    "COMPONENT_STATUS_READ",
    "OPERATIONS_READ",
    "SOLUTION_ADMIN"
];
const defaultPlanTasks = [
    { topic: "PCCE", task: "Finalize AW/HDS/CVP reporting alignment", priority: "CRITICAL", priorityNum: 1, status: "IN_PROGRESS", resource: "Contact Center Team / DB Admin", start: "1-Feb-26", finish: "30-Apr-26", duration: 89, pct: 65, comments: "Align dashboard numbers with CUIC stock reports and Cisco schema." },
    { topic: "PCCE", task: "PCCE API enablement and permissions", priority: "HIGH", priorityNum: 2, status: "IN_PROGRESS", resource: "Cisco Admin & App Owner", start: "10-Feb-26", finish: "15-Mar-26", duration: 34, pct: 55, comments: "Validate IIS auth, API user role, and supported actions." },
    { topic: "PCCE", task: "RTMT-style component monitoring", priority: "HIGH", priorityNum: 3, status: "PLANNED", resource: "Infrastructure Team", start: "1-Mar-26", finish: "30-Apr-26", duration: 60, pct: 25, comments: "SNMP/WMI/exporter required for CPU, memory, disk, and services." },
    { topic: "Eleveo", task: "Embed QM Grafana dashboards", priority: "HIGH", priorityNum: 1, status: "IN_PROGRESS", resource: "Eleveo Admin / App Owner", start: "15-Feb-26", finish: "15-Mar-26", duration: 29, pct: 45, comments: "Needs Grafana iframe URLs and allow_embedding enabled." },
    { topic: "Eleveo", task: "Recording health and storage dashboard", priority: "CRITICAL", priorityNum: 2, status: "PLANNED", resource: "Recording Team & Storage Admin", start: "1-Mar-26", finish: "20-Mar-26", duration: 20, pct: 15, comments: "Monitor recorder services, archive backlog, free storage, and failed recordings." },
    { topic: "Dtech", task: "SPOG support runbook", priority: "MEDIUM", priorityNum: 1, status: "PLANNED", resource: "Dtech / Operations", start: "15-Mar-26", finish: "30-Apr-26", duration: 46, pct: 10, comments: "Define approved graceful operations and escalation flow." },
    { topic: "Cisco Portal", task: "Cisco entitlement and TAC documentation", priority: "MEDIUM", priorityNum: 1, status: "ON_HOLD", resource: "Cisco Admin", start: null, finish: null, duration: null, pct: 20, comments: "Waiting for support contract and portal access validation." },
    { topic: "Survey", task: "CSAT feed integration", priority: "LOW", priorityNum: 1, status: "PLANNED", resource: "Business Team", start: "1-Apr-26", finish: "30-Apr-26", duration: 30, pct: 0, comments: "Source system and field mapping not finalized." },
    { topic: "Chat", task: "Digital channel reporting placeholders", priority: "LOW", priorityNum: 1, status: "PLANNED", resource: "Digital Channels", start: "1-Apr-26", finish: "30-Apr-26", duration: 30, pct: 0, comments: "Prepare model for future chat metrics." },
    { topic: "One Content", task: "Archive evidence and operations documents", priority: "MEDIUM", priorityNum: 1, status: "COMPLETED", resource: "App Owner & Operations", start: "1-Feb-26", finish: "20-Feb-26", duration: 20, pct: 100, comments: "Initial runbook and references captured." }
];
let planTasks = loadPlanTasks();

document.addEventListener("DOMContentLoaded", () => {
    const today = new Date().toISOString().slice(0, 10);
    qs("#fromDate").value = today;
    qs("#toDate").value = today;
    qs("#refreshBtn").addEventListener("click", refresh);
    qs("#agentFilterBtn")?.addEventListener("click", refresh);
    qs("#callsFilterBtn")?.addEventListener("click", refresh);
    qs("#adminQuickBtn")?.addEventListener("click", () => switchView("admin"));
    qs("#adminSaveUserBtn")?.addEventListener("click", saveAdminUser);
    qs("#adminSaveRoleBtn")?.addEventListener("click", saveAdminRole);
    qs("#saveAlertConfigBtn")?.addEventListener("click", saveAlertConfig);
    qs("#planAddTaskBtn")?.addEventListener("click", addPlanTask);
    qs("#planResetBtn")?.addEventListener("click", resetPlanTasks);
    qs("#adminUsername")?.addEventListener("change", fillAdminUserForm);
    qs("#adminRoleSelect")?.addEventListener("change", renderPermissionEditor);
    qs("#taskListMode")?.addEventListener("click", () => setPlanView("tasks"));
    qs("#resourceMode")?.addEventListener("click", () => setPlanView("resources"));
    ["#planTopicFilter", "#planStatusFilter", "#planPriorityFilter", "#planResourceFilter"].forEach(selector => {
        qs(selector)?.addEventListener("change", renderPlan);
    });
    qs("#collapseBtn").addEventListener("click", () => document.body.classList.toggle("collapsed"));
    qs("#logoutBtn").addEventListener("click", logout);
    document.querySelectorAll(".nav-item[data-view]").forEach(button => {
        button.addEventListener("click", () => switchView(button.dataset.view));
    });
    refresh();
});

function qs(selector) {
    return document.querySelector(selector);
}

function num(value) {
    if (value === null || value === undefined || Number.isNaN(Number(value))) return 0;
    return Number(value);
}

function pick(object, ...names) {
    for (const name of names) {
        if (object && object[name] !== undefined && object[name] !== null) return object[name];
    }
    return null;
}

function fmt(value) {
    return new Intl.NumberFormat().format(Math.round(num(value)));
}

function pct(value) {
    if (value === null || value === undefined) return "--";
    return `${Number(value).toFixed(1)}%`;
}

function seconds(value) {
    if (value === null || value === undefined) return "--";
    return `${Math.round(num(value))}`;
}

function metricSeconds(value) {
    return value === null || value === undefined ? "--" : `${seconds(value)} sec`;
}

function dateParams() {
    const params = new URLSearchParams({
        from: qs("#fromDate").value,
        to: qs("#toDate").value
    });
    const skill = firstFilterValue("#callsSkillFilter", "#businessSkillFilter", "#overviewSkillFilter");
    const agent = firstFilterValue("#agentPageFilter");
    if (skill) params.set("skillGroup", skill);
    if (agent) params.set("agentId", agent);
    return params.toString();
}

function callTypeParams() {
    const params = new URLSearchParams({
        from: qs("#fromDate").value,
        to: qs("#toDate").value
    });
    const skill = firstFilterValue("#callsSkillFilter", "#overviewSkillFilter");
    const callType = firstFilterValue("#callsCallTypeFilter");
    if (skill) params.set("skillGroup", skill);
    if (callType) params.set("callType", callType);
    return params.toString();
}

function agentParams() {
    const params = new URLSearchParams({
        from: qs("#fromDate").value,
        to: qs("#toDate").value
    });
    const agent = firstFilterValue("#agentPageFilter");
    const team = firstFilterValue("#agentTeamInput");
    if (agent) params.set("agentId", agent);
    if (team) params.set("team", team);
    return params.toString();
}

function callFlowParams() {
    const params = new URLSearchParams({
        from: qs("#fromDate").value,
        to: qs("#toDate").value
    });
    const callKey = firstFilterValue("#callKeyFilter");
    const agent = firstFilterValue("#agentPageFilter");
    if (callKey) params.set("callKey", callKey);
    if (agent) params.set("agentId", agent);
    return params.toString();
}

function ivrNodeParams() {
    const params = new URLSearchParams({
        from: qs("#fromDate").value,
        to: qs("#toDate").value
    });
    const appName = firstFilterValue("#ivrAppFilter");
    if (appName) params.set("appName", appName);
    return params.toString();
}

function firstFilterValue(...selectors) {
    for (const selector of selectors) {
        const element = qs(selector);
        const value = element?.value?.trim();
        if (value) return value;
    }
    return "";
}

async function api(path, options = {}) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), options.timeoutMs || 12000);
    const response = await fetch(path, {
        credentials: "same-origin",
        method: options.method || "GET",
        headers: options.body ? { "Content-Type": "application/json" } : undefined,
        body: options.body,
        signal: controller.signal
    }).finally(() => clearTimeout(timeout));
    if (!response.ok) {
        const text = await response.text();
        throw new Error(`${response.status} ${response.statusText}: ${text.slice(0, 220)}`);
    }
    return response.json();
}

async function safeLoad(key, path, fallback, options = {}) {
    try {
        state[key] = await api(path, options);
        return null;
    } catch (error) {
        state[key] = fallback;
        return `${key}: ${error.name === "AbortError" ? "request timed out" : error.message}`;
    }
}

async function logout() {
    try {
        await fetch("/logout", { method: "POST", credentials: "same-origin" });
    } finally {
        location.href = "/logout.html";
    }
}

async function refresh() {
    setStatus([{ text: "Refreshing live data...", level: "neutral" }]);
    const params = dateParams();
    const errors = [];
    const coreLoads = [
        safeLoad("user", "/api/v1/auth/me", null),
        safeLoad("calls", `/api/v1/metrics/calls?${params}`, [], { timeoutMs: 15000 }),
        safeLoad("callTypes", `/api/v1/metrics/call-types?${callTypeParams()}`, [], { timeoutMs: 15000 }),
        safeLoad("callFlow", `/api/v1/calls/flow?${callFlowParams()}`, [], { timeoutMs: 15000 }),
        safeLoad("agents", `/api/v1/agents/stats?${agentParams()}`, [], { timeoutMs: 15000 }),
        safeLoad("drops", `/api/v1/calls/dropped?${params}`, []),
        safeLoad("ivr", `/api/v1/metrics/ivr-containment?${params}`, []),
        safeLoad("cvpIvrNodes", `/api/v1/metrics/cvp-ivr-nodes?${ivrNodeParams()}`, [], { timeoutMs: 18000 }),
        safeLoad("components", "/api/v1/components/status", [], { timeoutMs: 16000 }),
        safeLoad("serverMetrics", "/api/v1/components/server-metrics", [], { timeoutMs: 8000 })
    ];
    const coreResults = await Promise.all(coreLoads);
    errors.push(...coreResults.filter(Boolean));
    renderAll(errors);

    const supportLoads = [
        safeLoad("assessment", "/api/v1/operations/assessment/last", null, { timeoutMs: 8000 }),
        safeLoad("queryPerformance", "/api/v1/monitoring/query-performance?limit=50", []),
        safeLoad("pcceApiStatus", "/api/v1/pcce-api/status", []),
        safeLoad("pcceCapabilities", "/api/v1/pcce-api/capabilities", []),
        safeLoad("pcceFunctions", "/api/v1/pcce-api/functions", []),
        safeLoad("pcceActions", "/api/v1/pcce-api/actions", []),
        safeLoad("rtmtCapabilities", "/api/v1/pcce-api/rtmt-capabilities", []),
        safeLoad("spogCapabilities", "/api/v1/pcce-api/spog-capabilities", []),
        safeLoad("smtpCapabilities", "/api/v1/operations/smtp-capabilities", []),
        safeLoad("spogOps", "/api/v1/operations/spog-capabilities", []),
        safeLoad("serverLogTargets", "/api/v1/operations/server-log-targets", []),
        safeLoad("grafanaDashboards", "/api/v1/integrations/grafana/dashboards", []),
        safeLoad("cuicReports", "/api/v1/cuic/reports", []),
        safeLoad("integrationCapabilities", "/api/v1/integrations/capabilities", []),
        safeLoad("finesseCapabilities", "/api/v1/finesse/capabilities", []),
        safeLoad("finesseSystem", "/api/v1/finesse/system", [], { timeoutMs: 12000 }),
        safeLoad("finesseAgents", "/api/v1/finesse/agents", [], { timeoutMs: 14000 }),
        safeLoad("finesseDialogs", "/api/v1/finesse/dialogs", [], { timeoutMs: 14000 }),
        safeLoad("finesseTeams", "/api/v1/finesse/teams", [], { timeoutMs: 14000 }),
        safeLoad("finesseQueues", "/api/v1/finesse/queues", [], { timeoutMs: 14000 }),
        safeLoad("skillGroups", "/api/v1/reference/skill-groups", []),
        safeLoad("callTypeOptions", "/api/v1/reference/call-types", []),
        safeLoad("agentOptions", "/api/v1/reference/agents", [])
    ];
    const results = await Promise.all(supportLoads);
    errors.push(...results.filter(Boolean));
    if (hasPermission("SOLUTION_ADMIN")) {
        const logError = await safeLoad("logs", "/api/v1/monitoring/logs?limit=80", []);
        if (logError) errors.push(logError);
        const adminResults = await Promise.all([
            safeLoad("adminUsers", "/api/v1/admin/users", []),
            safeLoad("adminRoles", "/api/v1/admin/roles", []),
            safeLoad("adminComponents", "/api/v1/admin/components", []),
            safeLoad("notificationSettings", "/api/v1/admin/notifications", null),
            safeLoad("serverLogTargets", "/api/v1/admin/server-log-targets", state.serverLogTargets)
        ]);
        errors.push(...adminResults.filter(Boolean));
    } else {
        state.logs = ["Log tail is available for administrators."];
        state.adminUsers = [];
        state.adminRoles = [];
        state.adminComponents = [];
    }
    renderAll(errors);
}

function setStatus(items) {
    qs("#statusStrip").innerHTML = items.map(item => `<div class="status-pill ${item.level}">${escapeHtml(item.text)}</div>`).join("");
}

function renderAll(errors) {
    renderKpis();
    renderCharts();
    renderBusiness();
    renderAgents();
    renderFinesse();
    renderDrops();
    renderCallTypes();
    renderComponents();
    renderReferenceOptions();
    renderIntegration();
    renderSmtp();
    renderSpog();
    renderEleveo();
    renderOperations();
    renderSupport();
    renderAdmin();
    renderPlan();

    const componentDown = state.components.filter(c => String(pick(c, "state")).toUpperCase() === "DOWN").length;
    const alertCount = pick(state.assessment, "alerts")?.length || 0;
    const items = [];
    items.push({ text: errors.length ? `${errors.length} data warnings` : "Live data loaded", level: errors.length ? "warn" : "good" });
    if (state.calls.some(row => isUnknownLabel(pick(row, "skill_group", "skillGroup")))) {
        items.push({ text: "CUIC alignment needed: skill/call filters not mapped", level: "warn" });
    }
    items.push({ text: `${componentDown} components down`, level: componentDown ? "bad" : "good" });
    items.push({ text: `${alertCount} operational alerts`, level: alertCount ? "warn" : "good" });
    if (errors[0]) items.push({ text: errors[0], level: "warn" });
    setStatus(items);
}

function renderKpis() {
    const offered = sum(state.calls, "calls_offered", "callsOffered");
    const handled = sum(state.calls, "calls_handled", "callsHandled");
    const abandoned = sum(state.calls, "calls_abandoned", "callsAbandoned");
    const dropped = sum(state.drops, "dropped_calls", "droppedCalls");
    const service = weightedAverage(state.calls, "service_level_pct", "serviceLevelPct", "calls_offered", "callsOffered");
    const aht = weightedAverage(state.calls, "avg_handle_time", "avgHandleTime", "calls_handled", "callsHandled");

    qs("#kpiOffered").textContent = fmt(offered);
    qs("#kpiHandled").textContent = fmt(handled);
    qs("#kpiAbandoned").textContent = fmt(abandoned);
    qs("#kpiDropped").textContent = fmt(dropped);
    qs("#trendDropped").textContent = dropped ? "Disposition based" : "Not configured";
    qs("#kpiService").textContent = pct(service);
    qs("#kpiAht").textContent = seconds(aht);
    qs("#trendService").textContent = service === null ? "Interval data unavailable" : "Target tracked";
    qs("#kpiAht").nextElementSibling.textContent = aht === null ? "Interval data unavailable" : "Seconds";
    qs("#chartRange").textContent = `${qs("#fromDate").value} to ${qs("#toDate").value}`;
    const skillLabel = firstFilterValue("#overviewSkillFilter", "#businessSkillFilter", "#callsSkillFilter");
    if (skillLabel) {
        qs("#trendOffered").textContent = `Skill: ${skillLabel}`;
        qs("#trendHandled").textContent = `Skill: ${skillLabel}`;
    } else {
        qs("#trendOffered").textContent = "Live HDS";
        qs("#trendHandled").textContent = "Live HDS";
    }
}

function renderCharts() {
    const hourly = groupByHour(state.calls);
    drawLineChart(qs("#volumeChart"), hourly.labels, [
        { label: "Offered", color: "#2ed3c2", values: hourly.offered },
        { label: "Handled", color: "#3d82f6", values: hourly.handled }
    ]);

    const skills = groupBySkill(state.calls, "calls_offered", "callsOffered");
    drawDoughnut(qs("#skillChart"), skills.labels, skills.values, colors);
    qs("#skillLegend").innerHTML = skills.labels.map((label, index) =>
        `<span><i class="dot" style="background:${colors[index % colors.length]}"></i>${escapeHtml(label)} <b>${fmt(skills.values[index])}</b></span>`
    ).join("");

    const ivr = (state.ivr || []).map(row => ({
        hour: pick(row, "hour"),
        value: pick(row, "ivr_containment_rate", "ivrContainmentRate")
    }));
    drawLineChart(qs("#ivrChart"), ivr.map(row => `${row.hour}:00`), [
        { label: "IVR", color: "#2ed3c2", values: ivr.map(row => num(row.value)) }
    ], 100);
}

function renderBusiness() {
    renderBusinessCards();
    renderCvpIvrNodes();
    const rows = [
        ["Calls Offered", fmt(sum(state.calls, "calls_offered", "callsOffered"))],
        ["Calls Handled", fmt(sum(state.calls, "calls_handled", "callsHandled"))],
        ["Calls Abandoned", fmt(sum(state.calls, "calls_abandoned", "callsAbandoned"))],
        ["Dropped Calls", fmt(sum(state.drops, "dropped_calls", "droppedCalls"))],
        ["Service Level", pct(weightedAverage(state.calls, "service_level_pct", "serviceLevelPct", "calls_offered", "callsOffered"))],
        ["Average Speed Answer", metricSeconds(weightedAverage(state.calls, "avg_speed_answer", "avgSpeedAnswer", "calls_handled", "callsHandled"))]
    ];
    qs("#businessMetrics").innerHTML = rows.map(([label, value]) => metricRow(label, value)).join("");

    const trend = serviceTrendByHour();
    drawLineChart(qs("#serviceTrendChart"), trend.labels, trend.series, 100);
    drawRadar(qs("#performanceRadar"), radarMetrics());
}

function renderCvpIvrNodes() {
    const count = qs("#cvpIvrNodeCount");
    const table = qs("#cvpIvrNodeTable");
    if (!count || !table) return;
    count.textContent = `${state.cvpIvrNodes.length} rows`;
    table.innerHTML = state.cvpIvrNodes.slice(0, 500).map(row => `<tr>
        <td>${escapeHtml(pick(row, "call_id", "callId") || "")}</td>
        <td>${escapeHtml(pick(row, "call_start_time", "callStartTime") || "")}</td>
        <td>${escapeHtml(pick(row, "caller_number", "callerNumber") || "")}</td>
        <td>${escapeHtml(pick(row, "app_name", "appName") || "")}</td>
        <td>${escapeHtml(pick(row, "duration") || "")}</td>
        <td>${escapeHtml(pick(row, "flag") || "")}</td>
        <td><span class="badge ${ivrDispositionClass(pick(row, "call_disposition_id", "callDispositionId"))}">${escapeHtml(pick(row, "call_disposition_flag_desc", "callDispositionFlagDesc") || "")}</span></td>
    </tr>`).join("") || `<tr><td colspan="7">No CVP IVR node rows for selected dates/app.</td></tr>`;
}

function ivrDispositionClass(code) {
    const value = Number(code);
    if ([18, 1001, 1044].includes(value)) return "down";
    if (value === 2) return "warn";
    return "up";
}

function renderAgents() {
    renderAgentFilters();
    const selectedTeam = document.querySelector(".team-filter.active")?.dataset.team || "ALL";
    const agents = selectedTeam === "ALL"
        ? state.agents
        : state.agents.filter(agent => (pick(agent, "team") || "UNKNOWN") === selectedTeam);
    qs("#agentCount").textContent = `${agents.length} rows`;
    qs("#agentsTable").innerHTML = agents.slice(0, 300).map(agent => {
        const status = String(pick(agent, "status") || "offline");
        const occupancy = pick(agent, "occupancy_pct", "occupancyPct");
        const adherence = pick(agent, "adherence_pct", "adherencePct");
        return `<tr>
            <td><strong>${escapeHtml(pick(agent, "agent_name", "agentName") || "")}</strong><span class="subline">${escapeHtml(pick(agent, "agent_id", "agentId") || "")} - ${escapeHtml(pick(agent, "team") || "UNKNOWN")}</span></td>
            <td><span class="badge ${status}">${escapeHtml(status.replace("_", " "))}</span></td>
            <td>${fmt(pick(agent, "calls_handled", "callsHandled"))}</td>
            <td>${seconds(pick(agent, "avg_handle_time", "avgHandleTime"))}</td>
            <td>${progressCell(occupancy)}</td>
            <td>${progressCell(adherence)}</td>
            <td>${fmt(pick(agent, "transfers"))}</td>
            <td>${minutes(pick(agent, "not_ready_time_min", "notReadyTimeMin"))}</td>
        </tr>`;
    }).join("");
}

function renderFinesse() {
    const agentGrid = qs("#finesseAgentGrid");
    if (!agentGrid) return;
    qs("#finesseAgentCount").textContent = `${state.finesseAgents.length} configured users`;
    qs("#finesseDialogCount").textContent = `${state.finesseDialogs.length} dialog probes`;
    agentGrid.innerHTML = state.finesseAgents.map(endpointCard).join("") ||
        `<article class="component-card"><h3>Finesse Agents</h3><span class="badge warn">not configured</span><p>Set FINESSE_ENABLED=true and FINESSE_USER_IDS or app user agent IDs.</p></article>`;
    qs("#finesseDialogList").innerHTML = state.finesseDialogs.map(item =>
        metricRow(`${pick(item, "name")} - HTTP ${pick(item, "status_code", "statusCode") || 0}`,
            `${fmt(pick(item, "latency_ms", "latencyMs"))} ms | ${snippet(pick(item, "body"), 180)}`)
    ).join("") || metricRow("Dialogs", "No configured Finesse user IDs");
    const teamQueueItems = [...state.finesseTeams, ...state.finesseQueues];
    qs("#finesseTeamQueueList").innerHTML = teamQueueItems.map(item =>
        metricRow(`${pick(item, "name")} - HTTP ${pick(item, "status_code", "statusCode") || 0}`,
            `${fmt(pick(item, "latency_ms", "latencyMs"))} ms | ${snippet(pick(item, "body"), 180)}`)
    ).join("") || metricRow("Teams / Queues", "Configure FINESSE_TEAM_IDS and FINESSE_QUEUE_IDS if needed");
    qs("#finesseSystemList").innerHTML = state.finesseSystem.map(item =>
        metricRow(`${pick(item, "name")} - HTTP ${pick(item, "status_code", "statusCode") || 0}`,
            `${fmt(pick(item, "latency_ms", "latencyMs"))} ms | ${snippet(pick(item, "body"), 200)}`)
    ).join("") || metricRow("SystemInfo", "Finesse system endpoint not loaded");
}

function renderDrops() {
    if (!qs("#dropList")) return;
    const grouped = groupBySkill(state.drops, "dropped_calls", "droppedCalls");
    qs("#dropList").innerHTML = grouped.labels.map((label, index) => metricRow(label, fmt(grouped.values[index]))).join("") || metricRow("Dropped Calls", "0");
}

function renderCallTypes() {
    renderCallFunnel();
    renderCallFlow();
    qs("#callTypeCount").textContent = `${state.callTypes.length} rows`;
    qs("#callTypeTable").innerHTML = state.callTypes.slice(0, 300).map(row => `<tr>
        <td>${escapeHtml(pick(row, "date") || "")}</td>
        <td>${escapeHtml(pick(row, "hour") ?? "")}</td>
        <td>${escapeHtml(pick(row, "call_type", "callType") || "")}</td>
        <td>${escapeHtml(pick(row, "skill_group", "skillGroup") || "")}</td>
        <td>${fmt(pick(row, "calls"))}</td>
        <td>${fmt(pick(row, "handled_calls", "handledCalls"))}</td>
    </tr>`).join("");
    const abandonment = abandonmentByHour();
    drawLineChart(qs("#abandonmentChart"), abandonment.labels, abandonment.series, 100);
    const queue = queueByHour();
    drawBarChart(qs("#queueChart"), queue.labels, queue.values, "#f4a51c");
    if (!queue.hasData) {
        const ctx = qs("#queueChart")?.getContext("2d");
        if (ctx) {
            const rect = qs("#queueChart").getBoundingClientRect();
            ctx.fillStyle = "#87a1c2";
            ctx.font = "16px Segoe UI";
            ctx.fillText("Queue wait data unavailable from current HDS query", 30, rect.height / 2);
        }
    }
}

function renderCallFlow() {
    qs("#callFlowCount").textContent = `${state.callFlow.length} events`;
    qs("#callFlowTimeline").innerHTML = state.callFlow.slice(0, 80).map(event => `<div class="trace-event">
        <div class="trace-dot"></div>
        <div>
            <strong>${escapeHtml(pick(event, "stage") || "")}</strong>
            <span>${escapeHtml(pick(event, "event_time", "eventTime") || "")} | ${escapeHtml(pick(event, "node") || "")} | ${escapeHtml(pick(event, "call_key", "callKey") || "")}</span>
            <p>${escapeHtml(pick(event, "call_type", "callType") || "")} / ${escapeHtml(pick(event, "skill_group", "skillGroup") || "")} / ${escapeHtml(pick(event, "agent") || "No agent")}</p>
            <small>${escapeHtml(pick(event, "detail") || "")}</small>
        </div>
    </div>`).join("") || `<div class="empty-state"><strong>No call flow events</strong><span>Enter a call key or widen the selected date range.</span></div>`;
}

function renderComponents() {
    const down = state.components.filter(c => String(pick(c, "state")).toUpperCase() === "DOWN").length;
    const up = state.components.filter(c => String(pick(c, "state")).toUpperCase() === "UP").length;
    const warn = state.components.filter(c => String(pick(c, "state")).toUpperCase() === "DISABLED").length;
    const avgLatency = state.components.length
        ? state.components.reduce((total, c) => total + num(pick(c, "latency_ms", "latencyMs")), 0) / state.components.length
        : 0;
    qs("#systemKpis").innerHTML = [
        systemKpi("Components", state.components.length, `${up} OK   ${warn} Disabled   ${down} Down`),
        systemKpi("Active Calls", fmt(sum(state.calls, "calls_handled", "callsHandled")), "Handled in selected range"),
        systemKpi("Alerts", pick(state.assessment, "alerts")?.length || 0, "Open operational alerts"),
        systemKpi("Avg Latency", `${Math.round(avgLatency)} ms`, "Current probe average")
    ].join("");
    qs("#componentSummary").textContent = `${state.components.length} components / ${down} down`;
    qs("#componentGrid").innerHTML = state.components.map(component => {
        const stateValue = String(pick(component, "state") || "UNKNOWN").toLowerCase();
        const badgeClass = stateValue === "up" ? "up" : stateValue === "down" ? "down" : "warn";
        const latency = num(pick(component, "latency_ms", "latencyMs"));
        const health = stateValue === "up" ? Math.max(5, Math.min(100, 100 - latency / 100)) : 0;
        return `<article class="component-card">
            <div class="component-row">
                <div>
                    <h3>${escapeHtml(pick(component, "name") || "")}</h3>
                    <span class="badge ${badgeClass}">${escapeHtml(stateValue)}</span>
                    <p>${escapeHtml(pick(component, "probe") || "")} - ${escapeHtml(pick(component, "target") || "")}</p>
                    <p>${fmt(latency)} ms - ${escapeHtml(pick(component, "detail") || "")}</p>
                    <p>${componentProbeHint(component)}</p>
                </div>
                <div class="ring" style="--p:${health}"><span>${Math.round(health)}%</span></div>
            </div>
        </article>`;
    }).join("");
    qs("#serverMetricGrid").innerHTML = state.serverMetrics.map(metric => {
        const stateValue = String(pick(metric, "state") || "UNKNOWN").toLowerCase();
        const badgeClass = stateValue === "up" ? "up" : stateValue === "not_configured" ? "warn" : "down";
        return `<article class="component-card">
            <h3>${escapeHtml(pick(metric, "component") || "")}</h3>
            <span class="badge ${badgeClass}">${escapeHtml(stateValue.replace("_", " "))}</span>
            <p>${escapeHtml(pick(metric, "host") || "")} - ${escapeHtml(pick(metric, "method") || "")}</p>
            <p>CPU ${metricPct(pick(metric, "cpu_pct", "cpuPct"))} | Memory ${metricPct(pick(metric, "memory_pct", "memoryPct"))} | Disk ${metricPct(pick(metric, "disk_pct", "diskPct"))}</p>
            <p>${escapeHtml(pick(metric, "services") || "")}</p>
            <p>${escapeHtml(pick(metric, "detail") || "")}</p>
        </article>`;
    }).join("") || `<article class="component-card"><h3>Server Metrics</h3><span class="badge warn">not configured</span><p>Configure SNMP/WMI/exporter collection for remote servers.</p></article>`;
}

function componentProbeHint(component) {
    const probe = String(pick(component, "probe") || "").toUpperCase();
    const name = String(pick(component, "name") || "");
    if (probe === "HOST") return "Host reachability only; service port may still be down.";
    if (name === "CTI_Server") return "CTI requires TCP service reachability on the configured CTI port.";
    if (probe === "HTTP") return "HTTP OK means web endpoint responded within configured status range.";
    return "";
}

function renderReferenceOptions() {
    fillDatalist("#skillOptions", state.skillGroups);
    fillDatalist("#callTypeOptions", state.callTypeOptions);
    fillDatalist("#agentOptions", state.agentOptions);
}

function renderIntegration() {
    qs("#integrationList").innerHTML = [
        metricRow("AW Database", "SQL Server"),
        metricRow("HDS Database", "SQL Server"),
        metricRow("CVP Reporting", "IBM Informix"),
        metricRow("PCCE APIs", `${state.pcceApiStatus.length} monitors configured`),
        metricRow("Authentication", "Spring Security"),
        metricRow("API Docs", "/swagger-ui/index.html")
    ].join("");
    qs("#userBox").textContent = JSON.stringify(state.user || {}, null, 2);
    qs("#cuicReportList").innerHTML = state.cuicReports.map(report =>
        metricRow(`${pick(report, "mode")} - ${pick(report, "name")}`, `${pick(report, "source")} | ${pick(report, "endpoint")}`)
    ).join("") || metricRow("CUIC Stock Reports", "Mapping catalog unavailable");
    qs("#integrationCapabilityList").innerHTML = state.integrationCapabilities.map(item =>
        metricRow(`${pick(item, "area")} - ${pick(item, "capability")}`, `${pick(item, "method")} | ${pick(item, "status")} | ${pick(item, "action")}`)
    ).join("") || metricRow("Integrations", "Capability catalog unavailable");
    qs("#finesseIntegrationGrid").innerHTML = [...state.finesseSystem, ...state.finesseAgents, ...state.finesseDialogs].slice(0, 12)
        .map(endpointCard).join("") ||
        `<article class="component-card"><h3>Finesse REST</h3><span class="badge warn">disabled</span><p>Configure pcce.finesse to enable live Finesse API monitoring.</p></article>`;
    qs("#finesseCapabilityList").innerHTML = state.finesseCapabilities.map(item =>
        metricRow(`${pick(item, "area")} - ${pick(item, "capability")}`, `${pick(item, "method")} | ${pick(item, "status")} | ${pick(item, "action")}`)
    ).join("") || metricRow("Finesse", "Capability catalog unavailable");
    qs("#pcceApiGrid").innerHTML = state.pcceApiStatus.map(item => {
        const stateValue = String(pick(item, "state") || "UNKNOWN").toLowerCase();
        const badgeClass = stateValue === "up" ? "up" : stateValue === "down" ? "down" : "warn";
        const statusCode = num(pick(item, "status_code", "statusCode"));
        return `<article class="component-card">
            <h3>${escapeHtml(pick(item, "name") || "")}</h3>
            <span class="badge ${badgeClass}">${escapeHtml(stateValue)}</span>
            <p>${escapeHtml(pick(item, "category") || "")}</p>
            <p>${escapeHtml(pick(item, "method") || "GET")} - ${escapeHtml(pick(item, "target") || "")}</p>
            <p>${fmt(pick(item, "latency_ms", "latencyMs"))} ms - ${statusCode ? `HTTP ${statusCode}` : escapeHtml(pick(item, "detail") || "")}</p>
        </article>`;
    }).join("") || `<article class="component-card"><h3>PCCE API</h3><span class="badge warn">disabled</span><p>Configure pcce.pcce-api monitors to enable API surveillance.</p></article>`;

    qs("#pcceCapabilityList").innerHTML = state.pcceCapabilities.map(item =>
        metricRow(pick(item, "category"), pick(item, "capability"))
    ).join("");

    qs("#pcceFunctionTable").innerHTML = state.pcceFunctions.map(item => `<tr>
        <td>${escapeHtml(pick(item, "category") || "")}</td>
        <td>${escapeHtml(pick(item, "function") || "")}</td>
        <td><span class="badge up">${escapeHtml(pick(item, "method") || "GET")}</span></td>
        <td>${escapeHtml(pick(item, "path") || "")}</td>
        <td>${escapeHtml(pick(item, "description") || "")}</td>
    </tr>`).join("");

    qs("#pcceActionTable").innerHTML = state.pcceActions.map(item => {
        const enabled = Boolean(pick(item, "enabled"));
        const id = pick(item, "id") || "";
        return `<tr>
            <td>${escapeHtml(pick(item, "category") || "")}</td>
            <td>${escapeHtml(pick(item, "name") || id)}</td>
            <td><span class="badge ${enabled ? "up" : "warn"}">${escapeHtml(pick(item, "method") || "GET")}</span></td>
            <td>${escapeHtml(pick(item, "path") || "")}</td>
            <td>${enabled ? "Enabled" : "Disabled"}</td>
            <td><button class="small-btn" data-action-id="${escapeHtml(id)}" ${enabled ? "" : "disabled"}>Run</button></td>
        </tr>`;
    }).join("");
    document.querySelectorAll("[data-action-id]").forEach(button => {
        button.addEventListener("click", () => executePcceAction(button.dataset.actionId));
    });

    qs("#rtmtCapabilityList").innerHTML = state.rtmtCapabilities.map(item =>
        metricRow(`${pick(item, "area")}`, `${pick(item, "capability")} - ${pick(item, "integration_method", "integrationMethod")}`)
    ).join("");
    qs("#spogCapabilityList").innerHTML = state.spogCapabilities.map(item =>
        metricRow(`${pick(item, "area")}`, `${pick(item, "capability")} - ${pick(item, "integration_method", "integrationMethod")}`)
    ).join("");
}

function endpointCard(item) {
    const statusCode = num(pick(item, "status_code", "statusCode"));
    const ok = statusCode >= 200 && statusCode < 400;
    const disabled = statusCode === 0 && String(pick(item, "body") || "").toLowerCase().includes("disabled");
    const badgeClass = ok ? "up" : disabled ? "warn" : "down";
    return `<article class="component-card">
        <h3>${escapeHtml(pick(item, "name") || "")}</h3>
        <span class="badge ${badgeClass}">${statusCode ? `HTTP ${statusCode}` : disabled ? "disabled" : "error"}</span>
        <p>${escapeHtml(pick(item, "method") || "GET")} - ${escapeHtml(pick(item, "target") || "")}</p>
        <p>${fmt(pick(item, "latency_ms", "latencyMs"))} ms</p>
        <pre class="body-preview">${escapeHtml(snippet(pick(item, "body"), 420))}</pre>
    </article>`;
}

function renderSmtp() {
    qs("#smtpCapabilityList").innerHTML = state.smtpCapabilities.map(item =>
        metricRow(`${pick(item, "area")} - ${pick(item, "capability")}`, `${pick(item, "status")} | ${pick(item, "action")}`)
    ).join("") || metricRow("SMTP", "Capabilities unavailable");
    qs("#smtpStatusList").innerHTML = [
        metricRow("SMTP", state.notificationSettings?.smtp_enabled || state.notificationSettings?.smtpEnabled ? "enabled" : "disabled"),
        metricRow("SMS", state.notificationSettings?.sms_enabled || state.notificationSettings?.smsEnabled ? "enabled" : "disabled"),
        metricRow("SMS Gateway", "D-Tech SMS microgateway with X-Correlation-Id, User-Agent, Basic Authorization"),
        metricRow("Minimum Severity", pick(state.notificationSettings, "minimum_severity", "minimumSeverity") || "CRITICAL"),
        metricRow("SMS Severity", pick(state.notificationSettings, "sms_minimum_severity", "smsMinimumSeverity") || "CRITICAL"),
        metricRow("SMS Rate Guard", `${pick(state.notificationSettings, "sms_max_alerts_per_assessment", "smsMaxAlertsPerAssessment") || 5} max / assessment`)
    ].join("");
    fillAlertConfigForm();
}

function renderSpog() {
    qs("#spogOpsList").innerHTML = state.spogOps.map(item =>
        metricRow(`${pick(item, "area")} - ${pick(item, "capability")}`, `${pick(item, "status")} | ${pick(item, "action")}`)
    ).join("") || metricRow("SPOG", "Capabilities unavailable");
    qs("#gracefulOpsList").innerHTML = [
        metricRow("Maintenance Mode", pick(state.assessment, "maintenance_mode", "maintenanceMode") ? "Enabled" : "Disabled"),
        metricRow("Alert Suppression", "Available through maintenance mode"),
        metricRow("Graceful App Shutdown", "Admin gated; enable only during approved window"),
        metricRow("Remote Cisco Service Actions", "Requires approved automation account and runbook")
    ].join("");
    qs("#serverLogTargetsTable").innerHTML = state.serverLogTargets.map(item => `<tr>
        <td>${escapeHtml(pick(item, "component") || "")}</td>
        <td><input class="inline-input" data-log-field="host" data-log-component="${escapeHtml(pick(item, "component") || "")}" value="${escapeHtml(pick(item, "host") || "")}"></td>
        <td><input class="inline-input" data-log-field="logPath" data-log-component="${escapeHtml(pick(item, "component") || "")}" value="${escapeHtml(pick(item, "log_path", "logPath") || "")}"></td>
        <td><input class="inline-input" data-log-field="collectionMethod" data-log-component="${escapeHtml(pick(item, "component") || "")}" value="${escapeHtml(pick(item, "collection_method", "collectionMethod") || "")}"></td>
        <td><span class="badge ${pick(item, "enabled") ? "up" : "warn"}">${pick(item, "enabled") ? "Enabled" : "Disabled"}</span></td>
        <td><button class="small-btn" data-log-save="${escapeHtml(pick(item, "component") || "")}">${pick(item, "enabled") ? "Save" : "Enable"}</button></td>
    </tr>`).join("");
    document.querySelectorAll("[data-log-save]").forEach(button => {
        button.addEventListener("click", () => saveServerLogTarget(button.dataset.logSave));
    });
}

function fillAlertConfigForm() {
    const settings = state.notificationSettings;
    if (!settings || !qs("#alertSmtpEnabled")) return;
    qs("#alertSmtpEnabled").checked = Boolean(pick(settings, "smtp_enabled", "smtpEnabled"));
    qs("#alertSmtpFrom").value = pick(settings, "smtp_from", "smtpFrom") || "";
    qs("#alertSmtpRecipients").value = (pick(settings, "smtp_recipients", "smtpRecipients") || []).join(", ");
    qs("#alertSubjectPrefix").value = pick(settings, "smtp_subject_prefix", "smtpSubjectPrefix") || "";
    qs("#alertSmsEnabled").checked = Boolean(pick(settings, "sms_enabled", "smsEnabled"));
    qs("#alertSmsUrl").placeholder = pick(settings, "sms_url_configured", "smsUrlConfigured") ? "configured; enter value to replace" : "D-Tech SMS gateway URL";
    qs("#alertSmsUserAgent").value = pick(settings, "sms_user_agent", "smsUserAgent") || "";
    qs("#alertSmsRecipients").value = (pick(settings, "sms_recipients", "smsRecipients") || []).join(", ");
    qs("#alertMinimumSeverity").value = pick(settings, "minimum_severity", "minimumSeverity") || "CRITICAL";
    qs("#alertSmsMinimumSeverity").value = pick(settings, "sms_minimum_severity", "smsMinimumSeverity") || "CRITICAL";
    qs("#alertSmsMax").value = pick(settings, "sms_max_alerts_per_assessment", "smsMaxAlertsPerAssessment") || 5;
}

async function saveAlertConfig() {
    qs("#alertConfigResult").textContent = "Saving alert settings...";
    try {
        const body = {
            smtpEnabled: qs("#alertSmtpEnabled").checked,
            smtpFrom: qs("#alertSmtpFrom").value.trim(),
            smtpRecipients: splitCsv(qs("#alertSmtpRecipients").value),
            smtpSubjectPrefix: qs("#alertSubjectPrefix").value.trim(),
            smsEnabled: qs("#alertSmsEnabled").checked,
            smsUrl: qs("#alertSmsUrl").value.trim() || null,
            smsAuthorization: qs("#alertSmsAuthorization").value.trim() || null,
            smsUserAgent: qs("#alertSmsUserAgent").value.trim(),
            smsRecipients: splitCsv(qs("#alertSmsRecipients").value),
            minimumSeverity: qs("#alertMinimumSeverity").value,
            smsMinimumSeverity: qs("#alertSmsMinimumSeverity").value,
            smsMaxAlertsPerAssessment: Number(qs("#alertSmsMax").value || 5)
        };
        state.notificationSettings = await api("/api/v1/admin/notifications", {
            method: "PUT",
            body: JSON.stringify(body)
        });
        qs("#alertSmsAuthorization").value = "";
        qs("#alertConfigResult").textContent = "Alert settings saved. Persist final values in env/YAML for production restart.";
        renderSmtp();
    } catch (error) {
        qs("#alertConfigResult").textContent = error.message;
    }
}

async function saveServerLogTarget(component) {
    try {
        const fields = [...document.querySelectorAll("[data-log-component]")]
            .filter(input => input.dataset.logComponent === component);
        const payload = { enabled: true };
        fields.forEach(input => payload[input.dataset.logField] = input.value.trim());
        const updated = await api(`/api/v1/admin/server-log-targets/${encodeURIComponent(component)}`, {
            method: "PUT",
            body: JSON.stringify(payload)
        });
        state.serverLogTargets = state.serverLogTargets.map(item =>
            String(pick(item, "component")).toLowerCase() === component.toLowerCase() ? updated : item);
        renderSpog();
    } catch (error) {
        alert(error.message);
    }
}

function renderEleveo() {
    const enabled = state.grafanaDashboards.filter(item => pick(item, "enabled") && pick(item, "url"));
    qs("#grafanaSummary").textContent = `${enabled.length} embedded dashboards`;
    qs("#grafanaDashboardGrid").innerHTML = enabled.map(item => `<article class="grafana-card">
        <div class="panel-head"><h2>${escapeHtml(pick(item, "name") || "")}</h2><span>${escapeHtml(pick(item, "area") || "")}</span></div>
        <p>${escapeHtml(pick(item, "description") || "")}</p>
        <iframe src="${escapeHtml(pick(item, "url") || "")}" loading="lazy" referrerpolicy="no-referrer"></iframe>
    </article>`).join("") || `<div class="empty-state">
        <strong>No Eleveo Grafana panels configured</strong>
        <span>Set ELEVEO_GRAFANA_ENABLED=true and provide iframe/share URLs for QM and Recording dashboards.</span>
    </div>`;
    qs("#grafanaChecklist").innerHTML = [
        metricRow("Grafana embedding", "Set allow_embedding=true on Eleveo Grafana"),
        metricRow("Authentication", "Use anonymous/viewer auth, SSO, or reverse proxy session"),
        metricRow("Browser policy", "Check X-Frame-Options and Content-Security-Policy"),
        metricRow("URLs", "Use panel/d-solo iframe links for stable layout")
    ].join("");
}

async function executePcceAction(id) {
    qs("#pcceActionResult").textContent = `Running ${id}...`;
    try {
        const bodyText = qs("#pcceActionBody").value.trim();
        const pathParams = parseJsonObject(qs("#pccePathParams").value.trim(), "Path params");
        const queryParams = parseJsonObject(qs("#pcceQueryParams").value.trim(), "Query params");
        const result = await api(`/api/v1/pcce-api/actions/${encodeURIComponent(id)}/execute`, {
            method: "POST",
            body: JSON.stringify({ body: bodyText || null, pathParams, queryParams })
        });
        qs("#pcceActionResult").textContent = JSON.stringify(result, null, 2);
    } catch (error) {
        qs("#pcceActionResult").textContent = error.message;
    }
}

function parseJsonObject(text, label) {
    if (!text) return {};
    const parsed = JSON.parse(text);
    if (!parsed || Array.isArray(parsed) || typeof parsed !== "object") {
        throw new Error(`${label} must be a JSON object`);
    }
    return parsed;
}

function renderOperations() {
    const alerts = pick(state.assessment, "alerts") || [];
    const readiness = pick(state.assessment, "readiness") || [];
    qs("#alertList").innerHTML = alerts.map(alert => `<div class="alert-row">
        <div><strong>${escapeHtml(pick(alert, "severity"))} - ${escapeHtml(pick(alert, "category"))}</strong><span>${escapeHtml(pick(alert, "message"))}</span></div>
        <span>${escapeHtml(pick(alert, "status"))}</span>
    </div>`).join("") || `<div class="alert-row"><div><strong>No active alerts</strong><span>Assessment is clean for the selected interval.</span></div><span>OK</span></div>`;

    qs("#readinessList").innerHTML = readiness.map(item =>
        metricRow(`${pick(item, "ready") ? "OK" : "!"} ${pick(item, "area")}`, pick(item, "finding"))
    ).join("");
}

function renderSupport() {
    qs("#queryPerformanceList").innerHTML = state.queryPerformance.slice(0, 20).map(item => {
        const ok = pick(item, "success");
        const elapsed = `${fmt(pick(item, "elapsed_ms", "elapsedMs"))} ms`;
        const name = pick(item, "name") || "query";
        const error = pick(item, "error");
        return `<div class="metric-row ${ok ? "" : "error-row"}">
            <span>${escapeHtml(name)}${error ? `<small>${escapeHtml(String(error).slice(0, 120))}</small>` : ""}</span>
            <strong>${escapeHtml(elapsed)}</strong>
        </div>`;
    }).join("") || metricRow("No query samples yet", "Run refresh");

    qs("#logBox").textContent = (state.logs || []).slice(-80).join("\n") || "No log lines found yet.";
}

function renderAdmin() {
    if (!hasPermission("SOLUTION_ADMIN")) {
        qs("#adminUserKpi").textContent = "--";
        qs("#adminRoleKpi").textContent = "--";
        qs("#adminComponentKpi").textContent = "--";
        qs("#adminAccessKpi").textContent = "Denied";
        qs("#adminUserCount").textContent = "Admin permission required";
        qs("#adminUsersTable").innerHTML = "";
        qs("#adminRolesList").innerHTML = metricRow("Access", "PERM_SOLUTION_ADMIN required");
        qs("#adminComponentsTable").innerHTML = "";
        qs("#adminPermissionGrid").innerHTML = "";
        qs("#adminCapabilityCards").innerHTML = adminCapabilityCards();
        return;
    }
    qs("#adminUserKpi").textContent = fmt(state.adminUsers.length);
    qs("#adminRoleKpi").textContent = fmt(state.adminRoles.length);
    qs("#adminComponentKpi").textContent = fmt(state.adminComponents.length);
    qs("#adminAccessKpi").textContent = "Enabled";
    qs("#adminUserCount").textContent = `${state.adminUsers.length} users`;
    fillDatalist("#adminUserOptions", state.adminUsers.map(user => ({
        value: pick(user, "username"),
        label: pick(user, "display_name", "displayName") || pick(user, "username"),
        detail: (pick(user, "roles") || []).join(", ")
    })));
    qs("#adminUsersTable").innerHTML = state.adminUsers.map(user => `<tr>
        <td>${escapeHtml(pick(user, "username") || "")}</td>
        <td>${escapeHtml(pick(user, "display_name", "displayName") || "")}</td>
        <td>${escapeHtml((pick(user, "roles") || []).join(", "))}</td>
        <td>${escapeHtml((pick(user, "allowed_teams", "allowedTeams") || []).join(", "))}</td>
        <td><span class="badge ${pick(user, "enabled") ? "up" : "down"}">${pick(user, "enabled") ? "enabled" : "disabled"}</span></td>
    </tr>`).join("");
    qs("#adminRolesList").innerHTML = state.adminRoles.map(role =>
        metricRow(pick(role, "role") || "", (pick(role, "permissions") || []).join(", "))
    ).join("");
    populateAdminRoleSelect();
    renderPermissionEditor();
    qs("#adminComponentsTable").innerHTML = state.adminComponents.map(component => `<tr>
        <td>${escapeHtml(pick(component, "name") || "")}</td>
        <td>${pick(component, "enabled") ? "true" : "false"}</td>
        <td>${escapeHtml(pick(component, "probe") || "")}</td>
        <td>${escapeHtml(pick(component, "url") || pick(component, "host") || "")}${pick(component, "port") ? `:${pick(component, "port")}` : ""}</td>
        <td>${escapeHtml(pick(component, "timeout") || "")}</td>
        <td>${pick(component, "trust_all_certificates", "trustAllCertificates") ? "trust all" : "default"}</td>
    </tr>`).join("");
    qs("#adminCapabilityCards").innerHTML = adminCapabilityCards();
}

function populateAdminRoleSelect() {
    const select = qs("#adminRoleSelect");
    if (!select) return;
    const current = select.value || "ADMIN";
    const roles = state.adminRoles.map(role => pick(role, "role")).filter(Boolean);
    select.innerHTML = roles.map(role => `<option value="${escapeHtml(role)}">${escapeHtml(role)}</option>`).join("");
    select.value = roles.includes(current) ? current : roles[0] || "ADMIN";
}

function renderPermissionEditor() {
    const roleName = qs("#adminRoleSelect")?.value;
    const role = state.adminRoles.find(item => pick(item, "role") === roleName);
    const selected = new Set(pick(role, "permissions") || []);
    qs("#adminPermissionGrid").innerHTML = permissionCatalog.map(permission => `<label class="check-card">
        <input type="checkbox" value="${escapeHtml(permission)}" ${selected.has(permission) ? "checked" : ""}>
        <span>${escapeHtml(permission.replaceAll("_", " "))}</span>
    </label>`).join("");
}

function fillAdminUserForm() {
    const username = qs("#adminUsername").value.trim();
    const user = state.adminUsers.find(item => pick(item, "username") === username);
    if (!user) return;
    qs("#adminDisplayName").value = pick(user, "display_name", "displayName") || "";
    qs("#adminPassword").value = "";
    qs("#adminAgentId").value = pick(user, "agent_id", "agentId") || "";
    qs("#adminTeams").value = (pick(user, "allowed_teams", "allowedTeams") || []).join(", ");
    qs("#adminUserRole").value = (pick(user, "roles") || ["VIEWER"])[0] || "VIEWER";
    qs("#adminUserEnabled").checked = Boolean(pick(user, "enabled"));
}

async function saveAdminUser() {
    const username = qs("#adminUsername").value.trim();
    if (!username) {
        qs("#adminActionResult").textContent = "Username is required.";
        return;
    }
    const password = qs("#adminPassword").value.trim();
    const body = {
        displayName: qs("#adminDisplayName").value.trim() || username,
        agentId: qs("#adminAgentId").value.trim() || null,
        enabled: qs("#adminUserEnabled").checked,
        allowedTeams: splitCsv(qs("#adminTeams").value),
        roles: [qs("#adminUserRole").value],
        extraPermissions: []
    };
    if (password) body.password = password;
    qs("#adminActionResult").textContent = `Saving ${username}...`;
    try {
        const result = await api(`/api/v1/admin/users/${encodeURIComponent(username)}`, {
            method: "PUT",
            body: JSON.stringify(body)
        });
        qs("#adminActionResult").textContent = JSON.stringify(result, null, 2);
        await safeLoad("adminUsers", "/api/v1/admin/users", []);
        renderAdmin();
    } catch (error) {
        qs("#adminActionResult").textContent = error.message;
    }
}

async function saveAdminRole() {
    const role = qs("#adminRoleSelect").value;
    const permissions = [...document.querySelectorAll("#adminPermissionGrid input:checked")].map(input => input.value);
    qs("#adminActionResult").textContent = `Saving permissions for ${role}...`;
    try {
        const result = await api(`/api/v1/admin/roles/${encodeURIComponent(role)}`, {
            method: "PUT",
            body: JSON.stringify({ permissions })
        });
        qs("#adminActionResult").textContent = JSON.stringify(result, null, 2);
        await safeLoad("adminRoles", "/api/v1/admin/roles", []);
        renderAdmin();
    } catch (error) {
        qs("#adminActionResult").textContent = error.message;
    }
}

function adminCapabilityCards() {
    const cards = [
        ["RBAC", "Create/update app users, assign roles, scope teams and agents."],
        ["Role Permissions", "Tune permissions for Admin, Workforce, Supervisor, Agent, and Viewer."],
        ["Components", "Review probes, target URLs, TLS trust, timeout and enabled state."],
        ["Audit", "User and permission changes are recorded by the in-memory audit service."],
        ["Alerts", "Webhook, SMTP and SMS thresholds are configurable in application.yml/env."],
        ["Diagnostics", "Use /api/v1/admin/diagnostics for AW/HDS/CVP schema checks."]
    ];
    return cards.map(([title, detail]) => `<article class="business-card"><h3>${escapeHtml(title)}</h3><p>${escapeHtml(detail)}</p></article>`).join("");
}

function renderPlan() {
    populatePlanFilters();
    const filtered = filteredPlanTasks();
    renderPlanKpis(filtered);
    renderTopicCards();
    qs("#planFilteredCount").textContent = `${filtered.length} tasks`;
    qs("#taskListMode").classList.toggle("active", planState.view === "tasks");
    qs("#resourceMode").classList.toggle("active", planState.view === "resources");
    qs("#planTaskView").style.display = planState.view === "tasks" ? "grid" : "none";
    qs("#planResourceView").style.display = planState.view === "resources" ? "grid" : "none";
    renderPlanTaskGroups(filtered);
    renderResourceView();
}

function populatePlanFilters() {
    fillSelect("#planTopicFilter", ["ALL", ...unique(planTasks.map(task => task.topic))], planState.topic);
    fillSelect("#planStatusFilter", ["ALL", "COMPLETED", "IN_PROGRESS", "ON_HOLD", "PLANNED"]);
    fillSelect("#planPriorityFilter", ["ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW"]);
    fillSelect("#planResourceFilter", ["ALL", ...unique(planTasks.flatMap(task => resourcesFor(task.resource)))]);
}

function fillSelect(selector, values, forcedValue) {
    const select = qs(selector);
    if (!select) return;
    const current = forcedValue || select.value || "ALL";
    select.innerHTML = values.map(value => `<option value="${escapeHtml(value)}">${escapeHtml(value === "ALL" ? "All" : value.replace("_", " "))}</option>`).join("");
    select.value = values.includes(current) ? current : "ALL";
}

function filteredPlanTasks() {
    planState.topic = qs("#planTopicFilter")?.value || planState.topic || "ALL";
    const topic = planState.topic;
    const status = qs("#planStatusFilter")?.value || "ALL";
    const priority = qs("#planPriorityFilter")?.value || "ALL";
    const resource = qs("#planResourceFilter")?.value || "ALL";
    return planTasks.filter(task =>
        (topic === "ALL" || task.topic === topic)
        && (status === "ALL" || task.status === status)
        && (priority === "ALL" || task.priority === priority)
        && (resource === "ALL" || resourcesFor(task.resource).includes(resource)));
}

function renderPlanKpis(tasks) {
    const total = planTasks.length;
    const completed = planTasks.filter(task => task.status === "COMPLETED").length;
    const inProgress = planTasks.filter(task => task.status === "IN_PROGRESS").length;
    const onHold = planTasks.filter(task => task.status === "ON_HOLD").length;
    const criticalOpen = planTasks.filter(task => task.priority === "CRITICAL" && task.status !== "COMPLETED").length;
    qs("#planKpis").innerHTML = [
        planKpi("Total Tasks", total, `${tasks.length} shown`, ""),
        planKpi("Completed", completed, `${Math.round(completed / total * 100)}% done`, "emerald"),
        planKpi("In Progress", inProgress, "active work", "blue"),
        planKpi("On Hold", onHold, "blocked or waiting", "amber"),
        planKpi("Critical Open", criticalOpen, "not completed", "red")
    ].join("");
}

function renderTopicCards() {
    const topics = unique(planTasks.map(task => task.topic));
    qs("#topicSummaryCards").innerHTML = topics.map(topic => {
        const tasks = planTasks.filter(task => task.topic === topic);
        const done = tasks.filter(task => task.status === "COMPLETED").length;
        const critical = tasks.filter(task => task.priority === "CRITICAL").length;
        const pctDone = Math.round(done / tasks.length * 100);
        return `<button class="topic-card ${planState.topic === topic ? "active" : ""}" data-topic="${escapeHtml(topic)}">
            <strong>${escapeHtml(topic)}</strong>
            <span>${tasks.length} tasks | ${critical} critical</span>
            <i><b style="width:${pctDone}%"></b></i>
            <em>${pctDone}% complete</em>
        </button>`;
    }).join("");
    document.querySelectorAll(".topic-card").forEach(card => card.addEventListener("click", () => {
        planState.topic = planState.topic === card.dataset.topic ? "ALL" : card.dataset.topic;
        renderPlan();
    }));
}

function renderPlanTaskGroups(tasks) {
    const topics = unique(tasks.map(task => task.topic));
    qs("#planTaskView").innerHTML = topics.map(topic => {
        const topicTasks = tasks.filter(task => task.topic === topic);
        const pctDone = Math.round(average(topicTasks, "pct") || 0);
        const collapsed = planState.collapsed.has(topic);
        return `<article class="plan-group panel ${collapsed ? "collapsed" : ""}">
            <button class="plan-group-head" data-collapse-topic="${escapeHtml(topic)}">
                <span>${collapsed ? ">" : "v"} ${escapeHtml(topic)}</span>
                <strong>${topicTasks.length} tasks</strong>
                ${statusMiniBadges(topicTasks)}
                <i><b style="width:${pctDone}%"></b></i>
                <em>${pctDone}%</em>
            </button>
            <div class="table-wrap plan-group-body"><table><thead><tr><th>Task</th><th>Priority</th><th>Status</th><th>Resource</th><th>Timeline</th><th>Progress</th></tr></thead><tbody>
                ${topicTasks.map(planTaskRow).join("")}
            </tbody></table></div>
        </article>`;
    }).join("") || `<article class="panel"><h2>No tasks match filters</h2></article>`;
    document.querySelectorAll("[data-collapse-topic]").forEach(button => button.addEventListener("click", () => {
        const topic = button.dataset.collapseTopic;
        planState.collapsed.has(topic) ? planState.collapsed.delete(topic) : planState.collapsed.add(topic);
        renderPlan();
    }));
    bindPlanEditors();
}

function planTaskRow(task) {
    const criticalOpen = task.priority === "CRITICAL" && task.status !== "COMPLETED";
    const index = planTasks.indexOf(task);
    return `<tr class="${criticalOpen ? "critical-row" : ""}">
        <td><strong>${escapeHtml(task.task)}</strong>${task.priorityNum ? `<span class="subline">Priority #${task.priorityNum}</span>` : ""}</td>
        <td>${badge(task.priority, priorityClass(task.priority))}</td>
        <td><select class="inline-input" data-plan-field="status" data-plan-index="${index}">${["COMPLETED", "IN_PROGRESS", "ON_HOLD", "PLANNED"].map(status => `<option ${task.status === status ? "selected" : ""}>${status}</option>`).join("")}</select></td>
        <td>${escapeHtml(task.resource)}</td>
        <td>${escapeHtml(task.start || "--")} / ${escapeHtml(task.finish || "--")}<span class="subline">${task.duration ?? "--"} days</span></td>
        <td>
            <input class="inline-input plan-pct-input" type="number" min="0" max="100" data-plan-field="pct" data-plan-index="${index}" value="${escapeHtml(task.pct)}">
            ${inlineProgress(task.pct)}
            <input class="inline-input" data-plan-field="comments" data-plan-index="${index}" value="${escapeHtml(task.comments)}">
            <button class="small-btn" data-plan-save="${index}">Save</button>
        </td>
    </tr>`;
}

function renderResourceView() {
    const people = unique(planTasks.flatMap(task => resourcesFor(task.resource)));
    const cards = people.map(name => {
        const tasks = planTasks.filter(task => resourcesFor(task.resource).includes(name));
        return { name, tasks };
    }).sort((a, b) => b.tasks.length - a.tasks.length);
    qs("#planResourceView").innerHTML = cards.map(({ name, tasks }) => {
        const done = tasks.filter(task => task.status === "COMPLETED").length;
        const active = tasks.filter(task => task.status === "IN_PROGRESS").length;
        const hold = tasks.filter(task => task.status === "ON_HOLD").length;
        const critical = tasks.filter(task => task.priority === "CRITICAL" && task.status !== "COMPLETED");
        return `<article class="resource-card">
            <div class="resource-head"><span class="avatar">${escapeHtml(name.slice(0, 1))}</span><div><h3>${escapeHtml(name)}</h3><small>${critical.length} critical open</small></div></div>
            <div class="resource-stats"><b>${tasks.length}</b><span>total</span><b>${done}</b><span>done</span><b>${active}</b><span>active</span><b>${hold}</b><span>hold</span></div>
            ${stackedProgress(tasks)}
            <h4>Active Work</h4>
            <div class="chip-list">${tasks.filter(task => task.status !== "COMPLETED").map(taskChip).join("") || "<span class='muted-value'>No active work</span>"}</div>
            ${critical.length ? `<div class="critical-box"><h4>Critical Open</h4>${critical.map(task => `<p><strong>${escapeHtml(task.task)}</strong><span>${escapeHtml(task.topic)} - ${escapeHtml(task.comments)}</span></p>`).join("")}</div>` : ""}
        </article>`;
    }).join("");
}

function setPlanView(view) {
    planState.view = view;
    renderPlan();
}

function loadPlanTasks() {
    try {
        const stored = localStorage.getItem("pcce.planTasks");
        return stored ? JSON.parse(stored) : defaultPlanTasks.map(task => ({ ...task }));
    } catch {
        return defaultPlanTasks.map(task => ({ ...task }));
    }
}

function savePlanTasks() {
    localStorage.setItem("pcce.planTasks", JSON.stringify(planTasks));
}

function bindPlanEditors() {
    document.querySelectorAll("[data-plan-save]").forEach(button => {
        button.addEventListener("click", () => savePlanTask(Number(button.dataset.planSave)));
    });
}

function savePlanTask(index) {
    const task = planTasks[index];
    if (!task) return;
    document.querySelectorAll(`[data-plan-index="${index}"]`).forEach(input => {
        const field = input.dataset.planField;
        task[field] = field === "pct" ? Math.max(0, Math.min(100, Number(input.value || 0))) : input.value;
    });
    savePlanTasks();
    renderPlan();
}

function addPlanTask() {
    const name = qs("#planEditTask").value.trim();
    if (!name) return;
    planTasks.push({
        topic: qs("#planEditTopic").value,
        task: name,
        priority: qs("#planEditPriority").value,
        priorityNum: null,
        status: qs("#planEditStatus").value,
        resource: qs("#planEditResource").value.trim() || "Unassigned",
        start: null,
        finish: null,
        duration: null,
        pct: Math.max(0, Math.min(100, Number(qs("#planEditPct").value || 0))),
        comments: qs("#planEditComments").value.trim()
    });
    savePlanTasks();
    qs("#planEditTask").value = "";
    qs("#planEditComments").value = "";
    renderPlan();
}

function resetPlanTasks() {
    if (!confirm("Reset project plan tasks to the default template?")) return;
    planTasks = defaultPlanTasks.map(task => ({ ...task }));
    savePlanTasks();
    renderPlan();
}

function unique(values) {
    return [...new Set(values.filter(Boolean))].sort();
}

function resourcesFor(value) {
    return String(value || "").split(/,|\/|&/).map(item => item.trim()).filter(Boolean);
}

function planKpi(label, value, detail, tone) {
    return `<article class="kpi-card compact plan-kpi ${tone}"><div class="kpi-top"><span>${escapeHtml(label)}</span></div><strong>${fmt(value)}</strong><small>${escapeHtml(detail)}</small></article>`;
}

function statusMiniBadges(tasks) {
    return ["COMPLETED", "IN_PROGRESS", "ON_HOLD", "PLANNED"].map(status => {
        const count = tasks.filter(task => task.status === status).length;
        return count ? `<span class="mini-badge ${statusClass(status)}">${count} ${status.replace("_", " ")}</span>` : "";
    }).join("");
}

function badge(label, tone) {
    return `<span class="badge ${tone}">${escapeHtml(label)}</span>`;
}

function priorityClass(priority) {
    return { CRITICAL: "down", HIGH: "high", MEDIUM: "warn", LOW: "low" }[priority] || "low";
}

function statusClass(status) {
    return { COMPLETED: "up", IN_PROGRESS: "on_call", ON_HOLD: "warn", PLANNED: "planned" }[status] || "low";
}

function inlineProgress(value) {
    return `<span class="progress-value">${fmt(value)}%</span><span class="progress-track"><i class="${value >= 90 ? "good" : value >= 40 ? "warn" : "bad"}" style="width:${value}%"></i></span>`;
}

function stackedProgress(tasks) {
    const total = tasks.length || 1;
    const done = tasks.filter(task => task.status === "COMPLETED").length / total * 100;
    const active = tasks.filter(task => task.status === "IN_PROGRESS").length / total * 100;
    const hold = tasks.filter(task => task.status === "ON_HOLD").length / total * 100;
    return `<div class="stacked"><i class="done" style="width:${done}%"></i><i class="active" style="width:${active}%"></i><i class="hold" style="width:${hold}%"></i></div>`;
}

function taskChip(task) {
    return `<span class="work-chip ${priorityClass(task.priority)}">${escapeHtml(task.task)} <small>${escapeHtml(task.topic)}</small></span>`;
}

function switchView(view) {
    document.querySelectorAll(".nav-item[data-view]").forEach(button => button.classList.toggle("active", button.dataset.view === view));
    document.querySelectorAll(".view").forEach(section => section.classList.toggle("active", section.id === `view-${view}`));
    qs("#pageTitle").textContent = pages[view][0];
    qs("#pageSubtitle").textContent = pages[view][1];
}

function hasPermission(permission) {
    const permissions = pick(state.user, "permissions") || [];
    return permissions.includes(permission);
}

function metricRow(label, value) {
    return `<div class="metric-row"><span>${escapeHtml(label)}</span><strong>${escapeHtml(String(value ?? "--"))}</strong></div>`;
}

function snippet(value, limit = 160) {
    const text = String(value ?? "").replace(/\s+/g, " ").trim();
    if (!text) return "--";
    return text.length > limit ? `${text.slice(0, limit)}...` : text;
}

function fillDatalist(selector, rows) {
    const list = qs(selector);
    if (!list) return;
    list.innerHTML = (rows || []).map(row => {
        const value = pick(row, "value") || "";
        const label = pick(row, "label") || value;
        const detail = pick(row, "detail") || "";
        return `<option value="${escapeHtml(value)}" label="${escapeHtml(label)}">${escapeHtml(detail)}</option>`;
    }).join("");
}

function metricPct(value) {
    return value === null || value === undefined ? "--" : `${Number(value).toFixed(1)}%`;
}

function splitCsv(value) {
    return String(value || "").split(",").map(item => item.trim()).filter(Boolean);
}

function isUnknownLabel(value) {
    const label = String(value || "").toUpperCase();
    return label === "UNKNOWN" || label === "UNMAPPED" || label.startsWith("SKILLTARGET ");
}

function systemKpi(label, value, detail) {
    return `<article class="kpi-card compact"><div class="kpi-top"><span>${escapeHtml(label)}</span></div><strong>${escapeHtml(String(value))}</strong><small>${escapeHtml(detail)}</small></article>`;
}

function renderBusinessCards() {
    const grouped = groupSkillMetrics();
    qs("#businessSkillCards").innerHTML = grouped.slice(0, 6).map((item, index) => `<article class="business-card">
        <h3><i class="dot" style="background:${colors[index % colors.length]}"></i>${escapeHtml(item.skill)}</h3>
        <div class="business-metrics">
            <span>Offered<strong>${fmt(item.offered)}</strong></span>
            <span>SL%<strong class="teal">${pct(item.service)}</strong></span>
            <span>Handled<strong>${fmt(item.handled)}</strong></span>
            <span>AHT<strong>${seconds(item.aht)}s</strong></span>
            <span>Abandoned<strong class="red">${fmt(item.abandoned)}</strong></span>
        </div>
    </article>`).join("") || `<article class="business-card"><h3>No skill group data</h3><p>Check HDS query mapping and selected date range.</p></article>`;
}

function groupSkillMetrics() {
    const map = new Map();
    state.calls.forEach(row => {
        const skill = pick(row, "skill_group", "skillGroup") || "UNKNOWN";
        const existing = map.get(skill) || { skill, offered: 0, handled: 0, abandoned: 0, weightedService: 0, serviceWeight: 0, weightedAht: 0, ahtWeight: 0 };
        const offered = num(pick(row, "calls_offered", "callsOffered"));
        const handled = num(pick(row, "calls_handled", "callsHandled"));
        existing.offered += offered;
        existing.handled += handled;
        existing.abandoned += num(pick(row, "calls_abandoned", "callsAbandoned"));
        const service = pick(row, "service_level_pct", "serviceLevelPct");
        if (service !== null && offered > 0) {
            existing.weightedService += num(service) * offered;
            existing.serviceWeight += offered;
        }
        const aht = pick(row, "avg_handle_time", "avgHandleTime");
        if (aht !== null && handled > 0) {
            existing.weightedAht += num(aht) * handled;
            existing.ahtWeight += handled;
        }
        map.set(skill, existing);
    });
    return [...map.values()].map(item => ({
        ...item,
        service: item.serviceWeight ? item.weightedService / item.serviceWeight : null,
        aht: item.ahtWeight ? item.weightedAht / item.ahtWeight : null
    })).sort((a, b) => b.offered - a.offered);
}

function serviceTrendByHour() {
    const byHour = new Map();
    state.calls.forEach(row => {
        const hour = num(pick(row, "hour"));
        const offered = num(pick(row, "calls_offered", "callsOffered"));
        const service = pick(row, "service_level_pct", "serviceLevelPct");
        const existing = byHour.get(hour) || { weighted: 0, weight: 0 };
        if (service !== null && offered > 0) {
            existing.weighted += num(service) * offered;
            existing.weight += offered;
        }
        byHour.set(hour, existing);
    });
    const entries = [...byHour.entries()].sort((a, b) => a[0] - b[0]);
    return {
        labels: entries.map(([hour]) => `${hour}:00`),
        series: [{ label: "Service Level", color: "#2ed3c2", values: entries.map(([, v]) => v.weight ? v.weighted / v.weight : 0) }]
    };
}

function radarMetrics() {
    const offered = sum(state.calls, "calls_offered", "callsOffered");
    const handled = sum(state.calls, "calls_handled", "callsHandled");
    const abandoned = sum(state.calls, "calls_abandoned", "callsAbandoned");
    const service = weightedAverage(state.calls, "service_level_pct", "serviceLevelPct", "calls_offered", "callsOffered");
    const aht = weightedAverage(state.calls, "avg_handle_time", "avgHandleTime", "calls_handled", "callsHandled");
    return [
        { label: "Service Level", value: service ?? 0 },
        { label: "Handled", value: offered ? handled / offered * 100 : 0 },
        { label: "Low Abandon", value: offered ? Math.max(0, 100 - abandoned / offered * 100) : 0 },
        { label: "AHT", value: aht ? Math.max(0, 100 - Math.min(100, aht / 10)) : 0 },
        { label: "IVR", value: average(state.ivr, "ivr_containment_rate", "ivrContainmentRate") ?? 0 }
    ];
}

function renderAgentFilters() {
    const teams = ["ALL", ...new Set(state.agents.map(agent => pick(agent, "team") || "UNKNOWN"))];
    const active = document.querySelector(".team-filter.active")?.dataset.team || "ALL";
    qs("#agentTeamFilters").innerHTML = teams.map(team => `<button class="team-filter ${team === active ? "active" : ""}" data-team="${escapeHtml(team)}">${escapeHtml(team === "ALL" ? "All" : team)}</button>`).join("");
    document.querySelectorAll(".team-filter").forEach(button => {
        button.addEventListener("click", () => {
            document.querySelectorAll(".team-filter").forEach(item => item.classList.remove("active"));
            button.classList.add("active");
            renderAgents();
        });
    });
}

function progressCell(value) {
    if (value === null || value === undefined) return `<span class="muted-value">--</span>`;
    const width = Math.max(0, Math.min(100, num(value)));
    const tone = width >= 90 ? "good" : width >= 75 ? "warn" : "bad";
    return `<span class="progress-value">${pct(width)}</span><span class="progress-track"><i class="${tone}" style="width:${width}%"></i></span>`;
}

function minutes(value) {
    if (value === null || value === undefined) return "--";
    return `${Math.round(num(value))}m`;
}

function renderCallFunnel() {
    const offered = sum(state.calls, "calls_offered", "callsOffered");
    const handled = sum(state.calls, "calls_handled", "callsHandled");
    const abandoned = sum(state.calls, "calls_abandoned", "callsAbandoned");
    const hasAbandonSource = state.calls.some(row => pick(row, "calls_abandoned", "callsAbandoned") !== null && pick(row, "calls_abandoned", "callsAbandoned") !== undefined);
    const ivr = average(state.ivr, "ivr_containment_rate", "ivrContainmentRate");
    const routed = Math.max(0, offered - (ivr === null ? 0 : offered * ivr / 100));
    const hasIvr = ivr !== null;
    const queued = hasIvr ? routed : Math.max(0, offered);
    const unknownMapping = state.calls.some(row => isUnknownLabel(pick(row, "skill_group", "skillGroup")));
    const node = (tone, label, value, percent, note) => `<div class="flow-node ${tone}">
        <span>${escapeHtml(label)}</span>
        <strong>${value === null ? "--" : fmt(value)}</strong>
        <small>${percent === null ? "--" : pct(percent)}${note ? ` | ${escapeHtml(note)}` : ""}</small>
    </div>`;
    qs("#callFunnel").innerHTML = `<div class="flow-diagram">
        ${node("primary", "Offered", offered, offered ? 100 : 0, "HDS")}
        <div class="flow-arrow">-&gt;</div>
        <div class="flow-split">
            ${node("teal", "IVR Contained", hasIvr ? offered * ivr / 100 : null, hasIvr ? ivr : null, hasIvr ? "CVP" : "CVP unavailable")}
            ${node("blue", "Queued / Routed", queued, offered ? queued / offered * 100 : null, hasIvr ? "estimated" : "from HDS")}
        </div>
        <div class="flow-arrow">-&gt;</div>
        <div class="flow-split">
            ${node("green", "Handled", handled, offered ? handled / offered * 100 : null, "agent connected")}
            ${node("red", "Abandoned", hasAbandonSource ? abandoned : null, hasAbandonSource && offered ? abandoned / offered * 100 : null, hasAbandonSource ? "not handled" : "source missing")}
        </div>
    </div>
    <div class="flow-note">${unknownMapping ? "Some calls are unmapped to skill groups. Use Admin diagnostics or CUIC SQL to map SkillGroupSkillTargetID/CallTypeID exactly." : "Funnel uses live HDS/CVP fields available for the selected filters."}</div>`;
}

function abandonmentByHour() {
    const hourly = groupByHour(state.calls);
    return {
        labels: hourly.labels,
        series: [{
            label: "Abandonment",
            color: "#ff626c",
            values: hourly.offered.map((offered, index) => offered ? (sumHourAbandoned(index, hourly.labels) / offered) * 100 : 0)
        }]
    };
}

function sumHourAbandoned(index, labels) {
    const hour = Number(String(labels[index]).replace(":00", ""));
    return state.calls
        .filter(row => num(pick(row, "hour")) === hour)
        .reduce((total, row) => total + num(pick(row, "calls_abandoned", "callsAbandoned")), 0);
}

function queueByHour() {
    const rows = groupByHour(state.calls);
    const values = rows.labels.map(label => {
        const hour = Number(String(label).replace(":00", ""));
        const matching = state.calls.filter(row => num(pick(row, "hour")) === hour);
        return average(matching, "avg_queue_time", "avgQueueTime");
    });
    return {
        labels: rows.labels,
        values: values.map(value => value === null ? 0 : value),
        hasData: values.some(value => value !== null)
    };
}

function average(rows, ...names) {
    const values = rows.map(row => pick(row, ...names)).filter(value => value !== null && value !== undefined).map(Number);
    return values.length ? values.reduce((a, b) => a + b, 0) / values.length : null;
}

function groupByHour(rows) {
    const map = new Map();
    rows.forEach(row => {
        const hour = num(pick(row, "hour"));
        const existing = map.get(hour) || { offered: 0, handled: 0 };
        existing.offered += num(pick(row, "calls_offered", "callsOffered"));
        existing.handled += num(pick(row, "calls_handled", "callsHandled"));
        map.set(hour, existing);
    });
    const labels = [...map.keys()].sort((a, b) => a - b).map(hour => `${hour}:00`);
    const values = [...map.entries()].sort((a, b) => a[0] - b[0]).map(([, value]) => value);
    return { labels, offered: values.map(v => v.offered), handled: values.map(v => v.handled) };
}

function sum(rows, ...names) {
    return rows.reduce((total, row) => total + num(pick(row, ...names)), 0);
}

function weightedAverage(rows, valueSnake, valueCamel, weightSnake, weightCamel) {
    let weighted = 0;
    let totalWeight = 0;
    rows.forEach(row => {
        const value = pick(row, valueSnake, valueCamel);
        const weight = num(pick(row, weightSnake, weightCamel));
        if (value !== null && value !== undefined && weight > 0) {
            weighted += num(value) * weight;
            totalWeight += weight;
        }
    });
    return totalWeight ? weighted / totalWeight : null;
}

function groupDropsByHour(rows) {
    const map = new Map();
    rows.forEach(row => {
        const hour = num(pick(row, "hour"));
        map.set(hour, (map.get(hour) || 0) + num(pick(row, "dropped_calls", "droppedCalls")));
    });
    const entries = [...map.entries()].sort((a, b) => a[0] - b[0]);
    return { labels: entries.map(([hour]) => `${hour}:00`), values: entries.map(([, value]) => value) };
}

function groupBySkill(rows, ...valueNames) {
    const map = new Map();
    rows.forEach(row => {
        const skill = pick(row, "skill_group", "skillGroup") || "UNKNOWN";
        map.set(skill, (map.get(skill) || 0) + num(pick(row, ...valueNames)));
    });
    const entries = [...map.entries()].sort((a, b) => b[1] - a[1]).slice(0, 6);
    return { labels: entries.map(([label]) => label), values: entries.map(([, value]) => value) };
}

function drawLineChart(canvas, labels, series, fixedMax) {
    if (!canvas) return;
    const ctx = setupCanvas(canvas);
    const { width, height } = canvas.getBoundingClientRect();
    const pad = { left: 58, right: 24, top: 24, bottom: 44 };
    ctx.clearRect(0, 0, width, height);
    drawGrid(ctx, width, height, pad);
    const max = fixedMax || Math.max(10, ...series.flatMap(s => s.values));
    const pointsFor = values => values.map((value, index) => ({
        x: pad.left + (labels.length <= 1 ? 0 : index * (width - pad.left - pad.right) / (labels.length - 1)),
        y: height - pad.bottom - (num(value) / max) * (height - pad.top - pad.bottom)
    }));
    series.forEach(s => {
        const points = pointsFor(s.values);
        if (!points.length) return;
        ctx.strokeStyle = s.color;
        ctx.lineWidth = 3;
        ctx.beginPath();
        points.forEach((point, index) => index ? ctx.lineTo(point.x, point.y) : ctx.moveTo(point.x, point.y));
        ctx.stroke();
        ctx.lineTo(points[points.length - 1].x, height - pad.bottom);
        ctx.lineTo(points[0].x, height - pad.bottom);
        ctx.closePath();
        ctx.fillStyle = s.color + "24";
        ctx.fill();
    });
    drawLabels(ctx, labels, width, height, pad);
}

function drawBarChart(canvas, labels, values, color) {
    if (!canvas) return;
    const ctx = setupCanvas(canvas);
    const { width, height } = canvas.getBoundingClientRect();
    const pad = { left: 48, right: 20, top: 20, bottom: 42 };
    ctx.clearRect(0, 0, width, height);
    drawGrid(ctx, width, height, pad);
    const max = Math.max(10, ...values);
    const slot = (width - pad.left - pad.right) / Math.max(1, values.length);
    values.forEach((value, index) => {
        const barHeight = (num(value) / max) * (height - pad.top - pad.bottom);
        ctx.fillStyle = color;
        ctx.fillRect(pad.left + index * slot + slot * .2, height - pad.bottom - barHeight, slot * .6, barHeight);
    });
    drawLabels(ctx, labels, width, height, pad);
}

function drawDoughnut(canvas, labels, values, palette) {
    if (!canvas) return;
    const ctx = setupCanvas(canvas);
    const { width, height } = canvas.getBoundingClientRect();
    ctx.clearRect(0, 0, width, height);
    const total = values.reduce((sum, value) => sum + num(value), 0) || 1;
    const radius = Math.min(width, height) * .32;
    const cx = width / 2;
    const cy = height / 2;
    let start = -Math.PI / 2;
    values.forEach((value, index) => {
        const angle = (num(value) / total) * Math.PI * 2;
        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.fillStyle = palette[index % palette.length];
        ctx.arc(cx, cy, radius, start, start + angle);
        ctx.closePath();
        ctx.fill();
        start += angle;
    });
    ctx.beginPath();
    ctx.fillStyle = "#151a23";
    ctx.arc(cx, cy, radius * .58, 0, Math.PI * 2);
    ctx.fill();
}

function drawRadar(canvas, metrics) {
    if (!canvas) return;
    const ctx = setupCanvas(canvas);
    const { width, height } = canvas.getBoundingClientRect();
    ctx.clearRect(0, 0, width, height);
    const cx = width / 2;
    const cy = height / 2 + 10;
    const radius = Math.min(width, height) * .32;
    const count = metrics.length || 1;
    ctx.strokeStyle = "#263241";
    ctx.fillStyle = "#87a1c2";
    ctx.font = "13px Segoe UI";
    for (let ring = 1; ring <= 4; ring++) {
        ctx.beginPath();
        for (let i = 0; i < count; i++) {
            const angle = -Math.PI / 2 + i * Math.PI * 2 / count;
            const r = radius * ring / 4;
            const x = cx + Math.cos(angle) * r;
            const y = cy + Math.sin(angle) * r;
            i ? ctx.lineTo(x, y) : ctx.moveTo(x, y);
        }
        ctx.closePath();
        ctx.stroke();
    }
    metrics.forEach((metric, i) => {
        const angle = -Math.PI / 2 + i * Math.PI * 2 / count;
        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.lineTo(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius);
        ctx.stroke();
        ctx.fillText(metric.label, cx + Math.cos(angle) * (radius + 18) - 28, cy + Math.sin(angle) * (radius + 18));
    });
    ctx.beginPath();
    metrics.forEach((metric, i) => {
        const angle = -Math.PI / 2 + i * Math.PI * 2 / count;
        const value = Math.max(0, Math.min(100, num(metric.value))) / 100;
        const x = cx + Math.cos(angle) * radius * value;
        const y = cy + Math.sin(angle) * radius * value;
        i ? ctx.lineTo(x, y) : ctx.moveTo(x, y);
    });
    ctx.closePath();
    ctx.fillStyle = "#2ed3c244";
    ctx.strokeStyle = "#2ed3c2";
    ctx.lineWidth = 3;
    ctx.fill();
    ctx.stroke();
}

function setupCanvas(canvas) {
    const ratio = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * ratio;
    canvas.height = rect.height * ratio;
    const ctx = canvas.getContext("2d");
    ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
    return ctx;
}

function drawGrid(ctx, width, height, pad) {
    ctx.strokeStyle = "#263241";
    ctx.lineWidth = 1;
    ctx.setLineDash([5, 5]);
    for (let i = 0; i <= 4; i++) {
        const y = pad.top + i * (height - pad.top - pad.bottom) / 4;
        ctx.beginPath();
        ctx.moveTo(pad.left, y);
        ctx.lineTo(width - pad.right, y);
        ctx.stroke();
    }
    ctx.setLineDash([]);
}

function drawLabels(ctx, labels, width, height, pad) {
    ctx.fillStyle = "#87a1c2";
    ctx.font = "14px Segoe UI";
    labels.forEach((label, index) => {
        const x = pad.left + (labels.length <= 1 ? 0 : index * (width - pad.left - pad.right) / (labels.length - 1));
        ctx.fillText(label, x - 18, height - 16);
    });
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, char => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    }[char]));
}
