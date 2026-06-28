package com.clinic.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        // 对于AI API路径，跳过此过滤器的处理
        if (requestURI.startsWith("/api/ai/")) {
            log.debug("AI API路径，跳过JWT认证过滤器: {}", requestURI);
            return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        
        // 对于AI API路径，直接放行，由Controller自行验证API Key
        if (requestURI.startsWith("/api/ai/")) {
            log.debug("AI API路径，跳过JWT认证: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        // 尝试获取Authorization头（支持大小写）
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader)) {
            authHeader = request.getHeader("authorization");
        }
        
        log.info("请求URI: {}, Authorization头: {}", requestURI, authHeader != null ? authHeader.substring(0, Math.min(20, authHeader.length())) + "..." : "null");

        String token = null;
        if (StringUtils.hasText(authHeader)) {
            if (authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                log.info("从Bearer提取token: {}", token.substring(0, Math.min(20, token.length())) + "...");
            } else {
                // 直接就是token（小程序端可能自动去掉了Bearer前缀）
                token = authHeader;
                log.info("直接使用token: {}", token.substring(0, Math.min(20, token.length())) + "...");
            }
        }
        
        if (StringUtils.hasText(token)) {

            boolean isValid = jwtUtil.validateToken(token);
            log.info("Token验证结果: {}", isValid);
            
            if (isValid) {
                Long userId = jwtUtil.getUserId(token);
                String username = jwtUtil.getUsername(token);
                Integer clinicId = jwtUtil.getClinicId(token);
                String realName = jwtUtil.getRealName(token);
                
                log.info("Token解析结果: userId={}, username={}, clinicId={}", userId, username, clinicId);

                if (userId != null && username != null) {
                    // 创建 UserDetailsImpl 对象作为 principal
                    UserDetailsImpl userDetails = new UserDetailsImpl(
                            userId,
                            username,
                            "", // token验证时不需要密码
                            clinicId != null ? clinicId.longValue() : null,
                            realName != null ? realName : username,
                            Collections.emptyList()
                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("认证成功，用户已设置到SecurityContext");
                } else {
                    log.warn("Token解析结果不完整: userId={}, username={}", userId, username);
                }
            } else {
                log.warn("Token验证失败");
            }
        } else {
            log.warn("Authorization头为空或格式不正确");
        }

        filterChain.doFilter(request, response);
    }
}