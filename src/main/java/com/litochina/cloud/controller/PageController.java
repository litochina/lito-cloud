package com.litochina.cloud.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author chenxx
 * @date 2020/8/11/011
 */
@Controller
@RequestMapping(path = "/api/page")
public class PageController {

    @GetMapping("/manage")
    public String main() {
        return "manage";
    }
}
