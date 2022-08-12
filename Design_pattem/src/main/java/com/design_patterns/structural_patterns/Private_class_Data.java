package com.design_patterns;

/**
 * @Author: linK
 * @Date: 2022/8/12 14:24
 * @Description TODO
 * Check list
 * Create data class. Move to data class all attributes that need hiding.
 * Create in main class instance of data class.
 * Main class must initialize data class through the data class's constructor.
 * Expose each attribute (variable or property) of data class through a getter.
 * Expose each attribute that will change in further through a setter.
 *
 * 检查清单
 *
 * 创建数据类。移动到数据类所有需要隐藏的属性。
 *
 * 在数据类的主类实例中创建。
 *
 * 主类必须通过数据类的构造函数初始化数据类。
 *
 * 通过 getter 公开数据类的每个属性（变量或属性）。
 *
 * 通过设置器公开将进一步更改的每个属性。
 */
public class Private_class_Data {
    public static void main(String[] args) {
        MainClass mainClass = new MainClass("1", "2", "3");
        mainClass.dataAttr.getDesc();
    }

}
class MainClass{
    DataAttr dataAttr;

    public MainClass(String name, String desc, String title) {
        this.dataAttr = new DataAttr(name, desc, title);
    }
}
class DataAttr{
    private String name;
    private String desc;
    private String title;

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getTitle() {
        return title;
    }

    public DataAttr(String name, String desc, String title) {
        this.name = name;
        this.desc = desc;
        this.title = title;
    }
}