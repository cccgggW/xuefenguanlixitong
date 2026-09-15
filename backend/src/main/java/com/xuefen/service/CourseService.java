package com.xuefen.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuefen.entity.Course;
import com.xuefen.entity.Enrollment;
import com.xuefen.entity.Grade;
import com.xuefen.mapper.CourseMapper;
import com.xuefen.mapper.EnrollmentMapper;
import com.xuefen.mapper.GradeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService extends ServiceImpl<CourseMapper, Course> {

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Autowired
    private GradeMapper gradeMapper;

    /**
     * 分页查询课程
     */
    public Page<Course> page(int pageNum, int pageSize, String keyword, String courseType) {
        Page<Course> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Course::getCourseName, keyword)
                    .or().like(Course::getCourseCode, keyword));
        }
        if (StringUtils.hasText(courseType)) {
            wrapper.eq(Course::getCourseType, courseType);
        }
        wrapper.orderByAsc(Course::getCourseCode);
        return page(page, wrapper);
    }

    /**
     * 根据课程编号查询
     */
    public Course getByCode(String courseCode) {
        return getOne(new LambdaQueryWrapper<Course>().eq(Course::getCourseCode, courseCode));
    }

    /**
     * 根据类型列表查询课程
     */
    public List<Course> getByTypes(List<String> types) {
        return baseMapper.selectByTypes(types);
    }

    /**
     * 查询必修课
     */
    public List<Course> getRequiredCourses() {
        return list(new LambdaQueryWrapper<Course>()
                .in(Course::getCourseType, Arrays.asList("必修", "公共基础", "专业基础", "专业核心"))
                .orderByAsc(Course::getSemester));
    }

    /**
     * 查询选修课
     */
    public List<Course> getElectiveCourses() {
        return list(new LambdaQueryWrapper<Course>()
                .eq(Course::getCourseType, "选修")
                .orderByAsc(Course::getCourseCode));
    }

    /**
     * 解析先修课程编号列表
     */
    public List<String> parsePrerequisites(String prerequisites) {
        if (!StringUtils.hasText(prerequisites)) {
            return java.util.Collections.emptyList();
        }
        return Arrays.stream(prerequisites.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    /**
     * 检查先修课程是否已满足
     * @param course 目标课程
     * @param passedCourseCodes 已通过课程编号列表
     */
    public boolean checkPrerequisitesMet(Course course, List<String> passedCourseCodes) {
        List<String> prereqs = parsePrerequisites(course.getPrerequisites());
        if (prereqs.isEmpty()) {
            return true;
        }
        return passedCourseCodes.containsAll(prereqs);
    }

    @Transactional
    public boolean removeCourse(Long id) {
        enrollmentMapper.delete(new LambdaQueryWrapper<Enrollment>().eq(Enrollment::getCourseId, id));
        gradeMapper.delete(new LambdaQueryWrapper<Grade>().eq(Grade::getCourseId, id));
        return removeById(id);
    }
}
