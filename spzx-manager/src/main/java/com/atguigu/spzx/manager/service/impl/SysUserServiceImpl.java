package com.atguigu.spzx.manager.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.atguigu.spzx.common.exception.GlobalException;
import com.atguigu.spzx.common.redis.RedisKey;
import com.atguigu.spzx.manager.mapper.SysUserMapper;
import com.atguigu.spzx.manager.service.SysUserService;
import com.atguigu.spzx.model.dto.system.LoginDto;
import com.atguigu.spzx.model.entity.system.SysUser;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.atguigu.spzx.model.vo.system.LoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private RedisKey redisKey = new RedisKey();


    @Override
    public LoginVo login(LoginDto loginDto) {

        // 校验验证码是否正确
        String captcha = loginDto.getCaptcha();     // 用户输入的验证码
        String codeKey = loginDto.getCodeKey();     // redis中验证码的数据key

        // 从Redis中获取验证码
        String redisCode = redisTemplate.opsForValue().get(redisKey.validateCodeKey(codeKey));
        if(StrUtil.isEmpty(redisCode) || !StrUtil.equalsIgnoreCase(redisCode , captcha)) {
            throw new GlobalException(ResultCodeEnum.VALIDATECODE_ERROR);
        }

        // 验证通过删除redis中的验证码
        redisTemplate.delete(redisKey.validateCodeKey(codeKey));

        //获取用户名
        String userName = loginDto.getUserName();

        //查user表
        SysUser sysUser = sysUserMapper.selectUserInfoByUserName(userName);

        //判断登录状态
        if (sysUser == null) {
            throw new GlobalException(ResultCodeEnum.LOGIN_ERROR);
        }

        String db_password = sysUser.getPassword();
        String input_password = loginDto.getPassword();

        input_password = DigestUtils.md5DigestAsHex(input_password.getBytes());
        if (!input_password.equals(db_password)) {
           throw new GlobalException(ResultCodeEnum.LOGIN_ERROR);
        }

        //登录成功
        String token = UUID.randomUUID().toString().replaceAll("-", "");

        //放入redis
        redisTemplate.opsForValue().set(redisKey.loginKey(token), JSON.toJSONString(sysUser), 7, TimeUnit.DAYS);

        LoginVo loginVo = new LoginVo();
        loginVo.setToken(token);

        return loginVo;
    }

    @Override
    public SysUser getUserInfo(String token) {
        String userJson = redisTemplate.opsForValue().get(redisKey.loginKey(token));
        return JSON.parseObject(userJson , SysUser.class) ;
    }

    @Override
    public void logout(String token) {
        redisTemplate.delete(redisKey.loginKey(token));
    }
}
