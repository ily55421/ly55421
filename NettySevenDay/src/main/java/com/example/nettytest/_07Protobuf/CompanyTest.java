package com.example.nettytest._07Protobuf;

import com.example.nettytest._07Protobuf.pojo.Company;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * @author lin 2022/8/13 0:09
 * 测试 protobuf 复杂对象序列化
 */
public class CompanyTest {

    public static void main(String[] args) throws InvalidProtocolBufferException {
        Company.Employee.Builder builder = Company.Employee.newBuilder();
        Company.Employee employee = builder.setAge(1).
                setName("员工")
                .setPhone(12122)
                .addWorks(Company.Work.newBuilder().setId(1).setName("222").setHandle("处理1"))
                .addWorks(Company.Work.newBuilder().setId(2).setName("222").setHandle("处理2"))
                .addWorks(Company.Work.newBuilder().setId(3).setName("222").setHandle("处理3")).build();

        byte[] buf = employee.toByteArray();
        System.out.println("序列化完毕，size=" + buf.length);

        Company.Employee employee1 = Company.Employee.parseFrom(buf);
        System.out.println(employee1.getName()+employee1.getWorks(1).getHandle());
        //序列化完毕，size=67
        //员工处理2
    }

}
