package com.tiankong44.tool.reefjournal.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tiankong44.tool.reefjournal.config.ReefJournalProperties;
import com.tiankong44.tool.reefjournal.dto.ReefWechatAccessTokenResponse;
import com.tiankong44.tool.reefjournal.dto.ReefWechatSendResponse;
import com.tiankong44.tool.reefjournal.dto.ReefWechatSessionResponse;
import com.tiankong44.tool.reefjournal.exception.ReefJournalException;
import com.tiankong44.tool.util.RedisUtil;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TYPE_EQUIPMENT;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TYPE_FEEDING;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TYPE_WATER_CHANGE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TYPE_WATER_TEST;

/**
 * 调用微信登录和订阅消息服务端接口，不记录凭证和访问令牌。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Component
public class ReefWechatClient {
    private static final String CODE_SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String SUBSCRIBE_SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";
    private static final String ACCESS_TOKEN_CACHE_KEY = "reef-journal:wechat:access-token";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ReefJournalProperties properties;
    private final RedisUtil redisUtil;

    /**
     * 创建独立微信客户端并复用项目 Jackson 和 Redis 能力。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param objectMapper 项目 JSON 序列化器
     * @param properties 服务端微信配置
     * @param redisUtil 共享 Redis 服务
     */
    public ReefWechatClient(ObjectMapper objectMapper, ReefJournalProperties properties, RedisUtil redisUtil) {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.redisUtil = redisUtil;
    }

    /**
     * 使用一次性 code 换取 OpenID，调用失败时不向小程序返回会话密钥。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param code 小程序 wx.login 返回的一次性凭证
     * @return 微信用户标识
     */
    public String exchangeCode(String code) {
        requireWechatCredentials();
        HttpUrl url = HttpUrl.parse(CODE_SESSION_URL).newBuilder()
                .addQueryParameter("appid", properties.getAppId())
                .addQueryParameter("secret", properties.getAppSecret())
                .addQueryParameter("js_code", code)
                .addQueryParameter("grant_type", "authorization_code")
                .build();
        ReefWechatSessionResponse response = executeGet(url, ReefWechatSessionResponse.class);
        if (response.getErrcode() != null || response.getOpenid() == null || response.getOpenid().trim().isEmpty()) {
            throw new ReefJournalException("微信登录校验没有通过，请重新进入小程序");
        }
        return response.getOpenid();
    }

    /**
     * 发送一条已获用户授权额度支持的订阅提醒。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param openid 微信用户标识
     * @param type 提醒类型
     * @param title 展示标题
     * @param dueAt 提醒时间文本
     * @return 微信接口是否确认接收该通知
     */
    public boolean sendReminder(String openid, String type, String title, String dueAt) {
        String templateId = templateId(type);
        String titleField = titleField(type);
        String timeField = timeField(type);
        if (isBlank(templateId) || isBlank(titleField) || isBlank(timeField)) {
            throw new ReefJournalException("订阅消息模板或字段待管理员配置");
        }
        String accessToken = getAccessToken();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("touser", openid);
        body.put("template_id", templateId);
        ObjectNode data = body.putObject("data");
        data.putObject(titleField).put("value", title);
        data.putObject(timeField).put("value", dueAt);
        HttpUrl url = HttpUrl.parse(SUBSCRIBE_SEND_URL).newBuilder().addQueryParameter("access_token", accessToken).build();
        ReefWechatSendResponse response = executePost(url, body, ReefWechatSendResponse.class);
        return response.getErrcode() != null && response.getErrcode() == 0;
    }

    /**
     * 检查单类订阅提醒的模板编号和模板字段是否均已配置。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param type 提醒类型
     * @return 模板配置完整时返回 true
     */
    public boolean isTemplateReady(String type) {
        return !isBlank(templateId(type)) && !isBlank(titleField(type)) && !isBlank(timeField(type));
    }

    /**
     * 从共享 Redis 读取或刷新小程序服务端访问令牌。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @return 可供订阅消息接口使用的访问令牌
     */
    private String getAccessToken() {
        String cached = redisUtil.get(ACCESS_TOKEN_CACHE_KEY);
        if (!isBlank(cached)) {
            return cached;
        }
        HttpUrl url = HttpUrl.parse(ACCESS_TOKEN_URL).newBuilder()
                .addQueryParameter("grant_type", "client_credential")
                .addQueryParameter("appid", properties.getAppId())
                .addQueryParameter("secret", properties.getAppSecret())
                .build();
        ReefWechatAccessTokenResponse response = executeGet(url, ReefWechatAccessTokenResponse.class);
        if (isBlank(response.getAccessToken()) || response.getExpiresIn() == null) {
            throw new ReefJournalException("微信提醒服务暂时无法取得访问令牌");
        }
        Long configuredTtl = properties.getReminder().getAccessTokenCacheSeconds();
        long cacheSeconds = Math.min(configuredTtl, response.getExpiresIn());
        redisUtil.setEx(ACCESS_TOKEN_CACHE_KEY, response.getAccessToken(), cacheSeconds, TimeUnit.SECONDS);
        return response.getAccessToken();
    }

    /**
     * 发送 GET 请求并将响应映射为独立 DTO。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param url 已完成参数编码的微信接口地址
     * @param responseType 接口响应 DTO 类型
     * @param <T> 响应 DTO 类型
     * @return 已解析的接口响应
     */
    private <T> T executeGet(HttpUrl url, Class<T> responseType) {
        Request request = new Request.Builder().url(url).get().build();
        return execute(request, responseType);
    }

    /**
     * 发送 JSON POST 请求并解析微信接口响应。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param url 已完成参数编码的微信接口地址
     * @param body 微信接口请求 JSON
     * @param responseType 接口响应 DTO 类型
     * @param <T> 响应 DTO 类型
     * @return 已解析的接口响应
     */
    private <T> T executePost(HttpUrl url, JsonNode body, Class<T> responseType) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(body);
            RequestBody requestBody = RequestBody.create(payload, JSON_MEDIA_TYPE);
            Request request = new Request.Builder().url(url).post(requestBody).build();
            return execute(request, responseType);
        } catch (IOException exception) {
            throw new ReefJournalException("微信提醒服务暂时无法处理请求");
        }
    }

    /**
     * 统一处理远端 HTTP 错误，避免把令牌和响应正文写入日志或返回值。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param request 微信服务端请求
     * @param responseType 响应 DTO 类型
     * @param <T> 响应 DTO 类型
     * @return 已解析的接口响应
     */
    private <T> T execute(Request request, Class<T> responseType) {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new ReefJournalException("微信服务暂时不可用，请稍后重试");
            }
            byte[] body = response.body().bytes();
            return objectMapper.readValue(body, responseType);
        } catch (IOException exception) {
            throw new ReefJournalException("微信服务暂时无法连接，请稍后重试");
        }
    }

    /**
     * 验证运行环境已提供小程序服务端凭据。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     */
    private void requireWechatCredentials() {
        if (isBlank(properties.getAppId()) || isBlank(properties.getAppSecret())) {
            throw new ReefJournalException("小程序登录配置待补充，请联系管理员");
        }
    }

    /**
     * 根据提醒类型读取对应模板编号。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param type 提醒类型编码
     * @return 配置的模板编号
     */
    private String templateId(String type) {
        if (TYPE_WATER_CHANGE.equals(type)) return properties.getWaterChangeTemplateId();
        if (TYPE_WATER_TEST.equals(type)) return properties.getWaterTestTemplateId();
        if (TYPE_FEEDING.equals(type)) return properties.getFeedingTemplateId();
        if (TYPE_EQUIPMENT.equals(type)) return properties.getEquipmentTemplateId();
        return null;
    }

    /**
     * 根据提醒类型读取模板标题字段编码。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param type 提醒类型编码
     * @return 模板字段编码
     */
    private String titleField(String type) {
        if (TYPE_WATER_CHANGE.equals(type)) return properties.getWaterChangeTitleField();
        if (TYPE_WATER_TEST.equals(type)) return properties.getWaterTestTitleField();
        if (TYPE_FEEDING.equals(type)) return properties.getFeedingTitleField();
        if (TYPE_EQUIPMENT.equals(type)) return properties.getEquipmentTitleField();
        return null;
    }

    /**
     * 根据提醒类型读取模板时间字段编码。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param type 提醒类型编码
     * @return 模板字段编码
     */
    private String timeField(String type) {
        if (TYPE_WATER_CHANGE.equals(type)) return properties.getWaterChangeTimeField();
        if (TYPE_WATER_TEST.equals(type)) return properties.getWaterTestTimeField();
        if (TYPE_FEEDING.equals(type)) return properties.getFeedingTimeField();
        if (TYPE_EQUIPMENT.equals(type)) return properties.getEquipmentTimeField();
        return null;
    }

    /**
     * 判断配置或接口字段是否为空白。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param value 待判断文本
     * @return 文本为空或仅有空白字符时返回 true
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
