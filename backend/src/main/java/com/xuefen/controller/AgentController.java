package com.xuefen.controller;

import com.xuefen.agent.AgentService;
import com.xuefen.agent.tool.ToolRegistry;
import com.xuefen.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI Agent 智能问答接口
 * 这是系统的核心亮点：不是简单筛选，而是多工具调用 + 业务规则 + 计算 + 推理
 */
@RestController
@RequestMapping("/agent")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @Autowired
    private ToolRegistry toolRegistry;

    /**
     * AI 问答主接口
     * 示例问题：
     * - "帮我看看我现在已修学分还差多少才能毕业，哪些必修课还没选？"
     * - "大三软件工程，下一学期怎么选课，兼顾考研和项目实践？"
     * - "我高数挂科，哪些选修课可以弥补学分缺口？"
     */
    @PostMapping("/chat")
    public Result<AgentService.AgentResponse> chat(@RequestBody Map<String, Object> request) {
        String question = (String) request.get("question");
        Long studentId = request.get("studentId") != null
                ? Long.valueOf(request.get("studentId").toString()) : null;
        String studentNo = request.get("studentNo") != null
                ? request.get("studentNo").toString() : null;

        if (question == null || question.trim().isEmpty()) {
            return Result.error("问题不能为空");
        }

        AgentService.AgentResponse response = agentService.chat(question.trim(), studentId, studentNo);
        return Result.success(response);
    }

    /**
     * 获取 Agent 已注册的工具列表
     */
    @GetMapping("/tools")
    public Result<Object> getTools() {
        return Result.success(toolRegistry.getAllTools().stream()
                .map(tool -> {
                    Map<String, Object> t = new HashMap<>();
                    t.put("name", tool.getName());
                    t.put("description", tool.getDescription());
                    t.put("keywords", tool.getKeywords());
                    return t;
                })
                .collect(Collectors.toList()));
    }

    /**
     * 快捷问题示例
     */
    @GetMapping("/examples")
    public Result<Object> getExamples() {
        return Result.success(java.util.Arrays.asList(
                "帮我看看我现在已修学分还差多少才能毕业，哪些必修课还没选？",
                "大三软件工程，下一学期怎么选课，兼顾考研和项目实践？",
                "我高数挂科了，哪些选修课可以弥补学分缺口？",
                "有哪些新文科类的选修课推荐？",
                "我的GPA是多少？各类型学分完成情况怎么样？"
        ));
    }
}
