package org.openstack4j.openstack.identity.v3.internal;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** OAuth 1.0a HMAC-SHA1 signing (RFC 5849) for Keystone OS-OAUTH1. */
public final class OAuth1Signer {

    private OAuth1Signer() {
    }

    /** Percent-encodes per RFC 3986: unreserved characters stay, everything else is %XX (upper case). */
    public static String encode(String value) {
        StringBuilder out = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xFF);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '.' || c == '_' || c == '~')
                out.append(c);
            else
                out.append('%').append(String.format("%02X", b & 0xFF));
        }
        return out.toString();
    }

    /** Scheme and host lower case, default port dropped, no query or fragment (RFC 5849 3.4.1.2). */
    static String baseUrl(String url) {
        URI uri = URI.create(url);
        String scheme = uri.getScheme().toLowerCase();
        int port = uri.getPort();
        boolean defaultPort = port == -1 || ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        return scheme + "://" + uri.getHost().toLowerCase() + (defaultPort ? "" : ":" + port) + uri.getRawPath();
    }

    public static String signature(String method, String url, Map<String, String> oauthParams, String consumerSecret, String tokenSecret) {
        StringJoiner params = new StringJoiner("&");
        Map<String, String> sorted = new TreeMap<>();
        oauthParams.forEach((k, v) -> sorted.put(encode(k), encode(v)));
        sorted.forEach((k, v) -> params.add(k + "=" + v));
        String base = method.toUpperCase() + "&" + encode(baseUrl(url)) + "&" + encode(params.toString());
        String key = encode(consumerSecret) + "&" + encode(tokenSecret == null ? "" : tokenSecret);
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(base.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA1 is not available", e);
        }
    }

    public static String header(Map<String, String> oauthParams, String signature) {
        StringJoiner header = new StringJoiner(", ", "OAuth ", "");
        oauthParams.forEach((k, v) -> header.add(encode(k) + "=\"" + encode(v) + "\""));
        header.add("oauth_signature=\"" + encode(signature) + "\"");
        return header.toString();
    }
}
