package com.design.mvc;

/**
 * @author lin 2022/8/8 22:26
 */
public class MVCPatternDemo {
    public static void main(String[] args) {

        //从数据可获取学生记录
        Student model = retriveStudentFromDatabase();

        //创建一个视图：把学生详细信息输出到控制台
        StudentView view = new StudentView();

        StudentController controller = new StudentController(model, view);

        controller.updateView();

        //更新模型数据
        controller.setStudentName("John");

        controller.updateView();
        //Student:
        //Name: Robert
        //Roll No: 10
        //Student:
        //Name: John
        //Roll No: 10
    }

    private static Student retriveStudentFromDatabase() {
        Student student = new Student();
        student.setName("Robert");
        student.setRollNo("10");
        return student;
    }
}