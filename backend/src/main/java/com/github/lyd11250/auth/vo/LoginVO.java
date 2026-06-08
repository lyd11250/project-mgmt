package com.github.lyd11250.auth.vo;

import lombok.Data;

/**
 * 登录结果：令牌名与令牌值。
 */
@Data
public class LoginVO {

    private String tokenName;

    private String token;

    public LoginVO(String tokenName, String token) {
        this.tokenName = tokenName;
        this.token = token;
    }
}
