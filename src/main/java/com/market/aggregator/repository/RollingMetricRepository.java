package com.market.aggregator.repository;

import com.market.aggregator.entity.RollingMetricEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RollingMetricRepository extends JpaRepository<RollingMetricEntity, Long> {

    List<RollingMetricEntity> findBySymbolOrderByTimestampDesc(String symbol, Pageable pageable);

    Optional<RollingMetricEntity> findFirstBySymbolOrderByTimestampDesc(String symbol);
}
