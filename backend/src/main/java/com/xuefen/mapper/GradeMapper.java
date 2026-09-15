package com.xuefen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuefen.entity.Grade;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface GradeMapper extends BaseMapper<Grade> {

    /**
     * 查询学生成绩（含课程信息）
     */
    @Select("SELECT g.*, c.course_code, c.course_name, c.credit, c.course_type " +
            "FROM grade g LEFT JOIN course c ON g.course_id = c.id " +
            "WHERE g.student_id = #{studentId} " +
            "ORDER BY g.semester")
    List<Map<String, Object>> selectWithCourseByStudent(@Param("studentId") Long studentId);

    /**
     * 统计学生已获得的总学分（仅及格课程）
     */
    @Select("SELECT COALESCE(SUM(credit_earned), 0) FROM grade " +
            "WHERE student_id = #{studentId} AND is_pass = 1")
    BigDecimal sumEarnedCredit(@Param("studentId") Long studentId);

    /**
     * 按课程类型统计已获得学分
     */
    @Select("SELECT c.course_type as courseType, COALESCE(SUM(g.credit_earned), 0) as totalCredit " +
            "FROM grade g LEFT JOIN course c ON g.course_id = c.id " +
            "WHERE g.student_id = #{studentId} AND g.is_pass = 1 " +
            "GROUP BY c.course_type")
    List<Map<String, Object>> sumCreditByType(@Param("studentId") Long studentId);

    /**
     * 查询学生挂科记录
     */
    @Select("SELECT g.*, c.course_code, c.course_name, c.credit, c.course_type " +
            "FROM grade g LEFT JOIN course c ON g.course_id = c.id " +
            "WHERE g.student_id = #{studentId} AND g.is_pass = 0")
    List<Map<String, Object>> selectFailedCourses(@Param("studentId") Long studentId);
}
