package com.toss.monitor;

import com.toss.TestcontainersConfiguration;
import com.toss.client.dto.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PriceSnapshotRepositoryTest {

    @Autowired
    PriceSnapshotRepository repository;

    @Test
    void savesAndFindsLatestBySymbol() {
        Instant t1 = Instant.parse("2026-06-23T00:00:00Z");
        repository.save(PriceSnapshot.of("005930", new BigDecimal("72000"), Currency.KRW, t1, t1));
        repository.save(PriceSnapshot.of("005930", new BigDecimal("72500"), Currency.KRW,
                t1.plusSeconds(60), t1.plusSeconds(60)));
        repository.save(PriceSnapshot.of("000660", new BigDecimal("180000"), Currency.KRW, t1, t1));

        var latest = repository.findFirstBySymbolOrderByFetchedAtDesc("005930");

        assertThat(latest).isPresent();
        assertThat(latest.get().lastPrice()).isEqualByComparingTo("72500");
        assertThat(latest.get().currency()).isEqualTo("KRW");
        assertThat(latest.get().id()).isNotNull();
    }
}
