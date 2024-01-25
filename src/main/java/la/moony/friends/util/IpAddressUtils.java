//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package la.moony.friends.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.server.ServerRequest;

public class IpAddressUtils {
    private static final String UNKNOWN = "unknown";
    private static final String X_REAL_IP = "X-Real-IP";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String PROXY_CLIENT_IP = "Proxy-Client-IP";
    private static final String WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";
    private static final String HTTP_CLIENT_IP = "HTTP_CLIENT_IP";
    private static final String HTTP_X_FORWARDED_FOR = "HTTP_X_FORWARDED_FOR";

    public IpAddressUtils() {
    }

    public static String getIpAddress(ServerRequest request) {
        try {
            return getIpAddressInternal(request);
        } catch (Exception var2) {
            return "unknown";
        }
    }

    private static String getIpAddressInternal(ServerRequest request) {
        HttpHeaders httpHeaders = request.headers().asHttpHeaders();
        String xrealIp = httpHeaders.getFirst("X-Real-IP");
        String xforwardedFor = httpHeaders.getFirst("X-Forwarded-For");
        if (StringUtils.isNotEmpty(xforwardedFor) && !"unknown".equalsIgnoreCase(xforwardedFor)) {
            int index = xforwardedFor.indexOf(",");
            return index != -1 ? xforwardedFor.substring(0, index) : xforwardedFor;
        } else {
            xforwardedFor = xrealIp;
            if (StringUtils.isNotEmpty(xrealIp) && !"unknown".equalsIgnoreCase(xrealIp)) {
                return xrealIp;
            } else {
                if (StringUtils.isBlank(xrealIp) || "unknown".equalsIgnoreCase(xrealIp)) {
                    xforwardedFor = httpHeaders.getFirst("Proxy-Client-IP");
                }

                if (StringUtils.isBlank(xforwardedFor) || "unknown".equalsIgnoreCase(xforwardedFor)) {
                    xforwardedFor = httpHeaders.getFirst("WL-Proxy-Client-IP");
                }

                if (StringUtils.isBlank(xforwardedFor) || "unknown".equalsIgnoreCase(xforwardedFor)) {
                    xforwardedFor = httpHeaders.getFirst("HTTP_CLIENT_IP");
                }

                if (StringUtils.isBlank(xforwardedFor) || "unknown".equalsIgnoreCase(xforwardedFor)) {
                    xforwardedFor = httpHeaders.getFirst("HTTP_X_FORWARDED_FOR");
                }

                if (StringUtils.isBlank(xforwardedFor) || "unknown".equalsIgnoreCase(xforwardedFor)) {
                    xforwardedFor = (String)request.remoteAddress().map((remoteAddress) -> {
                        return remoteAddress.getAddress().getHostAddress();
                    }).orElse("unknown");
                }

                return xforwardedFor;
            }
        }
    }
}
