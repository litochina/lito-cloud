package com.litochina.cloud.service.impl;

import com.litochina.base.common.common.ReturnCode;
import com.litochina.base.common.controller.ApiRespBuilder;
import com.litochina.base.common.controller.CurrentUserContext;
import com.litochina.base.common.dto.RequestDTO;
import com.litochina.base.common.dto.ResponseDTO;
import com.litochina.base.common.exception.ServiceException;
import com.litochina.base.common.utils.DateUtil;
import com.litochina.base.common.utils.IDUtils;
import com.litochina.base.common.utils.StringHandleUtil;
import com.litochina.base.model.dto.CookieInfoDTO;
import com.litochina.base.model.dto.SolutionSystemUserDTO;
import com.litochina.base.model.dto.SystemPermDTO;
import com.litochina.base.model.dto.UserPrivilegesDTO;
import com.litochina.base.model.entity.cloud.ForgotPassword;
import com.litochina.base.model.entity.cloud.User;
import com.litochina.base.model.entity.cloud.UserAlarmInfo;
import com.litochina.base.model.entity.common.VerifiCode;
import com.litochina.base.model.entity.device.DeviceTree;
import com.litochina.base.model.entity.device.DeviceTreeUser;
import com.litochina.base.model.enums.cloud.ForgotStatusEnum;
import com.litochina.base.model.enums.cloud.NodeTypeEnum;
import com.litochina.base.model.enums.cloud.UserStatusEnum;
import com.litochina.base.persistence.mapper.ProcedureMapper;
import com.litochina.base.persistence.mapper.cloud.ForgotPasswordMapper;
import com.litochina.base.persistence.mapper.cloud.PermissionMapper;
import com.litochina.base.persistence.mapper.cloud.UserAlarmInfoMapper;
import com.litochina.base.persistence.mapper.cloud.UserMapper;
import com.litochina.base.persistence.mapper.common.VerifiCodeMapper;
import com.litochina.base.persistence.mapper.device.DeviceTreeUserMapper;
import com.litochina.base.persistence.mapper.manage.SolutionSystemUserMapper;
import com.litochina.base.persistence.service.impl.BaseServiceImpl;
import com.litochina.bgsound.util.AESUtils;
import com.litochina.cloud.dto.MailDTO;
import com.litochina.cloud.dto.SubUserPermListDTO;
import com.litochina.cloud.dto.UserInfoDTO;
import com.litochina.cloud.service.MailService;
import com.litochina.cloud.service.RoleService;
import com.litochina.cloud.service.UserService;
import com.litochina.cloud.utils.GetMacAddressUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Service
public class UserServiceImpl extends BaseServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private SolutionSystemUserMapper solutionSystemUserMapper;
    @Resource
    private VerifiCodeMapper verifiCodeMapper;
    @Resource
    private ProcedureMapper procMapper;
    @Resource
    private DeviceTreeUserMapper deviceTreeUserMapper;
    @Resource
    private UserAlarmInfoMapper userAlarmInfoMapper;
    @Resource
    private PermissionMapper permissionMapper;
    @Resource
    private RoleService roleService;
    @Resource
    private MailService mailService;
    @Resource
    private ForgotPasswordMapper forgotPasswordMapper;
    @Resource
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Override
    public UserPrivilegesDTO login(RequestDTO requestDTO) {
        String flag = requestDTO.getParam("type");
        if (StringUtils.isBlank(flag)) {
            checkLoginVerifyCode(requestDTO);
        }
        User user = checkLoginUser(requestDTO);
        userAlarm(user);
        return getAndSetUserPrivileges(flag, user);
    }

    private UserPrivilegesDTO getAndSetUserPrivileges(String flag, User user) {

        UserPrivilegesDTO userprivilegesDTO = new UserPrivilegesDTO();
        userprivilegesDTO.setUser(user);

        String userId = user.getId();
        String logonToken = CurrentUserContext.getLogonUserTokenByType(flag, userId);
        //设置当前登录用户
        if (StringUtils.isBlank(logonToken)) {
            Set<String> permSet = buildUserPerm(user);
            String token = DigestUtils.md5Hex(new Date().getTime() + "")
                    + "-" + DigestUtils.md5Hex(user.getId())
                    + "-" + DigestUtils.md5Hex(IDUtils.getUUID());
            if (StringUtils.isNotBlank(flag)) {
                String threeTimToken = DigestUtils.md5Hex(token);
                CurrentUserContext.setThreeTimCurrentUser(userId, threeTimToken, permSet);
                userprivilegesDTO.setTimToken(threeTimToken);
            } else {
                CurrentUserContext.setCurrentUserPerm(user.getId(), token, permSet);
                userprivilegesDTO.setToken(token);
                if (StringUtils.isNotBlank(user.getParentId())) {
                    CurrentUserContext.setSubUser(user.getId(), user.getParentId());
                }
            }
        } else {
            //当前用户已登录，不刷新已有登录信息
            Set<String> permSet = refreshPerm(user);
            CurrentUserContext.refreshCurrentUserPerm(user.getId(), permSet, flag);
            if (StringUtils.isNotBlank(flag)) {
                userprivilegesDTO.setTimToken(logonToken);
            } else {
                userprivilegesDTO.setToken(logonToken);
            }
        }
        return userprivilegesDTO;
    }

    private void userAlarm(User user) {
        User parentUser = selectByPrimaryKey(user.getParentId());
        //异步判断该用户的设备数量是否已经超限，若超限，则保存该用户的告警信息，用户可继续登录
        String userId = user.getId();
        String parentId = user.getParentId();
        String deviceNumberMax = user.getDeviceNumberMax();
        if (StringUtils.isNotBlank(deviceNumberMax)) {
            User parUser = parentUser;
            new Thread(() -> {
                String parentUserId = userId;
                String maxNum = deviceNumberMax;
                if (null != parUser && StringUtils.isNotBlank(parentId)) {
                    parentUserId = checkIsSubUser(parentId).getId();
                    maxNum = parUser.getDeviceNumberMax();
                }

                DeviceTreeUser deviceTreeUser = deviceTreeUserMapper.getDeviceTreeUserByUserId(parentUserId);
                if (null == deviceTreeUser) {
                    throw new ServiceException(ReturnCode.PARAM_VALUE_INVALID, "根节点不存在");
                }
                List<DeviceTree> childNoteListByNodeId = procMapper.getChildNoteListByNodeId(deviceTreeUser.getTreeId(), NodeTypeEnum.DEVICE_NODE.getType());
                if (!"-1".equals(maxNum) && childNoteListByNodeId.size() > Integer.parseInt(maxNum)) {
                    UserAlarmInfo alarmInfo = new UserAlarmInfo();
                    alarmInfo.setUserId(userId);
                    alarmInfo.setExistDeviceNum(String.valueOf(childNoteListByNodeId.size()));
                    alarmInfo.setMaxDeviceNum(maxNum);
                    alarmInfo.setAlarmInfo("已达可添加设备数量上限，不能再添加设备");
                    alarmInfo.setCreatetime(new Date());
                    userAlarmInfoMapper.insert(alarmInfo);
                }
            }).start();
        }
    }

    private User checkLoginUser(RequestDTO requestDTO) {
        User user = new User();
        user.setPhone(requestDTO.get("phone").toString());
        user = selectOne(user);
        if (null == user) {
            throw new ServiceException(ReturnCode.USER_ERROR, "手机号未注册");
        }
        if (!DigestUtils.md5Hex(requestDTO.get("password").toString()).equals(user.getPassword())) {
            throw new ServiceException(ReturnCode.USER_ERROR, "密码有误");
        }
        user.setPassword(null);

        // 判断用户是否具备登录权限
        // 1.账号过期时间不为空，并且已超时
        // 2.子账号有效时间为空，父账号过期时间不为空，以父账号超时时间为准
        // 3.子账号有效时间不为空，父账号有效时间也不为空，且父账号时间小于子账号时间
        User parentUser = selectByPrimaryKey(user.getParentId());
        if ((null != user.getFailuretime() && new Date().after(user.getFailuretime()))
                || (null != parentUser && ((null == user.getFailuretime() && null != parentUser.getFailuretime()
                && new Date().after(parentUser.getFailuretime()))
                || (null != user.getFailuretime() && null != parentUser.getFailuretime()
                && user.getFailuretime().after(parentUser.getFailuretime()))))) {
            throw new ServiceException("当前账号登录权限已过期，请联系管理员！");
        }

        //需查询用户的mac地址是否有记录，有则能登录，否则不允许登录
        if (StringUtils.isNotBlank(user.getMacAddress())) {
            checkMacAddress(user.getMacAddress());
        } else {
            // 校验是否为子账号
            if (StringUtils.isNotBlank(user.getParentId())) {
                parentUser = checkIsSubUser(user.getParentId());
                checkMacAddress(parentUser.getMacAddress());
            } else {
                throw new ServiceException(ReturnCode.PERMISSION_INVALID, "该机器暂不具备登录权限，请联系奇信智能科技有限公司人员");
            }
        }
        return user;
    }

    private void checkLoginVerifyCode(RequestDTO requestDTO) {
        verifiCodeMapper.deletetime();
        VerifiCode verifiCode = new VerifiCode();
        verifiCode.setCode(requestDTO.getParam("code").toUpperCase());
        verifiCode.setTimekey(requestDTO.get("timekey").toString());
        verifiCode = verifiCodeMapper.selectOne(verifiCode);
        if (null == verifiCode) {
            throw new ServiceException(ReturnCode.USER_ERROR, "验证码有误");
        }
        verifiCodeMapper.delete(verifiCode);
    }

    // 登录时，如果用户已在线，刷新当前用户权限
    private Set<String> refreshPerm(User user) {
        return buildUserPerm(user);
    }

    private Set<String> buildUserPerm(User user) {
        if (StringUtils.isNotBlank(user.getParentId())) {
            return roleService.listPermsByUserId(user.getId());
        }

        Set<String> permSet = new HashSet<>();
        List<SolutionSystemUserDTO> systemList = solutionSystemUserMapper.findSystemList(user.getId());
        systemList.forEach(system -> {
            Set<String> collectSet = permissionMapper.listPermBySystemId(system.getSystemId()).stream()
                    .map(SystemPermDTO::getPermissionName).collect(Collectors.toSet());
            permSet.addAll(collectSet);
        });

        return permSet;
    }

    private User checkIsSubUser(String parentId) {
        User parentUser = new User();
        parentUser.setId(parentId);
        parentUser = selectOne(parentUser);
        if (null == parentUser) {
            throw new ServiceException(ReturnCode.PERMISSION_INVALID, "当前子账号无效，不允许登录");
        }
        return parentUser;
    }

    private void checkMacAddress(String macAddress) {
        String[] mac_list = macAddress.split(",");
        //获取当前服务器的mac地址
        boolean macFlag = false;
        try {
            List<String> macAddressList = GetMacAddressUtils.getMacAddressList();
            for (String mac_addr : macAddressList) {
                if (Arrays.asList(mac_list).contains(mac_addr)) {
                    macFlag = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!macFlag) {
            throw new ServiceException(ReturnCode.PERMISSION_INVALID, "该机器暂不具备登录权限，请联系奇信智能科技有限公司人员");
        }
    }

    @Override
    public User register(RequestDTO requestDTO) {
        checkLoginVerifyCode(requestDTO);

        String phone = requestDTO.getParam("phone");
        StringHandleUtil.PhoneFormatChecks(phone);

        User user = new User();
        user.setPhone(phone);
        List<User> sysList = select(user);
        if (sysList.size() > 0) {
            throw new ServiceException(ReturnCode.PHONE_REPEAT_ERROR, "手机号已注册");
        }

        String email = requestDTO.getParam("email");
        if (StringUtils.isBlank(email)) {
            throw new ServiceException(ReturnCode.PARAM_REQUIRED, ReturnCode.PARAM_REQUIRED.getMsg() + ": email");
        } else if (!StringHandleUtil.emailFormatCheck(email)) {
            throw new ServiceException("电子邮箱格式不正确，请重新填写");
        }

        user.setId(IDUtils.getUUIDNoBar());
        user.setEmail(email);
        user.setStatus(UserStatusEnum.WAITCONFIRM.getCode());
        user.setCreatetime(new Date());
        if (!insert(user)) {
            throw new ServiceException(ReturnCode.REGISTER_ERROR, "注册失败");
        }
        return user;
    }

    @Override
    public void changePwd(String userId, String oldPwd, String newPwd, String confirmPwd) {
        User user = selectByPrimaryKey(userId);
        if (!DigestUtils.md5Hex(oldPwd).equals(user.getPassword())) {
            throw new ServiceException(ReturnCode.USER_ERROR, "原密码与当前账号密码不一致，无法修改密码");
        } else if (!newPwd.equals(confirmPwd)) {
            throw new ServiceException(ReturnCode.USER_ERROR, "新密码与确认密码不一致，请重新填写");
        }

        String pwd = DigestUtils.md5Hex(newPwd);
        user.setPassword(pwd);
        user.setUpdatetime(new Date());
        updateByPrimaryKey(user);
    }

    @Override
    public void changePwd(String userId, String newPwd, String confirmPwd, String code, String timekey) {

        VerifiCode verifiCode = new VerifiCode();
        verifiCode.setCode(code);
        verifiCode.setTimekey(timekey);
        verifiCode = verifiCodeMapper.selectOne(verifiCode);
        if (null == verifiCode) {

            throw new ServiceException(ReturnCode.USER_ERROR, "验证码有误");
        }
        verifiCodeMapper.delete(verifiCode);

        User user = selectByPrimaryKey(userId);
        if (!newPwd.equals(confirmPwd)) {
            throw new ServiceException(ReturnCode.USER_ERROR, "新密码与确认密码不一致，请重新填写");
        }

        String pwd = DigestUtils.md5Hex(newPwd);
        user.setPassword(pwd);
        user.setUpdatetime(new Date());
        updateByPrimaryKey(user);
    }

    @Override
    public ReturnCode exit() {
        CurrentUserContext.delCurrentLoginUser();
        return ReturnCode.SUCCESS;
    }

    @Override
    public void threeTimExit() {
        CurrentUserContext.deleteThreeUser();
    }

    @Override
    public List<SolutionSystemUserDTO> getSystemsByUserId(String userId) {
        User user = selectByPrimaryKey(userId);
        if (!UserStatusEnum.NORMAL.getCode().equals(user.getStatus())
                || (null != user.getFailuretime() && new Date().after(user.getFailuretime()))) {
            return new ArrayList<SolutionSystemUserDTO>();
        }
        if (StringUtils.isNotBlank(user.getParentId())) {
            user = checkIsSubUser(user.getParentId());
        }
        List<SolutionSystemUserDTO> solutionList = solutionSystemUserMapper.findSystemList(user.getId());
        return null == solutionList ? new ArrayList<SolutionSystemUserDTO>() : solutionList;
    }

    @Override
    public ResponseDTO cacheCookieInfo(CookieInfoDTO cookieInfoDTO) {
        if (null == cookieInfoDTO || StringUtils.isBlank(cookieInfoDTO.getSn())) {
            throw new ServiceException("参数不能为空");
        }
        String userId = CurrentUserContext.checkLoginAndGetUserId();
        if (CurrentUserContext.setCookieInfo(userId, cookieInfoDTO)) {
            return ApiRespBuilder.success();
        }
        return ApiRespBuilder.error();
    }

    @Override
    public ResponseDTO getCookieInfo() {
        String userId = CurrentUserContext.checkLoginAndGetUserId();
        CookieInfoDTO cookieInfoDTO = CurrentUserContext.getCookieInfoByUserId(userId);
        return ApiRespBuilder.success(cookieInfoDTO);
    }

    @Override
    public UserPrivilegesDTO refreshToken(String phone) {
        return null;
    }

    @Override
    public void updateUserInfo(String userId, String email) {
        if (StringUtils.isBlank(email)) {
            throw new ServiceException(ReturnCode.PARAM_REQUIRED, ReturnCode.PARAM_REQUIRED.getMsg() + ": email");
        } else if (!StringHandleUtil.emailFormatCheck(email)) {
            throw new ServiceException("电子邮箱格式有误，请重新填写");
        }

        User user = selectByPrimaryKey(userId);
        if (null == user) {
            throw new ServiceException(ReturnCode.DATA_NOT_EXIST, "userId有误，当前用户不存在，无法修改个人信息");
        }

        user.setEmail(email);
        updateByPrimaryKey(user);
    }

    @Override
    public UserInfoDTO getUserInfo(String userId) {
        UserInfoDTO userInfo = new UserInfoDTO();
        User user = selectByPrimaryKey(userId);
        userInfo.setUserId(user.getId());
        userInfo.setPhone(user.getPhone());
        userInfo.setEmail(user.getEmail());
        userInfo.setIsSubAccount(0);
        if (!StringUtils.isBlank(user.getParentId())) {
            userInfo.setIsSubAccount(1);
            List<SubUserPermListDTO> subPermList = roleService.listPermsStatusByUserId(userId);
            userInfo.setPermList(subPermList);
        }

        return userInfo;
    }

    @Override
    public Map<String, String> checkAccount(RequestDTO requestDTO) {
        User user = checkPhoneAndEmail(requestDTO);
        Map<String, String> result = new HashMap<>();
        result.put("code", "200");
        result.put("msg", "验证成功");
        result.put("phone", user.getPhone());
        result.put("email", user.getEmail());
        return result;
    }

    private User checkPhoneAndEmail(RequestDTO requestDTO) {
        String phone = requestDTO.get("phone").toString();
        if (StringUtils.isBlank(phone)) {
            throw new ServiceException(ReturnCode.PARAM_EMPTY, ReturnCode.PARAM_EMPTY.getMsg() + "，phone不能为空");
        }
        User user = new User();
        user.setPhone(phone);
        user = selectOne(user);
        if (null == user) {
            throw new ServiceException(ReturnCode.USER_ERROR, "手机号未注册");
        }
        if (StringUtils.isBlank(user.getEmail())) {
            throw new ServiceException(ReturnCode.USER_ERROR, "手机号未绑定邮箱");
        }
        return user;
    }

    @Override
    @Transactional
    public ResponseDTO sendVerifiCode(RequestDTO requestDTO) {
        User user = checkPhoneAndEmail(requestDTO);

        if (!StringUtils.equals(requestDTO.get("email").toString(), user.getEmail())) {
            throw new ServiceException(ReturnCode.USER_ERROR, "邮箱与注册时绑定邮箱不一致");
        }
        ForgotPassword today = new ForgotPassword();
        today.setPhone(user.getPhone());
        List<ForgotPassword> todayForgotPasswords = forgotPasswordMapper.select(today);

        List<ForgotPassword> collect = todayForgotPasswords.stream().filter(
                forgotPassword ->
                {
                    String oldTime = DateUtil.formatDateToString(forgotPassword.getCreatetime(), "yyyy-MM-dd");
                    String nowTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    return oldTime.equals(nowTime);
                }).collect(Collectors.toList());
        if (collect.size() >= 3) {
            throw new ServiceException(ReturnCode.SERVICE_ERROR, String.format("已忘记密码三次，该服务今日已锁定，请联系奇信智能科技有限公司人员"));
        }

        String token = IDUtils.getUUIDNoBar().substring(0, 6);
        String text = "<div style=\"background:#fff;border:1px solid #ccc;margin:2%;padding:0 30px\">\n" +
                "<div style=\"line-height:40px;height:40px\">&nbsp;</div>\n" +
                "<p style=\"margin:0;padding:0;font-size:14px;line-height:30px;color:#333;font-family:arial,sans-serif;font-weight:bold\">尊敬的奇信物联网云平台用户：</p>\n" +
                "<div style=\"line-height:20px;height:20px\">&nbsp;</div>\n" +
                "<p style=\"margin:0;padding:0;line-height:30px;font-size:14px;color:#333;font-family:'宋体',arial,sans-serif\">您好！感谢您使用奇信物联网云平台，您的账号（" + StringHandleUtil.hidePhone(user.getPhone()) + "）正在进行邮箱认证，本次请求的验证码为：</p>\n" +
                "<p style=\"margin:0;padding:0;line-height:30px;font-size:14px;color:#333;font-family:'宋体',arial,sans-serif\"><b style=\"font-size:18px;color:#f90\">" + token + "</b><span style=\"margin:0;padding:0;margin-left:10px;line-height:30px;font-size:14px;color:#979797;font-family:'宋体',arial,sans-serif\">(为了保障您的账号安全，此验证码15分钟内有效，请在15分钟内完成验证。)</span></p>\n" +
                "<div style=\"line-height:80px;height:80px\">&nbsp;</div>\n" +
                "<p style=\"margin:0;padding:0;line-height:30px;font-size:14px;color:#333;font-family:'宋体',arial,sans-serif\">深圳市奇信智能科技有限公司</p>\n" +
                "<p style=\"margin:0;padding:0;line-height:30px;font-size:14px;color:#333;font-family:'宋体',arial,sans-serif\">" + DateUtil.formatDateToString(new Date(), "yyyy年MM月dd日") + "</p>\n" +
                "<div style=\"line-height:20px;height:20px\">&nbsp;</div>\n" +
                "</div>";
        MailDTO dto = MailDTO.builder().to(user.getEmail()).subject("奇信账号-邮箱安全认证").text(text).build();
        ResponseDTO responseDTO = mailService.sendHtmlMail(dto);
        if (responseDTO.isOk()) {
            saveForgotPassword(user, token);
        }
        return responseDTO;
    }

    private void saveForgotPassword(User user, String token) {
        //更新找回密码无效
        ForgotPassword forgotten = new ForgotPassword();
        forgotten.setPhone(user.getPhone());
        forgotten.setStatus(ForgotStatusEnum.NORMAL.getCode());
        List<ForgotPassword> forgotPasswords = forgotPasswordMapper.select(forgotten);
        forgotPasswords.stream().forEach(forgotPassword -> {
            forgotPassword.setStatus(ForgotStatusEnum.EXPIRY.getCode());
            forgotPasswordMapper.updateByPrimaryKey(forgotPassword);
        });

        //保存找回密码
        ForgotPassword forgotPassword = new ForgotPassword();
        forgotPassword.setId(IDUtils.getUUIDNoBar());
        forgotPassword.setUserId(user.getId());
        forgotPassword.setAccount(user.getAccount());
        forgotPassword.setPhone(user.getPhone());
        forgotPassword.setStatus(ForgotStatusEnum.NORMAL.getCode());

        LocalDateTime time = LocalDateTime.now();
        String now = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime dateTime = time.plusMinutes(15);
        String failure = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        forgotPassword.setFailuretime(DateUtil.strToDate(failure));
        forgotPassword.setCreatetime(DateUtil.strToDate(now));
        forgotPassword.setToken(token);
        forgotPassword.setEmail(user.getEmail());
        forgotPasswordMapper.insert(forgotPassword);
    }

    @Override
    @Transactional
    public User checkVerifiCode(RequestDTO requestDTO) {
        User user = checkPhoneAndEmail(requestDTO);

        ForgotPassword forgotPassword = new ForgotPassword();
        forgotPassword.setPhone(user.getPhone());
        forgotPassword.setStatus(ForgotStatusEnum.NORMAL.getCode());
        forgotPassword = forgotPasswordMapper.selectOne(forgotPassword);
        if (null == forgotPassword) {
            throw new ServiceException("找回密码未登记，请核实");
        }
        //校验验证码是否超时
        Date failure = forgotPassword.getFailuretime();
        Date now = new Date();
        if (now.compareTo(failure) > 0) {
            throw new ServiceException("验证码超时，请发送新验证码");
        }
        //验证错误
        if (!forgotPassword.getToken().equals(requestDTO.get("code").toString())) {
            throw new ServiceException("验证码错误，请确认验证码");
        }
        //更新找回密码记录
        forgotPassword.setStatus(ForgotStatusEnum.EXPIRY.getCode());
        forgotPasswordMapper.updateByPrimaryKey(forgotPassword);

        return user;
    }

    @Override
    public void sendInitialPassword(User user) {
        CompletableFuture.runAsync(() -> {

            Date failure = user.getFailuretime();
            if (failure != null && new Date().after(failure)) {
                throw new ServiceException("用户已失效");
            }

            String status = user.getStatus();
            if (status.equals(UserStatusEnum.DISABLE.getCode())) {
                throw new ServiceException("用户已禁用");
            }

            String email = user.getEmail();
            if (StringUtils.isBlank(email)) {
                throw new ServiceException("email为空");
            }

            String password = IDUtils.getUUIDNoBar().substring(0, 8);
            String text = "<div style=\"background:#fff;border:1px solid #ccc;margin:2%;padding:0 30px\">\n" +
                    "<div style=\"line-height:40px;height:40px\">&nbsp;</div>\n" +
                    "<p style=\"margin:0;padding:0;font-size:14px;line-height:30px;color:#333;font-family:arial,sans-serif;font-weight:bold\">尊敬的奇信物联网云平台用户：</p>\n" +
                    "<div style=\"line-height:20px;height:20px\">&nbsp;</div>\n" +
                    "<p style=\"margin:0;padding:0;line-height:30px;font-size:14px;color:#333;font-family:'宋体',arial,sans-serif\">您好！感谢您注册使用奇信物联网云平台，您的账号（" + StringHandleUtil.hidePhone(user.getPhone()) + "）正在进行邮箱认证，请妥善保管您的初始密码：</p>\n" +
                    "<p style=\"margin:0;padding:0;line-height:30px;font-size:14px;color:#333;font-family:'宋体',arial,sans-serif\"><b style=\"font-size:18px;color:#f90\">" + password + "</b><span style=\"margin:0;padding:0;margin-left:10px;line-height:30px;font-size:14px;color:#979797;font-family:'宋体',arial,sans-serif\">(为了保障您的账号安全，请在登录成功后到【个人信息-密码设置】中修改密码。)</span></p>\n" +
                    "<div style=\"line-height:80px;height:80px\">&nbsp;</div>\n" +
                    "<p style=\"margin:0;padding:0;line-height:30px;font-size:14px;color:#333;font-family:'宋体',arial,sans-serif\">深圳市奇信智能科技有限公司</p>\n" +
                    "<p style=\"margin:0;padding:0;line-height:30px;font-size:14px;color:#333;font-family:'宋体',arial,sans-serif\">" + DateUtil.formatDateToString(new Date(), "yyyy年MM月dd日") + "</p>\n" +
                    "<div style=\"line-height:20px;height:20px\">&nbsp;</div>\n" +
                    "</div>";
            MailDTO dto = MailDTO.builder().to(email).subject("奇信账号-邮箱安全认证").text(text).build();
            ResponseDTO res = mailService.sendHtmlMail(dto);
            if (res.isOk()) {
                user.setPassword(DigestUtils.md5Hex(AESUtils.encrypt(password)));
                boolean update = updateByPrimaryKey(user);
                if (!update) {
                    throw new ServiceException("修改用户密码失败");
                }
            }
        }, threadPoolTaskExecutor).exceptionally(e -> {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        });
    }
}
