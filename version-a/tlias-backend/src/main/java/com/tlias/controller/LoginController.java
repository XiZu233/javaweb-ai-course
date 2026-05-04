package com.tlias.controller;

import com.tlias.mapper.EmpMapper;
import com.tlias.pojo.Emp;
import com.tlias.pojo.Result;
import com.tlias.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private EmpMapper empMapper;

    @PostMapping
    public Result login(@RequestBody Emp emp) {
        log.info("员工登录：{}", emp.getUsername());

        // 教学简化版：直接通过用户名查询验证
        // 实际生产环境应使用加密密码对比
        Emp e = empMapper.getById(1); // 简化处理

        // 生成JWT令牌
        String jwt = JwtUtils.generateToken(1, emp.getUsername());
        return Result.success(jwt);
    }
}
