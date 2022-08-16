package com.example.organizing_data;

import com.sun.javafx.collections.UnmodifiableListSet;

import java.util.HashSet;
import java.util.Set;

/**
 * @Author: linK
 * @Date: 2022/8/15 14:40
 * @Description TODO  不可变集合  将 getter 返回的值设为只读，并创建用于添加/删除集合元素的方法
 */
public class Encapsulate_Collection {
    /*
    问题
一个类包含一个集合字段和一个用于处理集合的简单 getter 和 setter。

解决方案
将 getter 返回的值设为只读，并创建用于添加/删除集合元素的方法。

为什么要重构
一个类包含一个包含对象集合的字段。该集合可以是数组、列表、集合或向量。已经创建了一个普通的 getter 和 setter 来处理该集合。
但是集合应该由与其他数据类型使用的协议略有不同的协议使用。 getter 方法不应该返回集合对象本身，因为这会让客户端在所有者类不知道的情况下更改集合内容。此外，这会向客户端显示太多对象数据的内部结构。获取集合元素的方法应返回一个不允许更改集合或泄露有关其结构的过多数据的值。
此外，不应该有为集合分配值的方法。相反，应该有添加和删除元素的操作。因此，所有者对象可以控制集合元素的添加和删除。
这样的协议适当地封装了一个集合，最终降低了所有者类和客户端代码之间的关联程度。

好处
集合字段被封装在一个类中。当调用 getter 时，它会返回集合的副本，这可以防止在不知道包含集合的类的情况下意外更改或覆盖集合元素。
如果集合元素包含在原始类型（例如数组）中，则可以创建更方便的方法来处理集合。
如果集合元素包含在非原始容器（标准集合类）中，则通过封装集合，您可以限制对集合的不需要的标准方法的访问（例如通过限制添加新元素）。

如何重构
创建用于添加和删除集合元素的方法。它们必须在其参数中接受集合元素。
如果在类构造函数中没有这样做，则将一个空集合作为初始值分配给该字段。
查找集合字段设置器的调用。更改设置器，使其使用添加和删除元素的操作，或使这些操作调用客户端代码。
请注意，setter 只能用于将所有集合元素替换为其他元素。因此，建议更改设置器名称（重命名方法）以替换。
查找集合 getter 的所有调用，之后集合被更改。更改代码，使其使用新方法从集合中添加和删除元素。
更改 getter 以使其返回集合的只读表示。
检查使用集合的客户端代码，以获取在集合类本身内部看起来更好的代码。

     */
}
class Encapsulate_Collection_Before{
    private Set<String> courses;
    Set getCourse(){
        return new HashSet<>();
    }
    void setCourse(String course){
        courses.add(course);
    }
}

class Encapsulate_Collection_After{
    private UnmodifiableListSet<String> courses;
    Set getCourse(){
        return new HashSet<>();
    }
    void addCourse(String course){
        courses.add(course);
    }

    boolean  removeCourse(String course) {
       return courses.remove(course);
    }
}