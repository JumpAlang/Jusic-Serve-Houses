package com.scoder.jusic.security;

import com.scoder.jusic.configuration.TrustedProxyProperties;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * Resolves the first valid forwarded client address, then X-Real-IP, then the
 * network peer. Forwarded headers are supplied by the deployment's proxy.
 */
@Component
public class ClientIpResolver {

    private static final String UNKNOWN = "unknown";

    private final TrustedProxyProperties properties;

    public ClientIpResolver(TrustedProxyProperties properties) {
        this.properties = properties;
    }

    public String resolve(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest) {
            return resolve(((ServletServerHttpRequest) request).getServletRequest());
        }
        if (request != null && request.getRemoteAddress() != null) {
            return normalizeAddressLiteral(
                    request.getRemoteAddress().getAddress().getHostAddress());
        }
        return UNKNOWN;
    }

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String peer = normalizeAddressLiteral(request.getRemoteAddr());
        if (!properties.isUseForwardHeaders()) {
            return peer;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            String[] chain = forwardedFor.split(",");
            for (String address : chain) {
                String candidate = normalizeAddressLiteral(address);
                if (!UNKNOWN.equals(candidate)) {
                    return candidate;
                }
            }
        }

        String realIp = normalizeAddressLiteral(request.getHeader("X-Real-IP"));
        return !UNKNOWN.equals(realIp) ? realIp : peer;
    }

    /**
     * Parses and canonicalizes an IPv4/IPv6 literal without calling a name
     * service. Canonicalization also prevents one address from obtaining
     * several rate-limit identities by changing zero compression or padding.
     */
    private String normalizeAddressLiteral(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        String candidate = value.trim();
        if (candidate.startsWith("[") && candidate.endsWith("]")) {
            candidate = candidate.substring(1, candidate.length() - 1);
        }
        candidate = normalize(candidate);
        if (isIpv4Literal(candidate)) {
            return canonicalIpv4(candidate);
        }
        int[] ipv6 = parseIpv6(candidate);
        return ipv6 == null ? UNKNOWN : canonicalIpv6(ipv6);
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UNKNOWN;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        int zoneIndex = normalized.indexOf('%');
        return zoneIndex < 0 ? normalized : normalized.substring(0, zoneIndex);
    }

    private boolean isIpv4Literal(String value) {
        if (value == null || value.length() < 7 || value.length() > 15) {
            return false;
        }
        String[] octets = value.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }
        for (String octet : octets) {
            if (octet.isEmpty() || octet.length() > 3
                    || (octet.length() > 1 && octet.charAt(0) == '0')) {
                return false;
            }
            int number = 0;
            for (int index = 0; index < octet.length(); index++) {
                char character = octet.charAt(index);
                if (character < '0' || character > '9') {
                    return false;
                }
                number = number * 10 + character - '0';
            }
            if (number > 255) {
                return false;
            }
        }
        return true;
    }

    private String canonicalIpv4(String value) {
        String[] octets = value.split("\\.");
        return Integer.parseInt(octets[0]) + "."
                + Integer.parseInt(octets[1]) + "."
                + Integer.parseInt(octets[2]) + "."
                + Integer.parseInt(octets[3]);
    }

    private int[] parseIpv6(String value) {
        if (value == null || value.isEmpty() || value.length() > 64
                || value.indexOf(':') < 0) {
            return null;
        }
        String expanded = expandEmbeddedIpv4(value);
        if (expanded == null || expanded.indexOf("::") != expanded.lastIndexOf("::")) {
            return null;
        }

        int compression = expanded.indexOf("::");
        String leftText = compression < 0 ? expanded : expanded.substring(0, compression);
        String rightText = compression < 0 ? "" : expanded.substring(compression + 2);
        String[] left = leftText.isEmpty() ? new String[0] : leftText.split(":", -1);
        String[] right = rightText.isEmpty() ? new String[0] : rightText.split(":", -1);
        int explicitParts = left.length + right.length;
        if ((compression < 0 && explicitParts != 8)
                || (compression >= 0 && explicitParts >= 8)) {
            return null;
        }

        int[] result = new int[8];
        int target = 0;
        for (String part : left) {
            int parsed = parseHextet(part);
            if (parsed < 0) {
                return null;
            }
            result[target++] = parsed;
        }
        target += 8 - explicitParts;
        for (String part : right) {
            int parsed = parseHextet(part);
            if (parsed < 0) {
                return null;
            }
            result[target++] = parsed;
        }
        return result;
    }

    private String expandEmbeddedIpv4(String value) {
        if (value.indexOf('.') < 0) {
            return value;
        }
        int separator = value.lastIndexOf(':');
        if (separator < 0 || separator == value.length() - 1) {
            return null;
        }
        String ipv4 = value.substring(separator + 1);
        if (!isIpv4Literal(ipv4)) {
            return null;
        }
        String[] octets = ipv4.split("\\.");
        int high = Integer.parseInt(octets[0]) * 256 + Integer.parseInt(octets[1]);
        int low = Integer.parseInt(octets[2]) * 256 + Integer.parseInt(octets[3]);
        return value.substring(0, separator + 1)
                + Integer.toHexString(high) + ":" + Integer.toHexString(low);
    }

    private int parseHextet(String value) {
        if (value.isEmpty() || value.length() > 4) {
            return -1;
        }
        int result = 0;
        for (int index = 0; index < value.length(); index++) {
            int digit = Character.digit(value.charAt(index), 16);
            if (digit < 0) {
                return -1;
            }
            result = result * 16 + digit;
        }
        return result;
    }

    private String canonicalIpv6(int[] parts) {
        int bestStart = -1;
        int bestLength = 0;
        for (int index = 0; index < parts.length;) {
            if (parts[index] != 0) {
                index++;
                continue;
            }
            int end = index;
            while (end < parts.length && parts[end] == 0) {
                end++;
            }
            int length = end - index;
            if (length >= 2 && length > bestLength) {
                bestStart = index;
                bestLength = length;
            }
            index = end;
        }

        StringBuilder result = new StringBuilder();
        for (int index = 0; index < parts.length;) {
            if (index == bestStart) {
                result.append("::");
                index += bestLength;
                continue;
            }
            if (result.length() > 0 && result.charAt(result.length() - 1) != ':') {
                result.append(':');
            }
            result.append(Integer.toHexString(parts[index++]));
        }
        return result.length() == 0 ? "::" : result.toString();
    }
}
