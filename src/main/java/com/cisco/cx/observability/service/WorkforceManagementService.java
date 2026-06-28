package com.cisco.cx.observability.service;

import com.cisco.cx.observability.entity.AgentSkillAssignmentEntity;
import com.cisco.cx.observability.entity.IvrFeatureToggleEntity;
import com.cisco.cx.observability.model.AgentSkillAssignment;
import com.cisco.cx.observability.model.AgentSkillAssignmentRequest;
import com.cisco.cx.observability.model.IvrFeatureToggle;
import com.cisco.cx.observability.model.IvrFeatureToggleRequest;
import com.cisco.cx.observability.repository.AgentSkillAssignmentRepository;
import com.cisco.cx.observability.repository.IvrFeatureToggleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WorkforceManagementService {
    private final AgentSkillAssignmentRepository assignmentRepository;
    private final IvrFeatureToggleRepository ivrFeatureToggleRepository;
    private final AuditService auditService;

    public WorkforceManagementService(
            AgentSkillAssignmentRepository assignmentRepository,
            IvrFeatureToggleRepository ivrFeatureToggleRepository,
            AuditService auditService) {
        this.assignmentRepository = assignmentRepository;
        this.ivrFeatureToggleRepository = ivrFeatureToggleRepository;
        this.auditService = auditService;
    }

    public List<AgentSkillAssignment> assignments() {
        return assignmentRepository.findAllByOrderByAgentIdAscSkillGroupAsc().stream().map(this::toDto).toList();
    }

    public AgentSkillAssignment saveAssignment(AgentSkillAssignmentRequest request) {
        String agentId = required(request == null ? null : request.agentId(), "agentId");
        String skillGroup = required(request.skillGroup(), "skillGroup");
        AgentSkillAssignmentEntity entity = assignmentRepository.findByAgentIdAndSkillGroup(agentId, skillGroup)
                .orElseGet(AgentSkillAssignmentEntity::new);
        apply(entity, request);
        AgentSkillAssignment saved = toDto(assignmentRepository.save(entity));
        auditService.record("UPSERT_AGENT_SKILL_ASSIGNMENT", saved.agentId() + " -> " + saved.skillGroup(),
                Map.of("enabled", saved.enabled(), "team", safe(saved.teamName()), "source", safe(saved.source())));
        return saved;
    }

    public void deleteAssignment(long id) {
        assignmentRepository.deleteById(id);
        auditService.record("DELETE_AGENT_SKILL_ASSIGNMENT", String.valueOf(id), null);
    }

    public List<IvrFeatureToggle> ivrFeatures() {
        return ivrFeatureToggleRepository.findAllByOrderByAppNameAscFeatureKeyAsc().stream().map(this::toDto).toList();
    }

    public IvrFeatureToggle saveIvrFeature(IvrFeatureToggleRequest request) {
        String appName = required(request == null ? null : request.appName(), "appName");
        String featureKey = required(request.featureKey(), "featureKey");
        IvrFeatureToggleEntity entity = ivrFeatureToggleRepository.findByAppNameAndFeatureKey(appName, featureKey)
                .orElseGet(IvrFeatureToggleEntity::new);
        entity.setAppName(appName);
        entity.setFeatureKey(featureKey);
        entity.setEnabled(Boolean.TRUE.equals(request.enabled()));
        entity.setMinSeverity(textOr(request.minSeverity(), "WARNING"));
        entity.setConfigValue(valueOr(request.configValue(), null));
        entity.setNotes(valueOr(request.notes(), null));
        entity.setUpdatedAt(LocalDateTime.now());
        IvrFeatureToggle saved = toDto(ivrFeatureToggleRepository.save(entity));
        auditService.record("UPSERT_IVR_FEATURE", saved.appName() + ":" + saved.featureKey(),
                Map.of("enabled", saved.enabled(), "severity", safe(saved.minSeverity())));
        return saved;
    }

    public void deleteIvrFeature(long id) {
        ivrFeatureToggleRepository.deleteById(id);
        auditService.record("DELETE_IVR_FEATURE", String.valueOf(id), null);
    }

    private void apply(AgentSkillAssignmentEntity entity, AgentSkillAssignmentRequest request) {
        entity.setAgentId(required(request == null ? null : request.agentId(), "agentId"));
        entity.setAgentName(valueOr(request.agentName(), null));
        entity.setTeamName(valueOr(request.teamName(), null));
        entity.setSkillGroup(required(request.skillGroup(), "skillGroup"));
        entity.setProficiency(request.proficiency() == null ? null : Math.max(1, Math.min(10, request.proficiency())));
        entity.setEnabled(request.enabled() == null || request.enabled());
        entity.setSource(textOr(request.source(), "APP"));
        entity.setNotes(valueOr(request.notes(), null));
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private AgentSkillAssignment toDto(AgentSkillAssignmentEntity entity) {
        return new AgentSkillAssignment(entity.getId(), entity.getAgentId(), entity.getAgentName(),
                entity.getTeamName(), entity.getSkillGroup(), entity.getProficiency(), entity.isEnabled(),
                entity.getSource(), entity.getNotes(), entity.getUpdatedAt());
    }

    private IvrFeatureToggle toDto(IvrFeatureToggleEntity entity) {
        return new IvrFeatureToggle(entity.getId(), entity.getAppName(), entity.getFeatureKey(),
                entity.isEnabled(), entity.getMinSeverity(), entity.getConfigValue(),
                entity.getNotes(), entity.getUpdatedAt());
    }

    private String required(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String textOr(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String valueOr(String value, String fallback) {
        return value == null ? fallback : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
