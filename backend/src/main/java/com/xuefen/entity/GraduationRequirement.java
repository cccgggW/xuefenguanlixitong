package com.xuefen.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("graduation_requirement")
public class GraduationRequirement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String major;
    private String grade;
    private BigDecimal totalCredit;
    private BigDecimal requiredCredit;
    private BigDecimal electiveCredit;
    private BigDecimal publicBasicCredit;
    private BigDecimal majorBasicCredit;
    private BigDecimal majorCoreCredit;
    private BigDecimal practiceCredit;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
