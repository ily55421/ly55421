package com.design.dataAccessObject;

import java.util.List;

/**
 * @author lin 2022/8/9 0:50
 */
public interface StudentDao {
    public List<Student> getAllStudents();
    public Student getStudent(int rollNo);
    public void updateStudent(Student student);
    public void deleteStudent(Student student);
}
