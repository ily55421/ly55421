package com.design_patterns.creational_patterns;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * @Author: linK
 * @Date: 2022/8/12 16:27
 * @Description TODO
 */
public class Object_Pool_Simple {
}
// ObjectPool Class

abstract class ObjectPool<T> {
    private long expirationTime;
    /**
     * 线程安全的map
     */
    private Hashtable<T, Long> locked, unlocked;

    public ObjectPool() {
        expirationTime = 30000;
        // 30 seconds
        locked = new Hashtable<T, Long>();
        unlocked = new Hashtable<T, Long>();
    }

    protected abstract T create();

    public abstract boolean validate(T o);

    public abstract void expire(T o);

    /**
     * 检查对象
     *
     * @return
     */
    public synchronized T checkOut() {
        long now = System.currentTimeMillis();
        T t;
        //存在为上锁的对象
        if (unlocked.size() > 0) {
            //返回此哈希表中键的枚举。
            Enumeration<T> e = unlocked.keys();
            //判空
            while (e.hasMoreElements()) {
                //下一个元素
                t = e.nextElement();
                //(now - unlocked.get(t)) > expirationTime 判断对象 是否连接超时
                if ((now - unlocked.get(t)) > expirationTime) {
                    // object has expired 对象已超时 从map中删除
                    unlocked.remove(t);
                    // 关闭连接
                    expire(t);
                    t = null;
                } else {
                    // 校验对象是否已关闭   未关闭则放进locked中
                    if (validate(t)) {
                        // 移除对象
                        unlocked.remove(t);
                        // 添加到上锁map中
                        locked.put(t, now);
                        return (t);
                    } else {
                        // object failed validation  对象验证失败 则移除
                        unlocked.remove(t);
                        // 关闭连接
                        expire(t);
                        t = null;
                    }
                }
            }
        }
        // no objects available, create a new one    没有可用对象时，创建一个 新的
        t = create();
        locked.put(t, now);
        return (t);
    }

    public synchronized void checkIn(T t) {
        locked.remove(t);
        unlocked.put(t, System.currentTimeMillis());
    }
}

//The three remaining methods are abstract
//and therefore must be implemented by the subclass

class JDBCConnectionPool extends ObjectPool<Connection> {

    private String dsn, usr, pwd;

    public JDBCConnectionPool(String driver, String dsn, String usr, String pwd) {
        super();
        try {
            Class.forName(driver).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.dsn = dsn;
        this.usr = usr;
        this.pwd = pwd;
    }

    @Override
    protected Connection create() {
        try {
            return (DriverManager.getConnection(dsn, usr, pwd));
        } catch (SQLException e) {
            e.printStackTrace();
            return (null);
        }
    }

    @Override
    public void expire(Connection o) {
        try {
            ((Connection) o).close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean validate(Connection o) {
        try {
            return (!((Connection) o).isClosed());
        } catch (SQLException e) {
            e.printStackTrace();
            return (false);
        }
    }
}