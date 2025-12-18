package com.tiankong44.tool.apex.service;

import com.tiankong44.tool.apex.entity.ApexUser;
import com.tiankong44.tool.apex.entity.ApexUserDailyRecord;
import com.tiankong44.tool.base.entity.BaseRes;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public interface ApexService {
    BaseRes getUserStats(String userId);
    
    BaseRes getUserDailyRecords(String userId, Integer limit, Integer offset);
    
    BaseRes addTodayCount(String userId, Integer count);
    
    BaseRes backfillHistoryRecord(String userId, LocalDate date, Integer count);
    
    BaseRes getDailyRecordByDate(String userId, LocalDate date);
    
    BaseRes checkUserExists(String userId);
    
    BaseRes createUser(String userId);
    
    boolean doesUserExist(String userId);

    BaseRes deleteRecord(@NotBlank String userId, @NotNull(message = "记录不能为空") @Min(value = 1, message = "数量必须大于0") Integer id);
}