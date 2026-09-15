package com.xuefen.controller;

import com.xuefen.common.Result;
import com.xuefen.entity.Grade;
import com.xuefen.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/grades")
public class GradeController {

    @Autowired
    private GradeService gradeService;

    @GetMapping("/student/{studentId}")
    public Result<List<Map<String, Object>>> getByStudent(@PathVariable Long studentId) {
        return Result.success(gradeService.getWithCourseByStudent(studentId));
    }

    @GetMapping("/student/{studentId}/earned-credit")
    public Result<BigDecimal> getEarnedCredit(@PathVariable Long studentId) {
        return Result.success(gradeService.getEarnedCredit(studentId));
    }

    @GetMapping("/student/{studentId}/credit-by-type")
    public Result<List<Map<String, Object>>> getCreditByType(@PathVariable Long studentId) {
        return Result.success(gradeService.getCreditByType(studentId));
    }

    @GetMapping("/student/{studentId}/gpa")
    public Result<BigDecimal> getGPA(@PathVariable Long studentId) {
        return Result.success(gradeService.calculateGPA(studentId));
    }

    @GetMapping("/student/{studentId}/failed")
    public Result<List<Map<String, Object>>> getFailed(@PathVariable Long studentId) {
        return Result.success(gradeService.getFailedCourses(studentId));
    }

    @GetMapping("/student/{studentId}/semester-stats")
    public Result<Map<String, BigDecimal>> getSemesterStats(@PathVariable Long studentId) {
        return Result.success(gradeService.getSemesterCreditStats(studentId));
    }

    @PostMapping
    public Result<Grade> add(@RequestBody Grade grade) {
        return Result.success(gradeService.addGrade(grade));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Grade grade) {
        return Result.success(gradeService.updateById(grade));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(gradeService.removeById(id));
    }
}
