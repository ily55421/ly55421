package com.example.dealing_with_generalisation;

/**
 * @Author: linK
 * @Date: 2022/8/15 18:00
 * @Description TODO
 */
public class Pull_Up_Method {
/*
问题
您的子类具有执行类似工作的方法。

解决方案
使方法相同，然后将它们移至相关的超类。

为什么要重构
子类彼此独立地成长和发展，导致相同（或几乎相同）的领域和方法。

好处
摆脱重复的代码。如果您需要对方法进行更改，最好在一个地方进行，而不是在子类中搜索该方法的所有重复项。
如果出于某种原因，子类重新定义了超类方法但执行基本相同的工作，也可以使用这种重构技术。

如何重构
研究超类中的类似方法。如果它们不相同，请将它们格式化为彼此匹配。
如果方法使用不同的参数集，请将参数放在您希望在超类中看到的形式中。
将方法复制到超类。在这里，您可能会发现方法代码使用仅存在于子类中的字段和方法，因此在超类中不可用。要解决此问题，您可以：
对于字段：使用 Pull Up Field 或 Self-Encapsulate Field 在子类中创建 getter 和 setter；然后在超类中抽象地声明这些 getter。
对于方法：使用 Pull Up Method 或在超类中为它们声明抽象方法（请注意，如果之前没有，您的类将变为抽象）。
从子类中删除方法。
todo 检查调用该方法的位置。在某些地方，您可以用超类替换子类的使用。

 */
}

class Pull_Up_Method_Before {
    class Solider extends Pull_Up_Method_Before {
        public int getAge(int age) {
            return age;
        }
    }

    class Tanks extends Pull_Up_Method_Before {
        public int getAge(int age) {
            return age;
        }
    }
}

class Pull_Up_Method_After {
    public int getAge(int age) {
        return age;
    }

    class Solider extends Pull_Up_Method_After {
    }

    class Tanks extends Pull_Up_Method_After {
    }
}