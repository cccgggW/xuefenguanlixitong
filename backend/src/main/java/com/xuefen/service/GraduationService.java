package com.xuefen.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuefen.entity.Course;
import com.xuefen.entity.GraduationRequirement;
import com.xuefen.entity.Student;
import com.xuefen.mapper.GraduationRequirementMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 毕业要求与学分规划服务 - 核心业务逻辑
 */
@Service
public class GraduationService extends ServiceImpl<GraduationRequirementMapper, GraduationRequirement> {

    @Autowired
    private GraduationRequirementMapper graduationRequirementMapper;

    @Autowired
    private StudentService studentService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private GradeService gradeService;

    @Autowired
    private EnrollmentService enrollmentService;

    /**
     * 根据专业和年级查询毕业要求
     */
    public GraduationRequirement getByMajorAndGrade(String major, String grade) {
        return graduationRequirementMapper.selectByMajorAndGrade(major, grade);
    }

    /**
     * 学生学分完成情况分析
     * 这是 Agent 调用的核心工具之一：多表联合 + 数学计算
     */
    public Map<String, Object> analyzeCreditProgress(Long studentId) {
        Student student = studentService.getById(studentId);
        if (student == null) {
            throw new RuntimeException("学生不存在");
        }

        // 1. 查询毕业要求
        GraduationRequirement req = getByMajorAndGrade(student.getMajor(), student.getGrade());
        if (req == null) {
            throw new RuntimeException("未找到该专业年级的毕业要求: " + student.getMajor() + " " + student.getGrade());
        }

        // 2. 查询已获得学分（按类型）
        List<Map<String, Object>> creditByType = gradeService.getCreditByType(studentId);
        Map<String, BigDecimal> earnedByType = new HashMap<>();
        BigDecimal totalEarned = BigDecimal.ZERO;
        for (Map<String, Object> m : creditByType) {
            String type = (String) m.get("courseType");
            BigDecimal credit = (BigDecimal) m.get("totalCredit");
            earnedByType.put(type, credit);
            totalEarned = totalEarned.add(credit);
        }

        // 3. 计算各维度完成情况
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("student", student);
        result.put("requirement", req);
        result.put("totalEarned", totalEarned);
        result.put("totalRequired", req.getTotalCredit());
        result.put("totalRemaining", req.getTotalCredit().subtract(totalEarned));
        result.put("totalProgress", calcProgress(totalEarned, req.getTotalCredit()));

        // 各类型学分完成情况
        List<Map<String, Object>> typeProgress = new ArrayList<>();
        typeProgress.add(buildTypeProgress("必修", earnedByType, req.getRequiredCredit()));
        typeProgress.add(buildTypeProgress("选修", earnedByType, req.getElectiveCredit()));
        typeProgress.add(buildTypeProgress("公共基础", earnedByType, req.getPublicBasicCredit()));
        typeProgress.add(buildTypeProgress("专业基础", earnedByType, req.getMajorBasicCredit()));
        typeProgress.add(buildTypeProgress("专业核心", earnedByType, req.getMajorCoreCredit()));
        typeProgress.add(buildTypeProgress("实践", earnedByType, req.getPracticeCredit()));
        result.put("typeProgress", typeProgress);

        // 4. 未修必修课
        List<Course> missingRequired = getMissingRequiredCourses(studentId);
        result.put("missingRequiredCourses", missingRequired);
        result.put("missingRequiredCount", missingRequired.size());

        // 5. GPA
        result.put("gpa", gradeService.calculateGPA(studentId));

        // 6. 挂科情况
        List<Map<String, Object>> failed = gradeService.getFailedCourses(studentId);
        result.put("failedCourses", failed);
        result.put("failedCount", failed.size());

        return result;
    }

    /**
     * 构建某类型学分进度
     */
    private Map<String, Object> buildTypeProgress(String type, Map<String, BigDecimal> earned, BigDecimal required) {
        Map<String, Object> m = new LinkedHashMap<>();
        BigDecimal earnedCredit = earned.getOrDefault(type, BigDecimal.ZERO);
        m.put("type", type);
        m.put("earned", earnedCredit);
        m.put("required", required != null ? required : BigDecimal.ZERO);
        m.put("remaining", required != null ? required.subtract(earnedCredit) : BigDecimal.ZERO);
        m.put("progress", required != null && required.compareTo(BigDecimal.ZERO) > 0
                ? calcProgress(earnedCredit, required) : BigDecimal.ZERO);
        return m;
    }

    /**
     * 计算进度百分比
     */
    private BigDecimal calcProgress(BigDecimal earned, BigDecimal required) {
        if (required == null || required.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return earned.divide(required, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 获取学生未修的必修课列表
     * 逻辑：所有必修课 - 已选/已修课程
     */
    public List<Course> getMissingRequiredCourses(Long studentId) {
        // 所有必修课
        List<Course> allRequired = courseService.getRequiredCourses();
        // 学生已选课程ID
        List<Long> enrolledIds = enrollmentService.getCourseIdsByStudent(studentId);
        Set<Long> enrolledSet = new HashSet<>(enrolledIds);

        return allRequired.stream()
                .filter(c -> !enrolledSet.contains(c.getId()))
                .collect(Collectors.toList());
    }

    /**
     * 推荐选课方案
     * 这是 Agent 调用的核心工具：课程信息 + 学分约束 + 先修依赖 + 推理
     * @param studentId 学生ID
     * @param targetSemester 目标学期（如"第5学期"）
     * @param maxCredit 本学期最多学分
     * @param focus 侧重点（考研/项目实践/均衡）
     */
    public Map<String, Object> recommendCourses(Long studentId, String targetSemester,
                                                  BigDecimal maxCredit, String focus) {
        Student student = studentService.getById(studentId);
        if (student == null) {
            throw new RuntimeException("学生不存在");
        }

        // 1. 获取学生已通过课程编号（用于先修检查）
        List<String> passedCodes = gradeService.getPassedCourseCodes(studentId);
        // 2. 获取学生已选课程ID（避免重复推荐）
        List<Long> enrolledIds = enrollmentService.getCourseIdsByStudent(studentId);
        Set<Long> enrolledSet = new HashSet<>(enrolledIds);
        // 3. 获取未修必修课
        List<Course> missingRequired = getMissingRequiredCourses(studentId);
        // 4. 获取选修课
        List<Course> electives = courseService.getElectiveCourses();

        // 5. 筛选可修课程（满足先修条件 + 未选）
        List<Course> availableRequired = missingRequired.stream()
                .filter(c -> !enrolledSet.contains(c.getId()))
                .filter(c -> courseService.checkPrerequisitesMet(c, passedCodes))
                .collect(Collectors.toList());

        List<Course> availableElective = electives.stream()
                .filter(c -> !enrolledSet.contains(c.getId()))
                .filter(c -> courseService.checkPrerequisitesMet(c, passedCodes))
                .collect(Collectors.toList());

        // 6. 按侧重点排序
        if ("考研".equals(focus)) {
            // 考研优先：数学相关、专业核心课
            availableRequired.sort((a, b) -> {
                int aScore = scoreForGradSchool(a);
                int bScore = scoreForGradSchool(b);
                return Integer.compare(bScore, aScore);
            });
        } else if ("项目实践".equals(focus)) {
            // 项目实践优先：Web开发、移动开发、软件工程相关
            availableRequired.sort((a, b) -> {
                int aScore = scoreForPractice(a);
                int bScore = scoreForPractice(b);
                return Integer.compare(bScore, aScore);
            });
            availableElective.sort((a, b) -> {
                int aScore = scoreForPractice(a);
                int bScore = scoreForPractice(b);
                return Integer.compare(bScore, aScore);
            });
        }

        // 7. 学分预算分配：优先必修课，剩余学分给选修课
        List<Course> recommended = new ArrayList<>();
        BigDecimal usedCredit = BigDecimal.ZERO;

        for (Course c : availableRequired) {
            if (usedCredit.add(c.getCredit()).compareTo(maxCredit) <= 0) {
                recommended.add(c);
                usedCredit = usedCredit.add(c.getCredit());
            }
        }

        for (Course c : availableElective) {
            if (usedCredit.add(c.getCredit()).compareTo(maxCredit) <= 0) {
                recommended.add(c);
                usedCredit = usedCredit.add(c.getCredit());
            }
        }

        // 8. 不满足先修的课程（提示）
        List<Course> prereqBlocked = missingRequired.stream()
                .filter(c -> !enrolledSet.contains(c.getId()))
                .filter(c -> !courseService.checkPrerequisitesMet(c, passedCodes))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("student", student);
        result.put("targetSemester", targetSemester);
        result.put("maxCredit", maxCredit);
        result.put("focus", focus);
        result.put("recommendedCourses", recommended);
        result.put("recommendedCount", recommended.size());
        result.put("totalCredit", usedCredit);
        result.put("prereqBlockedCourses", prereqBlocked);
        result.put("availableRequiredCount", availableRequired.size());
        result.put("availableElectiveCount", availableElective.size());
        return result;
    }

    /**
     * 考研倾向评分
     */
    private int scoreForGradSchool(Course c) {
        int score = 0;
        String name = c.getCourseName();
        if (name.contains("数学") || name.contains("代数") || name.contains("概率")) score += 10;
        if (name.contains("数据结构") || name.contains("算法")) score += 8;
        if (name.contains("操作系统") || name.contains("计算机网络") || name.contains("组成原理")) score += 7;
        if ("专业核心".equals(c.getCourseType())) score += 5;
        return score;
    }

    /**
     * 项目实践倾向评分
     */
    private int scoreForPractice(Course c) {
        int score = 0;
        String name = c.getCourseName();
        if (name.contains("Web") || name.contains("开发") || name.contains("移动")) score += 10;
        if (name.contains("软件工程") || name.contains("项目管理")) score += 8;
        if (name.contains("数据库") || name.contains("测试")) score += 6;
        if ("选修".equals(c.getCourseType())) score += 3;
        return score;
    }

    /**
     * 挂科后学分弥补方案
     * 这是 Agent 调用的核心工具：挂科记录 + 选修课库 + 学分规则 + 推理
     */
    public Map<String, Object> creditRecoveryPlan(Long studentId) {
        Student student = studentService.getById(studentId);
        if (student == null) {
            throw new RuntimeException("学生不存在");
        }

        // 1. 查询挂科记录
        List<Map<String, Object>> failed = gradeService.getFailedCourses(studentId);
        BigDecimal failedCredit = BigDecimal.ZERO;
        for (Map<String, Object> f : failed) {
            BigDecimal credit = (BigDecimal) f.get("credit");
            if (credit != null) {
                failedCredit = failedCredit.add(credit);
            }
        }

        // 2. 查询当前已获学分和毕业要求
        GraduationRequirement req = getByMajorAndGrade(student.getMajor(), student.getGrade());
        BigDecimal earned = gradeService.getEarnedCredit(studentId);
        BigDecimal remaining = req.getTotalCredit().subtract(earned);

        // 3. 可用于弥补的选修课
        List<Course> electives = courseService.getElectiveCourses();
        List<Long> enrolledIds = enrollmentService.getCourseIdsByStudent(studentId);
        Set<Long> enrolledSet = new HashSet<>(enrolledIds);
        List<String> passedCodes = gradeService.getPassedCourseCodes(studentId);

        List<Course> availableElectives = electives.stream()
                .filter(c -> !enrolledSet.contains(c.getId()))
                .filter(c -> courseService.checkPrerequisitesMet(c, passedCodes))
                .collect(Collectors.toList());

        // 4. 生成弥补方案：优先选高学分选修课
        availableElectives.sort((a, b) -> b.getCredit().compareTo(a.getCredit()));

        List<Course> recoveryPlan = new ArrayList<>();
        BigDecimal recovered = BigDecimal.ZERO;
        for (Course c : availableElectives) {
            if (recovered.add(c.getCredit()).compareTo(failedCredit) <= 0
                    || recoveryPlan.isEmpty()) {
                recoveryPlan.add(c);
                recovered = recovered.add(c.getCredit());
            }
            if (recovered.compareTo(failedCredit) >= 0) {
                break;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("student", student);
        result.put("failedCourses", failed);
        result.put("failedCount", failed.size());
        result.put("failedCredit", failedCredit);
        result.put("earnedCredit", earned);
        result.put("totalRequired", req.getTotalCredit());
        result.put("remainingCredit", remaining);
        result.put("recoveryCourses", recoveryPlan);
        result.put("recoveryCredit", recovered);
        result.put("creditGap", failedCredit.subtract(recovered));
        result.put("availableElectives", availableElectives);
        return result;
    }
}
