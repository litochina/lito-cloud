package com.litochina.cloud.service;

import com.litochina.cloud.dto.SubRolePermDTO;
import com.litochina.cloud.dto.SubUserPermListDTO;

import java.util.List;
import java.util.Set;

/**
 * @author chenxx
 * @date 2020/7/16/016
 */
public interface RoleService {

    /**
     * 查询角色列表
     * @return java.util.List<com.qxaiot.base.entity.cloud.Role>
     **/
    List<SubRolePermDTO> getRoleList(String parentUserId);

    /**
     * 查询角色详情
     * @param roleId 角色id
     * @return com.qxaiot.cloud.dto.SubRolePermDTO
     **/
    SubRolePermDTO findRoleDetail(String roleId, String parentUserId);

    /**
     * 删除角色
     * @param roleId 角色id
     * @return void
     **/
    void deleteRoleByRoleId(String roleId, String parentUserId);

    /**
     * 保存角色信息
     * @param role 角色实体
     * @param parentUserId 母账号用户id
     * @return void
     **/
    void save(SubRolePermDTO role, String parentUserId);

    /**
     * 根据子账号用户id查询该账号拥有查看权限的系统
     * @param userId 用户id
     * @return java.util.Set<java.lang.String>
     **/
    Set<String> listFindSystemPermsByUserId(String userId);

    /**
     * 根据用户id查询该账号拥有的权限名称
     * @param userId 用户id
     * @return java.util.Set<java.lang.String>
     **/
    Set<String> listPermsByUserId(String userId);

    /**
     * 查询子账号每个系统权限情况
     * @param userId 子账号用户id
     * @return java.util.List<com.qxaiot.cloud.dto.SubUserPermListDTO>
     **/
    List<SubUserPermListDTO> listPermsStatusByUserId(String userId);


}
