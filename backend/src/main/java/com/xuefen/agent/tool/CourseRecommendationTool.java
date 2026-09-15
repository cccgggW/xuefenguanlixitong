package com.xuefen.agent.tool;

import com.xuefen.entity.Course;
import com.xuefen.service.GraduationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 工具4：智能选课推荐工具（核心工具）
 * Agent 动作：读取课程信息、学分约束、课程先后修依赖，推理给出选课方案
 * 体现：先修依赖检查 + 学分预算分配 + 侧重点推理 + 方案生成
 */
@Component
public class CourseRecommendationTool implements AgentTool {

    @Autowired
    private GraduationService graduationService;

    @Override
    public String getName() {
        return "course_recommendation";
    }

    @Override
    public String getDescription() {
        return "智能选课推荐：根据学生已修课程、先修依赖、学分预算和个人侧重点（考研/项目实践/均衡），推理生成下学期选课方案。参数: studentId(学生ID), targetSemester(目标学期), maxCredit(本学期最多学分), focus(侧重点: 考研/项目实践/均衡)";
    }

    @Override
    public String[] getKeywords() {
        return new String[]{"选课", "推荐", "下学期", "怎么选", "选课方案", "考研", "项目实践", "选什么课", "选课建议"};
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        try {
            if (!params.containsKey("studentId") || params.get("studentId") == null) {
                return ToolResult.fail("缺少学生ID参数");
            }
            Long studentId = Long.valueOf(params.get("studentId").toString());

            String targetSemester = params.containsKey("targetSemester") && params.get("targetSemester") != null
                    ? params.get("targetSemester").toString() : "第5学期";
            BigDecimal maxCredit = params.containsKey("maxCredit") && params.get("maxCredit") != null
                    ? new BigDecimal(params.get("maxCredit").toString()) : new BigDecimal("20");
            String focus = params.containsKey("focus") && params.get("focus") != null
                    ? params.get("focus").toString() : "均衡";

            // 调用核心业务逻辑：先修检查 + 学分分配 + 侧重点排序
            Map<String, Object> recommendation = graduationService.recommendCourses(
                    studentId, targetSemester, maxCredit, focus);

            // 生成推荐方案摘要
            @SuppressWarnings("unchecked")
            List<Course> recommended = (List<Course>) recommendation.get("recommendedCourses");
            BigDecimal totalCredit = (BigDecimal) recommendation.get("totalCredit");
            int recCount = (Integer) recommendation.get("recommendedCount");

            StringBuilder summary = new StringBuilder();
            summary.append("【智能选课推荐方案】\n");
            summary.append("目标学期: ").append(targetSemester).append("\n");
            summary.append("侧重点: ").append(focus).append("\n");
            summary.append("学分预算: ").append(maxCredit).append(" 学分\n");
            summary.append("推荐选课 ").append(recCount).append(" 门，共 ").append(totalCredit).append(" 学分:\n\n");

            for (int i = 0; i < recommended.size(); i++) {
                Course c = recommended.get(i);
                summary.append(i + 1).append(". ").append(c.getCourseName())
                       .append(" (").append(c.getCourseCode()).append(")")
                       .append(" - ").append(c.getCredit()).append("学分")
                       .append(" [").append(c.getCourseType()).append("]\n");
            }

            // 提示先修受阻课程
            @SuppressWarnings("unchecked")
            List<Course> blocked = (List<Course>) recommendation.get("prereqBlockedCourses");
            if (blocked != null && !blocked.isEmpty()) {
                summary.append("\n⚠️ 以下课程因先修条件未满足，暂不推荐:\n");
                for (Course c : blocked) {
                    summary.append("- ").append(c.getCourseName())
                           .append(" (需先修: ").append(c.getPrerequisites()).append(")\n");
                }
            }

            // 侧重点建议
            if ("考研".equals(focus)) {
                summary.append("\n💡 考研导向：优先推荐数学、数据结构、操作系统、计算机网络等考研核心科目。");
            } else if ("项目实践".equals(focus)) {
                summary.append("\n💡 项目实践导向：优先推荐Web开发、移动开发、数据库、软件工程等实践类课程。");
            }

            return ToolResult.ok(summary.toString(), recommendation);
        } catch (Exception e) {
            return ToolResult.fail("选课推荐失败: " + e.getMessage());
        }
    }
}
