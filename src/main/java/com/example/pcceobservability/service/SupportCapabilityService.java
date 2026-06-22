package com.example.pcceobservability.service;

import com.example.pcceobservability.model.ServerLogTarget;
import com.example.pcceobservability.model.SupportCapability;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SupportCapabilityService {

    public List<SupportCapability> smtpCapabilities() {
        return List.of(
                new SupportCapability("SMTP Alerts", "Send critical component/data alerts by email", "Config required", "Configure SMTP host, sender, recipients, and minimum severity"),
                new SupportCapability("SMTP Alerts", "Send daily banking operations summary", "Planned", "Use scheduled report export after CUIC mappings are finalized"),
                new SupportCapability("SMTP Alerts", "Send test email", "Config required", "Use approved SMTP relay account and test from Swagger/UI"));
    }

    public List<SupportCapability> spogCapabilities() {
        return List.of(
                new SupportCapability("Graceful Operations", "Maintenance mode and alert suppression", "Available", "Use Operations API maintenance endpoint"),
                new SupportCapability("Graceful Operations", "Graceful Spring Boot shutdown", "Admin gated", "Enable actuator shutdown only in approved maintenance window"),
                new SupportCapability("Log Collection", "Collect application logs", "Available", "Use Recent App Logs and monitoring/logs endpoint"),
                new SupportCapability("PCCE Log Monitoring", "Monitor Router/Logger/PG/CVP/Finesse/CUIC logs", "Config required", "Configure server log targets and collection method"),
                new SupportCapability("SPOG", "Single operational view across DB, APIs, components, alerts, logs", "Available", "Use System Health, Spring Boot App, PCCE Integration tabs"),
                new SupportCapability("RTMT Extension", "CPU/memory/disk/service/process counters", "Config required", "Integrate SNMP/WMI/JMX/enterprise monitoring source"));
    }

    public List<ServerLogTarget> serverLogTargets() {
        return List.of(
                new ServerLogTarget("ICM_Router", "vswsitrgr01", "Cisco ICM Router logs", "Windows share / agent / SIEM", false),
                new ServerLogTarget("ICM_Logger", "vswsitrgr01", "Cisco ICM Logger logs", "Windows share / agent / SIEM", false),
                new ServerLogTarget("PG", "vswsitpg01", "Cisco PG/CTI logs", "Windows share / agent / SIEM", false),
                new ServerLogTarget("CVP", "vswsitcvp01", "CVP Call Server logs", "SFTP / agent / SIEM", false),
                new ServerLogTarget("Finesse", "vssitfin01", "Finesse Tomcat/application logs", "SFTP / agent / SIEM", false),
                new ServerLogTarget("CUIC", "vssitcuic01", "CUIC application/reporting logs", "SFTP / agent / SIEM", false));
    }
}
