// Agent Performance and Finesse rendering extracted from dashboard.js.
// Loaded after dashboard.js so these functions share the existing dashboard state/helpers.

function renderAgents() {
    renderAgentFilters();
    const agents = filteredAgents();
    const tablePages = Math.max(1, Math.ceil(agents.length / uiState.agentPageSize));
    const cardPages = Math.max(1, Math.ceil(agents.length / uiState.agentCardPageSize));
    uiState.agentPage = Math.min(uiState.agentPage, tablePages - 1);
    uiState.agentCardPage = Math.min(uiState.agentCardPage, cardPages - 1);
    const tableStart = uiState.agentPage * uiState.agentPageSize;
    const cardStart = uiState.agentCardPage * uiState.agentCardPageSize;
    const pageRows = agents.slice(tableStart, tableStart + uiState.agentPageSize);
    const cardRows = agents.slice(cardStart, cardStart + uiState.agentCardPageSize);
    renderAgentVisuals(agents, cardRows);
    updateAgentPagers(agents.length, tablePages, cardPages);
    const table = qs("#agentsTable");
    if (!table) return;
    table.innerHTML = pageRows.map(agent => {
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
            <td>${agentMetricSeconds(agent, "avg_handle_time", "avgHandleTime")}</td>
            <td>${progressCell(occupancy)}</td>
            <td>${progressCell(adherence)}</td>
            <td>${fmt(pick(agent, "transfers"))}</td>
            <td>${minutes(pick(agent, "not_ready_time_min", "notReadyTimeMin"))}</td>
        </tr>`;
    }).join("") || `<tr><td colspan="10">No agents match current filters.</td></tr>`;
}

function updateAgentPagers(totalRows, tablePages, cardPages) {
    const count = qs("#agentCount");
    if (count) count.textContent = `${totalRows} agents | ${uiState.agentPageSize} per page`;
    const tableInfo = qs("#agentPageInfo");
    if (tableInfo) tableInfo.textContent = `Table page ${uiState.agentPage + 1}/${tablePages}`;
    const cardInfo = qs("#agentCardPageInfo");
    if (cardInfo) cardInfo.textContent = `Cards page ${uiState.agentCardPage + 1}/${cardPages}`;
    const tablePrev = qs("#agentPrevPage");
    const tableNext = qs("#agentNextPage");
    const cardPrev = qs("#agentCardPrevPage");
    const cardNext = qs("#agentCardNextPage");
    if (tablePrev) tablePrev.disabled = uiState.agentPage === 0;
    if (tableNext) tableNext.disabled = uiState.agentPage >= tablePages - 1;
    if (cardPrev) cardPrev.disabled = uiState.agentCardPage === 0;
    if (cardNext) cardNext.disabled = uiState.agentCardPage >= cardPages - 1;
}

function filteredAgents() {
    const selectedTeams = splitCsv(firstFilterValue("#agentTeamInput"));
    const availableTeams = new Set(state.agents.map(agent => pick(agent, "team") || "UNKNOWN"));
    const effectiveTeams = selectedTeams.filter(team => availableTeams.has(team));
    const selectedAgents = splitCsv(firstFilterValue("#agentPageFilter")).map(value => value.toLowerCase());
    const statusFilter = firstFilterValue("#agentStatusFilter").toLowerCase();
    const search = firstFilterValue("#agentSearchFilter").toLowerCase();
    return state.agents
        .filter(agent => effectiveTeams.length === 0 || effectiveTeams.includes(pick(agent, "team") || "UNKNOWN"))
        .filter(agent => {
            if (!selectedAgents.length) return true;
            const text = [
                pick(agent, "agent_name", "agentName"),
                pick(agent, "agent_id", "agentId")
            ].join(" ").toLowerCase();
            return selectedAgents.some(agentFilter => text.includes(agentFilter));
        })
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

function renderAgentVisuals(agents, pageRows) {
    renderAgentKpis(agents);
    renderAgentCards(pageRows);
    renderAgentCharts(pageRows);
    renderAgentApiWorkspace();
    renderRunningAgentCalls();
}

function renderAgentKpis(agents) {
    const total = agents.length;
    const active = agents.filter(agent => activeDialogsForAgent(agent).length > 0 || effectiveAgentStatus(agent) === "on_call").length;
    const handled = sum(agents, "calls_handled", "callsHandled");
    const aht = average(agents.filter(agent => pick(agent, "avg_handle_time", "avgHandleTime") !== null), "avg_handle_time", "avgHandleTime");
    const teams = unique(agents.map(agent => pick(agent, "team") || "UNKNOWN")).length;
    const finesseUsers = finesseDirectoryCount();
    const liveStates = parsedFinesseUsers();
    const activeDialogs = parsedFinesseDialogs().filter(dialog => dialog.state && !/ended|dropped|failed/i.test(dialog.state)).length;
    const container = qs("#agentKpis");
    if (!container) return;
    container.innerHTML = [
        agentKpi("Agents", total, `${teams} teams`),
        agentKpi("Active", active, "live calls / on call"),
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

function agentMetricSeconds(agent, ...fields) {
    const value = pick(agent, ...fields);
    if (value === null || value === undefined || num(value) === 0) return "--";
    return `${seconds(value)}s`;
}

function renderAgentCards(agents) {
    const container = qs("#agentVisualCards");
    if (!container) return;
    container.innerHTML = agents.map(agent => {
        const status = effectiveAgentStatus(agent);
        const calls = num(pick(agent, "calls_handled", "callsHandled"));
        const occupancy = pick(agent, "occupancy_pct", "occupancyPct");
        const adherence = pick(agent, "adherence_pct", "adherencePct");
        const liveDialogs = activeDialogsForAgent(agent);
        return `<article class="agent-card">
            <div class="agent-avatar">${escapeHtml(agentInitials(agent))}</div>
            <div class="agent-card-main">
                <div class="agent-card-head">
                    <div><strong>${escapeHtml(pick(agent, "agent_name", "agentName") || "Unknown Agent")}</strong><span>${escapeHtml(pick(agent, "agent_id", "agentId") || "")} - ${escapeHtml(pick(agent, "team") || "UNKNOWN")}</span></div>
                    <span class="badge ${status}">${escapeHtml(status.replace("_", " "))}</span>
                </div>
                <div class="agent-card-stats">
                    <span>Live <b>${fmt(liveDialogs.length)}</b></span>
                    <span>Calls <b>${fmt(calls)}</b></span>
                    <span>AHT <b>${agentMetricSeconds(agent, "avg_handle_time", "avgHandleTime")}</b></span>
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
    if (!qs("#agentAhtChart") || !qs("#agentOccupancyChart")) return;
    const top = agents;
    const labels = top.map(shortAgentName);
    const ahtSeries = [
        { label: "Talk", color: "#2ed3c2", values: top.map(agent => num(pick(agent, "avg_talk_time", "avgTalkTime")) || num(pick(agent, "avg_handle_time", "avgHandleTime"))) },
        { label: "Hold", color: "#f4a51c", values: top.map(agent => num(pick(agent, "avg_hold_time", "avgHoldTime"))) },
        { label: "Wrap", color: "#3d82f6", values: top.map(agent => num(pick(agent, "avg_wrap_time", "avgWrapTime"))) }
    ];
    const ahtHasData = ahtSeries.some(series => series.values.some(value => num(value) > 0));
    if (ahtHasData) {
        drawStackedBarChart(qs("#agentAhtChart"), labels, ahtSeries);
    } else {
        drawEmptyChart(qs("#agentAhtChart"), "AHT data was not returned for the selected period");
    }
    const occupancyValues = top.map(agent => effectiveOccupancy(agent));
    if (occupancyValues.some(value => value !== null && value > 0)) {
        drawHorizontalBarChart(qs("#agentOccupancyChart"), labels, occupancyValues.map(value => value ?? 0), colors);
    } else {
        drawEmptyChart(qs("#agentOccupancyChart"), "Occupancy was not returned by HDS/Finesse for this view");
    }
}

function renderAgentApiWorkspace() {
    const finesseUsers = finesseDirectoryCount();
    const finesseTeams = countXmlTags((state.finesseTeams.find(item => String(pick(item, "name") || "").toLowerCase().includes("teams directory")) || {}).body, "Team");
    qs("#agentApiWorkspace").innerHTML = [
        apiWorkspaceCard("Finesse Users", finesseUsers ?? "--", "GET /finesse/api/Users", "Live agent directory and desktop-visible users"),
        apiWorkspaceCard("Finesse Teams", finesseTeams ?? "--", "GET /finesse/api/Teams", "Supervisor/team inventory from Finesse"),
        apiWorkspaceCard("Running Calls", parsedFinesseDialogs().length, "GET /finesse/api/User/{id}/Dialogs", "Rendered below as live calls with caller/dialed/participant details"),
        apiWorkspaceCard("PCCE Agent Config", "Ready", "Unified Config", "Use the provisioning manager below for team/skill desired state")
    ].join("");
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
    </article>`).join("") || `<div class="empty-state"><strong>No running calls found</strong><span>${escapeHtml(finesseDialogEmptyReason())}</span></div>`;
}

function finesseDialogEmptyReason() {
    if (!state.finesseDialogs.length) return "No Finesse dialog probes returned. Check Finesse credentials and discovered users.";
    const ok = state.finesseDialogs.filter(item => num(pick(item, "status_code", "statusCode")) >= 200 && num(pick(item, "status_code", "statusCode")) < 300).length;
    const samples = state.finesseDialogs.slice(0, 3)
        .map(item => `${pick(item, "name")}: HTTP ${pick(item, "status_code", "statusCode") || 0} ${snippet(pick(item, "body"), 80)}`)
        .join(" | ");
    return ok
        ? `Dialog probes returned but no <Dialog> blocks parsed. Samples: ${samples}`
        : `Dialog probes did not return successful dialog XML. Samples: ${samples}`;
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
            const media = firstXmlBlock(block, "mediaProperties") || firstXmlBlock(block, "MediaProperties") || block;
            const participants = [...splitXmlBlocks(block, "Participant"), ...splitXmlBlocks(block, "participant")].map(participant =>
                [
                    xmlAnyTag(participant, "mediaAddress", "mediaAddressDisplayName", "address", "fromAddress"),
                    xmlTag(participant, "state"),
                    xmlTag(participant, "stateCause")
                ].filter(Boolean).join(" - "));
            dialogs.push({
                userId,
                agentId: userId,
                dialogId: xmlAnyTag(block, "id", "dialogId"),
                state: xmlAnyTag(block, "state", "stateName"),
                mediaType: xmlAnyTag(block, "mediaType", "mediaTypeName"),
                fromAddress: xmlAnyTag(media, "fromAddress", "fromAddressDisplayName", "ANI", "ani"),
                toAddress: xmlAnyTag(media, "toAddress", "toAddressDisplayName", "DNIS", "dnis"),
                ani: xmlAnyTag(media, "ANI", "ani", "fromAddress"),
                dnis: xmlAnyTag(media, "DNIS", "dnis", "toAddress"),
                callType: xmlAnyTag(media, "callTypeName", "callTypeId", "callVariable1"),
                callKey: xmlAnyTag(media, "callKeyCallId", "callKeyPrefix", "callGUID", "callGuid"),
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

function xmlAnyTag(xml, ...tagNames) {
    for (const tagName of tagNames) {
        const value = xmlTag(xml, tagName);
        if (value) return value;
    }
    return "";
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
    const status = effectiveAgentStatus(agent);
    if (status === "on_call") return 100;
    if (activeDialogsForAgent(agent).length > 0) return 100;
    return null;
}

function effectiveOccupancy(agent) {
    const value = pick(agent, "occupancy_pct", "occupancyPct");
    if (value !== null && value !== undefined && num(value) > 0) return num(value);
    return derivedAgentOccupancy(agent);
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
    const directory = state.finesseAgents.find(item => String(pick(item, "name") || "").toLowerCase().includes("users directory"));
    const directoryCount = directory ? countXmlTags(pick(directory, "body"), "User") : null;
    const teamQueueItems = [...state.finesseTeams, ...state.finesseQueues];
    const teamQueueList = qs("#finesseTeamQueueList");
    if (teamQueueList) teamQueueList.innerHTML = teamQueueItems.map(item =>
        metricRow(`${pick(item, "name")} - HTTP ${pick(item, "status_code", "statusCode") || 0}`,
            `${fmt(pick(item, "latency_ms", "latencyMs"))} ms | ${snippet(pick(item, "body"), 180)}`)
    ).join("") || metricRow("Teams / Queues", "Configure FINESSE_TEAM_IDS and FINESSE_QUEUE_IDS if needed");
    const discovery = qs("#finesseDiscoveryList");
    if (discovery) {
        discovery.innerHTML = [
            metricRow("Finesse users", directoryCount === null ? `${state.finesseAgents.length} probes` : `${directoryCount} discovered`),
            metricRow("User states", `${parsedFinesseUsers().length} detailed users parsed`),
            metricRow("Dialog probes", `${state.finesseDialogs.length} users checked`),
            metricRow("Active dialogs", `${parsedFinesseDialogs().length} rendered as live calls`)
        ].join("");
    }
}

