package com.tiankong44.tool.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Component
public class JwtTokenManager {

    private static final String PRIVATE_KEY = "MC4CAQAwBQYDK2VwBCIEIKSWbSMO+g1lU2XwoaRdwO6ldod/I7zWwl0VZPkH0+4n";
    private static final String KID = "TAGXUWFNUR";
    private static final String SUB = "2CTM7THK8C";
    private static final String REDIS_KEY = "jwt_token_hf_weather";
    @Autowired
    private RedisUtil redisUtil;

    public String getValidJWT() {
        // 从 Redis 中获取 JWT
        String jwt = redisUtil.get(REDIS_KEY);

        // 如果 JWT 不存在或即将过期，则重新生成 JWT
        if (jwt == null || isJwtExpiringSoon(jwt)) {
            jwt = generateNewJWT();
            // 将新的 JWT 保存到 Redis，设置过期时间为 15 分钟
            redisUtil.setEx(REDIS_KEY, jwt, 10, TimeUnit.MINUTES);
        }

        return jwt;
    }

    private boolean isJwtExpiringSoon(String jwt) {
        // 解析 JWT 的 payload 部分，检查 exp 时间
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) {
            return true; // 不合法的 JWT
        }

        String payloadEncoded = parts[1];
        String payloadJson = new String(Base64.getUrlDecoder().decode(payloadEncoded), StandardCharsets.UTF_8);
        JSONObject payload = JSON.parseObject(payloadJson);

        long exp = payload.getLong("exp");
        long now = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond();
        return (exp - now) < 60; // 如果剩余时间小于 1 分钟，则认为即将过期
    }

    public String generateNewJWT() {
        // Private key
        byte[] privateKeyBytes = Base64.getDecoder().decode(PRIVATE_KEY.trim().replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", ""));
        PKCS8EncodedKeySpec encoded = new PKCS8EncodedKeySpec(privateKeyBytes);
        PrivateKey privateKey = null;
        try {
            privateKey = new EdDSAPrivateKey(encoded);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
        // Header
        String headerJson = "{\"alg\": \"EdDSA\", \"kid\": \"" + KID + "\"}";

        // Payload
        long iat = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond();
        long exp = iat + 900; // 设置 JWT 的有效期为 15 分钟
        String payloadJson = "{\"sub\": \"" + SUB + "\", \"iat\": " + iat + ", \"exp\": " + exp + "}";

        // Base64url header+payload
        String headerEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String data = headerEncoded + "." + payloadEncoded;

        // Sign
        Signature signature = null;
        try {
            signature = new EdDSAEngine(MessageDigest.getInstance("SHA-512"));
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signature.sign();
            String signatureEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

            // Generate Token
            return data + "." + signatureEncoded;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }


    }

}