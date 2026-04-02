package com.capgemini.book_partner_frontend.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final Map<String, Long> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> timeWindows = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = request.getRemoteAddr();
        long currentTime = System.currentTimeMillis();

        timeWindows.putIfAbsent(ip, currentTime);
        requestCounts.putIfAbsent(ip, 0L);

        // Reset the window every 10 seconds
        if (currentTime - timeWindows.get(ip) > 10000) {
            timeWindows.put(ip, currentTime);
            requestCounts.put(ip, 0L);
        }

        long count = requestCounts.get(ip);
        if (count > 50) { // Limit: 50 requests per 10 seconds
            response.setStatus(429); // 429 Too Many Requests

            // Check if it's an AJAX/Fetch request
            String acceptHeader = request.getHeader("Accept");
            if (acceptHeader != null && acceptHeader.contains("application/json")) {
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Rate limit exceeded. Slow down.\"}");
            } else {
                // If they spam page refresh, show a fallback HTML page
                response.setContentType("text/html");
                response.getWriter().write(
                        "<html><body style='text-align:center; padding-top:10vh; font-family:sans-serif;'>" +
                                "<h2 style='color:#ef4444;'>⚠️ Whoa, Slow Down!</h2>" +
                                "<p>You are requesting pages too fast. Please wait 10 seconds.</p>" +
                                "<button onclick='location.reload()'>Refresh</button>" +
                                "</body></html>"
                );
            }
            return false; // Stop the request from reaching the controller
        }

        requestCounts.put(ip, count + 1);
        return true;
    }
}