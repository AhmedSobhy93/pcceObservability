package com.cisco.cx.observability.feature.workforce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_skill_assignments",
        uniqueConstraints = @UniqueConstraint(name = "uk_agent_skill_assignment", columnNames = {"agent_id", "skill_group"}))
public class AgentSkillAssignmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "agent_id", nullable = false, length = 160)
    private String agentId;
    @Column(name = "agent_name", length = 240)
    private String agentName;
    @Column(name = "team_name", length = 160)
    private String teamName;
    @Column(name = "skill_group", nullable = false, length = 240)
    private String skillGroup;
    private Integer proficiency;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(nullable = false, length = 80)
    private String source = "APP";
    @Column(length = 1000)
    private String notes;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getSkillGroup() { return skillGroup; }
    public void setSkillGroup(String skillGroup) { this.skillGroup = skillGroup; }
    public Integer getProficiency() { return proficiency; }
    public void setProficiency(Integer proficiency) { this.proficiency = proficiency; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
