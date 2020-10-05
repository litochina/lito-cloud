package com.litochina.cloud.dto;

import lombok.Data;

/**
 * 子账号角色权限DTO
 * 说明：
 * （1）当statusChecked, addChecked, editChecked, delChecked, operationChecked全部为true时，"checked"值为true，"isIndeterminate"值为false；
 * （2）当statusChecked, addChecked, editChecked, delChecked, operationChecked至少一个为true时，"checked"值为false，"isIndeterminate"值为true
 * （3）当所有都不选择时，"checked"值为false，"isIndeterminate"值为false
 *
 * @author chenxx
 * @date 2020/7/16/016
 */
@Data
public class SubPermissionDTO {
    private String systemType;
    private String systemName;
    private boolean indeterminate; //
    private boolean checked;  // 整体勾选
    private boolean statusChecked;  // 状态勾选
    private boolean addChecked; // 添加勾选
    private boolean editChecked; // 编辑勾选
    private boolean delChecked; // 删除勾选
    private boolean operationChecked; // 操作勾选
}
