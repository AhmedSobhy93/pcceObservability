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
    cvpApiStatus: [],
    cvpCapabilities: [],
    cvpFunctions: [],
    cvpActions: [],
    rtmtCapabilities: [],
    spogCapabilities: [],
    smtpCapabilities: [],
    spogOps: [],
    serverLogTargets: [],
    machineInventory: [],
    grafanaDashboards: [],
    cuicReports: [],
    integrationCapabilities: [],
    finesseCapabilities: [],
    finesseSystem: [],
    finesseAgents: [],
    finesseDialogs: [],
    finesseTeams: [],
    finesseQueues: [],
    jmxStatus: [],
    appDynamicsStatus: [],
    liveDataStatus: [],
    liveDataTokenProbe: [],
    realtimeSnapshots: [],
    callFlow: [],
    skillGroups: [],
    callTypeOptions: [],
    agentOptions: [],
    adminUsers: [],
    adminRoles: [],
    adminComponents: [],
    adminConfigReadiness: [],
    notificationSettings: null,
    projectTasks: []
};

const pages = {
    overview: ["Dashboard Overview", "Cisco PCCE 12.6.2 - Real-time Contact Center Analytics"],
    business: ["Business Metrics", "Service level, handle time, IVR containment, and contact center KPIs"],
    agents: ["Agent Performance", "Workforce team and agent productivity view"],
    calls: ["Call Analytics", "Dropped calls, queue behavior, and skill group distribution"],
    system: ["System Health", "PCCE, CVP, CUIC, Finesse, PG, CTI, and gateway status"],
    eleveo: ["Eleveo QM & Recording", "Grafana monitoring for quality management and recording platforms"],
    integration: ["PCCE Integration", "AW/HDS SQL Server and CVP Informix connectivity"],
    cvp: ["CVP APIs", "CVP REST use cases, VXML apps, media, Syslog, SNMP, diagnostics and VVB operations"],
    advanced: ["Advanced Monitoring", "Secure JMX, AppDynamics, and PCCE Live Data readiness"],
    smtp: ["Alerts", "Webhook, SMTP, SMS thresholds, escalation, and notification readiness"],
    spog: ["SPOG Operations", "Single pane operations, graceful actions, and log collection"],
    admin: ["Admin", "Users, role permissions, and runtime configuration"],
    app: ["Spring Boot App", "Operational alerts, readiness checks, and support status"],
    plan: ["Project Plan", "Production hardening and banking support checklist"]
};

const colors = ["#2ed3c2", "#3d82f6", "#f4a51c", "#8d6cf7", "#24e0a4", "#ff626c"];
const uiState = { ivrNodePage: 0, ivrNodePageSize: 50, agentPage: 0, agentPageSize: 25, journeyPage: 0, journeyPageSize: 12, callTypePage: 0, callTypePageSize: 50 };
const businessSettings = loadBusinessSettings();
const planState = { view: "tasks", topic: "ALL", collapsed: new Set() };
let activeView = "overview";
let refreshInProgress = false;
let liveRefreshInProgress = false;
let pendingRefresh = false;
const permissionCatalog = [
    "CALL_METRICS_READ",
    "AGENT_STATS_READ",
    "DROPPED_CALLS_READ",
    "IVR_METRICS_READ",
    "COMPONENT_STATUS_READ",
    "OPERATIONS_READ",
    "SOLUTION_ADMIN"
];
let planTasks = [];

document.addEventListener("DOMContentLoaded", () => {
    const today = new Date().toISOString().slice(0, 10);
    qs("#fromDate").value = today;
    qs("#toDate").value = today;
    initBusinessSettings();
    qs("#refreshBtn").addEventListener("click", refresh);
    qs("#agentFilterBtn")?.addEventListener("click", refresh);
    qs("#callsFilterBtn")?.addEventListener("click", refresh);
    qs("#ivrPrevPage")?.addEventListener("click", () => {
        uiState.ivrNodePage = Math.max(0, uiState.ivrNodePage - 1);
        renderCvpIvrNodes();
    });
    qs("#ivrNextPage")?.addEventListener("click", () => {
        uiState.ivrNodePage += 1;
        renderCvpIvrNodes();
    });
    qs("#agentPrevPage")?.addEventListener("click", () => {
        uiState.agentPage = Math.max(0, uiState.agentPage - 1);
        renderAgents();
    });
    qs("#agentNextPage")?.addEventListener("click", () => {
        uiState.agentPage += 1;
        renderAgents();
    });
    qs("#journeyPrevPage")?.addEventListener("click", () => {
        uiState.journeyPage = Math.max(0, uiState.journeyPage - 1);
        renderCvpJourney();
    });
    qs("#journeyNextPage")?.addEventListener("click", () => {
        uiState.journeyPage += 1;
        renderCvpJourney();
    });
    qs("#callTypePrevPage")?.addEventListener("click", () => {
        uiState.callTypePage = Math.max(0, uiState.callTypePage - 1);
        renderCallTypes();
    });
    qs("#callTypeNextPage")?.addEventListener("click", () => {
        uiState.callTypePage += 1;
        renderCallTypes();
    });
    qs("#adminQuickBtn")?.addEventListener("click", () => switchView("admin"));
    qs("#adminSaveUserBtn")?.addEventListener("click", saveAdminUser);
    qs("#adminSaveRoleBtn")?.addEventListener("click", saveAdminRole);
    qs("#saveAlertConfigBtn")?.addEventListener("click", saveAlertConfig);
    qs("#loadInventoryBtn")?.addEventListener("click", loadMachineInventory);
    qs("#planAddTaskBtn")?.addEventListener("click", addPlanTask);
    qs("#planResetBtn")?.addEventListener("click", resetPlanTasks);
    qs("#planExportCsvBtn")?.addEventListener("click", () => window.open("/api/v1/project/tasks/export.csv", "_blank"));
    qs("#planCopyShareBtn")?.addEventListener("click", copyProjectShareLink);
    qs("#planGenerateTemplateBtn")?.addEventListener("click", generatePlanTemplate);
    qs("#adminUsername")?.addEventListener("change", fillAdminUserForm);
    qs("#adminRoleSelect")?.addEventListener("change", renderPermissionEditor);
    qs("#taskListMode")?.addEventListener("click", () => setPlanView("tasks"));
    qs("#resourceMode")?.addEventListener("click", () => setPlanView("resources"));
    ["#planTopicFilter", "#planStatusFilter", "#planPriorityFilter", "#planResourceFilter"].forEach(selector => {
        qs(selector)?.addEventListener("change", renderPlan);
    });
    const debouncedRefresh = debounce(() => {
        uiState.ivrNodePage = 0;
        uiState.journeyPage = 0;
        uiState.callTypePage = 0;
        refresh();
    }, 450);
    [
        "#fromDate", "#toDate", "#overviewSkillFilter", "#businessSkillFilter", "#ivrAppFilter",
        "#agentPageFilter", "#agentTeamInput", "#callsSkillFilter", "#callsCallTypeFilter", "#callKeyFilter"
    ].forEach(selector => {
        const element = qs(selector);
        element?.addEventListener(element.type === "date" ? "change" : "input", debouncedRefresh);
    });
    ["#businessSlTarget", "#businessAhtTarget", "#businessFallbackMode", "#businessAhtProxyMode", "#businessMinCalls"].forEach(selector => {
        qs(selector)?.addEventListener("change", () => {
            saveBusinessSettings();
            renderBusiness();
            renderKpis();
        });
    });
    ["#agentStatusFilter", "#agentSearchFilter"].forEach(selector => {
        qs(selector)?.addEventListener("input", debounce(() => {
            uiState.agentPage = 0;
            renderAgents();
        }, 250));
        qs(selector)?.addEventListener("change", () => {
            uiState.agentPage = 0;
            renderAgents();
        });
    });
    ["#inventorySearch", "#inventoryTypeFilter"].forEach(selector => {
        qs(selector)?.addEventListener("input", renderMachineInventory);
        qs(selector)?.addEventListener("change", renderMachineInventory);
    });
    ["#ivrCallIdFilter", "#ivrCallerFilter"].forEach(selector => {
        qs(selector)?.addEventListener("input", debounce(() => {
            uiState.ivrNodePage = 0;
            renderCvpIvrNodes();
        }, 250));
    });
    qs("#collapseBtn").addEventListener("click", () => document.body.classList.toggle("collapsed"));
    qs("#logoutBtn").addEventListener("click", logout);
    qs("#projectPlanLink")?.addEventListener("click", () => location.href = "/project");
    document.querySelectorAll(".nav-item[data-view]").forEach(button => {
        button.addEventListener("click", () => switchView(button.dataset.view));
    });
    refresh();
    setInterval(refreshLiveData, 10000);
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

function debounce(fn, waitMs) {
    let timer = null;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), waitMs);
    };
}

function loadBusinessSettings() {
    try {
        return {
            slTarget: 80,
            ahtTarget: 300,
            fallbackMode: "derived",
            ahtProxyMode: "none",
            minCalls: 0,
            ...JSON.parse(localStorage.getItem("pcceBusinessSettings") || "{}")
        };
    } catch {
        return { slTarget: 80, ahtTarget: 300, fallbackMode: "derived", ahtProxyMode: "none", minCalls: 0 };
    }
}

function initBusinessSettings() {
    if (qs("#businessSlTarget")) qs("#businessSlTarget").value = businessSettings.slTarget;
    if (qs("#businessAhtTarget")) qs("#businessAhtTarget").value = businessSettings.ahtTarget;
    if (qs("#businessFallbackMode")) qs("#businessFallbackMode").value = businessSettings.fallbackMode;
    if (qs("#businessAhtProxyMode")) qs("#businessAhtProxyMode").value = businessSettings.ahtProxyMode;
    if (qs("#businessMinCalls")) qs("#businessMinCalls").value = businessSettings.minCalls;
}

function saveBusinessSettings() {
    businessSettings.slTarget = Number(qs("#businessSlTarget")?.value || businessSettings.slTarget || 80);
    businessSettings.ahtTarget = Number(qs("#businessAhtTarget")?.value || businessSettings.ahtTarget || 300);
    businessSettings.fallbackMode = qs("#businessFallbackMode")?.value || businessSettings.fallbackMode || "derived";
    businessSettings.ahtProxyMode = qs("#businessAhtProxyMode")?.value || businessSettings.ahtProxyMode || "none";
    businessSettings.minCalls = Number(qs("#businessMinCalls")?.value || 0);
    localStorage.setItem("pcceBusinessSettings", JSON.stringify(businessSettings));
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
    if (refreshInProgress) {
        pendingRefresh = true;
        return;
    }
    refreshInProgress = true;
    try {
        setStatus([{ text: "Refreshing live data...", level: "neutral" }]);
        const params = dateParams();
        const errors = [];
        const wantsBusiness = ["overview", "business"].includes(activeView);
        const wantsAgents = activeView === "agents";
        const wantsCalls = activeView === "calls";
        const wantsSystem = activeView === "system";
        const wantsIntegration = activeView === "integration";
        const wantsCvp = activeView === "cvp";
        const wantsAdvanced = activeView === "advanced";
        const wantsAlerts = activeView === "smtp";
        const wantsSpog = activeView === "spog";
        const wantsAdmin = activeView === "admin";
        const wantsApp = activeView === "app";
        const wantsPlan = activeView === "plan";
        const wantsEleveo = activeView === "eleveo";

    const coreLoads = [
        safeLoad("user", "/api/v1/auth/me", null),
        safeLoad("assessment", "/api/v1/operations/assessment/last", state.assessment, { timeoutMs: 5000 })
    ];
    if (["overview", "system", "app", "spog"].includes(activeView)) {
        coreLoads.push(safeLoad("components", "/api/v1/components/status", state.components, { timeoutMs: 12000 }));
    }
    if (wantsBusiness || wantsCalls) {
        coreLoads.push(
                safeLoad("calls", `/api/v1/metrics/calls?${params}`, [], { timeoutMs: 15000 }),
                safeLoad("callTypes", `/api/v1/metrics/call-types?${callTypeParams()}`, [], { timeoutMs: 15000 }),
                safeLoad("drops", `/api/v1/calls/dropped?${params}`, []),
                safeLoad("ivr", `/api/v1/metrics/ivr-containment?${params}`, []));
    }
    if (wantsCalls) {
        coreLoads.push(
                safeLoad("callFlow", `/api/v1/calls/flow?${callFlowParams()}`, [], { timeoutMs: 15000 }),
                safeLoad("cvpIvrNodes", `/api/v1/metrics/cvp-ivr-nodes?${ivrNodeParams()}`, [], { timeoutMs: 12000 }));
    }
    if (wantsAgents) {
        coreLoads.push(safeLoad("agents", `/api/v1/agents/stats?${agentParams()}`, [], { timeoutMs: 15000 }));
    }
    if (wantsSystem) {
        coreLoads.push(safeLoad("serverMetrics", "/api/v1/components/server-metrics", [], { timeoutMs: 8000 }));
    }
    const coreResults = await Promise.all(coreLoads);
    errors.push(...coreResults.filter(Boolean));
    renderAll(errors);
    if (wantsAgents) {
        const finesseAgentError = await safeLoad("finesseAgents", "/api/v1/finesse/agents", state.finesseAgents, { timeoutMs: 12000 });
        if (finesseAgentError) errors.push(finesseAgentError);
        renderAgents();
        renderFinesse();
        const finesseDialogError = await safeLoad("finesseDialogs", "/api/v1/finesse/dialogs", state.finesseDialogs, { timeoutMs: 12000 });
        if (finesseDialogError) errors.push(finesseDialogError);
        renderAgents();
        renderFinesse();
    }

    const supportLoads = [];
    if (!state.skillGroups.length) supportLoads.push(safeLoad("skillGroups", "/api/v1/reference/skill-groups", state.skillGroups));
    if (!state.callTypeOptions.length) supportLoads.push(safeLoad("callTypeOptions", "/api/v1/reference/call-types", state.callTypeOptions));
    if (!state.agentOptions.length) supportLoads.push(safeLoad("agentOptions", "/api/v1/reference/agents", state.agentOptions));
    if (wantsIntegration || wantsAdvanced || wantsSpog) {
        supportLoads.push(
                safeLoad("pcceApiStatus", "/api/v1/pcce-api/status", state.pcceApiStatus, { timeoutMs: 8000 }),
                safeLoad("pcceCapabilities", "/api/v1/pcce-api/capabilities", state.pcceCapabilities),
                safeLoad("pcceFunctions", "/api/v1/pcce-api/functions", state.pcceFunctions),
                safeLoad("pcceActions", "/api/v1/pcce-api/actions", state.pcceActions),
                safeLoad("rtmtCapabilities", "/api/v1/pcce-api/rtmt-capabilities", state.rtmtCapabilities),
                safeLoad("spogCapabilities", "/api/v1/pcce-api/spog-capabilities", state.spogCapabilities));
    }
    if (wantsCvp) {
        supportLoads.push(
                safeLoad("cvpApiStatus", "/api/v1/cvp-api/status", state.cvpApiStatus, { timeoutMs: 10000 }),
                safeLoad("cvpCapabilities", "/api/v1/cvp-api/capabilities", state.cvpCapabilities),
                safeLoad("cvpFunctions", "/api/v1/cvp-api/functions", state.cvpFunctions),
                safeLoad("cvpActions", "/api/v1/cvp-api/actions", state.cvpActions));
    }
    if (wantsAgents || wantsIntegration) {
        supportLoads.push(
                safeLoad("finesseCapabilities", "/api/v1/finesse/capabilities", state.finesseCapabilities),
                safeLoad("finesseSystem", "/api/v1/finesse/system", state.finesseSystem, { timeoutMs: 12000 }),
                safeLoad("finesseTeams", "/api/v1/finesse/teams", state.finesseTeams, { timeoutMs: 12000 }),
                safeLoad("finesseQueues", "/api/v1/finesse/queues", state.finesseQueues, { timeoutMs: 12000 }));
    }
    if (wantsAdvanced) {
        supportLoads.push(
                safeLoad("jmxStatus", "/api/v1/integrations/advanced/jmx", state.jmxStatus),
                safeLoad("appDynamicsStatus", "/api/v1/integrations/advanced/app-dynamics", state.appDynamicsStatus),
                safeLoad("liveDataStatus", "/api/v1/integrations/advanced/live-data", state.liveDataStatus),
                safeLoad("liveDataTokenProbe", "/api/v1/live-data/token-probe", state.liveDataTokenProbe, { timeoutMs: 10000 }),
                safeLoad("realtimeSnapshots", "/api/v1/live-data/realtime-snapshots", state.realtimeSnapshots, { timeoutMs: 10000 }));
    }
    if (wantsAlerts || wantsSpog) {
        supportLoads.push(
                safeLoad("smtpCapabilities", "/api/v1/operations/smtp-capabilities", state.smtpCapabilities),
                safeLoad("spogOps", "/api/v1/operations/spog-capabilities", state.spogOps),
                safeLoad("serverLogTargets", "/api/v1/operations/server-log-targets", state.serverLogTargets));
    }
    if (wantsEleveo) {
        supportLoads.push(safeLoad("grafanaDashboards", "/api/v1/integrations/grafana/dashboards", state.grafanaDashboards));
    }
    if (wantsIntegration) {
        supportLoads.push(
                safeLoad("cuicReports", "/api/v1/cuic/reports", state.cuicReports),
                safeLoad("integrationCapabilities", "/api/v1/integrations/capabilities", state.integrationCapabilities));
    }
    if (wantsApp || wantsAdmin) {
        supportLoads.push(
                safeLoad("queryPerformance", "/api/v1/monitoring/query-performance?limit=50", state.queryPerformance),
                safeLoad("logs", "/api/v1/monitoring/logs?limit=80", state.logs));
    }
    if (wantsPlan) {
        supportLoads.push(safeLoad("projectTasks", "/api/v1/project/tasks", state.projectTasks, { timeoutMs: 8000 }));
    }
    const results = await Promise.all(supportLoads);
    errors.push(...results.filter(Boolean));
    if (hasPermission("SOLUTION_ADMIN") && wantsAdmin) {
        const adminResults = await Promise.all([
            safeLoad("adminUsers", "/api/v1/admin/users", []),
            safeLoad("adminRoles", "/api/v1/admin/roles", []),
            safeLoad("adminComponents", "/api/v1/admin/components", []),
            safeLoad("adminConfigReadiness", "/api/v1/admin/diagnostics/config-readiness", []),
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
    } finally {
        refreshInProgress = false;
        if (pendingRefresh) {
            pendingRefresh = false;
            setTimeout(refresh, 0);
        }
    }
}

async function refreshLiveData() {
    if (refreshInProgress || liveRefreshInProgress) return;
    if (!["overview", "business", "agents", "calls", "advanced"].includes(activeView)) return;
    liveRefreshInProgress = true;
    const params = dateParams();
    const loads = [];
    if (["overview", "business", "calls"].includes(activeView)) {
        loads.push(
                safeLoad("calls", `/api/v1/metrics/calls?${params}`, state.calls, { timeoutMs: 8000 }),
                safeLoad("drops", `/api/v1/calls/dropped?${params}`, state.drops, { timeoutMs: 8000 }),
                safeLoad("ivr", `/api/v1/metrics/ivr-containment?${params}`, state.ivr, { timeoutMs: 8000 }));
    }
    if (activeView === "agents") {
        await safeLoad("agents", `/api/v1/agents/stats?${agentParams()}`, state.agents, { timeoutMs: 8000 });
        renderAgents();
        renderFinesse();
        loads.push(
                safeLoad("finesseAgents", "/api/v1/finesse/agents", state.finesseAgents, { timeoutMs: 8000 }),
                safeLoad("finesseDialogs", "/api/v1/finesse/dialogs", state.finesseDialogs, { timeoutMs: 8000 }));
    }
    if (activeView === "advanced" || activeView === "overview") {
        loads.push(safeLoad("realtimeSnapshots", "/api/v1/live-data/realtime-snapshots", state.realtimeSnapshots, { timeoutMs: 8000 }));
    }
    try {
        await Promise.all(loads);
        renderKpis();
        renderCharts();
        renderBusiness();
        renderAgents();
        renderFinesse();
        renderCallTypes();
        renderAdvanced();
    } finally {
        liveRefreshInProgress = false;
    }
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
    renderCvpApi();
    renderSmtp();
    renderSpog();
    renderEleveo();
    renderAdvanced();
    renderOperations();
    renderSupport();
    renderAdmin();
    planTasks = state.projectTasks || [];
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
    const service = businessServiceLevel(state.calls).value;
    const aht = businessAht(state.calls).value;

    qs("#kpiOffered").textContent = fmt(offered);
    qs("#kpiHandled").textContent = fmt(handled);
    qs("#kpiAbandoned").textContent = fmt(abandoned);
    qs("#kpiDropped").textContent = fmt(dropped);
    qs("#trendDropped").textContent = dropped ? "Disposition based" : "Not configured";
    qs("#kpiService").textContent = pct(service);
    qs("#kpiAht").textContent = seconds(aht);
    qs("#trendService").textContent = businessServiceLevel(state.calls).source;
    qs("#kpiAht").nextElementSibling.textContent = businessAht(state.calls).source;
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
    renderCvpJourney();
    renderBusinessRules();
    const offered = sum(state.calls, "calls_offered", "callsOffered");
    const handled = sum(state.calls, "calls_handled", "callsHandled");
    const abandoned = sum(state.calls, "calls_abandoned", "callsAbandoned");
    const dropped = sum(state.drops, "dropped_calls", "droppedCalls");
    const service = businessServiceLevel(state.calls);
    const aht = businessAht(state.calls);
    const asa = businessAsa(state.calls);
    const ivrContainment = businessIvrContainment();
    const fcr = businessFcr(state.calls);
    const rows = [
        ["Calls Offered", fmt(offered)],
        ["Calls Handled", fmt(handled)],
        ["Calls Abandoned", fmt(abandoned)],
        ["Dropped Calls", fmt(dropped)],
        ["Answer Rate", pct(offered ? handled / offered * 100 : null)],
        ["Abandon Rate", pct(offered ? abandoned / offered * 100 : null)],
        ["Drop Rate", pct(offered ? dropped / offered * 100 : null)],
        ["Service Level", `${pct(service.value)} (${service.short})`],
        ["Avg Handle Time", `${metricSeconds(aht.value)} (${aht.short})`],
        ["Average Speed Answer", `${metricSeconds(asa.value)} (${asa.short})`],
        ["IVR Containment", `${pct(ivrContainment.value)} (${ivrContainment.short})`],
        ["First Call Resolution", `${pct(fcr.value)} (${fcr.short})`]
    ];
    qs("#businessMetrics").innerHTML = rows.map(([label, value]) => metricRow(label, value)).join("");

    const trend = serviceTrendByHour();
    drawLineChart(qs("#serviceTrendChart"), trend.labels, trend.series, 100);
    drawRadar(qs("#performanceRadar"), radarMetrics());
    const fcrBars = fcrBySkill();
    drawHorizontalBarChart(qs("#fcrBySkillChart"), fcrBars.labels, fcrBars.values, colors);
    const ivrBars = ivrByApp();
    drawHorizontalBarChart(qs("#ivrBySkillChart"), ivrBars.labels, ivrBars.values, colors);
}

function renderCvpIvrNodes() {
    const count = qs("#cvpIvrNodeCount");
    const table = qs("#cvpIvrNodeTable");
    if (!count || !table) return;
    const callIdFilter = firstFilterValue("#ivrCallIdFilter").toLowerCase();
    const callerFilter = firstFilterValue("#ivrCallerFilter").toLowerCase();
    const rows = state.cvpIvrNodes.filter(row => {
        const callId = String(pick(row, "call_id", "callId") || "").toLowerCase();
        const caller = String(pick(row, "caller_number", "callerNumber") || "").toLowerCase();
        return (!callIdFilter || callId.includes(callIdFilter))
            && (!callerFilter || caller.includes(callerFilter));
    });
    const totalPages = Math.max(1, Math.ceil(rows.length / uiState.ivrNodePageSize));
    uiState.ivrNodePage = Math.min(uiState.ivrNodePage, totalPages - 1);
    const start = uiState.ivrNodePage * uiState.ivrNodePageSize;
    const pageRows = rows.slice(start, start + uiState.ivrNodePageSize);
    count.textContent = `${rows.length} rows | page ${uiState.ivrNodePage + 1}/${totalPages}`;
    qs("#ivrPrevPage").disabled = uiState.ivrNodePage === 0;
    qs("#ivrNextPage").disabled = uiState.ivrNodePage >= totalPages - 1;
    table.innerHTML = pageRows.map(row => {
        const callId = pick(row, "call_id", "callId") || "";
        return `<tr>
        <td>${escapeHtml(pick(row, "call_id", "callId") || "")}</td>
        <td>${escapeHtml(pick(row, "call_start_time", "callStartTime") || "")}</td>
        <td>${escapeHtml(pick(row, "caller_number", "callerNumber") || "")}</td>
        <td>${escapeHtml(pick(row, "app_name", "appName") || "")}</td>
        <td>${escapeHtml(pick(row, "duration") || "")}</td>
        <td>${escapeHtml(pick(row, "flag") || "")}</td>
        <td><span class="badge ${ivrDispositionClass(pick(row, "call_disposition_id", "callDispositionId"))}">${escapeHtml(pick(row, "call_disposition_flag_desc", "callDispositionFlagDesc") || "")}</span></td>
        <td><button class="small-btn" data-trace-call="${escapeHtml(callId)}">Trace</button></td>
    </tr>`;
    }).join("") || `<tr><td colspan="8">No CVP IVR node rows for selected dates/app/filters.</td></tr>`;
    document.querySelectorAll("[data-trace-call]").forEach(button => {
        button.addEventListener("click", () => {
            qs("#callKeyFilter").value = button.dataset.traceCall;
            switchView("calls");
            refresh();
        });
    });
}

function renderCvpJourney() {
    const grid = qs("#cvpJourneyGrid");
    const count = qs("#cvpJourneyCount");
    if (!grid || !count) return;
    const journeys = cvpJourneys();
    const totalPages = Math.max(1, Math.ceil(journeys.length / uiState.journeyPageSize));
    uiState.journeyPage = Math.min(uiState.journeyPage, totalPages - 1);
    const start = uiState.journeyPage * uiState.journeyPageSize;
    const pageRows = journeys.slice(start, start + uiState.journeyPageSize);
    count.textContent = `${journeys.length} calls`;
    qs("#journeyPageInfo").textContent = `Page ${uiState.journeyPage + 1}/${totalPages}`;
    qs("#journeyPrevPage").disabled = uiState.journeyPage === 0;
    qs("#journeyNextPage").disabled = uiState.journeyPage >= totalPages - 1;
    grid.innerHTML = pageRows.map(journey => `<article class="journey-card">
        <div class="journey-head">
            <div>
                <strong>${escapeHtml(journey.caller || "Unknown caller")}</strong>
                <span>${escapeHtml(journey.callId)}</span>
            </div>
            <span class="badge ${ivrDispositionClass(journey.dispositionId)}">${escapeHtml(journey.disposition || "Unknown")}</span>
        </div>
        <div class="journey-meta">
            <span>${escapeHtml(journey.app || "Unknown app")}</span>
            <span>${escapeHtml(journey.start || "")}</span>
            <span>${escapeHtml(journey.duration || "")} sec</span>
        </div>
        <div class="journey-path">
            ${journey.nodes.map((node, index) => `<span class="${/agent routing/i.test(node) ? "terminal" : ""}"><b>${index + 1}</b>${escapeHtml(node)}</span>`).join("<i>-&gt;</i>")}
        </div>
        <button class="small-btn" data-trace-call="${escapeHtml(journey.callId)}">Open Trace</button>
    </article>`).join("") || `<div class="empty-state"><strong>No CVP journey rows</strong><span>Select a date/app with CVP Reporting data.</span></div>`;
}

function cvpJourneys() {
    const map = new Map();
    state.cvpIvrNodes.forEach(row => {
        const callId = pick(row, "call_id", "callId") || "UNKNOWN";
        const existing = map.get(callId) || {
            callId,
            caller: pick(row, "caller_number", "callerNumber"),
            start: pick(row, "call_start_time", "callStartTime"),
            app: pick(row, "app_name", "appName"),
            duration: pick(row, "duration"),
            disposition: pick(row, "call_disposition_flag_desc", "callDispositionFlagDesc"),
            dispositionId: pick(row, "call_disposition_id", "callDispositionId"),
            nodes: []
        };
        const flag = pick(row, "flag");
        if (flag && !existing.nodes.includes(flag)) existing.nodes.push(flag);
        map.set(callId, existing);
    });
    return [...map.values()]
        .map(journey => ({ ...journey, nodes: orderJourneyNodes(journey.nodes) }))
        .sort((a, b) => String(b.start || "").localeCompare(String(a.start || "")));
}

function orderJourneyNodes(nodes) {
    return [...nodes].sort((left, right) => journeyNodeRank(left) - journeyNodeRank(right));
}

function journeyNodeRank(node) {
    const value = String(node || "").toLowerCase();
    if (value.includes("language")) return 10;
    if (value.includes("mainmenu") || value.includes("main menu")) return 20;
    if (value.includes("menu")) return 30;
    if (value.includes("product")) return 35;
    if (value.includes("agent routing")) return 90;
    return 50;
}

function ivrDispositionClass(code) {
    const value = Number(code);
    if ([18, 1001, 1044].includes(value)) return "down";
    if (value === 2) return "warn";
    return "up";
}

function renderAgents() {
    renderAgentFilters();
    const agents = filteredAgents();
    renderAgentVisuals(agents);
    const totalPages = Math.max(1, Math.ceil(agents.length / uiState.agentPageSize));
    uiState.agentPage = Math.min(uiState.agentPage, totalPages - 1);
    const start = uiState.agentPage * uiState.agentPageSize;
    const pageRows = agents.slice(start, start + uiState.agentPageSize);
    qs("#agentCount").textContent = `${agents.length} rows`;
    qs("#agentPageInfo").textContent = `Page ${uiState.agentPage + 1}/${totalPages}`;
    qs("#agentPrevPage").disabled = uiState.agentPage === 0;
    qs("#agentNextPage").disabled = uiState.agentPage >= totalPages - 1;
    qs("#agentsTable").innerHTML = pageRows.map(agent => {
        const status = effectiveAgentStatus(agent);
        const occupancy = pick(agent, "occupancy_pct", "occupancyPct");
        const adherence = pick(agent, "adherence_pct", "adherencePct");
        const liveDialogs = activeDialogsForAgent(agent);
        const firstDialog = liveDialogs[0] || {};
        return `<tr>
            <td><strong>${escapeHtml(pick(agent, "agent_name", "agentName") || "")}</strong><span class="subline">${escapeHtml(pick(agent, "agent_id", "agentId") || "")} - ${escapeHtml(pick(agent, "team") || "UNKNOWN")}</span></td>
            <td><span class="badge ${status}">${escapeHtml(status.replace("_", " "))}</span></td>
            <td>${fmt(liveDialogs.length)}</td>
            <td>${escapeHtml(firstDialog.fromAddress || firstDialog.ani || "--")}</td>
            <td>${fmt(pick(agent, "calls_handled", "callsHandled"))}</td>
            <td>${seconds(pick(agent, "avg_handle_time", "avgHandleTime"))}</td>
            <td>${progressCell(occupancy)}</td>
            <td>${progressCell(adherence)}</td>
            <td>${fmt(pick(agent, "transfers"))}</td>
            <td>${minutes(pick(agent, "not_ready_time_min", "notReadyTimeMin"))}</td>
        </tr>`;
    }).join("") || `<tr><td colspan="10">No agents match current filters.</td></tr>`;
}

function filteredAgents() {
    const selectedTeam = document.querySelector(".team-filter.active")?.dataset.team || "ALL";
    const statusFilter = firstFilterValue("#agentStatusFilter").toLowerCase();
    const search = firstFilterValue("#agentSearchFilter").toLowerCase();
    return (selectedTeam === "ALL"
        ? state.agents
        : state.agents.filter(agent => (pick(agent, "team") || "UNKNOWN") === selectedTeam))
        .filter(agent => !statusFilter || effectiveAgentStatus(agent) === statusFilter)
        .filter(agent => {
            if (!search) return true;
            return [
                pick(agent, "agent_name", "agentName"),
                pick(agent, "agent_id", "agentId"),
                pick(agent, "team"),
                pick(agent, "skill_group", "skillGroup")
            ].join(" ").toLowerCase().includes(search);
        });
}

function renderAgentVisuals(agents) {
    renderAgentKpis(agents);
    renderAgentCards(agents);
    renderAgentCharts(agents);
    renderAgentApiWorkspace();
    renderRunningAgentCalls();
    renderAgentActionCenter();
}

function renderAgentKpis(agents) {
    const total = agents.length;
    const active = agents.filter(agent => num(pick(agent, "calls_handled", "callsHandled")) > 0).length;
    const handled = sum(agents, "calls_handled", "callsHandled");
    const aht = average(agents.filter(agent => pick(agent, "avg_handle_time", "avgHandleTime") !== null), "avg_handle_time", "avgHandleTime");
    const teams = unique(agents.map(agent => pick(agent, "team") || "UNKNOWN")).length;
    const finesseUsers = finesseDirectoryCount();
    const liveStates = parsedFinesseUsers();
    const activeDialogs = parsedFinesseDialogs().filter(dialog => dialog.state && !/ended|dropped|failed/i.test(dialog.state)).length;
    qs("#agentKpis").innerHTML = [
        agentKpi("Agents", total, `${teams} teams`),
        agentKpi("Active", active, "handled calls in range"),
        agentKpi("Handled", fmt(handled), "selected interval"),
        agentKpi("AHT", metricSeconds(aht), "Cisco interval/proxy"),
        agentKpi("Finesse Users", finesseUsers ?? "--", "live directory"),
        agentKpi("Ready", liveStates.filter(user => user.state === "READY").length, "Finesse live"),
        agentKpi("Not Ready", liveStates.filter(user => user.state === "NOT_READY").length, "Finesse live"),
        agentKpi("Running Calls", activeDialogs, "Finesse dialogs")
    ].join("");
}

function agentKpi(label, value, detail) {
    return `<article class="kpi-card compact agent-kpi"><div class="kpi-top"><span>${escapeHtml(label)}</span></div><strong>${escapeHtml(String(value))}</strong><small>${escapeHtml(detail)}</small></article>`;
}

function renderAgentCards(agents) {
    const top = [...agents]
        .sort((a, b) => num(pick(b, "calls_handled", "callsHandled")) - num(pick(a, "calls_handled", "callsHandled")))
        .slice(0, 8);
    qs("#agentVisualCards").innerHTML = top.map(agent => {
        const status = effectiveAgentStatus(agent);
        const calls = num(pick(agent, "calls_handled", "callsHandled"));
        const occupancy = pick(agent, "occupancy_pct", "occupancyPct");
        const adherence = pick(agent, "adherence_pct", "adherencePct");
        return `<article class="agent-card">
            <div class="agent-avatar">${escapeHtml(agentInitials(agent))}</div>
            <div class="agent-card-main">
                <div class="agent-card-head">
                    <div><strong>${escapeHtml(pick(agent, "agent_name", "agentName") || "Unknown Agent")}</strong><span>${escapeHtml(pick(agent, "agent_id", "agentId") || "")} - ${escapeHtml(pick(agent, "team") || "UNKNOWN")}</span></div>
                    <span class="badge ${status}">${escapeHtml(status.replace("_", " "))}</span>
                </div>
                <div class="agent-card-stats">
                    <span>Calls <b>${fmt(calls)}</b></span>
                    <span>AHT <b>${seconds(pick(agent, "avg_handle_time", "avgHandleTime"))}s</b></span>
                    <span>Transfers <b>${fmt(pick(agent, "transfers"))}</b></span>
                    <span>Not Ready <b>${minutes(pick(agent, "not_ready_time_min", "notReadyTimeMin"))}</b></span>
                </div>
                <div class="agent-mini-bars">
                    <label>Occupancy ${pct(occupancy)}</label>${miniBar(occupancy)}
                    <label>Adherence ${pct(adherence)}</label>${miniBar(adherence)}
                </div>
            </div>
        </article>`;
    }).join("") || `<div class="empty-state"><strong>No agents</strong><span>Change filters or date range.</span></div>`;
}

function renderAgentCharts(agents) {
    const top = [...agents]
        .sort((a, b) => num(pick(b, "calls_handled", "callsHandled")) - num(pick(a, "calls_handled", "callsHandled")))
        .slice(0, 10);
    const labels = top.map(shortAgentName);
    drawStackedBarChart(qs("#agentAhtChart"), labels, [
        { label: "Talk", color: "#2ed3c2", values: top.map(agent => num(pick(agent, "avg_talk_time", "avgTalkTime")) || num(pick(agent, "avg_handle_time", "avgHandleTime"))) },
        { label: "Hold", color: "#f4a51c", values: top.map(agent => num(pick(agent, "avg_hold_time", "avgHoldTime"))) },
        { label: "Wrap", color: "#3d82f6", values: top.map(agent => num(pick(agent, "avg_wrap_time", "avgWrapTime"))) }
    ]);
    drawHorizontalBarChart(qs("#agentOccupancyChart"), labels, top.map(agent => pick(agent, "occupancy_pct", "occupancyPct") ?? derivedAgentOccupancy(agent)), colors);
}

function renderAgentApiWorkspace() {
    const finesseUsers = finesseDirectoryCount();
    const finesseTeams = countXmlTags((state.finesseTeams.find(item => String(pick(item, "name") || "").toLowerCase().includes("teams directory")) || {}).body, "Team");
    const agentActions = state.pcceActions.filter(action => /agent|team|skill|attribute/i.test(`${pick(action, "id")} ${pick(action, "name")} ${pick(action, "category")}`));
    qs("#agentApiWorkspace").innerHTML = [
        apiWorkspaceCard("Finesse Users", finesseUsers ?? "--", "GET /finesse/api/Users", "Live agent directory and desktop-visible users"),
        apiWorkspaceCard("Finesse Teams", finesseTeams ?? "--", "GET /finesse/api/Teams", "Supervisor/team inventory from Finesse"),
        apiWorkspaceCard("Dialogs", state.finesseDialogs.length, "GET /finesse/api/User/{id}/Dialogs", "Live call/dialog state per user"),
        apiWorkspaceCard("PCCE Agent Config", agentActions.length, "Unified Config", "Agent, team, skill group, and attribute APIs")
    ].join("") + `<div class="api-chip-row">${agentActions.slice(0, 10).map(action => `<button class="small-btn" data-action-id="${escapeHtml(pick(action, "id") || "")}" ${pick(action, "enabled") ? "" : "disabled"}>${escapeHtml(pick(action, "name") || pick(action, "id") || "")}</button>`).join("")}</div>`;
    document.querySelectorAll("#agentApiWorkspace [data-action-id]").forEach(button => {
        button.addEventListener("click", () => executePcceAction(button.dataset.actionId));
    });
}

function renderRunningAgentCalls() {
    const calls = parsedFinesseDialogs();
    const activeCalls = calls.filter(call => call.dialogId || call.fromAddress || call.toAddress || call.state);
    qs("#runningCallCount").textContent = `${activeCalls.length} dialogs`;
    qs("#runningCallGrid").innerHTML = activeCalls.map(call => `<article class="running-call-card">
        <div class="running-call-head">
            <div><strong>${escapeHtml(call.agentId || call.userId || "Unknown Agent")}</strong><span>${escapeHtml(call.dialogId || "No dialog id")}</span></div>
            <span class="badge ${dialogBadge(call.state)}">${escapeHtml(call.state || "unknown")}</span>
        </div>
        <div class="running-call-meta">
            <span>Caller <b>${escapeHtml(call.fromAddress || call.ani || "--")}</b></span>
            <span>Dialed <b>${escapeHtml(call.toAddress || call.dnis || "--")}</b></span>
            <span>Media <b>${escapeHtml(call.mediaType || "--")}</b></span>
            <span>Call Type <b>${escapeHtml(call.callType || "--")}</b></span>
        </div>
        <div class="running-call-flow">
            ${call.participants.map(participant => `<span>${escapeHtml(participant)}</span>`).join("") || "<span>No participant detail</span>"}
        </div>
        <details><summary>Raw Finesse Dialog</summary><pre class="mini-json">${escapeHtml(call.raw || "")}</pre></details>
    </article>`).join("") || `<div class="empty-state"><strong>No running calls found</strong><span>Finesse Dialogs returned no active dialog details for discovered users.</span></div>`;
}

function renderAgentActionCenter() {
    const pcceActions = state.pcceActions.filter(action => /agent|team|skill|attribute/i.test(`${pick(action, "id")} ${pick(action, "name")} ${pick(action, "category")}`));
    const finesseActions = [
        ["Set Agent Ready", "PUT", "/finesse/api/User/{id}", "<User><state>READY</state></User>", "Agent or supervisor permission required"],
        ["Set Agent Not Ready", "PUT", "/finesse/api/User/{id}", "<User><state>NOT_READY</state><reasonCodeId>{id}</reasonCodeId></User>", "Requires valid reason code"],
        ["Dialog Hold", "PUT", "/finesse/api/Dialog/{dialogId}", "<Dialog><requestedAction>HOLD</requestedAction></Dialog>", "Only while dialog is active and permitted"],
        ["Dialog Retrieve", "PUT", "/finesse/api/Dialog/{dialogId}", "<Dialog><requestedAction>RETRIEVE</requestedAction></Dialog>", "Only for held dialog"],
        ["Dialog Drop", "PUT", "/finesse/api/Dialog/{dialogId}", "<Dialog><requestedAction>DROP</requestedAction></Dialog>", "High impact; keep disabled unless approved"]
    ];
    qs("#agentActionCenter").innerHTML = `
        <div class="action-section">
            <h3>PCCE Unified Config Actions</h3>
            <div class="api-chip-row">${pcceActions.map(action => `<button class="small-btn" data-action-id="${escapeHtml(pick(action, "id") || "")}" ${pick(action, "enabled") ? "" : "disabled"}>${escapeHtml(pick(action, "name") || pick(action, "id") || "")}</button>`).join("") || "<span class='muted-value'>No agent configuration actions loaded</span>"}</div>
        </div>
        <div class="action-section">
            <h3>Finesse Operational Actions</h3>
            <div class="agent-action-list">${finesseActions.map(([name, method, path, body, note]) => `<article class="agent-action-card">
                <strong>${escapeHtml(name)}</strong>
                <span>${escapeHtml(method)} ${escapeHtml(path)}</span>
                <code>${escapeHtml(body)}</code>
                <small>${escapeHtml(note)}. Execution is intentionally not enabled until an admin approves action safety.</small>
            </article>`).join("")}</div>
        </div>`;
    document.querySelectorAll("#agentActionCenter [data-action-id]").forEach(button => {
        button.addEventListener("click", () => executePcceAction(button.dataset.actionId));
    });
}

function apiWorkspaceCard(title, value, method, detail) {
    return `<article class="agent-api-card"><span>${escapeHtml(title)}</span><strong>${escapeHtml(String(value))}</strong><small>${escapeHtml(method)}</small><p>${escapeHtml(detail)}</p></article>`;
}

function finesseDirectoryCount() {
    const directory = state.finesseAgents.find(item => String(pick(item, "name") || "").toLowerCase().includes("users directory"));
    return directory ? countXmlTags(pick(directory, "body"), "User") : null;
}

function parsedFinesseUsers() {
    return state.finesseAgents
        .filter(item => /^User\s+/i.test(String(pick(item, "name") || "")) && num(pick(item, "status_code", "statusCode")) < 400)
        .map(item => {
            const body = pick(item, "body") || "";
            return {
                id: xmlTag(body, "id") || String(pick(item, "name") || "").replace(/^User\s+/i, ""),
                loginId: xmlTag(body, "loginId"),
                firstName: xmlTag(body, "firstName"),
                lastName: xmlTag(body, "lastName"),
                extension: xmlTag(body, "extension"),
                state: xmlTag(body, "state"),
                teamId: xmlTag(body, "teamId"),
                teamName: xmlTag(body, "teamName"),
                roles: xmlTags(body, "role").join(", "),
                raw: body
            };
        });
}

function parsedFinesseDialogs() {
    const dialogs = [];
    state.finesseDialogs.forEach(item => {
        const userId = String(pick(item, "name") || "").replace(/^Dialogs\s+/i, "");
        const body = String(pick(item, "body") || "");
        splitXmlBlocks(body, "Dialog").forEach(block => {
            const media = firstXmlBlock(block, "mediaProperties") || block;
            const participants = splitXmlBlocks(block, "Participant").map(participant =>
                [
                    xmlTag(participant, "mediaAddress"),
                    xmlTag(participant, "state"),
                    xmlTag(participant, "stateCause")
                ].filter(Boolean).join(" - "));
            dialogs.push({
                userId,
                agentId: userId,
                dialogId: xmlTag(block, "id"),
                state: xmlTag(block, "state"),
                mediaType: xmlTag(block, "mediaType"),
                fromAddress: xmlTag(media, "fromAddress") || xmlTag(media, "fromAddressDisplayName"),
                toAddress: xmlTag(media, "toAddress") || xmlTag(media, "DNIS"),
                ani: xmlTag(media, "ANI"),
                dnis: xmlTag(media, "DNIS"),
                callType: xmlTag(media, "callTypeName") || xmlTag(media, "callTypeId"),
                callKey: xmlTag(media, "callKeyCallId") || xmlTag(media, "callKeyPrefix"),
                participants,
                raw: block
            });
        });
    });
    return dialogs;
}

function activeDialogsForAgent(agent) {
    const agentId = String(pick(agent, "agent_id", "agentId") || "").trim().toLowerCase();
    const agentName = String(pick(agent, "agent_name", "agentName") || "").trim().toLowerCase();
    const user = matchingFinesseUser(agent);
    const finesseIds = [user?.id, user?.loginId, user?.extension].filter(Boolean).map(value => String(value).toLowerCase());
    return parsedFinesseDialogs().filter(dialog => {
        const dialogUser = String(dialog.userId || dialog.agentId || "").toLowerCase();
        const raw = String(dialog.raw || "").toLowerCase();
        return (agentId && (dialogUser.includes(agentId) || raw.includes(agentId)))
            || (agentName && raw.includes(agentName))
            || finesseIds.some(value => dialogUser === value || dialogUser.includes(value) || raw.includes(value));
    });
}

function splitXmlBlocks(xml, tagName) {
    const text = String(xml || "");
    const pattern = new RegExp(`<${tagName}\\b[^>]*>[\\s\\S]*?<\\/${tagName}>`, "gi");
    return text.match(pattern) || [];
}

function firstXmlBlock(xml, tagName) {
    return splitXmlBlocks(xml, tagName)[0] || "";
}

function xmlTags(xml, tagName) {
    const text = String(xml || "");
    const pattern = new RegExp(`<${tagName}\\b[^>]*>\\s*([\\s\\S]*?)\\s*<\\/${tagName}>`, "gi");
    const values = [];
    let match;
    while ((match = pattern.exec(text)) !== null) {
        values.push(stripXml(match[1]));
    }
    return values;
}

function stripXml(value) {
    return String(value || "").replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim();
}

function dialogBadge(state) {
    const value = String(state || "").toLowerCase();
    if (/active|talking|initiated|alerting|reserved/.test(value)) return "on_call";
    if (/held|hold/.test(value)) return "warn";
    if (/drop|fail|end/.test(value)) return "down";
    return "up";
}

function agentInitials(agent) {
    const name = pick(agent, "agent_name", "agentName") || pick(agent, "agent_id", "agentId") || "?";
    return String(name).split(/\s+/).filter(Boolean).slice(0, 2).map(part => part[0]).join("").toUpperCase();
}

function shortAgentName(agent) {
    return String(pick(agent, "agent_name", "agentName") || pick(agent, "agent_id", "agentId") || "Agent").split(/\s+/)[0].slice(0, 12);
}

function miniBar(value) {
    const width = value === null || value === undefined ? 0 : Math.max(0, Math.min(100, num(value)));
    return `<span class="mini-track"><i style="width:${width}%"></i></span>`;
}

function derivedAgentOccupancy(agent) {
    const calls = num(pick(agent, "calls_handled", "callsHandled"));
    return Math.min(100, calls * 5);
}

function effectiveAgentStatus(agent) {
    return finesseStatusForAgent(agent) || String(pick(agent, "status") || "offline").toLowerCase();
}

function finesseStatusForAgent(agent) {
    const match = matchingFinesseUser(agent);
    return match ? mapFinesseState(match.state) : null;
}

function matchingFinesseUser(agent) {
    const agentId = String(pick(agent, "agent_id", "agentId") || "").trim().toLowerCase();
    const agentName = String(pick(agent, "agent_name", "agentName") || "").trim().toLowerCase();
    if (!agentId && !agentName) return null;
    return parsedFinesseUsers().find(user => {
        const fields = [user.id, user.loginId, user.extension, `${user.firstName || ""} ${user.lastName || ""}`]
            .filter(Boolean)
            .map(value => String(value).trim().toLowerCase());
        const raw = String(user.raw || "").toLowerCase();
        return fields.some(value => (agentId && (value === agentId || value.includes(agentId) || agentId.includes(value)))
                || (agentName && (value === agentName || value.includes(agentName) || agentName.includes(value))))
            || (agentId && raw.includes(agentId))
            || (agentName && raw.includes(agentName));
    }) || null;
}

function mapFinesseState(value) {
    const finesseState = String(value || "").trim().toUpperCase();
    if (!finesseState) return null;
    if (finesseState === "READY") return "available";
    if (["TALKING", "RESERVED", "HOLD", "ACTIVE"].includes(finesseState)) return "on_call";
    if (finesseState.includes("WORK")) return "wrap_up";
    if (finesseState === "NOT_READY") return "not_ready";
    if (["LOGOUT", "LOGGED_OUT", "OFFLINE"].includes(finesseState)) return "offline";
    return finesseState.toLowerCase().replaceAll(" ", "_");
}

function renderFinesse() {
    const agentGrid = qs("#finesseAgentGrid");
    if (!agentGrid) return;
    const directory = state.finesseAgents.find(item => String(pick(item, "name") || "").toLowerCase().includes("users directory"));
    const directoryCount = directory ? countXmlTags(pick(directory, "body"), "User") : null;
    qs("#finesseAgentCount").textContent = directoryCount === null
        ? `${state.finesseAgents.length} Finesse probes`
        : `${directoryCount} Finesse users discovered`;
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
    const totalPages = Math.max(1, Math.ceil(state.callTypes.length / uiState.callTypePageSize));
    uiState.callTypePage = Math.min(uiState.callTypePage, totalPages - 1);
    const start = uiState.callTypePage * uiState.callTypePageSize;
    const rows = state.callTypes.slice(start, start + uiState.callTypePageSize);
    qs("#callTypeCount").textContent = `${state.callTypes.length} rows`;
    qs("#callTypePageInfo").textContent = `Page ${uiState.callTypePage + 1}/${totalPages}`;
    qs("#callTypePrevPage").disabled = uiState.callTypePage === 0;
    qs("#callTypeNextPage").disabled = uiState.callTypePage >= totalPages - 1;
    qs("#callTypeTable").innerHTML = rows.map(row => `<tr>
        <td>${escapeHtml(pick(row, "date") || "")}</td>
        <td>${escapeHtml(pick(row, "hour") ?? "")}</td>
        <td>${escapeHtml(pick(row, "call_type", "callType") || "")}</td>
        <td>${escapeHtml(pick(row, "skill_group", "skillGroup") || "")}</td>
        <td>${fmt(pick(row, "calls"))}</td>
        <td>${fmt(pick(row, "handled_calls", "handledCalls"))}</td>
    </tr>`).join("") || `<tr><td colspan="6">No call type rows for current filters.</td></tr>`;
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
                    <h3>${escapeHtml(pick(component, "display_name", "displayName") || pick(component, "name") || "")}</h3>
                    <span class="badge ${badgeClass}">${escapeHtml(stateValue)}</span>
                    <p>${componentMeta(component)}</p>
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
            <h3>${escapeHtml(pick(metric, "display_name", "displayName") || pick(metric, "component") || "")}</h3>
            <span class="badge ${badgeClass}">${escapeHtml(stateValue.replace("_", " "))}</span>
            <p>${componentMeta(metric)}</p>
            <p>${escapeHtml(pick(metric, "host") || "")} - ${escapeHtml(pick(metric, "method") || "")}</p>
            <p>CPU ${metricPct(pick(metric, "cpu_pct", "cpuPct"))} | Memory ${metricPct(pick(metric, "memory_pct", "memoryPct"))} | Disk ${metricPct(pick(metric, "disk_pct", "diskPct"))}</p>
            <p>${escapeHtml(pick(metric, "services") || "")}</p>
            <p>${escapeHtml(pick(metric, "detail") || "")}</p>
        </article>`;
    }).join("") || `<article class="component-card"><h3>Server Metrics</h3><span class="badge warn">not configured</span><p>Configure SNMP/WMI/exporter collection for remote servers.</p></article>`;
}

function componentMeta(item) {
    const parts = [pick(item, "site"), pick(item, "side"), pick(item, "tier")].filter(Boolean);
    return parts.length ? parts.map(part => escapeHtml(part)).join(" | ") : "Topology metadata not set";
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

function renderCvpApi() {
    const grid = qs("#cvpApiGrid");
    if (!grid) return;
    grid.innerHTML = state.cvpApiStatus.map(item => {
        const stateValue = String(pick(item, "state") || "UNKNOWN").toLowerCase();
        const badgeClass = stateValue === "up" ? "up" : stateValue === "down" ? "down" : stateValue === "disabled" ? "disabled" : "warn";
        const statusCode = num(pick(item, "status_code", "statusCode"));
        return `<article class="component-card api-health-card">
            <div class="component-title"><h3>${escapeHtml(pick(item, "name") || "")}</h3><span class="badge ${badgeClass}">${escapeHtml(stateValue)}</span></div>
            <p>${escapeHtml(pick(item, "category") || "")}</p>
            <small>${escapeHtml(pick(item, "method") || "GET")} - ${escapeHtml(pick(item, "target") || "")}</small>
            <div class="component-detail">${fmt(pick(item, "latency_ms", "latencyMs"))} ms - ${statusCode ? `HTTP ${statusCode}` : escapeHtml(pick(item, "detail") || "")}</div>
        </article>`;
    }).join("") || `<article class="component-card"><h3>CVP API</h3><span class="badge warn">disabled</span><p>Set CVP_API_ENABLED=true and CVP_API_BASE_URL to enable CVP REST visualization.</p></article>`;

    qs("#cvpCapabilityList").innerHTML = state.cvpCapabilities.map(item =>
        metricRow(pick(item, "category"), pick(item, "capability"))
    ).join("") || metricRow("CVP Developer APIs", "Capability catalog unavailable");

    qs("#cvpFunctionTable").innerHTML = state.cvpFunctions.map(item => `<tr>
        <td>${escapeHtml(pick(item, "category") || "")}</td>
        <td>${escapeHtml(pick(item, "function") || "")}</td>
        <td><span class="badge up">${escapeHtml(pick(item, "method") || "GET")}</span></td>
        <td>${escapeHtml(pick(item, "path") || "")}</td>
        <td>${escapeHtml(pick(item, "description") || "")}</td>
    </tr>`).join("");

    qs("#cvpActionTable").innerHTML = state.cvpActions.map(item => {
        const enabled = Boolean(pick(item, "enabled"));
        const id = pick(item, "id") || "";
        return `<tr>
            <td>${escapeHtml(pick(item, "category") || "")}</td>
            <td>${escapeHtml(pick(item, "name") || id)}</td>
            <td><span class="badge ${enabled ? "up" : "warn"}">${escapeHtml(pick(item, "method") || "GET")}</span></td>
            <td>${escapeHtml(pick(item, "path") || "")}</td>
            <td>${enabled ? "Enabled" : "Disabled"}</td>
            <td><button class="small-btn" data-cvp-action-id="${escapeHtml(id)}" ${enabled ? "" : "disabled"}>Run</button></td>
        </tr>`;
    }).join("");
    document.querySelectorAll("[data-cvp-action-id]").forEach(button => {
        button.addEventListener("click", () => executeCvpAction(button.dataset.cvpActionId));
    });
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
        <pre class="body-preview">${escapeHtml(finesseBodyPreview(item))}</pre>
    </article>`;
}

function finesseBodyPreview(item) {
    const name = String(pick(item, "name") || "");
    const body = String(pick(item, "body") || "");
    if (name.toLowerCase().includes("users directory")) {
        const count = countXmlTags(body, "User");
        return count === null ? snippet(body, 420) : `${count} users returned by /finesse/api/Users`;
    }
    if (body.includes("custom Cisco error page") || body.includes("<title> Cisco System")) {
        if (name.toLowerCase().includes("queues directory")) {
            return "Queue directory endpoint is not exposed on this Finesse deployment. Use Teams, Users, Dialogs, and HDS/CUIC queue metrics.";
        }
        return "Cisco error page returned. For /User/{id}, verify the value is the Finesse login ID from AW LoginName, not only SkillTargetID.";
    }
    return snippet(body, 420);
}

function countXmlTags(body, tagName) {
    const text = String(body || "");
    if (!text) return null;
    const matches = text.match(new RegExp(`<${tagName}(\\s|>)`, "g"));
    return matches ? matches.length : null;
}

function xmlTag(body, tagName) {
    const text = String(body || "");
    const match = text.match(new RegExp(`<${tagName}[^>]*>([\\s\\S]*?)</${tagName}>`, "i"));
    return match ? match[1].trim() : "";
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
    renderMachineInventory();
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

function renderMachineInventory() {
    const grid = qs("#inventoryGrid");
    const summary = qs("#inventorySummary");
    if (!grid || !summary) return;
    populateInventoryTypeFilter();
    const search = firstFilterValue("#inventorySearch").toLowerCase();
    const typeFilter = firstFilterValue("#inventoryTypeFilter");
    const machines = state.machineInventory.filter(machine => {
        const haystack = [
            machine.name, machine.hostName, machine.type, machine.version,
            ...machine.networks.map(network => network.address),
            ...machine.services.map(service => `${service.type} ${service.port} ${service.status || ""}`)
        ].join(" ").toLowerCase();
        return (!typeFilter || machine.type === typeFilter) && (!search || haystack.includes(search));
    });
    summary.textContent = `${machines.length}/${state.machineInventory.length} machines`;
    grid.innerHTML = machines.map(machine => inventoryCard(machine)).join("") ||
        `<div class="empty-state"><strong>No machine inventory loaded</strong><span>Use Load From PCCE or run the machineInventory.list API action.</span></div>`;
}

function inventoryCard(machine) {
    const publicIp = machine.networks.find(network => network.type === "PUBLIC")?.address || "";
    const privateIp = machine.networks.find(network => network.type === "PRIVATE")?.address || "";
    const serviceCount = machine.services.length;
    const alertTone = machine.statusCount ? "warn" : "up";
    return `<article class="inventory-card">
        <div class="inventory-head">
            <h3>${escapeHtml(inventoryDisplayName(machine))}</h3>
            <span class="badge ${alertTone}">${machine.statusCount || 0} Alerts</span>
        </div>
        <p>${escapeHtml(machine.hostName || machine.name || "")}</p>
        <div class="inventory-facts">
            <span>Type <b>${escapeHtml(machine.type || "--")}</b></span>
            <span>Public IP <b>${escapeHtml(publicIp || "--")}</b></span>
            <span>Private IP <b>${escapeHtml(privateIp || "--")}</b></span>
            <span>Version <b>${escapeHtml(machine.version || "--")}</b></span>
        </div>
        <div class="service-chips">
            ${machine.services.slice(0, 8).map(service => `<span title="${escapeHtml(service.description || service.type)}">${escapeHtml(service.type)}${service.port ? `:${escapeHtml(String(service.port))}` : ""}</span>`).join("")}
            ${serviceCount > 8 ? `<span>+${serviceCount - 8} more</span>` : ""}
        </div>
    </article>`;
}

function inventoryDisplayName(machine) {
    const type = String(machine.type || "");
    if (type === "CCE_ROGGER") return "Unified CCE Rogger";
    if (type === "CCE_AW") return "Unified CCE AW-HDS-DDS";
    if (type === "CCE_PG") return "Unified CCE PG";
    if (type.includes("CVP")) return "Unified CVP";
    if (type === "FINESSE") return "Finesse";
    if (type.includes("CUIC")) return "CUIC-LD-IDS Publisher";
    if (type.includes("CUCM")) return "Unified CM Publisher";
    if (type.includes("VVB")) return "Virtualized Voice Browser";
    return machine.name || machine.hostName || type || "Machine";
}

function populateInventoryTypeFilter() {
    const select = qs("#inventoryTypeFilter");
    if (!select) return;
    const current = select.value;
    const types = unique(state.machineInventory.map(machine => machine.type).filter(Boolean)).sort();
    select.innerHTML = `<option value="">All</option>` + types.map(type => `<option value="${escapeHtml(type)}">${escapeHtml(type)}</option>`).join("");
    select.value = types.includes(current) ? current : "";
}

function parseMachineInventory(xmlText) {
    const text = String(xmlText || "");
    if (!text.trim()) return [];
    const doc = new DOMParser().parseFromString(text, "application/xml");
    if (!doc.querySelector("parsererror")) {
        return [...doc.querySelectorAll("machine")].map(machineNodeFromXml);
    }
    return parseMachineInventoryFallback(text);
}

function machineNodeFromXml(machine) {
    const networkContainer = childElement(machine, "networks");
    const networks = childrenByTag(networkContainer, "network").map(network => ({
        type: nodeText(network, "type"),
        address: nodeText(network, "address")
    }));
    const services = childrenByTag(networkContainer, "network")
            .flatMap(network => childrenByTag(childElement(network, "services"), "service"))
            .map(service => ({
        type: nodeText(service, "type"),
        port: nodeText(service, "port"),
        status: nodeText(service, "status"),
        description: nodeText(service, "description"),
        userName: nodeText(service, "userName")
    }));
    return {
        name: nodeText(machine, "name"),
        hostName: nodeText(machine, "hostName"),
        type: nodeText(machine, "type"),
        version: nodeText(machine, "versionInfo > version") || nodeText(machine, "version"),
        refUrl: nodeText(machine, "refURL"),
        networks,
        services,
        statusCount: services.filter(service => service.status && service.status !== "IN_SYNC").length
    };
}

function nodeText(root, selector) {
    return root.querySelector(selector)?.textContent?.trim() || "";
}

function childElement(root, tagName) {
    return [...(root?.children || [])].find(child => child.tagName === tagName) || null;
}

function childrenByTag(root, tagName) {
    return [...(root?.children || [])].filter(child => child.tagName === tagName);
}

function parseMachineInventoryFallback(text) {
    const chunks = text.split("<machine>").slice(1).map(chunk => chunk.split("</machine>")[0]);
    return chunks.map(chunk => {
        const tag = name => (chunk.match(new RegExp(`<${name}>([\\s\\S]*?)</${name}>`)) || [])[1]?.trim() || "";
        const networks = [...chunk.matchAll(/<network>([\s\S]*?)<\/network>/g)].map(match => ({
            type: (match[1].match(/<type>([\s\S]*?)<\/type>/) || [])[1]?.trim() || "",
            address: (match[1].match(/<address>([\s\S]*?)<\/address>/) || [])[1]?.trim() || ""
        }));
        const services = [...chunk.matchAll(/<service>([\s\S]*?)<\/service>/g)].map(match => ({
            type: (match[1].match(/<type>([\s\S]*?)<\/type>/) || [])[1]?.trim() || "",
            port: (match[1].match(/<port>([\s\S]*?)<\/port>/) || [])[1]?.trim() || "",
            status: (match[1].match(/<status>([\s\S]*?)<\/status>/) || [])[1]?.trim() || "",
            description: (match[1].match(/<description>([\s\S]*?)<\/description>/) || [])[1]?.trim() || ""
        }));
        return {
            name: tag("name"),
            hostName: tag("hostName"),
            type: tag("type"),
            version: (chunk.match(/<version>([\s\S]*?)<\/version>/) || [])[1]?.trim() || "",
            refUrl: tag("refURL"),
            networks,
            services,
            statusCount: services.filter(service => service.status && service.status !== "IN_SYNC").length
        };
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

function renderAdvanced() {
    qs("#jmxGrid").innerHTML = renderIntegrationCards(state.jmxStatus, "Secure JMX");
    qs("#appDynamicsGrid").innerHTML = renderIntegrationCards(state.appDynamicsStatus, "AppDynamics");
    qs("#liveDataGrid").innerHTML = renderIntegrationCards(state.liveDataStatus, "PCCE Live Data");
    qs("#liveDataTokenGrid").innerHTML = renderIntegrationCards(state.liveDataTokenProbe, "Live Data Token");
    renderRealtimeSnapshots();
    qs("#jmxNotes").innerHTML = [
        metricRow("Cisco secure JMX", "Use CVP/OAMP secure JMX certificate exchange before enabling polling."),
        metricRow("Scope", "Best for CVP JVM health, memory, threads and selected MBeans; not a replacement for HDS/CUIC reporting."),
        metricRow("Banking control", "Use read-only JMX user, approved truststore, short timeout and explicit target list.")
    ].join("");
    qs("#appDynamicsNotes").innerHTML = [
        metricRow("Existing agents", "Use installed AppDynamics machine/application agents on PCCE nodes where available."),
        metricRow("Dashboards", "Embed or deep-link controller dashboards for CVP, Finesse, CUIC, AW/HDS and JVM tiers."),
        metricRow("Alerting", "Keep AppDynamics as telemetry source; route business-impact alerts through this portal/SMS/SMTP.")
    ].join("");
    qs("#liveDataNotes").innerHTML = [
        metricRow("Realtime first", "Use Live Data for agent state, skill group and call type realtime widgets."),
        metricRow("HDS protection", "Keep historical dashboards on bounded HDS queries; avoid aggressive refresh against HDS."),
        metricRow("CUIC alignment", "Use the same Live Data host, token path and user configured in CUIC datasource.")
    ].join("");
}

function renderRealtimeSnapshots() {
    const snapshots = state.realtimeSnapshots || [];
    const up = snapshots.filter(item => String(pick(item, "state")).toLowerCase() === "up").length;
    qs("#realtimeSnapshotSummary").innerHTML = [
        metricRow("Snapshot Sources", `${snapshots.length} configured`),
        metricRow("Available Now", `${up} responding`),
        metricRow("Refresh Strategy", "Use Live Data for wallboards; keep HDS for historical reporting."),
        metricRow("Cisco Sources", "Agent, Skill Group and Call Type realtime tables.")
    ].join("");
    qs("#realtimeSnapshotGrid").innerHTML = snapshots.map(item => {
        const rows = pick(item, "rows") || [];
        const error = pick(item, "error");
        return `<article class="component-card realtime-card">
            <div class="component-title"><h3>${escapeHtml(pick(item, "name") || "")}</h3><span class="badge ${error ? "warn" : "up"}">${escapeHtml(pick(item, "state") || "")}</span></div>
            <p>${escapeHtml(pick(item, "source") || "")}</p>
            <small>${escapeHtml(pick(item, "description") || "")}</small>
            ${error ? `<div class="component-detail warn-text">${escapeHtml(error)}</div>` : `<div class="component-detail">${rows.length} rows returned</div>`}
            ${rows.length ? `<pre class="mini-json">${escapeHtml(JSON.stringify(rows.slice(0, 3), null, 2))}</pre>` : ""}
        </article>`;
    }).join("") || `<article class="component-card"><h3>AW Real Time</h3><span class="badge warn">not loaded</span><p>No realtime snapshots returned.</p></article>`;
}

function renderIntegrationCards(items, emptyTitle) {
    return (items || []).map(item => {
        const stateValue = String(pick(item, "state") || "unknown").toLowerCase();
        const badge = stateValue.includes("configured") || stateValue.includes("mapped") ? "up"
            : stateValue.includes("disabled") ? "disabled"
            : "down";
        return `<article class="component-card">
            <div class="component-title"><h3>${escapeHtml(pick(item, "name") || "")}</h3><span class="badge ${badge}">${escapeHtml(pick(item, "state") || "")}</span></div>
            <p>${escapeHtml(pick(item, "category") || "")}</p>
            <small>${escapeHtml(pick(item, "target") || "")}</small>
            <div class="component-detail">${escapeHtml(pick(item, "detail") || "")}</div>
        </article>`;
    }).join("") || `<article class="component-card"><h3>${escapeHtml(emptyTitle)}</h3><span class="badge warn">not loaded</span><p>Configuration endpoint unavailable.</p></article>`;
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
        if (id === "machineInventory.list") {
            state.machineInventory = parseMachineInventory(pick(result, "body") || "");
            renderMachineInventory();
            switchView("spog");
        }
    } catch (error) {
        qs("#pcceActionResult").textContent = error.message;
    }
}

async function executeCvpAction(id) {
    qs("#cvpActionResult").textContent = `Running ${id}...`;
    try {
        const bodyText = qs("#cvpActionBody").value.trim();
        const pathParams = parseJsonObject(qs("#cvpPathParams").value.trim(), "Path params");
        const queryParams = parseJsonObject(qs("#cvpQueryParams").value.trim(), "Query params");
        const result = await api(`/api/v1/cvp-api/actions/${encodeURIComponent(id)}/execute`, {
            method: "POST",
            body: JSON.stringify({ body: bodyText || null, pathParams, queryParams })
        });
        qs("#cvpActionResult").textContent = JSON.stringify(result, null, 2);
    } catch (error) {
        qs("#cvpActionResult").textContent = error.message;
    }
}

async function loadMachineInventory() {
    qs("#inventorySummary").textContent = "Loading inventory from PCCE...";
    try {
        const result = await api("/api/v1/pcce-api/actions/machineInventory.list/execute", {
            method: "POST",
            body: JSON.stringify({ queryParams: { resultsPerPage: "100", time: String(Date.now()) } })
        });
        state.machineInventory = parseMachineInventory(pick(result, "body") || "");
        renderMachineInventory();
    } catch (error) {
        qs("#inventorySummary").textContent = error.message;
        state.machineInventory = [];
        renderMachineInventory();
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
        qs("#adminConfigReadinessGrid").innerHTML = "";
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
    renderAdminConfigReadiness();
    qs("#adminComponentsTable").innerHTML = state.adminComponents.map(component => `<tr>
        <td><strong>${escapeHtml(pick(component, "display_name", "displayName") || pick(component, "name") || "")}</strong><span class="subline">${escapeHtml(pick(component, "name") || "")}</span></td>
        <td>${escapeHtml(componentMeta(component))}</td>
        <td>${pick(component, "enabled") ? "true" : "false"}</td>
        <td>${escapeHtml(pick(component, "probe") || "")}</td>
        <td>${escapeHtml(pick(component, "url") || pick(component, "host") || "")}${pick(component, "port") ? `:${pick(component, "port")}` : ""}</td>
        <td>${escapeHtml(pick(component, "timeout") || "")}</td>
        <td>${pick(component, "trust_all_certificates", "trustAllCertificates") ? "trust all" : "default"}</td>
    </tr>`).join("");
    qs("#adminCapabilityCards").innerHTML = adminCapabilityCards();
}

function renderAdminConfigReadiness() {
    qs("#adminConfigReadinessGrid").innerHTML = state.adminConfigReadiness.map(item => {
        const ready = Boolean(pick(item, "ready"));
        return `<article class="component-card">
            <h3>${escapeHtml(pick(item, "area") || "")}</h3>
            <span class="badge ${ready ? "up" : "warn"}">${ready ? "ready" : "needs config"}</span>
            <p>${escapeHtml(pick(item, "finding") || "")}</p>
            <p>${escapeHtml(pick(item, "recommendation") || "")}</p>
        </article>`;
    }).join("") || `<article class="component-card"><h3>Config readiness</h3><span class="badge warn">not loaded</span><p>Admin diagnostics endpoint unavailable.</p></article>`;
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
    qs("#adminPermissionGrid").innerHTML = currentPermissionCatalog().map(permission => `<label class="check-card">
        <input type="checkbox" value="${escapeHtml(permission)}" ${selected.has(permission) ? "checked" : ""}>
        <span>${escapeHtml(permission.replaceAll("_", " "))}</span>
    </label>`).join("");
}

function currentPermissionCatalog() {
    return unique([
        ...permissionCatalog,
        ...state.adminRoles.flatMap(role => pick(role, "permissions") || [])
    ]);
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
        ["User Lifecycle", "Disable a user by clearing Enabled / allowed to login in the User Editor."],
        ["LDAP / AD Ready", "Configure APP_LDAP_* variables for AD bind/search and map AD groups to app roles."],
        ["Role Permissions", "Tune permissions for Admin, Workforce, Supervisor, Agent, and Viewer."],
        ["Components", "Review probes, target URLs, TLS trust, timeout and enabled state."],
        ["App Database", "Project plan and portal data use the app DB, not browser-only cache."],
        ["Environment Profiles", "Use application-local.yml for SIT and application-prod.yml for A/B-side production topology."],
        ["Alerts", "Webhook, SMTP and SMS thresholds are runtime configurable, then should be persisted through env/YAML."],
        ["Diagnostics", "Use /api/v1/admin/diagnostics for AW/HDS/CVP schema checks."],
        ["Cisco Inventory", "Use PCCE machineinventory from SPOG Ops to validate nodes, services, ports, and versions."]
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
        planKpi("Completed", completed, `${total ? Math.round(completed / total * 100) : 0}% done`, "emerald"),
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
            <div class="table-wrap plan-group-body"><table><thead><tr><th>Task</th><th>Priority</th><th>Status</th><th>Resource / Owner</th><th>Timeline</th><th>Progress / Controls</th></tr></thead><tbody>
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
    const id = task.id || index;
    return `<tr class="${criticalOpen ? "critical-row" : ""}">
        <td><strong>${escapeHtml(task.task)}</strong>${task.priorityNum ? `<span class="subline">Priority #${task.priorityNum}</span>` : ""}
            <span class="subline">ID ${escapeHtml(id)}${task.milestone ? ` - ${escapeHtml(task.milestone)}` : ""}</span>
            ${task.deliverable ? `<span class="subline">Deliverable: ${escapeHtml(task.deliverable)}</span>` : ""}
            ${task.dependsOn ? `<span class="subline">Depends on: ${escapeHtml(task.dependsOn)}</span>` : ""}
            ${task.blockedBy ? `<span class="subline danger-text">Blocked by: ${escapeHtml(task.blockedBy)}</span>` : ""}
        </td>
        <td>${badge(task.priority, priorityClass(task.priority))}</td>
        <td><select class="inline-input" data-plan-field="status" data-plan-index="${index}">${["COMPLETED", "IN_PROGRESS", "ON_HOLD", "PLANNED"].map(status => `<option ${task.status === status ? "selected" : ""}>${status}</option>`).join("")}</select></td>
        <td>
            <input class="inline-input" data-plan-field="resource" data-plan-index="${index}" value="${escapeHtml(task.resource || "")}">
            <input class="inline-input" data-plan-field="owner" data-plan-index="${index}" value="${escapeHtml(task.owner || "")}" placeholder="Owner">
            <input class="inline-input" data-plan-field="team" data-plan-index="${index}" value="${escapeHtml(task.team || "")}" placeholder="Team">
            ${task.shareWith ? `<span class="subline">Share: ${escapeHtml(task.shareWith)}</span>` : ""}
        </td>
        <td>
            <input class="inline-input" data-plan-field="milestone" data-plan-index="${index}" value="${escapeHtml(task.milestone || "")}" placeholder="Milestone">
            <input class="inline-input" data-plan-field="start" data-plan-index="${index}" value="${escapeHtml(task.start || "")}" placeholder="Start">
            <input class="inline-input" data-plan-field="finish" data-plan-index="${index}" value="${escapeHtml(task.finish || "")}" placeholder="Finish">
            <input class="inline-input" data-plan-field="duration" data-plan-index="${index}" type="number" min="0" value="${escapeHtml(task.duration ?? "")}" placeholder="Days">
        </td>
        <td>
            <input class="inline-input plan-pct-input" type="number" min="0" max="100" data-plan-field="pct" data-plan-index="${index}" value="${escapeHtml(task.pct)}">
            ${inlineProgress(task.pct)}
            <select class="inline-input" data-plan-field="risk" data-plan-index="${index}">${["LOW", "MEDIUM", "HIGH", "CRITICAL"].map(risk => `<option ${String(task.risk || "MEDIUM") === risk ? "selected" : ""}>${risk}</option>`).join("")}</select>
            <input class="inline-input" data-plan-field="dependsOn" data-plan-index="${index}" value="${escapeHtml(task.dependsOn || "")}" placeholder="Depends on">
            <input class="inline-input" data-plan-field="blockedBy" data-plan-index="${index}" value="${escapeHtml(task.blockedBy || "")}" placeholder="Blocked by">
            <input class="inline-input" data-plan-field="deliverable" data-plan-index="${index}" value="${escapeHtml(task.deliverable || "")}" placeholder="Deliverable">
            <input class="inline-input" data-plan-field="shareWith" data-plan-index="${index}" value="${escapeHtml(task.shareWith || "")}" placeholder="Share with">
            <input class="inline-input" data-plan-field="comments" data-plan-index="${index}" value="${escapeHtml(task.comments)}">
            <button class="small-btn" data-plan-save="${index}">Save</button>
            <button class="small-btn danger-btn" data-plan-delete="${index}">Delete</button>
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

function bindPlanEditors() {
    document.querySelectorAll("[data-plan-save]").forEach(button => {
        button.addEventListener("click", () => savePlanTask(Number(button.dataset.planSave)));
    });
    document.querySelectorAll("[data-plan-delete]").forEach(button => {
        button.addEventListener("click", () => deletePlanTask(Number(button.dataset.planDelete)));
    });
}

async function savePlanTask(index) {
    const task = { ...planTasks[index] };
    if (!task) return;
    document.querySelectorAll(`[data-plan-index="${index}"]`).forEach(input => {
        const field = input.dataset.planField;
        task[field] = ["pct", "duration"].includes(field) ? Number(input.value || 0) : input.value;
    });
    await persistPlanTask(index, task, "PUT");
}

async function addPlanTask() {
    const name = qs("#planEditTask").value.trim();
    if (!name) return;
    await persistPlanTask(null, {
        topic: qs("#planEditTopic").value,
        task: name,
        priority: qs("#planEditPriority").value,
        priorityNum: null,
        status: qs("#planEditStatus").value,
        resource: qs("#planEditResource").value.trim() || "Unassigned",
        owner: qs("#planEditOwner").value.trim(),
        team: qs("#planEditTeam").value.trim(),
        milestone: qs("#planEditMilestone").value.trim(),
        start: qs("#planEditStart").value.trim(),
        finish: qs("#planEditFinish").value.trim(),
        duration: Number(qs("#planEditDuration").value || 0),
        pct: Math.max(0, Math.min(100, Number(qs("#planEditPct").value || 0))),
        dependsOn: qs("#planEditDependsOn").value.trim(),
        blockedBy: qs("#planEditBlockedBy").value.trim(),
        risk: qs("#planEditRisk").value,
        deliverable: qs("#planEditDeliverable").value.trim(),
        shareWith: qs("#planEditShareWith").value.trim(),
        externalRef: qs("#planEditExternalRef").value.trim(),
        comments: qs("#planEditComments").value.trim()
    }, "POST");
    ["#planEditTask", "#planEditOwner", "#planEditTeam", "#planEditMilestone", "#planEditStart", "#planEditFinish",
        "#planEditDuration", "#planEditDependsOn", "#planEditBlockedBy", "#planEditDeliverable", "#planEditShareWith",
        "#planEditExternalRef", "#planEditComments"].forEach(selector => qs(selector).value = "");
}

async function resetPlanTasks() {
    const error = await safeLoad("projectTasks", "/api/v1/project/tasks", [], { timeoutMs: 8000 });
    if (error) {
        showPlanMessage(error);
        return;
    }
    planTasks = state.projectTasks || [];
    renderPlan();
    showPlanMessage("Project plan reloaded from app database.");
}

async function persistPlanTask(index, task, method) {
    const target = method === "POST" ? "/api/v1/project/tasks"
        : task.id ? `/api/v1/project/tasks/id/${encodeURIComponent(task.id)}`
            : `/api/v1/project/tasks/${index}`;
    showPlanMessage("Saving project plan...");
    try {
        await api(target, { method, body: JSON.stringify(task) });
        const error = await safeLoad("projectTasks", "/api/v1/project/tasks", [], { timeoutMs: 8000 });
        if (error) {
            showPlanMessage(error);
            return;
        }
        planTasks = state.projectTasks || [];
        renderPlan();
        showPlanMessage("Project plan saved in app database.");
    } catch (error) {
        showPlanMessage(error.message);
    }
}

async function deletePlanTask(index) {
    const task = planTasks[index];
    if (!task || !task.id) {
        showPlanMessage("Task must be saved before it can be deleted.");
        return;
    }
    if (!confirm(`Delete task: ${task.task}?`)) {
        return;
    }
    showPlanMessage("Deleting project task...");
    try {
        await api(`/api/v1/project/tasks/id/${encodeURIComponent(task.id)}`, { method: "DELETE" });
        const error = await safeLoad("projectTasks", "/api/v1/project/tasks", [], { timeoutMs: 8000 });
        if (error) {
            showPlanMessage(error);
            return;
        }
        planTasks = state.projectTasks || [];
        renderPlan();
        showPlanMessage("Project task deleted.");
    } catch (error) {
        showPlanMessage(error.message);
    }
}

async function copyProjectShareLink() {
    const link = `${location.origin}/project`;
    try {
        await navigator.clipboard.writeText(link);
        showPlanMessage(`Share link copied: ${link}`);
    } catch {
        showPlanMessage(`Share link: ${link}`);
    }
}

async function generatePlanTemplate() {
    const body = {
        topic: qs("#planTemplateTopic").value.trim() || "Team Delivery",
        team: qs("#planTemplateTeam").value.trim(),
        owner: qs("#planTemplateOwner").value.trim(),
        resource: qs("#planTemplateResource").value.trim(),
        start: qs("#planTemplateStart").value.trim(),
        finish: qs("#planTemplateFinish").value.trim(),
        shareWith: qs("#planTemplateShareWith").value.trim()
    };
    showPlanMessage("Generating team plan...");
    try {
        await api("/api/v1/project/templates", {
            method: "POST",
            body: JSON.stringify(body)
        });
        const error = await safeLoad("projectTasks", "/api/v1/project/tasks", [], { timeoutMs: 8000 });
        if (error) {
            showPlanMessage(error);
            return;
        }
        planTasks = state.projectTasks || [];
        planState.topic = body.topic;
        renderPlan();
        ["#planTemplateTopic", "#planTemplateTeam", "#planTemplateOwner", "#planTemplateResource",
            "#planTemplateStart", "#planTemplateFinish", "#planTemplateShareWith"].forEach(selector => qs(selector).value = "");
        showPlanMessage("Reusable team plan generated.");
    } catch (error) {
        showPlanMessage(error.message);
    }
}

function showPlanMessage(message) {
    const result = qs("#planActionResult") || qs("#planFilteredCount");
    if (result) result.textContent = message;
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
    activeView = view;
    document.querySelectorAll(".nav-item[data-view]").forEach(button => button.classList.toggle("active", button.dataset.view === view));
    document.querySelectorAll(".view").forEach(section => section.classList.toggle("active", section.id === `view-${view}`));
    qs("#pageTitle").textContent = pages[view][0];
    qs("#pageSubtitle").textContent = pages[view][1];
    refresh();
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
            <span>FCR<strong>${pct(item.fcr)}</strong></span>
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
    return [...map.values()].filter(item => item.offered >= num(businessSettings.minCalls)).map(item => ({
        ...item,
        service: item.serviceWeight ? item.weightedService / item.serviceWeight : derivedPct(item.handled, item.offered),
        aht: item.ahtWeight ? item.weightedAht / item.ahtWeight : businessAht(state.calls).value,
        fcr: businessFcrForCounts(item.offered, item.handled, item.abandoned),
        answerRate: derivedPct(item.handled, item.offered),
        abandonRate: derivedPct(item.abandoned, item.offered)
    })).sort((a, b) => b.offered - a.offered);
}

function serviceTrendByHour() {
    const topSkills = groupSkillMetrics().slice(0, 3).map(item => item.skill);
    const hours = unique(state.calls.map(row => num(pick(row, "hour")))).sort((a, b) => a - b);
    const labels = hours.map(hour => `${hour}:00`);
    return {
        labels,
        series: topSkills.map((skill, index) => ({
            label: skill,
            color: colors[index % colors.length],
            values: hours.map(hour => skillHourService(skill, hour))
        })).concat([{ label: "Target", color: "#f4a51c", values: labels.map(() => businessSettings.slTarget) }])
    };
}

function skillHourService(skill, hour) {
    const rows = state.calls.filter(row => (pick(row, "skill_group", "skillGroup") || "UNKNOWN") === skill && num(pick(row, "hour")) === hour);
    const real = weightedAverage(rows, "service_level_pct", "serviceLevelPct", "calls_offered", "callsOffered");
    if (real !== null) return real;
    if (businessSettings.fallbackMode !== "derived") return 0;
    return derivedPct(sum(rows, "calls_handled", "callsHandled"), sum(rows, "calls_offered", "callsOffered")) || 0;
}

function radarMetrics() {
    return groupSkillMetrics().slice(0, 3).map((skill, index) => {
        const rows = state.calls.filter(row => (pick(row, "skill_group", "skillGroup") || "UNKNOWN") === skill.skill);
        const offered = sum(rows, "calls_offered", "callsOffered");
        const handled = sum(rows, "calls_handled", "callsHandled");
        const abandoned = sum(rows, "calls_abandoned", "callsAbandoned");
        const service = businessServiceLevel(rows).value;
        const aht = businessAht(rows).value;
        return {
            label: skill.skill,
            color: colors[index % colors.length],
            metrics: [
                { label: "Service Level", value: service ?? 0 },
                { label: "FCR", value: businessFcr(rows).value ?? 0 },
                { label: "Handled", value: offered ? handled / offered * 100 : 0 },
                { label: "Low Abandon", value: offered ? Math.max(0, 100 - abandoned / offered * 100) : 0 },
                { label: "AHT", value: aht ? Math.max(0, 100 - Math.min(100, aht / Math.max(1, businessSettings.ahtTarget) * 100)) : 0 }
            ]
        };
    });
}

function businessServiceLevel(rows) {
    const real = weightedAverage(rows, "service_level_pct", "serviceLevelPct", "calls_offered", "callsOffered");
    if (real !== null) return { value: real, source: "Cisco interval ServiceLevel", short: "Cisco" };
    if (businessSettings.fallbackMode !== "derived") return { value: null, source: "Cisco interval unavailable", short: "missing" };
    const offered = sum(rows, "calls_offered", "callsOffered");
    const handled = sum(rows, "calls_handled", "callsHandled");
    return { value: derivedPct(handled, offered), source: "Derived handled/offered fallback", short: "derived" };
}

function businessAht(rows) {
    const real = weightedAverage(rows, "avg_handle_time", "avgHandleTime", "calls_handled", "callsHandled");
    if (real !== null) {
        return { value: real, source: "Cisco interval AHT", short: "Cisco" };
    }
    if (businessSettings.ahtProxyMode === "ivr_duration") {
        const proxy = averageCvpDuration();
        if (proxy !== null) {
            return { value: proxy, source: "Derived IVR duration proxy", short: "proxy" };
        }
    }
    return { value: null, source: "Needs HDS interval AHT fields", short: "missing" };
}

function businessAsa(rows) {
    const real = weightedAverage(rows, "avg_speed_answer", "avgSpeedAnswer", "calls_handled", "callsHandled");
    return real === null
        ? { value: null, source: "Needs HDS interval ASA/AnswerWaitTime", short: "missing" }
        : { value: real, source: "Cisco interval ASA", short: "Cisco" };
}

function businessIvrContainment() {
    const real = average(state.ivr, "ivr_containment_rate", "ivrContainmentRate");
    if (real !== null) return { value: real, source: "CVP Reporting containment query", short: "CVP" };
    const journeys = cvpJourneys();
    if (!journeys.length || businessSettings.fallbackMode !== "derived") return { value: null, source: "CVP containment unavailable", short: "missing" };
    const contained = journeys.filter(j => !/agent routing/i.test(j.nodes.join(" ")) && ![2, 18, 1001, 1044].includes(Number(j.dispositionId))).length;
    return { value: contained / journeys.length * 100, source: "Derived from CVP node journey", short: "derived" };
}

function businessFcr(rows) {
    const real = weightedAverage(rows, "first_call_resolution", "firstCallResolution", "calls_handled", "callsHandled");
    if (real !== null) return { value: real, source: "Configured FCR field", short: "Cisco" };
    if (businessSettings.fallbackMode !== "derived") return { value: null, source: "FCR source not configured", short: "missing" };
    const offered = sum(rows, "calls_offered", "callsOffered");
    const handled = sum(rows, "calls_handled", "callsHandled");
    const abandoned = sum(rows, "calls_abandoned", "callsAbandoned");
    const proxy = offered ? Math.max(0, (handled - abandoned * 0.15) / offered * 100) : null;
    return { value: proxy, source: "Derived proxy until CRM/repeat-call source is mapped", short: "proxy" };
}

function businessFcrForCounts(offered, handled, abandoned) {
    return offered ? Math.max(0, (handled - abandoned * 0.15) / offered * 100) : null;
}

function renderBusinessRules() {
    qs("#businessCalcSummary").textContent = `SL target ${businessSettings.slTarget}% | AHT target ${businessSettings.ahtTarget}s | ${businessSettings.fallbackMode}`;
    const sl = businessServiceLevel(state.calls);
    const aht = businessAht(state.calls);
    const asa = businessAsa(state.calls);
    const ivr = businessIvrContainment();
    const fcr = businessFcr(state.calls);
    qs("#businessCalcRules").innerHTML = [
        metricRow("Service Level", `${sl.source}. Formula: Cisco ServiceLevelCalls / ServiceLevelCallsOffered; fallback: handled / offered.`),
        metricRow("AHT", `${aht.source}. Formula: Cisco interval talk + wrap (+ hold when mapped) / handled.`),
        metricRow("ASA", `${asa.source}. Formula: AnswerWaitTime / handled.`),
        metricRow("IVR Containment", `${ivr.source}. Normal/Error/Hangup cause IDs configurable in CVP SQL.`),
        metricRow("First Call Resolution", `${fcr.source}. Recommended production source: CRM repeat-contact or survey resolution flag.`)
    ].join("");
}

function fcrBySkill() {
    const items = groupSkillMetrics().slice(0, 8);
    return { labels: items.map(item => item.skill), values: items.map(item => businessFcr(state.calls.filter(row => (pick(row, "skill_group", "skillGroup") || "UNKNOWN") === item.skill)).value || 0) };
}

function ivrByApp() {
    const map = new Map();
    cvpJourneys().forEach(journey => {
        const app = journey.app || "UNKNOWN";
        const item = map.get(app) || { total: 0, contained: 0 };
        item.total += 1;
        if (!/agent routing/i.test(journey.nodes.join(" ")) && ![2, 18, 1001, 1044].includes(Number(journey.dispositionId))) item.contained += 1;
        map.set(app, item);
    });
    const entries = [...map.entries()].slice(0, 8);
    return { labels: entries.map(([label]) => label), values: entries.map(([, item]) => derivedPct(item.contained, item.total) || 0) };
}

function derivedPct(numerator, denominator) {
    return denominator ? numerator / denominator * 100 : null;
}

function averageCvpDuration() {
    const values = cvpJourneys()
        .map(journey => Number(journey.duration))
        .filter(value => Number.isFinite(value) && value >= 0);
    return values.length ? values.reduce((a, b) => a + b, 0) / values.length : null;
}

function renderAgentFilters() {
    const teams = ["ALL", ...new Set(state.agents.map(agent => pick(agent, "team") || "UNKNOWN"))];
    const active = document.querySelector(".team-filter.active")?.dataset.team || "ALL";
    qs("#agentTeamFilters").innerHTML = teams.map(team => `<button class="team-filter ${team === active ? "active" : ""}" data-team="${escapeHtml(team)}">${escapeHtml(team === "ALL" ? "All" : team)}</button>`).join("");
    document.querySelectorAll(".team-filter").forEach(button => {
        button.addEventListener("click", () => {
            document.querySelectorAll(".team-filter").forEach(item => item.classList.remove("active"));
            button.classList.add("active");
            uiState.agentPage = 0;
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

function drawHorizontalBarChart(canvas, labels, values, palette) {
    if (!canvas) return;
    const ctx = setupCanvas(canvas);
    const { width, height } = canvas.getBoundingClientRect();
    const pad = { left: 130, right: 38, top: 28, bottom: 34 };
    ctx.clearRect(0, 0, width, height);
    drawGrid(ctx, width, height, pad);
    const rows = labels.length || 1;
    const slot = (height - pad.top - pad.bottom) / rows;
    const max = 100;
    ctx.font = "14px Segoe UI";
    labels.forEach((label, index) => {
        const y = pad.top + index * slot + slot * .28;
        const barWidth = Math.max(0, Math.min(100, num(values[index]))) / max * (width - pad.left - pad.right);
        ctx.fillStyle = "#87a1c2";
        ctx.textAlign = "right";
        ctx.fillText(String(label).slice(0, 18), pad.left - 14, y + 10);
        ctx.fillStyle = palette[index % palette.length];
        roundRect(ctx, pad.left, y, barWidth, Math.max(16, slot * .38), 7);
        ctx.fill();
        ctx.textAlign = "left";
        ctx.fillStyle = "#f6f8fb";
        ctx.fillText(`${Math.round(num(values[index]))}%`, pad.left + barWidth + 8, y + 12);
    });
    ctx.textAlign = "left";
    ctx.fillStyle = "#87a1c2";
    [0, 25, 50, 75, 100].forEach(mark => {
        const x = pad.left + mark / 100 * (width - pad.left - pad.right);
        ctx.fillText(String(mark), x - 5, height - 10);
    });
}

function drawStackedBarChart(canvas, labels, series) {
    if (!canvas) return;
    const ctx = setupCanvas(canvas);
    const { width, height } = canvas.getBoundingClientRect();
    const pad = { left: 58, right: 24, top: 28, bottom: 54 };
    ctx.clearRect(0, 0, width, height);
    drawGrid(ctx, width, height, pad);
    const totals = labels.map((_, index) => series.reduce((total, item) => total + num(item.values[index]), 0));
    const max = Math.max(10, ...totals);
    const slot = (width - pad.left - pad.right) / Math.max(1, labels.length);
    labels.forEach((label, index) => {
        let yBase = height - pad.bottom;
        series.forEach(item => {
            const value = num(item.values[index]);
            const barHeight = value / max * (height - pad.top - pad.bottom);
            ctx.fillStyle = item.color;
            ctx.fillRect(pad.left + index * slot + slot * .18, yBase - barHeight, slot * .64, barHeight);
            yBase -= barHeight;
        });
    });
    drawLabels(ctx, labels, width, height, pad);
    drawChartLegend(ctx, series.map(item => item.label), series.map(item => item.color), width, height);
}

function drawChartLegend(ctx, labels, palette, width, height) {
    if (!labels.length) return;
    ctx.font = "14px Segoe UI";
    let x = Math.max(16, width / 2 - labels.length * 56);
    const y = height - 18;
    labels.forEach((label, index) => {
        ctx.fillStyle = palette[index % palette.length];
        ctx.beginPath();
        ctx.arc(x, y - 4, 6, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = "#87a1c2";
        ctx.fillText(String(label).slice(0, 14), x + 12, y);
        x += 110;
    });
}

function roundRect(ctx, x, y, width, height, radius) {
    const r = Math.min(radius, width / 2, height / 2);
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.arcTo(x + width, y, x + width, y + height, r);
    ctx.arcTo(x + width, y + height, x, y + height, r);
    ctx.arcTo(x, y + height, x, y, r);
    ctx.arcTo(x, y, x + width, y, r);
    ctx.closePath();
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
    const series = Array.isArray(metrics) && metrics[0]?.metrics ? metrics : [{ label: "Metrics", color: "#2ed3c2", metrics }];
    const axes = series[0]?.metrics || [];
    const cx = width / 2;
    const cy = height / 2;
    const radius = Math.min(width, height) * .28;
    const count = axes.length || 1;
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
    axes.forEach((metric, i) => {
        const angle = -Math.PI / 2 + i * Math.PI * 2 / count;
        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.lineTo(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius);
        ctx.stroke();
        ctx.fillText(metric.label, cx + Math.cos(angle) * (radius + 18) - 28, cy + Math.sin(angle) * (radius + 18));
    });
    series.forEach((entry, seriesIndex) => {
        ctx.beginPath();
        entry.metrics.forEach((metric, i) => {
            const angle = -Math.PI / 2 + i * Math.PI * 2 / count;
            const value = Math.max(0, Math.min(100, num(metric.value))) / 100;
            const x = cx + Math.cos(angle) * radius * value;
            const y = cy + Math.sin(angle) * radius * value;
            i ? ctx.lineTo(x, y) : ctx.moveTo(x, y);
        });
        ctx.closePath();
        ctx.fillStyle = `${entry.color || colors[seriesIndex % colors.length]}33`;
        ctx.strokeStyle = entry.color || colors[seriesIndex % colors.length];
        ctx.lineWidth = 3;
        ctx.fill();
        ctx.stroke();
    });
    drawChartLegend(ctx, series.map(entry => entry.label), series.map((entry, index) => entry.color || colors[index % colors.length]), width, height);
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
