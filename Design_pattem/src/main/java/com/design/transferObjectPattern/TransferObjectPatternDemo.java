package com.design.transferObjectPattern;

/**
 * @author lin 2022/8/9 1:15
 */
public class TransferObjectPatternDemo {
    public static void main(String[] args) {
        StudentBO studentBusinessObject = new StudentBO();

        //输出所有的学生
        for (StudentVO student : studentBusinessObject.getAllStudents()) {
            System.out.println("Student: [RollNo : "
                    +student.getRollNo()+", Name : "+student.getName()+" ]");
        }

        //更新学生    bo 传输 vo
        StudentVO student =studentBusinessObject.getAllStudents().get(0);
        student.setName("Michael");
        studentBusinessObject.updateStudent(student);

        //获取学生
        studentBusinessObject.getStudent(0);
        System.out.println("Student: [RollNo : "
                +student.getRollNo()+", Name : "+student.getName()+" ]");

        //Student: [RollNo : 0, Name : Robert ]
        //Student: [RollNo : 1, Name : John ]
        //Student: Roll No 0, updated in the database
        //Student: [RollNo : 0, Name : Michael ]
    }
}