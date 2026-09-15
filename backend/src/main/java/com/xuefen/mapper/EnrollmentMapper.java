package com.xuefen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuefen.entity.Enrollment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EnrollmentMapper extends BaseMapper<Enrollment> {

    /**
     * 查询学生所有选课记录（含课程信息）
     */
    @Select("SELECT e.*, c.course_code, c.course_name, c.credit, c.course_type, c.semester as suggest_semester " +
            "FROM enrollment e LEFT JOIN course c ON e.course_id = c.id " +
            "WHERE e.student_id = #{studentId} " +
            "ORDER BY e.semester")
    List<java.util.Map<String, Object>> selectWithCourseByStudent(@Param("studentId") Long studentId);

    /**
     * 查询学生已选课程ID列表
     */
    @Select("SELECT course_id FROM enrollment WHERE student_id = #{studentId}")
    List<Long> selectCourseIdsByStudent(@Param("studentId") Long studentId);
}
