package com.xuefen.agent.tool;

import com.xuefen.service.GraduationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 工具3：学分进度分析工具（核心工具）
 * Agent 动作：①查询该学生已选课程 ②查询毕业要求学分规则 ③筛选未修必修课 ④做数学计算 ⑤综合给出建议
 * 体现：多表联合 + 业务规则 + 数学计算 + 逻辑推理
 */
@Component
public class CreditAnalysisTool implements AgentTool {

    @Autowired
    private GraduationService graduationService;

    @Override
    public String getName() {
        return "credit_analysis";
    }

    @Override
    public String getDescription() {
        return "学分进度综合分析：查询学生已修学分、毕业要求、各类型学分完成率、未修必修课、GPA、挂科情况，计算还差多少学分才能毕业。参数: studentId(学生ID)";
    }

    @Override
    public String[] getKeywords() {
        return new String[]{"学分", "毕业", "还差多少", "已修", "未修", "必修课", "毕业要求", "GPA", "绩点", "学分进度", "能不能毕业"};
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        try {
            if (!params.containsKey("studentId") || params.get("studentId") == null) {
                return ToolResult.fail("缺少学生ID参数");
            }
            Long studentId = Long.valueOf(params.get("studentId").toString());

            // 调用核心业务逻辑：多表联合查询 + 数学计算
            Map<String, Object> analysis = graduationService.analyzeCreditProgress(studentId);

            // 提取关键数据用于生成建议
            BigDecimal totalEarned = (BigDecimal) analysis.get("totalEarned");
            BigDecimal totalRequired = (BigDecimal) analysis.get("totalRequired");
            BigDecimal totalRemaining = (BigDecimal) analysis.get("totalRemaining");
            BigDecimal totalProgress = (BigDecimal) analysis.get("totalProgress");
            BigDecimal gpa = (BigDecimal) analysis.get("gpa");
            int missingCount = (Integer) analysis.get("missingRequiredCount");
            int failedCount = (Integer) analysis.get("failedCount");

            // 生成综合分析摘要
            StringBuilder summary = new StringBuilder();
            summary.append("【学分进度分析】\n");
            summary.append("已修学分: ").append(totalEarned).append(" / ").append(totalRequired)
                   .append(" (完成率 ").append(totalProgress).append("%)\n");
            summary.append("距离毕业还差: ").append(totalRemaining).append(" 学分\n");
            summary.append("当前GPA: ").append(gpa).append("\n");
            summary.append("未修必修课: ").append(missingCount).append(" 门\n");
            summary.append("挂科记录: ").append(failedCount).append(" 门\n");

            // 逻辑推理：给出状态判断
            if (totalRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                summary.append("\n✅ 学分已达标，满足毕业学分要求！");
            } else if (totalProgress.compareTo(new BigDecimal("75")) >= 0) {
                summary.append("\n📈 进度良好（超过75%），按正常节奏选课即可毕业。");
            } else if (totalProgress.compareTo(new BigDecimal("50")) >= 0) {
                summary.append("\n⚠️ 进度过半，需注意未修必修课较多，建议优先选必修课。");
            } else {
                summary.append("\n🚨 进度偏慢，建议每学期多选课，优先完成必修课。");
            }

            if (failedCount > 0) {
                summary.append("\n⚠️ 存在挂科记录，挂科学分不计入已修学分，需补考或重修。");
            }

            return ToolResult.ok(summary.toString(), analysis);
        } catch (Exception e) {
            return ToolResult.fail("学分分析失败: " + e.getMessage());
        }
    }
}
