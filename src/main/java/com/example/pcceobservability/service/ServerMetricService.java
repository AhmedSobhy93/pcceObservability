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
            host = defaultHost(target);
        }
        if (!StringUtils.hasText(host)) {
            return new ServerMetric(target.getName(), displayName(target), target.getSide(), target.getSite(), target.getTier(),
                    "", "NONE", "UNAVAILABLE",
                    null, null, null, "", "No host configured for this component", Instant.now());
        }
        if (!isLocalHost(host)) {
            return new ServerMetric(target.getName(), displayName(target), target.getSide(), target.getSite(), target.getTier(),
                    host, "SNMP/WMI/AGENT", "NOT_CONFIGURED",
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
        return new ServerMetric(target.getName(), displayName(target), target.getSide(), target.getSite(), target.getTier(),
                host, "LOCAL_JVM", "UP",
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
            case ICM_Distributor_AW -> "Cisco AW/Distributor, config and admin services";
            case HDS -> "Historical Data Server, SQL Server reporting data";
            case Live_Data -> "Cisco Live Data service";
            case CVP_CallServer -> "Cisco CVP Call Server, VXML/Tomcat";
            case CVP_ReportingServer -> "Informix, CVP Reporting";
            case CVP_VXMLServer -> "CVP VXML Server, IVR applications";
            case CVP_OAMP -> "CVP Operations Console";
            case VVB -> "Cisco Virtualized Voice Browser";
            case CTI_Server -> "CTI Server, PG services";
            case PG_CUCM, PG_CVP, PG_ECE -> "Peripheral Gateway services";
            case Finesse -> "Cisco Finesse, Tomcat";
            case CUIC -> "Cisco CUIC, Intelligence Center";
            case MediaSense -> "MediaSense services";
            case VoIP_Gateway -> "SIP/H.323 gateway reachability";
            case SIP_Proxy -> "SIP proxy / CUBE proxy reachability";
            case Load_Balancer -> "Load balancer VIP and pool health";
            case CUCM_Publisher, CUCM_Subscriber -> "Cisco Unified CM node";
            case IM_P -> "Cisco IM and Presence";
            case ECE -> "Cisco Enterprise Chat and Email";
            case Eleveo_QM, Eleveo_Recording, Eleveo_WFM -> "Eleveo platform services";
            case Database_SQL -> "SQL Server availability";
            case Domain_Controller -> "Active Directory domain services";
            case DNS -> "DNS service";
            case NTP -> "NTP time source";
        };
    }

    private String defaultHost(ComponentTarget target) {
        return switch (target.getName()) {
            case ICM_Router -> "vswsitrgr01";
            case ICM_Logger -> "vswsitaw01";
            case ICM_Distributor_AW, HDS, Database_SQL -> "vswsitaw01";
            case Live_Data -> "vswsitaw01";
            case CVP_CallServer -> "vswsitcvp01";
            case CVP_ReportingServer -> "vswsitcvpr01";
            case CVP_VXMLServer, CVP_OAMP -> "vswsitcvp01";
            case VVB -> "vssitvvb01";
            case CTI_Server, PG_CUCM, PG_CVP, PG_ECE -> "vswsitpg01";
            case Finesse -> "vssitfin01";
            case CUIC -> "vssitcuic01";
            case CUCM_Publisher, CUCM_Subscriber, IM_P, ECE, Eleveo_QM, Eleveo_Recording, Eleveo_WFM,
                    VoIP_Gateway, SIP_Proxy, Load_Balancer, MediaSense, Domain_Controller, DNS, NTP -> null;
        };
    }

    private String displayName(ComponentTarget target) {
        return StringUtils.hasText(target.getDisplayName()) ? target.getDisplayName() : target.getName().name();
    }
}
