package com.tiankong44.tool.reefjournal.support;

import com.tiankong44.tool.reefjournal.dto.ReefPageResponse;

import java.util.List;

import static com.tiankong44.tool.reefjournal.ReefJournalConstants.DEFAULT_PAGE_NUMBER;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.DEFAULT_PAGE_SIZE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.MAX_PAGE_SIZE;
import static com.tiankong44.tool.reefjournal.ReefJournalConstants.ONE;

/**
 * 海缸模块统一计算分页范围和响应结构。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
public final class ReefPageSupport {
    /**
     * 禁止实例化静态分页支持类。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     */
    private ReefPageSupport() {
    }

    /**
     * 将非法或缺失的页码归一到项目默认页码。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param pageNum 客户端页码
     * @return 可用于数据库分页的正页码
     */
    public static int pageNumber(Integer pageNum) {
        return pageNum == null || pageNum < ONE ? DEFAULT_PAGE_NUMBER : pageNum;
    }

    /**
     * 限制单页数据量，避免客户端传入过大的读取范围。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param pageSize 客户端请求条数
     * @return 归一后的单页条数
     */
    public static int pageSize(Integer pageSize) {
        if (pageSize == null || pageSize < ONE) return DEFAULT_PAGE_SIZE;
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 计算数据库 LIMIT 查询使用的偏移量。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param pageNum 已归一的页码
     * @param pageSize 已归一的单页条数
     * @return 零起始行偏移
     */
    public static long offset(int pageNum, int pageSize) {
        return (long) (pageNum - ONE) * pageSize;
    }

    /**
     * 组装统一分页响应并将总数序列化为字符串。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param pageNum 已归一的页码
     * @param pageSize 已归一的单页条数
     * @param total 命中记录总数
     * @param records 当前页记录，空值统一规范为空数组
     * @param <T> 列表元素类型
     * @return 统一分页响应
     */
    public static <T> ReefPageResponse<T> response(int pageNum, int pageSize, long total, List<T> records) {
        return new ReefPageResponse<>(pageNum, pageSize, String.valueOf(total), records == null ? java.util.Collections.emptyList() : records);
    }
}
