package com.toss.dashboard;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 대시보드 페이지(Thymeleaf) 렌더링.
 */
@Controller
public class DashboardViewController {

    @GetMapping("/")
    public String dashboard() {
        return "dashboard";
    }
}
