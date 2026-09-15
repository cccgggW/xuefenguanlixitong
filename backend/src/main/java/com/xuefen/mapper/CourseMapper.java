package com.xuefen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuefen.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    /**
     * 根据课程类型列表查询课程
     */
    @Select("<script>" +
            "SELECT * FROM course WHERE course_type IN " +
            "<foreach item='type' collection='types' open='(' separator=',' close=')'>" +
            "#{type}" +
            "</foreach>" +
            "</script>")
    List<Course> selectByTypes(@Param("types") List<String> types);

    /**
     * 查询某学期建议开设的课程
     */
    @Select("SELECT * FROM course WHERE semester = #{semester}")
    List<Course> selectBySemester(@Param("semester") String semester);
}
