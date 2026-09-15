package com.xuefen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuefen.entity.GraduationRequirement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GraduationRequirementMapper extends BaseMapper<GraduationRequirement> {

    /**
     * 根据专业和年级查询毕业要求
     * 兼容年级格式："2023" 与 "2023级" 均可匹配
     */
    @Select("SELECT * FROM graduation_requirement WHERE major = #{major} " +
            "AND REPLACE(grade, '级', '') = REPLACE(#{grade}, '级', '') " +
            "LIMIT 1")
    GraduationRequirement selectByMajorAndGrade(@Param("major") String major, @Param("grade") String grade);
}
