package com.toss.history;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 일일 시세 재동기 스케줄러. */
@Component
public class BackfillScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackfillScheduler.class);

    private final BackfillService backfill;

    public BackfillScheduler(BackfillService backfill) {
        this.backfill = backfill;
    }

    /**
     * 매일 08:00 KST. 미국장 마감(16:00 ET) 후 Yahoo EOD 가 확정된 시점에 유니버스 전체를 풀 재동기한다
     * — 새 봉 추가 + 배당 발생 시 과거 adj_close 소급조정을 한 번에 반영(upsert). 유니버스가 커지면
     * 증분+주간 풀로 분리.
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
    void dailySync() {
        log.info("일일 시세 재동기 시작 (08:00 KST)");
        int n = backfill.backfillUniverse();
        log.info("일일 시세 재동기 완료: {} 종목", n);
    }
}
