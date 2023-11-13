package com.atguigu.spzx.common.redis;

import lombok.Data;

@Data
public class RedisKey {

    public String validateCodeKey(String codeKey) {
        return "user:login:validatecode:" + codeKey;
    }

    public String loginKey(String token) {
        return "user:login:" + token;
    }
}
