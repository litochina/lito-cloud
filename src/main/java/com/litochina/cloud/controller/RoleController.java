package com.litochina.cloud.controller;

import com.litochina.base.common.annotation.RequestDTORequired;
import com.litochina.base.common.common.ReturnCode;
import com.litochina.base.common.controller.ApiRespBuilder;
import com.litochina.base.common.controller.CurrentUserContext;
import com.litochina.base.common.dto.RequestDTO;
import com.litochina.base.common.dto.ResponseDTO;
import com.litochina.base.common.exception.ServiceException;
import com.litochina.cloud.dto.SubRolePermDTO;
import com.litochina.cloud.service.RoleService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author chenxx
 * @date 2020/7/16/016
 */
@RestController
@RequestMapping(path = "/api/role")
public class RoleController {

    @Resource
    private RoleService roleService;

    @GetMapping(path = "/test")
    @RequiresPermissions(value = {"light:device-add", "led:device-add"}, logical= Logical.OR)
    public ResponseDTO test() {
        return ApiRespBuilder.success("允许访问");
    }

    @PostMapping(path = "/list")
    public ResponseDTO getRolesList(@RequestBody RequestDTO request) {
        String userId = CurrentUserContext.checkLoginAndGetUserId();
        String roleNameStr = request.getParam("roleNameStr");
        List<SubRolePermDTO> roleList = roleService.getRoleList(userId);

        if (!StringUtils.isBlank(roleNameStr)) {
            roleList = roleList.stream().filter(role -> role.getRoleName().contains(roleNameStr))
                    .collect(Collectors.toList());
        }
        return ApiRespBuilder.success("角色列表查询完成", roleList);
    }

    @PostMapping(path = "/save")
    public ResponseDTO saveRole(@RequestBody RequestDTO request) {
        String userId = CurrentUserContext.checkLoginAndGetUserId();
        SubRolePermDTO role = request.getParam(SubRolePermDTO.class);
        if (null == role) {
            throw new ServiceException(ReturnCode.PARAM_EMPTY);
        } else if (StringUtils.isBlank(role.getRoleName())) {
            throw new ServiceException(ReturnCode.PARAM_EMPTY, "roleName" + ReturnCode.PARAM_EMPTY.getMsg());
        } else if (null == role.getPermissions() || null == role.getOperationList()) {
            throw new ServiceException(ReturnCode.PARAM_EMPTY, "权限相关参数不允许为空");
        }

        roleService.save(role, userId);
        if (StringUtils.isBlank(role.getRoleId())) {
            return ApiRespBuilder.success("角色创建成功");
        } else {
            return ApiRespBuilder.success("角色编辑成功");
        }
    }

    @PostMapping(path = "/find")
    public ResponseDTO findRole(@RequestBody RequestDTO request) {
        String userId = CurrentUserContext.checkLoginAndGetUserId();
        String roleId = request.getParam("roleId");
        SubRolePermDTO role = roleService.findRoleDetail(roleId, userId);
        return ApiRespBuilder.success("角色相关权限查询成功", role);
    }

    @PostMapping(path = "/delete")
    @RequestDTORequired({"roleId"})
    public ResponseDTO deleteRole(@RequestBody RequestDTO request) {
        String userId = CurrentUserContext.checkLoginAndGetUserId();
        String roleId = request.getParam("roleId");
        roleService.deleteRoleByRoleId(roleId, userId);
        return ApiRespBuilder.success("角色删除成功");
    }
}
