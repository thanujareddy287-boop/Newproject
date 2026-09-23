package com.example.studentapp.service;

import com.example.studentapp.dto.StudentRequest;
import com.example.studentapp.model.Student;
import com.example.studentapp.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {
    private final StudentRepository repository;

    public StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    public List<Student> findAll() {
        return repository.findAll();
    }

    public Student create(StudentRequest request) {
        Student student = new Student(
                request.getName().trim(),
                request.getEmail().trim().toLowerCase(),
                request.getCourse().trim()
        );
        return repository.save(student);
    }

    public Student update(Long id, StudentRequest request) {
        Student student = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        student.setName(request.getName().trim());
        student.setEmail(request.getEmail().trim().toLowerCase());
        student.setCourse(request.getCourse().trim());

        return repository.save(student);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Student not found");
        }
        repository.deleteById(id);
    }
}
