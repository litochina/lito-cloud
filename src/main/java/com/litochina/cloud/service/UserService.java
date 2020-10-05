
package com.litochina.cloud.service;

import com.litochina.base.common.common.ReturnCode;
import com.litochina.base.common.dto.RequestDTO;
import com.litochina.base.common.dto.ResponseDTO;
import com.litochina.base.model.dto.CookieInfoDTO;
import com.litochina.base.model.dto.SolutionSystemUserDTO;
import com.litochina.base.model.dto.UserPrivilegesDTO;
import com.litochina.base.model.entity.cloud.User;
import com.litochina.base.persistence.service.BaseService;
import com.litochina.cloud.dto.UserInfoDTO;

import java.util.List;
import java.util.Map;

/**
* @author liuhx 
* @date 2019/10/8
* @version
* 说明: 
*/
public interface UserService extends BaseService<User> {
	/**
	 * 登陆
	 * @param requestDTO
	 * @return
	 */
	UserPrivilegesDTO login(RequestDTO requestDTO);
	/**
	 * 注册
	 * @param requestDTO
	 * @return
	 */
	User register(RequestDTO requestDTO);

	/**
	 * 修改个人密码
	 * @param userId 当前登录用户
	 * @param oldPwd 原密码
	 * @param newPwd 新密码
	 * @param confirmPwd 确认密码
	 * @return void
	 **/
	void changePwd(String userId, String oldPwd, String newPwd, String confirmPwd);

	/**
	 * 修改个人密码
	 * @param userId 当前用户id
	 * @param newPwd 新密码
	 * @param confirmPwd 确认密码
	 * @return void
	 **/
	void changePwd(String userId, String newPwd, String confirmPwd, String code, String timekey);

	/**
	 * 退出登陆
	 * @return
	 */
	public ReturnCode exit();

	/**
	 * 3D退出登陆
	 * @return
	 */
	void threeTimExit();
//	/**
//	 * 修改个人信息
//	 * @param requestDTO
//	 * @return
//	 */
//	public User change_information(RequestDTO requestDTO);
	List<SolutionSystemUserDTO> getSystemsByUserId(String userId);

	/**
	 * 保存cookie信息到redis
	 * @param cookieInfoDTO
	 * @return
	 */
	ResponseDTO cacheCookieInfo(CookieInfoDTO cookieInfoDTO);

	/**
	 * 获取cookie信息
	 * @return
	 */
	ResponseDTO getCookieInfo();

	/**
	 * 刷新用户token值
	 * @param
	 * @return com.qxaiot.base.dto.UserprivilegesDTO
	 **/
    UserPrivilegesDTO refreshToken(String phone);

    /**
     * 修改用户个人信息
     * @param userId 当前登录用户id
     * @param email 电子邮箱
     * @return void
     **/
	void updateUserInfo(String userId, String email);

	/**
	 * 查询用户个人信息
	 * @param userId 用户id
	 * @return com.qxaiot.cloud.dto.UserInfoDTO
	 **/
	UserInfoDTO getUserInfo(String userId);

    /**
     * 通过手机号查询当前用户是否有绑定邮箱
     *
     * @param requestDTO
     * @return com.qxaiot.base.dto.ResponseDTO
     **/
	Map<String, String> checkAccount(RequestDTO requestDTO);

	/**
	 * 发送找回密码验证码
	 *
	 * @param requestDTO
	 * @return void
	 **/
	ResponseDTO sendVerifiCode(RequestDTO requestDTO);

	/**
	 * 确认验证码
	 *
	 * @param requestDTO
	 * @return com.qxaiot.base.dto.ResponseDTO
	 **/
	User checkVerifiCode(RequestDTO requestDTO);


	/**
	 * 发送初始密码
	 *
	 * @param user
	 * @return void
	 **/
	void sendInitialPassword(User user);
}
