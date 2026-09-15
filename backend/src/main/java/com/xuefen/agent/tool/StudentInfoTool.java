package com.xuefen.agent.tool;

import com.xuefen.entity.Student;
import com.xuefen.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具1：学生信息查询工具
 * Agent 动作：查询该学生基本信息
 */
@Component
public class StudentInfoTool implements AgentTool {

    @Autowired
    private StudentService studentService;

    @Override
    public String getName() {
        return "student_info_query";
    }

    @Override
    public String getDescription() {
        return "查询学生基本信息，包括学号、姓名、专业、年级、班级等。参数: studentId(学生ID) 或 studentNo(学号)";
    }

    @Override
    public String[] getKeywords() {
        return new String[]{"学生信息", "我的信息", "个人信息", "学号", "专业", "年级", "student"};
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        try {
            Student student = null;
            if (params.containsKey("studentId") && params.get("studentId") != null) {
                Long studentId = Long.valueOf(params.get("studentId").toString());
                student = studentService.getById(studentId);
            } else if (params.containsKey("studentNo") && params.get("studentNo") != null) {
                String studentNo = params.get("studentNo").toString();
                student = studentService.getByStudentNo(studentNo);
            }

            if (student == null) {
                return ToolResult.fail("未找到学生信息");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("id", student.getId());
            result.put("studentNo", student.getStudentNo());
            result.put("name", student.getName());
            result.put("gender", student.getGender());
            result.put("grade", student.getGrade());
            result.put("major", student.getMajor());
            result.put("department", student.getDepartment());
            result.put("className", student.getClassName());
            result.put("status", student.getStatus());

            return ToolResult.ok("查询到学生信息: " + student.getName() + "(" + student.getStudentNo() + "), "
                    + student.getGrade() + student.getMajor() + "专业", result);
        } catch (Exception e) {
            return ToolResult.fail("学生信息查询失败: " + e.getMessage());
        }
    }
}
