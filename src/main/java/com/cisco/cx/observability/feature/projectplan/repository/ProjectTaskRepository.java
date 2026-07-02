package com.cisco.cx.observability.feature.projectplan.repository;

import com.cisco.cx.observability.feature.projectplan.domain.ProjectTaskEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTaskRepository extends JpaRepository<ProjectTaskEntity, Long> {
    List<ProjectTaskEntity> findAllByOrderBySortOrderAscIdAsc();
}
