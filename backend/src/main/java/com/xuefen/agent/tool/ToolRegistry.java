package com.xuefen.agent.tool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 工具注册器 - 管理所有 Agent 可调用的工具
 */
@Component
public class ToolRegistry {

    @Autowired
    private List<AgentTool> tools;

    private final Map<String, AgentTool> toolMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (AgentTool tool : tools) {
            toolMap.put(tool.getName(), tool);
        }
        System.out.println("[Agent] 已注册 " + toolMap.size() + " 个工具: " + toolMap.keySet());
    }

    /**
     * 根据名称获取工具
     */
    public AgentTool getTool(String name) {
        return toolMap.get(name);
    }

    /**
     * 获取所有工具
     */
    public Collection<AgentTool> getAllTools() {
        return toolMap.values();
    }

    /**
     * 根据用户问题匹配可能需要的工具（基于关键词）
     */
    public List<AgentTool> matchTools(String question) {
        List<AgentTool> matched = new ArrayList<>();
        String lowerQuestion = question.toLowerCase();
        for (AgentTool tool : toolMap.values()) {
            for (String keyword : tool.getKeywords()) {
                if (lowerQuestion.contains(keyword.toLowerCase())) {
                    matched.add(tool);
                    break;
                }
            }
        }
        return matched;
    }

    /**
     * 获取所有工具的描述（用于提示词）
     */
    public String getToolsDescription() {
        StringBuilder sb = new StringBuilder();
        for (AgentTool tool : toolMap.values()) {
            sb.append("- ").append(tool.getName()).append(": ")
              .append(tool.getDescription()).append("\n");
        }
        return sb.toString();
    }
}
