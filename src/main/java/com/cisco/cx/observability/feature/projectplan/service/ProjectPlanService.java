package com.cisco.cx.observability.feature.projectplan.service;

import com.cisco.cx.observability.feature.projectplan.domain.ProjectTaskEntity;
import com.cisco.cx.observability.feature.projectplan.domain.ProjectTaskDto;
import com.cisco.cx.observability.feature.projectplan.domain.TopicStat;
import com.cisco.cx.observability.feature.monitoring.domain.ResourceStat;
import com.cisco.cx.observability.feature.projectplan.repository.ProjectTaskRepository;
import com.cisco.cx.observability.feature.projectplan.web.ProjectTemplateRequest;
import com.cisco.cx.observability.feature.projectplan.web.ProjectTaskUpdateRequest;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProjectPlanService {

    private final ProjectTaskRepository projectTaskRepository;

    public ProjectPlanService(ProjectTaskRepository projectTaskRepository) {
        this.projectTaskRepository = projectTaskRepository;
    }

    @PostConstruct
    void seedDefaults() {
        if (projectTaskRepository.count() > 0 && !containsLegacySamplePlan()) {
            return;
        }
        if (projectTaskRepository.count() > 0) {
            projectTaskRepository.deleteAll();
        }
        List<ProjectTaskDto> defaults = currentProgramTasks();
        for (int index = 0; index < defaults.size(); index++) {
            projectTaskRepository.save(toEntity(defaults.get(index), index + 1));
        }
    }

    private boolean containsLegacySamplePlan() {
        return entities().stream()
                .anyMatch(task -> "Upgrade PCCE from 12.5 to 12.6.2".equalsIgnoreCase(task.getTask()));
    }

    public List<ProjectTaskDto> tasks() {
        return entities().stream().map(this::toDto).toList();
    }

    public ProjectTaskDto add(ProjectTaskUpdateRequest request) {
        ProjectTaskDto created = toTask(request, new ProjectTaskDto(null, "PCCE", "New task", "MEDIUM", null,
                "PLANNED", "Unassigned", null, null, null, null, null, null, 0,
                null, null, "MEDIUM", null, null, null, null));
        ProjectTaskEntity entity = toEntity(created, nextSortOrder());
        return toDto(projectTaskRepository.save(entity));
    }

    public ProjectTaskDto updateById(long id, ProjectTaskUpdateRequest request) {
        ProjectTaskEntity entity = projectTaskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown task id: " + id));
        apply(entity, toTask(request, toDto(entity)));
        return toDto(projectTaskRepository.save(entity));
    }

    public ProjectTaskDto update(int index, ProjectTaskUpdateRequest request) {
        List<ProjectTaskEntity> entities = entities();
        if (index < 0 || index >= entities.size()) {
            throw new IllegalArgumentException("Unknown task index: " + index);
        }
        ProjectTaskEntity entity = entities.get(index);
        apply(entity, toTask(request, toDto(entity)));
        return toDto(projectTaskRepository.save(entity));
    }

    public void deleteById(long id) {
        if (!projectTaskRepository.existsById(id)) {
            throw new IllegalArgumentException("Unknown task id: " + id);
        }
        projectTaskRepository.deleteById(id);
    }

    public List<ProjectTaskDto> generateTemplate(ProjectTemplateRequest request) {
        String topic = textOr(request == null ? null : request.topic(), "Team Delivery");
        String team = textOr(request == null ? null : request.team(), topic);
        String owner = valueOr(request == null ? null : request.owner(), null);
        String resource = textOr(request == null ? null : request.resource(), owner == null ? "Unassigned" : owner);
        String start = valueOr(request == null ? null : request.start(), null);
        String finish = valueOr(request == null ? null : request.finish(), null);
        String shareWith = valueOr(request == null ? null : request.shareWith(), "Stakeholders");
        List<String> steps = templateSteps(topic);
        List<ProjectTaskDto> created = new ArrayList<>();
        for (int index = 0; index < steps.size(); index++) {
            String status = index == 0 ? "IN_PROGRESS" : "PLANNED";
            String priority = index < 2 ? "HIGH" : "MEDIUM";
            ProjectTaskDto task = new ProjectTaskDto(null, topic, steps.get(index), priority, index + 1, status,
                    resource, owner, team, milestoneFor(index), start, finish, null, index == 0 ? 10 : 0,
                    index == 0 ? null : steps.get(index - 1), null, "MEDIUM",
                    deliverableFor(steps.get(index)), shareWith, null, "Generated template for " + team);
            created.add(toDto(projectTaskRepository.save(toEntity(task, nextSortOrder()))));
        }
        return created;
    }

    public String csv() {
        StringBuilder builder = new StringBuilder("id,topic,task,priority,priority_num,status,resource,owner,team,milestone,start,finish,duration,pct,depends_on,blocked_by,risk,deliverable,share_with,external_ref,comments\n");
        for (ProjectTaskDto task : tasks()) {
            builder.append(csv(task.id())).append(',')
                    .append(csv(task.topic())).append(',')
                    .append(csv(task.task())).append(',')
                    .append(csv(task.priority())).append(',')
                    .append(csv(task.priorityNum())).append(',')
                    .append(csv(task.status())).append(',')
                    .append(csv(task.resource())).append(',')
                    .append(csv(task.owner())).append(',')
                    .append(csv(task.team())).append(',')
                    .append(csv(task.milestone())).append(',')
                    .append(csv(task.start())).append(',')
                    .append(csv(task.finish())).append(',')
                    .append(csv(task.duration())).append(',')
                    .append(csv(task.pct())).append(',')
                    .append(csv(task.dependsOn())).append(',')
                    .append(csv(task.blockedBy())).append(',')
                    .append(csv(task.risk())).append(',')
                    .append(csv(task.deliverable())).append(',')
                    .append(csv(task.shareWith())).append(',')
                    .append(csv(task.externalRef())).append(',')
                    .append(csv(task.comments())).append('\n');
        }
        return builder.toString();
    }

    public Map<String, TopicStat> topicStats() {
        List<ProjectTaskDto> tasks = tasks();
        Map<String, List<ProjectTaskDto>> grouped = new LinkedHashMap<>();
        for (ProjectTaskDto task : tasks) {
            grouped.computeIfAbsent(task.topic(), ignored -> new ArrayList<>()).add(task);
        }
        Map<String, TopicStat> stats = new LinkedHashMap<>();
        grouped.forEach((topic, topicTasks) -> stats.put(topic, stat(topicTasks)));
        return stats;
    }

    public List<ResourceStat> resourceStats() {
        List<ProjectTaskDto> tasks = tasks();
        Map<String, List<ProjectTaskDto>> grouped = new LinkedHashMap<>();
        for (ProjectTaskDto task : tasks) {
            for (String resource : resources(task.resource())) {
                grouped.computeIfAbsent(resource, ignored -> new ArrayList<>()).add(task);
            }
        }
        return grouped.entrySet().stream()
                .map(entry -> {
                    List<ProjectTaskDto> assigned = entry.getValue();
                    return new ResourceStat(
                            entry.getKey(),
                            assigned.size(),
                            count(assigned, "COMPLETED"),
                            count(assigned, "IN_PROGRESS"),
                            count(assigned, "ON_HOLD"),
                            assigned.stream().filter(task -> !"COMPLETED".equals(task.status())).toList(),
                            assigned.stream().filter(this::criticalOpen).toList());
                })
                .sorted((left, right) -> Integer.compare(right.total(), left.total()))
                .toList();
    }

    public int totalTasks() {
        return tasks().size();
    }

    public int completedCount() {
        return count(tasks(), "COMPLETED");
    }

    public int inProgressCount() {
        return count(tasks(), "IN_PROGRESS");
    }

    public int onHoldCount() {
        return count(tasks(), "ON_HOLD");
    }

    public int criticalOpenCount() {
        return (int) tasks().stream().filter(this::criticalOpen).count();
    }

    private TopicStat stat(List<ProjectTaskDto> topicTasks) {
        return new TopicStat(
                topicTasks.size(),
                count(topicTasks, "COMPLETED"),
                count(topicTasks, "IN_PROGRESS"),
                count(topicTasks, "ON_HOLD"),
                count(topicTasks, "PLANNED"),
                (int) topicTasks.stream().filter(task -> "CRITICAL".equals(task.priority())).count());
    }

    private int count(List<ProjectTaskDto> values, String status) {
        return (int) values.stream().filter(task -> status.equals(task.status())).count();
    }

    private boolean criticalOpen(ProjectTaskDto task) {
        return "CRITICAL".equals(task.priority()) && !"COMPLETED".equals(task.status());
    }

    private ProjectTaskDto toTask(ProjectTaskUpdateRequest request, ProjectTaskDto fallback) {
        if (request == null) {
            return fallback;
        }
        return new ProjectTaskDto(
                fallback.id(),
                textOr(request.topic(), fallback.topic()),
                textOr(request.task(), fallback.task()),
                textOr(request.priority(), fallback.priority()),
                request.priorityNum() == null ? fallback.priorityNum() : request.priorityNum(),
                textOr(request.status(), fallback.status()),
                textOr(request.resource(), fallback.resource()),
                valueOr(request.owner(), fallback.owner()),
                valueOr(request.team(), fallback.team()),
                valueOr(request.milestone(), fallback.milestone()),
                valueOr(request.start(), fallback.start()),
                valueOr(request.finish(), fallback.finish()),
                request.duration() == null ? fallback.duration() : request.duration(),
                clamp(request.pct() == null ? fallback.pct() : request.pct()),
                valueOr(request.dependsOn(), fallback.dependsOn()),
                valueOr(request.blockedBy(), fallback.blockedBy()),
                textOr(request.risk(), fallback.risk() == null ? "MEDIUM" : fallback.risk()),
                valueOr(request.deliverable(), fallback.deliverable()),
                valueOr(request.shareWith(), fallback.shareWith()),
                valueOr(request.externalRef(), fallback.externalRef()),
                valueOr(request.comments(), fallback.comments()));
    }

    private String textOr(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String valueOr(String value, String fallback) {
        return value == null ? fallback : value.trim();
    }

    private Integer clamp(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, value));
    }

    private List<String> resources(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of("Unassigned");
        }
        List<String> resources = new ArrayList<>();
        for (String item : value.split(",|/|&")) {
            if (StringUtils.hasText(item)) {
                resources.add(item.trim());
            }
        }
        return resources.isEmpty() ? List.of("Unassigned") : resources;
    }

    private List<ProjectTaskEntity> entities() {
        return projectTaskRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    private int nextSortOrder() {
        return entities().stream().mapToInt(ProjectTaskEntity::getSortOrder).max().orElse(0) + 1;
    }

    private ProjectTaskDto toDto(ProjectTaskEntity entity) {
        return new ProjectTaskDto(
                entity.getId(),
                entity.getTopic(),
                entity.getTask(),
                entity.getPriority(),
                entity.getPriorityNum(),
                entity.getStatus(),
                entity.getResource(),
                entity.getOwner(),
                entity.getTeam(),
                entity.getMilestone(),
                entity.getStart(),
                entity.getFinish(),
                entity.getDuration(),
                entity.getPct(),
                entity.getDependsOn(),
                entity.getBlockedBy(),
                entity.getRisk(),
                entity.getDeliverable(),
                entity.getShareWith(),
                entity.getExternalRef(),
                entity.getComments());
    }

    private ProjectTaskEntity toEntity(ProjectTaskDto task, int sortOrder) {
        ProjectTaskEntity entity = new ProjectTaskEntity();
        entity.setSortOrder(sortOrder);
        apply(entity, task);
        return entity;
    }

    private void apply(ProjectTaskEntity entity, ProjectTaskDto task) {
        entity.setTopic(task.topic());
        entity.setTask(task.task());
        entity.setPriority(task.priority());
        entity.setPriorityNum(task.priorityNum());
        entity.setStatus(task.status());
        entity.setResource(task.resource());
        entity.setOwner(task.owner());
        entity.setTeam(task.team());
        entity.setMilestone(task.milestone());
        entity.setStart(task.start());
        entity.setFinish(task.finish());
        entity.setDuration(task.duration());
        entity.setPct(clamp(task.pct()));
        entity.setDependsOn(task.dependsOn());
        entity.setBlockedBy(task.blockedBy());
        entity.setRisk(task.risk());
        entity.setDeliverable(task.deliverable());
        entity.setShareWith(task.shareWith());
        entity.setExternalRef(task.externalRef());
        entity.setComments(task.comments());
        entity.setUpdatedAt(java.time.LocalDateTime.now());
    }

    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private List<String> templateSteps(String topic) {
        String normalized = topic == null ? "" : topic.toLowerCase();
        if (normalized.contains("security")) {
            return List.of("Define security scope and owners", "Review access and hardening baseline",
                    "Validate certificates, secrets and audit controls", "Run UAT and risk acceptance",
                    "Prepare production change and rollback plan");
        }
        if (normalized.contains("network")) {
            return List.of("Confirm topology and firewall matrix", "Validate routing, VIPs and DNS",
                    "Test connectivity and failover paths", "Document monitoring ports and probes",
                    "Production readiness sign-off");
        }
        if (normalized.contains("report") || normalized.contains("cuic")) {
            return List.of("Confirm stock report mapping", "Validate AW/HDS/CVP query alignment",
                    "Build dashboards and filters", "Reconcile numbers with business owners",
                    "Publish stakeholder report pack");
        }
        if (normalized.contains("appd") || normalized.contains("monitor")) {
            return List.of("Confirm monitoring scope and node inventory", "Validate agent/controller connectivity",
                    "Build dashboard links and alert policies", "Run incident simulation",
                    "Operational handover and support guide");
        }
        return List.of("Confirm scope and stakeholders", "Define work packages and dependencies",
                "Assign resources and delivery owners", "Run UAT and readiness review",
                "Production rollout and handover");
    }

    private String milestoneFor(int index) {
        return switch (index) {
            case 0 -> "Scope";
            case 1 -> "Design";
            case 2 -> "Build";
            case 3 -> "Validation";
            default -> "Handover";
        };
    }

    private String deliverableFor(String task) {
        return task + " completed, documented and accepted by owner.";
    }

    private static List<ProjectTaskDto> currentProgramTasks() {
        return List.of(
                task("PCCE", "PCCE SSO", "MEDIUM", 6, "IN_PROGRESS", "Ayman", "4-Feb-26", "30-Jun-26", 30, null),
                task("PCCE", "Monitoring: Splunk reports, SCOM, AppDynamics, network and RTMT", "MEDIUM", 4, "IN_PROGRESS", "Ayman & Fawzi", "27-Jan-26", "2-Feb-26", 7, "Support Team complete SMTP prerequisites"),
                task("PCCE", "Finesse, CUIC Pen Testing + VAPT", "HIGH", 12, "IN_PROGRESS", "Ayman & InfoSec", "19-Jan-26", "30-Jun-26", 4, "Pending InfoSec to rescan"),
                task("PCCE", "Migration Planning and sizing", "MEDIUM", 7, "IN_PROGRESS", "Ayman", "5-Feb-26", "24-Feb-26", 20, null),
                task("PCCE", "Fraud Outbound Announcement (Dialer)", "CRITICAL", 11, "IN_PROGRESS", "Ayman", "4-Feb-26", "30-Apr-26", 10, "Done SIT, expected to be deployed UAT 21-Jun. Manual Outbound not working now"),
                task("PCCE", "Management Dashboard", "LOW", null, "IN_PROGRESS", "Sobhy", null, null, null, null),
                task("PCCE", "CCB SIT/UAT issue", "LOW", null, "IN_PROGRESS", "Ayman", null, null, null, null),
                task("Eleveo", "Sentiment analysis installation", "CRITICAL", null, "IN_PROGRESS", "Ayman, Sobhy", null, null, 7, null),
                task("Eleveo", "Check Eleveo new features", "MEDIUM", 9, "ON_HOLD", "Ayman", "3-Feb-26", "11-Feb-26", 7, null),
                task("Eleveo", "Check Eleveo APIs", "LOW", null, "ON_HOLD", "Fawzi", null, null, 5, null),
                task("Eleveo", "Support Team Admin Account Usage", "LOW", null, "ON_HOLD", "Ayman", null, null, null, null),
                task("Eleveo", "Datalake integration", "HIGH", 10, "COMPLETED", "Ayman & Data Team", "25-Jan-26", "25-Jan-26", 5, "Configuration done from my side, pending Data Team"),
                task("Dtech", "WE SMS GW Configs", "MEDIUM", 4, "COMPLETED", "Fawzi", "15-Feb-26", "23-Feb-26", 7, null),
                task("Dtech", "Email GW and SMTP Integration", "MEDIUM", 5, "IN_PROGRESS", "Fawzi", "15-Feb-26", "25-Feb-26", 10, "Pending design"),
                task("Dtech", "New features including opened points, topics, and Huawei", "MEDIUM", 7, "IN_PROGRESS", "Fawzi", "8-Feb-26", "5-Mar-26", 30, "Huawei pending installation"),
                task("Dtech", "Versioning and deployments", "MEDIUM", 6, "COMPLETED", "Fawzi", "8-Feb-26", "19-Feb-26", 10, "Pending preparation"),
                task("Dtech", "Content Changes Best Way: Portal and ETCS", "CRITICAL", 3, "COMPLETED", "Fawzi", "25-Jan-26", "26-Feb-26", 30, "Pending Testing Team"),
                task("Dtech", "New portal and close old one", "MEDIUM", 8, "COMPLETED", "Fawzi & Ayman", "15-Feb-26", null, 10, null),
                task("Dtech", "Docs", "LOW", 9, "IN_PROGRESS", "Fawzi", "15-Feb-26", null, 10, null),
                task("Dtech", "Certs and health check", "LOW", null, "IN_PROGRESS", "Fawzi", null, null, 5, "Pending SIT, UAT is done"),
                task("Dtech", "Dtech DevOps pipeline creation", "LOW", null, "PLANNED", "Fawzi", null, null, null, null),
                task("Dtech", "Audit enhancements: admin user, report cleanup, split password, subscriber update dimmed", "LOW", null, "IN_PROGRESS", "Fawzi", null, null, null, null),
                task("Dtech", "Prod cleansing", "CRITICAL", 3, "COMPLETED", "Fawzi", null, null, null, "Pending Support team"),
                task("Dtech", "Dtech PenTesting", "MEDIUM", null, "PLANNED", "Fawzi", null, null, null, null),
                task("Dtech", "LDAP integration", "LOW", null, "IN_PROGRESS", "Fawzi", null, null, null, null),
                task("Dtech", "Customer preferences fatigue", "HIGH", null, "PLANNED", "Fawzi", null, null, null, null),
                task("Cisco Portal", "Maker and checker", "MEDIUM", null, "ON_HOLD", "Fawzi", null, null, 4, null),
                task("Cisco Portal", "Agent Auto Creation", "HIGH", null, "ON_HOLD", "Fawzi", null, null, null, null),
                task("Cisco Portal", "Monitoring", "MEDIUM", null, "ON_HOLD", "Fawzi", null, null, 5, null),
                task("Cisco Portal", "Audit", "MEDIUM", null, "ON_HOLD", "Fawzi", null, null, 5, null),
                task("Cisco Portal", "Deployments", "MEDIUM", null, "COMPLETED", "Fawzi", null, null, null, "Fawzi to follow up with Heba on Jenkins Prod looping Kadry"),
                task("Cisco Portal", "Agent Scheduling", "MEDIUM", null, "ON_HOLD", "Fawzi", null, null, null, null),
                task("Cisco Portal", "Managing IVR products and offers prompts from customer portal", "LOW", null, "ON_HOLD", "Fawzi", null, null, null, null),
                task("Cisco Portal", "Security Scanning", "LOW", null, "ON_HOLD", "Fawzi", null, null, 10, null),
                task("Survey", "Phase 2 Development", "HIGH", null, "ON_HOLD", "Fawzi", null, null, 60, null),
                task("Survey", "UI Enhancements", "MEDIUM", 10, "COMPLETED", "Fawzi", "8-Feb-26", null, 15, null),
                task("Survey", "OpenShift SIT/UAT/Prod/DR Deployment", "HIGH", 11, "IN_PROGRESS", "Fawzi", "8-Feb-26", null, 15, "SIT Done, UAT Done, Prod-DR in progress"),
                task("Survey", "DevOps", "HIGH", null, "COMPLETED", "Fawzi", null, null, 4, "SIT Done, UAT in progress, Prod-DR planning"),
                task("Survey", "Monitoring", "MEDIUM", null, "ON_HOLD", "Fawzi", null, null, 10, "Pending meeting with Kadry/Support team"),
                task("Survey", "Performance Test", "HIGH", 12, "IN_PROGRESS", "Fawzi", "5-Feb-26", null, 10, "SIT done, UAT expected to be better with Redis caching"),
                task("Survey", "Datalake sync", "MEDIUM", 13, "IN_PROGRESS", "Fawzi", null, null, 7, "Pending Data Team"),
                task("Survey", "New APIs for getting surveys by user and action IDs", "HIGH", 14, "COMPLETED", "Fawzi", "4-Feb-26", "5-Feb-26", 1, null),
                task("Survey", "API customization for Mobile Journey", "LOW", 1, "COMPLETED", "Fawzi", null, null, null, null),
                task("Survey", "Security Scanning", "HIGH", null, "ON_HOLD", "Fawzi", null, null, 10, null),
                task("Chat", "Deployments SIT/UAT/Prod/DR", "CRITICAL", 1, "COMPLETED", "Fawzi", "Dec-25", "28-May-26", null, "SIT/UAT done - Prod/DR sanity"),
                task("Chat", "SIT Cycle", "HIGH", 2, "COMPLETED", "Fawzi", "26-Jan", "26-Feb", null, null),
                task("Chat", "UAT/Prod/DR Sanity", "HIGH", 2, "COMPLETED", "Fawzi", null, null, null, "UAT done"),
                task("Chat", "PenTesting fixes and VAPT on the new version", "MEDIUM", 3, "IN_PROGRESS", "Vendor", null, null, null, "Pending Aya Wanis to share new findings"),
                task("Chat", "New PenTesting Cycle", "HIGH", 2, "COMPLETED", "Security, Vendor", null, null, null, null),
                task("Chat", "Infrastructure and logging enhancement", "MEDIUM", 3, "ON_HOLD", "Vendor", null, null, null, null),
                task("Chat", "Opened Bugs: EE-1848", "CRITICAL", 3, "IN_PROGRESS", "Vendor", null, null, null, null),
                task("Chat", "Confirmation on APIs Checklist", "LOW", 4, "COMPLETED", "Vendor", null, null, null, null),
                task("Chat", "Performance Test", "HIGH", 2, "IN_PROGRESS", "Testing team, Fawzi", null, null, null, "Pending Testing Team"),
                task("Chat", "Chat Encryption/Decryption", "CRITICAL", null, "IN_PROGRESS", "Unassigned", null, null, null, null),
                task("Chat", "Agent Availability", "LOW", null, "ON_HOLD", "Unassigned", null, null, null, "Pending deployment"),
                task("Chat", "Data Cleansing", "CRITICAL", 3, "IN_PROGRESS", "Unassigned", null, null, null, null),
                task("Chat", "Vendor handover and docs", "CRITICAL", null, "ON_HOLD", "Vendor", null, null, null, null),
                task("Chat", "User Management/Sync LDAP integration", "HIGH", null, "ON_HOLD", "Unassigned", null, null, null, null),
                task("Chat", "Pipeline", "MEDIUM", 3, "PLANNED", "Fawzi, DevOps", null, null, null, null),
                task("One Content", "Deployment SIT/UAT/Prod/DR Deployment", "HIGH", 11, "IN_PROGRESS", "Fawzi", "8-Feb-26", null, 15, "SIT Done, UAT Done, Prod-DR in progress"),
                task("One Content", "DevOps", "HIGH", null, "COMPLETED", "Fawzi", null, null, 4, "SIT Done, UAT in progress, Prod-DR planning"),
                task("One Content", "Monitoring", "MEDIUM", null, "ON_HOLD", "Fawzi", null, null, 10, "Pending meeting with Kadry/Support team"),
                task("One Content", "Performance Test", "HIGH", 12, "IN_PROGRESS", "Fawzi", "5-Feb-26", null, 10, "SIT done, UAT expected to be better with Redis caching"),
                task("One Content", "Datalake sync", "MEDIUM", 13, "IN_PROGRESS", "Fawzi", null, null, 7, null));
    }

    private static ProjectTaskDto task(String topic, String task, String priority, Integer priorityNum, String status,
                                       String resource, String start, String finish, Integer duration, String comments) {
        int pct = switch (status) {
            case "COMPLETED" -> 100;
            case "IN_PROGRESS" -> 50;
            default -> 0;
        };
        String owner = firstResource(resource);
        return new ProjectTaskDto(null, topic, task, priority, priorityNum, status, resource,
                owner, topic, status, start, finish, duration, pct, null, null,
                "CRITICAL".equals(priority) ? "HIGH" : "MEDIUM",
                null, "Stakeholders, delivery team, managers", null, comments);
    }

    private static String firstResource(String resource) {
        if (!StringUtils.hasText(resource) || "Unassigned".equalsIgnoreCase(resource)) {
            return null;
        }
        return resource.split(",|/|&")[0].trim();
    }
}
