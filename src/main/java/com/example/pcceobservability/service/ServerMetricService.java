package com.example.pcceobservability.service;

import com.example.pcceobservability.config.PcceProperties;
import com.example.pcceobservability.config.PcceProperties.ComponentTarget;
import com.example.pcceobservability.model.ServerMetric;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ServerMetricService {

    private final PcceProperties pcceProperties;

    public ServerMetricService(PcceProperties pcceProperties) {
        this.pcceProperties = pcceProperties;
    }

    public List<ServerMetric> metrics() {
        return pcceProperties.getComponents().stream()
                .map(this::metric)
                .toList();
    }

    private ServerMetric metric(ComponentTarget target) {
        String host = StringUtils.hasText(target.getHost()) ? target.getHost() : hostFromUrl(target.getUrl());
        if (!StringUtils.hasText(host)) {
            return new ServerMetric(target.getName(), "", "NONE", "UNAVAILABLE",
                    null, null, null, "", "No host configured for this component", Instant.now());
        }
        if (!isLocalHost(host)) {
            return new ServerMetric(target.getName(), host, "SNMP/WMI/AGENT", "NOT_CONFIGURED",
                    null, null, null, serviceHint(target),
                    "Remote CPU, memory, disk and Windows service status require SNMP, WMI/WinRM, or an installed exporter/agent.",
                    Instant.now());
        }
        return localMetric(target, host);
    }

    private ServerMetric localMetric(ComponentTarget target, String host) {
        java.lang.management.OperatingSystemMXBean base = ManagementFactory.getOperatingSystemMXBean();
        Double cpu = null;
        Double memory = null;
        if (base instanceof com.sun.management.OperatingSystemMXBean os) {
            double cpuLoad = os.getCpuLoad();
            cpu = cpuLoad < 0 ? null : cpuLoad * 100;
            long total = os.getTotalMemorySize();
            long free = os.getFreeMemorySize();
            memory = total <= 0 ? null : ((double) (total - free) / total) * 100;
        }
        double disk = diskUsedPct();
        return new ServerMetric(target.getName(), host, "LOCAL_JVM", "UP",
                cpu, memory, disk, serviceHint(target), "Local host metrics from JVM OS bean", Instant.now());
    }

    private double diskUsedPct() {
        long total = 0;
        long free = 0;
        for (File root : File.listRoots()) {
            total += root.getTotalSpace();
            free += root.getFreeSpace();
        }
        return total <= 0 ? 0 : ((double) (total - free) / total) * 100;
    }

    private boolean isLocalHost(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.equals(InetAddress.getLocalHost());
        } catch (Exception ex) {
            return false;
        }
    }

    private String hostFromUrl(String url) {
        try {
            return StringUtils.hasText(url) ? java.net.URI.create(url).getHost() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String serviceHint(ComponentTarget target) {
        return switch (target.getName()) {
            case ICM_Router -> "Cisco ICM Router, Distributor";
            case ICM_Logger -> "Cisco ICM Logger, SQL Server";
            case CVP_CallServer -> "Cisco CVP Call Server, VXML/Tomcat";
            case CVP_ReportingServer -> "Informix, CVP Reporting";
            case CTI_Server -> "CTI Server, PG services";
            case PG_CUCM, PG_CVP -> "Peripheral Gateway services";
            case Finesse -> "Cisco Finesse, Tomcat";
            case CUIC -> "Cisco CUIC, Intelligence Center";
            case MediaSense -> "MediaSense services";
            case VoIP_Gateway -> "SIP/H.323 gateway reachability";
        };
    }
}
