package com.xuefen.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuefen.common.Result;
import com.xuefen.entity.Course;
import com.xuefen.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @GetMapping("/page")
    public Result<Page<Course>> page(@RequestParam(defaultValue = "1") int pageNum,
                                      @RequestParam(defaultValue = "10") int pageSize,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String courseType) {
        return Result.success(courseService.page(pageNum, pageSize, keyword, courseType));
    }

    @GetMapping("/list")
    public Result<List<Course>> list() {
        return Result.success(courseService.list());
    }

    @GetMapping("/{id}")
    public Result<Course> getById(@PathVariable Long id) {
        return Result.success(courseService.getById(id));
    }

    @GetMapping("/code/{code}")
    public Result<Course> getByCode(@PathVariable String code) {
        return Result.success(courseService.getByCode(code));
    }

    @GetMapping("/required")
    public Result<List<Course>> required() {
        return Result.success(courseService.getRequiredCourses());
    }

    @GetMapping("/elective")
    public Result<List<Course>> elective() {
        return Result.success(courseService.getElectiveCourses());
    }

    @PostMapping
    public Result<Course> add(@RequestBody Course course) {
        courseService.save(course);
        return Result.success(course);
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Course course) {
        return Result.success(courseService.updateById(course));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(courseService.removeCourse(id));
    }
}
