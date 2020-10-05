package com.litochina.cloud.dto;

import lombok.Data;

import java.util.List;

/**
 * 子账号角色DTO
 * @author chenxx
 * @date 2020/7/16/016
 */
@Data
public class SubRolePermDTO {
    private String roleId;
    private String roleName;
    private String roleDescription;
    private String linkUserNum;  // 关联用户数
    private String createTime; //创建时间
    private List<SubPermissionDTO> permissions;
    private List<SubUserPermAllDTO> operationList;
}
