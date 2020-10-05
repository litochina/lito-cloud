package com.litochina.cloud.shiro;

import com.litochina.base.common.common.BaseConstant;
import com.litochina.base.common.common.ReturnCode;
import com.litochina.base.common.controller.CurrentUserContext;
import com.litochina.base.common.dto.CurrentUserDTO;
import com.litochina.base.common.exception.ServiceException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author chenxx
 * @date 2019/9/20/020
 */
public class SaasShiroRealm extends AuthorizingRealm {
    private static final Logger logger = LoggerFactory.getLogger(BaseConstant.LOGGER_NAME_INTERFACE);

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof SaasAuthToken;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        CurrentUserDTO currentUser = CurrentUserContext.getCurrentUserByToken(principalCollection.toString());
        if (null == currentUser) {
            logger.error("login AuthorizationError: {}", "用户token已失效, 无法获取权限");
            throw new ServiceException(ReturnCode.LOGIN_OVERTIME, "当前用户" + ReturnCode.TOKEN_TIMEOUT.getMsg());
        }

        // 获取用户权限集
        SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();
        simpleAuthorizationInfo.setStringPermissions(currentUser.getUserPermissions());
        return simpleAuthorizationInfo;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) {
        String token = (String) authenticationToken.getCredentials();
        CurrentUserDTO currentUser = CurrentUserContext.getCurrentUserByToken(token);
        if (null == currentUser) {
            logger.error("login AuthenticationException: {}", "用户token已失效, 需重新登录");
            throw new ServiceException(ReturnCode.LOGIN_OVERTIME, "当前用户" + ReturnCode.TOKEN_TIMEOUT.getMsg());
        }

        return new SimpleAuthenticationInfo(token, token, this.getName());
    }
}
