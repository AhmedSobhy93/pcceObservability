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
    agentSkillAssignments: [],
    provisioningCatalog: null,
    provisioningPlan: null,
    ivrFeatures: [],
    projectTasks: []
};

const pages = window.CxDashboardPages;

const colors = ["#2ed3c2", "#3d82f6", "#f4a51c", "#8d6cf7", "#24e0a4", "#ff626c"];
const uiState = {
    ivrNodePage: 0,
    ivrNodePageSize: 10,
    businessCardPage: 0,
    businessPageSize: 10,
    agentPage: 0,
    agentCardPage: 0,
    agentPageSize: 10,
    agentCardPageSize: 10,
    journeyPage: 0,
    journeyPageSize: 10,
    callTypePage: 0,
    callTypePageSize: 10
};
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
    qs("#fromDate").value = isoDaysAgo(6);
    qs("#toDate").value = today;
    qs("#datePreset").value = "last7";
    initBusinessSettings();
    initMultiSelects();
    activeView = requestedView();
    applyActiveView(false);
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
    qs("#agentCardPrevPage")?.addEventListener("click", () => {
        uiState.agentCardPage = Math.max(0, uiState.agentCardPage - 1);
        renderAgents();
    });
    qs("#agentCardNextPage")?.addEventListener("click", () => {
        uiState.agentCardPage += 1;
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
    qs("#businessCardsPrevPage")?.addEventListener("click", () => {
        uiState.businessCardPage = Math.max(0, uiState.businessCardPage - 1);
        renderBusinessCards();
    });
    qs("#businessCardsNextPage")?.addEventListener("click", () => {
        uiState.businessCardPage += 1;
        renderBusinessCards();
    });
    qs("#adminQuickBtn")?.addEventListener("click", () => switchView("admin"));
    qs("#adminSaveUserBtn")?.addEventListener("click", saveAdminUser);
    qs("#adminSaveRoleBtn")?.addEventListener("click", saveAdminRole);
    qs("#saveAlertConfigBtn")?.addEventListener("click", saveAlertConfig);
    qs("#loadInventoryBtn")?.addEventListener("click", loadMachineInventory);
    qs("#loadAgentSkillCatalogBtn")?.addEventListener("click", loadAgentSkillCatalog);
    qs("#asmBuildPlanBtn")?.addEventListener("click", () => submitAgentSkillPlan("plan"));
    qs("#asmDryRunBtn")?.addEventListener("click", () => submitAgentSkillPlan("dryRun"));
    qs("#asmSaveLocalBtn")?.addEventListener("click", saveAgentSkillDesiredState);
    qs("#asmExecuteBtn")?.addEventListener("click", () => submitAgentSkillPlan("execute"));
    qs("#saveIvrFeatureBtn")?.addEventListener("click", saveIvrFeature);
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
        uiState.businessCardPage = 0;
        refresh();
    }, 450);
    [
        "#datePreset", "#fromDate", "#toDate", "#overviewSkillFilter", "#businessSkillFilter", "#ivrAppFilter",
        "#agentPageFilter", "#agentTeamInput", "#callsSkillFilter", "#callsCallTypeFilter", "#callKeyFilter"
    ].forEach(selector => {
        const element = qs(selector);
        element?.addEventListener("change", event => {
            if (selector === "#datePreset") applyDatePreset(event.target.value);
            if (selector === "#fromDate" || selector === "#toDate") qs("#datePreset").value = "custom";
            debouncedRefresh();
        });
        element?.addEventListener("input", debouncedRefresh);
    });
    ["#businessSlTarget", "#businessAhtTarget", "#businessFallbackMode", "#businessAhtProxyMode", "#businessMinCalls", "#businessFcrWindowDays", "#businessViewMode", "#businessPageSize"].forEach(selector => {
        qs(selector)?.addEventListener("change", () => {
            saveBusinessSettings();
            uiState.businessPageSize = Number(qs("#businessPageSize")?.value || 10);
            uiState.ivrNodePageSize = uiState.businessPageSize;
            uiState.journeyPageSize = uiState.businessPageSize;
            uiState.callTypePageSize = uiState.businessPageSize;
            uiState.businessCardPage = 0;
            uiState.ivrNodePage = 0;
            uiState.journeyPage = 0;
            uiState.callTypePage = 0;
            renderBusiness();
            renderKpis();
        });
    });
    ["#agentPageFilter", "#agentTeamInput", "#agentStatusFilter", "#agentSearchFilter"].forEach(selector => {
        qs(selector)?.addEventListener("input", debounce(() => {
            uiState.agentPage = 0;
            uiState.agentCardPage = 0;
            renderAgents();
        }, 250));
        qs(selector)?.addEventListener("change", () => {
            uiState.agentPage = 0;
            uiState.agentCardPage = 0;
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
    document.addEventListener("click", event => {
        if (!event.target.closest(".multi-field")) {
            document.querySelectorAll(".multi-field.open").forEach(field => field.classList.remove("open"));
        }
    });
    refresh();
    setInterval(refreshLiveData, 10000);
});

function requestedView() {
    const view = normalizeView(new URLSearchParams(window.location.search).get("view") || "overview");
    return pages[view] ? view : "overview";
}

function normalizeView(view) {
    const value = String(view || "").trim();
    if (value === "agentSkills" || value.toLowerCase() === "agentskills") {
        return "agent-skills";
    }
    return value;
}

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

function isoDaysAgo(days) {
    const date = new Date();
    date.setDate(date.getDate() - days);
    return date.toISOString().slice(0, 10);
}

function applyDatePreset(preset) {
    const today = new Date();
    const setRange = (from, to) => {
        qs("#fromDate").value = from.toISOString().slice(0, 10);
        qs("#toDate").value = to.toISOString().slice(0, 10);
    };
    if (preset === "live") setRange(today, today);
    if (preset === "yesterday") {
        const yesterday = new Date(today);
        yesterday.setDate(today.getDate() - 1);
        setRange(yesterday, yesterday);
    }
    if (preset === "last7") {
        const from = new Date(today);
        from.setDate(today.getDate() - 6);
        setRange(from, today);
    }
    if (preset === "last30") {
        const from = new Date(today);
        from.setDate(today.getDate() - 29);
        setRange(from, today);
    }
    if (preset === "month") {
        const from = new Date(today.getFullYear(), today.getMonth(), 1);
        setRange(from, today);
    }
    if (preset === "lastMonth") {
        const from = new Date(today.getFullYear(), today.getMonth() - 1, 1);
        const to = new Date(today.getFullYear(), today.getMonth(), 0);
        setRange(from, to);
    }
}

function dateParams() {
    const dates = selectedDates();
    const params = new URLSearchParams({
        from: dates.from,
        to: dates.to
    });
    const skills = splitCsv(pageSkillFilter());
    if (skills.length === 1) params.set("skillGroup", skills[0]);
    return params.toString();
}

function callTypeParams() {
    const dates = selectedDates();
    const params = new URLSearchParams({
        from: dates.from,
        to: dates.to
    });
    const skill = firstFilterValue("#callsSkillFilter");
    const callType = firstFilterValue("#callsCallTypeFilter");
    const skills = splitCsv(skill);
    if (skills.length === 1) params.set("skillGroup", skills[0]);
    if (callType) params.set("callType", callType);
    return params.toString();
}

function agentParams() {
    const dates = selectedDates();
    const params = new URLSearchParams({
        from: dates.from,
        to: dates.to
    });
    const agents = splitCsv(firstFilterValue("#agentPageFilter"));
    const team = firstFilterValue("#agentTeamInput");
    if (agents.length === 1) params.set("agentId", agents[0]);
    if (team && !team.includes(",")) params.set("team", team);
    return params.toString();
}

function callFlowParams() {
    const dates = selectedDates();
    const params = new URLSearchParams({
        from: dates.from,
        to: dates.to
    });
    const callKey = firstFilterValue("#callKeyFilter");
    if (callKey) params.set("callKey", callKey);
    return params.toString();
}

function ivrNodeParams() {
    const dates = selectedDates();
    const params = new URLSearchParams({
        from: dates.from,
        to: dates.to
    });
    const apps = splitCsv(activeView === "business" || activeView === "cvp" ? firstFilterValue("#ivrAppFilter") : "");
    if (apps.length === 1) params.set("appName", apps[0]);
    return params.toString();
}

function selectedDates() {
    const today = new Date().toISOString().slice(0, 10);
    const from = validDate(qs("#fromDate")?.value) || today;
    const to = validDate(qs("#toDate")?.value) || from;
    return { from, to: to < from ? from : to };
}

function validDate(value) {
    return /^\d{4}-\d{2}-\d{2}$/.test(value || "") ? value : "";
}

function pageSkillFilter() {
    if (activeView === "business") return firstFilterValue("#businessSkillFilter");
    if (activeView === "calls") return firstFilterValue("#callsSkillFilter");
    if (activeView === "overview") return firstFilterValue("#overviewSkillFilter");
    return "";
}

function filteredCallRows(view = activeView) {
    const selector = view === "business" ? "#businessSkillFilter"
        : view === "calls" ? "#callsSkillFilter"
        : view === "overview" ? "#overviewSkillFilter"
        : "";
    const skills = splitCsv(selector ? firstFilterValue(selector) : "");
    if (!skills.length) return state.calls;
    return state.calls.filter(row => skills.includes(pick(row, "skill_group", "skillGroup") || "UNKNOWN"));
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
            fcrWindowDays: 7,
            viewMode: "daily",
            pageSize: 10,
            ...JSON.parse(localStorage.getItem("pcceBusinessSettings") || "{}")
        };
    } catch {
        return { slTarget: 80, ahtTarget: 300, fallbackMode: "derived", ahtProxyMode: "none", minCalls: 0, fcrWindowDays: 7, viewMode: "daily", pageSize: 10 };
    }
}

function initBusinessSettings() {
    if (qs("#businessSlTarget")) qs("#businessSlTarget").value = businessSettings.slTarget;
    if (qs("#businessAhtTarget")) qs("#businessAhtTarget").value = businessSettings.ahtTarget;
    if (qs("#businessFallbackMode")) qs("#businessFallbackMode").value = businessSettings.fallbackMode;
    if (qs("#businessAhtProxyMode")) qs("#businessAhtProxyMode").value = businessSettings.ahtProxyMode;
    if (qs("#businessMinCalls")) qs("#businessMinCalls").value = businessSettings.minCalls;
    if (qs("#businessFcrWindowDays")) qs("#businessFcrWindowDays").value = businessSettings.fcrWindowDays;
    if (qs("#businessViewMode")) qs("#businessViewMode").value = businessSettings.viewMode || "daily";
    if (qs("#businessPageSize")) qs("#businessPageSize").value = businessSettings.pageSize || 10;
    uiState.businessPageSize = Number(businessSettings.pageSize || 10);
    uiState.ivrNodePageSize = uiState.businessPageSize;
    uiState.journeyPageSize = uiState.businessPageSize;
    uiState.callTypePageSize = uiState.businessPageSize;
}

function saveBusinessSettings() {
    businessSettings.slTarget = Number(qs("#businessSlTarget")?.value || businessSettings.slTarget || 80);
    businessSettings.ahtTarget = Number(qs("#businessAhtTarget")?.value || businessSettings.ahtTarget || 300);
    businessSettings.fallbackMode = qs("#businessFallbackMode")?.value || businessSettings.fallbackMode || "derived";
    businessSettings.ahtProxyMode = qs("#businessAhtProxyMode")?.value || businessSettings.ahtProxyMode || "none";
    businessSettings.minCalls = Number(qs("#businessMinCalls")?.value || 0);
    businessSettings.fcrWindowDays = Number(qs("#businessFcrWindowDays")?.value || 7);
    businessSettings.viewMode = qs("#businessViewMode")?.value || "daily";
    businessSettings.pageSize = Number(qs("#businessPageSize")?.value || 10);
    localStorage.setItem("pcceBusinessSettings", JSON.stringify(businessSettings));
}

async function api(path, options = {}) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), options.timeoutMs || 12000);
    const method = options.method || "GET";
    const response = await fetch(path, {
        credentials: "same-origin",
        method,
        headers: {
            ...(options.body ? { "Content-Type": "application/json" } : {}),
            ...csrfHeaders(method)
        },
        body: options.body,
        signal: controller.signal
    }).finally(() => clearTimeout(timeout));
    if (!response.ok) {
        const text = await response.text();
        throw new Error(`${response.status} ${response.statusText}: ${text.slice(0, 220)}`);
    }
    return response.json();
}

function csrfHeaders(method = "GET") {
    if (String(method).toUpperCase() === "GET") {
        return {};
    }
    const token = document.cookie
            .split("; ")
            .find(row => row.startsWith("XSRF-TOKEN="));
    return token ? { "X-XSRF-TOKEN": decodeURIComponent(token.split("=")[1]) } : {};
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
        await fetch("/logout", { method: "POST", credentials: "same-origin", headers: csrfHeaders("POST") });
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
    const errors = [];
    try {
        setStatus([{ text: "Refreshing live data...", level: "neutral" }]);
        const params = dateParams();
        const wantsOverview = activeView === "overview";
        const wantsBusiness = activeView === "business";
        const wantsAgents = activeView === "agents";
        const wantsAgentSkills = activeView === "agent-skills";
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
    if (wantsOverview || wantsBusiness || wantsCalls) {
        coreLoads.push(
                safeLoad("calls", `/api/v1/metrics/calls?${params}`, [], { timeoutMs: 15000 }),
                safeLoad("drops", `/api/v1/calls/dropped?${params}`, []));
    }
    if (wantsBusiness) {
        coreLoads.push(
                safeLoad("ivr", `/api/v1/metrics/ivr-containment?${params}`, []),
                safeLoad("cvpIvrNodes", `/api/v1/metrics/cvp-ivr-nodes?${ivrNodeParams()}`, state.cvpIvrNodes, { timeoutMs: 12000 }));
    }
    if (wantsCalls) {
        coreLoads.push(safeLoad("callTypes", `/api/v1/metrics/call-types?${callTypeParams()}`, [], { timeoutMs: 15000 }));
    }
    if (wantsCalls) {
        coreLoads.push(
                safeLoad("callFlow", `/api/v1/calls/flow?${callFlowParams()}`, [], { timeoutMs: 15000 }),
                safeLoad("cvpIvrNodes", `/api/v1/metrics/cvp-ivr-nodes?${ivrNodeParams()}`, [], { timeoutMs: 12000 }));
    }
    if (wantsAgents) {
        coreLoads.push(
                safeLoad("agents", `/api/v1/agents/stats?${agentParams()}`, [], { timeoutMs: 15000 }),
                safeLoad("agentSkillAssignments", "/api/v1/workforce-management/agent-skill-assignments", state.agentSkillAssignments, { timeoutMs: 8000 }));
    }
    if (wantsAgentSkills) {
        coreLoads.push(
                safeLoad("provisioningCatalog", "/api/v1/agent-skill-management/catalog", state.provisioningCatalog, { timeoutMs: 15000 }),
                safeLoad("agentSkillAssignments", "/api/v1/workforce-management/agent-skill-assignments", state.agentSkillAssignments, { timeoutMs: 8000 }));
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
        safeRender("agents", renderAgents, errors);
        safeRender("finesse", renderFinesse, errors);
        const finesseDialogError = await safeLoad("finesseDialogs", "/api/v1/finesse/dialogs", state.finesseDialogs, { timeoutMs: 12000 });
        if (finesseDialogError) errors.push(finesseDialogError);
        safeRender("agents", renderAgents, errors);
        safeRender("finesse", renderFinesse, errors);
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
                safeLoad("cvpActions", "/api/v1/cvp-api/actions", state.cvpActions),
                safeLoad("ivrFeatures", "/api/v1/workforce-management/ivr-features", state.ivrFeatures));
    }
    if (wantsAgents || wantsAgentSkills || wantsIntegration || wantsSystem) {
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
    } catch (error) {
        console.error("dashboard_refresh_failed", error);
        errors.push(`refresh: ${error.message || error}`);
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
    try {
        const params = dateParams();
        const loads = [];
        if (activeView === "overview") {
            loads.push(
                    safeLoad("calls", `/api/v1/metrics/calls?${params}`, state.calls, { timeoutMs: 8000 }),
                    safeLoad("drops", `/api/v1/calls/dropped?${params}`, state.drops, { timeoutMs: 8000 }));
        }
        if (activeView === "business") {
            loads.push(
                    safeLoad("calls", `/api/v1/metrics/calls?${params}`, state.calls, { timeoutMs: 8000 }),
                    safeLoad("drops", `/api/v1/calls/dropped?${params}`, state.drops, { timeoutMs: 8000 }),
                    safeLoad("ivr", `/api/v1/metrics/ivr-containment?${params}`, state.ivr, { timeoutMs: 8000 }),
                    safeLoad("cvpIvrNodes", `/api/v1/metrics/cvp-ivr-nodes?${ivrNodeParams()}`, state.cvpIvrNodes, { timeoutMs: 8000 }));
        }
        if (activeView === "calls") {
            loads.push(
                    safeLoad("calls", `/api/v1/metrics/calls?${params}`, state.calls, { timeoutMs: 8000 }),
                    safeLoad("drops", `/api/v1/calls/dropped?${params}`, state.drops, { timeoutMs: 8000 }),
                    safeLoad("callTypes", `/api/v1/metrics/call-types?${callTypeParams()}`, state.callTypes, { timeoutMs: 8000 }));
        }
        if (activeView === "agents") {
            await safeLoad("agents", `/api/v1/agents/stats?${agentParams()}`, state.agents, { timeoutMs: 8000 });
            safeRender("agents", renderAgents);
            safeRender("finesse", renderFinesse);
            loads.push(
                    safeLoad("finesseAgents", "/api/v1/finesse/agents", state.finesseAgents, { timeoutMs: 8000 }),
                    safeLoad("finesseDialogs", "/api/v1/finesse/dialogs", state.finesseDialogs, { timeoutMs: 8000 }));
        }
        if (activeView === "advanced" || activeView === "overview") {
            loads.push(safeLoad("realtimeSnapshots", "/api/v1/live-data/realtime-snapshots", state.realtimeSnapshots, { timeoutMs: 8000 }));
        }
        await Promise.all(loads);
        safeRender("kpis", renderKpis);
        safeRender("charts", renderCharts);
        safeRender("business", renderBusiness);
        safeRender("agents", renderAgents);
        safeRender("finesse", renderFinesse);
        safeRender("callTypes", renderCallTypes);
        safeRender("advanced", renderAdvanced);
    } catch (error) {
        console.error("live_refresh_failed", error);
    } finally {
        liveRefreshInProgress = false;
    }
}

function setStatus(items) {
    qs("#statusStrip").innerHTML = items.map(item => `<div class="status-pill ${item.level}">${escapeHtml(item.text)}</div>`).join("");
}

function safeRender(name, renderer, errors = []) {
    try {
        renderer();
    } catch (error) {
        console.error(`render_${name}_failed`, error);
        errors.push(`${name}: ${error.message || error}`);
    }
}

function renderAll(errors) {
    safeRender("kpis", renderKpis, errors);
    safeRender("charts", renderCharts, errors);
    safeRender("business", renderBusiness, errors);
    safeRender("agents", renderAgents, errors);
    safeRender("agentSkills", renderAgentSkillManagement, errors);
    safeRender("management", renderManagementControls, errors);
    safeRender("finesse", renderFinesse, errors);
    safeRender("drops", renderDrops, errors);
    safeRender("callTypes", renderCallTypes, errors);
    safeRender("components", renderComponents, errors);
    safeRender("referenceOptions", renderReferenceOptions, errors);
    safeRender("integration", renderIntegration, errors);
    safeRender("cvpApi", renderCvpApi, errors);
    safeRender("smtp", renderSmtp, errors);
    safeRender("spog", renderSpog, errors);
    safeRender("eleveo", renderEleveo, errors);
    safeRender("advanced", renderAdvanced, errors);
    safeRender("operations", renderOperations, errors);
    safeRender("support", renderSupport, errors);
    safeRender("admin", renderAdmin, errors);
    planTasks = state.projectTasks || [];
    safeRender("plan", renderPlan, errors);

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
    const rows = filteredCallRows();
    const offered = sum(rows, "calls_offered", "callsOffered");
    const handled = sum(rows, "calls_handled", "callsHandled");
    const abandoned = sum(rows, "calls_abandoned", "callsAbandoned");
    const dropped = sum(state.drops, "dropped_calls", "droppedCalls");
    const unhandled = Math.max(0, offered - handled);
    const service = businessServiceLevel(rows).value;
    const aht = businessAht(rows).value;

    qs("#kpiOffered").textContent = fmt(offered);
    qs("#kpiHandled").textContent = fmt(handled);
    qs("#kpiAbandoned").textContent = fmt(abandoned);
    qs("#kpiDropped").textContent = fmt(dropped);
    qs("#trendDropped").textContent = dropped ? "Disposition based" : "Not configured";
    qs("#kpiService").textContent = pct(service);
    qs("#kpiAht").textContent = seconds(aht);
    qs("#trendService").textContent = businessServiceLevel(rows).source;
    qs("#kpiAht").nextElementSibling.textContent = businessAht(rows).source;
    qs("#chartRange").textContent = `${qs("#fromDate").value} to ${qs("#toDate").value}`;
    const skillLabel = pageSkillFilter();
    if (skillLabel) {
        qs("#trendOffered").textContent = `Skill: ${skillLabel}`;
        qs("#trendHandled").textContent = `Skill: ${skillLabel}`;
    } else {
        qs("#trendOffered").textContent = "Live HDS";
        qs("#trendHandled").textContent = "Live HDS";
    }
}

function renderCharts() {
    const rows = filteredCallRows(activeView);
    const hourly = groupByHour(rows);
    if (!hourly.hasData) {
        drawEmptyChart(qs("#volumeChart"), "No call volume rows for selected range");
    } else {
    drawLineChart(qs("#volumeChart"), hourly.labels, [
        { label: "Offered", color: "#2ed3c2", values: hourly.offered },
        { label: "Handled", color: "#3d82f6", values: hourly.handled }
    ]);
    }

    const skills = groupBySkill(rows, "calls_offered", "callsOffered");
    if (skills.labels.length) drawDoughnut(qs("#skillChart"), skills.labels, skills.values, colors);
    else drawEmptyChart(qs("#skillChart"), "No skill group volume for selected range");
    qs("#skillLegend").innerHTML = skills.labels.map((label, index) =>
        `<span><i class="dot" style="background:${colors[index % colors.length]}"></i>${escapeHtml(label)} <b>${fmt(skills.values[index])}</b></span>`
    ).join("");

    const ivr = (state.ivr || []).map(row => ({
        hour: pick(row, "hour"),
        value: pick(row, "ivr_containment_rate", "ivrContainmentRate")
    }));
    if (ivr.some(row => row.value !== null && row.value !== undefined)) {
        drawLineChart(qs("#ivrChart"), ivr.map(row => `${row.hour}:00`), [
            { label: "IVR", color: "#2ed3c2", values: ivr.map(row => num(row.value)) }
        ], 100);
    } else {
        const derived = businessIvrContainment();
        if (derived.value !== null) drawHorizontalBarChart(qs("#ivrChart"), ["IVR"], [derived.value], ["#2ed3c2"]);
        else drawEmptyChart(qs("#ivrChart"), "IVR containment unavailable for selected range");
    }
}

// renderBusiness lives in /dashboard/js/views/cx-dashboard-business-calls.js.

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
    const finesseSystemGrid = qs("#finesseSystemGrid");
    if (finesseSystemGrid) {
        finesseSystemGrid.innerHTML = state.finesseSystem.map(endpointCard).join("") ||
            `<article class="component-card"><h3>Finesse SystemInfo</h3><span class="badge warn">not loaded</span><p>Open System Health after configuring Finesse base URL and credentials.</p></article>`;
    }
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
    refreshMultiSelects();
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

    const capabilityList = qs("#cvpCapabilityList");
    if (capabilityList) {
        capabilityList.innerHTML = state.cvpCapabilities.map(item =>
            metricRow(pick(item, "category"), pick(item, "capability"))
        ).join("") || metricRow("CVP Developer APIs", "Capability catalog unavailable");
    }

    const functionTable = qs("#cvpFunctionTable");
    if (functionTable) functionTable.innerHTML = state.cvpFunctions.map(item => `<tr>
        <td>${escapeHtml(pick(item, "category") || "")}</td>
        <td>${escapeHtml(pick(item, "function") || "")}</td>
        <td><span class="badge up">${escapeHtml(pick(item, "method") || "GET")}</span></td>
        <td>${escapeHtml(pick(item, "path") || "")}</td>
        <td>${escapeHtml(pick(item, "description") || "")}</td>
    </tr>`).join("");

    const actionTable = qs("#cvpActionTable");
    if (actionTable) actionTable.innerHTML = state.cvpActions.map(item => {
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
    prepareApiActionForm("pcce", id);
    qs("#pcceActionResult").textContent = `Running ${id}...`;
    try {
        const bodyText = qs("#pcceActionBody").value.trim();
        const pathParams = parseJsonObject(qs("#pccePathParams").value.trim(), "Path params");
        const queryParams = parseJsonObject(qs("#pcceQueryParams").value.trim(), "Query params");
        const result = await api(`/api/v1/pcce-api/actions/${encodeURIComponent(id)}/execute`, {
            method: "POST",
            body: JSON.stringify({ body: bodyText || null, pathParams, queryParams })
        });
        qs("#pcceActionResult").textContent = formatApiActionResult(result);
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
    prepareApiActionForm("cvp", id);
    qs("#cvpActionResult").textContent = `Running ${id}...`;
    try {
        const bodyText = qs("#cvpActionBody").value.trim();
        const pathParams = parseJsonObject(qs("#cvpPathParams").value.trim(), "Path params");
        const queryParams = parseJsonObject(qs("#cvpQueryParams").value.trim(), "Query params");
        const result = await api(`/api/v1/cvp-api/actions/${encodeURIComponent(id)}/execute`, {
            method: "POST",
            body: JSON.stringify({ body: bodyText || null, pathParams, queryParams })
        });
        qs("#cvpActionResult").textContent = formatApiActionResult(result);
    } catch (error) {
        qs("#cvpActionResult").textContent = error.message;
    }
}

function prepareApiActionForm(scope, id) {
    const actions = scope === "pcce" ? state.pcceActions : state.cvpActions;
    const action = actions.find(item => String(pick(item, "id")).toLowerCase() === String(id).toLowerCase());
    if (!action) return;
    const path = pick(action, "path") || "";
    const pathTemplate = Object.fromEntries([...path.matchAll(/\{([^}]+)}/g)].map(match => [match[1], ""]));
    const queryTemplate = defaultQueryParamsForAction(scope, id);
    const pathBox = qs(`#${scope}PathParams`);
    const queryBox = qs(`#${scope}QueryParams`);
    const bodyBox = qs(`#${scope}ActionBody`);
    if (pathBox) {
        pathBox.placeholder = `Path params for ${path}: ${JSON.stringify(pathTemplate)}`;
        if (!pathBox.value.trim()) pathBox.value = JSON.stringify(pathTemplate, null, 2);
    }
    if (queryBox) {
        queryBox.placeholder = `Query params for ${id}: ${JSON.stringify(queryTemplate)}`;
        if (!queryBox.value.trim()) queryBox.value = JSON.stringify(queryTemplate, null, 2);
    }
    if (bodyBox) {
        const method = String(pick(action, "method") || "GET").toUpperCase();
        bodyBox.placeholder = ["POST", "PUT", "PATCH"].includes(method)
            ? `Payload for ${method} ${path}`
            : "No body needed for GET/DELETE actions";
    }
}

function defaultQueryParamsForAction(scope, id) {
    if (scope === "pcce") {
        if (id === "machineInventory.list") return { resultsPerPage: "100", time: String(Date.now()) };
        if (id.endsWith(".list") || id === "users.list") return { resultsPerPage: "100" };
    }
    return {};
}

function formatApiActionResult(result) {
    const status = pick(result, "status_code", "statusCode");
    const body = String(pick(result, "body") || "");
    const lines = [
        `Action: ${pick(result, "id")}`,
        `Method: ${pick(result, "method")}`,
        `Status: ${status || 0}`,
        `Latency: ${pick(result, "latency_ms", "latencyMs") || 0} ms`,
        `Target: ${pick(result, "target")}`,
        `Executed: ${pick(result, "executed_at", "executedAt")}`,
        "",
        "Body:",
        prettyBody(body)
    ];
    return lines.join("\n");
}

function prettyBody(body) {
    const text = String(body || "").trim();
    if (!text) return "(empty)";
    if (text.startsWith("{") || text.startsWith("[")) {
        try {
            return JSON.stringify(JSON.parse(text), null, 2);
        } catch {
            return text;
        }
    }
    if (text.startsWith("<")) {
        return text
            .replace(/></g, ">\n<")
            .replace(/\n\s*\n/g, "\n")
            .slice(0, 12000);
    }
    return text.slice(0, 12000);
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
    fillPlanConfigSelects();
}

function fillSelect(selector, values, forcedValue) {
    const select = qs(selector);
    if (!select) return;
    const current = forcedValue || select.value || "ALL";
    select.innerHTML = values.map(value => `<option value="${escapeHtml(value)}">${escapeHtml(value === "ALL" ? "All" : value.replace("_", " "))}</option>`).join("");
    select.value = values.includes(current) ? current : "ALL";
}

function fillPlanConfigSelects() {
    fillSelect("#planEditResource", planResources(), qs("#planEditResource")?.value || "Unassigned");
    fillSelect("#planEditOwner", planOwners(), qs("#planEditOwner")?.value || "Unassigned");
    fillSelect("#planEditTeam", planTeams(), qs("#planEditTeam")?.value || "General");
    fillSelect("#planEditMilestone", planMilestones(), qs("#planEditMilestone")?.value || "Delivery");
    fillSelect("#planTemplateTopic", planTopics(), qs("#planTemplateTopic")?.value || planTopics()[0]);
    fillSelect("#planTemplateResource", planResources(), qs("#planTemplateResource")?.value || "Unassigned");
    fillSelect("#planTemplateOwner", planOwners(), qs("#planTemplateOwner")?.value || "Unassigned");
    fillSelect("#planTemplateTeam", planTeams(), qs("#planTemplateTeam")?.value || "General");
}

function planSelect(field, index, values, selected) {
    const normalized = selected || "";
    const options = unique(["", ...values, normalized]).map(value =>
        `<option value="${escapeHtml(value)}" ${value === normalized ? "selected" : ""}>${escapeHtml(value || "--")}</option>`).join("");
    return `<select class="inline-input" data-plan-field="${field}" data-plan-index="${index}">${options}</select>`;
}

function planTopics() {
    return unique(["PCCE", "Eleveo", "Dtech", "Cisco Portal", "Survey", "Chat", "One Content", ...planTasks.map(task => task.topic)]);
}

function planResources() {
    return unique(["Unassigned", ...planTasks.flatMap(task => resourcesFor(task.resource)), ...planTasks.flatMap(task => resourcesFor(task.owner))]);
}

function planOwners() {
    return unique(["Unassigned", ...planTasks.flatMap(task => resourcesFor(task.owner)), ...planTasks.flatMap(task => resourcesFor(task.resource))]);
}

function planTeams() {
    return unique(["General", "Contact Center", "Reporting", "Network", "Security", "Business", ...planTasks.map(task => task.team), ...planTasks.map(task => task.topic)]);
}

function planMilestones() {
    return unique(["Assessment", "Design", "Build", "UAT", "Migration", "Go Live", "Hypercare", "Delivery", ...planTasks.map(task => task.milestone)]);
}

function planDates(field) {
    return unique(planTasks.map(task => task[field]).filter(Boolean));
}

function planBlockedByOptions() {
    return unique(["", "Network team", "Security approval", "Vendor dependency", "Business sign-off", "Change window", ...planTasks.map(task => task.blockedBy)]);
}

function planDeliverables() {
    return unique(["", "Design document", "Configured service", "Test evidence", "Migration package", "Runbook", "Dashboard", ...planTasks.map(task => task.deliverable)]);
}

function planShareGroups() {
    return unique(["", "Managers", "Stakeholders", "Business owners", "Support team", "Project team", ...planTasks.map(task => task.shareWith)]);
}

function planDependencyOptions(task) {
    return unique(["", ...planTasks.filter(candidate => candidate !== task).map(candidate => candidate.task)]);
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
    qs("#planTaskView").innerHTML = `<article class="panel full plan-table-panel">
        <div class="panel-head"><h2>Tasks</h2><span>One row per task, dropdown-managed fields</span></div>
        <div class="table-wrap plan-flat-table"><table>
            <thead><tr><th>Topic</th><th>Task</th><th>Priority</th><th>Status</th><th>Resource</th><th>Owner</th><th>Team</th><th>Milestone</th><th>Timeline</th><th>Progress</th><th>Notes</th><th></th></tr></thead>
            <tbody>${tasks.map(planTaskRow).join("") || `<tr><td colspan="12">No tasks match filters</td></tr>`}</tbody>
        </table></div>
    </article>`;
    bindPlanEditors();
}

function planTaskRow(task) {
    const criticalOpen = task.priority === "CRITICAL" && task.status !== "COMPLETED";
    const index = planTasks.indexOf(task);
    const id = task.id || index;
    return `<tr class="${criticalOpen ? "critical-row" : ""}">
        <td>${planSelect("topic", index, planTopics(), task.topic)}</td>
        <td><input class="inline-input plan-task-name" data-plan-field="task" data-plan-index="${index}" value="${escapeHtml(task.task || "")}"><span class="subline">ID ${escapeHtml(id)}${task.priorityNum ? ` | #${escapeHtml(task.priorityNum)}` : ""}</span></td>
        <td>${planSelect("priority", index, ["CRITICAL", "HIGH", "MEDIUM", "LOW"], task.priority)}</td>
        <td>${planSelect("status", index, ["COMPLETED", "IN_PROGRESS", "ON_HOLD", "PLANNED"], task.status)}</td>
        <td>${planSelect("resource", index, planResources(), task.resource || "Unassigned")}</td>
        <td>${planSelect("owner", index, planOwners(), task.owner || task.resource || "Unassigned")}</td>
        <td>${planSelect("team", index, planTeams(), task.team || task.topic || "General")}</td>
        <td>${planSelect("milestone", index, planMilestones(), task.milestone || "Delivery")}</td>
        <td class="timeline-cell">
            ${planSelect("start", index, planDates("start"), task.start || "")}
            ${planSelect("finish", index, planDates("finish"), task.finish || "")}
            <input class="inline-input tiny-input" data-plan-field="duration" data-plan-index="${index}" type="number" min="0" value="${escapeHtml(task.duration ?? "")}" placeholder="Days">
        </td>
        <td class="progress-cell">
            <input class="inline-input plan-pct-input" type="number" min="0" max="100" data-plan-field="pct" data-plan-index="${index}" value="${escapeHtml(task.pct)}">
            ${inlineProgress(task.pct)}
            ${planSelect("risk", index, ["LOW", "MEDIUM", "HIGH", "CRITICAL"], task.risk || "MEDIUM")}
        </td>
        <td class="notes-cell">
            ${planSelect("dependsOn", index, planDependencyOptions(task), task.dependsOn || "")}
            ${planSelect("blockedBy", index, planBlockedByOptions(), task.blockedBy || "")}
            ${planSelect("deliverable", index, planDeliverables(), task.deliverable || "")}
            ${planSelect("shareWith", index, planShareGroups(), task.shareWith || "")}
            <input class="inline-input" data-plan-field="comments" data-plan-index="${index}" value="${escapeHtml(task.comments || "")}" placeholder="Comments">
        </td>
        <td class="row-actions">
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
        resource: qs("#planEditResource").value || "Unassigned",
        owner: qs("#planEditOwner").value,
        team: qs("#planEditTeam").value,
        milestone: qs("#planEditMilestone").value,
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
    ["#planEditTask", "#planEditStart", "#planEditFinish",
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
        topic: qs("#planTemplateTopic").value || "Team Delivery",
        team: qs("#planTemplateTeam").value,
        owner: qs("#planTemplateOwner").value,
        resource: qs("#planTemplateResource").value,
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
        ["#planTemplateStart", "#planTemplateFinish", "#planTemplateShareWith"].forEach(selector => qs(selector).value = "");
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

function renderAgentSkillManagement() {
    const catalog = state.provisioningCatalog || {};
    const agents = pick(catalog, "agents") || [];
    const teams = pick(catalog, "teams") || [];
    const skills = pick(catalog, "skill_groups", "skillGroups") || [];
    const deskSettings = pick(catalog, "desk_settings", "deskSettings") || [];
    const executionEnabled = Boolean(pick(catalog, "execution_enabled", "executionEnabled"));
    const warnings = pick(catalog, "warnings") || [];
    const kpis = [
        agentKpi("PCCE Agents", agents.length, "catalog"),
        agentKpi("Teams", teams.length, "agent teams"),
        agentKpi("Skill Groups", skills.length, "routing skills"),
        agentKpi("Desk Settings", deskSettings.length, "agent desktop"),
        agentKpi("CUCM AXL", pick(catalog, "cucm_axl_enabled", "cucmAxlEnabled") ? "Enabled" : "Disabled", "user, DN, CSF phone"),
        agentKpi("Execution", executionEnabled ? "Live" : "Dry Run", executionEnabled ? "write actions enabled" : "protected")
    ];
    qs("#agentSkillCatalogKpis").innerHTML = kpis.join("");
    qs("#agentSkillExecutionMode").textContent = executionEnabled
            ? "Live execution enabled - use approved change window"
            : "Dry-run protected - set AGENT_PROVISIONING_EXECUTION_ENABLED=true for writes";
    qs("#asmExecuteBtn").disabled = !executionEnabled;
    fillSelect("#asmTeam", teams, "Select team");
    fillSelect("#asmDeskSettings", deskSettings, "Select desk settings");
    renderAgentSkillPlan(state.provisioningPlan, warnings);
    renderAsmAssignments();
    refreshMultiSelects();
}

function fillSelect(selector, rows, emptyLabel) {
    const select = qs(selector);
    if (!select) return;
    const current = select.value;
    const options = [`<option value="">${escapeHtml(emptyLabel)}</option>`].concat((rows || []).map(row => {
        const value = pick(row, "name", "id") || "";
        const label = [pick(row, "name"), pick(row, "id")].filter(Boolean).join(" - ");
        return `<option value="${escapeHtml(value)}">${escapeHtml(label || value)}</option>`;
    }));
    select.innerHTML = options.join("");
    if (current && Array.from(select.options).some(option => option.value === current)) {
        select.value = current;
    }
}

function renderAgentSkillPlan(plan, catalogWarnings = []) {
    const steps = pick(plan, "steps") || [];
    const warnings = [...catalogWarnings, ...(pick(plan, "warnings") || [])].filter(Boolean);
    qs("#asmPlanSummary").textContent = plan
            ? `${escapeHtml(pick(plan, "pcce_user_name", "pcceUserName") || "")} | ${steps.length} steps | ${pick(plan, "dry_run", "dryRun") ? "dry run" : "live"}`
            : warnings.length ? warnings.join(" | ") : "No plan built";
    qs("#asmPlanTable").innerHTML = steps.map(step => {
        const status = String(pick(step, "status") || "SKIPPED").toLowerCase();
        return `<tr>
            <td>${fmt(pick(step, "order"))}</td>
            <td>${escapeHtml(pick(step, "system") || "")}</td>
            <td><strong>${escapeHtml(pick(step, "action") || "")}</strong><span class="subline">${escapeHtml(pick(step, "target") || "")}</span></td>
            <td>${escapeHtml(pick(step, "method") || "")}</td>
            <td><span class="badge ${status === "ok" ? "up" : status === "failed" ? "down" : "warn"}">${escapeHtml(pick(step, "status") || "")}</span></td>
            <td>${escapeHtml(pick(step, "detail") || "")}</td>
        </tr>`;
    }).join("") || `<tr><td colspan="6">No provisioning plan yet.</td></tr>`;
    const preview = steps
            .filter(step => pick(step, "payload_preview", "payloadPreview"))
            .map(step => `# ${pick(step, "system")} - ${pick(step, "action")}\n${pick(step, "payload_preview", "payloadPreview")}`)
            .join("\n\n");
    qs("#asmPayloadPreview").textContent = preview || (warnings.join("\n") || "Load catalog and build a plan.");
}

function renderAsmAssignments() {
    const table = qs("#asmAssignmentTable");
    if (!table) return;
    table.innerHTML = (state.agentSkillAssignments || []).map(item => `
        <tr>
            <td><strong>${escapeHtml(pick(item, "agent_id", "agentId") || "")}</strong><span class="subline">${escapeHtml(pick(item, "agent_name", "agentName") || "")}</span></td>
            <td>${escapeHtml(pick(item, "team_name", "teamName") || "")}</td>
            <td>${escapeHtml(pick(item, "skill_group", "skillGroup") || "")}</td>
            <td>${escapeHtml(String(pick(item, "proficiency") ?? ""))}</td>
            <td><span class="badge ${pick(item, "enabled") ? "up" : "down"}">${pick(item, "enabled") ? "enabled" : "disabled"}</span></td>
            <td>${escapeHtml(pick(item, "source") || "APP")}</td>
            <td>${escapeHtml(formatDateTime(pick(item, "updated_at", "updatedAt")))}</td>
            <td><button class="tiny-button danger" type="button" onclick="deleteAgentSkillAssignment(${Number(pick(item, "id"))})">Remove</button></td>
        </tr>
    `).join("") || `<tr><td colspan="8">No local desired-state assignments saved yet.</td></tr>`;
}

async function loadAgentSkillCatalog() {
    const error = await safeLoad("provisioningCatalog", "/api/v1/agent-skill-management/catalog", state.provisioningCatalog, { timeoutMs: 15000 });
    if (error) {
        setStatus([{ text: error, level: "warn" }]);
    }
    renderAgentSkillManagement();
}

async function submitAgentSkillPlan(mode) {
    const body = agentSkillManagementRequest();
    const endpoint = mode === "plan"
            ? "/api/v1/agent-skill-management/plan"
            : `/api/v1/agent-skill-management/execute?dryRun=${mode !== "execute"}`;
    state.provisioningPlan = await api(endpoint, { method: "POST", body: JSON.stringify(body), timeoutMs: mode === "execute" ? 30000 : 15000 });
    renderAgentSkillManagement();
}

async function saveAgentSkillDesiredState() {
    const request = agentSkillManagementRequest();
    const skills = request.skillGroupNames.length ? request.skillGroupNames : [""];
    for (const skill of skills) {
        await api("/api/v1/workforce-management/agent-skill-assignments", {
            method: "POST",
            body: JSON.stringify({
                agentId: request.agentId || request.baseUsername,
                agentName: request.displayName || [request.firstName, request.lastName].filter(Boolean).join(" "),
                teamName: request.teamName,
                skillGroup: skill,
                proficiency: request.proficiency,
                enabled: true,
                source: request.localUser ? "LOCAL_CUCM" : "LDAP_SSO",
                notes: request.notes
            }),
            timeoutMs: 8000
        });
    }
    await safeLoad("agentSkillAssignments", "/api/v1/workforce-management/agent-skill-assignments", [], { timeoutMs: 8000 });
    renderAgentSkillManagement();
}

function agentSkillManagementRequest() {
    return {
        baseUsername: qs("#asmBaseUsername")?.value,
        firstName: qs("#asmFirstName")?.value,
        lastName: qs("#asmLastName")?.value,
        displayName: qs("#asmDisplayName")?.value,
        mail: qs("#asmMail")?.value,
        agentId: qs("#asmAgentId")?.value,
        dn: qs("#asmDn")?.value,
        teamName: qs("#asmTeam")?.value,
        skillGroupNames: splitCsv(qs("#asmSkillGroups")?.value),
        supervisedTeamNames: splitCsv(qs("#asmSupervisedTeams")?.value),
        deskSettingsName: qs("#asmDeskSettings")?.value,
        proficiency: qs("#asmProficiency")?.value ? Number(qs("#asmProficiency").value) : null,
        userMode: qs("#asmUserMode")?.value,
        agentType: qs("#asmAgentType")?.value,
        autoRollbackOnError: qs("#asmRollback")?.value === "true",
        localUser: qs("#asmUserMode")?.value === "new",
        notes: qs("#asmNotes")?.value
    };
}

function renderManagementControls() {
    renderIvrFeatures();
}

function renderIvrFeatures() {
    const table = qs("#ivrFeatureTable");
    if (!table) return;
    table.innerHTML = (state.ivrFeatures || []).map(item => `
        <tr>
            <td>${escapeHtml(pick(item, "app_name", "appName") || "")}</td>
            <td><strong>${escapeHtml(pick(item, "feature_key", "featureKey") || "")}</strong></td>
            <td><span class="badge ${pick(item, "enabled") ? "up" : "down"}">${pick(item, "enabled") ? "enabled" : "disabled"}</span></td>
            <td>${escapeHtml(pick(item, "min_severity", "minSeverity") || "")}</td>
            <td>${escapeHtml(pick(item, "config_value", "configValue") || "")}</td>
            <td>${escapeHtml(pick(item, "notes") || "")}</td>
            <td>${escapeHtml(formatDateTime(pick(item, "updated_at", "updatedAt")))}</td>
            <td><button class="tiny-button danger" type="button" onclick="deleteIvrFeature(${Number(pick(item, "id"))})">Remove</button></td>
        </tr>
    `).join("") || `<tr><td colspan="8">No IVR feature controls saved yet.</td></tr>`;
}

function formatDateTime(value) {
    if (!value) return "";
    return String(value).replace("T", " ").replace(/\.\d+$/, "");
}

async function saveIvrFeature() {
    const body = {
        appName: qs("#ivrFeatureApp")?.value,
        featureKey: qs("#ivrFeatureKey")?.value,
        enabled: qs("#ivrFeatureEnabled")?.value === "true",
        minSeverity: qs("#ivrFeatureSeverity")?.value,
        configValue: qs("#ivrFeatureValue")?.value,
        notes: qs("#ivrFeatureNotes")?.value
    };
    await api("/api/v1/workforce-management/ivr-features", { method: "POST", body: JSON.stringify(body), timeoutMs: 8000 });
    await safeLoad("ivrFeatures", "/api/v1/workforce-management/ivr-features", [], { timeoutMs: 8000 });
    renderIvrFeatures();
}

async function deleteAgentSkillAssignment(id) {
    if (!id) return;
    await api(`/api/v1/workforce-management/agent-skill-assignments/${id}`, { method: "DELETE", timeoutMs: 8000 });
    await safeLoad("agentSkillAssignments", "/api/v1/workforce-management/agent-skill-assignments", [], { timeoutMs: 8000 });
    renderAsmAssignments();
}

async function deleteIvrFeature(id) {
    if (!id) return;
    await api(`/api/v1/workforce-management/ivr-features/${id}`, { method: "DELETE", timeoutMs: 8000 });
    await safeLoad("ivrFeatures", "/api/v1/workforce-management/ivr-features", [], { timeoutMs: 8000 });
    renderIvrFeatures();
}

function switchView(view) {
    view = normalizeView(view);
    if (!pages[view]) {
        view = "overview";
    }
    activeView = view;
    applyActiveView(true);
    refresh();
}

function applyActiveView(updateUrl) {
    document.querySelectorAll(".nav-item[data-view]").forEach(button => button.classList.toggle("active", button.dataset.view === activeView));
    document.querySelectorAll(".view").forEach(section => section.classList.toggle("active", section.id === `view-${activeView}`));
    qs("#pageTitle").textContent = pages[activeView][0];
    qs("#pageSubtitle").textContent = pages[activeView][1];
    if (updateUrl) {
        const url = new URL(window.location.href);
        url.searchParams.set("view", activeView);
        history.replaceState(null, "", url);
    }
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

function initMultiSelects() {
    document.querySelectorAll(".multi-field").forEach(field => {
        if (field.dataset.ready === "true") return;
        field.dataset.ready = "true";
        field.innerHTML = `
            <button type="button" class="multi-trigger" aria-haspopup="listbox" aria-expanded="false">
                <span class="multi-title">${escapeHtml(field.dataset.label || "Filter")}</span>
                <span class="multi-summary">All</span>
                <span class="multi-chips" aria-live="polite"></span>
            </button>
            <div class="multi-menu" role="listbox" aria-multiselectable="true">
                <label class="multi-search"><span>Search</span><input type="search" placeholder="Type to filter"></label>
                <div class="multi-actions">
                    <button type="button" data-action="all">All</button>
                    <button type="button" data-action="clear">Clear</button>
                </div>
                <div class="multi-options"></div>
            </div>`;
        field.querySelector(".multi-trigger").addEventListener("click", event => {
            event.stopPropagation();
            const opened = field.classList.toggle("open");
            field.querySelector(".multi-trigger").setAttribute("aria-expanded", String(opened));
            document.querySelectorAll(".multi-field.open").forEach(other => {
                if (other !== field) other.classList.remove("open");
            });
        });
        field.querySelector(".multi-menu").addEventListener("click", event => event.stopPropagation());
        field.querySelector(".multi-search input").addEventListener("input", event => {
            event.stopPropagation();
            filterMultiOptions(field);
        });
        field.querySelector("[data-action='all']").addEventListener("click", () => setMultiValue(field, ""));
        field.querySelector("[data-action='clear']").addEventListener("click", () => setMultiValue(field, ""));
    });
}

function refreshMultiSelects() {
    initMultiSelects();
    document.querySelectorAll(".multi-field").forEach(field => {
        const selected = selectedMultiValues(field);
        const options = field.querySelector(".multi-options");
        options.innerHTML = multiSourceOptions(field.dataset.source).map(option => `
            <label class="multi-option" data-text="${escapeHtml(option.search)}">
                <input type="checkbox" value="${escapeHtml(option.value)}" ${selected.includes(option.value) ? "checked" : ""}>
                <span>${escapeHtml(option.label)}</span>
                ${option.detail ? `<small>${escapeHtml(option.detail)}</small>` : ""}
            </label>`).join("") || `<div class="multi-empty">No values loaded</div>`;
        options.querySelectorAll("input[type='checkbox']").forEach(input => {
            input.addEventListener("change", () => {
                const next = Array.from(options.querySelectorAll("input[type='checkbox']:checked")).map(item => item.value);
                setMultiValue(field, next.join(","));
                filterMultiOptions(field);
            });
        });
        updateMultiSummary(field);
        filterMultiOptions(field);
    });
}

function multiSourceValues(source) {
    return multiSourceOptions(source).map(option => option.value);
}

function multiSourceOptions(source) {
    const catalog = state.provisioningCatalog || {};
    const rows = source === "callTypes" ? state.callTypeOptions
        : source === "agents" ? state.agentOptions
        : source === "teams" ? state.agents.map(agent => pick(agent, "team") || "UNKNOWN")
        : source === "provisioningSkills" ? (pick(catalog, "skill_groups", "skillGroups") || [])
        : source === "provisioningTeams" ? (pick(catalog, "teams") || [])
        : source === "cvpApps" ? state.cvpIvrNodes.map(row => pick(row, "app_name", "appName") || "UNKNOWN")
        : state.skillGroups;
    const seen = new Map();
    (rows || []).forEach(row => {
        const value = String(pick(row, "value", "id", "agent_id", "agentId", "name", "enterpriseName", "callType", "label") || row || "").trim();
        if (!value) return;
        const label = String(pick(row, "label", "name", "enterpriseName", "agent_name", "agentName", "callType", "value") || value).trim();
        const detail = String(pick(row, "detail", "team", "skill_group", "skillGroup", "id") || "").trim();
        const key = value.toLowerCase();
        if (!seen.has(key)) {
            seen.set(key, {
                value,
                label: label === value && detail ? `${label}` : label,
                detail,
                search: `${value} ${label} ${detail}`.toLowerCase()
            });
        }
    });
    return [...seen.values()].sort((a, b) => a.label.localeCompare(b.label));
}

function selectedMultiValues(field) {
    return splitCsv(qs(`#${field.dataset.target}`)?.value || "");
}

function setMultiValue(field, value) {
    const target = qs(`#${field.dataset.target}`);
    if (!target) return;
    target.value = value;
    updateMultiSummary(field);
    target.dispatchEvent(new Event("input", { bubbles: true }));
    target.dispatchEvent(new Event("change", { bubbles: true }));
}

function updateMultiSummary(field) {
    const values = selectedMultiValues(field);
    const summary = field.querySelector(".multi-summary");
    if (summary) {
        summary.textContent = values.length === 0 ? "All" : values.length <= 2 ? values.join(", ") : `${values.length} selected`;
    }
    const chips = field.querySelector(".multi-chips");
    if (chips) {
        chips.innerHTML = values.slice(0, 4).map(value => `
            <span class="multi-chip" data-remove-value="${escapeHtml(value)}" role="button" tabindex="0" aria-label="Remove ${escapeHtml(value)}">
                <span>${escapeHtml(value)}</span>
            </span>`).join("") + (values.length > 4 ? `<span class="multi-chip more">+${values.length - 4}</span>` : "");
        chips.querySelectorAll("[data-remove-value]").forEach(button => {
            const removeValue = event => {
                event.stopPropagation();
                const remove = button.dataset.removeValue;
                setMultiValue(field, values.filter(value => value !== remove).join(","));
                filterMultiOptions(field);
            };
            button.addEventListener("click", removeValue);
            button.addEventListener("keydown", event => {
                if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    removeValue(event);
                }
            });
        });
    }
    field.querySelectorAll(".multi-options input[type='checkbox']").forEach(input => {
        input.checked = values.includes(input.value);
    });
}

function filterMultiOptions(field) {
    const query = field.querySelector(".multi-search input")?.value?.trim().toLowerCase() || "";
    field.querySelectorAll(".multi-option").forEach(option => {
        const text = String(option.dataset.text || option.textContent || "").toLowerCase();
        option.hidden = Boolean(query) && !text.includes(query);
    });
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

// renderBusinessCards lives in /dashboard/js/views/cx-dashboard-business-calls.js.

function renderAgentFilters() {
    refreshMultiSelects();
    qs("#agentTeamFilters").innerHTML = "";
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

// renderCallFunnel lives in /dashboard/js/views/cx-dashboard-business-calls.js.

function average(rows, ...names) {
    const values = rows.map(row => pick(row, ...names)).filter(value => value !== null && value !== undefined).map(Number);
    return values.length ? values.reduce((a, b) => a + b, 0) / values.length : null;
}

function groupByHour(rows) {
    const map = new Map();
    for (let hour = 0; hour < 24; hour += 1) {
        map.set(hour, { offered: 0, handled: 0 });
    }
    rows.forEach(row => {
        const hour = num(pick(row, "hour"));
        const existing = map.get(hour) || { offered: 0, handled: 0 };
        existing.offered += num(pick(row, "calls_offered", "callsOffered"));
        existing.handled += num(pick(row, "calls_handled", "callsHandled"));
        map.set(hour, existing);
    });
    const labels = [...map.keys()].sort((a, b) => a - b).map(hour => `${hour}:00`);
    const values = [...map.entries()].sort((a, b) => a[0] - b[0]).map(([, value]) => value);
    return {
        labels,
        offered: values.map(v => v.offered),
        handled: values.map(v => v.handled),
        hasData: values.some(v => v.offered > 0 || v.handled > 0)
    };
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
    window.CxDashboardCharts.drawLineChart(canvas, labels, series, fixedMax);
}

function drawBarChart(canvas, labels, values, color) {
    window.CxDashboardCharts.drawBarChart(canvas, labels, values, color);
}

function drawHorizontalBarChart(canvas, labels, values, palette) {
    window.CxDashboardCharts.drawHorizontalBarChart(canvas, labels, values, palette);
}

function drawEmptyChart(canvas, message) {
    window.CxDashboardCharts.drawEmptyChart(canvas, message);
}

function drawStackedBarChart(canvas, labels, series) {
    window.CxDashboardCharts.drawStackedBarChart(canvas, labels, series);
}

function drawDoughnut(canvas, labels, values, palette) {
    window.CxDashboardCharts.drawDoughnut(canvas, labels, values, palette);
}

function drawRadar(canvas, metrics) {
    window.CxDashboardCharts.drawRadar(canvas, metrics, colors);
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


