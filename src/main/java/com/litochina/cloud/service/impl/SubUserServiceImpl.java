package com.litochina.cloud.service.impl;
import com.litochina.base.common.common.ReturnCode;
import com.litochina.base.common.controller.CurrentUserContext;
import com.litochina.base.common.exception.ServiceException;
import com.litochina.base.common.utils.DateUtil;
import com.litochina.base.common.utils.IDUtils;
import com.litochina.base.model.dto.UserRoleListDTO;
import com.litochina.base.model.entity.cloud.User;
import com.litochina.base.model.entity.cloud.UserRole;
import com.litochina.base.model.enums.cloud.UserStatusEnum;
import com.litochina.base.persistence.mapper.cloud.UserMapper;
import com.litochina.base.persistence.mapper.cloud.UserRoleMapper;
import com.litochina.cloud.dto.SubRolePermDTO;
import com.litochina.cloud.dto.SubUserDTO;
import com.litochina.cloud.service.RoleService;
import com.litochina.cloud.service.SubUserService;
import com.litochina.cloud.service.UserService;
import jdk.nashorn.internal.ir.ReturnNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author chenxx
 * @date 2020/7/15/015
 */
@Service
@Slf4j
public class SubUserServiceImpl implements SubUserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private RoleService roleService;

    @Resource
    private UserService userService;

    @Override
    public List<SubUserDTO> list(String parentUserId) {
        List<User> userList = userMapper.listUsersByParentId(parentUserId);
        List<SubUserDTO> subUserList = new ArrayList<>();
        userList.forEach(user -> {
            SubUserDTO subUser = new SubUserDTO();
            subUser.setUserId(user.getId());
            subUser.setPhone(user.getPhone());
            subUser.setEmail(user.getEmail());
            subUser.setStatus(user.getStatus());
            subUser.setEmail(user.getEmail());
            if (null == user.getEmail()) {
                subUser.setEmail("");
            }
            String statusName = UserStatusEnum.NORMAL.getCode().equals(user.getStatus()) ?
                    UserStatusEnum.NORMAL.getDesc() : UserStatusEnum.DISABLE.getDesc();
            subUser.setStatusName(statusName);
            if (null == user.getFailuretime()) {
                subUser.setFailureTime("");
            } else {
                subUser.setFailureTime(DateUtil.formatDateToString(user.getFailuretime()));
            }
            subUser.setCreateTime(DateUtil.formatDateToString(user.getCreatetime()));
            List<UserRoleListDTO> userRoleList = userRoleMapper.listUserRolesByUserId(user.getId());
            if (userRoleList.size() > 0) {
                subUser.setRole(userRoleList.get(0));
            } else {
                subUser.setRole(new UserRoleListDTO());
            }
            subUserList.add(subUser);
        });
        return subUserList;
    }

    @Transactional
    @Override
    public void save(SubUserDTO subUser, String parentUserId) {
        if (null == subUser.getRole()) {
            List<SubRolePermDTO> roleList = roleService.getRoleList(parentUserId);
            if (roleList.size() == 0) {
                throw new ServiceException(ReturnCode.DATA_NOT_EXIST, "当前不存在任何可选角色，请前往角色管理创建");
            }
            throw new ServiceException(ReturnCode.PARAM_EMPTY, "账号角色不允许为空");
        }
        User mainUser = userMapper.selectByPrimaryKey(parentUserId);
        if (subUser.getPhone().equals(mainUser.getPhone())) {
            throw new ServiceException(ReturnCode.PARAM_VALUE_INVALID, "当前输入手机号与主账号一样，无法创建该账号");
        } else if (StringUtils.isBlank(subUser.getUserId()) && null != userMapper.findUserByPhone(subUser.getPhone())) {
            throw new ServiceException(ReturnCode.PARAM_VALUE_INVALID, "当前手机号已存在，无法创建该账号，请重新填写");
        }

        User user;
        boolean isAdd = false;
        if (StringUtils.isBlank(subUser.getUserId())) {
            user = new User();
            user.setParentId(parentUserId);
            user.setCreatetime(new Date());
            user.setId(IDUtils.getUUIDNoBar());
            user.setPhone(subUser.getPhone());
            isAdd = true;
        } else {
            user = userMapper.selectByPrimaryKey(subUser.getUserId());
            if (null == user) {
                throw new ServiceException(ReturnCode.DATA_NOT_EXIST, "当前查询用户不存在，无法编辑账号信息");
            }
            user.setUpdatetime(new Date());
        }

        if (null == subUser.getFailureTime()) {
            user.setFailuretime(null);
        } else {
            user.setFailuretime(DateUtil.strToDate(subUser.getFailureTime()));
        }
        user.setEmail(subUser.getEmail());
        user.setStatus(subUser.getStatus());

        if (null != subUser.getRole() && !StringUtils.isBlank(subUser.getRole().getRoleId())) {
            UserRole userRole = userRoleMapper.findByUserId(subUser.getUserId());
            if (null != userRole) {
                userRoleMapper.updateRoleByUserId(subUser.getRole().getRoleId(), userRole.getUserId());
            } else {
                userRole = new UserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(subUser.getRole().getRoleId());
                userRoleMapper.insert(userRole);
            }
        }

        if (isAdd) {
            userMapper.insert(user);
            userService.sendInitialPassword(user);
        } else {
            userMapper.updateByPrimaryKey(user);
            CurrentUserContext.refreshCurrentUserPerm(user.getId(), roleService.listPermsByUserId(user.getId()), null);
        }
    }

    @Override
    public void setUsable(String userId, String failureTime, String status) {
        if (null == userId) {
            throw new ServiceException(ReturnCode.PARAM_REQUIRED, ReturnCode.PARAM_REQUIRED.getMsg() + ": userId");
        } else if (!UserStatusEnum.NORMAL.getCode().equals(status) &&
                (!UserStatusEnum.DISABLE.getCode().equals(status))) {
            throw new ServiceException(ReturnCode.PARAM_VALUE_INVALID, ReturnCode.PARAM_VALUE_INVALID.getMsg() + ": status");
        }

        User user = userMapper.selectByPrimaryKey(userId);
        if (null == user) {
            throw new ServiceException(ReturnCode.DATA_NOT_EXIST, "用户账号有误，" + ReturnCode.DATA_NOT_EXIST);
        } else if (user.getStatus().equals(status)) {
            throw new ServiceException("当前账号状态与设置一致，无需再次设置");
        }

        if (status.equals(UserStatusEnum.NORMAL.getCode())) {
            if (!StringUtils.isBlank(failureTime)) {
                user.setFailuretime(DateUtil.strToDate(failureTime));
            } else {
                user.setFailuretime(null);
            }
        }
        user.setStatus(status);
        userMapper.updateByPrimaryKey(user);
    }

    @Transactional
    @Override
    public void deleteUser(String userId) {
        int res = userRoleMapper.deleteRoleByUserId(userId);
        if (res > 0) {
            int del = userMapper.deleteByPrimaryKey(userId);
            if (del == 0) {
                throw new ServiceException("子账号删除失败");
            }
        } else {
            throw new ServiceException("删除用户关联角色失败，无法删除用户");
        }
    }
}
