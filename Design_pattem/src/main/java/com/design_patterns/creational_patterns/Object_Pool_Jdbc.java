package com.design_patterns.creational_patterns;

import java.sql.Connection;

/**
 * @Author: linK
 * @Date: 2022/8/12 16:43
 * @Description TODO JDBCConnectionPool will allow the application to borrow and return database connections:
 * JDBCConnectionPool 将允许应用程序借用和返回数据库连接
 */
public class Object_Pool_Jdbc {
    public static void main(String args[]) {
        // Do something...

        // Create the ConnectionPool:
        JDBCConnectionPool pool = new JDBCConnectionPool(
                "org.hsqldb.jdbcDriver", "jdbc:hsqldb://localhost/mydb",
                "sa", "secret");

        // Get a connection:
        Connection con = pool.checkOut();

        // Use the connection

        // Return the connection: 归还
        pool.checkIn(con);

    }
}

