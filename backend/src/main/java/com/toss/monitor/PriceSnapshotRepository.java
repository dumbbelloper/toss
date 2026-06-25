package com.toss.monitor;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

/**
 * 시세 스냅샷 저장소.
 */
public interface PriceSnapshotRepository extends CrudRepository<PriceSnapshot, Long> {

    /** 종목별 최신 스냅샷. */
    Optional<PriceSnapshot> findFirstBySymbolOrderByFetchedAtDesc(String symbol);
}
