package com.tiankong44.tool.reefjournal.job;

import com.tiankong44.tool.reefjournal.service.ReefReminderDispatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.tiankong44.tool.reefjournal.ReefJournalConstants.TIME_ZONE;

/**
 * 按外部配置的周期触发海缸提醒调度。
 *
 * @Author zhanghao_SMEICS
 * @Date 2026-11-05
 */
@Component
public class ReefReminderJob {
    private final ReefReminderDispatchService dispatchService;

    /**
     * 创建只负责定时触发的提醒任务。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     * @param dispatchService 分布式锁保护的提醒派发服务
     */
    public ReefReminderJob(ReefReminderDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    /**
     * 每次调度只交给共享锁保护的派发服务处理。
     *
     * @Author zhanghao_SMEICS
     * @Date 2026-11-05
     */
    @Scheduled(cron = "${reef-journal.reminder.dispatch-cron}", zone = TIME_ZONE)
    public void dispatch() {
        // 将到期筛选和订阅消息发送交给独立调度服务执行。
        dispatchService.dispatchDue();
    }
}
