package com.tiankong44.tool.workday.generator;

import com.tiankong44.tool.workday.entity.config.ScheduleConfig;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @Description :
 * @Author zhanghao_SMEICS
 * @Date 2025/5/27  09:50
 **/
public class ScheduleGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final Map<String, ScheduleConfig> SCHEDULE_CONFIG_MAP = new HashMap<>();

    static {
        addScheduleEntry(new ScheduleConfig("◆◆", "夜班", "8:00-24:00", "fas fa-moon", "#e74c3c"));
        addScheduleEntry(new ScheduleConfig("◇", "白班", "8:00-18:00", "fas fa-sun", "#3498db"));
        addScheduleEntry(new ScheduleConfig("/", "休息", "好好休息吧", "fas fa-bed", "#2ecc71"));
        addScheduleEntry(new ScheduleConfig("M1", "中班1", "15:00-24:00", "fas fa-cloud-sun", "#3498db"));
        addScheduleEntry(new ScheduleConfig("M2", "中班", "14:00-22:00", "fas fa-cloud-moon", "#3498db"));
        addScheduleEntry(new ScheduleConfig("M3", "值班白班", "09:00-18:00", "fas fa-sun", "#3498db"));
        addScheduleEntry(new ScheduleConfig("M4", "值班中班", "15:00-09:00", "fas fa-moon", "#3498db"));
        addScheduleEntry(new ScheduleConfig("▲", "特殊", "特殊时间", "fas fa-question", "#000000"));
    }

    private static void addScheduleEntry(ScheduleConfig config) {
        SCHEDULE_CONFIG_MAP.put(config.getStatus(), config);
    }

    public static String getDescription(String status) {
        return getConfig(status).map(ScheduleConfig::getDescription).orElse("未知");
    }

    public static String getWorkTime(String status) {
        return getConfig(status).map(ScheduleConfig::getWorkTime).orElse("未知");
    }

    public static String getIcon(String status) {
        return getConfig(status).map(ScheduleConfig::getIcon).orElse("fas fa-question");
    }

    public static String getColor(String status) {
        return getConfig(status).map(ScheduleConfig::getColor).orElse("#000000");
    }

    private static Optional<ScheduleConfig> getConfig(String status) {
        return Optional.ofNullable(SCHEDULE_CONFIG_MAP.get(status));
    }

    public static ScheduleConfig getConfigEntity(String status) {
        return SCHEDULE_CONFIG_MAP.get(status);
    }

    public List<String> generateScheduleInserts(LocalDate startDate, LocalDate endDate) {
        List<String> insertStatements = new ArrayList<>();
        LocalDate currentDate = startDate;

        Map<String, String> customSchedule = getCustomSchedule();
        Map<String, String> temporarySchedule = getTemporarySchedule();
        String[] cycleSchedule = getCycleSchedule();
        int cycleIndex = 0;

        StringBuilder batchInsert = new StringBuilder("INSERT INTO future_workday (work_date, work_status, status, work_status_desc, work_time, icon, color) VALUES ").append(System.lineSeparator());
        int count = 0;

        while (!currentDate.isAfter(endDate)) {
            //String scheduleStatus = determineScheduleStatus(
            //        currentDate.format(DATE_FORMATTER),
            //        customSchedule,
            //        temporarySchedule,
            //        cycleSchedule,
            //        cycleIndex
            //);
            String scheduleStatus="";
            String formatCurrentDate = currentDate.format(DATE_FORMATTER);
            if (customSchedule.containsKey(formatCurrentDate)) {
                scheduleStatus= customSchedule.get(formatCurrentDate);
            }else if (temporarySchedule.containsKey(formatCurrentDate)){
                scheduleStatus= temporarySchedule.get(formatCurrentDate);
            }else {
                scheduleStatus = cycleSchedule[cycleIndex % cycleSchedule.length];
                cycleIndex++;
            }

            ScheduleConfig config = getConfigEntity(scheduleStatus);

            if (count > 0) {
                batchInsert.append(", ");
            }

            batchInsert.append(String.format(
                    "('%s', '%s', 1, '%s', '%s', '%s', '%s')",
                    currentDate.atStartOfDay(),
                    scheduleStatus,
                    config.getDescription(),
                    config.getWorkTime(),
                    config.getIcon(),
                    config.getColor()
            )).append(System.lineSeparator());

            count++;

            if (count == 1000) {
                insertStatements.add(batchInsert.toString());
                resetBatchInsert(batchInsert);
                count = 0;
            }

            currentDate = currentDate.plusDays(1);

        }

        if (count > 0) {
            insertStatements.add(batchInsert.toString());
        }

        return insertStatements;
    }

    private String determineScheduleStatus(
            String dateStr,
            Map<String, String> customSchedule,
            Map<String, String> temporarySchedule,
            String[] cycleSchedule,
            int cycleIndex
    ) {
        // 先检查 customSchedule
        if (customSchedule.containsKey(dateStr)) {
            return customSchedule.get(dateStr);
        }

        // 然后使用 cycleSchedule
        String cycleStatus = cycleSchedule[cycleIndex % cycleSchedule.length];

        // 最后用 temporarySchedule 覆盖（如果存在）
        if (temporarySchedule.containsKey(dateStr)) {
            return temporarySchedule.get(dateStr);
        }

        return cycleStatus;
    }

    private void resetBatchInsert(StringBuilder batchInsert) {
        batchInsert.setLength(0);
        batchInsert.append("INSERT INTO future_workday (work_date, work_status, status, work_status_desc, work_time, icon, color) VALUES ");
    }

    private Map<String, String> getCustomSchedule() {
        // 使用 HashMap 替代 Map.of()
        Map<String, String> customSchedule = new HashMap<>();
        customSchedule.put("2025/05/20", "/");
        customSchedule.put("2025/05/21", "◇");
        customSchedule.put("2025/05/22", "◇");
        customSchedule.put("2025/05/23", "◇");
        customSchedule.put("2025/05/24", "M2");
        customSchedule.put("2025/05/25", "M2");
        customSchedule.put("2025/05/26", "◇");
        customSchedule.put("2025/05/27", "◆◆");
        customSchedule.put("2025/05/28", "/");
        customSchedule.put("2025/05/29", "/");
        customSchedule.put("2025/05/30", "M2");
        customSchedule.put("2025/05/31", "M4");
        customSchedule.put("2025/06/01", "/");
        customSchedule.put("2025/06/02", "/");
        customSchedule.put("2025/06/03", "◇");
        return customSchedule;
    }

    private Map<String, String> getTemporarySchedule() {
        // 使用 HashMap 替代 Map.of()
        Map<String, String> temporarySchedule = new HashMap<>();
        // 添加临时排班
        return temporarySchedule;
    }

    private String[] getCycleSchedule() {

        String cycle = "/\t/\t◆◆\tΔΔ\t/\t/\t◇\t◆◆\t/\t/\tΔΔ\t◇";
        String[] split = cycle.split("\t");
        for (int i = 0; i < split.length; i++) {
            if (split[i].equals("Δ")) {
                split[i] = "M1";
            }
            if (split[i].equals("ΔΔ")) {
                split[i] = "M2";
            }

        }


        return split;
    }


    public static void main(String[] args) {
        ScheduleGenerator scheduleGenerator = new ScheduleGenerator();


        scheduleGenerator.generateScheduleInserts(LocalDate.of(2025, 5, 20), LocalDate.of(2025, 12, 31))
                .forEach(System.out::println);
    }
}
