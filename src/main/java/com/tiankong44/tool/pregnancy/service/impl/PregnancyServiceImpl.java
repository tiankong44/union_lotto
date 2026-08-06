package com.tiankong44.tool.pregnancy.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.pregnancy.dto.AntenatalTaskSaveRequest;
import com.tiankong44.tool.pregnancy.dto.ContractionSaveRequest;
import com.tiankong44.tool.pregnancy.dto.FetalMovementSessionSaveRequest;
import com.tiankong44.tool.pregnancy.dto.HealthRecordSaveRequest;
import com.tiankong44.tool.pregnancy.dto.ProfileSaveRequest;
import com.tiankong44.tool.pregnancy.dto.SyncBatchItem;
import com.tiankong44.tool.pregnancy.dto.SyncBatchRequest;
import com.tiankong44.tool.pregnancy.entity.AntenatalTask;
import com.tiankong44.tool.pregnancy.entity.ContractionSession;
import com.tiankong44.tool.pregnancy.entity.FetalMovementSession;
import com.tiankong44.tool.pregnancy.entity.PregnancyHealthRecord;
import com.tiankong44.tool.pregnancy.entity.PregnancyProfile;
import com.tiankong44.tool.pregnancy.entity.PregnancySyncRecord;
import com.tiankong44.tool.pregnancy.mapper.AntenatalTaskMapper;
import com.tiankong44.tool.pregnancy.mapper.ContractionSessionMapper;
import com.tiankong44.tool.pregnancy.mapper.FetalMovementSessionMapper;
import com.tiankong44.tool.pregnancy.mapper.PregnancyHealthRecordMapper;
import com.tiankong44.tool.pregnancy.mapper.PregnancyProfileMapper;
import com.tiankong44.tool.pregnancy.mapper.PregnancySyncRecordMapper;
import com.tiankong44.tool.pregnancy.service.PregnancyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 孕期助手业务服务实现，负责记录幂等和汇总查询。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
@Service
public class PregnancyServiceImpl implements PregnancyService {
    @Resource
    private PregnancyProfileMapper pregnancyProfileMapper;

    @Resource
    private FetalMovementSessionMapper fetalMovementSessionMapper;

    @Resource
    private ContractionSessionMapper contractionSessionMapper;

    @Resource
    private PregnancyHealthRecordMapper pregnancyHealthRecordMapper;

    @Resource
    private AntenatalTaskMapper antenatalTaskMapper;

    @Resource
    private PregnancySyncRecordMapper pregnancySyncRecordMapper;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 查询单用户的孕期档案。
     *
     * @return 档案数据
     */
    @Override
    public BaseRes getProfile() {
        QueryWrapper<PregnancyProfile> query = new QueryWrapper<>();
        // 查询单用户唯一的孕期档案。
        return BaseRes.success(pregnancyProfileMapper.selectOne(query));
    }

    /**
     * 新增或更新单用户的孕期档案。
     *
     * @param request 档案字段
     * @return 保存后的档案
     */
    @Override
    @Transactional
    public BaseRes saveProfile(ProfileSaveRequest request) {
        if (request == null) {
            return BaseRes.failure("档案内容不能为空");
        }
        QueryWrapper<PregnancyProfile> query = new QueryWrapper<>();
        // 读取单用户档案，决定执行新增还是更新。
        PregnancyProfile profile = pregnancyProfileMapper.selectOne(query);
        LocalDateTime now = LocalDateTime.now();
        if (profile == null) {
            profile = new PregnancyProfile();
            profile.setCreatedAt(now);
        }
        profile.setLmpDate(request.getLmpDate());
        profile.setDueDate(request.getDueDate());
        profile.setBabyCount(request.getBabyCount() == null ? 1 : request.getBabyCount());
        profile.setNickname(request.getNickname());
        profile.setNote(request.getNote());
        profile.setUpdatedAt(now);
        if (profile.getId() == null) {
            // 创建新档案并保留创建时间。
            pregnancyProfileMapper.insert(profile);
        } else {
            // 更新已有档案，避免单用户产生多份档案。
            pregnancyProfileMapper.updateById(profile);
        }
        return BaseRes.success(profile);
    }

    /**
     * 保存胎动会话并处理重复客户端请求。
     *
     * @param request 胎动会话字段
     * @return 保存后的会话
     */
    @Override
    @Transactional
    public BaseRes saveFetalMovement(FetalMovementSessionSaveRequest request) {
        if (request == null) {
            return BaseRes.failure("胎动会话参数不能为空");
        }
        if (request.getEndedAt().isBefore(request.getStartedAt())) {
            return BaseRes.failure("结束时间不能早于开始时间");
        }
        QueryWrapper<FetalMovementSession> query = new QueryWrapper<>();
        query.eq("client_record_id", request.getClientRecordId());
        // 先查询客户端编号，保证网络重试不会产生重复会话。
        FetalMovementSession existing = fetalMovementSessionMapper.selectOne(query);
        if (existing != null) {
            return BaseRes.success(existing);
        }
        FetalMovementSession session = new FetalMovementSession();
        session.setClientRecordId(request.getClientRecordId());
        session.setSessionMode(defaultValue(request.getSessionMode(), "free"));
        session.setStartedAt(request.getStartedAt());
        session.setEndedAt(request.getEndedAt());
        session.setMovementCount(request.getMovementCount());
        session.setTargetCount(request.getTargetCount());
        session.setAverageStrength(request.getAverageStrength());
        session.setNote(request.getNote());
        session.setRecordStatus("SYNCED");
        setCreatedAndUpdated(session);
        // 保存已结束的胎动会话。
        fetalMovementSessionMapper.insert(session);
        return BaseRes.success(session);
    }

    /**
     * 查询胎动会话历史，可按日期范围筛选。
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 会话列表
     */
    @Override
    public BaseRes listFetalMovements(LocalDate startDate, LocalDate endDate) {
        QueryWrapper<FetalMovementSession> query = new QueryWrapper<>();
        appendDateRange(query, "started_at", startDate, endDate);
        query.orderByDesc("started_at");
        // 按开始时间倒序返回胎动会话，便于优先展示最近记录。
        return BaseRes.success(fetalMovementSessionMapper.selectList(query));
    }

    /**
     * 保存宫缩计时记录并处理重复客户端请求。
     *
     * @param request 宫缩字段
     * @return 保存后的宫缩记录
     */
    @Override
    @Transactional
    public BaseRes saveContraction(ContractionSaveRequest request) {
        if (request == null) {
            return BaseRes.failure("宫缩记录参数不能为空");
        }
        if (request.getEndedAt().isBefore(request.getStartedAt())) {
            return BaseRes.failure("结束时间不能早于开始时间");
        }
        QueryWrapper<ContractionSession> query = new QueryWrapper<>();
        query.eq("client_record_id", request.getClientRecordId());
        // 先查询客户端编号，避免同步重试重复写入宫缩记录。
        ContractionSession existing = contractionSessionMapper.selectOne(query);
        if (existing != null) {
            return BaseRes.success(existing);
        }
        ContractionSession record = new ContractionSession();
        record.setClientRecordId(request.getClientRecordId());
        record.setStartedAt(request.getStartedAt());
        record.setEndedAt(request.getEndedAt());
        record.setDurationSeconds(request.getDurationSeconds());
        record.setIntervalSeconds(request.getIntervalSeconds());
        record.setIntensity(request.getIntensity());
        record.setNote(request.getNote());
        record.setRecordStatus("SYNCED");
        setCreatedAndUpdated(record);
        // 保存已结束的宫缩计时结果。
        contractionSessionMapper.insert(record);
        return BaseRes.success(record);
    }

    /**
     * 查询宫缩记录历史，可按日期范围筛选。
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 宫缩记录列表
     */
    @Override
    public BaseRes listContractions(LocalDate startDate, LocalDate endDate) {
        QueryWrapper<ContractionSession> query = new QueryWrapper<>();
        appendDateRange(query, "started_at", startDate, endDate);
        query.orderByDesc("started_at");
        // 按开始时间倒序返回宫缩历史。
        return BaseRes.success(contractionSessionMapper.selectList(query));
    }

    /**
     * 保存孕期健康记录并处理重复客户端请求。
     *
     * @param request 健康记录字段
     * @return 保存后的健康记录
     */
    @Override
    @Transactional
    public BaseRes saveHealthRecord(HealthRecordSaveRequest request) {
        if (request == null) {
            return BaseRes.failure("健康记录参数不能为空");
        }
        QueryWrapper<PregnancyHealthRecord> query = new QueryWrapper<>();
        query.eq("client_record_id", request.getClientRecordId());
        // 先查询客户端编号，避免重复提交形成重复健康记录。
        PregnancyHealthRecord existing = pregnancyHealthRecordMapper.selectOne(query);
        if (existing != null) {
            return BaseRes.success(existing);
        }
        PregnancyHealthRecord record = new PregnancyHealthRecord();
        record.setClientRecordId(request.getClientRecordId());
        record.setRecordType(request.getRecordType());
        record.setValueJson(request.getValueJson());
        record.setUnit(request.getUnit());
        record.setRecordedAt(request.getRecordedAt());
        record.setNote(request.getNote());
        record.setRecordStatus("SYNCED");
        setCreatedAndUpdated(record);
        // 保存用户主动录入的健康记录，不做医疗结论推断。
        pregnancyHealthRecordMapper.insert(record);
        return BaseRes.success(record);
    }

    /**
     * 查询健康记录，可按记录类型过滤。
     *
     * @param recordType 记录类型
     * @return 健康记录列表
     */
    @Override
    public BaseRes listHealthRecords(String recordType) {
        QueryWrapper<PregnancyHealthRecord> query = new QueryWrapper<>();
        if (!isBlank(recordType)) {
            query.eq("record_type", recordType);
        }
        query.orderByDesc("recorded_at");
        // 按记录时间倒序返回健康记录。
        return BaseRes.success(pregnancyHealthRecordMapper.selectList(query));
    }

    /**
     * 保存产检待办并处理重复客户端请求。
     *
     * @param request 待办字段
     * @return 保存后的待办
     */
    @Override
    @Transactional
    public BaseRes saveTask(AntenatalTaskSaveRequest request) {
        if (request == null) {
            return BaseRes.failure("待办参数不能为空");
        }
        QueryWrapper<AntenatalTask> query = new QueryWrapper<>();
        query.eq("client_record_id", request.getClientRecordId());
        // 先查询客户端编号，确保重复同步只保留一条待办。
        AntenatalTask existing = antenatalTaskMapper.selectOne(query);
        if (existing != null) {
            existing.setTitle(request.getTitle());
            existing.setTaskType(defaultValue(request.getTaskType(), "custom"));
            existing.setPlannedAt(request.getPlannedAt());
            existing.setStatus(defaultValue(request.getStatus(), "TODO"));
            existing.setNote(request.getNote());
            existing.setCompletedAt("DONE".equals(existing.getStatus()) ? LocalDateTime.now() : null);
            existing.setUpdatedAt(LocalDateTime.now());
            // 更新已有待办，保证本地完成状态同步到服务端。
            antenatalTaskMapper.updateById(existing);
            return BaseRes.success(existing);
        }
        AntenatalTask task = new AntenatalTask();
        task.setClientRecordId(request.getClientRecordId());
        task.setTitle(request.getTitle());
        task.setTaskType(defaultValue(request.getTaskType(), "custom"));
        task.setPlannedAt(request.getPlannedAt());
        task.setStatus(defaultValue(request.getStatus(), "TODO"));
        task.setNote(request.getNote());
        setCreatedAndUpdated(task);
        // 保存待办，后续状态变化由同一客户端编号幂等更新。
        antenatalTaskMapper.insert(task);
        return BaseRes.success(task);
    }

    /**
     * 查询待办列表，可按状态过滤。
     *
     * @param status 待办状态
     * @return 待办列表
     */
    @Override
    public BaseRes listTasks(String status) {
        QueryWrapper<AntenatalTask> query = new QueryWrapper<>();
        if (!isBlank(status)) {
            query.eq("status", status);
        }
        query.orderByAsc("planned_at");
        // 按计划时间升序返回待办，方便形成时间线。
        return BaseRes.success(antenatalTaskMapper.selectList(query));
    }

    /**
     * 汇总单用户的基础记录指标。
     *
     * @return 汇总指标和个人趋势基础数据
     */
    @Override
    public BaseRes getSummary() {
        QueryWrapper<FetalMovementSession> movementQuery = new QueryWrapper<>();
        // 读取胎动历史用于计算个人基线，不与固定医疗阈值比较。
        List<FetalMovementSession> movements = fetalMovementSessionMapper.selectList(movementQuery);
        QueryWrapper<ContractionSession> contractionQuery = new QueryWrapper<>();
        List<ContractionSession> contractions = contractionSessionMapper.selectList(contractionQuery);
        QueryWrapper<PregnancyHealthRecord> healthQuery = new QueryWrapper<>();
        List<PregnancyHealthRecord> healthRecords = pregnancyHealthRecordMapper.selectList(healthQuery);
        QueryWrapper<AntenatalTask> taskQuery = new QueryWrapper<>();
        taskQuery.eq("status", "TODO");
        List<AntenatalTask> pendingTasks = antenatalTaskMapper.selectList(taskQuery);

        int totalMovements = 0;
        long totalDurationSeconds = 0L;
        for (FetalMovementSession movement : movements) {
            totalMovements += safeInt(movement.getMovementCount());
            if (movement.getStartedAt() != null && movement.getEndedAt() != null) {
                totalDurationSeconds += Duration.between(movement.getStartedAt(), movement.getEndedAt()).getSeconds();
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("movementSessionCount", movements.size());
        result.put("movementTotal", totalMovements);
        result.put("movementAverageDurationSeconds", movements.isEmpty() ? 0 : totalDurationSeconds / movements.size());
        result.put("contractionCount", contractions.size());
        result.put("healthRecordCount", healthRecords.size());
        result.put("pendingTaskCount", pendingTasks.size());
        result.put("trendMessage", buildTrendMessage(movements));
        return BaseRes.success(result);
    }

    /**
     * 保存本地优先同步批次，按客户端记录编号幂等。
     *
     * @param request 同步批次
     * @return 同步结果列表
     */
    @Override
    @Transactional
    public BaseRes sync(SyncBatchRequest request) {
        if (request == null || request.getItems() == null) {
            return BaseRes.failure("同步参数不能为空");
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (SyncBatchItem item : request.getItems()) {
            QueryWrapper<PregnancySyncRecord> query = new QueryWrapper<>();
            query.eq("entity_type", item.getEntityType())
                    .eq("client_record_id", item.getClientRecordId());
            // 读取客户端记录，重复上传时更新载荷而不是重复新增。
            PregnancySyncRecord syncRecord = pregnancySyncRecordMapper.selectOne(query);
            LocalDateTime now = LocalDateTime.now();
            if (syncRecord == null) {
                syncRecord = new PregnancySyncRecord();
                syncRecord.setEntityType(item.getEntityType());
                syncRecord.setClientRecordId(item.getClientRecordId());
                syncRecord.setCreatedAt(now);
            }
            syncRecord.setPayload(item.getPayload());
            syncRecord.setPayloadHash(DigestUtil.md5Hex(item.getPayload()));
            syncRecord.setSyncStatus("SYNCED");
            syncRecord.setLastError(null);
            syncRecord.setUpdatedAt(now);
            if (syncRecord.getId() == null) {
                // 首次上传时创建同步记录。
                pregnancySyncRecordMapper.insert(syncRecord);
            } else {
                // 重复上传时覆盖同一客户端记录的最新载荷。
                pregnancySyncRecordMapper.updateById(syncRecord);
            }
            Map<String, Object> itemResult = new HashMap<>();
            itemResult.put("entityType", item.getEntityType());
            itemResult.put("clientRecordId", item.getClientRecordId());
            try {
                // 将本地载荷落入对应业务表，保证服务端查询与同步结果一致。
                BaseRes materializedResult = materializeSyncItem(item);
                if (materializedResult.getCode() != 0) {
                    throw new IllegalStateException(materializedResult.getMessage());
                }
                itemResult.put("status", "SYNCED");
            } catch (Exception exception) {
                syncRecord.setSyncStatus("FAILED");
                syncRecord.setLastError(exception.getMessage());
                syncRecord.setUpdatedAt(LocalDateTime.now());
                // 记录落库失败原因，保留原始载荷供后续重试和排查。
                pregnancySyncRecordMapper.updateById(syncRecord);
                itemResult.put("status", "FAILED");
                itemResult.put("error", "业务记录落库失败");
            }
            results.add(itemResult);
        }
        return BaseRes.success(results);
    }

    /**
     * 按同步实体类型把本地载荷写入业务表。
     *
     * @param item 同步记录
     * @return 业务保存结果
     * @throws Exception 载荷解析或业务保存失败时抛出
     */
    private BaseRes materializeSyncItem(SyncBatchItem item) throws Exception {
        switch (item.getEntityType()) {
            case "profile":
                // 解析孕期档案载荷并更新单用户档案。
                return saveProfile(readPayload(item.getPayload(), ProfileSaveRequest.class));
            case "fetal-movement-session":
                // 解析胎动会话载荷并按客户端编号保存。
                return saveFetalMovement(readPayload(item.getPayload(), FetalMovementSessionSaveRequest.class));
            case "contraction-session":
                // 解析宫缩载荷并按客户端编号保存。
                return saveContraction(readPayload(item.getPayload(), ContractionSaveRequest.class));
            case "health-record":
                // 解析健康记录载荷并按客户端编号保存。
                return saveHealthRecord(readPayload(item.getPayload(), HealthRecordSaveRequest.class));
            case "antenatal-task":
                // 解析待办载荷并同步当前状态。
                return saveTask(readPayload(item.getPayload(), AntenatalTaskSaveRequest.class));
            default:
                return BaseRes.failure("不支持的同步实体类型");
        }
    }

    /**
     * 使用忽略未知字段的配置解析本地载荷，兼容前端状态字段。
     *
     * @param payload JSON 载荷
     * @param targetType 目标 DTO 类型
     * @param <T> DTO 类型
     * @return 解析后的 DTO
     * @throws Exception JSON 格式无效时抛出
     */
    private <T> T readPayload(String payload, Class<T> targetType) throws Exception {
        return objectMapper.readerFor(targetType)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(payload);
    }

    /**
     * 为查询追加日期范围条件，结束日期按次日零点处理以包含整天数据。
     *
     * @param query 查询条件
     * @param field 数据库时间字段
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    private void appendDateRange(QueryWrapper<?> query, String field, LocalDate startDate, LocalDate endDate) {
        if (startDate != null) {
            query.ge(field, startDate.atStartOfDay());
        }
        if (endDate != null) {
            query.lt(field, endDate.plusDays(1).atStartOfDay());
        }
    }

    /**
     * 生成不带医疗结论的个人趋势文字。
     *
     * @param movements 胎动会话列表
     * @return 趋势提示
     */
    private String buildTrendMessage(List<FetalMovementSession> movements) {
        if (movements == null || movements.size() < 2) {
            return "继续记录几次后，可查看自己的历史变化。";
        }
        FetalMovementSession latest = movements.get(0);
        FetalMovementSession previous = movements.get(1);
        int latestCount = safeInt(latest.getMovementCount());
        int previousCount = safeInt(previous.getMovementCount());
        if (latestCount < previousCount) {
            return "最近一次记录低于上一次个人记录；如果这与平时感受不同，请及时联系产科。";
        }
        if (latestCount > previousCount) {
            return "最近一次记录高于上一次个人记录，继续按自己的日常节奏记录。";
        }
        return "最近两次记录接近，继续观察自己的日常变化。";
    }

    /**
     * 设置新增实体的创建和更新时间。
     *
     * @param entity 需要写入时间的实体
     */
    private void setCreatedAndUpdated(Object entity) {
        LocalDateTime now = LocalDateTime.now();
        if (entity instanceof FetalMovementSession) {
            FetalMovementSession record = (FetalMovementSession) entity;
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
        } else if (entity instanceof ContractionSession) {
            ContractionSession record = (ContractionSession) entity;
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
        } else if (entity instanceof PregnancyHealthRecord) {
            PregnancyHealthRecord record = (PregnancyHealthRecord) entity;
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
        } else if (entity instanceof AntenatalTask) {
            AntenatalTask record = (AntenatalTask) entity;
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
        }
    }

    /**
     * 将空字符串替换成领域默认值。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 非空文本
     */
    private String defaultValue(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    /**
     * 判断文本是否为空。
     *
     * @param value 文本
     * @return 是否为空
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 将可能为空的数量转换为安全整数。
     *
     * @param value 数值
     * @return 非空数值或0
     */
    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
