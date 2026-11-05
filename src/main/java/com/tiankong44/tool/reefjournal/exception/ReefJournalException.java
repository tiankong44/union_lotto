package com.tiankong44.tool.reefjournal.exception;

/**
 * 海缸日记模块业务异常，承载可供用户理解的失败原因。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
public class ReefJournalException extends RuntimeException {
    /**
     * 创建海缸模块业务异常。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param message 明确的业务失败说明
     */
    public ReefJournalException(String message) {
        super(message);
    }
}
