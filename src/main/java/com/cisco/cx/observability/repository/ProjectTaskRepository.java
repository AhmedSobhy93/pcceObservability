package com.cisco.cx.observability.repository;

import com.cisco.cx.observability.entity.ProjectTaskEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTaskRepository extends JpaRepository<ProjectTaskEntity, Long> {
    List<ProjectTaskEntity> findAllByOrderBySortOrderAscIdAsc();
}
