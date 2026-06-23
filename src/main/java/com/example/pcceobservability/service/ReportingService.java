package com.example.pcceobservability.service;

import com.example.pcceobservability.config.PcceProperties;
import com.example.pcceobservability.model.AgentStat;
import com.example.pcceobservability.model.AgentStatus;
import com.example.pcceobservability.model.CallMetric;
import com.example.pcceobservability.model.CallTypeMetric;
import com.example.pcceobservability.model.ContactCenterSummary;
import com.example.pcceobservability.model.DispositionBreakdown;
import com.example.pcceobservability.model.DroppedCallMetric;
import com.example.pcceobservability.model.ReferenceOption;
import com.example.pcceobservability.security.AccessControlService;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
            return timedQuery("hds.callMetrics.tcdFallback", () -> hdsJdbcTemplate.query(
                        pcceProperties.getQueries().getCallMetricsTcdFallback(),
                        this::mapCallMetric,
                        start(from),
                        exclusiveEnd(to),
                        normalizedSkillGroup,
                        normalizedSkillGroup,
                        normalizedSkillGroup));
        } catch (DataAccessException ex) {
            log.warn("call_metrics_tcd_fallback_unavailable error={}", ex.getMostSpecificCause().getMessage());
            return intervalMetrics;
        }
    }

    public List<AgentStat> agentStats(LocalDate from, LocalDate to, String agentId, String team) {
        validateDateRange(from, to);
        String normalizedAgentId = blankToNull(accessControlService.scopedAgentId(agentId));
        String normalizedTeam = blankToNull(accessControlService.scopedTeam(team));
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
        if (!tcdStats.isEmpty()) {
            return tcdStats;
        }
        return timedQuery("hds.agentStats.roster", () -> hdsJdbcTemplate.query(
                pcceProperties.getQueries().getAgentStats(),
                this::mapAgentStat,
                start(from),
                exclusiveEnd(to),
                normalizedAgentId,
                normalizedAgentId,
                normalizedTeam,
                normalizedTeam));
    }

    public List<CallTypeMetric> callTypeMetrics(LocalDate from, LocalDate to, String callType, String skillGroup) {
        validateDateRange(from, to);
        String normalizedCallType = blankToNull(callType);
        String normalizedSkillGroup = blankToNull(skillGroup);
        return timedQuery("hds.callTypeMetrics", () -> hdsJdbcTemplate.query(
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
    }

    public List<ReferenceOption> skillGroups() {
        return timedQuery("aw.reference.skillGroups", () -> awJdbcTemplate.query("""
                SELECT TOP 500
                    CAST(SkillTargetID AS varchar(50)) AS value,
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
                    CAST(CallTypeID AS varchar(50)) AS value,
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

    public List<DroppedCallMetric> droppedCalls(LocalDate from, LocalDate to, String skillGroup) {
        validateDateRange(from, to);
        if (!pcceProperties.getQueries().isDroppedCallsEnabled()) {
            return List.of();
        }
        String normalizedSkillGroup = blankToNull(skillGroup);
        return timedQuery("hds.droppedCalls", () -> hdsJdbcTemplate.query(
                    pcceProperties.getQueries().getDroppedCalls(),
                    this::mapDroppedCallMetric,
                    start(from),
                    exclusiveEnd(to),
                    normalizedSkillGroup,
                    normalizedSkillGroup,
                    normalizedSkillGroup));
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
                                rs.getObject("call_date", LocalDate.class),
                                rs.getInt("call_hour"),
                                rs.getBigDecimal("ivr_containment_rate")),
                        start(from),
                        exclusiveEnd(to)));
        } catch (DataAccessException ex) {
            log.warn("ivr_containment_unavailable error={}", ex.getMostSpecificCause().getMessage());
            return List.of();
        }
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
