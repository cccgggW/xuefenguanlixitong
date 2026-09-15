package com.xuefen.agent;

import com.xuefen.agent.llm.LlmClient;
import com.xuefen.agent.tool.AgentTool;
import com.xuefen.agent.tool.ToolRegistry;
import com.xuefen.entity.Student;
import com.xuefen.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI Agent 核心服务
 * 职责：意图识别 → 工具选择 → 多工具编排调用 → 综合回答生成
 *
 * 接入模式（双引擎）：
 * - LLM 引擎（默认）：调用智谱大模型做意图识别、工具规划、自然语言回答生成
 * - 规则引擎（兜底）：LLM 不可用或调用失败时，自动回退到关键词匹配 + 硬编码工具链
 *
 * 这不是简单的单表查询，而是：
 * 1. 解析用户模糊问题，识别意图
 * 2. 拆解任务，确定需要调用哪些工具
 * 3. 按依赖顺序执行工具（前一个工具的输出作为后一个的输入）
 * 4. 综合多个工具的结果，生成有逻辑推理的回答
 */
@Service
public class AgentService {

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private StudentService studentService;

    @Autowired
    private LlmClient llmClient;

    /**
     * Agent 问答主入口
     * @param question 用户问题
     * @param studentId 当前学生ID（可选，用于个性化回答）
     * @param studentNo 当前学号（可选）
     */
    public AgentResponse chat(String question, Long studentId, String studentNo) {
        AgentResponse response = new AgentResponse();
        response.setQuestion(question);
        List<String> thoughtChain = new ArrayList<>();
        List<Map<String, Object>> toolCalls = new ArrayList<>();

        // 步骤1：解析学生身份
        Student student = resolveStudent(studentId, studentNo);
        if (student != null) {
            response.setStudentId(student.getId());
            response.setStudentName(student.getName());
            thoughtChain.add("识别到学生: " + student.getName() + "(" + student.getStudentNo() + "), "
                    + student.getGrade() + student.getMajor() + "专业");
        }

        // 步骤2：意图识别（规则引擎关键词匹配）
        Intent intent = recognizeIntent(question);
        List<ToolCallPlan> plan;
        boolean llmPlanned = false;

        if (intent == Intent.COMPREHENSIVE && llmClient.isReady()) {
            // 未命中任何关键词：不调用工具，直接交由 LLM 回答（见步骤4）
            intent = Intent.CHITCHAT;
            plan = new ArrayList<>();
            thoughtChain.add("规则引擎未命中关键词，直接调用 LLM 回答（不调用工具）");
        } else if (requiresStudent(intent) && student == null) {
            // 需要学生数据但未选中学生：引导用户先选择要询问的同学
            thoughtChain.add("未选中学生，无法执行需要学生数据的工具");
            response.setIntent(intent.name());
            response.setThoughtChain(thoughtChain);
            response.setToolCalls(toolCalls);
            response.setAnswer(buildNoStudentAnswer(intent));
            response.setSuccess(true);
            return response;
        } else {
            // 工具链由规则引擎生成，保证完整性和正确性
            plan = planToolCalls(intent, question, student);
            if (llmClient.isReady()) {
                llmPlanned = true; // 标记启用 LLM，后续用 LLM 生成回答
                thoughtChain.add("规则引擎识别意图: " + intent.getDescription() + "（LLM 将用于回答生成）");
            } else {
                thoughtChain.add("LLM 未启用，使用规则引擎: " + intent.getDescription());
            }
            thoughtChain.add("工具调用计划: " + plan.size() + " 个工具");
            for (ToolCallPlan p : plan) {
                thoughtChain.add("  → " + p.toolName + ": " + p.reason);
            }
        }

        response.setIntent(intent.name());

        // 步骤3：按顺序执行工具（支持前一个输出作为后一个输入）
        Map<String, Object> context = new HashMap<>();
        if (student != null) {
            context.put("studentId", student.getId());
            context.put("studentNo", student.getStudentNo());
        }

        StringBuilder finalAnswer = new StringBuilder();
        boolean hasError = false;

        for (int i = 0; i < plan.size(); i++) {
            ToolCallPlan p = plan.get(i);
            AgentTool tool = toolRegistry.getTool(p.toolName);
            if (tool == null) {
                thoughtChain.add("工具 " + p.toolName + " 未找到，跳过");
                continue;
            }

            // 合并上下文参数（系统注入的 studentId 等会覆盖 LLM 给出的同名参数）
            Map<String, Object> params = new HashMap<>(p.params);
            params.putAll(context);

            thoughtChain.add("执行工具[" + (i+1) + "/" + plan.size() + "]: " + tool.getName());

            try {
                AgentTool.ToolResult result = tool.execute(params);
                Map<String, Object> callRecord = new LinkedHashMap<>();
                callRecord.put("tool", tool.getName());
                callRecord.put("description", tool.getDescription());
                callRecord.put("success", result.isSuccess());
                callRecord.put("message", result.getMessage());
                toolCalls.add(callRecord);

                if (result.isSuccess()) {
                    thoughtChain.add("  结果: " + result.getMessage());
                    // 将结果存入上下文，供后续工具使用
                    context.put(tool.getName() + "_result", result.getData());

                    // 累加回答内容（作为 LLM 总结的输入，也是兜底输出）
                    if (result.getMessage() != null && !result.getMessage().isEmpty()) {
                        if (finalAnswer.length() > 0) {
                            finalAnswer.append("\n\n");
                        }
                        finalAnswer.append(result.getMessage());
                    }
                } else {
                    thoughtChain.add("  失败: " + result.getMessage());
                    hasError = true;
                }
            } catch (Exception e) {
                thoughtChain.add("  异常: " + e.getMessage());
                hasError = true;
                Map<String, Object> callRecord = new LinkedHashMap<>();
                callRecord.put("tool", tool.getName());
                callRecord.put("success", false);
                callRecord.put("message", e.getMessage());
                toolCalls.add(callRecord);
            }
        }

        // 步骤4：综合生成最终回答（优先 LLM 总结，失败回退拼接）
        response.setThoughtChain(thoughtChain);
        response.setToolCalls(toolCalls);

        String answerText;
        if (finalAnswer.length() == 0) {
            // 没有工具结果：闲聊或未命中关键词的普通问题，直接由 LLM 回答，否则用兜底引导
            if (intent == Intent.CHITCHAT && llmClient.isReady()) {
                try {
                    answerText = llmClient.chat(buildDirectAnswerPrompt(student), question);
                    thoughtChain.add("LLM 直接回答（未调用工具）");
                } catch (Exception e) {
                    thoughtChain.add("LLM 直接回答失败，使用固定回复: " + e.getMessage());
                    answerText = generateChitchatFallback();
                }
            } else {
                answerText = generateFallbackAnswer(question, student);
            }
        } else if (llmPlanned && llmClient.isReady()) {
            // LLM 规划成功时，用 LLM 基于工具结果生成自然语言回答
            try {
                answerText = llmClient.summarize(question, finalAnswer.toString());
                if (answerText == null || answerText.trim().isEmpty()) {
                    answerText = finalAnswer.toString();
                    thoughtChain.add("LLM 回答为空，使用工具结果拼接");
                } else {
                    thoughtChain.add("LLM 生成最终回答（基于工具结果综合推理）");
                }
            } catch (Exception e) {
                answerText = finalAnswer.toString();
                thoughtChain.add("LLM 回答生成失败，使用工具结果拼接: " + e.getMessage());
            }
        } else if (!hasError && plan.size() > 1) {
            // 规则引擎多工具调用成功时，添加综合总结
            finalAnswer.append("\n\n---\n");
            finalAnswer.append(generateSummary(intent, student, context));
            answerText = finalAnswer.toString();
        } else {
            answerText = finalAnswer.toString();
        }

        response.setAnswer(answerText);
        response.setSuccess(!hasError || finalAnswer.length() > 0);

        return response;
    }

    /**
     * 解析学生身份
     */
    private Student resolveStudent(Long studentId, String studentNo) {
        if (studentId != null) {
            return studentService.getById(studentId);
        }
        if (studentNo != null && !studentNo.isEmpty()) {
            return studentService.getByStudentNo(studentNo);
        }
        // 未指定学生：不默认关联任何学生，由上层引导用户先选择要询问的同学
        return null;
    }

    /**
     * 将 LLM 返回的意图文本映射到 Intent 枚举
     */
    private Intent mapIntent(String intentText) {
        if (intentText == null || intentText.trim().isEmpty()) {
            return Intent.COMPREHENSIVE;
        }
        String t = intentText.toLowerCase();
        for (Intent i : Intent.values()) {
            if (i.name().toLowerCase().equals(t) || i.getDescription().equals(intentText.trim())) {
                return i;
            }
        }
        // 模糊匹配：包含关键词
        if (t.contains("挂科") || t.contains("弥补") || t.contains("补考") || t.contains("重修")) {
            return Intent.CREDIT_RECOVERY;
        }
        if (t.contains("选课") || t.contains("推荐") || t.contains("下学期")) {
            return Intent.COURSE_RECOMMENDATION;
        }
        if (t.contains("学分") || t.contains("毕业") || t.contains("gpa") || t.contains("绩点")) {
            return Intent.CREDIT_ANALYSIS;
        }
        if (t.contains("课程")) {
            return Intent.COURSE_QUERY;
        }
        if (t.contains("学生") || t.contains("信息") || t.contains("学号")) {
            return Intent.STUDENT_INFO;
        }
        if (t.contains("闲聊") || t.contains("你好") || t.contains("你是谁") || t.contains("介绍")
                || t.contains("能做什么") || t.contains("谢谢") || t.contains("再见")) {
            return Intent.CHITCHAT;
        }
        return Intent.COMPREHENSIVE;
    }

    /**
     * 意图识别（规则引擎兜底）
     */
    private Intent recognizeIntent(String question) {
        String q = question.toLowerCase();

        // 挂科/弥补类
        if (containsAny(q, "挂科", "挂了", "不及格", "补考", "重修", "弥补", "补救", "没过")) {
            return Intent.CREDIT_RECOVERY;
        }

        // 学分/毕业进度类
        if (containsAny(q, "学分", "毕业", "还差", "已修", "未修", "能不能毕业", "够不够", "GPA", "绩点")) {
            return Intent.CREDIT_ANALYSIS;
        }

        // 选课推荐类
        if (containsAny(q, "选课", "推荐", "下学期", "怎么选", "选什么", "考研", "项目实践", "选课方案")) {
            return Intent.COURSE_RECOMMENDATION;
        }

        // 课程查询类
        if (containsAny(q, "课程", "必修课", "选修课", "先修", "课程库", "有哪些课")) {
            return Intent.COURSE_QUERY;
        }

        // 学生信息类
        if (containsAny(q, "我的信息", "个人信息", "学生信息", "学号", "专业", "班级")) {
            return Intent.STUDENT_INFO;
        }

        // 闲聊/普通问题类（不调用任何工具，直接由 LLM 回答）
        if (containsAny(q, "你好", "您好", "hi", "hello", "在吗", "在不在",
                "你是谁", "你是什么", "什么ai", "介绍一下", "自我介绍",
                "能做什么", "会什么", "功能", "谢谢", "感谢", "多谢",
                "再见", "拜拜", "bye", "thanks", "早上好", "下午好", "晚上好")) {
            return Intent.CHITCHAT;
        }

        // 默认：综合分析
        return Intent.COMPREHENSIVE;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 规划工具调用链（规则引擎兜底）
     * 关键：根据意图确定工具执行顺序，前一个工具的输出可作为后一个的输入
     */
    private List<ToolCallPlan> planToolCalls(Intent intent, String question, Student student) {
        List<ToolCallPlan> plan = new ArrayList<>();
        Map<String, Object> baseParams = new HashMap<>();
        if (student != null) {
            baseParams.put("studentId", student.getId());
        }

        switch (intent) {
            case CREDIT_ANALYSIS:
                // 学分分析：①学生信息 ②学分进度分析（多表联合+计算）
                plan.add(new ToolCallPlan("student_info_query", "确认学生身份信息", baseParams));
                plan.add(new ToolCallPlan("credit_analysis", "查询已修学分、毕业要求、未修必修课，做数学计算", baseParams));
                break;

            case COURSE_RECOMMENDATION:
                // 选课推荐：①学生信息 ②学分分析（了解现状）③课程库查询 ④选课推荐（推理）
                plan.add(new ToolCallPlan("student_info_query", "确认学生身份和专业年级", baseParams));
                plan.add(new ToolCallPlan("credit_analysis", "分析当前学分进度和未修必修课", baseParams));
                plan.add(new ToolCallPlan("course_query", "查询可选课程库和先修依赖", baseParams));
                Map<String, Object> recParams = new HashMap<>(baseParams);
                recParams.put("targetSemester", extractSemester(question));
                recParams.put("maxCredit", extractMaxCredit(question));
                recParams.put("focus", extractFocus(question));
                plan.add(new ToolCallPlan("course_recommendation", "基于学分约束+先修依赖+侧重点，推理生成选课方案", recParams));
                break;

            case CREDIT_RECOVERY:
                // 挂科弥补：①学生信息 ②学分分析 ③挂科弥补方案
                plan.add(new ToolCallPlan("student_info_query", "确认学生身份", baseParams));
                plan.add(new ToolCallPlan("credit_analysis", "分析当前学分和挂科影响", baseParams));
                plan.add(new ToolCallPlan("credit_recovery", "查询挂科记录、匹配选修课、生成弥补方案", baseParams));
                break;

            case COURSE_QUERY:
                plan.add(new ToolCallPlan("course_query", "查询课程库信息", baseParams));
                break;

            case STUDENT_INFO:
                plan.add(new ToolCallPlan("student_info_query", "查询学生基本信息", baseParams));
                break;

            case CHITCHAT:
                // 闲聊类问题：不调用任何工具，直接由 LLM 回答
                break;

            case COMPREHENSIVE:
            default:
                // 综合问题：全量分析
                plan.add(new ToolCallPlan("student_info_query", "确认学生身份", baseParams));
                plan.add(new ToolCallPlan("credit_analysis", "全面学分进度分析", baseParams));
                plan.add(new ToolCallPlan("course_recommendation", "给出选课建议", baseParams));
                break;
        }

        return plan;
    }

    private String extractSemester(String question) {
        if (question.contains("大三") || question.contains("3年级")) return "第5学期";
        if (question.contains("大二") || question.contains("2年级")) return "第3学期";
        if (question.contains("大四") || question.contains("4年级")) return "第7学期";
        return "第5学期";
    }

    private String extractMaxCredit(String question) {
        // 可从问题中提取，默认20
        return "20";
    }

    private String extractFocus(String question) {
        if (question.contains("考研")) return "考研";
        if (question.contains("项目") || question.contains("实践") || question.contains("就业")) return "项目实践";
        return "均衡";
    }

    /**
     * 判断该意图是否必须依赖学生数据
     */
    private boolean requiresStudent(Intent intent) {
        switch (intent) {
            case CREDIT_ANALYSIS:
            case COURSE_RECOMMENDATION:
            case CREDIT_RECOVERY:
            case STUDENT_INFO:
            case COMPREHENSIVE:
                return true;
            default:
                return false;
        }
    }

    /**
     * 未选中学生时的引导回答（不调用任何工具）
     */
    private String buildNoStudentAnswer(Intent intent) {
        return "请先在右侧「学生查询」中选中具体要询问的同学（输入学号或姓名后点击查询），"
                + "我才能为你做" + intent.getDescription() + "。\n\n"
                + "如果你不需要针对某位学生，也可以直接问我课程相关的问题，例如：\"有哪些选修课？\"";
    }

    /**
     * 直接回答（不调用工具）时使用的系统提示词
     */
    private String buildDirectAnswerPrompt(Student student) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是校园课程学业规划智能助手，一位亲切专业的学业指导老师。\n");
        sb.append("请用自然、友好、简洁的中文回答用户的问题。\n");
        sb.append("要求：\n");
        sb.append("1. 一般性的知识、学习方法、系统使用等问题可以直接回答。\n");
        sb.append("2. 涉及某位学生具体的学分、成绩、选课、挂科数据时不要编造，应引导用户换用更明确的问法，"
                + "例如：\"我还差多少学分才能毕业\"\"帮我推荐下学期选课\"\"我高数挂科了怎么弥补\"\"我的GPA是多少\"\"有哪些选修课\"。\n");
        sb.append("3. 如果用户问你是谁或能做什么，请简要介绍自己的核心功能：学分进度分析、智能选课推荐、挂科学分弥补、课程查询。\n");
        sb.append("4. 不要提及\"工具\"\"API\"\"调用\"等内部机制。\n");
        if (student != null) {
            sb.append("5. 当前已选中学生：").append(student.getName()).append("（")
                    .append(student.getStudentNo()).append("，").append(student.getGrade())
                    .append(student.getMajor()).append("）。可参考该生的基本信息，但不得编造其学分、成绩、选课等具体数据。\n");
        }
        return sb.toString();
    }

    /**
     * 生成综合总结（规则引擎兜底）
     */
    private String generateSummary(Intent intent, Student student, Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 综合总结:\n");
        if (student != null) {
            sb.append("学生 ").append(student.getName()).append("(").append(student.getStudentNo()).append(")");
            sb.append("，").append(student.getGrade()).append(student.getMajor()).append("专业\n");
        }
        sb.append("以上分析基于课程库、选课记录、成绩表和培养方案规则的多表联合查询与数学计算。");
        sb.append("如需更详细的信息或有其他问题，请继续提问。");
        return sb.toString();
    }

    /**
     * 闲聊兜底回复（LLM 不可用时使用）
     */
    private String generateChitchatFallback() {
        return "你好！我是校园课程学业规划智能助手 🎓\n\n"
                + "我可以帮你：\n"
                + "1. 📊 分析学分进度，计算还差多少学分毕业\n"
                + "2. 📚 智能推荐选课方案（兼顾考研/项目实践）\n"
                + "3. 🔧 挂科学分弥补方案\n"
                + "4. 📖 课程查询和先修依赖分析\n\n"
                + "有什么学业问题尽管问我！";
    }

    /**
     * 兜底回答
     */
    private String generateFallbackAnswer(String question, Student student) {
        StringBuilder sb = new StringBuilder();
        sb.append("你好！我是学业规划智能助手。\n\n");
        sb.append("我可以帮你解答以下问题：\n");
        sb.append("1. 📊 学分进度：\"我还差多少学分才能毕业？\"\n");
        sb.append("2. 📚 选课推荐：\"大三下学期怎么选课？兼顾考研和项目实践\"\n");
        sb.append("3. 🔧 挂科弥补：\"我高数挂科了，哪些选修课可以弥补学分？\"\n");
        sb.append("4. 📖 课程查询：\"有哪些选修课？计算机专业的必修课有哪些？\"\n\n");
        if (student != null) {
            sb.append("当前关联学生: ").append(student.getName()).append("(").append(student.getStudentNo()).append(")\n");
        }
        sb.append("请尝试用更具体的方式提问，我会调用多个工具为你综合分析！");
        return sb.toString();
    }

    /**
     * 意图枚举
     */
    public enum Intent {
        CREDIT_ANALYSIS("学分进度分析"),
        COURSE_RECOMMENDATION("智能选课推荐"),
        CREDIT_RECOVERY("挂科学分弥补"),
        COURSE_QUERY("课程信息查询"),
        STUDENT_INFO("学生信息查询"),
        CHITCHAT("闲聊/普通问题"),
        COMPREHENSIVE("综合分析");

        private final String description;
        Intent(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    /**
     * 工具调用计划
     */
    private static class ToolCallPlan {
        String toolName;
        String reason;
        Map<String, Object> params;

        ToolCallPlan(String toolName, String reason, Map<String, Object> params) {
            this.toolName = toolName;
            this.reason = reason;
            this.params = new HashMap<>(params);
        }
    }

    /**
     * Agent 响应结构
     */
    public static class AgentResponse {
        private String question;
        private String intent;
        private Long studentId;
        private String studentName;
        private String answer;
        private List<String> thoughtChain;
        private List<Map<String, Object>> toolCalls;
        private boolean success;

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }
        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        public List<String> getThoughtChain() { return thoughtChain; }
        public void setThoughtChain(List<String> thoughtChain) { this.thoughtChain = thoughtChain; }
        public List<Map<String, Object>> getToolCalls() { return toolCalls; }
        public void setToolCalls(List<Map<String, Object>> toolCalls) { this.toolCalls = toolCalls; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }
}
