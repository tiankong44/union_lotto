package com.tiankong44.tool.reefjournal.dto;

import com.tiankong44.tool.reefjournal.exception.ReefJournalException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单批恢复中使用的编号映射及待写入映射集合。
 * @Author zhanghao_SMEICS
 * @Date 2026-11-12
 */
public class ReefBackupRestoreContext {
    private final Map<String, String> ids = new HashMap<>();
    private final List<ReefBackupMappingDto> added = new ArrayList<>();

    /** 装载本用户已恢复的编号映射。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public ReefBackupRestoreContext(List<ReefBackupMappingDto> existing) {
        for (ReefBackupMappingDto item : existing) ids.put(item.getEntityType() + ":" + item.getOriginalId(), item.getRestoredId());
    }

    /** 判断备份内记录是否已经恢复。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public boolean contains(String type, String oldId) { return ids.containsKey(type + ":" + oldId); }

    /** 取得已恢复引用，缺失时明确中止当前批次。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public String require(String type, String oldId) {
        String value = ids.get(type + ":" + oldId);
        if (value == null) throw new ReefJournalException("备份中的关联记录缺失，请确认备份完整");
        return value;
    }

    /** 登记本批新记录及其原编号，随后批量写入数据库。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public void add(String type, String oldId, String newId) {
        if (oldId == null || !oldId.matches("[1-9][0-9]{0,18}") || contains(type, oldId)) throw new ReefJournalException("备份中的记录编号无效或重复");
        ReefBackupMappingDto item = new ReefBackupMappingDto();
        item.setEntityType(type);
        item.setOriginalId(oldId);
        item.setRestoredId(newId);
        ids.put(type + ":" + oldId, newId);
        added.add(item);
    }

    /** 返回本批新产生的编号映射。 @Author zhanghao_SMEICS @Date 2026-11-12 */
    public List<ReefBackupMappingDto> getAdded() { return added; }
}
