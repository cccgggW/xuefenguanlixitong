package com.xuefen.agent.tool;

import com.xuefen.entity.Course;
import com.xuefen.service.GraduationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 工具5：挂科学分弥补方案工具（核心工具）
 * Agent 动作：查询挂科记录、选修课库、学分规则，筛选 + 推理给出建议
 * 体现：挂科分析 + 学分缺口计算 + 选修课匹配 + 弥补方案生成
 */
@Component
public class CreditRecoveryTool implements AgentTool {

    @Autowired
    private GraduationService graduationService;

    @Override
    public String getName() {
        return "credit_recovery";
    }

    @Override
    public String getDescription() {
        return "挂科学分弥补方案：查询学生挂科记录、计算学分缺口、匹配可选选修课、生成弥补方案。参数: studentId(学生ID)";
    }

    @Override
    public String[] getKeywords() {
        return new String[]{"挂科", "不及格", "补考", "重修", "弥补", "学分缺口", "挂了", "没过", "补救"};
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        try {
            if (!params.containsKey("studentId") || params.get("studentId") == null) {
                return ToolResult.fail("缺少学生ID参数");
            }
            Long studentId = Long.valueOf(params.get("studentId").toString());

            // 调用核心业务逻辑：挂科分析 + 学分缺口 + 选修课匹配
            Map<String, Object> recovery = graduationService.creditRecoveryPlan(studentId);

            // 提取关键数据
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> failed = (List<Map<String, Object>>) recovery.get("failedCourses");
            BigDecimal failedCredit = (BigDecimal) recovery.get("failedCredit");
            BigDecimal earned = (BigDecimal) recovery.get("earnedCredit");
            BigDecimal totalRequired = (BigDecimal) recovery.get("totalRequired");
            BigDecimal remaining = (BigDecimal) recovery.get("remainingCredit");
            @SuppressWarnings("unchecked")
            List<Course> recoveryCourses = (List<Course>) recovery.get("recoveryCourses");
            BigDecimal recovered = (BigDecimal) recovery.get("recoveryCredit");
            BigDecimal gap = (BigDecimal) recovery.get("creditGap");

            // 生成弥补方案摘要
            StringBuilder summary = new StringBuilder();
            summary.append("【挂科学分弥补方案】\n\n");

            if (failed.isEmpty()) {
                summary.append("✅ 恭喜！你没有挂科记录，无需学分弥补。\n");
                summary.append("当前已修学分: ").append(earned).append(" / ").append(totalRequired).append("\n");
                summary.append("距离毕业还差: ").append(remaining).append(" 学分\n");
                return ToolResult.ok(summary.toString(), recovery);
            }

            summary.append("⚠️ 挂科情况:\n");
            for (Map<String, Object> f : failed) {
                summary.append("- ").append(f.get("course_name"))
                       .append(" (").append(f.get("course_code")).append(")")
                       .append(" 成绩: ").append(f.get("score"))
                       .append(" 学分损失: ").append(f.get("credit")).append("\n");
            }
            summary.append("共挂科 ").append(failed.size()).append(" 门，损失学分: ").append(failedCredit).append("\n\n");

            summary.append("📊 学分概况:\n");
            summary.append("已获得学分: ").append(earned).append(" / ").append(totalRequired).append("\n");
            summary.append("距离毕业还差: ").append(remaining).append(" 学分\n\n");

            summary.append("💡 弥补方案（推荐选修课）:\n");
            if (recoveryCourses.isEmpty()) {
                summary.append("暂无可推荐的选修课，请联系教务老师。\n");
            } else {
                for (int i = 0; i < recoveryCourses.size(); i++) {
                    Course c = recoveryCourses.get(i);
                    summary.append(i + 1).append(". ").append(c.getCourseName())
                           .append(" (").append(c.getCourseCode()).append(")")
                           .append(" - ").append(c.getCredit()).append("学分")
                           .append(" [").append(c.getCourseCategory() != null ? c.getCourseCategory() : c.getCourseType()).append("]\n");
                }
                summary.append("\n推荐课程可弥补学分: ").append(recovered).append("\n");
                if (gap.compareTo(BigDecimal.ZERO) > 0) {
                    summary.append("仍有学分缺口: ").append(gap).append("，建议额外选课或参加补考/重修。\n");
                } else {
                    summary.append("✅ 推荐课程可完全弥补挂科学分缺口！\n");
                }
            }

            summary.append("\n📌 建议:\n");
            summary.append("1. 优先参加挂科课程的补考或重修，恢复原课程学分\n");
            summary.append("2. 同时选修上述推荐课程，确保总学分达标\n");
            summary.append("3. 注意选修课的选课限制和先修要求\n");
            summary.append("4. 建议每学期选课不超过25学分，避免学业负担过重");

            return ToolResult.ok(summary.toString(), recovery);
        } catch (Exception e) {
            return ToolResult.fail("学分弥补方案生成失败: " + e.getMessage());
        }
    }
}
