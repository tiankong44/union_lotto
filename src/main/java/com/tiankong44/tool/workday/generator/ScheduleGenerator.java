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


    public List<String> generateScheduleInserts(String cycle, LocalDate startDate, LocalDate endDate) {
        List<String> insertStatements = new ArrayList<>();
        LocalDate currentDate = startDate;

        Map<String, String> customSchedule = getCustomSchedule();
        Map<String, String> temporarySchedule = getTemporarySchedule();
        String[] cycleSchedule = getCycleSchedule(cycle);
        int cycleIndex = 0;

        StringBuilder batchInsert = new StringBuilder("INSERT INTO future_workday (work_date, work_status, status) VALUES ");
        StringJoiner valuesJoiner = new StringJoiner(", ");
        int count = 0;

        while (!currentDate.isAfter(endDate)) {
            String scheduleStatus = "";
            String formatCurrentDate = currentDate.format(DATE_FORMATTER);
            if (customSchedule.containsKey(formatCurrentDate)) {
                scheduleStatus = customSchedule.get(formatCurrentDate);
            } else if (temporarySchedule.containsKey(formatCurrentDate)) {
                scheduleStatus = temporarySchedule.get(formatCurrentDate);
            } else {
                scheduleStatus = cycleSchedule[cycleIndex % cycleSchedule.length];
                cycleIndex++;
            }

            valuesJoiner.add(String.format("('%s', '%s', 1)\r\n", currentDate.atStartOfDay(), scheduleStatus));

            count++;

            if (count == 1000) {
                insertStatements.add(batchInsert.toString() + valuesJoiner);
                valuesJoiner = new StringJoiner(", ");
                count = 0;
            }

            currentDate = currentDate.plusDays(1);
        }

        if (count > 0) {
            insertStatements.add(batchInsert.toString() + valuesJoiner);
        }

        return insertStatements;
    }


    private Map<String, String> getCustomSchedule() {
        return new HashMap<>();
    }

    private Map<String, String> getTemporarySchedule() {
        return new HashMap<>();
    }

    private String[] getCycleSchedule(String cycle) {
        Map<String, String> replaceMap = new HashMap<>();
        replaceMap.put("Δ", "M1");
        replaceMap.put("ΔΔ", "M2");

        String[] split = cycle.split("\\t");
        for (int i = 0; i < split.length; i++) {
            split[i] = replaceMap.getOrDefault(split[i], split[i]);
        }
        return split;
    }


    public static void main(String[] args) {
        ScheduleGenerator scheduleGenerator = new ScheduleGenerator();

        String cycle = "/\t/\t◇\t◆◆";
        LocalDate startDate = LocalDate.of(2026, 4, 12);
        LocalDate endDate = LocalDate.of(2026, 8, 30);
        scheduleGenerator.generateScheduleInserts(cycle, startDate, endDate)
                .forEach(System.out::println);
    }
}
