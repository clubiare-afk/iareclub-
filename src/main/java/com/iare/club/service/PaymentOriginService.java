package com.iare.club.service;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentOriginService {
    private final Set<String> allowed;
    private final String fallback;

    public PaymentOriginService(@Value("${moneymint.payment.allowed-return-origins}") String origins,
                                @Value("${moneymint.site-origin}") String fallback) {
        LinkedHashSet<String> configured = new LinkedHashSet<>();
        for (String origin : origins.split(",")) configured.add(normalize(origin));
        this.allowed = Set.copyOf(configured);
        this.fallback = normalize(fallback);
        if (!allowed.contains(this.fallback)) throw new IllegalStateException("Money Mint site origin must be in the payment return-origin allowlist");
    }

    public String trustedOrigin(HttpServletRequest request) {
        String forwardedProto = first(request.getHeader("X-Forwarded-Proto"));
        String forwardedHost = first(request.getHeader("X-Forwarded-Host"));
        String scheme = forwardedProto == null ? request.getScheme() : forwardedProto;
        String host = forwardedHost == null ? request.getHeader("Host") : forwardedHost;
        if (host == null || host.isBlank()) host = request.getServerName() + port(request);
        try {
            String candidate = normalize(scheme + "://" + host);
            return allowed.contains(candidate) ? candidate : fallback;
        } catch (IllegalArgumentException ignored) { return fallback; }
    }

    public String safeStoredOrigin(String origin) {
        if (origin == null || origin.isBlank()) return fallback;
        try { String normalized=normalize(origin);return allowed.contains(normalized)?normalized:fallback; }
        catch (IllegalArgumentException ignored) { return fallback; }
    }

    private static String first(String value) { return value == null ? null : value.split(",", 2)[0].strip(); }
    private static String port(HttpServletRequest request) { int port=request.getServerPort();return port==80||port==443?"":":"+port; }
    private static String normalize(String raw) {
        URI uri=URI.create(raw.strip().replaceAll("/+$", ""));
        if (uri.getScheme()==null||uri.getHost()==null||uri.getUserInfo()!=null||uri.getPath()!=null&&!uri.getPath().isEmpty()||uri.getQuery()!=null||uri.getFragment()!=null)
            throw new IllegalArgumentException("Invalid payment origin");
        String scheme=uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http")&&!scheme.equals("https")) throw new IllegalArgumentException("Invalid payment origin scheme");
        int port=uri.getPort();return scheme+"://"+uri.getHost().toLowerCase(Locale.ROOT)+(port<0?"":":"+port);
    }
}
