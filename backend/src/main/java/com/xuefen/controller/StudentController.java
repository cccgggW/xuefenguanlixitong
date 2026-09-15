package com.xuefen.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuefen.common.Result;
import com.xuefen.entity.Student;
import com.xuefen.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @GetMapping("/page")
    public Result<Page<Student>> page(@RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String major,
                                       @RequestParam(required = false) String grade) {
        return Result.success(studentService.page(pageNum, pageSize, keyword, major, grade));
    }

    @GetMapping("/list")
    public Result<List<Student>> list() {
        return Result.success(studentService.list());
    }

    @GetMapping("/{id}")
    public Result<Student> getById(@PathVariable Long id) {
        return Result.success(studentService.getById(id));
    }

    @GetMapping("/no/{studentNo}")
    public Result<Student> getByStudentNo(@PathVariable String studentNo) {
        return Result.success(studentService.getByStudentNo(studentNo));
    }

    @PostMapping
    public Result<Student> add(@RequestBody Student student) {
        studentService.save(student);
        return Result.success(student);
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Student student) {
        return Result.success(studentService.updateById(student));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(studentService.removeStudent(id));
    }
}
