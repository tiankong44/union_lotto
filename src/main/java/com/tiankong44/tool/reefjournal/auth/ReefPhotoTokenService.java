package com.tiankong44.tool.reefjournal.auth;

import com.tiankong44.tool.reefjournal.config.ReefJournalProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

import static com.tiankong44.tool.reefjournal.ReefJournalConstants.PHOTO_TOKEN_SEPARATOR;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TOKEN_PAYLOAD_SEPARATOR;

/**
 * 为单张照片签发短时访问凭证，避免将用户登录令牌放入图片 URL。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Service
public class ReefPhotoTokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final ReefJournalProperties properties;

    /**
     * 创建使用海缸登录签名密钥的照片链接令牌服务。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param properties 海缸模块签名密钥和照片链接时长配置
     */
    public ReefPhotoTokenService(ReefJournalProperties properties) {
        this.properties = properties;
    }

    /**
     * 生成与用户、照片编号及过期时间绑定的签名。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param userId 照片所属用户编号
     * @param photoId 照片编号
     * @return 短时照片内容访问签名
     */
    public String issue(Long userId, Long photoId) {
        Long ttl = properties.getPhotoUrlTtlSeconds();
        String key = properties.getTokenSigningKey();
        if (ttl == null || ttl <= 0L || key == null || key.length() < 32) {
            throw new IllegalStateException("海缸照片链接签名配置尚未补充");
        }
        long expiresAt = Instant.now().getEpochSecond() + ttl;
        String payload = userId + TOKEN_PAYLOAD_SEPARATOR + photoId + TOKEN_PAYLOAD_SEPARATOR + expiresAt;
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encoded + PHOTO_TOKEN_SEPARATOR + sign(encoded, key);
    }

    /**
     * 验证签名有效期并确认签名绑定了请求中的照片编号。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param photoId 路径中的照片编号
     * @param token 短时访问签名
     * @return 签名中的所属用户编号；校验失败时返回空值
     */
    public Long verify(String photoId, String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 2 || properties.getTokenSigningKey() == null) return null;
            byte[] expected = sign(parts[0], properties.getTokenSigningKey()).getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(expected, parts[1].getBytes(StandardCharsets.UTF_8))) return null;
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] claims = payload.split(TOKEN_PAYLOAD_SEPARATOR, -1);
            if (claims.length != 3 || !photoId.equals(claims[1])) return null;
            if (Long.parseLong(claims[2]) <= Instant.now().getEpochSecond()) return null;
            return Long.valueOf(claims[0]);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 使用服务器密钥计算 Base64URL HMAC-SHA256。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param content 待签名内容
     * @param key 服务器签名密钥
     * @return 签名文本
     */
    private String sign(String content, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("无法签署海缸照片链接", exception);
        }
    }
}
