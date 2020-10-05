package com.litochina.cloud.service.impl;

import com.litochina.base.common.common.ReturnCode;
import com.litochina.base.common.exception.ServiceException;
import com.litochina.base.common.utils.DateUtil;
import com.litochina.base.common.utils.IDUtils;
import com.litochina.base.model.dto.RolePermissionDTO;
import com.litochina.base.model.dto.SolutionSystemUserDTO;
import com.litochina.base.model.dto.SystemPermDTO;
import com.litochina.base.model.dto.UserRolePermDTO;
import com.litochina.base.model.entity.cloud.Role;
import com.litochina.base.model.entity.cloud.RolePermission;
import com.litochina.base.model.enums.cloud.PermissionBaseEnum;
import com.litochina.base.model.enums.cloud.RoleTitleEnum;
import com.litochina.base.persistence.mapper.cloud.PermissionMapper;
import com.litochina.base.persistence.mapper.cloud.RoleMapper;
import com.litochina.base.persistence.mapper.cloud.RolePermissionMapper;
import com.litochina.base.persistence.mapper.cloud.UserRoleMapper;
import com.litochina.cloud.dto.SubPermissionDTO;
import com.litochina.cloud.dto.SubRolePermDTO;
import com.litochina.cloud.dto.SubUserPermAllDTO;
import com.litochina.cloud.dto.SubUserPermListDTO;
import com.litochina.cloud.service.RoleService;
import com.litochina.cloud.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author chenxx
 * @date 2020/7/16/016
 */
@Service
@Slf4j
public class RoleServiceImpl implements RoleService {

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private RolePermissionMapper rolePermissionMapper;

    @Resource
    private PermissionMapper permissionMapper;

    @Resource
    private UserService userService;

    @Override
    public List<SubRolePermDTO> getRoleList(String parentUserId) {
        List<Role> roleList = roleMapper.listRolesByParentUserId(parentUserId);

        List<SubRolePermDTO> resultList = new ArrayList<>();
        roleList.forEach(role -> {
            SubRolePermDTO subRole = new SubRolePermDTO();
            subRole.setRoleId(role.getRoleId());
            subRole.setRoleName(role.getRoleName());
            subRole.setRoleDescription(role.getRoleDescription() == null ? "" : role.getRoleDescription());
            subRole.setCreateTime(DateUtil.formatDateStr(role.getCreatetime(), null));
            int num = userRoleMapper.countUsersByRoleId(role.getRoleId());
            subRole.setLinkUserNum(String.valueOf(num));

            resultList.add(subRole);
        });
        return resultList;
    }

    @Override
    public SubRolePermDTO findRoleDetail(String roleId, String parentUserId) {
        //查询母账号所有系统
        List<SolutionSystemUserDTO> systemList = userService.getSystemsByUserId(parentUserId);
        if (StringUtils.isBlank(roleId)) {
            // roleId为空，查询所有
            SubRolePermDTO result = new SubRolePermDTO();
            List<SubPermissionDTO> allPermList = new ArrayList<>();
            systemList.forEach(sys -> {
                SubPermissionDTO subDto = new SubPermissionDTO();
                subDto.setSystemName(sys.getSystemName());
                subDto.setSystemType(sys.getSystemDesc());
                subDto.setIndeterminate(false);
                subDto.setChecked(false);
                subDto.setStatusChecked(false);
                subDto.setAddChecked(false);
                subDto.setEditChecked(false);
                subDto.setDelChecked(false);
                subDto.setOperationChecked(false);
                allPermList.add(subDto);
            });
            result.setPermissions(allPermList);
            return result;
        }

        Role role = checkRoleExist(roleId, parentUserId);
        SubRolePermDTO subRole = new SubRolePermDTO();
        subRole.setRoleId(role.getRoleId());
        subRole.setRoleName(role.getRoleName());
        subRole.setRoleDescription(role.getRoleDescription());
        List<SubPermissionDTO> permList = new ArrayList<>();

        List<UserRolePermDTO> userRolePermList = rolePermissionMapper.listPermByRoleId(roleId);
        Map<String, List<UserRolePermDTO>> rolePermMap = null;
        if (userRolePermList.size() > 0) {
            rolePermMap = userRolePermList.stream()
                    .collect(Collectors.groupingBy(UserRolePermDTO::getSystemType));
        }


        int[] count = new int[6];
        for (int i = 0; i < systemList.size(); i++) {
            SolutionSystemUserDTO systemDto = systemList.get(i);
            SubPermissionDTO subPerm = new SubPermissionDTO();
            subPerm.setSystemType(systemDto.getSystemDesc());
            subPerm.setSystemName(systemDto.getSystemName());
            subPerm.setIndeterminate(false);
            subPerm.setChecked(false);

            if (null != rolePermMap && rolePermMap.containsKey(systemDto.getSystemDesc())) {
                // 存在权限
                List<UserRolePermDTO> rolePermList = rolePermMap.get(systemDto.getSystemDesc());
                if (rolePermList.size() == 5) {
                    subPerm.setIndeterminate(false);
                    subPerm.setChecked(true);
                    count[0]++;
                } else {
                    subPerm.setIndeterminate(true);
                    subPerm.setChecked(false);
                }
                rolePermList.forEach(perm -> {
                    String[] permArray = perm.getPermName().split(":");
                    buildCheckData(subPerm, permArray[1], perm.getSort(), count);
                });
            }
            permList.add(subPerm);
        }
        subRole.setPermissions(permList);

        List<SubUserPermAllDTO> operationList = new ArrayList<>();
        boolean none = true;
        int numCount = 0;
        for (int j = 0; j < count.length; j++) {
            SubUserPermAllDTO permAllDto = new SubUserPermAllDTO();
            permAllDto.setName(RoleTitleEnum.getNameByIndex(j));
            if (count[j] == systemList.size()) {
                permAllDto.setIndeterminate(false);
                permAllDto.setCheckAll(true);
                none = false;
                numCount++;
            } else if (count[j] == 0) {
                permAllDto.setIndeterminate(false);
                permAllDto.setCheckAll(false);
            } else {
                permAllDto.setIndeterminate(true);
                permAllDto.setCheckAll(false);
                none = false;
            }
            operationList.add(permAllDto);
        }

        if (!none && numCount < count.length - 1) {
            operationList.get(0).setIndeterminate(true);
        } else if (numCount == count.length - 1) {
            operationList.get(0).setIndeterminate(false);
        }
        subRole.setOperationList(operationList);

        return subRole;
    }

    private SubPermissionDTO buildCheckData(SubPermissionDTO subPerm, String permStr, int index, int[] count) {
        if (PermissionBaseEnum.DEVICE_ENABLE.getName().equals(permStr)) {
            subPerm.setStatusChecked(true);
            count[index]++;
        } else {
            if (subPerm.isStatusChecked()) {
                subPerm.setStatusChecked(true);
            } else {
                subPerm.setStatusChecked(false);
            }
        }

        if (PermissionBaseEnum.DEVICE_ADD.getName().equals(permStr)) {
            subPerm.setAddChecked(true);
            count[index]++;
        } else {
            if (subPerm.isAddChecked()) {
                subPerm.setAddChecked(true);
            } else {
                subPerm.setAddChecked(false);
            }
        }
        if (PermissionBaseEnum.DEVICE_EDIT.getName().equals(permStr)) {
            subPerm.setEditChecked(true);
            count[index]++;
        } else {
            if (subPerm.isEditChecked()) {
                subPerm.setEditChecked(true);
            } else {
                subPerm.setEditChecked(false);
            }
        }
        if (PermissionBaseEnum.DEVICE_DELETE.getName().equals(permStr)) {
            subPerm.setDelChecked(true);
            count[index]++;
        } else {
            if (subPerm.isDelChecked()) {
                subPerm.setDelChecked(true);
            } else {
                subPerm.setDelChecked(false);
            }
        }
        if (PermissionBaseEnum.DEVICE_OPERATION.getName().equals(permStr)) {
            subPerm.setOperationChecked(true);
            count[index]++;
        } else {
            if (subPerm.isOperationChecked()) {
                subPerm.setOperationChecked(true);
            } else {
                subPerm.setOperationChecked(false);
            }
        }
        return subPerm;
    }

    @Transactional
    @Override
    public void deleteRoleByRoleId(String roleId, String parentUserId) {
        Role role = checkRoleExist(roleId, parentUserId);
        int num = userRoleMapper.countUsersByRoleId(role.getRoleId());
        if (num > 0) {
            throw new ServiceException("该角色存在关联的用户，不允许删除");
        } else {
            String roleName = role.getRoleName();
            rolePermissionMapper.deleteByRoleId(roleId);
            roleMapper.delete(role);
            log.info("角色【{}】已删除", roleName);
        }
    }

    private Role checkRoleExist(String roleId, String parentUserId) {
        List<Role> roleList = roleMapper.listRolesByParentUserId(parentUserId);
        Map<String, Role> roleMap = roleList.stream().collect(Collectors.toMap(Role::getRoleId, Role -> Role));
        if (null == roleMap.get(roleId)) {
            throw new ServiceException(ReturnCode.DATA_NOT_EXIST, ReturnCode.DATA_NOT_EXIST.getMsg() + ", roleId有误");
        }

        return roleMap.get(roleId);
    }

    @Transactional
    @Override
    public void save(SubRolePermDTO subRole, String parentUserId) {
        boolean flag = false;
        for (int i = 0; i < subRole.getOperationList().size(); i++) {
            SubUserPermAllDTO dto = subRole.getOperationList().get(i);
            if (dto.isCheckAll() && !dto.isIndeterminate() ||
                    (!dto.isCheckAll() && dto.isIndeterminate()) ||
                    (!dto.isCheckAll() && !dto.isIndeterminate())) {
                flag = true;
            }
        }
        if (!flag) {
            throw new ServiceException(ReturnCode.PARAM_EMPTY, "角色关联权限不允许为空");
        }

        String roleId = subRole.getRoleId();

        boolean isAdd = false;
        Role role;
        if (StringUtils.isBlank(roleId)) {
            Role findRole = roleMapper.findRoleByMainUserIdAndRoleName(parentUserId, subRole.getRoleName());
            if (null != findRole) {
                throw new ServiceException("当前已存在同名角色，不允许重复添加");
            }
            // 新增角色
            isAdd = true;
            role = new Role();
            role.setRoleId(IDUtils.getUUIDNoBar());
            role.setMainUserId(parentUserId);
            role.setCreatetime(DateUtil.formatDateToString(new Date()));
        } else {
            role = roleMapper.selectByPrimaryKey(roleId);
            if (null == role) {
                throw new ServiceException(ReturnCode.DATA_NOT_EXIST, "roleId有误，当前查询角色不存在");
            }
            role.setUpdatetime(DateUtil.formatDateToString(new Date()));
            // 编辑角色
        }
        role.setRoleName(subRole.getRoleName());
        role.setRoleDescription(subRole.getRoleDescription());

        // 过滤无选中情况的数据
        List<SubPermissionDTO> permList = subRole.getPermissions().stream()
                .filter(permDto -> !(!permDto.isChecked() && !permDto.isIndeterminate()))
                .collect(Collectors.toList());
        // 构建权限
        Map<String, List<SystemPermDTO>> systemPermMap = permissionMapper.listPermBySystemId(null).stream()
                .collect(Collectors.groupingBy(SystemPermDTO::getSystemType));
        List<RolePermission> rolePermList = new ArrayList<>();
        for (int i = 0; i < permList.size(); i++) {
            SubPermissionDTO permDto = permList.get(i);
            if (null != systemPermMap.get(permDto.getSystemType())) {
                Map<Integer, String> permMap = systemPermMap.get(permDto.getSystemType()).stream()
                        .collect(Collectors.toMap(SystemPermDTO::getSort, SystemPermDTO::getPermissionId));
                rolePermList.addAll(buildRolePermList(permDto, role.getRoleId(), permMap));
            }
        }

        if (isAdd) {
            roleMapper.insert(role);
        } else {
            roleMapper.updateByPrimaryKey(role);
            rolePermissionMapper.deleteByRoleId(roleId);
        }
        rolePermList.stream().filter(rolePerm -> null != rolePerm.getPermissionId())
                .forEach(rolePerm -> rolePermissionMapper.insert(rolePerm));
    }

    @Override
    public Set<String> listFindSystemPermsByUserId(String userId) {
        List<RolePermissionDTO> permList = userRoleMapper.listPermsByUserId(userId);
        List<RolePermissionDTO> filterList = permList.stream().filter(perm -> PermissionBaseEnum.DEVICE_ENABLE.getSort() == perm.getSort())
                .collect(Collectors.toList());
        return filterList.stream().map(RolePermissionDTO::getSystemType).collect(Collectors.toSet());
    }

    @Override
    public Set<String> listPermsByUserId(String userId) {
        return rolePermissionMapper.listPermByUserId(userId).stream()
                .map(UserRolePermDTO::getPermName).collect(Collectors.toSet());
    }

    @Override
    public List<SubUserPermListDTO> listPermsStatusByUserId(String userId) {
        List<SubUserPermListDTO> resultList = new ArrayList<>();
        List<RolePermissionDTO> permList = userRoleMapper.listPermsByUserId(userId);
        if (permList.size() == 0) {
            return resultList;
        }

        Map<String, List<RolePermissionDTO>> permMap = permList.stream().collect(Collectors.groupingBy(RolePermissionDTO::getSystemType));
        permMap.keySet().forEach(systemType -> {
            Set<Integer> systemPermSet = permMap.get(systemType).stream()
                    .map(RolePermissionDTO::getSort).collect(Collectors.toSet());
            SubUserPermListDTO subPerm = new SubUserPermListDTO();
            subPerm.setSystemType(systemType);
            subPerm.setStatus(systemPermSet.contains(PermissionBaseEnum.DEVICE_ENABLE.getSort()));
            subPerm.setOperation(systemPermSet.contains(PermissionBaseEnum.DEVICE_OPERATION.getSort()));
            subPerm.setAdd(systemPermSet.contains(PermissionBaseEnum.DEVICE_ADD.getSort()));
            subPerm.setEdit(systemPermSet.contains(PermissionBaseEnum.DEVICE_EDIT.getSort()));
            subPerm.setDel(systemPermSet.contains(PermissionBaseEnum.DEVICE_DELETE.getSort()));

            resultList.add(subPerm);
        });
        return resultList;
    }

    private List<RolePermission> buildRolePermList(SubPermissionDTO permDto, String roleId, Map<Integer, String> permMap) {
        List<RolePermission> resultList = new ArrayList<>();
        if (permDto.isStatusChecked()) {
            resultList.add(buildRolePerm(roleId, permMap.get(PermissionBaseEnum.DEVICE_ENABLE.getSort())));
        }
        if (permDto.isAddChecked()) {
            resultList.add(buildRolePerm(roleId, permMap.get(PermissionBaseEnum.DEVICE_ADD.getSort())));
        }
        if (permDto.isEditChecked()) {
            resultList.add(buildRolePerm(roleId, permMap.get(PermissionBaseEnum.DEVICE_EDIT.getSort())));
        }
        if (permDto.isDelChecked()) {
            resultList.add(buildRolePerm(roleId, permMap.get(PermissionBaseEnum.DEVICE_DELETE.getSort())));
        }
        if (permDto.isOperationChecked()) {
            resultList.add(buildRolePerm(roleId, permMap.get(PermissionBaseEnum.DEVICE_OPERATION.getSort())));
        }
        return resultList;
    }

    private RolePermission buildRolePerm(String roleId, String permId) {
        RolePermission rolePerm = new RolePermission();
        rolePerm.setRoleId(roleId);
        rolePerm.setPermissionId(permId);
        return rolePerm;
    }
}
