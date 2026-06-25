package com.toss.history;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 백테스트 데이터 적재 관리 API. */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final BackfillService backfill;
    private final PriceDailyDao dao;

    public HistoryController(BackfillService backfill, PriceDailyDao dao) {
        this.backfill = backfill;
        this.dao = dao;
    }

    /** 유니버스 백필 트리거(배치). 적재 후 현황 반환. */
    @PostMapping("/backfill")
    public Map<String, Object> backfill() {
        int symbols = backfill.backfillUniverse();
        return Map.of("backfilledSymbols", symbols, "coverage", dao.coverage());
    }

    /** 적재 현황(심볼별 봉 수·기간). */
    @GetMapping("/coverage")
    public List<Map<String, Object>> coverage() {
        return dao.coverage();
    }

    /** 백테스트 종목 선택용 유니버스(+커버리지). */
    @GetMapping("/universe")
    public List<Map<String, Object>> universe() {
        return dao.universe();
    }
}
