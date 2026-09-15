package com.xuefen.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuefen.entity.Enrollment;
import com.xuefen.entity.Grade;
import com.xuefen.entity.Student;
import com.xuefen.mapper.EnrollmentMapper;
import com.xuefen.mapper.GradeMapper;
import com.xuefen.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StudentService extends ServiceImpl<StudentMapper, Student> {

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Autowired
    private GradeMapper gradeMapper;

    /**
     * 分页查询学生
     */
    public Page<Student> page(int pageNum, int pageSize, String keyword, String major, String grade) {
        Page<Student> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Student::getName, keyword)
                    .or().like(Student::getStudentNo, keyword));
        }
        if (StringUtils.hasText(major)) {
            wrapper.eq(Student::getMajor, major);
        }
        if (StringUtils.hasText(grade)) {
            wrapper.eq(Student::getGrade, grade);
        }
        wrapper.orderByAsc(Student::getStudentNo);
        return page(page, wrapper);
    }

    /**
     * 根据学号查询
     */
    public Student getByStudentNo(String studentNo) {
        return getOne(new LambdaQueryWrapper<Student>().eq(Student::getStudentNo, studentNo));
    }

    @Transactional
    public boolean removeStudent(Long id) {
        enrollmentMapper.delete(new LambdaQueryWrapper<Enrollment>().eq(Enrollment::getStudentId, id));
        gradeMapper.delete(new LambdaQueryWrapper<Grade>().eq(Grade::getStudentId, id));
        return removeById(id);
    }
}
