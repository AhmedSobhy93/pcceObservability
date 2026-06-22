const state = {
    summary: null,
    calls: [],
    callTypes: [],
    agents: [],
    drops: [],
    ivr: [],
    components: [],
    assessment: null,
    user: null,
    queryPerformance: [],
    logs: [],
    pcceApiStatus: [],
    pcceCapabilities: [],
    pcceFunctions: [],
    pcceActions: [],
    rtmtCapabilities: [],
    spogCapabilities: []
};

const pages = {
    overview: ["Dashboard Overview", "Cisco PCCE 12.6.2 - Real-time Contact Center Analytics"],
    business: ["Business Metrics", "Service level, handle time, IVR containment, and contact center KPIs"],
    agents: ["Agent Performance", "Workforce team and agent productivity view"],
    calls: ["Call Analytics", "Dropped calls, queue behavior, and skill group distribution"],
    system: ["System Health", "PCCE, CVP, CUIC, Finesse, PG, CTI, and gateway status"],
    integration: ["PCCE Integration", "AW/HDS SQL Server and CVP Informix connectivity"],
    app: ["Spring Boot App", "Operational alerts, readiness checks, and support status"],
    plan: ["Project Plan", "Production hardening and banking support checklist"]
};

const colors = ["#2ed3c2", "#3d82f6", "#f4a51c", "#8d6cf7", "#24e0a4", "#ff626c"];

document.addEventListener("DOMContentLoaded", () => {
    const today = new Date().toISOString().slice(0, 10);
    qs("#fromDate").value = today;
    qs("#toDate").value = today;
    qs("#refreshBtn").addEventListener("click", refresh);
    qs("#collapseBtn").addEventListener("click", () => document.body.classList.toggle("collapsed"));
    qs("#logoutBtn").addEventListener("click", () => location.href = "/swagger-ui/index.html");
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

function dateParams() {
    return `from=${qs("#fromDate").value}&to=${qs("#toDate").value}`;
}

async function api(path, options = {}) {
    const response = await fetch(path, {
        credentials: "same-origin",
        method: options.method || "GET",
        headers: options.body ? { "Content-Type": "application/json" } : undefined,
        body: options.body
    });
    if (!response.ok) {
        const text = await response.text();
        throw new Error(`${response.status} ${response.statusText}: ${text.slice(0, 220)}`);
    }
    return response.json();
}

async function safeLoad(key, path, fallback) {
    try {
        state[key] = await api(path);
        return null;
    } catch (error) {
        state[key] = fallback;
        return `${key}: ${error.message}`;
    }
}

async function refresh() {
    setStatus([{ text: "Refreshing live data...", level: "neutral" }]);
    const params = dateParams();
    const errors = [];
    const loads = [
        safeLoad("user", "/api/v1/auth/me", null),
        safeLoad("calls", `/api/v1/metrics/calls?${params}`, []),
        safeLoad("callTypes", `/api/v1/metrics/call-types?${params}`, []),
        safeLoad("agents", `/api/v1/agents/stats?${params}`, []),
        safeLoad("drops", `/api/v1/calls/dropped?${params}`, []),
        safeLoad("ivr", `/api/v1/metrics/ivr-containment?${params}`, []),
        safeLoad("components", "/api/v1/components/status", []),
        safeLoad("assessment", `/api/v1/operations/assessment?${params}`, null),
        safeLoad("queryPerformance", "/api/v1/monitoring/query-performance?limit=50", []),
        safeLoad("pcceApiStatus", "/api/v1/pcce-api/status", []),
        safeLoad("pcceCapabilities", "/api/v1/pcce-api/capabilities", []),
        safeLoad("pcceFunctions", "/api/v1/pcce-api/functions", []),
        safeLoad("pcceActions", "/api/v1/pcce-api/actions", []),
        safeLoad("rtmtCapabilities", "/api/v1/pcce-api/rtmt-capabilities", []),
        safeLoad("spogCapabilities", "/api/v1/pcce-api/spog-capabilities", [])
    ];
    const results = await Promise.all(loads);
    errors.push(...results.filter(Boolean));
    if (hasPermission("SOLUTION_ADMIN")) {
        const logError = await safeLoad("logs", "/api/v1/monitoring/logs?limit=80", []);
        if (logError) errors.push(logError);
    } else {
        state.logs = ["Log tail is available for administrators."];
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
    renderDrops();
    renderCallTypes();
    renderComponents();
    renderIntegration();
    renderOperations();
    renderSupport();
    renderPlan();

    const componentDown = state.components.filter(c => String(pick(c, "state")).toUpperCase() === "DOWN").length;
    const alertCount = pick(state.assessment, "alerts")?.length || 0;
    const items = [];
    items.push({ text: errors.length ? `${errors.length} data warnings` : "Live data loaded", level: errors.length ? "warn" : "good" });
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
    qs("#chartRange").textContent = `${qs("#fromDate").value} to ${qs("#toDate").value}`;
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

    const drops = groupDropsByHour(state.drops);
    drawBarChart(qs("#dropsChart"), drops.labels, drops.values, "#ff626c");

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
    const rows = [
        ["Calls Offered", fmt(sum(state.calls, "calls_offered", "callsOffered"))],
        ["Calls Handled", fmt(sum(state.calls, "calls_handled", "callsHandled"))],
        ["Calls Abandoned", fmt(sum(state.calls, "calls_abandoned", "callsAbandoned"))],
        ["Dropped Calls", fmt(sum(state.drops, "dropped_calls", "droppedCalls"))],
        ["Service Level", pct(weightedAverage(state.calls, "service_level_pct", "serviceLevelPct", "calls_offered", "callsOffered"))],
        ["Average Speed Answer", seconds(weightedAverage(state.calls, "avg_speed_answer", "avgSpeedAnswer", "calls_handled", "callsHandled")) + " sec"]
    ];
    qs("#businessMetrics").innerHTML = rows.map(([label, value]) => metricRow(label, value)).join("");

    const trend = serviceTrendByHour();
    drawLineChart(qs("#serviceTrendChart"), trend.labels, trend.series, 100);
    drawRadar(qs("#performanceRadar"), radarMetrics());
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
            <td><strong>${escapeHtml(pick(agent, "agent_name", "agentName") || "")}</strong><span class="subline">${escapeHtml(pick(agent, "agent_id", "agentId") || "")} · ${escapeHtml(pick(agent, "team") || "UNKNOWN")}</span></td>
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

function renderDrops() {
    const grouped = groupBySkill(state.drops, "dropped_calls", "droppedCalls");
    qs("#dropList").innerHTML = grouped.labels.map((label, index) => metricRow(label, fmt(grouped.values[index]))).join("") || metricRow("Dropped Calls", "0");
}

function renderCallTypes() {
    renderCallFunnel();
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
                </div>
                <div class="ring" style="--p:${health}"><span>${Math.round(health)}%</span></div>
            </div>
        </article>`;
    }).join("");
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

async function executePcceAction(id) {
    qs("#pcceActionResult").textContent = `Running ${id}...`;
    try {
        const bodyText = qs("#pcceActionBody").value.trim();
        const result = await api(`/api/v1/pcce-api/actions/${encodeURIComponent(id)}/execute`, {
            method: "POST",
            body: JSON.stringify({ body: bodyText || null })
        });
        qs("#pcceActionResult").textContent = JSON.stringify(result, null, 2);
    } catch (error) {
        qs("#pcceActionResult").textContent = error.message;
    }
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

function renderPlan() {
    const items = [
        ["1", "Validate Cisco SQL mappings for AW/HDS and CVP Informix"],
        ["2", "Enable probes for all redundant production components"],
        ["3", "Replace default passwords and configure HTTPS"],
        ["4", "Forward logs and Prometheus metrics to enterprise monitoring"],
        ["5", "Define dropped-call and SLA thresholds with operations"]
    ];
    qs("#planList").innerHTML = items.map(([step, text]) => `<div class="plan-row"><strong>${step}</strong><span>${escapeHtml(text)}</span></div>`).join("");
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
    const ivr = average(state.ivr, "ivr_containment_rate", "ivrContainmentRate");
    const routed = Math.max(0, offered - (ivr === null ? 0 : offered * ivr / 100));
    const items = [
        ["Offered", offered, 100],
        ["IVR Contained", ivr === null ? null : offered * ivr / 100, ivr],
        ["Routed to Agent", routed, offered ? routed / offered * 100 : 0],
        ["Handled", handled, offered ? handled / offered * 100 : 0],
        ["Abandoned", abandoned, offered ? abandoned / offered * 100 : 0]
    ];
    qs("#callFunnel").innerHTML = items.map(([label, value, percent], index) => `<div class="funnel-item">
        <strong>${value === null ? "--" : fmt(value)}</strong>
        <span>${escapeHtml(label)}</span>
        <small>${percent === null ? "--" : pct(percent)}</small>
    </div>${index < items.length - 1 ? `<b class="funnel-arrow">→</b>` : ""}`).join("");
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
        return average(matching, "avg_queue_time", "avgQueueTime") || 0;
    });
    return { labels: rows.labels, values };
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
