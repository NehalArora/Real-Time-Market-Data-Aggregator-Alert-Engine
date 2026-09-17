package com.market.aggregator.repository;

import com.market.aggregator.entity.TickEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TickRepository extends JpaRepository<TickEntity, Long> {

    List<TickEntity> findBySymbolOrderByTimestampDesc(String symbol, Pageable pageable);

    List<TickEntity> findBySymbolAndTimestampBetweenOrderByTimestampAsc(String symbol, Long fromTimestamp, Long toTimestamp);
}
