package com.cisco.cx.observability.repository;

import com.cisco.cx.observability.entity.AgentSkillAssignmentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentSkillAssignmentRepository extends JpaRepository<AgentSkillAssignmentEntity, Long> {
    List<AgentSkillAssignmentEntity> findAllByOrderByAgentIdAscSkillGroupAsc();

    Optional<AgentSkillAssignmentEntity> findByAgentIdAndSkillGroup(String agentId, String skillGroup);
}
