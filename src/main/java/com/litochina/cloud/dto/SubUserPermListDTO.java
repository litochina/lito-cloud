package com.litochina.cloud.dto;

import lombok.Data;

/**
 * 子账号用户已有权限情况
 * @author chenxx
 * @date 2020/7/21/021
 */
@Data
public class SubUserPermListDTO {
    private String systemType;
    private boolean isStatus;
    private boolean isOperation;
    private boolean isAdd;
    private boolean isEdit;
    private boolean isDel;
}
