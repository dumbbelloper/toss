package com.toss.digest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 시장 요약 수동 발송/미리보기.
 */
@RestController
@RequestMapping("/api/digest")
public class DigestController {

    private final MarketDigestService digest;

    public DigestController(MarketDigestService digest) {
        this.digest = digest;
    }

    /** 지금 발송. */
    @PostMapping("/send")
    public Map<String, Object> send() {
        return Map.of("sent", true, "message", digest.sendDigest());
    }

    /** 발송 없이 텍스트 미리보기. */
    @GetMapping("/preview")
    public String preview() {
        return digest.buildMessage();
    }
}
