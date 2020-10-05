package com.litochina.cloud.controller;

import com.litochina.base.common.common.BaseConstant;
import com.litochina.base.common.controller.ApiRespBuilder;
import com.litochina.base.common.controller.CurrentUserContext;
import com.litochina.base.common.dto.CurrentUserDTO;
import com.litochina.base.common.dto.RequestDTO;
import com.litochina.base.common.dto.ResponseDTO;
import com.litochina.base.common.utils.VerifyCodeUtils;
import com.litochina.base.model.dto.CookieInfoDTO;
import com.litochina.base.model.dto.SolutionSystemUserDTO;
import com.litochina.base.model.dto.UserPrivilegesDTO;
import com.litochina.base.model.entity.common.VerifiCode;
import com.litochina.cloud.dto.UserInfoDTO;
import com.litochina.cloud.service.RoleService;
import com.litochina.cloud.service.UserService;
import com.litochina.cloud.service.VerifiCodeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private VerifiCodeService verifiCodeService;

    @Resource
    private RoleService roleService;

    @PostMapping(path = "/login")
    public ResponseDTO login(@RequestBody RequestDTO requestDTO, HttpServletRequest request) {
        String ipAddress = request.getHeader("x-forwarded-for");
        request.getSession().setAttribute(BaseConstant.USER_IP, ipAddress);
        return ApiRespBuilder.success("登录成功", userService.login(requestDTO));
    }

    @PostMapping(path = "/uncheck_login")
    public Map<String, String> uncheckLogin(@RequestBody RequestDTO requestDTO) {
        UserPrivilegesDTO dto = userService.login(requestDTO);
        Map<String, String> result = new HashMap<>();
        result.put("code", "200");
        result.put("msg", "登录成功");
        result.put("id", dto.getUser().getId());
        result.put("phone", dto.getUser().getPhone());
        result.put("token", dto.getTimToken());
        return result;
    }

    @PostMapping(path = "/refresh_token")
    public Map<String, String> refreshUnCheckLoginToken(@RequestBody RequestDTO requestDTO) {
        String phone = requestDTO.getParam("phone");
        UserPrivilegesDTO dto = userService.refreshToken(phone);
        Map<String, String> result = new HashMap<>();
        result.put("code", "200");
        result.put("msg", "登录成功");
        result.put("id", dto.getUser().getId());
        result.put("phone", dto.getUser().getPhone());
        result.put("token", dto.getTimToken());
        return result;
    }

    @PostMapping(path = "/register")
    public ResponseDTO register(@RequestBody RequestDTO requestDTO) {
        return ApiRespBuilder.success("注册成功", userService.register(requestDTO));
    }

    @PostMapping(path = "/changePwd")
    public ResponseDTO changePwd(@RequestBody RequestDTO requestDTO) {
        String userId = CurrentUserContext.checkLoginAndGetUserId();
        String oldPwd = requestDTO.getParam("oldPwd");
        String newPwd = requestDTO.getParam("newPwd");
        String confirmPwd = requestDTO.getParam("confirmPwd");
        userService.changePwd(userId, oldPwd, newPwd, confirmPwd);
        return ApiRespBuilder.success("用户密码修改成功");
    }

    @PostMapping(path = "/uncheck_changePwd")
    public ResponseDTO uncheckChangePwd(@RequestBody RequestDTO requestDTO) {
        String userId = requestDTO.getParam("userId");
        String newPwd = requestDTO.getParam("newPwd");
        String confirmPwd = requestDTO.getParam("confirmPwd");
        String code = requestDTO.getParam("code").toUpperCase();
        String timekey = requestDTO.get("timekey").toString();
        userService.changePwd(userId, newPwd, confirmPwd, code, timekey);
        return ApiRespBuilder.success("用户密码修改成功");
    }

    @PostMapping(path = "/update/info")
    public ResponseDTO editUserInfo(@RequestBody RequestDTO requestDTO) {
        String userId = CurrentUserContext.checkLoginAndGetUserId();
        String email = requestDTO.getParam("email");
        userService.updateUserInfo(userId, email);
        return ApiRespBuilder.success("个人信息修改成功");
    }

    @PostMapping(path = "/exit")
    public ResponseDTO exit() {
        return ApiRespBuilder.success("退出成功", userService.exit());
    }

    @PostMapping(path = "/tim_exit")
    public ResponseDTO ThreeTimExit() {
        userService.threeTimExit();
        return ApiRespBuilder.success("登出成功");
    }

    @GetMapping(path = "/getCode")
    public ResponseDTO getCode(HttpServletResponse resp, HttpServletRequest req) throws IOException {

        resp.setHeader("Pragma", "No-cache");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setDateHeader("Expires", 0);
        resp.setContentType("image/jpeg");
        //生成随机字串
        String verifyCode = VerifyCodeUtils.generateVerifyCode(4);
        //生成图片
        int width = 100;//宽
        int height = 40;//高
        VerifyCodeUtils.outputImage(width, height, resp.getOutputStream(), verifyCode);
        VerifiCode verifiCode = new VerifiCode();
        verifiCode.setCode(verifyCode);
        verifiCode.setTimekey(req.getParameter("timekey"));
        verifiCode.setCreatetime(new Date());
        verifiCodeService.save(verifiCode);
        return ApiRespBuilder.success(verifiCode);
    }

    @PostMapping("/getSystemsByUserId")
    public ResponseDTO getSystemsByUserId() {
        CurrentUserDTO currentUser = CurrentUserContext.getCurrentUser();
        String userId = currentUser.getUserId();
        boolean isSub = false;
        if (StringUtils.isNotBlank(currentUser.getParentId())) {
            isSub = true;
        }
        List<SolutionSystemUserDTO> systemList = userService.getSystemsByUserId(userId);
        if (isSub) {
            Set<String> systemSet = roleService.listFindSystemPermsByUserId(currentUser.getUserId());
            systemList = systemList.stream().filter(system -> systemSet.contains(system.getSystemDesc()))
                    .collect(Collectors.toList());
        }

        return ApiRespBuilder.success("查询成功", systemList);
    }

    @PostMapping(path = "/info")
    public ResponseDTO getUserInfoList() {
        String userId = CurrentUserContext.checkLoginAndGetUserId();
        UserInfoDTO userInfo = userService.getUserInfo(userId);
        return ApiRespBuilder.success("个人信息查询成功", userInfo);
    }

    @PostMapping(path = "/timekeygetCode")
    public ResponseDTO timekeygetCode(@RequestBody RequestDTO requestDTO) {
        return ApiRespBuilder.success(verifiCodeService.selectver(requestDTO));
    }

    @PostMapping(path = "/cookieInfo/cache")
    public ResponseDTO cacheCookieInfo(@RequestBody CookieInfoDTO cookieInfoDTO) {
        return userService.cacheCookieInfo(cookieInfoDTO);
    }

    @GetMapping(path = "/cookieInfo/get")
    public ResponseDTO cacheCookieInfo() {
        return userService.getCookieInfo();
    }

    @PostMapping(path = "/check_account")
    public Map<String, String> checkAccount(@RequestBody RequestDTO requestDTO) {
        return userService.checkAccount(requestDTO);
    }

    @PostMapping(path = "/sendVerifiCode")
    public ResponseDTO sendVerifiCode(@RequestBody RequestDTO requestDTO) {
        return userService.sendVerifiCode(requestDTO);
    }

    @PostMapping(path = "/checkVerifiCode")
    public ResponseDTO checkVerifiCode(@RequestBody RequestDTO requestDTO) {
        return ApiRespBuilder.success("确认成功", userService.checkVerifiCode(requestDTO));
    }
}
