package com.xuefen.agent.llm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 大模型（LLM）客户端
 * 基于 OpenAI 兼容的 Chat Completions 接口，当前接入智谱 AI（BigModel）。
 *
 * 职责：
 * 1. 读取 ai.llm 配置，构建带超时的 RestTemplate
 * 2. 提供通用 chat(system, user) 方法
 * 3. 提供 Agent 专用的 planTools（工具规划）和 summarize（结果总结）方法
 * 4. 所有异常向上抛出，由 AgentService 决定是否回退规则引擎
 */
@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    @Autowired
    private LlmProperties props;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = Math.max(1, props.getTimeoutSeconds()) * 1000;
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(factory);
        log.info("[LLM] 初始化完成: enabled={}, baseUrl={}, model={}, timeout={}s",
                props.isEnabled(), props.getBaseUrl(), props.getModel(), props.getTimeoutSeconds());
    }

    /** 是否具备调用 LLM 的条件 */
    public boolean isReady() {
        return props.isReady();
    }

    /**
     * 通用对话补全
     * @param systemPrompt 系统提示词
     * @param userContent  用户内容
     * @return 模型返回的文本
     */
    public String chat(String systemPrompt, String userContent) {
        if (!isReady()) {
            throw new IllegalStateException("LLM 未启用或缺少 api-key/base-url 配置");
        }

        List<JSONObject> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
            messages.add(sys);
        }
        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", userContent);
        messages.add(user);

        JSONObject body = new JSONObject();
        body.put("model", props.getModel());
        body.put("messages", messages);
        body.put("temperature", 0.3);
        body.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(props.getApiKey());

        String url = props.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        log.debug("[LLM] 请求 {} model={}, messages={}", url, props.getModel(), messages.size());

        ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.POST,
                new HttpEntity<>(body.toJSONString(), headers),
                String.class);

        if (resp.getStatusCode() != HttpStatus.OK || resp.getBody() == null) {
            throw new RuntimeException("LLM 请求失败: HTTP " + resp.getStatusCode());
        }

        JSONObject json = JSON.parseObject(resp.getBody());
        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("LLM 返回为空: " + resp.getBody());
        }
        String content = choices.getJSONObject(0).getJSONObject("message").getString("content");
        if (content == null) {
            throw new RuntimeException("LLM 返回 content 为空");
        }
        return content.trim();
    }

    // ==================== Agent 专用方法 ====================

    /**
     * 让 LLM 根据用户问题和可用工具清单，规划工具调用链。
     * 返回严格 JSON：{"intent":"...","toolCalls":[{"name":"...","params":{...}}]}
     */
    public PlanResult planTools(String question, String toolsDescription) {
        String systemPrompt = buildPlanningSystemPrompt(toolsDescription);
        String raw = chat(systemPrompt, "用户问题：" + question);
        return parsePlanResult(raw);
    }

    /**
     * 让 LLM 基于工具执行结果，生成面向用户的自然语言回答。
     */
    public String summarize(String question, String toolResultsText) {
        String systemPrompt = "你是校园课程学业规划智能助手。请根据用户的问题和工具执行结果，生成一段自然、清晰、友好的中文回答。\n"
                + "要求：\n"
                + "1. 严格基于工具结果中的事实数据回答，不要编造数据。\n"
                + "2. 结构清晰，可使用分点、小标题。\n"
                + "3. 语气亲切专业，像一位学业指导老师。\n"
                + "4. 回答中不要提及\"工具\"\"API\"\"调用\"等内部机制。\n"
                + "5. 如果工具结果显示存在风险（如挂科、学分不足），要明确提醒并给出建议。";
        String userContent = "用户问题：" + question + "\n\n工具执行结果：\n" + toolResultsText
                + "\n\n请给出最终回答：";
        return chat(systemPrompt, userContent);
    }

    private String buildPlanningSystemPrompt(String toolsDescription) {
        return "你是校园课程学业规划智能助手的\"任务规划器\"。你的唯一职责是：根据用户提问，从可用工具中选择合适的工具并给出参数，生成一个完整的工具调用计划。\n\n"
                + "可用工具清单：\n" + toolsDescription + "\n"
                + "核心规则：\n"
                + "1. 只能调用上述工具，不得编造工具名。\n"
                + "2. student_info_query 只是身份确认的第一步，绝不能作为唯一工具。涉及学分、成绩、选课、挂科、毕业的问题，必须继续调用对应的分析工具。\n"
                + "3. 工具调用顺序需符合逻辑：先 student_info_query 确认身份，再 credit_analysis 分析现状，最后根据问题调用 course_recommendation 或 credit_recovery。\n"
                + "4. 参数值必须从用户问题中提取；无法确定的参数不要填写，系统会自动注入 studentId。\n"
                + "5. 严格输出 JSON，不要输出任何其他文字，不要用代码块包裹。格式：\n"
                + "{\"intent\":\"一句话概括用户意图\",\"toolCalls\":[{\"name\":\"工具名\",\"params\":{\"参数名\":\"参数值\"}}]}\n\n"
                + "示例：\n"
                + "- 问：\"我还差多少学分才能毕业？\" → 必须调用 student_info_query + credit_analysis\n"
                + "- 问：\"我高数挂科了怎么弥补？\" → 必须调用 student_info_query + credit_analysis + credit_recovery\n"
                + "- 问：\"大三下学期怎么选课？\" → 必须调用 student_info_query + credit_analysis + course_query + course_recommendation\n"
                + "- 问：\"有哪些选修课？\" → 只需调用 course_query";
    }

    /**
     * 解析 LLM 返回的规划 JSON，兼容可能的 markdown 代码块包裹。
     */
    private PlanResult parsePlanResult(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new RuntimeException("LLM 规划返回为空");
        }
        String text = raw.trim();
        // 去除可能的 ```json ... ``` 包裹
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastBacktick = text.lastIndexOf("```");
            if (firstNewline > 0 && lastBacktick > firstNewline) {
                text = text.substring(firstNewline + 1, lastBacktick).trim();
            }
        }
        // 找到第一个 { 和最后一个 }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }

        JSONObject json = JSON.parseObject(text);
        PlanResult result = new PlanResult();
        result.intent = json.getString("intent");
        result.toolCalls = new ArrayList<>();
        JSONArray arr = json.getJSONArray("toolCalls");
        if (arr != null) {
            for (int i = 0; i < arr.size(); i++) {
                JSONObject call = arr.getJSONObject(i);
                PlanCall pc = new PlanCall();
                pc.name = call.getString("name");
                pc.params = call.getJSONObject("params");
                result.toolCalls.add(pc);
            }
        }
        return result;
    }

    /** LLM 规划结果 */
    public static class PlanResult {
        public String intent;
        public List<PlanCall> toolCalls;
    }

    /** 单次工具调用规划 */
    public static class PlanCall {
        public String name;
        public Map<String, Object> params;
    }
}
