package com.example.moving_features_betweeno_objects;

import java.util.Date;

/**
 * @Author: linK
 * @Date: 2022/8/15 14:05
 * @Description TODO 引用外部方法
 */
public class Introduce_Foreign_Method {
    /*
    问题
实用程序类不包含您需要的方法，并且您不能将该方法添加到类中。

解决方案
将方法添加到客户端类并将实用程序类的对象作为参数传递给它。

为什么要重构
您有使用某个类的数据和方法的代码。您意识到代码在类中的新方法中看起来和工作得更好。但是您不能将该方法添加到类中，因为例如，该类位于第三方库中。
当您想要移动到该方法的代码在程序的不同位置重复多次时，这种重构具有很大的回报。
由于您将实用程序类的对象传递给新方法的参数，因此您可以访问其所有字段。在方法内部，您几乎可以做任何您想做的事情，就好像该方法是实用程序类的一部分一样。

好处
删除代码重复。如果您的代码在多个地方重复，您可以将这些代码片段替换为方法调用。即使考虑到外部方法位于次优位置，这也比重复要好。

缺点
在您之后维护代码的人并不总是清楚在客户端类中使用实用程序类的方法的原因。如果该方法可以在其他类中使用，您可以通过为实用程序类创建一个包装器并将该方法放置在那里而受益。当有几种这样的实用方法时，这也是有益的。引入本地扩展可以帮助解决这个问题。

如何重构
在客户端类中创建一个新方法。
在此方法中，创建一个参数，实用程序类的对象将传递给该参数。如果可以从客户端类中获取此对象，则不必创建此类参数。
将相关代码片段提取到该方法中，并用方法调用替换它们。
请务必在方法的注释中留下 Foreign 方法标记，以及将此方法放置在实用程序类中的建议（如果以后可能的话）。这将使将来维护该软件的人更容易理解为什么此方法位于此特定类中。

     */
}
class Introduce_Foreign_Method_Before{
    private Date previousEnd;
    // ...
    void sendReport() {
        Date nextDay = new Date(previousEnd.getYear(),
                previousEnd.getMonth(), previousEnd.getDate() + 1);
        // ...
    }
}
class Introduce_Foreign_Method_After{
    private Date previousEnd;

    // ...
    void sendReport() {
        Date newStart = nextDay(previousEnd);
        // ...
    }
    private static Date nextDay(Date arg) {
        return new Date(arg.getYear(), arg.getMonth(), arg.getDate() + 1);
    }
}
