package com.example.pcceobservability.repository;

import com.example.pcceobservability.entity.ProjectTaskEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTaskRepository extends JpaRepository<ProjectTaskEntity, Long> {
    List<ProjectTaskEntity> findAllByOrderBySortOrderAscIdAsc();
}
