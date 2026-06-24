package com.toss.monitor;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 관심종목(watchlist) 관리. 설정의 초기 종목으로 시드되며 런타임에 추가/삭제 가능. 스레드 안전.
 */
@Service
public class WatchlistService {

    private final Set<String> symbols = new CopyOnWriteArraySet<>();

    public WatchlistService(MonitorProperties props) {
        symbols.addAll(props.symbols());
    }

    public Set<String> symbols() {
        return Set.copyOf(symbols);
    }

    public boolean isEmpty() {
        return symbols.isEmpty();
    }

    /** 추가. 새로 추가됐으면 true. */
    public boolean add(String symbol) {
        return symbols.add(symbol.trim());
    }

    /** 제거. 존재해서 제거됐으면 true. */
    public boolean remove(String symbol) {
        return symbols.remove(symbol);
    }
}
