package com.xuefen.agent.tool;

import com.xuefen.entity.Course;
import com.xuefen.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具2：课程信息查询工具
 * Agent 动作：读取课程信息、课程类型、学分、先修依赖
 */
@Component
public class CourseQueryTool implements AgentTool {

    @Autowired
    private CourseService courseService;

    @Override
    public String getName() {
        return "course_query";
    }

    @Override
    public String getDescription() {
        return "查询课程库信息，支持按课程名称/编号搜索、按类型筛选（必修/选修/公共基础/专业基础/专业核心/实践）、查询先修依赖。参数: keyword(关键词), courseType(课程类型)";
    }

    @Override
    public String[] getKeywords() {
        return new String[]{"课程", "选课", "必修课", "选修课", "先修", "学分", "course", "课程库"};
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        try {
            Map<String, Object> result = new HashMap<>();

            // 查询所有课程概览
            List<Course> allCourses = courseService.list();
            result.put("totalCount", allCourses.size());

            // 按类型统计
            Map<String, Integer> typeCount = new HashMap<>();
            for (Course c : allCourses) {
                typeCount.merge(c.getCourseType(), 1, Integer::sum);
            }
            result.put("typeStats", typeCount);

            // 如果指定了类型，返回该类型课程
            if (params.containsKey("courseType") && params.get("courseType") != null) {
                String type = params.get("courseType").toString();
                List<Course> typed = courseService.list(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Course>()
                        .eq(Course::getCourseType, type)
                        .orderByAsc(Course::getCourseCode)
                );
                result.put("courses", typed);
                result.put("count", typed.size());
                return ToolResult.ok("查询到 " + type + " 课程 " + typed.size() + " 门", result);
            }

            // 如果指定了关键词，搜索
            if (params.containsKey("keyword") && params.get("keyword") != null) {
                String keyword = params.get("keyword").toString();
                List<Course> searched = courseService.list(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Course>()
                        .like(Course::getCourseName, keyword)
                        .or().like(Course::getCourseCode, keyword)
                );
                result.put("courses", searched);
                result.put("count", searched.size());
                return ToolResult.ok("搜索到包含\"" + keyword + "\"的课程 " + searched.size() + " 门", result);
            }

            // 默认返回必修课和选修课列表
            result.put("requiredCourses", courseService.getRequiredCourses());
            result.put("electiveCourses", courseService.getElectiveCourses());
            return ToolResult.ok("课程库共 " + allCourses.size() + " 门课程", result);
        } catch (Exception e) {
            return ToolResult.fail("课程查询失败: " + e.getMessage());
        }
    }
}
