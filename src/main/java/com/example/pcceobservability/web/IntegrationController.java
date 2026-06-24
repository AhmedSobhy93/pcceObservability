package com.example.pcceobservability.web;

import com.example.pcceobservability.model.IntegrationCapability;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationController {

    @GetMapping("/capabilities")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public List<IntegrationCapability> capabilities() {
        return List.of(
                new IntegrationCapability("CUIC Stock Reports", "Historical, live and realtime reporting alignment", "AW/HDS/CUIC SQL mapping", "Framework ready", "Load exact CUIC SQL/report XML to match numbers exactly"),
                new IntegrationCapability("SNMP", "CPU, memory, disk, interface and process telemetry", "SNMP v2/v3 polling", "Config required", "Configure community/user, OIDs and server targets"),
                new IntegrationCapability("Secure JMX", "CVP JVM and service telemetry through secured JMX", "JMX RMI over Cisco secured CVP/OAMP setup", "Config required", "Configure truststore, JMX service URLs and read-only credentials"),
                new IntegrationCapability("PCCE Live Data", "Realtime agent, skill group, call type and precision queue telemetry", "CUIC Live Data API and WebSocket", "Config ready", "Configure Live Data host, token path and WebSocket port from CUIC datasource"),
                new IntegrationCapability("AppDynamics", "APM and infrastructure telemetry from installed agents on PCCE nodes", "AppDynamics Controller REST/deep links", "Config ready", "Configure controller URL, application, account and dashboard links"),
                new IntegrationCapability("PCCE Logs", "Router, Logger, PG, CTI, CVP, Finesse and CUIC log collection", "Windows share, SFTP, agent, or SIEM", "Config required", "Configure log target path and approved collection account"),
                new IntegrationCapability("Finesse", "Live agent/dialog/team monitoring", "Finesse REST API", "Framework ready", "Configure pcce.finesse base URL, credentials, user IDs and team IDs"),
                new IntegrationCapability("CUCM", "Device registration, CTI Manager, SIP trunk and call-processing status", "AXL/RIS/SNMP/syslog", "Config required", "Configure CUCM publisher/subscriber URLs and API credentials"),
                new IntegrationCapability("IM&P", "Presence, XMPP and service status", "REST/SOAP/SNMP/syslog", "Config required", "Configure IM&P nodes and monitoring credentials"),
                new IntegrationCapability("Call Flow Trace", "Readable call path across routing, CVP, queue, agent and disposition", "HDS TCD + CVP Reporting + logs", "Partial live", "Use /api/v1/calls/flow and enrich with CVP/CUCM logs when enabled"));
    }
}
