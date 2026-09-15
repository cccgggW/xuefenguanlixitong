package com.xuefen.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuefen.entity.Course;
import com.xuefen.entity.Grade;
import com.xuefen.mapper.GradeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GradeService extends ServiceImpl<GradeMapper, Grade> {

    @Autowired
    private GradeMapper gradeMapper;

    @Autowired
    private CourseService courseService;

    /**
     * 查询学生成绩（含课程信息）
     */
    public List<Map<String, Object>> getWithCourseByStudent(Long studentId) {
        return gradeMapper.selectWithCourseByStudent(studentId);
    }

    /**
     * 统计学生已获得总学分
     */
    public BigDecimal getEarnedCredit(Long studentId) {
        return gradeMapper.sumEarnedCredit(studentId);
    }

    /**
     * 按课程类型统计已获得学分
     */
    public List<Map<String, Object>> getCreditByType(Long studentId) {
        return gradeMapper.sumCreditByType(studentId);
    }

    /**
     * 查询挂科记录
     */
    public List<Map<String, Object>> getFailedCourses(Long studentId) {
        return gradeMapper.selectFailedCourses(studentId);
    }

    /**
     * 计算GPA（4.0制）
     */
    public BigDecimal calculateGPA(Long studentId) {
        List<Grade> grades = list(new LambdaQueryWrapper<Grade>()
                .eq(Grade::getStudentId, studentId)
                .eq(Grade::getIsPass, 1));
        if (grades.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalPoints = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        for (Grade g : grades) {
            BigDecimal credit = g.getCreditEarned();
            if (credit == null || credit.compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal points = scoreToGPA(g.getScore());
            totalPoints = totalPoints.add(points.multiply(credit));
            totalCredits = totalCredits.add(credit);
        }
        if (totalCredits.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalPoints.divide(totalCredits, 2, RoundingMode.HALF_UP);
    }

    /**
     * 分数转GPA（4.0制）
     */
    private BigDecimal scoreToGPA(BigDecimal score) {
        if (score == null) return BigDecimal.ZERO;
        double s = score.doubleValue();
        if (s >= 90) return new BigDecimal("4.0");
        if (s >= 85) return new BigDecimal("3.7");
        if (s >= 82) return new BigDecimal("3.3");
        if (s >= 78) return new BigDecimal("3.0");
        if (s >= 75) return new BigDecimal("2.7");
        if (s >= 72) return new BigDecimal("2.3");
        if (s >= 68) return new BigDecimal("2.0");
        if (s >= 64) return new BigDecimal("1.5");
        if (s >= 60) return new BigDecimal("1.0");
        return BigDecimal.ZERO;
    }

    /**
     * 获取学生已通过课程编号列表
     */
    public List<String> getPassedCourseCodes(Long studentId) {
        List<Grade> passed = list(new LambdaQueryWrapper<Grade>()
                .eq(Grade::getStudentId, studentId)
                .eq(Grade::getIsPass, 1));
        Set<String> codes = new HashSet<>();
        for (Grade g : passed) {
            Course c = courseService.getById(g.getCourseId());
            if (c != null) {
                codes.add(c.getCourseCode());
            }
        }
        return new ArrayList<>(codes);
    }

    /**
     * 录入成绩（自动计算等级和是否及格）
     */
    public Grade addGrade(Grade grade) {
        // 重复录入校验：同一学生同一课程只能有一条成绩记录
        Long existing = count(new LambdaQueryWrapper<Grade>()
                .eq(Grade::getStudentId, grade.getStudentId())
                .eq(Grade::getCourseId, grade.getCourseId()));
        if (existing != null && existing > 0) {
            throw new RuntimeException("该课程已录入过成绩，请勿重复录入（可在列表中编辑修改）");
        }
        // 自动计算等级
        grade.setGradeLevel(calculateGradeLevel(grade.getScore()));
        // 自动计算是否及格
        boolean isPass = grade.getScore() != null && grade.getScore().compareTo(new BigDecimal("60")) >= 0;
        grade.setIsPass(isPass ? 1 : 0);
        // 自动计算获得学分
        Course course = courseService.getById(grade.getCourseId());
        if (course != null) {
            grade.setCreditEarned(isPass ? course.getCredit() : BigDecimal.ZERO);
        }
        save(grade);
        return grade;
    }

    /**
     * 计算成绩等级
     */
    private String calculateGradeLevel(BigDecimal score) {
        if (score == null) return "未知";
        double s = score.doubleValue();
        if (s >= 90) return "优秀";
        if (s >= 80) return "良好";
        if (s >= 70) return "中等";
        if (s >= 60) return "及格";
        return "不及格";
    }

    /**
     * 获取学生各学期学分统计
     */
    public Map<String, BigDecimal> getSemesterCreditStats(Long studentId) {
        List<Grade> grades = list(new LambdaQueryWrapper<Grade>()
                .eq(Grade::getStudentId, studentId)
                .eq(Grade::getIsPass, 1)
                .orderByAsc(Grade::getSemester));
        Map<String, BigDecimal> stats = new LinkedHashMap<>();
        for (Grade g : grades) {
            String sem = g.getSemester();
            BigDecimal credit = g.getCreditEarned() != null ? g.getCreditEarned() : BigDecimal.ZERO;
            stats.merge(sem, credit, BigDecimal::add);
        }
        return stats;
    }
}
