package com.litochina.cloud.controller;

import com.litochina.base.common.annotation.RequestDTORequired;
import com.litochina.base.common.common.ReturnCode;
import com.litochina.base.common.controller.ApiRespBuilder;
import com.litochina.base.common.controller.CurrentUserContext;
import com.litochina.base.common.dto.RequestDTO;
import com.litochina.base.common.dto.ResponseDTO;
import com.litochina.base.common.exception.ServiceException;
import com.litochina.base.common.utils.StringHandleUtil;
import com.litochina.base.model.enums.cloud.UserStatusEnum;
import com.litochina.cloud.dto.SubUserDTO;
import com.litochina.cloud.service.SubUserService;
import jodd.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author chenxx
 * @date 2020/7/15/015
 */
@RestController
@RequestMapping(path = "/api/setting/user")
public class SubUserController {

    @Resource
    private SubUserService subUserService;

    @PostMapping(path = "/save")
    public ResponseDTO save(@RequestBody RequestDTO request) {
        String userId = CurrentUserContext.checkLoginAndGetUserId();
        SubUserDTO subUser = request.getParam(SubUserDTO.class);
        if (null == subUser) {
            throw new ServiceException(ReturnCode.PARAM_EMPTY);
        } else if (StringUtils.isBlank(subUser.getPhone())) {
            throw new ServiceException(ReturnCode.PARAM_REQUIRED, ReturnCode.PARAM_REQUIRED.getMsg() + ": phone");
        } else if (StringUtils.isBlank(subUser.getUserId())) {
            // 新增情况
            // 校验手机号格式
            StringHandleUtil.PhoneFormatChecks(subUser.getPhone());
            // 校验email
            if (StringUtils.isBlank(subUser.getEmail())) {
                throw new ServiceException(ReturnCode.PARAM_REQUIRED, ReturnCode.PARAM_REQUIRED.getMsg() + ": email");
            }
        } else if (StringUtils.isBlank(subUser.getStatus())) {
            subUser.setStatus(UserStatusEnum.DISABLE.getCode()); // 未设置时默认账号禁用
        } else if (!StringHandleUtil.emailFormatCheck(subUser.getEmail())) {
            throw new ServiceException("电子邮箱格式有误，请重新填写");
        }
        subUserService.save(subUser, userId);
        if (StringUtil.isBlank(subUser.getUserId())) {
            return ApiRespBuilder.success("子账号创建成功，初始密码已发送到关联邮箱");
        }
        return ApiRespBuilder.success("子账号信息编辑成功");
    }

    @PostMapping(path = "/list")
    public ResponseDTO list(@RequestBody RequestDTO request) {
        String userId = CurrentUserContext.checkLoginAndGetUserId();
        List<SubUserDTO> subUserList = subUserService.list(userId);

        String phoneNum = request.getParam("phoneNum");
        if (subUserList.size() > 0 && (!StringUtils.isBlank(phoneNum))) {
            subUserList = subUserList.stream().filter(subUser -> subUser.getPhone().contains(phoneNum))
                    .collect(Collectors.toList());
        }

        return ApiRespBuilder.success("子账号用户列表查询完成", subUserList);
    }

    @PostMapping(path = "/setUsable")
    @RequestDTORequired({"userId", "status"})
    public ResponseDTO setEnable(@RequestBody RequestDTO request) {
        CurrentUserContext.checkLoginAndGetUserId();
        String userId = request.getParam("userId");
        String failureTime = request.getParam("failureTime");
        String status = request.getParam("status");
        subUserService.setUsable(userId, failureTime, status);
        return ApiRespBuilder.success("子账号设置成功");
    }

    @PostMapping(path = "/delete")
    public ResponseDTO delete(@RequestBody RequestDTO request) {
        CurrentUserContext.checkLoginAndGetUserId();
        String userId = request.getParam("userId");
        subUserService.deleteUser(userId);
        return ApiRespBuilder.success("子账号删除成功");
    }
}
