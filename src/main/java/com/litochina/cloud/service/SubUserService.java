package com.litochina.cloud.service;


import com.litochina.cloud.dto.SubUserDTO;

import java.util.List;

/**
 * @author chenxx
 * @date 2020/7/15/015
 */
public interface SubUserService {

    /**
     * 查询子账号列表
     * @param parentUserId 母账号userId
     * @return java.util.List<com.qxaiot.cloud.dto.SubUserDTO>
     **/
    List<SubUserDTO> list(String parentUserId);

    /**
     * 保存子账号信息
     * @param subUser 子账号实体类
     * @param parentUserId 母账号userId
     * @return void
     **/
    void save(SubUserDTO subUser, String parentUserId);

    /**
     * 设置账号启用禁用
     * @param userId 子账号id
     * @param failureTime 过期时间
     * @param status 账号状态
     * @return void
     **/
    void setUsable(String userId, String failureTime, String status);

    /**
     * 删除子账号
     * @param userId 子账号id
     * @return void
     **/
    void deleteUser(String userId);
}
