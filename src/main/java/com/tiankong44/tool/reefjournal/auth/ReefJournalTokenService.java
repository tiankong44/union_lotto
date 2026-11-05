package com.tiankong44.tool.reefjournal.auth;

import com.tiankong44.tool.reefjournal.config.ReefJournalProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TOKEN_PAYLOAD_SEPARATOR;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TOKEN_SEPARATOR;

/**
 * 为小程序用户签发和校验无状态 HMAC 令牌。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Service
public class ReefJournalTokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final ReefJournalProperties properties;

    /**
     * 创建使用外部签名密钥的令牌服务。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param properties 海缸服务端配置
     */
    public ReefJournalTokenService(ReefJournalProperties properties) {
        this.properties = properties;
    }

    /**
     * 生成跨实例可验证的访问令牌。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 已登录的内部用户编号
     * @return 携带过期时间与签名的 bearer token
     */
    public String issue(Long userId) {
        String key = properties.getTokenSigningKey();
        Long ttl = properties.getTokenTtlSeconds();
        if (key == null || key.length() < 32 || ttl == null || ttl <= 0L) {
            throw new IllegalStateException("海缸登录签名密钥或令牌有效期尚未配置");
        }
        long expiresAt = Instant.now().getEpochSecond() + ttl;
        String payload = userId + TOKEN_PAYLOAD_SEPARATOR + expiresAt;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + TOKEN_SEPARATOR + sign(encodedPayload, key);
    }

    /**
     * 校验令牌签名与过期时间，非法令牌返回空值。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param token 客户端 bearer token
     * @return 已认证用户编号；校验失败时返回空值
     */
    public Long verify(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 2 || properties.getTokenSigningKey() == null) {
                return null;
            }
            byte[] expected = sign(parts[0], properties.getTokenSigningKey()).getBytes(StandardCharsets.UTF_8);
            byte[] actual = parts[1].getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(expected, actual)) {
                return null;
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] claims = payload.split(TOKEN_PAYLOAD_SEPARATOR, -1);
            if (claims.length != 2 || Long.parseLong(claims[1]) <= Instant.now().getEpochSecond()) {
                return null;
            }
            return Long.valueOf(claims[0]);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 使用服务端密钥计算 HMAC-SHA256 签名。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param content 待签名的令牌负载
     * @param key 服务端签名密钥
     * @return Base64URL 编码签名
     */
    private String sign(String content, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("无法校验海缸登录令牌", exception);
        }
    }
}
