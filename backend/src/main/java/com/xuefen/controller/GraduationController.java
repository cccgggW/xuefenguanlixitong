package com.xuefen.controller;

import com.xuefen.common.Result;
import com.xuefen.entity.Course;
import com.xuefen.entity.GraduationRequirement;
import com.xuefen.service.GraduationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/graduation")
public class GraduationController {

    @Autowired
    private GraduationService graduationService;

    @GetMapping("/requirement")
    public Result<GraduationRequirement> getRequirement(@RequestParam String major,
                                                          @RequestParam String grade) {
        return Result.success(graduationService.getByMajorAndGrade(major, grade));
    }

    @GetMapping("/list")
    public Result<List<GraduationRequirement>> list() {
        return Result.success(graduationService.list());
    }

    /**
     * 学生学分完成情况分析（核心AI工具接口）
     */
    @GetMapping("/analyze/{studentId}")
    public Result<Map<String, Object>> analyze(@PathVariable Long studentId) {
        return Result.success(graduationService.analyzeCreditProgress(studentId));
    }

    /**
     * 获取未修必修课
     */
    @GetMapping("/missing-required/{studentId}")
    public Result<List<Course>> missingRequired(@PathVariable Long studentId) {
        return Result.success(graduationService.getMissingRequiredCourses(studentId));
    }

    /**
     * 推荐选课方案（核心AI工具接口）
     */
    @GetMapping("/recommend/{studentId}")
    public Result<Map<String, Object>> recommend(@PathVariable Long studentId,
                                                   @RequestParam(defaultValue = "第5学期") String targetSemester,
                                                   @RequestParam(defaultValue = "20") BigDecimal maxCredit,
                                                   @RequestParam(defaultValue = "均衡") String focus) {
        return Result.success(graduationService.recommendCourses(studentId, targetSemester, maxCredit, focus));
    }

    /**
     * 挂科学分弥补方案（核心AI工具接口）
     */
    @GetMapping("/recovery/{studentId}")
    public Result<Map<String, Object>> recovery(@PathVariable Long studentId) {
        return Result.success(graduationService.creditRecoveryPlan(studentId));
    }

    @PostMapping
    public Result<GraduationRequirement> add(@RequestBody GraduationRequirement req) {
        graduationService.save(req);
        return Result.success(req);
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody GraduationRequirement req) {
        return Result.success(graduationService.updateById(req));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(graduationService.removeById(id));
    }
}
