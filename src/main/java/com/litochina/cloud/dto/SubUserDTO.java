package com.litochina.cloud.dto;

import com.litochina.base.model.dto.UserRoleListDTO;
import lombok.Data;

/**
 * 子账号DTO
 *
 * @author chenxx
 * @date 2020/7/15/015
 */
@Data
public class SubUserDTO {
    private String userId;
    private String phone;
    private String email;
    private String status;
    private String statusName;
    private String failureTime;
    private String createTime;
    private UserRoleListDTO role; //角色
}
