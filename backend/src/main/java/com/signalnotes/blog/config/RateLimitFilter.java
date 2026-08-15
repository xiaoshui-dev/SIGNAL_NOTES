package com.signalnotes.blog.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    @Override protected boolean shouldNotFilter(HttpServletRequest request) { String path=request.getRequestURI(); return !"POST".equals(request.getMethod()) || !(path.equals("/api/comments")||path.matches("/api/comments/\\d+/report")||path.equals("/api/contact")||path.equals("/api/subscriptions")); }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException { long minute=Instant.now().getEpochSecond()/60;String key=request.getRemoteAddr()+":"+request.getRequestURI();Window value=windows.compute(key,(k,current)->current==null||current.minute()!=minute?new Window(minute,1):new Window(minute,current.count()+1));if(value.count()>15){response.setStatus(429);response.setContentType(MediaType.APPLICATION_JSON_VALUE);response.getWriter().write("{\"status\":429,\"message\":\"操作过于频繁，请一分钟后重试\"}");return;}chain.doFilter(request,response); }
    private record Window(long minute,int count) {}
}
