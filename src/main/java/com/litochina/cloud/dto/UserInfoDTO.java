package com.litochina.cloud.dto;

import lombok.Data;

import java.util.List;

/**
 * @author chenxx
 * @date 2020/7/27/027
 */
@Data
public class UserInfoDTO {
    private String userId;
    private String phone;
    private String email;
    private int isSubAccount; //是否子账号，0否，1是
    private List<SubUserPermListDTO> permList; //权限列表，母账号为空
}
