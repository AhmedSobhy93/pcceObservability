package com.example.pcceobservability.service;

import com.example.pcceobservability.entity.ProjectTaskEntity;
import com.example.pcceobservability.model.ProjectTaskDto;
import com.example.pcceobservability.model.ProjectTaskUpdateRequest;
import com.example.pcceobservability.model.ResourceStat;
import com.example.pcceobservability.model.TopicStat;
import com.example.pcceobservability.repository.ProjectTaskRepository;
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
        if (projectTaskRepository.count() > 0) {
            return;
        }
        List<ProjectTaskDto> defaults = defaultTasks();
        for (int index = 0; index < defaults.size(); index++) {
            projectTaskRepository.save(toEntity(defaults.get(index), index + 1));
        }
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

    private static List<ProjectTaskDto> defaultTasks() {
        return List.of(
                new ProjectTaskDto("PCCE", "Upgrade PCCE from 12.5 to 12.6.2", "CRITICAL", 1, "IN_PROGRESS", "Ahmed", "1-Feb-26", "30-Apr-26", 88, 60, "Underway - lab done, prod scheduled"),
                new ProjectTaskDto("PCCE", "CVP upgrade & IVR script migration", "CRITICAL", 2, "IN_PROGRESS", "Ahmed / Khalid", "1-Mar-26", "31-May-26", 91, 40, "Dependent on PCCE upgrade"),
                new ProjectTaskDto("PCCE", "Finesse desktop customisation", "HIGH", 3, "PLANNED", "Khalid", "1-Apr-26", "30-Jun-26", 91, 0, "Awaiting CVP sign-off"),
                new ProjectTaskDto("PCCE", "CUIC report migration", "HIGH", 4, "IN_PROGRESS", "Sara", "1-Feb-26", "31-Mar-26", 59, 70, "Legacy reports mapped"),
                new ProjectTaskDto("PCCE", "Agent PG failover testing", "CRITICAL", 5, "ON_HOLD", "Ahmed", "1-Apr-26", "30-Apr-26", 30, 0, "Blocked - network team"),
                new ProjectTaskDto("PCCE", "Rogger/Logger redundancy validation", "HIGH", 6, "PLANNED", "Ahmed", "1-May-26", "31-May-26", 31, 0, "Post upgrade"),
                new ProjectTaskDto("PCCE", "SIP trunk cut-over", "CRITICAL", 7, "ON_HOLD", "Khalid / Telecom", "1-Apr-26", "30-Apr-26", 30, 0, "Carrier config pending"),
                new ProjectTaskDto("PCCE", "CCE Admin security hardening", "MEDIUM", 8, "PLANNED", "Sara", "1-May-26", "30-Jun-26", 61, 0, null),
                new ProjectTaskDto("PCCE", "DR site replication setup", "HIGH", 9, "PLANNED", "Ahmed", "1-Jun-26", "31-Jul-26", 61, 0, null),
                new ProjectTaskDto("PCCE", "Post-upgrade health dashboard", "MEDIUM", 10, "IN_PROGRESS", "Sara", "1-Mar-26", "30-Apr-26", 61, 50, "Base44 dashboard wired"),
                new ProjectTaskDto("Eleveo", "Deploy Eleveo WFM 6.x", "CRITICAL", 1, "IN_PROGRESS", "Noura", "1-Jan-26", "31-Mar-26", 89, 75, "Server provisioned"),
                new ProjectTaskDto("Eleveo", "Integrate Eleveo with PCCE ACD", "CRITICAL", 2, "IN_PROGRESS", "Noura / Ahmed", "1-Feb-26", "30-Apr-26", 88, 50, "API mapping in progress"),
                new ProjectTaskDto("Eleveo", "Agent scheduling go-live", "HIGH", 3, "PLANNED", "Noura", "1-May-26", "31-May-26", 31, 0, "Depends on integration"),
                new ProjectTaskDto("Eleveo", "Adherence real-time feed", "HIGH", 4, "PLANNED", "Noura", "1-May-26", "30-Jun-26", 61, 0, null),
                new ProjectTaskDto("Eleveo", "Forecasting model setup", "MEDIUM", 5, "PLANNED", "Noura", "1-Jun-26", "31-Jul-26", 61, 0, null),
                new ProjectTaskDto("Eleveo", "Recording compliance config", "HIGH", 6, "IN_PROGRESS", "Khalid", "1-Feb-26", "31-Mar-26", 59, 60, "PCI rules applied"),
                new ProjectTaskDto("Eleveo", "QM evaluation forms", "MEDIUM", 7, "PLANNED", "Sara", "1-Apr-26", "30-Jun-26", 91, 0, null),
                new ProjectTaskDto("Eleveo", "UAT & training", "HIGH", 8, "PLANNED", "Noura / Sara", "1-Jun-26", "31-Jul-26", 61, 0, null),
                new ProjectTaskDto("Dtech", "Dtech CTI middleware upgrade", "CRITICAL", 1, "IN_PROGRESS", "Rami", "1-Jan-26", "28-Feb-26", 59, 80, "UAT phase"),
                new ProjectTaskDto("Dtech", "Screen-pop integration with Finesse", "HIGH", 2, "IN_PROGRESS", "Rami", "1-Feb-26", "31-Mar-26", 58, 60, "JS gadget built"),
                new ProjectTaskDto("Dtech", "CRM data connector", "HIGH", 3, "PLANNED", "Rami / Sara", "1-Mar-26", "30-Apr-26", 61, 0, "CRM creds awaited"),
                new ProjectTaskDto("Dtech", "Reporting API for Dtech", "MEDIUM", 4, "PLANNED", "Sara", "1-Apr-26", "31-May-26", 61, 0, null),
                new ProjectTaskDto("Dtech", "Dtech load testing", "HIGH", 5, "PLANNED", "Rami", "1-May-26", "31-May-26", 31, 0, null),
                new ProjectTaskDto("Dtech", "Production cutover", "CRITICAL", 6, "PLANNED", "Rami / Ahmed", "1-Jun-26", "15-Jun-26", 15, 0, "Go/no-go gate"),
                new ProjectTaskDto("Cisco Portal", "Self-service portal deployment", "HIGH", 1, "IN_PROGRESS", "Lina", "1-Feb-26", "30-Apr-26", 88, 55, "UX review done"),
                new ProjectTaskDto("Cisco Portal", "SSO / AD integration", "CRITICAL", 2, "IN_PROGRESS", "Lina / Ahmed", "1-Feb-26", "31-Mar-26", 58, 70, "SAML config in test"),
                new ProjectTaskDto("Cisco Portal", "Knowledge base migration", "MEDIUM", 3, "IN_PROGRESS", "Lina", "1-Mar-26", "30-Apr-26", 61, 40, "Content imported"),
                new ProjectTaskDto("Cisco Portal", "Ticket routing rules", "HIGH", 4, "PLANNED", "Lina", "1-Apr-26", "31-May-26", 61, 0, null),
                new ProjectTaskDto("Cisco Portal", "Portal UAT", "HIGH", 5, "PLANNED", "Lina / Sara", "1-May-26", "31-May-26", 31, 0, null),
                new ProjectTaskDto("Cisco Portal", "Go-live & hypercare", "HIGH", 6, "PLANNED", "Lina", "1-Jun-26", "15-Jun-26", 15, 0, null),
                new ProjectTaskDto("Survey", "Post-call IVR survey design", "HIGH", 1, "COMPLETED", "Sara", "1-Jan-26", "28-Feb-26", 59, 100, "Live"),
                new ProjectTaskDto("Survey", "SMS survey integration", "HIGH", 2, "IN_PROGRESS", "Sara / Khalid", "1-Feb-26", "31-Mar-26", 58, 65, "API connected"),
                new ProjectTaskDto("Survey", "CSAT dashboard", "MEDIUM", 3, "IN_PROGRESS", "Sara", "1-Mar-26", "30-Apr-26", 61, 50, "Charts built"),
                new ProjectTaskDto("Survey", "NPS reporting", "MEDIUM", 4, "PLANNED", "Sara", "1-Apr-26", "31-May-26", 61, 0, null),
                new ProjectTaskDto("Survey", "Survey A/B testing", "LOW", 5, "PLANNED", "Sara", "1-May-26", "30-Jun-26", 61, 0, null),
                new ProjectTaskDto("Chat", "Cisco ECE chat channel setup", "CRITICAL", 1, "IN_PROGRESS", "Khalid", "1-Jan-26", "31-Mar-26", 89, 70, "Routing rules done"),
                new ProjectTaskDto("Chat", "Chatbot NLU training", "HIGH", 2, "IN_PROGRESS", "Khalid / Noura", "1-Feb-26", "30-Apr-26", 88, 45, "Intents mapped"),
                new ProjectTaskDto("Chat", "Chat-to-voice escalation", "HIGH", 3, "PLANNED", "Khalid", "1-Apr-26", "31-May-26", 61, 0, null),
                new ProjectTaskDto("Chat", "Agent chat UI customisation", "MEDIUM", 4, "PLANNED", "Khalid", "1-Apr-26", "30-Jun-26", 91, 0, null),
                new ProjectTaskDto("Chat", "Chat reporting in CUIC", "MEDIUM", 5, "PLANNED", "Sara", "1-May-26", "30-Jun-26", 61, 0, null),
                new ProjectTaskDto("Chat", "Load & failover testing", "HIGH", 6, "PLANNED", "Khalid / Ahmed", "1-Jun-26", "30-Jun-26", 30, 0, null),
                new ProjectTaskDto("One Content", "Content platform assessment", "HIGH", 1, "COMPLETED", "Lina", "1-Jan-26", "31-Jan-26", 31, 100, "Done"),
                new ProjectTaskDto("One Content", "Knowledge article migration", "HIGH", 2, "IN_PROGRESS", "Lina", "1-Feb-26", "31-Mar-26", 58, 70, "800/1200 articles done"),
                new ProjectTaskDto("One Content", "Search & tagging taxonomy", "MEDIUM", 3, "IN_PROGRESS", "Lina", "1-Mar-26", "30-Apr-26", 61, 40, null),
                new ProjectTaskDto("One Content", "Agent-facing KB widget", "HIGH", 4, "PLANNED", "Lina / Rami", "1-Apr-26", "31-May-26", 61, 0, "Finesse gadget spec ready"),
                new ProjectTaskDto("One Content", "Content approval workflow", "MEDIUM", 5, "PLANNED", "Lina", "1-May-26", "30-Jun-26", 61, 0, null),
                new ProjectTaskDto("One Content", "Go-live & training", "HIGH", 6, "PLANNED", "Lina / Sara", "1-Jun-26", "30-Jun-26", 30, 0, null));
    }
}
