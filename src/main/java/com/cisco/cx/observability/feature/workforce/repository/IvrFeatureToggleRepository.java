package com.cisco.cx.observability.feature.workforce.repository;

import com.cisco.cx.observability.feature.workforce.domain.IvrFeatureToggleEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IvrFeatureToggleRepository extends JpaRepository<IvrFeatureToggleEntity, Long> {
    List<IvrFeatureToggleEntity> findAllByOrderByAppNameAscFeatureKeyAsc();
    Optional<IvrFeatureToggleEntity> findByAppNameAndFeatureKey(String appName, String featureKey);
}
