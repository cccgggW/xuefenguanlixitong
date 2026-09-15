package com.xuefen.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String courseCode;
    private String classNo;
    private String courseName;
    private BigDecimal credit;
    private Integer hours;
    private String courseType;
    private String courseCategory;
    private String courseAttr;
    private String department;
    private String semester;
    private String prerequisites;
    private String enrollLimit;
    private Integer capacity;
    private Integer weekDay;
    private Integer startSection;
    private Integer durationSections;
    private String weeks;
    private String classroom;
    private String campus;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
