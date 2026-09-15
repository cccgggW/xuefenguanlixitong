package com.xuefen.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuefen.entity.Enrollment;
import com.xuefen.mapper.EnrollmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EnrollmentService extends ServiceImpl<EnrollmentMapper, Enrollment> {

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    /**
     * 查询学生选课记录（含课程信息）
     */
    public List<Map<String, Object>> getWithCourseByStudent(Long studentId) {
        return enrollmentMapper.selectWithCourseByStudent(studentId);
    }

    /**
     * 查询学生已选课程ID列表
     */
    public List<Long> getCourseIdsByStudent(Long studentId) {
        return enrollmentMapper.selectCourseIdsByStudent(studentId);
    }

    /**
     * 查询学生某学期的选课
     */
    public List<Enrollment> getByStudentAndSemester(Long studentId, String semester) {
        return list(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getStudentId, studentId)
                .eq(Enrollment::getSemester, semester));
    }

    /**
     * 检查学生是否已选某课程
     */
    public boolean isEnrolled(Long studentId, Long courseId) {
        return count(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getStudentId, studentId)
                .eq(Enrollment::getCourseId, courseId)
                .ne(Enrollment::getStatus, "已退")) > 0;
    }

    /**
     * 选课
     */
    public Enrollment enroll(Long studentId, Long courseId, String semester) {
        if (isEnrolled(studentId, courseId)) {
            throw new RuntimeException("该课程已选，不可重复选课");
        }
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        enrollment.setSemester(semester);
        enrollment.setStatus("已选");
        save(enrollment);
        return enrollment;
    }

    /**
     * 退课
     */
    public boolean drop(Long enrollmentId) {
        Enrollment enrollment = getById(enrollmentId);
        if (enrollment == null) {
            throw new RuntimeException("选课记录不存在");
        }
        enrollment.setStatus("已退");
        return updateById(enrollment);
    }

    /**
     * 获取学生所有选课学期列表
     */
    public List<String> getSemestersByStudent(Long studentId) {
        List<Enrollment> all = list(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getStudentId, studentId)
                .select(Enrollment::getSemester)
                .groupBy(Enrollment::getSemester)
                .orderByAsc(Enrollment::getSemester));
        return all.stream().map(Enrollment::getSemester).distinct().collect(Collectors.toList());
    }
}
