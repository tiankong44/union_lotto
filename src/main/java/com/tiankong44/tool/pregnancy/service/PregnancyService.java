package com.tiankong44.tool.pregnancy.service;

import com.tiankong44.tool.base.entity.BaseRes;
import com.tiankong44.tool.pregnancy.dto.AntenatalTaskSaveRequest;
import com.tiankong44.tool.pregnancy.dto.ContractionSaveRequest;
import com.tiankong44.tool.pregnancy.dto.FetalMovementSessionSaveRequest;
import com.tiankong44.tool.pregnancy.dto.HealthRecordSaveRequest;
import com.tiankong44.tool.pregnancy.dto.ProfileSaveRequest;
import com.tiankong44.tool.pregnancy.dto.RecordDeleteRequest;
import com.tiankong44.tool.pregnancy.dto.RecordNoteUpdateRequest;

import java.time.LocalDate;

/**
 * 孕期助手业务服务接口。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-08-06
 */
public interface PregnancyService {
    /**
     * 查询单用户孕期档案。
     *
     * @return 档案数据，未创建时返回空数据
     */
    BaseRes getProfile();

    /**
     * 保存单用户孕期档案。
     *
     * @param request 档案字段
     * @return 保存后的档案
     */
    BaseRes saveProfile(ProfileSaveRequest request);

    /**
     * 保存一条已结束的胎动会话，并按客户端编号幂等处理重复请求。
     *
     * @param request 胎动会话字段
     * @return 保存后的会话
     */
    BaseRes saveFetalMovement(FetalMovementSessionSaveRequest request);

    /**
     * 查询单用户胎动会话历史。
     *
     * @param startDate 开始日期，可为空
     * @param endDate 结束日期，可为空
     * @return 胎动会话列表
     */
    BaseRes listFetalMovements(LocalDate startDate, LocalDate endDate);

    /**
     * 保存一条宫缩记录，并按客户端编号幂等处理重复请求。
     *
     * @param request 宫缩字段
     * @return 保存后的宫缩记录
     */
    BaseRes saveContraction(ContractionSaveRequest request);

    /**
     * 查询单用户宫缩记录。
     *
     * @param startDate 开始日期，可为空
     * @param endDate 结束日期，可为空
     * @return 宫缩记录列表
     */
    BaseRes listContractions(LocalDate startDate, LocalDate endDate);

    /**
     * 保存一条孕期健康记录，并按客户端编号幂等处理重复请求。
     *
     * @param request 健康记录字段
     * @return 保存后的健康记录
     */
    BaseRes saveHealthRecord(HealthRecordSaveRequest request);

    /**
     * 查询单用户健康记录。
     *
     * @param recordType 记录类型，可为空
     * @return 健康记录列表
     */
    BaseRes listHealthRecords(String recordType);

    /**
     * 保存一条产检待办，并按客户端编号幂等处理重复请求。
     *
     * @param request 待办字段
     * @return 保存后的待办
     */
    BaseRes saveTask(AntenatalTaskSaveRequest request);

    /**
     * 查询单用户待办事项。
     *
     * @param status 状态，可为空
     * @return 待办列表
     */
    BaseRes listTasks(String status);

    /**
     * 更新孕期记录备注，支持补充、修改和清空四类记录的备注。
     *
     * @param request 记录类型、客户端编号和备注内容
     * @return 更新后的记录；记录不存在或类型不支持时返回业务失败
     */
    BaseRes updateRecordNote(RecordNoteUpdateRequest request);

    /**
     * 删除单用户的一条已保存记录，并清理已知从属数据。
     *
     * @param request 记录类型和客户端记录编号
     * @return 删除成功结果；记录不存在、类型或待办状态不支持时返回业务失败
     */
    BaseRes deleteRecord(RecordDeleteRequest request);

    /**
     * 汇总记录数量和个人趋势基础指标。
     *
     * @return 汇总数据
     */
    BaseRes getSummary();

}
