package com.tlias.aspect;

import com.alibaba.fastjson.JSON;
import com.tlias.mapper.OperateLogMapper;
import com.tlias.pojo.OperateLog;
import com.tlias.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LogAspect {

    @Autowired
    private OperateLogMapper operateLogMapper;

    @Autowired
    private HttpServletRequest request;

    @Around("@annotation(com.tlias.anno.Log)")
    public Object recordLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long begin = System.currentTimeMillis();

        // 执行目标方法
        Object result = joinPoint.proceed();

        long end = System.currentTimeMillis();

        // 记录操作日志
        OperateLog operateLog = new OperateLog();
        operateLog.setOperateEmpId(getCurrentEmpId());
        operateLog.setOperateTime(LocalDateTime.now());
        operateLog.setClassName(joinPoint.getTarget().getClass().getName());
        operateLog.setMethodName(joinPoint.getSignature().getName());
        operateLog.setMethodParams(Arrays.toString(joinPoint.getArgs()));
        operateLog.setReturnValue(JSON.toJSONString(result));
        operateLog.setCostTime(end - begin);

        operateLogMapper.insert(operateLog);
        log.info("操作日志已记录：{}", operateLog);

        return result;
    }

    private Integer getCurrentEmpId() {
        try {
            String token = request.getHeader("token");
            if (token != null) {
                return JwtUtils.getEmpId(token);
            }
        } catch (Exception e) {
            log.warn("获取当前用户ID失败");
        }
        return null;
    }
}
