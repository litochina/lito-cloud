package com.litochina.cloud.controller;
import com.litochina.base.common.controller.ApiRespBuilder;
import com.litochina.base.common.dto.RequestDTO;
import com.litochina.base.common.dto.ResponseDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenxx
 * @date 2020/7/16/016
 */
@RestController
@RequestMapping(path = "/api/permission")
public class PermissionController {

    @PostMapping(path = "/save")
    public ResponseDTO savePermission(@RequestBody RequestDTO request) {
        String permissionId = request.getParam("permissionId");
        String permissionName = request.getParam("permissionName");
        String permissionDesc = request.getParam("permissionDesc");
        String systemType = request.getParam("systemType");
        return ApiRespBuilder.success("权限添加成功");
    }
}
