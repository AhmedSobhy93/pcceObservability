package com.cisco.cx.observability.feature.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.cisco.cx.observability.feature.components.service.ComponentStatusService;
import com.cisco.cx.observability.feature.reporting.PcceReportingProperties;
import com.cisco.cx.observability.feature.reporting.domain.CallMetric;
import com.cisco.cx.observability.feature.reporting.domain.CallTypeMetric;
import com.cisco.cx.observability.feature.monitoring.service.QueryPerformanceService;
import com.cisco.cx.observability.platform.config.PerformanceProperties;
import com.cisco.cx.observability.security.access.AccessControlService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

class ReportingServiceTest {

    @Test
    void callMetricsConvertsBlankSkillGroupFilterToNullQueryArguments() {
        JdbcTemplate hdsJdbcTemplate = mock(JdbcTemplate.class);
        PcceReportingProperties properties = properties();
        doReturn(List.of()).when(hdsJdbcTemplate).query(
                eq(properties.getCallMetrics()),
                anyCallMetricMapper(),
                any(Object[].class));

        ReportingService service = service(hdsJdbcTemplate, mock(JdbcTemplate.class), properties);

        service.callMetrics(LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-01"), "   ");

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(hdsJdbcTemplate).query(eq(properties.getCallMetrics()), anyCallMetricMapper(), args.capture());
        assertThat(args.getValue()).hasSize(5);
        assertThat(args.getValue()[2]).isNull();
        assertThat(args.getValue()[3]).isNull();
        assertThat(args.getValue()[4]).isNull();
    }

    @Test
    void callTypeMetricsPreservesNonBlankFiltersForQueryArguments() {
        JdbcTemplate hdsJdbcTemplate = mock(JdbcTemplate.class);
        JdbcTemplate awJdbcTemplate = mock(JdbcTemplate.class);
        PcceReportingProperties properties = properties();
        doReturn(List.of(new CallTypeMetric(
                LocalDate.parse("2026-06-01"),
                10,
                "Support",
                "EN_Reg_Others",
                1L,
                1L))).when(hdsJdbcTemplate).query(
                eq(properties.getCallTypeMetrics()),
                anyCallTypeMetricMapper(),
                any(Object[].class));
        doReturn(Map.of()).when(awJdbcTemplate).query(anyString(), anyReferenceMapExtractor());

        ReportingService service = service(hdsJdbcTemplate, awJdbcTemplate, properties);

        service.callTypeMetrics(
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-01"),
                "Support",
                "EN_Reg_Others");

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(hdsJdbcTemplate).query(eq(properties.getCallTypeMetrics()), anyCallTypeMetricMapper(), args.capture());
        assertThat(args.getValue()).hasSize(8);
        assertThat(args.getValue()[2]).isEqualTo("Support");
        assertThat(args.getValue()[3]).isEqualTo("Support");
        assertThat(args.getValue()[4]).isEqualTo("Support");
        assertThat(args.getValue()[5]).isEqualTo("EN_Reg_Others");
        assertThat(args.getValue()[6]).isEqualTo("EN_Reg_Others");
        assertThat(args.getValue()[7]).isEqualTo("EN_Reg_Others");
    }

    private ReportingService service(JdbcTemplate hdsJdbcTemplate, JdbcTemplate awJdbcTemplate, PcceReportingProperties properties) {
        return new ReportingService(
                awJdbcTemplate,
                hdsJdbcTemplate,
                mock(JdbcTemplate.class),
                properties,
                new PerformanceProperties(),
                mock(ComponentStatusService.class),
                mock(AccessControlService.class),
                new DispositionCodeService(),
                mock(QueryPerformanceService.class));
    }

    private PcceReportingProperties properties() {
        PcceReportingProperties properties = new PcceReportingProperties();
        properties.setCallMetricsTcdFallbackEnabled(false);
        return properties;
    }

    @SuppressWarnings("unchecked")
    private RowMapper<CallMetric> anyCallMetricMapper() {
        return any(RowMapper.class);
    }

    @SuppressWarnings("unchecked")
    private RowMapper<CallTypeMetric> anyCallTypeMetricMapper() {
        return any(RowMapper.class);
    }

    @SuppressWarnings("unchecked")
    private ResultSetExtractor<Map<String, String>> anyReferenceMapExtractor() {
        return any(ResultSetExtractor.class);
    }
}
