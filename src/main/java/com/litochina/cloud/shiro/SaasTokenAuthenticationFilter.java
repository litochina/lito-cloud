package com.litochina.cloud.shiro;

import com.litochina.base.common.common.BaseConstant;
import com.litochina.base.common.common.ReturnCode;
import com.litochina.base.common.controller.ApiRespBuilder;
import com.litochina.base.common.utils.JsonUtils;
import com.litochina.base.common.utils.SystemConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.Asserts;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Slf4j
public class SaasTokenAuthenticationFilter extends BasicHttpAuthenticationFilter {

    private AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) throws UnauthorizedException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String[] anonUrl = StringUtils.splitByWholeSeparatorPreserveAllTokens(SystemConfigUtil.getProperty("system.anonUrl"), ",");
        Asserts.notNull(anonUrl, "缺少配置 system.anonUrl");

        String requestURI = httpServletRequest.getServletPath();

        for (String url : anonUrl) {
            if (pathMatcher.match(url, requestURI) || requestURI.contains("layui"))
                return true;
        }

        if (isLoginAttempt(request, response)) {
            return executeLogin(request, response);
        }
        return false;
    }

    @Override
    protected boolean isLoginAttempt(ServletRequest request, ServletResponse response) {
        HttpServletRequest req = (HttpServletRequest) request;
        String token = req.getHeader(BaseConstant.HEADER_AUTHENTICATION_KEY);
        return token != null;
    }

    @Override
    protected boolean executeLogin(ServletRequest request, ServletResponse response) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String token = httpServletRequest.getHeader(BaseConstant.HEADER_AUTHENTICATION_KEY);

        SaasAuthToken authToken = new SaasAuthToken(token);
        try {
            getSubject(request, response).login(authToken);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }

    /**
     * 对跨域提供支持
     */
    @Override
    protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        httpServletResponse.setHeader("Access-control-Allow-Origin", httpServletRequest.getHeader("Origin"));
        httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS,PUT,DELETE");
        httpServletResponse.setHeader("Access-Control-Allow-Headers", httpServletRequest.getHeader("Access-Control-Request-Headers"));
        // 跨域时会首先发送一个 option请求，这里我们给 option请求直接返回正常状态
        if (httpServletRequest.getMethod().equals(RequestMethod.OPTIONS.name())) {
            httpServletResponse.setStatus(HttpStatus.OK.value());
            return false;
        }
        return super.preHandle(request, response);
    }

    @Override
    protected boolean sendChallenge(ServletRequest request, ServletResponse response) {
        log.debug("Authentication required: sending 401 Authentication challenge response.");
        HttpServletResponse httpResponse = WebUtils.toHttp(response);
        httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        httpResponse.setCharacterEncoding("utf-8");
        httpResponse.setContentType("application/json; charset=utf-8");
        try (PrintWriter out = httpResponse.getWriter()) {
            String responseJson = JsonUtils.toJson(ApiRespBuilder.error(ReturnCode.LOGIN_OVERTIME));
            out.print(responseJson);
        } catch (IOException e) {
            log.error("sendChallenge error：", e);
        }
        return false;
    }
}
