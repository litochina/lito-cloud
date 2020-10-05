package com.litochina.cloud.controller;

import com.litochina.base.common.controller.ApiRespBuilder;
import com.litochina.base.common.dto.ResponseDTO;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenxx
 * @date 2019/9/25/025
 */
@RestController
@RequestMapping("/api/home")
public class HomeController {

    @RequiresPermissions("home:list")
    @PostMapping("/systemlist")
    public ResponseDTO getUserSystemList() {

        return ApiRespBuilder.success();
    }
}
