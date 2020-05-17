package com.scoder.jusic.util;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketSession;

import javax.servlet.http.HttpServletRequest;

/**
 * @author H
 */
public class IPUtils {

    public static final String IP = "0:0:0:0:0:0:0:1";

    public static String getRemoteAddress(ServerHttpRequest request) {
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        return getRemoteAddress(servletRequest);
    }

    public static String getRemoteAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        System.out.println("x-forwarded-for ip: " + ip);
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个ip值，第一个ip才是真实ip
            if (ip.indexOf(",") != -1) {
                ip = ip.split(",")[0];
            }
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    public static String getIPV4(WebSocketSession session) {
        try {
            String s = session.getAttributes().get("remoteAddress").toString();
            if (s.matches(".*?:.*?")) {
                return "127.0.0.1";
            } else {
                StringBuilder result = new StringBuilder();
                char[] chars = s.toCharArray();
                for (char aChar : chars) {
                    String s1 = String.valueOf(aChar);
                    if (s1.matches("\\d") || s1.matches("\\.")) {
                        result.append(s1);
                    }
                }
                return result.toString();
            }
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

}