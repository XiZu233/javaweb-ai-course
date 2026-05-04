package com.tlias.filter;

import com.alibaba.fastjson.JSON;
import com.tlias.pojo.Result;
import com.tlias.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@WebFilter(urlPatterns = "/*")
public class TokenFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String url = request.getRequestURI();
        log.info("请求URL：{}", url);

        // 登录请求直接放行
        if (url.contains("login")) {
            chain.doFilter(req, res);
            return;
        }

        // 获取token
        String token = request.getHeader("token");
        if (!StringUtils.hasLength(token)) {
            log.info("token为空，返回未登录信息");
            response.getWriter().write(JSON.toJSONString(Result.error("NOT_LOGIN")));
            return;
        }

        // 校验token
        try {
            JwtUtils.parseToken(token);
            chain.doFilter(req, res);
        } catch (Exception e) {
            log.error("token校验失败：{}", e.getMessage());
            response.getWriter().write(JSON.toJSONString(Result.error("NOT_LOGIN")));
        }
    }
}
