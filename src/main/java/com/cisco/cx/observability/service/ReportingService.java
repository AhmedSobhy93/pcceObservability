package com.cisco.cx.observability.service;

import com.cisco.cx.observability.config.PcceProperties;
import com.cisco.cx.observability.model.AgentStat;
import com.cisco.cx.observability.model.AgentStatus;
import com.cisco.cx.observability.model.CallMetric;
import com.cisco.cx.observability.model.CallFlowEvent;
import com.cisco.cx.observability.model.CallTypeMetric;
import com.cisco.cx.observability.model.ContactCenterSummary;
import com.cisco.cx.observability.model.CvpIvrNodeMetric;
import com.cisco.cx.observability.model.CuicReportView;
import com.cisco.cx.observability.model.DispositionBreakdown;
import com.cisco.cx.observability.model.DroppedCallMetric;
import com.cisco.cx.observability.model.ReferenceOption;
import com.cisco.cx.observability.security.AccessControlService;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReportingService {

    private static final Logger log = LoggerFactory.getLogger(ReportingService.class);

    private final JdbcTemplate hdsJdbcTemplate;
    private final JdbcTemplate awJdbcTemplate;
    private final JdbcTemplate cvpReportingJdbcTemplate;
    private final PcceProperties pcceProperties;
    private final ComponentStatusService componentStatusService;
    private final AccessControlService accessControlService;
    private final DispositionCodeService dispositionCodeService;
    private final QueryPerformanceService queryPerformanceService;

    public ReportingService(
            @Qualifier("awJdbcTemplate") JdbcTemplate awJdbcTemplate,
            @Qualifier("hdsJdbcTemplate") JdbcTemplate hdsJdbcTemplate,
            @Qualifier("cvpReportingJdbcTemplate") JdbcTemplate cvpReportingJdbcTemplate,
            PcceProperties pcceProperties,
            ComponentStatusService componentStatusService,
            AccessControlService accessControlService,
            DispositionCodeService dispositionCodeService,
            QueryPerformanceService queryPerformanceService) {
        this.awJdbcTemplate = awJdbcTemplate;
        this.hdsJdbcTemplate = hdsJdbcTemplate;
        this.cvpReportingJdbcTemplate = cvpReportingJdbcTemplate;
        this.pcceProperties = pcceProperties;
        this.componentStatusService = componentStatusService;
        this.accessControlService = accessControlService;
        this.dispositionCodeService = dispositionCodeService;
        this.queryPerformanceService = queryPerformanceService;
    }

    public List<CallMetric> callMetrics(LocalDate from, LocalDate to, String skillGroup) {
        validateDateRange(from, to);
        String normalizedSkillGroup = blankToNull(skillGroup);
        boolean filtered = normalizedSkillGroup != null;
        List<CallMetric> intervalMetrics = timedQuery("hds.callMetrics", () -> hdsJdbcTemplate.query(
                    pcceProperties.getQueries().getCallMetrics(),
                    this::mapCallMetric,
                    start(from),
                    exclusiveEnd(to),
                    normalizedSkillGroup,
                    normalizedSkillGroup,
                    normalizedSkillGroup));
        if (!pcceProperties.getQueries().isCallMetricsTcdFallbackEnabled()
                || intervalMetrics.stream().mapToLong(metric -> nullToZero(metric.callsOffered())).sum() > 0) {
            return intervalMetrics;
        }
        log.warn("call_metrics_interval_empty using_tcd_fallback=true from={} to={} skillGroup={}",
                from, to, normalizedSkillGroup);
        try {
            List<CallMetric> fallbackMetrics = timedQuery("hds.callMetrics.tcdFallback", () -> hdsJdbcTemplate.query(
                        pcceProperties.getQueries().getCallMetricsTcdFallback(),
                        this::mapCallMetric,
                        start(from),
                        exclusiveEnd(to),
                        normalizedSkillGroup,
                        normalizedSkillGroup,
                        normalizedSkillGroup));
            List<CallMetric> enriched = enrichCallMetricLabels(fallbackMetrics);
            if (filtered && enriched.stream().mapToLong(metric -> nullToZero(metric.callsOffered())).sum() == 0) {
                log.warn("call_metrics_filtered_empty retrying_unfiltered=true from={} to={} skillGroup={}", from, to, normalizedSkillGroup);
                List<CallMetric> unfiltered = timedQuery("hds.callMetrics.tcdFallback.unfiltered", () -> hdsJdbcTemplate.query(
                        pcceProperties.getQueries().getCallMetricsTcdFallback(),
                        this::mapCallMetric,
                        start(from),
                        exclusiveEnd(to),
                        null,
                        null,
                        null));
                return filterCallMetrics(enrichCallMetricLabels(unfiltered), normalizedSkillGroup);
            }
            return enriched;
        } catch (DataAccessException ex) {
            log.warn("call_metrics_tcd_fallback_unavailable error={}", ex.getMostSpecificCause().getMessage());
            return intervalMetrics;
        }
    }

    public List<AgentStat> agentStats(LocalDate from, LocalDate to, String agentId, String team) {
        validateDateRange(from, to);
        String normalizedAgentId = blankToNull(accessControlService.scopedAgentId(agentId));
        String normalizedTeam = blankToNull(accessControlService.scopedTeam(team));
        List<AgentStat> roster = timedQuery("aw.agentStats.roster", () -> awJdbcTemplate.query(
                pcceProperties.getQueries().getAgentStats(),
                this::mapAgentStat,
                start(from),
                normalizedAgentId,
                normalizedAgentId,
                normalizedAgentId,
                normalizedTeam,
                normalizedTeam));
        List<AgentStat> tcdStats = timedQuery("hds.agentStats.tcd", () -> hdsJdbcTemplate.query(
                pcceProperties.getQueries().getAgentStatsTcd(),
                this::mapAgentStat,
                start(from),
                exclusiveEnd(to),
                normalizedAgentId,
                normalizedAgentId,
                normalizedAgentId,
                normalizedTeam,
                normalizedTeam));
        List<AgentStat> enrichedTcd = aggregateAgents(enrichAgentLabels(tcdStats));
        if (roster.isEmpty()) {
            return enrichedTcd;
        }
        return mergeRosterWithActivity(roster, enrichedTcd);
    }

    public List<CallTypeMetric> callTypeMetrics(LocalDate from, LocalDate to, String callType, String skillGroup) {
        validateDateRange(from, to);
        String normalizedCallType = blankToNull(callType);
        String normalizedSkillGroup = blankToNull(skillGroup);
        List<CallTypeMetric> metrics = timedQuery("hds.callTypeMetrics", () -> hdsJdbcTemplate.query(
                pcceProperties.getQueries().getCallTypeMetrics(),
                (rs, rowNum) -> new CallTypeMetric(
                        rs.getObject("date", LocalDate.class),
                        rs.getInt("hour"),
                        rs.getString("call_type"),
                        rs.getString("skill_group"),
                        rs.getLong("calls"),
                        rs.getLong("handled_calls")),
                start(from),
                exclusiveEnd(to),
                normalizedCallType,
                normalizedCallType,
                normalizedCallType,
                normalizedSkillGroup,
                normalizedSkillGroup,
                normalizedSkillGroup));
        List<CallTypeMetric> enriched = enrichCallTypeLabels(metrics);
        if ((normalizedCallType != null || normalizedSkillGroup != null) && enriched.isEmpty()) {
            log.warn("call_type_metrics_filtered_empty retrying_unfiltered=true from={} to={} callType={} skillGroup={}",
                    from, to, normalizedCallType, normalizedSkillGroup);
            List<CallTypeMetric> unfiltered = timedQuery("hds.callTypeMetrics.unfiltered", () -> hdsJdbcTemplate.query(
                    pcceProperties.getQueries().getCallTypeMetrics(),
                    (rs, rowNum) -> new CallTypeMetric(
                            rs.getObject("date", LocalDate.class),
                            rs.getInt("hour"),
                            rs.getString("call_type"),
                            rs.getString("skill_group"),
                            rs.getLong("calls"),
                            rs.getLong("handled_calls")),
                    start(from),
                    exclusiveEnd(to),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));
            return filterCallTypeMetrics(enrichCallTypeLabels(unfiltered), normalizedCallType, normalizedSkillGroup);
        }
        return enriched;
    }

    public List<CuicReportView> cuicReports() {
        return List.of(
                new CuicReportView("stock.skill-group-interval", "Skill Group Historical All Fields", "Historical",
                        "HDS t_Skill_Group_Interval", "CUIC-style interval KPIs: offered, handled, abandon, service level, AHT, ASA and queue wait.", "/api/v1/metrics/calls"),
                new CuicReportView("stock.agent-team", "Agent Team Historical", "Historical",
                        "HDS t_Termination_Call_Detail + AW config", "Agent/team productivity aligned to CUIC filters where HDS joins are available.", "/api/v1/agents/stats"),
                new CuicReportView("stock.call-type", "Call Type Historical", "Historical",
                        "HDS t_Termination_Call_Detail + t_Call_Type", "Call type and skill-group breakdown for routing/business views.", "/api/v1/metrics/call-types"),
                new CuicReportView("stock.disposition", "Call Disposition Historical", "Historical",
                        "HDS t_Termination_Call_Detail", "Disposition-code breakdown for abandoned/dropped-call validation.", "/api/v1/calls/disposition-breakdown"),
                new CuicReportView("stock.ivr-containment", "CVP IVR Containment", "Historical",
                        "CVP Reporting Informix", "IVR containment from CVP reporting where schema is configured.", "/api/v1/metrics/ivr-containment"),
                new CuicReportView("stock.realtime-readiness", "Live/Realtime Operational Status", "Realtime",
                        "AW config + probes + APIs", "Live component/API/readiness view; true UCCE realtime tables can be added when exact schema is confirmed.", "/api/v1/operations/assessment/last"));
    }

    public List<CallFlowEvent> callFlow(LocalDate from, LocalDate to, String callKey, String agentId) {
        validateDateRange(from, to);
        String normalizedCallKey = blankToNull(callKey);
        String normalizedAgentId = blankToNull(agentId);
        return timedQuery("hds.callFlow", () -> hdsJdbcTemplate.query("""
                SELECT TOP 500
                    tcd.DateTime AS event_time,
                    COALESCE(CAST(tcd.RouterCallKey AS varchar(50)), CAST(tcd.RecoveryKey AS varchar(50)), CAST(tcd.DateTime AS varchar(30))) AS call_key,
                    COALESCE(pg.EnterpriseName, 'PCCE') AS node,
                    CASE
                        WHEN COALESCE(tcd.AgentSkillTargetID, 0) > 0 THEN 'Agent connected'
                        WHEN COALESCE(tcd.CallDisposition, 0) > 0 THEN 'Disposition ' + CAST(tcd.CallDisposition AS varchar(20))
                        ELSE 'Routed / queued'
                    END AS stage,
                    COALESCE(ct.EnterpriseName, 'CallType ' + CAST(tcd.CallTypeID AS varchar(50)), 'UNMAPPED') AS call_type,
                    COALESCE(sg.EnterpriseName, 'SkillTarget ' + CAST(tcd.SkillGroupSkillTargetID AS varchar(50)), 'UNMAPPED') AS skill_group,
                    COALESCE(p.FirstName + ' ' + p.LastName, p.LoginName, CASE WHEN tcd.AgentSkillTargetID IS NULL THEN NULL ELSE 'Agent ' + CAST(tcd.AgentSkillTargetID AS varchar(50)) END) AS agent,
                    CAST(tcd.CallDisposition AS varchar(50)) AS disposition,
                    CAST(NULL AS bigint) AS duration_seconds,
                    'RouterCallKey=' + COALESCE(CAST(tcd.RouterCallKey AS varchar(50)), 'n/a')
                        + '; PeripheralCallKey=' + COALESCE(CAST(tcd.PeripheralCallKey AS varchar(50)), 'n/a')
                        + '; RecoveryKey=' + COALESCE(CAST(tcd.RecoveryKey AS varchar(50)), 'n/a') AS detail
                FROM t_Termination_Call_Detail tcd
                LEFT JOIN t_Call_Type ct ON ct.CallTypeID = tcd.CallTypeID
                LEFT JOIN t_Skill_Group sg ON sg.SkillTargetID = tcd.SkillGroupSkillTargetID
                LEFT JOIN t_Agent a ON a.SkillTargetID = tcd.AgentSkillTargetID
                LEFT JOIN t_Person p ON p.PersonID = a.PersonID
                LEFT JOIN t_Peripheral pg ON pg.PeripheralID = tcd.PeripheralID
                WHERE tcd.DateTime >= ? AND tcd.DateTime < ?
                  AND (? IS NULL OR CAST(tcd.RouterCallKey AS varchar(50)) = ? OR CAST(tcd.PeripheralCallKey AS varchar(50)) = ? OR CAST(tcd.RecoveryKey AS varchar(50)) = ?)
                  AND (? IS NULL OR p.LoginName = ? OR CAST(tcd.AgentSkillTargetID AS varchar(50)) = ?)
                ORDER BY tcd.DateTime
                """, (rs, rowNum) -> new CallFlowEvent(
                rs.getTimestamp("event_time") == null ? null : rs.getTimestamp("event_time").toLocalDateTime(),
                rs.getString("call_key"),
                rs.getString("node"),
                rs.getString("stage"),
                rs.getString("call_type"),
                rs.getString("skill_group"),
                rs.getString("agent"),
                rs.getString("disposition"),
                rs.getLong("duration_seconds"),
                rs.getString("detail")),
                start(from),
                exclusiveEnd(to),
                normalizedCallKey,
                normalizedCallKey,
                normalizedCallKey,
                normalizedCallKey,
                normalizedAgentId,
                normalizedAgentId,
                normalizedAgentId));
    }

    public List<ReferenceOption> skillGroups() {
        return timedQuery("aw.reference.skillGroups", () -> awJdbcTemplate.query("""
                SELECT TOP 500
                    EnterpriseName AS value,
                    EnterpriseName AS label,
                    CAST(SkillTargetID AS varchar(50)) AS detail
                FROM t_Skill_Group
                WHERE EnterpriseName IS NOT NULL
                ORDER BY EnterpriseName
                """, (rs, rowNum) -> new ReferenceOption(
                rs.getString("value"),
                rs.getString("label"),
                rs.getString("detail"))));
    }

    public List<ReferenceOption> callTypes() {
        return timedQuery("aw.reference.callTypes", () -> awJdbcTemplate.query("""
                SELECT TOP 500
                    EnterpriseName AS value,
                    EnterpriseName AS label,
                    CAST(CallTypeID AS varchar(50)) AS detail
                FROM t_Call_Type
                WHERE EnterpriseName IS NOT NULL
                ORDER BY EnterpriseName
                """, (rs, rowNum) -> new ReferenceOption(
                rs.getString("value"),
                rs.getString("label"),
                rs.getString("detail"))));
    }

    public List<ReferenceOption> agents() {
        return timedQuery("aw.reference.agents", () -> awJdbcTemplate.query("""
                SELECT TOP 1000
                    COALESCE(p.LoginName, CAST(a.SkillTargetID AS varchar(50))) AS value,
                    COALESCE(p.FirstName + ' ' + p.LastName, p.LoginName, CAST(a.SkillTargetID AS varchar(50))) AS label,
                    CAST(a.SkillTargetID AS varchar(50)) AS detail
                FROM t_Agent a
                LEFT JOIN t_Person p ON p.PersonID = a.PersonID
                ORDER BY label
                """, (rs, rowNum) -> new ReferenceOption(
                rs.getString("value"),
                rs.getString("label"),
                rs.getString("detail"))));
    }

    public List<CvpIvrNodeMetric> cvpIvrNodes(LocalDate from, LocalDate to, String appName) {
        validateDateRange(from, to);
        String normalizedAppName = blankToNull(appName);
        Object[] args = cvpArgs(from, to, normalizedAppName);
        String nodeSql = normalizedAppName == null
                ? PcceProperties.DefaultSql.CVP_IVR_NODES
                : PcceProperties.DefaultSql.CVP_IVR_NODES_BY_APP;
        try {
            return timedQuery("cvp.ivrNodes", () -> cvpReportingJdbcTemplate.query(
                    nodeSql,
                    this::mapCvpIvrNode,
                    args));
        } catch (DataAccessException ex) {
            log.warn("cvp_ivr_nodes_unavailable using_session_fallback=true error={}", ex.getMostSpecificCause().getMessage());
            String sessionSql = normalizedAppName == null
                    ? PcceProperties.DefaultSql.CVP_IVR_SESSIONS
                    : PcceProperties.DefaultSql.CVP_IVR_SESSIONS_BY_APP;
            try {
                return timedQuery("cvp.ivrSessionsFallback", () -> cvpReportingJdbcTemplate.query(
                        sessionSql,
                        this::mapCvpIvrNode,
                        args));
            } catch (DataAccessException fallbackEx) {
                log.warn("cvp_ivr_sessions_unavailable error={}", fallbackEx.getMostSpecificCause().getMessage());
                return List.of();
            }
        }
    }

    private Object[] cvpArgs(LocalDate from, LocalDate to, String appName) {
        if (appName == null) {
            return new Object[] { start(from), exclusiveEnd(to) };
        }
        return new Object[] { start(from), exclusiveEnd(to), appName };
    }

    private CvpIvrNodeMetric mapCvpIvrNode(ResultSet rs, int rowNum) throws SQLException {
        return new CvpIvrNodeMetric(
                rs.getString("call_id"),
                toLocalDateTime(rs, "call_start_time"),
                toLocalDateTime(rs, "call_end_time"),
                rs.getString("caller_number"),
                rs.getString("app_name"),
                rs.getString("duration"),
                rs.getString("flag"),
                rs.getObject("call_disposition_id") == null ? null : rs.getInt("call_disposition_id"),
                rs.getString("call_disposition_flag_desc"));
    }

    public List<DroppedCallMetric> droppedCalls(LocalDate from, LocalDate to, String skillGroup) {
        validateDateRange(from, to);
        if (!pcceProperties.getQueries().isDroppedCallsEnabled()) {
            return List.of();
        }
        String normalizedSkillGroup = blankToNull(skillGroup);
        List<DroppedCallMetric> drops = timedQuery("hds.droppedCalls", () -> hdsJdbcTemplate.query(
                    pcceProperties.getQueries().getDroppedCalls(),
                    this::mapDroppedCallMetric,
                    start(from),
                    exclusiveEnd(to),
                    normalizedSkillGroup,
                    normalizedSkillGroup,
                    normalizedSkillGroup));
        List<DroppedCallMetric> enriched = enrichDroppedLabels(drops);
        if (normalizedSkillGroup != null && enriched.isEmpty()) {
            log.warn("dropped_calls_filtered_empty retrying_unfiltered=true from={} to={} skillGroup={}", from, to, normalizedSkillGroup);
            List<DroppedCallMetric> unfiltered = timedQuery("hds.droppedCalls.unfiltered", () -> hdsJdbcTemplate.query(
                    pcceProperties.getQueries().getDroppedCalls(),
                    this::mapDroppedCallMetric,
                    start(from),
                    exclusiveEnd(to),
                    null,
                    null,
                    null));
            return filterDroppedCalls(enrichDroppedLabels(unfiltered), normalizedSkillGroup);
        }
        return enriched;
    }

    public List<DispositionBreakdown> dispositionBreakdown(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        return timedQuery("hds.dispositionBreakdown", () -> hdsJdbcTemplate.query(
                    pcceProperties.getQueries().getDispositionBreakdown(),
                    (rs, rowNum) -> {
                        int code = rs.getInt("disposition_code");
                        var reference = dispositionCodeService.find(code);
                        return new DispositionBreakdown(
                                code,
                                reference.name(),
                                reference.category(),
                                reference.countedAsDrop(),
                                rs.getLong("calls"));
                    },
                    start(from),
                    exclusiveEnd(to)));
    }

    public List<IvrContainmentMetric> ivrContainment(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        try {
            return timedQuery("cvp.ivrContainment", () -> cvpReportingJdbcTemplate.query(
                        pcceProperties.getQueries().getIvrContainment(),
                        (rs, rowNum) -> new IvrContainmentMetric(
                                toLocalDate(rs.getObject("call_date")),
                                toHour(rs.getObject("call_hour")),
                                rs.getBigDecimal("ivr_containment_rate")),
                        start(from),
                        exclusiveEnd(to)));
        } catch (DataAccessException ex) {
            log.warn("ivr_containment_unavailable error={}", ex.getMostSpecificCause().getMessage());
            return deriveIvrContainmentFromNodes(from, to);
        }
    }

    private List<IvrContainmentMetric> deriveIvrContainmentFromNodes(LocalDate from, LocalDate to) {
        List<CvpIvrNodeMetric> nodes = cvpIvrNodes(from, to, null);
        if (nodes.isEmpty()) {
            return List.of();
        }
        Map<String, CvpIvrNodeMetric> calls = new LinkedHashMap<>();
        for (CvpIvrNodeMetric node : nodes) {
            if (node.callId() != null) {
                calls.putIfAbsent(node.callId(), node);
            }
        }
        Map<String, int[]> buckets = new LinkedHashMap<>();
        for (CvpIvrNodeMetric call : calls.values()) {
            if (call.callStartTime() == null) {
                continue;
            }
            String key = call.callStartTime().toLocalDate() + "|" + call.callStartTime().getHour();
            int[] counts = buckets.computeIfAbsent(key, ignored -> new int[2]);
            counts[0] += 1;
            if (!isIvrNonContained(call.callDispositionId())) {
                counts[1] += 1;
            }
        }
        List<IvrContainmentMetric> metrics = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : buckets.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            int total = entry.getValue()[0];
            int contained = entry.getValue()[1];
            BigDecimal rate = total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(contained * 100.0 / total);
            metrics.add(new IvrContainmentMetric(LocalDate.parse(parts[0]), Integer.parseInt(parts[1]), rate));
        }
        return metrics;
    }

    private boolean isIvrNonContained(Integer causeId) {
        if (causeId == null) {
            return false;
        }
        return causeId == 2 || causeId == 18 || causeId == 1001 || causeId == 1044;
    }

    public ContactCenterSummary summary(LocalDate from, LocalDate to) {
        List<CallMetric> calls = callMetrics(from, to, null);
        List<DroppedCallMetric> drops = droppedCalls(from, to, null);

        long offered = calls.stream().mapToLong(metric -> nullToZero(metric.callsOffered())).sum();
        long handled = calls.stream().mapToLong(metric -> nullToZero(metric.callsHandled())).sum();
        long abandoned = calls.stream().mapToLong(metric -> nullToZero(metric.callsAbandoned())).sum();
        long dropped = drops.stream().mapToLong(metric -> nullToZero(metric.droppedCalls())).sum();

        return new ContactCenterSummary(
                offered,
                handled,
                abandoned,
                dropped,
                weightedAverage(calls, offered, MetricValue.SERVICE_LEVEL),
                weightedAverage(calls, handled, MetricValue.AHT),
                weightedAverage(calls, handled, MetricValue.ASA),
                componentStatusService.status());
    }

    private CallMetric mapCallMetric(ResultSet rs, int rowNum) throws SQLException {
        return new CallMetric(
                rs.getObject("date", LocalDate.class),
                rs.getInt("hour"),
                rs.getString("skill_group"),
                rs.getLong("calls_offered"),
                rs.getLong("calls_handled"),
                rs.getLong("calls_abandoned"),
                rs.getBigDecimal("service_level_pct"),
                rs.getBigDecimal("avg_handle_time"),
                rs.getBigDecimal("avg_talk_time"),
                rs.getBigDecimal("avg_hold_time"),
                rs.getBigDecimal("avg_wrap_time"),
                rs.getBigDecimal("avg_speed_answer"),
                rs.getBigDecimal("avg_queue_time"),
                rs.getBigDecimal("max_queue_time"),
                rs.getBigDecimal("transfer_rate"),
                rs.getBigDecimal("first_call_resolution"),
                rs.getBigDecimal("ivr_containment_rate"),
                rs.getBigDecimal("csat_score"));
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        String text = value.toString().trim();
        if (text.length() >= 10) {
            return LocalDate.parse(text.substring(0, 10));
        }
        return null;
    }

    private Integer toHour(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof java.sql.Time time) {
            return time.toLocalTime().getHour();
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().getHour();
        }
        String text = value.toString().trim();
        java.util.regex.Matcher hourOnly = java.util.regex.Pattern.compile("^\\s*(\\d{1,2})\\s*$").matcher(text);
        if (hourOnly.find()) {
            return Integer.parseInt(hourOnly.group(1));
        }
        java.util.regex.Matcher timeMatcher = java.util.regex.Pattern.compile("\\b(\\d{1,2}):\\d{2}").matcher(text);
        if (timeMatcher.find()) {
            return Integer.parseInt(timeMatcher.group(1));
        }
        java.util.regex.Matcher trailingHour = java.util.regex.Pattern.compile("(\\d{1,2})\\s*$").matcher(text);
        if (trailingHour.find()) {
            return Integer.parseInt(trailingHour.group(1));
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{1,2})").matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private AgentStat mapAgentStat(ResultSet rs, int rowNum) throws SQLException {
        return new AgentStat(
                rs.getObject("date", LocalDate.class),
                rs.getString("agent_name"),
                rs.getString("agent_id"),
                rs.getString("team"),
                rs.getString("skill_group"),
                AgentStatus.valueOf(rs.getString("status")),
                rs.getLong("calls_handled"),
                rs.getBigDecimal("avg_handle_time"),
                rs.getBigDecimal("avg_talk_time"),
                rs.getBigDecimal("avg_hold_time"),
                rs.getBigDecimal("avg_wrap_time"),
                rs.getBigDecimal("occupancy_pct"),
                rs.getBigDecimal("adherence_pct"),
                rs.getLong("transfers"),
                rs.getBigDecimal("login_duration_min"),
                rs.getBigDecimal("not_ready_time_min"));
    }

    private DroppedCallMetric mapDroppedCallMetric(ResultSet rs, int rowNum) throws SQLException {
        Timestamp lastDropTime = rs.getTimestamp("last_drop_time");
        return new DroppedCallMetric(
                rs.getObject("date", LocalDate.class),
                rs.getInt("hour"),
                rs.getString("skill_group"),
                rs.getLong("dropped_calls"),
                lastDropTime == null ? null : lastDropTime.toLocalDateTime());
    }

    private List<CallMetric> enrichCallMetricLabels(List<CallMetric> metrics) {
        Map<String, String> skills = referenceMap("aw.reference.skillMap", "t_Skill_Group", "SkillTargetID");
        return metrics.stream()
                .map(metric -> new CallMetric(
                        metric.date(),
                        metric.hour(),
                        resolveLabel(metric.skillGroup(), "SkillTarget ", skills),
                        metric.callsOffered(),
                        metric.callsHandled(),
                        metric.callsAbandoned(),
                        metric.serviceLevelPct(),
                        metric.avgHandleTime(),
                        metric.avgTalkTime(),
                        metric.avgHoldTime(),
                        metric.avgWrapTime(),
                        metric.avgSpeedAnswer(),
                        metric.avgQueueTime(),
                        metric.maxQueueTime(),
                        metric.transferRate(),
                        metric.firstCallResolution(),
                        metric.ivrContainmentRate(),
                        metric.csatScore()))
                .toList();
    }

    private List<CallTypeMetric> enrichCallTypeLabels(List<CallTypeMetric> metrics) {
        Map<String, String> skills = referenceMap("aw.reference.skillMap", "t_Skill_Group", "SkillTargetID");
        Map<String, String> callTypes = referenceMap("aw.reference.callTypeMap", "t_Call_Type", "CallTypeID");
        return metrics.stream()
                .map(metric -> new CallTypeMetric(
                        metric.date(),
                        metric.hour(),
                        resolveLabel(metric.callType(), "CallType ", callTypes),
                        resolveLabel(metric.skillGroup(), "SkillTarget ", skills),
                        metric.calls(),
                        metric.handledCalls()))
                .toList();
    }

    private List<DroppedCallMetric> enrichDroppedLabels(List<DroppedCallMetric> metrics) {
        Map<String, String> skills = referenceMap("aw.reference.skillMap", "t_Skill_Group", "SkillTargetID");
        return metrics.stream()
                .map(metric -> new DroppedCallMetric(
                        metric.date(),
                        metric.hour(),
                        resolveLabel(metric.skillGroup(), "SkillTarget ", skills),
                        metric.droppedCalls(),
                        metric.lastDropTime()))
                .toList();
    }

    private List<AgentStat> enrichAgentLabels(List<AgentStat> agents) {
        Map<String, String> skills = referenceMap("aw.reference.skillMap", "t_Skill_Group", "SkillTargetID");
        Map<String, String> agentNames = agentNameMap();
        return agents.stream()
                .map(agent -> new AgentStat(
                        agent.date(),
                        resolveMapValue(agent.agentName(), "Agent ", agentNames),
                        agent.agentId(),
                        agent.team(),
                        resolveLabel(agent.skillGroup(), "SkillTarget ", skills),
                        agent.status(),
                        agent.callsHandled(),
                        agent.avgHandleTime(),
                        agent.avgTalkTime(),
                        agent.avgHoldTime(),
                        agent.avgWrapTime(),
                        agent.occupancyPct(),
                        agent.adherencePct(),
                        agent.transfers(),
                        agent.loginDurationMin(),
                        agent.notReadyTimeMin()))
                .toList();
    }

    private List<CallMetric> filterCallMetrics(List<CallMetric> metrics, String skillGroup) {
        if (!StringUtils.hasText(skillGroup)) {
            return metrics;
        }
        return metrics.stream()
                .filter(metric -> looseMatches(metric.skillGroup(), skillGroup))
                .toList();
    }

    private List<CallTypeMetric> filterCallTypeMetrics(List<CallTypeMetric> metrics, String callType, String skillGroup) {
        return metrics.stream()
                .filter(metric -> !StringUtils.hasText(callType) || looseMatches(metric.callType(), callType))
                .filter(metric -> !StringUtils.hasText(skillGroup) || looseMatches(metric.skillGroup(), skillGroup))
                .toList();
    }

    private List<DroppedCallMetric> filterDroppedCalls(List<DroppedCallMetric> metrics, String skillGroup) {
        if (!StringUtils.hasText(skillGroup)) {
            return metrics;
        }
        return metrics.stream()
                .filter(metric -> looseMatches(metric.skillGroup(), skillGroup))
                .toList();
    }

    private boolean looseMatches(String actual, String expected) {
        if (!StringUtils.hasText(expected)) {
            return true;
        }
        if (!StringUtils.hasText(actual)) {
            return false;
        }
        String normalizedActual = normalizeFilterToken(actual);
        for (String expectedToken : expected.split("[,;]")) {
            String normalizedExpected = normalizeFilterToken(expectedToken);
            if (!normalizedExpected.isBlank()
                    && (normalizedActual.equals(normalizedExpected)
                    || normalizedActual.contains(normalizedExpected)
                    || normalizedExpected.contains(normalizedActual))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeFilterToken(String value) {
        return value == null ? "" : value
                .replace("SkillTarget ", "")
                .replace("CallType ", "")
                .replaceAll("[^A-Za-z0-9]+", "")
                .toUpperCase();
    }

    private List<AgentStat> aggregateAgents(List<AgentStat> agents) {
        Map<String, AgentStat> grouped = new LinkedHashMap<>();
        for (AgentStat agent : agents) {
            String key = agent.agentId() == null ? agent.agentName() : agent.agentId();
            AgentStat existing = grouped.get(key);
            if (existing == null) {
                grouped.put(key, agent);
            } else {
                grouped.put(key, new AgentStat(
                        existing.date(),
                        existing.agentName(),
                        existing.agentId(),
                        firstText(existing.team(), agent.team()),
                        firstText(existing.skillGroup(), agent.skillGroup()),
                        existing.status(),
                        nullToZero(existing.callsHandled()) + nullToZero(agent.callsHandled()),
                        existing.avgHandleTime(),
                        existing.avgTalkTime(),
                        existing.avgHoldTime(),
                        existing.avgWrapTime(),
                        existing.occupancyPct(),
                        existing.adherencePct(),
                        nullToZero(existing.transfers()) + nullToZero(agent.transfers()),
                        existing.loginDurationMin(),
                        existing.notReadyTimeMin()));
            }
        }
        return new ArrayList<>(grouped.values());
    }

    private List<AgentStat> mergeRosterWithActivity(List<AgentStat> roster, List<AgentStat> activity) {
        Map<String, AgentStat> merged = new LinkedHashMap<>();
        for (AgentStat agent : roster) {
            merged.put(agentKey(agent), agent);
        }
        for (AgentStat active : activity) {
            String key = agentKey(active);
            AgentStat base = merged.get(key);
            if (base == null) {
                log.debug("agent_activity_without_roster_match ignored agentId={} agentName={}",
                        active.agentId(), active.agentName());
            } else {
                merged.put(key, new AgentStat(
                        active.date() == null ? base.date() : active.date(),
                        firstText(active.agentName(), base.agentName()),
                        firstText(active.agentId(), base.agentId()),
                        firstText(active.team(), base.team()),
                        firstText(active.skillGroup(), base.skillGroup()),
                        active.status(),
                        active.callsHandled(),
                        active.avgHandleTime(),
                        active.avgTalkTime(),
                        active.avgHoldTime(),
                        active.avgWrapTime(),
                        active.occupancyPct(),
                        active.adherencePct(),
                        active.transfers(),
                        active.loginDurationMin(),
                        active.notReadyTimeMin()));
            }
        }
        return new ArrayList<>(merged.values());
    }

    private String agentKey(AgentStat agent) {
        return firstText(agent.agentId(), agent.agentName());
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) && !"UNMAPPED".equalsIgnoreCase(first) ? first : second;
    }

    private Map<String, String> referenceMap(String queryName, String table, String idColumn) {
        try {
            return timedQuery(queryName, () -> awJdbcTemplate.query(
                    "SELECT CAST(" + idColumn + " AS varchar(50)) AS id, EnterpriseName AS label FROM " + table + " WHERE EnterpriseName IS NOT NULL",
                    rs -> {
                        Map<String, String> values = new HashMap<>();
                        while (rs.next()) {
                            values.put(rs.getString("id"), rs.getString("label"));
                        }
                        return values;
                    }));
        } catch (DataAccessException ex) {
            log.warn("reference_map_unavailable table={} error={}", table, ex.getMostSpecificCause().getMessage());
            return Map.of();
        }
    }

    private Map<String, String> agentNameMap() {
        try {
            return timedQuery("aw.reference.agentNameMap", () -> awJdbcTemplate.query("""
                    SELECT
                        CAST(a.SkillTargetID AS varchar(50)) AS id,
                        COALESCE(p.FirstName + ' ' + p.LastName, p.LoginName, CAST(a.SkillTargetID AS varchar(50))) AS label
                    FROM t_Agent a
                    LEFT JOIN t_Person p ON p.PersonID = a.PersonID
                    """, rs -> {
                Map<String, String> values = new HashMap<>();
                while (rs.next()) {
                    values.put(rs.getString("id"), rs.getString("label"));
                }
                return values;
            }));
        } catch (DataAccessException ex) {
            log.warn("agent_name_map_unavailable error={}", ex.getMostSpecificCause().getMessage());
            return Map.of();
        }
    }

    private String resolveLabel(String value, String prefix, Map<String, String> labels) {
        if (value == null) {
            return null;
        }
        if (value.startsWith(prefix)) {
            return labels.getOrDefault(value.substring(prefix.length()).trim(), value);
        }
        return labels.getOrDefault(value, value);
    }

    private String resolveMapValue(String value, String prefix, Map<String, String> labels) {
        return resolveLabel(value, prefix, labels);
    }

    private LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private LocalDateTime start(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime exclusiveEnd(LocalDate date) {
        return date.plusDays(1).atStartOfDay();
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to date must be on or after from date");
        }
    }

    private long nullToZero(Long value) {
        return value == null ? 0 : value;
    }

    private <T> T timedQuery(String name, Supplier<T> supplier) {
        long start = System.nanoTime();
        try {
            T result = supplier.get();
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            if (elapsedMs >= pcceProperties.getPerformance().getSlowQueryWarningMs()) {
                log.warn("slow_query name={} elapsedMs={}", name, elapsedMs);
            } else {
                log.info("query name={} elapsedMs={}", name, elapsedMs);
            }
            queryPerformanceService.record(name, elapsedMs, true, null);
            return result;
        } catch (RuntimeException ex) {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log.error("query_failed name={} elapsedMs={} error={}", name, elapsedMs, ex.getMessage());
            queryPerformanceService.record(name, elapsedMs, false, ex.getMessage());
            throw ex;
        }
    }

    private BigDecimal weightedAverage(List<CallMetric> calls, long denominator, MetricValue value) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = calls.stream()
                .map(metric -> value.extract(metric).multiply(BigDecimal.valueOf(value.weight(metric))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(denominator), 2, java.math.RoundingMode.HALF_UP);
    }

    private enum MetricValue {
        SERVICE_LEVEL {
            @Override
            BigDecimal extract(CallMetric metric) {
                return metric.serviceLevelPct() == null ? BigDecimal.ZERO : metric.serviceLevelPct();
            }

            @Override
            long weight(CallMetric metric) {
                return metric.callsOffered() == null ? 0 : metric.callsOffered();
            }
        },
        AHT {
            @Override
            BigDecimal extract(CallMetric metric) {
                return metric.avgHandleTime() == null ? BigDecimal.ZERO : metric.avgHandleTime();
            }
        },
        ASA {
            @Override
            BigDecimal extract(CallMetric metric) {
                return metric.avgSpeedAnswer() == null ? BigDecimal.ZERO : metric.avgSpeedAnswer();
            }
        };

        abstract BigDecimal extract(CallMetric metric);

        long weight(CallMetric metric) {
            return metric.callsHandled() == null ? 0 : metric.callsHandled();
        }
    }

    public record IvrContainmentMetric(LocalDate date, Integer hour, BigDecimal ivrContainmentRate) {
    }
}
