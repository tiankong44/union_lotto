package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.reefjournal.auth.ReefJournalTokenService;
import com.tiankong44.tool.reefjournal.client.ReefWechatClient;
import com.tiankong44.tool.reefjournal.dto.ReefLoginResponse;
import com.tiankong44.tool.reefjournal.exception.ReefJournalException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/**
 * 处理微信身份映射及小程序登录态签发。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Service
public class ReefAuthService {
    private final JdbcTemplate jdbcTemplate;
    private final ReefWechatClient wechatClient;
    private final ReefJournalTokenService tokenService;

    /**
     * 创建微信身份映射服务。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param jdbcTemplate 用户身份表访问器
     * @param wechatClient 微信登录凭证客户端
     * @param tokenService 海缸模块令牌服务
     */
    public ReefAuthService(JdbcTemplate jdbcTemplate, ReefWechatClient wechatClient, ReefJournalTokenService tokenService) {
        this.jdbcTemplate = jdbcTemplate;
        this.wechatClient = wechatClient;
        this.tokenService = tokenService;
    }

    /**
     * 先向微信校验一次性 code，再创建或复用内部用户并签发访问令牌。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param code 小程序一次性登录凭证
     * @return 小程序访问令牌和字符串用户编号
     */
    public ReefLoginResponse login(String code) {
        // 远程凭证交换在数据库操作之前完成，避免事务占用连接等待微信响应。
        String openid = wechatClient.exchangeCode(code);
        Long userId = findUserId(openid);
        if (userId == null) {
            userId = createUser(openid);
        }
        return new ReefLoginResponse(tokenService.issue(userId), String.valueOf(userId));
    }

    /**
     * 按微信 OpenID 查找内部用户编号。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param openid 微信小程序用户标识
     * @return 已存在的内部编号；尚未注册时返回空值
     */
    private Long findUserId(String openid) {
        List<Long> userIds = jdbcTemplate.query("SELECT id FROM reef_user WHERE openid = ? LIMIT 1",
                (resultSet, rowNumber) -> resultSet.getLong("id"), openid);
        return userIds.isEmpty() ? null : userIds.get(0);
    }

    /**
     * 注册首次登录用户，并处理并发首次登录的唯一键竞争。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param openid 微信小程序用户标识
     * @return 新用户内部编号
     */
    private Long createUser(String openid) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update((Connection connection) -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO reef_user (openid, created_at) VALUES (?, NOW())", Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, openid);
                return statement;
            }, keyHolder);
            Number generatedKey = keyHolder.getKey();
            if (generatedKey == null) {
                throw new ReefJournalException("用户资料暂时无法建立，请稍后重试");
            }
            return generatedKey.longValue();
        } catch (DuplicateKeyException exception) {
            Long existingUserId = findUserId(openid);
            if (existingUserId != null) return existingUserId;
            throw new ReefJournalException("用户资料暂时无法建立，请稍后重试");
        }
    }
}
