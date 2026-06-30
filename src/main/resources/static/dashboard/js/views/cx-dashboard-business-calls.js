// Business Metrics and Call Analytics logic extracted from dashboard.js.
// Loaded after dashboard.js so these functions share the existing dashboard state/helpers.

// Business and call analytics renderers.
function renderBusiness() {
    renderBusinessCards();
    renderCvpIvrNodes();
    renderCvpJourney();
    renderBusinessRules();
    const rowsSource = filteredCallRows("business");
    const offered = sum(rowsSource, "calls_offered", "callsOffered");
    const handled = sum(rowsSource, "calls_handled", "callsHandled");
    const abandoned = sum(rowsSource, "calls_abandoned", "callsAbandoned");
    const unhandled = Math.max(0, offered - handled);
    const dropped = sum(state.drops, "dropped_calls", "droppedCalls");
    const service = businessServiceLevel(rowsSource);
    const aht = businessAht(rowsSource);
    const asa = businessAsa(rowsSource);
    const ivrContainment = businessIvrContainment();
    const fcr = businessFcr(rowsSource);
    const rows = [
        ["Calls Offered", fmt(offered)],
        ["Calls Handled", fmt(handled)],
        ["Unhandled / Ringing", fmt(unhandled)],
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
    trend.hasData ? drawLineChart(qs("#serviceTrendChart"), trend.labels, trend.series, 100)
        : drawEmptyChart(qs("#serviceTrendChart"), "Service level trend unavailable for selected range");
    const radar = radarMetrics();
    radar.length ? drawRadar(qs("#performanceRadar"), radar) : drawEmptyChart(qs("#performanceRadar"), "Performance radar needs call rows");
    const fcrBars = fcrBySkill();
    fcrBars.labels.length ? drawHorizontalBarChart(qs("#fcrBySkillChart"), fcrBars.labels, fcrBars.values, colors)
        : drawEmptyChart(qs("#fcrBySkillChart"), "FCR unavailable for selected range");
    const ivrBars = ivrByApp();
    ivrBars.labels.length ? drawHorizontalBarChart(qs("#ivrBySkillChart"), ivrBars.labels, ivrBars.values, colors)
        : drawEmptyChart(qs("#ivrBySkillChart"), "IVR containment unavailable from CVP journey");
}

function renderCvpIvrNodes() {
    const count = qs("#cvpIvrNodeCount");
    const table = qs("#cvpIvrNodeTable");
    if (!count || !table) return;
    const callIdFilter = firstFilterValue("#ivrCallIdFilter").toLowerCase();
    const callerFilter = firstFilterValue("#ivrCallerFilter").toLowerCase();
    const appFilters = splitCsv(firstFilterValue("#ivrAppFilter"));
    const rows = state.cvpIvrNodes.filter(row => {
        const callId = String(pick(row, "call_id", "callId") || "").toLowerCase();
        const caller = String(pick(row, "caller_number", "callerNumber") || "").toLowerCase();
        const app = String(pick(row, "app_name", "appName") || "UNKNOWN");
        return (!callIdFilter || callId.includes(callIdFilter))
            && (!callerFilter || caller.includes(callerFilter))
            && (!appFilters.length || appFilters.includes(app));
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
        <td><span class="badge ${ivrDispositionClass(pick(row, "call_disposition_id", "callDispositionId"))}">${escapeHtml(pick(row, "call_disposition_flag_desc", "callDispositionFlagDesc") || "")}</span><span class="subline">${escapeHtml(abandonPartyForNode(row))}</span></td>
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
            <span>${escapeHtml(journey.abandonParty)}</span>
        </div>
        <div class="journey-path">
            ${journey.nodes.map((node, index) => `<span class="${/agent routing/i.test(node) ? "terminal" : ""}"><b>${index + 1}</b>${escapeHtml(node)}</span>`).join("<i>-&gt;</i>")}
        </div>
        <button class="small-btn" data-trace-call="${escapeHtml(journey.callId)}">Open Trace</button>
    </article>`).join("") || `<div class="empty-state"><strong>No CVP journey rows</strong><span>Select a date/app with CVP Reporting data.</span></div>`;
}

function cvpJourneys() {
    const map = new Map();
    const appFilters = splitCsv(firstFilterValue("#ivrAppFilter"));
    state.cvpIvrNodes
        .filter(row => !appFilters.length || appFilters.includes(pick(row, "app_name", "appName") || "UNKNOWN"))
        .forEach(row => {
        const callId = pick(row, "call_id", "callId") || "UNKNOWN";
        const existing = map.get(callId) || {
            callId,
            caller: pick(row, "caller_number", "callerNumber"),
            start: pick(row, "call_start_time", "callStartTime"),
            app: pick(row, "app_name", "appName"),
            duration: pick(row, "duration"),
            disposition: pick(row, "call_disposition_flag_desc", "callDispositionFlagDesc"),
            dispositionId: pick(row, "call_disposition_id", "callDispositionId"),
            abandonParty: abandonPartyForNode(row),
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

function abandonPartyForNode(rowOrJourney) {
    const code = Number(pick(rowOrJourney, "call_disposition_id", "callDispositionId", "dispositionId"));
    const description = String(pick(rowOrJourney, "call_disposition_flag_desc", "callDispositionFlagDesc", "disposition") || "").toLowerCase();
    const nodes = String(pick(rowOrJourney, "nodes") || "").toLowerCase();
    if (code === 2 || code === 1001 || /abandon|hangup|hang up/.test(description)) {
        return "Customer abandon / hangup";
    }
    if (code === 13 || /called party disconnected/.test(description)) {
        return nodes.includes("agent routing") ? "Agent/called-party disconnect" : "Called-party disconnect";
    }
    if ([18, 1044].includes(code) || /error/.test(description)) {
        return "System / IVR error";
    }
    if (code === 0 || code === 1 || /normal/.test(description)) {
        return "Completed";
    }
    return code ? `Disposition ${code}` : "Not classified";
}

// Agent Performance and Finesse rendering lives in /dashboard/js/views/cx-dashboard-agents.js.

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
    const selectedCallKey = firstFilterValue("#callKeyFilter");
    const cvpTrace = selectedCallKey ? cvpTraceEvents(selectedCallKey) : [];
    const hdsEvents = selectedCallKey
        ? state.callFlow.filter(event => normalizeToken(pick(event, "call_key", "callKey")) === normalizeToken(selectedCallKey))
        : state.callFlow;
    const events = hdsEvents.length ? hdsEvents : cvpTrace;
    qs("#callFlowCount").textContent = `${events.length} events`;
    qs("#callFlowTimeline").innerHTML = events.slice(0, 80).map(event => `<div class="trace-event">
        <div class="trace-dot"></div>
        <div>
            <strong>${escapeHtml(pick(event, "stage") || "")}</strong>
            <span>${escapeHtml(pick(event, "event_time", "eventTime") || "")} | ${escapeHtml(pick(event, "node") || "")} | ${escapeHtml(pick(event, "call_key", "callKey") || "")}</span>
            <p>${escapeHtml(pick(event, "call_type", "callType") || "")} / ${escapeHtml(pick(event, "skill_group", "skillGroup") || "")} / ${escapeHtml(pick(event, "agent") || "No agent")}</p>
            <small>${escapeHtml(pick(event, "detail") || "")}</small>
        </div>
    </div>`).join("") || `<div class="empty-state"><strong>No call flow events</strong><span>Enter a call key or widen the selected date range.</span></div>`;
}

function cvpTraceEvents(callKey) {
    const target = normalizeToken(callKey);
    return (state.cvpIvrNodes || [])
        .filter(row => normalizeToken(pick(row, "call_id", "callId")) === target)
        .map((row, index) => ({
            stage: pick(row, "flag") || `CVP node ${index + 1}`,
            event_time: pick(row, "call_start_time", "callStartTime"),
            node: pick(row, "app_name", "appName") || "CVP IVR",
            call_key: pick(row, "call_id", "callId"),
            call_type: "CVP IVR",
            skill_group: "IVR Journey",
            agent: "",
            detail: `${pick(row, "caller_number", "callerNumber") || "Unknown caller"} | ${pick(row, "call_disposition_flag_desc", "callDispositionFlagDesc") || ""} | Duration ${pick(row, "duration") || "--"} sec`
        }));
}

function normalizeToken(value) {
    return String(value || "").replace(/[^A-Za-z0-9]/g, "").toUpperCase();
}

// Business metric calculations and chart view models.
function renderBusinessCards() {
    const grouped = groupSkillMetrics(filteredCallRows("business"));
    const totalPages = Math.max(1, Math.ceil(grouped.length / uiState.businessPageSize));
    uiState.businessCardPage = Math.min(uiState.businessCardPage, totalPages - 1);
    const start = uiState.businessCardPage * uiState.businessPageSize;
    const rows = grouped.slice(start, start + uiState.businessPageSize);
    const pageInfo = qs("#businessCardsPageInfo");
    if (pageInfo) pageInfo.textContent = `${grouped.length} skill groups | page ${uiState.businessCardPage + 1}/${totalPages}`;
    const prev = qs("#businessCardsPrevPage");
    const next = qs("#businessCardsNextPage");
    if (prev) prev.disabled = uiState.businessCardPage === 0;
    if (next) next.disabled = uiState.businessCardPage >= totalPages - 1;
    qs("#businessSkillCards").innerHTML = rows.map((item, index) => `<article class="business-card">
        <h3><i class="dot" style="background:${colors[index % colors.length]}"></i>${escapeHtml(item.skill)}</h3>
        <div class="business-metrics">
            <span>Offered<strong>${fmt(item.offered)}</strong></span>
            <span>SL%<strong class="teal">${pct(item.service)}</strong></span>
            <span>FCR<strong>${pct(item.fcr)}</strong></span>
            <span>Handled<strong>${fmt(item.handled)}</strong></span>
            <span>Unhandled<strong class="amber">${fmt(Math.max(0, item.offered - item.handled))}</strong></span>
            <span>AHT<strong>${seconds(item.aht)}s</strong></span>
            <span>Abandoned<strong class="red">${fmt(item.abandoned)}</strong></span>
        </div>
    </article>`).join("") || `<article class="business-card"><h3>No skill group data</h3><p>Check HDS query mapping and selected date range.</p></article>`;
}

function groupSkillMetrics(rows = filteredCallRows("business")) {
    const map = new Map();
    rows.forEach(row => {
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
        aht: item.ahtWeight ? item.weightedAht / item.ahtWeight : businessAht(rows).value,
        fcr: businessFcrForCounts(item.offered, item.handled, item.abandoned),
        answerRate: derivedPct(item.handled, item.offered),
        abandonRate: derivedPct(item.abandoned, item.offered)
    })).sort((a, b) => b.offered - a.offered);
}

function serviceTrendByHour() {
    const rows = filteredCallRows("business");
    const topSkills = groupSkillMetrics(rows).slice(0, 3).map(item => item.skill);
    const hours = Array.from({ length: 24 }, (_, hour) => hour);
    const labels = hours.map(hour => `${hour}:00`);
    const series = topSkills.map((skill, index) => ({
        label: skill,
        color: colors[index % colors.length],
        values: hours.map(hour => skillHourService(skill, hour, rows))
    }));
    return {
        labels,
        series: series.concat([{ label: "Target", color: "#f4a51c", values: labels.map(() => businessSettings.slTarget) }]),
        hasData: topSkills.length > 0
    };
}

function skillHourService(skill, hour, sourceRows = filteredCallRows("business")) {
    const rows = sourceRows.filter(row => (pick(row, "skill_group", "skillGroup") || "UNKNOWN") === skill && num(pick(row, "hour")) === hour);
    const real = weightedAverage(rows, "service_level_pct", "serviceLevelPct", "calls_offered", "callsOffered");
    if (real !== null) return real;
    if (businessSettings.fallbackMode !== "derived") return 0;
    return derivedPct(sum(rows, "calls_handled", "callsHandled"), sum(rows, "calls_offered", "callsOffered")) || 0;
}

function radarMetrics() {
    const sourceRows = filteredCallRows("business");
    return groupSkillMetrics(sourceRows).slice(0, 3).map((skill, index) => {
        const rows = sourceRows.filter(row => (pick(row, "skill_group", "skillGroup") || "UNKNOWN") === skill.skill);
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
    return { value: proxy, source: `Derived proxy until ANI repeat-call source is mapped; target window ${businessSettings.fcrWindowDays || 7} days`, short: "proxy" };
}

function businessFcrForCounts(offered, handled, abandoned) {
    return offered ? Math.max(0, (handled - abandoned * 0.15) / offered * 100) : null;
}

function renderBusinessRules() {
    qs("#businessCalcSummary").textContent = `SL target ${businessSettings.slTarget}% | AHT target ${businessSettings.ahtTarget}s | FCR window ${businessSettings.fcrWindowDays || 7}d | ${businessSettings.fallbackMode}`;
    const rows = filteredCallRows("business");
    const sl = businessServiceLevel(rows);
    const aht = businessAht(rows);
    const asa = businessAsa(rows);
    const ivr = businessIvrContainment();
    const fcr = businessFcr(rows);
    qs("#businessCalcRules").innerHTML = [
        metricRow("Service Level", `${sl.source}. Formula: Cisco ServiceLevelCalls / ServiceLevelCallsOffered; fallback: handled / offered.`),
        metricRow("AHT", `${aht.source}. Formula: Cisco interval talk + wrap (+ hold when mapped) / handled.`),
        metricRow("ASA", `${asa.source}. Formula: AnswerWaitTime / handled.`),
        metricRow("IVR Containment", `${ivr.source}. Normal/Error/Hangup cause IDs configurable in CVP SQL.`),
        metricRow("First Call Resolution", `${fcr.source}. Recommended production source: CRM repeat-contact or survey resolution flag.`)
    ].join("");
}

function fcrBySkill() {
    const sourceRows = filteredCallRows("business");
    const items = groupSkillMetrics(sourceRows).slice(0, 8);
    return { labels: items.map(item => item.skill), values: items.map(item => businessFcr(sourceRows.filter(row => (pick(row, "skill_group", "skillGroup") || "UNKNOWN") === item.skill)).value || 0) };
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

// Call analytics funnel, abandonment and queue helpers.
function renderCallFunnel() {
    const source = funnelSource();
    const offered = source.offered;
    const handled = source.handled;
    const abandoned = source.abandoned;
    const hasAbandonSource = source.hasAbandonSource;
    const ivr = businessIvrContainment().value;
    const routed = Math.max(0, offered - (ivr === null ? 0 : offered * ivr / 100));
    const hasIvr = ivr !== null;
    const queued = hasIvr ? routed : Math.max(0, offered);
    const unknownMapping = source.unknownMapping;
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
    <div class="flow-note">${source.note}${unknownMapping ? " Some calls are unmapped to skill groups. Use Admin diagnostics or CUIC SQL to map SkillGroupSkillTargetID/CallTypeID exactly." : ""}</div>`;
}

function funnelSource() {
    const callTypeFilter = firstFilterValue("#callsCallTypeFilter");
    if (callTypeFilter && state.callTypes.length) {
        const offered = sum(state.callTypes, "calls");
        const handled = sum(state.callTypes, "handled_calls", "handledCalls");
        return {
            offered,
            handled,
            abandoned: Math.max(0, offered - handled),
            hasAbandonSource: false,
            unknownMapping: state.callTypes.some(row => isUnknownLabel(pick(row, "skill_group", "skillGroup"))),
            note: "Funnel uses HDS call-type rows for the selected call type and period."
        };
    }
    const rows = activeView === "calls" ? filteredCallRows("calls") : state.calls;
    return {
        offered: sum(rows, "calls_offered", "callsOffered"),
        handled: sum(rows, "calls_handled", "callsHandled"),
        abandoned: sum(rows, "calls_abandoned", "callsAbandoned"),
        hasAbandonSource: rows.some(row => pick(row, "calls_abandoned", "callsAbandoned") !== null && pick(row, "calls_abandoned", "callsAbandoned") !== undefined),
        unknownMapping: rows.some(row => isUnknownLabel(pick(row, "skill_group", "skillGroup"))),
        note: "Funnel uses live HDS/CVP fields available for the selected filters."
    };
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

