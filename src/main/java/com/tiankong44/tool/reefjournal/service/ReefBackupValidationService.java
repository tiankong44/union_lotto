package com.tiankong44.tool.reefjournal.service;

import com.tiankong44.tool.reefjournal.exception.ReefJournalException;
import org.springframework.stereotype.Service;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;

/**
 * 校验备份中的结构化数据和数字字段，避免绕过现有接口校验。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
@Service
public class ReefBackupValidationService {
    private final Validator validator;

    /** 创建复用项目 Bean Validation 的校验服务。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public ReefBackupValidationService(Validator validator) { this.validator = validator; }

    /** 校验恢复前转换出的请求对象。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public <T> void validate(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) throw new ReefJournalException("备份内容不完整：" + violations.iterator().next().getMessage());
    }

    /** 保留未填写数值，拒绝无法解析的备份数据。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public BigDecimal decimal(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try { return new BigDecimal(value); }
        catch (NumberFormatException exception) { throw new ReefJournalException("备份中包含无法识别的数字"); }
    }
}
