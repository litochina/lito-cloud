package com.litochina.cloud.shiro;

import lombok.Data;
import org.apache.shiro.authc.AuthenticationToken;

/**
 *
 * @author chenxx
 * @since 2019/09/20
 **/
@Data
public class SaasAuthToken implements AuthenticationToken {

    private static final long serialVersionUID = 1282057025599826155L;

    private String token;

    private String exipreAt;

    public SaasAuthToken(String token) {
        this.token = token;
    }

    public SaasAuthToken(String token, String exipreAt) {
        this.token = token;
        this.exipreAt = exipreAt;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

}
