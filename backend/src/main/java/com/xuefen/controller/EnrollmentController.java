package com.xuefen.controller;

import com.xuefen.common.Result;
import com.xuefen.entity.Enrollment;
import com.xuefen.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    @GetMapping("/student/{studentId}")
    public Result<List<Map<String, Object>>> getByStudent(@PathVariable Long studentId) {
        return Result.success(enrollmentService.getWithCourseByStudent(studentId));
    }

    @GetMapping("/student/{studentId}/semesters")
    public Result<List<String>> getSemesters(@PathVariable Long studentId) {
        return Result.success(enrollmentService.getSemestersByStudent(studentId));
    }

    @GetMapping("/student/{studentId}/semester/{semester}")
    public Result<List<Enrollment>> getByStudentAndSemester(@PathVariable Long studentId,
                                                               @PathVariable String semester) {
        return Result.success(enrollmentService.getByStudentAndSemester(studentId, semester));
    }

    @PostMapping("/enroll")
    public Result<Enrollment> enroll(@RequestParam Long studentId,
                                      @RequestParam Long courseId,
                                      @RequestParam String semester) {
        return Result.success(enrollmentService.enroll(studentId, courseId, semester));
    }

    @PostMapping("/drop/{id}")
    public Result<Boolean> drop(@PathVariable Long id) {
        return Result.success(enrollmentService.drop(id));
    }

    @PostMapping
    public Result<Enrollment> add(@RequestBody Enrollment enrollment) {
        enrollmentService.save(enrollment);
        return Result.success(enrollment);
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Enrollment enrollment) {
        return Result.success(enrollmentService.updateById(enrollment));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(enrollmentService.removeById(id));
    }
}
