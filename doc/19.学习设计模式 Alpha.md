# 学习设计模式 Alpha



**本笔记是  [《大话设计模式》](https://book.douban.com/subject/2334288/) 读书笔记。目前只是把笔记整理出来，相关代码还没有完善，目前只是发布一个 Alpha 版，后续会增加相关代码，同时还会结合其他材料，进一步完善。敬请持续关注！谢谢！**

## 1. 面向对象程序设计

GoF的《设计模式》好比是世界顶级足球射门集锦，《重构》、《敏捷软件开发》、《设计模式解析》好比是一场场最精彩的足球比赛。

他们尽管有一定天分，但却也是从最底层通过努力一点一点慢慢显露出来的，我们需要的不仅仅是世界杯上的那定乾坤的一脚，更需要这一脚之前是如何练出那种神奇的方法，对于程序员来讲，精彩的代码是如何想出来的，要比看到精彩的代码更加令人期待。

《重构与模式》中有一句经典之语：“如果想成为一名更优秀的软件设计师，了解优秀软件设计的演变过程比学习优秀设计本身更有价值，因为设计的演变过程中蕴藏着大智慧。”

通过这些模式让你找到“封装变化”、“对象间松散耦合”、“针对接口编程”的感觉，从而设计出易维护、易扩展、易复用、灵活性好的程序。

如果说，数学是思维的体操，那设计模式，就是面向对象编程思维的体操。

“话说三国时期，曹操带领百万大军攻打东吴，大军在长江赤壁驻扎，军船连成一片，眼看就要灭掉东吴，统一天下，曹操大悦，于是大宴众文武，在酒席间，曹操诗性大发，不觉吟道：‘喝酒唱歌，人生真爽。……’。众文武齐呼：‘丞相好诗！’于是一臣子速命印刷工匠刻版印刷，以便流传天下。” “样张出来给曹操一看，曹操感觉不妥，说道：‘喝与唱，此话过俗，应改为‘对酒当歌’较好！’，于是此臣就命工匠重新来过。工匠眼看连夜刻版之工，彻底白费，心中叫苦不迭。只得照办。” “样张再次出来请曹操过目，曹操细细一品，觉得还是不好，说：‘人生真爽太过直接，应改问语才够意境，因此应改为‘对酒当歌，人生几何？……’当臣转告工匠之时，工匠晕倒……！”

第一，要改，只需更改要改之字，此为可维护；第二，这些字并非用完这次就无用，完全可以在后来的印刷中重复使用，此乃可复用；第三，此诗若要加字，只需另刻字加入即可，这是可扩展；第四，字的排列其实可能是竖排可能是横排，此时只需将活字移动就可做到满足排列需求，此是灵活性好。

通过封装、继承、多态把程序的耦合度降低

用设计模式使得程序更加的灵活，容易修改，并且易于复用。

中国古代的四大发明，另三种应该都是科技的进步，伟大的创造或发现。而唯有活字印刷，实在是思想的成功，面向对象的胜利。

聚合表示一种弱的‘拥有’关系，体现的是A对象可以包含B对象，但B对象不是A对象的一部分[DPE]（DPE表示此句摘自《设计模式》（第2版），详细摘要说明见附录二）。聚合关系用空心的菱形+实线箭头来表示。”

合成（Composition，也有翻译成‘组合’的）是一种强的‘拥有’关系，体现了严格的部分和整体的关系，部分和整体的生命周期一样[DPE]。在这里鸟和其翅膀就是合成（组合）关系，因为它们是部分和整体的关系，并且翅膀和鸟的生命周期是相同的。合成关系用实心的菱形+实线箭头来表示。

编程是一门技术，更加是一门艺术，不能只满足于写完代码运行结果正确就完事，时常考虑如何让代码更加简练，更加容易维护，容易扩展和复用，只有这样才可以真正得到提高。

### 1.1. 面向对象入门

|      | 本节内容是针对 C# 语言的。和 Java 语言语法略有出入。阅读时请注意。 |
| ---- | ------------------------------------------------------------ |

对象是一个自包含的实体，用一组可识别的特性和行为来标识。面向对象编程，英文叫Object-Oriented Programming，其实就是针对对象来进行编程的意思。

类就是具有相同的属性和功能的对象的抽象的集合

第一，类名称首字母记着要大写。多个单词则各个首字母大写；

第二，对外公开的方法需要用‘public’修饰符。

实例，就是一个真实的对象。

实例化就是创建对象的过程，使用new关键字来创建。

构造方法，又叫构造函数，其实就是对类进行初始化。构造方法与类同名，无返回值，也不需要void，在new时候调用。

所有类都有构造方法，如果你不编码则系统默认生成空的构造方法，若你有定义的构造方法，那么默认的构造方法就会失效了。

方法重载提供了创建同名的多个方法的能力，但这些方法需使用不同的参数类型。

方法重载时，两个方法必须要方法名相同，但参数类型或个数必须要有所不同，否则重载就没有意义了。

方法重载可在不改变原方法的基础上，新增功能。

属性是一个方法或一对方法，但在调用它的代码看来，它是一个字段，即属性适合于以字段的方式使用方法调用的场合。这里还需要解释一下字段的意思，字段是存储类要满足其设计所需要的数据，字段是与类相关的变量。

public表示它所修饰的类成员可以允许其他任何类来访问，俗称公有的。而private表示只允许同一个类中的成员访问，其他类包括它的子类无法访问，俗称私有的。

通常字段都是private，即私有的变量，而属性都是public，即公有的变量。

属性有两个方法get和set。get访问器返回与声明的属性相同的数据类型，表示的意思是调用时可以得到内部字段的值或引用；set访问器没有显式设置参数，但它有一个隐式参数，用关键字value表示，它的作用是调用属性时可以给内部的字段或引用赋值。

每个对象都包含它能进行操作所需要的所有信息，这个特性称为封装，因此对象不必依赖其他对象来完成自己的操作。

封装有很多好处，

1. 第一、良好的封装能够减少耦合
2. 第二、类内部的实现可以自由地修改
3. 第三、类具有清晰的对外接口

对象的继承代表了一种‘is-a’的关系，如果两个对象A和B，可以描述为‘B是A’，则表明B可以继承A。

继承者还可以理解为是对被继承者的特殊化，因为它除了具备被继承者的特性外，还具备自己独有的个性。

继承定义了类如何相互关联，共享特性。继承的工作方式是，定义父类和子类，或叫做基类和派生类，其中子类继承父类的所有特性。子类不但继承了父类的所有特性，还可以定义新的特性。

如果子类继承于父类，第一、子类拥有父类非private的属性和功能；第二、子类具有自己的属性和功能，即子类可以扩展父类没有的属性和功能；第三、子类还可以以自己的方式实现父类的功能（方法重写）。

protected表示继承时子类可以对基类有完全访问权

子类从它的父类中继承的成员有方法、域、属性、事件、索引指示器，但对于构造方法，有一些特殊，它不能被继承，只能被调用。对于调用父类的成员，可以用base关键字。”

不用继承的话，如果要修改功能，就必须在所有重复的方法中修改，代码越多，出错的可能就越大，而继承的优点是，继承使得所有子类公共的部分都放在了父类，使得代码得到了共享，这就避免了重复，另外，继承可使得修改或扩展继承而来的实现都较为容易。

继承是有缺点的，那就是父类变，则子类不得不变。

继承会破坏包装，父类实现细节暴露给子类，这其实是增大了两个类之间的耦合性。

继承显然是一种类与类之间强耦合的关系。

当两个类之间具备‘is-a’的关系时，就可以考虑用继承

面向对象的第三大特性——多态。

多态表示不同的对象可以执行相同的动作，但要通过它们自己的实现代码来执行。

第一，子类以父类的身份出现，儿子代表老子表演，化妆后就是以父亲身份出现了。第二、子类在工作时以自己的方式来实现，儿子模仿得再好，那也是模仿，儿子只能用自己理解的表现方式去模仿父亲的作品；第三、子类以父类的身份出现时，子类特有的属性和方法不可以使用，儿子经过多年学习，其实已经有了自己的创作，自己的绝活，但在此时，代表父亲表演时，绝活是不能表现出来的。当然，如果父亲还有别的儿子会表演，也可以在此时代表父亲上场，道理也是一样的。这就是多态。

为了使子类的实例完全接替来自父类的类成员，父类必须将该成员声明为虚拟的。这是通过在该成员的返回类型之前添加virtual关键字来实现。

子类可以选择使用override关键字，将父类实现替换为它自己的实现，这就是方法重写Override，或者叫做方法覆写。

不同的对象可以执行相同的动作，但要通过它们自己的实现代码来执行。

对象的声明必须是父类，而不是子类，实例化的对象是子类，这才能实现多态。多态的原理是当方法被调用时，无论对象是否被转换为其父类，都只有位于对象继承链最末端的方法实现会被调用。也就是说，虚方法是按照其运行时类型而非编译时类型进行动态绑定调用的。[AMNFP]

没有学过设计模式，那么对多态、乃至对面向对象的理解多半都是肤浅和片面的。

C#允许把类和方法声明为abstract，即抽象类和抽象方法。

第一，抽象类不能实例化

第二，抽象方法是必须被子类重写的方法

第三，如果类中包含抽象方法，那么类就必须定义为抽象类，不论是否还包含其他一般方法。

考虑让抽象类拥有尽可能多的共同代码，拥有尽可能少的数据[J&DP]。

抽象类通常代表一个抽象概念，它提供一个继承的出发点，当设计一个新的抽象类时，一定是用来继承的，所以，在一个以继承关系形成的等级结构里面，树叶节点应当是具体类，而树枝节点均应当是抽象类[J&DP]。

接口是把隐式公共方法和属性组合起来，以封装特定功能的一个集合。一旦类实现了接口，类就可以支持接口所指定的所有属性和成员。声明接口在语法上与声明抽象类完全相同，但不允许提供接口中任何成员的执行方式。

实现接口的类就必须要实现接口中的所有方法和属性。

一个类可以支持多个接口，多个类也可以支持相同的接口。

记住，接口的命名，前面要加一个大写字母‘I’，这是规范。

接口用interface声明，而不是class，接口名称前要加‘I’，接口中的方法或属性前面不能有修饰符、方法没有方法体。

抽象类可以给出一些成员的实现，接口却不包含成员的实现，抽象类的抽象成员可被子类部分实现，接口的成员需要实现类完全实现，一个类只能继承一个抽象类，但可实现多个接口等等。

第一，类是对对象的抽象；抽象类是对类的抽象；接口是对行为的抽象。接口是对类的局部（行为）进行的抽象，而抽象类是对类整体（字段、属性、方法）的抽象。如果只关注行为抽象，那么也可以认为接口就是抽象类。总之，不论是接口、抽象类、类甚至对象，都是在不同层次、不同角度进行抽象的结果，它们的共性就是抽象。第二，如果行为跨越不同类的对象，可使用接口；对于一些相似的类对象，用继承抽象类。

实现接口和继承抽象类并不冲突

第三，从设计角度讲，抽象类是从子类中发现了公共的东西，泛化出父类，然后子类继承父类，而接口是根本不知子类的存在，方法如何实现还不确认，预先定义。

通过重构改善既有代码的设计。

抽象类往往都是通过重构得来的，当然，如果你事先意识到多种分类的可能，那么事先就设计出抽象类也是完全可以的。而接口就完全不是一回事

抽象类是自底而上抽象出来的，而接口则是自顶向下设计出来的。

要想真正把抽象类和接口用好，还是需要好好用心地去学习设计模式。只有真正把设计模式理解好了，那么你才能算是真正会合理应用抽象类和接口了。

数组优点，比如说数组在内存中连续存储，因此可以快速而容易地从头到尾遍历元素，可以快速修改元素等等。缺点嘛，应该是创建时必须要指定数组变量的大小，还有在两个元素之间添加元素也比较困难。

NET Framework提供了用于数据存储和检索的专用类，这些类统称集合。这些类提供对堆栈、队列、列表和哈希表的支持。大多数集合类实现相同的接口。

ArrayList是命名空间System.Collections下的一部分，它是使用大小可按需动态增加的数组实现IList接口[MSDN]。

ArrayList的容量是ArrayList可以保存的元素数。ArrayList的默认初始容量为0。随着元素添加到ArrayList中，容量会根据需要通过重新分配自动增加。使用整数索引可以访问此集合中的元素。此集合中的索引从零开始。[MSDN]

ArrayList不是类型安全的。

装箱就是把值类型打包到Object引用类型的一个实例中。

拆箱就是指从对象中提取值类型。

相对于简单的赋值而言，装箱和拆箱过程需要进行大量的计算。对值类型进行装箱时，必须分配并构造一个全新的对象。其次，拆箱所需的强制转换也需要进行大量的计算[MSDN]。

泛型是具有占位符（类型参数）的类、结构、接口和方法，这些占位符是类、结构、接口和方法所存储或使用的一个或多个类型的占位符。泛型集合类可以将类型参数用作它所存储的对象的类型的占位符；类型参数作为其字段的类型和其方法的参数类型出现[MSDN]。

巨人也有会走弯路的时候，何况我们常人。

通常情况下，都建议使用泛型集合，因为这样可以获得类型安全的直接优点而不需要从基集合类型派生并实现类型特定的成员。此外，如果集合元素为值类型，泛型集合类型的性能通常优于对应的非泛型集合类型（并优于从非泛型基集合类型派生的类型），因为使用泛型时不必对元素进行装箱[MSDN]。

委托是对函数的封装，可以当作给方法的特征指定一个名称。而事件则是委托的一种特殊形式，当发生有意义的事情时，事件对象处理通知过程[PC#]。

事件其实就是设计模式中观察者模式在.NET中的一种实现方式。

委托是一种引用方法的类型。一旦为委托分配了方法，委托将与该方法具有完全相同的行为[MSDN]。委托对象用关键字delegate来声明。而事件是说在发生其他类或对象关注的事情时，类或对象可通过事件通知它们[MSDN]。事件对象用event关键字声明。

EventArgs 是包含事件数据的类的基类[MSDN]。换句话说，这个类的作用就是用来在事件触发时传递数据用的。

学无止境，你需要不断地练习实践才可能真正成为优秀的软件工程师。

## 2. 设计模式设计原则

### 2.1. 合成/聚合复用原则

刚开始学会用面向对象的继承时，感觉它既新颖又功能强大，所以只要可以用，就都用上继承。这就好比是‘有了新锤子，所有的东西看上去都成了钉子。[DPE]

事实上，很多情况用继承会带来麻烦。

对象的继承关系是在编译时就定义好了，所以无法在运行时改变从父类继承的实现。子类的实现与它的父类有非常紧密的依赖关系，以至于父类实现中的任何变化必然会导致子类发生变化。当你需要复用子类时，如果继承下来的实现不适合解决新的问题，则父类必须重写或被其他更适合的类替换。这种依赖关系限制了灵活性并最终限制了复用性[DP]。

在面向对象设计中，我们还有一个很重要的设计原则，那就是合成/聚合复用原则。即优先使用对象合成/聚合，而不是类继承[DP]。

合成（Composition，也有翻译成组合）和聚合（Aggregation）都是关联的特殊种类。聚合表示一种弱的‘拥有’关系，体现的是A对象可以包含B对象，但B对象不是A对象的一部分；合成则是一种强的‘拥有’关系，体现了严格的部分和整体的关系，部分和整体的生命周期一样[DPE]。

合成/聚合复用原则的好处是，优先使用对象的合成/聚合将有助于你保持每个类被封装，并被集中在单个任务上。这样类和类继承层次会保持较小规模，并且不太可能增长为不可控制的庞然大物[DP]。

开放-封闭原则。这样的设计显然不会修改原来的代码，而只是扩展类就行了。但今天我的感受最深的是合成/聚合复用原则，也就是优先使用对象的合成或聚合，而不是类继承。聚合的魅力无限呀。相比，继承的确很容易造成不必要的麻烦。

盲目使用继承当然就会造成麻烦，而其本质原因主要是什么？” “我想应该是，继承是一种强耦合的结构。父类变，子类就必须要变。

在用继承时，一定要在是‘is-a’的关系时再考虑使用，而不是任何时候都去使用。

### 2.2. 依赖倒置原则

CPU的对外都是针脚式或触点式等标准的接口。啊，我明白了，这就是接口的最大好处。CPU只需要把接口定义好，内部再复杂我也不让外界知道，而主板只需要预留与CPU针脚的插槽就可以了。

比如我们之前讲过的单一职责原则，就刚才修电脑的事，显然内存坏了，不应该成为更换CPU的理由，它们各自的职责是明确的。再比如开放-封闭原则，内存不够只要插槽足够就可以添加，硬盘不够可以用移动硬盘等，PC的接口是有限的，所以扩展有限，软件系统设计得好，却可以无限地扩展。

依赖倒转原则，也有翻译成依赖倒置原则的。

依赖倒转原则，原话解释是抽象不应该依赖细节，细节应该依赖于抽象，这话绕口，说白了，就是要针对接口编程，不要对实现编程，无论主板、CPU、内存、硬盘都是在针对接口设计的，如果针对实现来设计，内存就要对应到具体的某个品牌的主板，那就会出现换内存需要把主板也换了的尴尬。

面向过程的开发时，为了使得常用代码可以复用，一般都会把这些常用代码写成许许多多函数的程序库，这样我们在做新项目时，去调用这些低层的函数就可以了。

这也就叫做高层模块依赖低层模块。

### 2.3. 里氏代换原则

里氏代换原则是Barbara Liskov女士在1988年发表的[ASD]

一个软件实体如果使用的是一个父类的话，那么一定适用于其子类，而且它察觉不出父类对象和子类对象的区别。也就是说，在软件里面，把父类都替换成它的子类，程序的行为没有变化，简单地说，子类型必须能够替换掉它们的父类型[ASD]。

也正因为有了这个原则，使得继承复用成为了可能，只有当子类可以替换掉父类，软件单位的功能不受到影响时，父类才能真正被复用，而子类也能够在父类的基础上增加新的行为。

由于有里氏代换原则，才使得开放-封闭成为了可能。

由于子类型的可替换性才使得使用父类类型的模块在无需修改的情况下就可以扩展。

依赖倒转其实就是谁也不要依靠谁，除了约定的接口，大家都可以灵活自如。

依赖倒转其实可以说是面向对象设计的标志，用哪种语言来编写程序不重要，如果编写时考虑的都是如何针对抽象编程而不是针对细节编程，即程序中所有的依赖关系都是终止于抽象类或者接口，那就是面向对象的设计，反之那就是过程化的设计了[ASD]。

### 2.4. 开放-封闭原则

在软件设计模式中，这种不能修改，但可以扩展的思想也是最重要的一种设计原则，它就是开放-封闭原则（The Open-Closeed Principle，简称OCP）或叫开-闭原则。

对于扩展是开放的（Open for extension）

对于更改是封闭的（Closed for modification）’[ASD]。

怎样的设计才能面对需求的改变却可以保持相对稳定，从而使得系统可以在第一个版本以后不断推出新的版本呢？[ASD]，开放-封闭给我们答案。

开放-封闭原则的意思就是说，你设计的时候，时刻要考虑，尽量让这个类是足够好，写好了就不要去修改了，如果新需求来，我们增加一些类就完事了，原来的代码能不动则不动。

无论模块是多么的‘封闭’，都会存在一些无法对之封闭的变化。既然不可能完全封闭，设计人员必须对于他设计的模块应该对哪种变化封闭做出选择。他必须先猜测出最有可能发生的变化种类，然后构造抽象来隔离那些变化[ASD]。

我们是很难预先猜测，但我们却可以在发生小变化时，就及早去想办法应对发生更大变化的可能。也就是说，等到变化发生时立即采取行动[ASD]。正所谓，同一地方，摔第一跤不是你的错，再次在此摔跤就是你的不对了。

在我们最初编写代码时，假设变化不会发生。当变化发生时，我们就创建抽象来隔离以后发生的同类变化[ASD]。

面对需求，对程序的改动是通过增加新代码进行的，而不是更改现有的代码[ASD]。这就是‘开放-封闭原则’的精神所在。

我们希望的是在开发工作展开不久就知道可能发生的变化。查明可能发生的变化所等待的时间越长，要创建正确的抽象就越困难[ASD]。

开放-封闭原则是面向对象设计的核心所在。遵循这个原则可以带来面向对象技术所声称的巨大好处，也就是可维护、可扩展、可复用、灵活性好。开发人员应该仅对程序中呈现出频繁变化的那些部分做出抽象，然而，对于应用程序中的每个部分都刻意地进行抽象同样不是一个好主意。拒绝不成熟的抽象和抽象本身一样重要[ASD]

### 2.5. 单一职责原则

就一个类而言，应该仅有一个引起它变化的原因[ASD]。

如果一个类承担的职责过多，就等于把这些职责耦合在一起，一个职责的变化可能会削弱或者抑制这个类完成其他职责的能力。这种耦合会导致脆弱的设计，当变化发生时，设计会遭受到意想不到的破坏[ASD]。

软件设计真正要做的许多内容，就是发现职责并把那些职责相互分离[ASD]。其实要去判断是否应该分离出类来，也不难，那就是如果你能够想到多于一个的动机去改变一个类，那么这个类就具有多于一个的职责[ASD]，就应该考虑类的职责分离。”

编程时，我们却是要在类的职责分离上多思考，做到单一职责，这样你的代码才是真正的易维护、易扩展、易复用、灵活多样。

- [面向对象设计-单一职责原则(SRP) - 简书](http://www.jianshu.com/p/f9d15827465d)
- [设计模式六大原则之 单一职责原则 | DannyLee](http://studyai.site/2015/09/06/单一职责原则/)

### 2.6. 迪米特法则

‘迪米特法则（LoD）’也叫最少知识原则。[J&DP]

迪米特法则首先强调的前提是在类的结构设计上，每一个类都应当尽量降低成员的访问权限[J&DP]，也就是说，一个类包装好自己的private状态，不需要让别的类知道的字段或行为就不要公开。

迪米特法则其根本思想，是强调了类之间的松耦合。

类之间的耦合越弱，越有利于复用，一个处在弱耦合的类被修改，不会对有关系的类造成波及。也就是说，信息的隐藏促进了软件的复用。

## 3. 单例模式

如果你不对构造方法做改动的话，是不可能阻止他人不去用new的。所以我们完全可以直接就把这个类的构造方法改成私有（private），你应该知道，所有类都有构造方法，不编码则系统默认生成空的构造方法，若有显示定义的构造方法，默认的构造方法就会失效。

客户端不再考虑是否需要去实例化的问题，而把责任都给了应该负责的类去处理。其实这就是一个很基本的设计模式：单例模式。

通常我们可以让一个全局变量使得一个对象被访问，但它不能防止你实例化多个对象。一个最好的办法就是，让类自身负责保存它的唯一实例。这个类可以保证没有其他实例可以被创建，并且它可以提供一个访问该实例的方法。[DP]

Singleton类，定义一个GetInstance操作，允许客户访问它的唯一实例。GetInstance是一个静态方法，主要负责创建自己的唯一实例。

单例模式除了可以保证唯一的实例

单例模式因为Singleton类封装它的唯一实例，这样它可以严格地控制客户怎样访问它以及何时访问它。简单地说就是对唯一实例的受控访问。

实用类通常也会采用私有化的构造方法来避免其有实例。

实用类不保存状态，仅提供一些静态方法或静态属性让你使用，而单例类是有状态的。实用类不能用于继承多态，而单例虽然实例唯一，却是可以有子类来继承。实用类只不过是一些方法属性的集合，而单例却是有着唯一的对象实例。

lock是确保当一个线程位于代码的临界区时，另一个线程不进入临界区。如果其他线程试图进入锁定的代码，则它将一直等待（即被阻止），直到该对象被释放。[MSDN]

这段代码使得对象实例由最先进入的那个线程创建，以后的线程在进入时不会再去创建对象实例了。由于有了lock，就保证了多线程环境下的同时访问也不会造成多个实例的生成。

不用让线程每次都加锁，而只是在实例未被创建的时候再加锁处理。同时也能保证多线程的安全。这种做法被称为Double-Check Locking（双重锁定）。

C#与公共语言运行库也提供了一种‘静态初始化’方法，这种方法不需要开发人员显式地编写线程安全代码，即可解决多线程环境下它是不安全的问题。[MSDN]

这样的实现与前面的示例类似，也是解决了单例模式试图解决的两个基本问题：全局访问和实例化控制，公共静态属性为访问实例提供了一个全局访问点。不同之处在于它依赖公共语言运行库来初始化变量。由于构造方法是私有的，因此不能在类本身以外实例化Singleton类；因此，变量引用的是可以在系统中存在的唯一的实例。不过要注意，instance变量标记为readonly，这意味着只能在静态初始化期间或在类构造函数中分配变量[MSDN]。由于这种静态初始化的方式是在自己被加载时就将自己实例化，所以被形象地称之为饿汉式单例类，原先的单例模式处理方式是要在第一次被引用时，才会将自己实例化，所以就被称为懒汉式单例类。[J&DP]

饿汉式，即静态初始化的方式，它是类一加载就实例化的对象，所以要提前占用系统资源。然而懒汉式，又会面临着多线程访问的安全性问题，需要做双重锁定这样的处理才可以保证安全。所以到底使用哪一种方式，取决于实际的需求。

### 3.1. 使用反射和反序列化产生多个“单例类”实例

D瓜哥根据以前学习设计模式的经验来看，单例模式的实现类只能产生一个对象。估计这也是我们大家普遍看法。但是，在一次面试时，被面试官问到“单例模式的实现类能否产生多个对象？”然后，D瓜哥当场就Hold不住了。

不过，当时善意的面试官提醒，可以用反射。昨天看《Java 程序性能优化》时，提到用反序列化也可以产生多个方法。然后，又被我同学问到这个问题。干脆写一篇文章，总结一下吧。

### 3.2. 定义

根据 GoF 的著名著作 [《设计模式》](http://book.douban.com/subject/1052241/)，单例模式的定义如下：

> - 单例模式（Singleton）
>
>   保证一个类仅有一个实例，并提供一个访问它的全局访问点。

— Erich Gamma、Richard Helm、Ralph Johnson、John Vlissides《设计模式》

![image-20220123210518455](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123210518455-9091d6.png)

在Java中，单例模式实现方式有两种：

- 静态初始化的方式是在自己被加载时就将自己实例化。这种方式被称为“饿汉式单例类”；
- 在第一次被引用时，才会将自己实例化。这种方式被称为“懒汉式单例类”。

下面做针对介绍。

### 3.3. 饿汉式单例类

饿汉式单例类的实现代码如下：

代码 1. 饿汉式单例类

```java
package com.diguage.didp.singleton;

/**
 * 饿汉式单例类
 *
 */
public class HungrySingleton {
  private static HungrySingleton instance = new HungrySingleton();

  private HungrySingleton() {}

  public static HungrySingleton getInstance() {
    return instance;
  }
}
```

### 3.4. 懒汉式单例类

懒汉式单例类的实现代码如下：

代码 2. 懒汉式单例类

```java
package com.diguage.didp.singleton;

/**
 * 懒汉式单例类
 */
public class LazySingleton {
  private static LazySingleton instance = null;

  private LazySingleton() {}

  public static LazySingleton getInstance() {
    if (null == instance) {
      synchronized (LazySingleton.class) {
        if (null == instance) {
          instance = new LazySingleton();
        }
      }
    }
    return instance;
  }
}
```

代码 3. 懒汉式单例类的并发测试

```java
package com.diguage.didp.singleton;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 单例模式并发性测试
 */
public class ConcurrentTest {
  public static void main(String[] args) {
    int THREAD_COUNT = 10000;
    CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
    ConcurrentMap concurrentMap = new ConcurrentHashMap();
    ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
    for (int i = 0; i < THREAD_COUNT; i++) {
      executorService.execute(new SingletonFactory(latch, concurrentMap, i));
      latch.countDown();
    }
    executorService.shutdown();
  }
}

class SingletonFactory implements Runnable {
  private CountDownLatch latch;
  private ConcurrentMap concurrentMap;
  private int id;

  public SingletonFactory(CountDownLatch latch, ConcurrentMap concurrentMap, int id) {
    this.latch = latch;
    this.concurrentMap = concurrentMap;
    this.id = id;
  }

  public void run() {
    try {
      latch.await();
      LazySingleton instance = LazySingleton.getInstance();
      concurrentMap.put(instance, instance);
      System.out.println(id + "\t" + concurrentMap.size());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
```

代码 4. 带 `volatile` 修饰的懒汉式单例类

```java
package com.diguage.didp.singleton;

import java.io.Serializable;

/**
 * 带 <code>volatile</code> 修饰的懒汉式单例类
 */
public class VolatileLazySingleton implements Serializable {
  private static volatile VolatileLazySingleton instance = null;

  private VolatileLazySingleton() {}

  public static VolatileLazySingleton getInstance() {
    if (null == instance) {
      synchronized (VolatileLazySingleton.class) {
        if (null == instance) {
          instance = new VolatileLazySingleton();
        }
      }
    }
    return instance;
  }
}
```

代码 5. 可序列化懒汉式单例类

```java
package com.diguage.didp.singleton;

import java.io.Serializable;

/**
 * 可以序列化的懒汉式单例类
 *
 * <p>注：不正确
 *
 */
public class SerializableLazySingleton implements Serializable {
  private static volatile SerializableLazySingleton instance = null;

  private SerializableLazySingleton() {}

  public static SerializableLazySingleton getInstance() {
    if (null == instance) {
      synchronized (SerializableLazySingleton.class) {
        if (null == instance) {
          instance = new SerializableLazySingleton();
        }
      }
    }
    return instance;
  }
}
```

代码 6. 正确的可序列化懒汉式单例类

```java
package com.diguage.didp.singleton;

import java.io.Serializable;

/**
 * 可以序列化的懒汉式单例类
 *
 */
public class CorrectSerializableLazySingleton implements Serializable {

  private static volatile CorrectSerializableLazySingleton instance = null;

  private CorrectSerializableLazySingleton() {}

  public static CorrectSerializableLazySingleton getInstance() {
    if (null == instance) {
      synchronized (CorrectSerializableLazySingleton.class) {
        if (null == instance) {
          instance = new CorrectSerializableLazySingleton();
        }
      }
    }
    return instance;
  }

  private Object readResolve() {
    return instance;
  }
}
```

代码 7. 序列化测试

```java
package com.diguage.didp.singleton;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 序列化测试
 *
 */
public class SerializationTest {
  public static void main(String[] args) throws IOException, ClassNotFoundException {

    testSerialization(SerializableLazySingleton.getInstance());
    testSerialization(CorrectSerializableLazySingleton.getInstance());
    testSerialization(Singleton.INSTANCE);
  }

  public static <T> void testSerialization(T t) throws IOException, ClassNotFoundException {
    // 将对象写入数组中
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(t);
    baos.close();

    byte[] objectByteArray = baos.toByteArray();

    // 从数组中读取对象
    ByteArrayInputStream bais = new ByteArrayInputStream(objectByteArray);
    ObjectInputStream ois = new ObjectInputStream(bais);
    T newInstance = (T) ois.readObject();
    //判断是否是同一个对象
    Class<?> clazz = t.getClass();
    String canonicalName = clazz.getCanonicalName();
    System.out.println(canonicalName + ": " + (newInstance == t));
  }
}
```

代码 8. 枚举实现的单例模式

```java
package com.diguage.didp.singleton;


public enum Singleton {
  INSTANCE
}
```

代码 9. 单例模式的反射测试

```java
package com.diguage.didp.singleton;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * 反射测试
 *
 */
public class ReflectionTest {
  public static void main(String[] args)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
          InstantiationException {
    testReflection(CorrectSerializableLazySingleton.getInstance());
    //    testReflection(Singleton.INSTANCE);
  }

  private static <T> void testReflection(T t)
      throws NoSuchMethodException, InstantiationException, IllegalAccessException,
          InvocationTargetException {
    Class<?> clazz = t.getClass();
    Class<?>[] params = {};
    Constructor<?> constructor = clazz.getDeclaredConstructor(params);
    constructor.setAccessible(true);
    T instance = (T) constructor.newInstance();
    System.out.println((instance == t) + "\t:\t" + clazz.getCanonicalName());
  }
}
```

代码 10. 单例模式的 Java 黑魔法测试

```java
package com.diguage.didp.singleton;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * {@link Unsafe} 创建实例测试
 */
public class UnsafeTest {
  public static void main(String[] args)
      throws NoSuchFieldException, IllegalAccessException, InstantiationException {
    testUnsafe(CorrectSerializableLazySingleton.getInstance());
    testUnsafe(Singleton.INSTANCE);
  }

  private static <T> void testUnsafe(T t)
      throws NoSuchFieldException, IllegalAccessException, InstantiationException {
    Unsafe unsafe = getUnsafe();
    Class<?> clazz = t.getClass();
    T instance = (T) unsafe.allocateInstance(clazz);
    System.out.println((instance == t) + "\t:\t" + clazz.getCanonicalName());
  }

  private static Unsafe getUnsafe() throws NoSuchFieldException, IllegalAccessException {
    // 通过反射得到theUnsafe对应的Field对象
    Field field = Unsafe.class.getDeclaredField("theUnsafe");
    // 设置该Field为可访问
    field.setAccessible(true);
    // 通过Field得到该Field对应的具体对象，传入null是因为该Field为static的
    return (Unsafe) field.get(null);
  }
}
```

两种方式各有优缺。

饿汉式，即静态初始化的方式，它是类一加载就实例化对象。

所以，加载时间比较长，而且要提前占用系统资源，如果后续用不到这个对象，会造成浪费。

懒汉式，又会面临多线程访问的安全性问题，需要使用双重锁定才能保证安全性，由于 Java 虚拟机的问题，在 JDK5 以后双重锁定能可以正常工作；另外，由于是在使用时初始化，在第一次调用时耗时比较长。所以，至于到底使用哪种方式，取决于实际需求。

代码 11. 并发性测试

```java
package com.diguage.didp.singleton;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 单例模式并发性测试
 *
 * @author D瓜哥, https://www.diguage.com/
 * @since 16/11/2016.
 */
public class ConcurrentTest {
  public static void main(String[] args) {
    int THREAD_COUNT = 10000;
    CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
    ConcurrentMap concurrentMap = new ConcurrentHashMap();
    ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
    for (int i = 0; i < THREAD_COUNT; i++) {
      executorService.execute(new SingletonFactory(latch, concurrentMap, i));
      latch.countDown();
    }
    executorService.shutdown();
  }
}

class SingletonFactory implements Runnable {
  private CountDownLatch latch;
  private ConcurrentMap concurrentMap;
  private int id;

  public SingletonFactory(CountDownLatch latch, ConcurrentMap concurrentMap, int id) {
    this.latch = latch;
    this.concurrentMap = concurrentMap;
    this.id = id;
  }

  public void run() {
    try {
      latch.await();
      LazySingleton instance = LazySingleton.getInstance();
      concurrentMap.put(instance, instance);
      System.out.println(id + "\t" + concurrentMap.size());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
```

### 3.5. 产生多个“单例类”实例

还回到文章开头，问到的问题：单例模式的实现类能否产生多个对象？既然写这篇文章，那么答案肯定是肯定的。那么怎么产生呢？

有两种方式可以做到。我们这里分别来试验一下。

问题：既然 `static` 变量是类共享的，修改后，其他对象也应该立即修改，那么还需要 `volidate` 吗？

### 3.6. 枚举

《The Java® Language Specification Java SE 8 Edition》

> **An enum type has no instances other than those defined by its enum constants. It is a compile-time error to attempt to explicitly instantiate an enum type (§15.9.1).**
>
> In addition to the compile-time error, three further mechanisms ensure that no instances of an enum type exist beyond those defined by its enum constants:
>
> - The final clone method in Enum ensures that enum constants can never be cloned.
> - Reflective instantiation of enum types is prohibited.
> - Special treatment by the serialization mechanism ensures that duplicate instances are never created as a result of deserialization.

— James Gosling, Bill Joy, Guy Steele, Gilad Bracha, Alex Buckley《The Java® Language Specification Java SE 8 Edition》 -- 8.9. Enum Types

### 3.7. 注册表或者应用上下文

后续补充内容，典型例子就是 Spring 框架。

### 3.8. 参考资料：

- [设计模式-单例模式(Singleton)各种写法和分析比较 - 李可乐的专栏 - 博客频道 - CSDN.NET](http://blog.csdn.net/card361401376/article/details/51340822?hmsr=toutiao.io&utm_medium=toutiao.io&utm_source=toutiao.io)
- [《大话设计模式》](http://book.douban.com/subject/2334288/)
- [《Java程序性能优化》](http://book.douban.com/subject/19969386/)
- [Creating New Class Instances](http://docs.oracle.com/javase/tutorial/reflect/member/ctorInstance.html)
- [单例与序列化的那些事儿-HollisChuang’s Blog](http://www.hollischuang.com/archives/1144)
- [Chapter 8. Classes](http://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.9)

## 4. 代理模式

### 4.1. 定义

> - 代理模式（Proxy）
>
>   为其他对象提供一种代理以控制这个对象的访问。

— Erich Gamma、Richard Helm、Ralph Johnson、John Vlissides《设计模式》

### 4.2. 类图

![image-20220123210949927](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123210949927-f6df7e.png)

Subject类，定义了RealSubject和Proxy的共用接口，这样就在任何使用RealSubject的地方都可以使用Proxy。

### 4.3. 静态代理模式

代码 12. proxy/Subject.java 类

```
package com.diguage.didp.proxy;

/**
 * Subject类，定义了 {@link RealSubject} 和 {@link Proxy} 的共用接口。
 *
 */
public interface Subject {
  void request();
}
```

RealSubject类，定义Proxy所代表的真实实体。

代码 13. proxy/RealSubject.java 类

```
package com.diguage.didp.proxy;

/**
 * RealSubject 类
 *
 */
public class RealSubject implements Subject {
  @Override
  public void request() {
    System.out.println("真是请求！");
  }
}
```

Proxy类，保存一个引用使得代理可以访问实体，并提供一个与Subject的接口相同的接口，这样代理就可以用来替代实体。

代码 14. proxy/Proxy.java 类

```java
package com.diguage.didp.proxy;


 
public class Proxy implements Subject {
  private Subject realSubject;

  public Proxy(Subject realSubject) {
    this.realSubject = realSubject;
  }

  @Override
  public void request() {
    System.out.println("使用代理开始请求…");
    realSubject.request();
  }
}
```

代码 15. proxy/Client.java 类

```java
package com.diguage.didp.proxy;

/**
 * Client 类
 *
 */
public class Client {
  public static void main(String[] args) {
    Subject subject = new Proxy(new RealSubject());
    subject.request();
  }
}
```

#### 4.3.1. 再来一个例子…

代码 16. proxy/UserService.java 类

```java
package com.diguage.didp.proxy;

/**
 * 用户接口
 *
 */
public interface UserService {
  String getById(int id);
}
```

代码 17. proxy/UserServiceImpl.java 类

```java
package com.diguage.didp.proxy;

import java.util.concurrent.TimeUnit;


public class UserServiceImpl implements UserService {
  @Override
  public String getById(int id) {
    try {
      System.out.println("真是请求：根据ID获取对应用户…");
      TimeUnit.MILLISECONDS.sleep(id);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return "User-" + id;
  }
}
```

代码 18. proxy/UserServiceAuthenticationProxy.java 类

```java
package com.diguage.didp.proxy;


public class UserServiceAuthenticationProxy implements UserService {
  UserService userService;

  public UserServiceAuthenticationProxy(UserService userService) {
    this.userService = userService;
  }

  @Override
  public String getById(int id) {
    if (id < 1000) {
      System.out.println("非特权用户，禁止访问……");
      return null;
    }
    return userService.getById(id);
  }
}
```

代码 19. proxy/UserServiceProxyMain.java 类

```java
package com.diguage.didp.proxy;


public class UserServiceProxyMain {
  public static void main(String[] args) {
    UserService userService = new UserServiceImpl();
    UserService proxy = new UserServiceAuthenticationProxy(userService);
    System.out.println(proxy.getById(123));
  }
}
```

|      | **请思考：**到现在为止，你们发现什么问题了吗？代理模式这样实现有什么问题吗？还有其他改进空间吗？？ |
| ---- | ------------------------------------------------------------ |

### 4.4. 动态代理

先看一个图，看看方法调用的：

![image-20220123211144349](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123211144349-4d41a4.png)

Java 是面向对象，那么 Java 如何对 Java 中 `package`、 `class`、 方法等如何建模呢？

我们可以从上图的流程中可以横切一刀，如下图：



![image-20220123211156450](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123211156450-edfb31.png)

代码 20. proxy/ProfilerInvocationHandler.java 类

```java
package com.diguage.didp.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;


public class ProfilerInvocationHandler implements InvocationHandler {
  private Object realObject;

  public ProfilerInvocationHandler(Object realObject) {
    this.realObject = realObject;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    System.out.printf("Profiler======\n%s\n", method.getName());
    long start = System.currentTimeMillis();
    Object result = method.invoke(realObject, args);
    long time = System.currentTimeMillis() - start;
    System.out.println("耗时: " + time);
    System.out.println("Profiler======");
    return result;
  }
}
```

代码 21. proxy/LoggerInvocationHandler.java 类

```java
package com.diguage.didp.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;


public class LoggerInvocationHandler implements InvocationHandler {
  private Object realObject;

  public LoggerInvocationHandler(Object realObject) {
    this.realObject = realObject;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    System.out.printf("Logger------\nMethod: %s\n", method.getName());
    if (args != null && args.length > 0) {
      System.out.println("Params:");
      for (Object arg : args) {
          System.out.println(arg);
      }
    }
    Object result = method.invoke(realObject, args);
    System.out.println("Logger------");
    return result;
  }
}
```

代码 22. proxy/UserServiceDynamicProxyMain.java 类

```java
package com.diguage.didp.proxy;

import java.lang.reflect.Proxy;

/**
 * @author D瓜哥, https://www.diguage.com/
 * @since 16/11/2016.
 */
public class UserServiceDynamicProxyMain {
  public static void main(String[] args) {
    UserService userService = new UserServiceImpl();

    ClassLoader classLoader = UserService.class.getClassLoader();
    Class<?>[] interfaces = UserServiceImpl.class.getInterfaces();

    UserService profilerInstance =
        (UserService)
            Proxy.newProxyInstance(
                classLoader,
                interfaces,
                new ProfilerInvocationHandler(userService));

    UserService logProfilerInstance =
        (UserService)
            Proxy.newProxyInstance(
                classLoader,
                interfaces,
                new LoggerInvocationHandler(profilerInstance));

    System.out.println("最终结果：" + logProfilerInstance.getById(345));

    Subject subject = (Subject) Proxy.newProxyInstance(
        RealSubject.class.getClassLoader(),
        RealSubject.class.getInterfaces(),
        new LoggerInvocationHandler(new RealSubject()));
    subject.request();
  }

//  public static <T> T newProxyInstance(ClassLoader classLoader,
//                                       InvocationHandler handler, T... t) {
//    return (T) Proxy.newProxyInstance(classLoader, t.getClass(), handler);
//  }
}
```

流程与这个类似这样的，还有 Java Web 中的 `Filter`， Spring MVC 中的 ``

|      | **请思考：**代理模式这样实现有什么问题吗？还有其他改进空间吗？？ |
| ---- | ------------------------------------------------------------ |

### 4.5. 来个黑魔法…

代码 23. proxy/asm/Account.java 类

```java
package com.diguage.didp.proxy.asm;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Account {
  public int operation() {
    System.out.println("operation……");
    int timeout = new Random().nextInt(1000);
    try {
      TimeUnit.MILLISECONDS.sleep(timeout + 1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return timeout;
  }

  public String getById(int id) {
    System.out.println("getById……");
    try {
      // 以参数传递过来的数字来决定休眠时间，
      // 来检验实际效果
      TimeUnit.MILLISECONDS.sleep(id);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return "Account-" + id;
  }

  public static void main(String[] args) {
    Account account = new Account();
    account.operation();
    account.getById(123);
  }
}
```

我们来看一下，编译后生成的文件……

```bash
cd /Users/diguage/Documents/wiki.diguage.com/java/deep-in-design-patterns/target/classes/com/diguage/didp/proxy/asm

vim /Users/diguage/Documents/wiki.diguage.com/java/deep-in-design-patterns/target/classes/com/diguage/didp/proxy/asm/Account.class

:%!xxd


javap -v /Users/diguage/Documents/wiki.diguage.com/java/deep-in-design-patterns/target/classes/com/diguage/didp/proxy/asm/Account.class
```

Java 虚拟机规范中，对 Java Class 文件格式的定义： [Chapter 4. The class File Format](https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html)

![image-20220123211343325](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123211343325-393b54.png)



图 1. Java Class文件格式

Java 虚拟机规范中，对 Java 方法表格式的定义： [Chapter 4. The class File Format](https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.6)

![image-20220123211401220](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123211401220-ea40f5.png)



图 2. Java Class文件格式-方法表格式

![image-20220123211413705](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123211413705-80b856.png)



图 3. Java 处理流程

![image-20220123211432490](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123211432490-7bd01f.png)



图 4. JVM 语言

![image-20220123211444715](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123211444715-75c7da.png)



图 5. JVM 语言

1. [实例分析Java Class的文件结构 | 酷 壳 - CoolShell](https://coolshell.cn/articles/9229.html)
2. [利用javap反编译Scala字节码文件 - 咩咩声 不绝于耳](https://my.oschina.net/sulliy/blog/61133)
3. [Java Virtual Machine](https://avaldes.com/java-virtual-machine/)
4. [4. Java Tools - Client-Server Web Apps with JavaScript and Java [Book](https://www.safaribooksonline.com/library/view/client-server-web-apps/9781449369323/ch04.html)]
5. [Java Memory Model](http://tutorials.jenkov.com/java-concurrency/java-memory-model.html)

时间统计类……

代码 24. proxy/asm/Profiler.java 类

```java
package com.diguage.didp.proxy.asm;


public class Profiler {
  static ThreadLocal<Long> t = new ThreadLocal<Long>();

  public static void start() {
    t.set(System.currentTimeMillis());
  }

  public static void end() {
    long time = System.currentTimeMillis() - t.get();
    System.out.print(Thread.currentThread().getStackTrace()[2] + " speed:");
    System.out.println(time);
  }
}
```

修改字节码，在每个方法的开始部分和返回部分来插入我们的时间统计代码……

代码 25. proxy/asm/ProfilerMethodAdapter.java 类

```java
package com.diguage.didp.proxy.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public class ProfilerMethodAdapter extends MethodVisitor implements Opcodes {

  public ProfilerMethodAdapter(MethodVisitor mv) {
    super(Opcodes.ASM5, mv);
  }

  @Override
  public void visitCode() {
    visitMethodInsn(Opcodes.INVOKESTATIC, "com/diguage/didp/proxy/asm/Profiler", "start", "()V");
    super.visitCode();
  }

  @Override
  public void visitInsn(int opcode) {
    if (opcode >= IRETURN && opcode <= RETURN) {
      visitMethodInsn(Opcodes.INVOKESTATIC, "com/diguage/didp/proxy/asm/Profiler", "end", "()V");
    }
    mv.visitInsn(opcode);
  }
}
```

代码 26. proxy/asm/ProfilerClassAdapter.java 类

```java
package com.diguage.didp.proxy.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public class ProfilerClassAdapter extends ClassVisitor {
  public ProfilerClassAdapter(ClassVisitor cv) {
    super(Opcodes.ASM5, cv);
  }

  @Override
  public MethodVisitor visitMethod(
          final int access,
          final String name,
          final String desc,
          final String signature,
          final String[] exceptions) {
    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    MethodVisitor wrapperMv = mv;
    if (mv != null) {
      // TODO 如何实现只编制指定类下的方法呢？
//      if (name.equals("operation")) {
      wrapperMv = new ProfilerMethodAdapter(mv);
//      }
    }
    return wrapperMv;
  }
}
```

![image-20220123211551905](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123211551905-e010e2.png)

图 6. 操起键盘就是干

代码 27. proxy/asm/ProfilerWeaveMain.java 类

```java
package com.diguage.didp.proxy.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;


public class ProfilerWeaveMain {
  public static void main(String[] args) throws IOException, URISyntaxException {
    System.out.println("开始修改字节码…");
    // 如何实现编制指点类？通过配置文件来获得！
    String className = Account.class.getName();
    ClassReader cr = new ClassReader(className);
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    ProfilerClassAdapter classAdapter = new ProfilerClassAdapter(cw);
    cr.accept(classAdapter, ClassReader.SKIP_DEBUG);
    byte[] data = cw.toByteArray();
    URL url =
        Account.class.getClassLoader().getResource(className.replaceAll("\\.", "/") + ".class");
//    System.out.println(url);
//    System.out.println(
//        "file:/Users/diguage/Documents/wiki.diguage.com/java/deep-in-design-patterns/target/classes/com/diguage/didp/proxy/asm/Account.class");
//    //    File file = new File(url.replaceFirst("file:/", "file:///"));
    File file = new File(url.toURI());
//    System.out.println(file.exists());
//    if (!file.exists()) {
//      System.out.println("Create：" + file.createNewFile());
//    }
//    System.out.println(file.isFile());
//    System.out.println(file.canWrite());
    FileOutputStream fout = new FileOutputStream(file);
    fout.write(data);
    fout.close();
    System.out.println("字节码修改完毕。");
  }
}

```

再来运行一下看看…

代码 28. proxy/asm/AfterWeaveProfilerMain.java 类

```java
package com.diguage.didp.proxy.asm;


public class AfterWeaveProfilerMain {
  public static void main(String[] args) {
    Account account = new Account();
    System.out.println(account.operation());
    // 可以通过传递不同的数字来观察耗时统计，
    // 来检验是否修改过字节码……
    System.out.println(account.getById(2345));
    System.out.println(account.toString());
  }
}
```

> 更多操作在使用 ASM 生成代理 class 文件时，发现确定类文件的路径是个大问题。这里需要注意的是针对 file 的 URL 格式规范。
>
> 1. [file URI scheme](https://en.wikipedia.org/wiki/File_URI_scheme)
> 2. [Is a “file://” path a URL?](http://stackoverflow.com/questions/13869526/is-a-file-path-a-url)
> 3. [Uniform Resource Identifier (URI) Schemes](http://www.iana.org/assignments/uri-schemes/uri-schemes.xhtml)
> 4. [RFC1738: Uniform Resource Locators (URL)](http://www.rfc-editor.org/rfc/rfc1738.txt)

|      | **请思考：**代理模式这样实现有什么问题吗？还有其他改进空间吗？？ |
| ---- | ------------------------------------------------------------ |

### 4.6. 来点通俗易懂的…

用 Byte Buddy 来搞…

代码 29. proxy/bytebuddy/ProfilerAnnotationInterceptor.java 类

```java
package com.diguage.didp.proxy.bytebuddy;

import net.bytebuddy.asm.Advice;

public class ProfilerAnnotationInterceptor {
  public static ThreadLocal<Long> threadLocal = new ThreadLocal();

  @Advice.OnMethodEnter
  public static void enter(@Advice.Origin("#t.#m") String signature) {
    System.out.printf("Enter: %s\n", signature);
    long start = System.currentTimeMillis();
    threadLocal.set(start);
  }

  @Advice.OnMethodExit
  public static void exit(@Advice.Origin("#t.#m") String signature) {
    long value = System.currentTimeMillis() - threadLocal.get();
    System.out.printf("Exit: %s\nTime: %d\n\n", signature, value);
  }
}
```

代码 30. proxy/bytebuddy/ProflierAnnotationAgent.java 类

```java
package com.diguage.didp.proxy.bytebuddy;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;


public class ProflierAnnotationAgent {
  public static void premain(String agentArgs, Instrumentation instrumentation) {
    System.out.println("Premain started");
    try {
      new AgentBuilder.Default()
//        .with(AgentBuilder.Listener.StreamWriting.toSystemOut()) // Debug
          .with(AgentBuilder.TypeStrategy.Default.REBASE)
//        .type((typeDescription, classLoader, javaModule, aClass, protectionDomain) -> true)
          .type(ElementMatchers.any())
          .transform((builder, typeDescription, classLoader, javaModule) ->
              builder.visit(Advice.to(ProfilerAnnotationInterceptor.class).on(ElementMatchers.any())))
          .installOn(instrumentation);
    } catch (RuntimeException e) {
      System.out.println("Exception instrumenting code : " + e);
      e.printStackTrace();
    }
  }
}
```

#### 4.6.1. 再来一组更简单的…

代码 31. proxy/bytebuddy/ProfilerInterceptor.java 类

```java
package com.diguage.didp.proxy.bytebuddy;

import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;



public class ProfilerInterceptor {
  @RuntimeType
  public static Object intercept(@Origin Method method,
                                 @SuperCall Callable<?> callable) {
    long start = System.currentTimeMillis();
    try {
      return callable.call();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage(), e);
    } finally {
      System.out.println(method + " took " + (System.currentTimeMillis() - start) + "\n\n");
    }
  }
}
```

代码 32. proxy/bytebuddy/ProflierAgent.java 类

```java
package com.diguage.didp.proxy.bytebuddy;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;


public class ProflierAgent {
  public static void premain(String arguments,
                             Instrumentation instrumentation) {
    new AgentBuilder.Default()
        .type(ElementMatchers.any())
        .transform((builder, type, classLoader, module) ->
            builder.method(ElementMatchers.any())
                .intercept(MethodDelegation.to(ProfilerInterceptor.class))
        ).installOn(instrumentation);
  }
}
```

|      | **请思考：**代理模式这样实现有什么问题吗？还有其他改进空间吗？？ |
| ---- | ------------------------------------------------------------ |

[Manipulating Bytecode with ASM and Javassist - New Relic blog](https://blog.newrelic.com/2014/09/29/diving-bytecode-manipulation-creating-audit-log-asm-javassist/)

### 4.7. 可不可更透明点…

代码 33. proxy/agent/PreMainAddTimeStatAgent.java 类

```java
package com.diguage.didp.proxy.agent;

import com.diguage.didp.proxy.asm.ProfilerClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.Instrumentation;


public class PreMainAddTimeStatAgent {
  public static void premain(String agentArgs, Instrumentation instrumentation) {
    System.out.println("agentArgs: " + agentArgs);
    instrumentation.addTransformer(
        (loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
          if (className.equals("com/diguage/didp/proxy/asm/Account")) {
            System.out.println("meet com.diguage.didp.proxy.asm.Account");
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw =
                new ClassWriter(
                    (ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES));
            ProfilerClassAdapter classAdapter = new ProfilerClassAdapter(cw);
            cr.accept(classAdapter, ClassReader.SKIP_DEBUG);
            return cw.toByteArray();
          } else {
            System.out.println("\nload: " + className);
            return classfileBuffer;
          }
        });
  }
}
```

代码 34. proxy/agent/RunAccountMain.java 类

```java
package com.diguage.didp.proxy.agent;

import com.diguage.didp.proxy.asm.Account;



public class RunAccountMain {
  public static void main(String[] args) {
    Account account = new Account();
    System.out.println(account.getById(789));
    System.out.println(account.operation());
  }
}
```

|      | **请思考：**代理模式这样实现有什么问题吗？还有其他改进空间吗？应用上线后怎么搞？ |
| ---- | ------------------------------------------------------------ |

1. [How-to guide to writing a javaagent | zeroturnaround.com](https://zeroturnaround.com/rebellabs/how-to-inspect-classes-in-your-jvm/)
2. [javaagent | 刘正阳](https://liuzhengyang.github.io/2017/03/15/javaagent/)
3. [Java agents, Javassist and Byte Buddy | Ivan Yurchenko](https://ivanyu.me/blog/2017/11/04/java-agents-javassist-and-byte-buddy/)
4. [Easily Create Java Agents with Byte Buddy](https://www.infoq.com/articles/Easily-Create-Java-Agents-with-ByteBuddy)
5. [Performance comparison of Java Runtime Code Generation Libraries zeroturnaround.com](https://zeroturnaround.com/rebellabs/testing-the-performance-of-4-java-runtime-code-generators-cglib-javassist-jdk-proxy-byte-buddy/)

### 4.8. Java 应用上线后…

BTrace

### 4.9. 作用

一般来说分为几种

- 第一种应用是远程代理，也就是为一个对象在不同的地址空间提供局部代表。这样可以隐藏一个对象存在于不同地址空间的事实[DP]。
- 第二种应用是虚拟代理，是根据需要创建开销很大的对象。通过它来存放实例化需要很长时间的真实对象[DP]。
- 第三种应用是安全代理，用来控制真实对象访问时的权限[DP]。
- 第四种是智能指引，是指当调用真实的对象时，代理处理另外一些事[DP]。

代理模式其实就是在访问对象时引入一定程度的间接性，因为这种间接性，可以附加多种用途。

代理就是真实对象的代表。

1. [AOP的实现机制](http://www.iteye.com/topic/1116696)

## 5. 抽象工厂模式

![image-20220123211922325](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123211922325-c44114.png)

**菜鸟程序员碰到问题，只会用时间来摆平**

工厂方法模式是定义一个用于创建对象的接口，让子类决定实例化哪一个类。

AbstractProductA和AbstractProductB是两个抽象产品，之所以为抽象，是因为它们都有可能有两种不同的实现，就刚才的例子来说就是User和Department，而ProductA1、ProductA2和ProductB1、ProductB2就是对两个抽象产品的具体分类的实现

IFactory是一个抽象工厂接口，它里面应该包含所有的产品创建的抽象方法。而ConcreteFactory1和ConcreteFactory2就是具体的工厂了。

通常是在运行时刻再创建一个ConcreteFactory类的实例，这个具体的工厂再创建具有特定实现的产品对象，也就是说，为创建不同的产品对象，客户端应使用不同的具体工厂。

最大的好处便是易于交换产品系列，由于具体工厂类

在一个应用中只需要在初始化的时候出现一次，这就使得改变一个应用的具体工厂变得非常容易，它只需要改变具体工厂即可使用不同的产品配置。

我们的设计不能去防止需求的更改，那么我们的理想便是让改动变得最小

第二大好处是，它让具体的创建实例过程与客户端分离，客户端是通过它们的抽象接口操纵实例，产品的具体类名也被具体工厂的实现分离，不会出现在客户代码中。

是个模式都是会有缺点的，都有不适用的时候，要辨证地看待问题哦。

编程是门艺术，这样大批量的改动，显然是非常丑陋的做法。

客户端没有出现任何一个SQL Server或Access的字样，达到了解耦的目的。

依赖注入（Dependency Injection）

从这个角度上说，所有在用简单工厂的地方，都可以考虑用反射技术来去除switch或if，解除分支判断带来的耦合。

一个程序员如果从来没有熬夜写程序的经历，不能算是一个好程序员，因为他没有痴迷过，所以他不会有大成就。

**无痴迷，不成功。**

代码 35. abstractfactory/AbstractFactory.java 类

```java
package com.diguage.didp.abstractfactory;

/**
 * AbstractFactory 类
 *
 */
public abstract class AbstractFactory {
  public abstract AbstractProductA createProductA();

  public abstract AbstractProductB createProductB();
}
```

代码 36. abstractfactory/AbstractProductA.java 类

```java
package com.diguage.didp.abstractfactory;

/**
 * AbstractProductA 类
 *
 */
public abstract class AbstractProductA {
}
```

代码 37. abstractfactory/AbstractProductB.java 类

```java
package com.diguage.didp.abstractfactory;

/**
 * AbstractProductB 类
 *
 */
public abstract class AbstractProductB {
}
```

代码 38. abstractfactory/Client.java 类

```java
package com.diguage.didp.abstractfactory;

/**
 * Client 类
 *
 */
public class Client {
  private AbstractFactory factory;
  private AbstractProductA productA;
  private AbstractProductB productB;

  private String type;

  public Client(String type) {
    this.type = type;
    if ("A".equals(this.type) || "a".equals(this.type)) {
      factory = new ConcreteFactory1();
    } else {
      factory = new ConcreteFactory2();
    }
    productA = factory.createProductA();
    productB = factory.createProductB();
  }

  public AbstractFactory getFactory() {
    return factory;
  }

  public AbstractProductA getProductA() {
    return productA;
  }

  public AbstractProductB getProductB() {
    return productB;
  }

  public static void main(String[] args) {
    Client client = new Client("a");
    System.out.println(client.getFactory().getClass());
    System.out.println(client.getProductA().getClass());
    System.out.println(client.getProductB().getClass());
  }
}
```

代码 39. abstractfactory/ConcreteFactory1.java 类

```java
package com.diguage.didp.abstractfactory;

/**
 * ConcreteFactory1 类
 *
 */
public class ConcreteFactory1 extends AbstractFactory {
  public AbstractProductA createProductA() {
    return new ProductA1();
  }

  public AbstractProductB createProductB() {
    return new ProductB1();
  }
}
```

代码 40. abstractfactory/ConcreteFactory2.java 类

```java
package com.diguage.didp.abstractfactory;

/**
 * ConcreteFactory2 类
 *
 */
public class ConcreteFactory2 extends AbstractFactory {
  public AbstractProductA createProductA() {
    return new ProductA2();
  }

  public AbstractProductB createProductB() {
    return new ProductB2();
  }
}
```

代码 41. abstractfactory/ProductA1.java 类

```java
package com.diguage.didp.abstractfactory;

/**
 * ProductA1 类
 *
 */
public class ProductA1 extends AbstractProductA {
}
```

代码 42. abstractfactory/ProductA2.java 类

```java
package com.diguage.didp.abstractfactory;

/**
 * ProductA2 类
 *
 */
public class ProductA2 extends AbstractProductA {
}
```

代码 43. abstractfactory/ProductB1.java 类

```java
package com.diguage.didp.abstractfactory;

/**
 * ProductB1 类
 *
 */
public class ProductB1 extends AbstractProductB {
}
```

代码 44. abstractfactory/ProductB2.java 类

```java
package com.diguage.didp.abstractfactory;

/**
 * ProductB2 类
 *
 */
public class ProductB2 extends AbstractProductB {
}
```

## 6. 适配器模式

![image-20220123212155350](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123212155350-49d74d.png)

简单地说，就是需要的东西就在面前，但却不能使用，而短时间又无法改造它，于是我们就想办法适配它。

系统的数据和行为都正确，但接口不符时，我们应该考虑用适配器，目的是使控制范围之外的一个原有对象与某个接口匹配。适配器模式主要应用于希望复用一些现存的类，但是接口又与复用环境要求不一致的情况

在GoF的设计模式中，对适配器模式讲了两种类型，类适配器模式和对象适配器模式，由于类适配器模式通过多重继承对一个接口与另一个接口进行匹配，而C#、VB.NET、JAVA等语言都不支持多重继承（C++支持），也就是一个类只有一个父类，所以我们这里主要讲的是对象适配器。

Target（这是客户所期待的接口。目标可以是具体的或抽象的类，也可以是接口）

Adaptee（需要适配的类）

Adapter（通过在内部包装一个Adaptee对象，把源接口转换成目标接口）

在想使用一个已经存在的类，但如果它的接口，也就是它的方法和你的要求不相同时，就应该考虑用适配器模式？

两个类所做的事情相同或相似，但是具有不同的接口时要使用它。

客户代码可以统一调用同一接口就行了，这样应该可以更简单、更直接、更紧凑。

其实用适配器模式也是无奈之举，很有点‘亡羊补牢’的感觉

公司内部，类和方法的命名应该有规范，最好前期就设计好，然后如果真的如你所说，接口不相同时，首先不应该考虑用适配器，而是应该考虑通过重构统一接口。

在双方都不太容易修改的时候再使用适配器模式适配，

DataAdapter用作DataSet和数据源之间的适配器以便检索和保存数据。DataAdapter通过映射Fill（这更改了DataSet中的数据以便与数据源中的数据相匹配）和Update（这更改了数据源中的数据以便与DataSet中的数据相匹配）来提供这一适配器[MSDN]。

代码 45. adapter/Adaptee.java 类

```java
package com.diguage.didp.adapter;

/**
 * Adaptee 类
 *
 */
public class Adaptee {
  public void specificRequest() {
    System.out.println("特殊请求！");
  }
}
```

代码 46. adapter/Adapter.java 类

```java
package com.diguage.didp.adapter;

/**
 * Adapter 类
 *
 */
public class Adapter extends Target {
  private Adaptee adaptee = new Adaptee();

  public void request() {
    adaptee.specificRequest();
  }
}
```

代码 47. adapter/Client.java 类

```java
package com.diguage.didp.adapter;

/**
 * Client 类
 *
 */
public class Client {
  public static void main(String[] args) {
    Target target = new Adapter();
    target.request();
  }
}
```

代码 48. adapter/Target.java 类

```java
package com.diguage.didp.adapter;

/**
 * Target 类
 *
 */
public class Target {
  public void request() {
    System.out.println("普通请求");
  }
}
```

## 7. 桥接模式

![image-20220123212304795](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123212304795-b9a44d.png)

什么叫抽象与它的实现分离，这并不是说，让抽象类与其派生类分离，因为这没有任何意义。实现指的是抽象类和它的派生类用来实现自己的对象[DPE]。

桥接模式的核心意图就是把这些实现独立出来，让它们各自地变化。这就使得**每种实现的变化不会影响其他实现，从而达到应对变化的目的。**

Implementor类

ConcreteImplementorA和ConcreteImplementorB等派生类

Abstraction类

RefinedAbstraction类

桥接模式所说的‘将抽象部分与它的实现部分分离’，还是不好理解，我的理解就是实现系统可能有多角度分类，每一种分类都有可能变化，那么就把这种多角度分离出来让它们独立变化，减少它们之间的耦合。

在发现我们需要多角度去分类实现对象，而只用继承会造成大量的类增加，不能满足开放-封闭原则时，就应该要考虑用桥接模式了。

只要真正深入地理解了设计原则，很多设计模式其实就是原则的应用而已，或许在不知不觉中就在使用设计模式了。

代码 49. bridge/Abstraction.java 类

```java
package com.diguage.didp.bridge;

/**
 * Abstraction 类
 *
 */
public abstract class Abstraction {
  protected Implementor implementor;

  public void setImplementor(Implementor implementor) {
    this.implementor = implementor;
  }

  public void operation() {
    implementor.operationImpl();
  }
}
```

代码 50. bridge/ConcreteImplementorA.java 类

```java
package com.diguage.didp.bridge;

/**
 * ConcreteImplementorA 类
 *
 */
public class ConcreteImplementorA extends Implementor {
  @Override
  public void operationImpl() {
    System.out.println("具体实现A的方法执行！");
  }
}
```

代码 51. bridge/ConcreteImplementorB.java 类

```java
package com.diguage.didp.bridge;

/**
 * ConcreteImplementorB 类
 *
 */
public class ConcreteImplementorB extends Implementor {
  @Override
  public void operationImpl() {
    System.out.println("具体实现B的方法执行！");
  }
}
```

代码 52. bridge/Implementor.java 类

```java
package com.diguage.didp.bridge;

/**
 * Implementor 类
 */
public abstract class Implementor {
  public abstract void operationImpl();
}
```

代码 53. bridge/RefinedAbstraction.java 类

```java
package com.diguage.didp.bridge;

/**
 * RefinedAbstraction 类
 *
 */
public class RefinedAbstraction extends Abstraction {
  @Override
  public void operation() {
    implementor.operationImpl();
  }
}
```

代码 54. bridge/Client.java 类

```java
package com.diguage.didp.bridge;


public class Client {
  public static void main(String[] args) {
    Abstraction abstraction = new RefinedAbstraction();
    abstraction.setImplementor(new ConcreteImplementorA());
    abstraction.operation();


    abstraction.setImplementor(new ConcreteImplementorB());
    abstraction.operation();
  }
}
```

## 8. 建造者模式

![image-20220123212530494](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123212530494-9a58a1.png)

如果你需要将一个复杂对象的构建与它的表示分离，使得同样的构建过程可以创建不同的表示的意图时，我们需要应用于一个设计模式，‘建造者（Builder）模式’，又叫生成器模式。建造者模式可以将一个产品的内部表象与产品的生成过程分割开来，从而可以使一个建造过程生成具有不同的内部表象的产品对象。如果我们用了建造者模式，那么用户就只需指定需要建造的类型就可以得到它们，而具体建造的过程和细节就不需知道了。

是为创建一个Product对象的各个部件指定的抽象接口。

它是具体建造者，实现Builder接口，构造和装配各个部件。

它是构建一个使用Builder接口的对象。

它主要是用于创建一些复杂的对象，这些对象内部构建间的建造顺序通常是稳定的，但对象内部的构建通常面临着复杂的变化。

建造者模式的好处就是使得建造代码与表示代码分离，由于建造者隐藏了该产品是如何组装的，所以若需要改变一个产品的内部表示，只需要再定义一个具体的建造者就可以了。

建造者模式是在当创建复杂对象的算法应该独立于该对象的组成部分以及它们的装配方式时适用的模式。

代码 55. builder/Builder.java 类

```java
package com.diguage.didp.builder;

/**
 * Builder 类
 *
 */
public abstract class Builder {
  public abstract void buildPartA();

  public abstract void buildPartB();

  public abstract Product getResult();
}
```

代码 56. builder/ConcreteBuilder.java 类

```java
package com.diguage.didp.builder;

/**
 * ConcreteBuilder 类
 *
 */
public class ConcreteBuilder extends Builder {
  private Product product = new Product();

  @Override
  public void buildPartA() {
    product.add("部件A");
  }

  @Override
  public void buildPartB() {
    product.add("部件B");
  }

  @Override
  public Product getResult() {
    return product;
  }
}
```

代码 57. builder/Director.java 类

```java
package com.diguage.didp.builder;

/**
 * Director 类
 *
 */
public class Director {
  public void construct(Builder builder) {
    builder.buildPartA();
    builder.buildPartB();
  }
}
```

代码 58. builder/Product.java 类

```java
package com.diguage.didp.builder;

import java.util.ArrayList;
import java.util.List;

/**
 * Product 类
 *
 */
public class Product {
  List<String> parts = new ArrayList<String>();

  public void add(String part) {
    parts.add(part);
  }

  public void show() {
    System.out.println("产品创建---");
    parts.forEach(System.out::println);
  }
}
```

代码 59. builder/Client.java 类

```
package com.diguage.didp.builder;


public class Client {
  public static void main(String[] args) {
    Director director = new Director();
    Builder builder = new ConcreteBuilder();
    director.construct(builder);
    Product product = builder.getResult();
    product.show();
  }
}
```

## 9. 职责链模式

![image-20220123212700254](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123212700254-218a10.png)

发出这个请求的客户端并不知道这当中的哪一个对象最终处理这个请求，这样系统的更改可以在不影响客户端的情况下动态地重新组织和分配责任。

Handler类，定义一个处理请示的接口。

ConcreteHandler类，具体处理者类，处理它所负责的请求，可访问它的后继者，如果可处理该请求，就处理之，否则就将该请求转发给它的后继者。

最关键的是当客户提交一个请求时，请求是沿链传递直至有一个ConcreteHandler对象负责处理它。[DP]

这就使得接收者和发送者都没有对方的明确信息，且链中的对象自己也并不知道链的结构。结果是职责链可简化对象的相互连接，它们仅需保持一个指向其后继者的引用，而不需保持它所有的候选接受者的引用[DP]。这也就大大降低了耦合度了。

由于是在客户端来定义链的结构，也就是说，我可以随时地增加或修改处理一个请求的结构。增强了给对象指派职责的灵活性[DP]。

一个请求极有可能到了链的末端都得不到处理，或者因为没有正确配置而得不到处理，这就很糟糕了。需要事先考虑全面。

代码 60. chainofresponsibility/Client.java 类

```java
package com.diguage.didp.chainofresponsibility;

import java.util.Arrays;

/**
 * Client 类
 *
 */
public class Client {
  public static void main(String[] args) {
    Handler h1 = new ConcreteHandler1();
    Handler h2 = new ConcreteHandler2();
    Handler h3 = new ConcreteHandler3();
    h1.setSuccessor(h2);
    h2.setSuccessor(h3);

    int[] requests = new int[]{1, 2, 3, 5, 8, 13, 21};
    Arrays.stream(requests).forEach(h1::handleRequest);
  }
}
```

代码 61. chainofresponsibility/ConcreteHandler1.java 类

```java
package com.diguage.didp.chainofresponsibility;

/**
 * ConcreteHandler1 类
 *
 */
public class ConcreteHandler1 extends Handler {
  @Override
  public void handleRequest(int request) {
    if (request > 0 && request < 10) {
      System.out.printf("%s 处理 %s%n", this.getClass().getName(), request);
    } else if (successor != null) {
      successor.handleRequest(request);
    }
  }
}
```

代码 62. chainofresponsibility/ConcreteHandler2.java 类

```java
package com.diguage.didp.chainofresponsibility;

/**
 * ConcreteHandler2 类
 *
 */
public class ConcreteHandler2 extends Handler {
  @Override
  public void handleRequest(int request) {
    if (request >= 10 && request < 20) {
      System.out.printf("%s 处理 %s%n", this.getClass().getName(), request);
    } else if (successor != null) {
      successor.handleRequest(request);
    }
  }
}
```

代码 63. chainofresponsibility/Handler.java 类

```java
package com.diguage.didp.chainofresponsibility;

/**
 * Handler 类
 *
 */
public abstract class Handler {
  protected Handler successor;

  public void setSuccessor(Handler successor) {
    this.successor = successor;
  }

  public abstract void handleRequest(int request);
}
```

代码 64. chainofresponsibility/ConcreteHandler3.java 类

```java
package com.diguage.didp.chainofresponsibility;

/**
 * ConcreteHandler2 类
 *
 */
public class ConcreteHandler3 extends Handler {
  @Override
  public void handleRequest(int request) {
    if (request >= 20 && request < 30) {
      System.out.printf("%s 处理 %s%n", this.getClass().getName(), request);
    } else if (successor != null) {
      successor.handleRequest(request);
    }
  }
}
```

## 10. 命令模式

![image-20220123212819434](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123212819434-399211.png)

这其实就是‘行为请求者’与‘行为实现者’的紧耦合。

对请求排队或记录请求日志，以及支持可撤销的操作等行为时，‘行为请求者’与‘行为实现者’的紧耦合是不太适合的。

Command类，用来声明执行操作的接口。

ConcreteCommand类，将一个接收者对象绑定于一个动作，调用接收者相应的操作，以实现Execute。

Invoker类，要求该命令执行这个请求。

Receiver类，知道如何实施与执行一个与请求相关的操作，任何类都可能作为一个接收者。

客户端代码，创建一个具体命令对象并设定它的接收者。

命令模式的优点。

第一，它能较容易地设计一个命令队列；第二，在需要的情况下，可以较容易地将命令记入日志；第三，允许接收请求的一方决定是否要否决请求。

第四，可以容易地实现对请求的撤销和重做；第五，由于加进新的具体命令类不影响其他的类，因此增加新的具体命令类很容易。其实还有最关键的优点就是命令模式把请求一个操作的对象与知道怎么执行一个操作的对象分割开。[DP]

比如命令模式支持撤销/恢复操作功能，但你还不清楚是否需要这个功能时，你要不要实现命令模式？

敏捷开发原则告诉我们，不要为代码添加基于猜测的、实际不需要的功能。如果不清楚一个系统是否需要命令模式，一般就不要着急去实现它，事实上，在需要的时候通过重构实现这个模式并不困难，只有在真正需要如撤销/恢复操作等功能时，把原来的代码重构为命令模式才有意义。[R2P]

代码 65. command/Client.java 类

```java
package com.diguage.didp.command;

/**
 * Client 类
 *
 */
public class Client {
  public static void main(String[] args) {
    Receiver receiver = new Receiver();
    Command command = new ConcreteCommand(receiver);
    Invoker invoker = new Invoker();
    invoker.setCommand(command);
    invoker.executeCommand();
  }
}
```

代码 66. command/Command.java 类

```java
package com.diguage.didp.command;

/**
 * Command 类
 *
 */
public abstract class Command {
  protected Receiver receiver;

  public Command(Receiver receiver) {
    this.receiver = receiver;
  }

  public abstract void execute();
}
```

代码 67. command/ConcreteCommand.java 类

```java 
package com.diguage.didp.command;

/**
 * ConcreteCommand 类
 *
 */
public class ConcreteCommand extends Command {
  public ConcreteCommand(Receiver receiver) {
    super(receiver);
  }

  public void execute() {
    receiver.action();
  }
}
```

代码 68. command/Invoker.java 类

```java
package com.diguage.didp.command;

/**
 * Invoker 类
 *
 */
public class Invoker {
  private Command command;

  public void setCommand(Command command) {
    this.command = command;
  }

  public void executeCommand() {
    command.execute();
  }
}
```

代码 69. command/Receiver.java 类

```java
package com.diguage.didp.command;

/**
 * Receiver 类
 *
 */
public class Receiver {
  public void action() {
    System.out.printf("执行请求！");
  }
}
```

## 11. 组合模式

![image-20220123212942907](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123212942907-01064e.png)

组合模式 注: Netty 中 CompositeByteBuf 就是组合模式的实现。

Component为组合中的对象声明接口，在适当情况下，实现所有类共有接口的默认行为。声明一个接口用于访问和管理Component的子部件。

Leaf在组合中表示叶节点对象，叶节点没有子节点。

Composite定义有枝节点行为，用来存储子部件，在Component接口中实现与子部件有关的操作，比如增加Add和删除Remove。

客户端代码，能通过Component接口操作组合部件的对象。

透明方式，也就是说在Component中声明所有用来管理子对象的方法，其中包括Add、Remove等。这样实现Component接口的所有子类都具备了Add和Remove。这样做的好处就是叶节点和枝节点对于外界没有区别，它们具备完全一致的行为接口。但问题也很明显，因为Leaf类本身不具备Add()、Remove()方法的功能，所以实现它是没有意义的。

安全方式，也就是在Component接口中不去声明Add和Remove方法，那么子类的Leaf也就不需要去实现它，而是在Composite声明所有用来管理子类对象的方法，这样做就不会出现刚才提到的问题，不过由于不够透明，所以树叶和树枝类将不具有相同的接口，客户端的调用需要做相应的判断，带来了不便。

当你发现需求中是体现部分与整体层次的结构时，以及你希望用户可以忽略组合对象与单个对象的不同，统一地使用组合结构中的所有对象时，就应该考虑用组合模式了

组合模式这样就定义了包含人力资源部和财务部这些基本对象和分公司、办事处等组合对象的类层次结构。基本对象可以被组合成更复杂的组合对象，而这个组合对象又可以被组合，这样不断地递归下去，客户代码中，任何用到基本对象的地方都可以使用组合对象了。

用户是不用关心到底是处理一个叶节点还是处理一个组合组件，也就用不着为定义组合而写一些选择判断语句了。

组合模式让客户可以一致地使用组合结构和单个对象。

代码 70. composite/Client.java 类

```java
package com.diguage.didp.composite;

/**
 * Client 类
 *
 */
public class Client {
  public static void main(String[] args) {
    Composite root = new Composite("root");
    root.add(new Leaf("Leaf A"));
    root.add(new Leaf("Leaf B"));

    Composite comp = new Composite("Composite X");
    comp.add(new Leaf("Leaf XA"));
    comp.add(new Leaf("Leaf XB"));

    root.add(comp);

    Composite comp2 = new Composite("Composite XY");
    comp2.add(new Leaf("Leaf XYA"));
    comp2.add(new Leaf("Leaf XYB"));
    comp.add(comp2);

    root.add(new Leaf("Leaf C"));

    Leaf leaf = new Leaf("Leaf D");
    root.add(leaf);
    root.remove(leaf);

    root.display(1);
  }
}
```

代码 71. composite/Component.java 类

```java
package com.diguage.didp.composite;

/**
 * Component 类
 *
 */
public abstract class Component {
  protected String name;

  public Component(String name) {
    this.name = name;
  }

  public abstract void add(Component component);

  public abstract void remove(Component component);

  public abstract void display(int depth);
}

```

代码 72. composite/Composite.java 类

```java
package com.diguage.didp.composite;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite 类
 *
 */
public class Composite extends Component {
  private List<Component> children = new ArrayList<>();

  public Composite(String name) {
    super(name);
  }

  @Override
  public void add(Component component) {
    children.add(component);
  }

  @Override
  public void remove(Component component) {
    children.remove(component);
  }

  @Override
  public void display(int depth) {
    StringBuilder sb = new StringBuilder(depth + name.length());
    for (int i = 0; i < depth; i++) {
      sb.append("-");
    }
    System.out.println(sb.append(name).toString());

    children.forEach(s -> s.display(depth + 2));
  }
}
```

代码 73. composite/Leaf.java 类

```java
package com.diguage.didp.composite;

/**
 * Leaf 类
 *
 */
public class Leaf extends Component {
  public Leaf(String name) {
    super(name);
  }

  @Override
  public void add(Component component) {
    throw new UnsupportedOperationException("不支持向叶子中添加元素！");
  }

  @Override
  public void remove(Component component) {
    throw new UnsupportedOperationException("不支持向叶子中删除元素！");
  }

  @Override
  public void display(int depth) {
    StringBuilder sb = new StringBuilder(depth + name.length());
    for (int i = 0; i < depth; i++) {
      sb.append("-");
    }
    System.out.println(sb.append(name).toString());
  }
}
```

## 12. 装饰器模式

![image-20220123213054755](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123213054755-4c7d29.png)

建造者模式要求建造的过程必须是稳定的

我们需要把所需的功能按正确的顺序串联起来进行控制

Component是定义一个对象接口，可以给这些对象动态地添加职责。ConcreteComponent是定义了一个具体的对象，也可以给这个对象添加一些职责。Decorator，装饰抽象类，继承了Component，从外类来扩展Component类的功能，但对于Component来说，是无需知道Decorator的存在的。至于ConcreteDecorator就是具体的装饰对象，起到给Component添加职责的功能[DPE]。

装饰模式是利用SetComponent来对对象进行包装的。这样每个装饰对象的实现就和如何使用这个对象分离开了，每个装饰对象只关心自己的功能，不需要关心如何被添加到对象链当中[DPE]。

学习模式要善于变通，如果只有一个ConcreteComponent类而没有抽象的Component类，那么Decorator类可以是ConcreteComponent的一个子类。同样道理，如果只有一个ConcreteDecorator类，那么就没有必要建立一个单独的Decorator类，而可以把Decorator和ConcreteDecorator的责任合并成一个类。

装饰模式是为已有功能动态地添加更多功能的一种方式。

当系统需要新功能的时候，是向旧的类中添加新的代码。这些新加的代码通常装饰了原有类的核心职责或主要行为，

在主类中加入了新的字段，新的方法和新的逻辑，从而增加了主类的复杂度，就像你起初的那个‘人’类，而这些新加入的东西仅仅是为了满足一些只在某种特定情况下才会执行的特殊行为的需要。而装饰模式却提供了一个非常好的解决方案，它把每个要装饰的功能放在单独的类中，并让这个类包装它所要装饰的对象，因此，当需要执行特殊行为时，客户代码就可以在运行时根据需要有选择地、按顺序地使用装饰功能包装对象了[DP]。

装饰模式的优点我总结下来就是，把类中的装饰功能从类中搬移去除，这样可以简化原有的类。

这样做更大的好处就是有效地把类的核心职责和装饰功能区分开了。而且可以去除相关类中重复的装饰逻辑。

装饰模式的装饰顺序很重要

最理想的情况，是保证装饰类之间彼此独立，这样它们就可以以任意的顺序进行组合了。

代码 74. decorator/Client.java 类

```java
package com.diguage.didp.decorator;

/**
 * Client 类
 *
 */
public class Client {
  public static void main(String[] args) {
    Component component = new ConcreteComponent();
    Decorator decoratorA = new ConcreteDecoratorA();
    Decorator decoratorB = new ConcreteDecoratorB();
    decoratorA.setComponent(component);
    decoratorB.setComponent(decoratorA);
    decoratorB.operation();
  }
}
```

代码 75. decorator/Component.java 类

```java
package com.diguage.didp.decorator;

/**
 * Component 类
 *
 */
public abstract class Component {
  public abstract void operation();
}
```

代码 76. decorator/ConcreteComponent.java 类

```java
package com.diguage.didp.decorator;

/**
 * ConcreteComponent 类
 *
 */
public class ConcreteComponent extends Component {
  public void operation() {
  }
}
```

代码 77. decorator/ConcreteDecoratorA.java 类

```java
package com.diguage.didp.decorator;

/**
 * 具体装饰 A 类
 *
 */
public class ConcreteDecoratorA extends Decorator {

  private String addedState;

  @Override
  public void operation() {
    super.operation();
    addedState = "区别于其他装饰对象的状态值！";
    System.out.println("具体装饰对象 A 的操作");
  }
}
```

代码 78. decorator/ConcreteDecoratorB.java 类

```java
package com.diguage.didp.decorator;

/**
 * 具体装饰 B 类
 *
 */
public class ConcreteDecoratorB extends Decorator {

  private String addedState;

  @Override
  public void operation() {
    super.operation();
    addedState = "区别于其他装饰对象的状态值！";
    addBehavior();
  }

  private void addBehavior() {
    System.out.println("附加的装饰动作！");
  }
}
```

代码 79. decorator/Decorator.java 类

```java
package com.diguage.didp.decorator;


public class Decorator extends Component {
  protected Component component;

  public void operation() {
    if (component != null) {
      component.operation();
    }
  }

  public void setComponent(Component component) {
    this.component = component;
  }
}
```

## 13. 外观模式

![image-20220123213228995](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123213228995-fa3ad7.png)

外观模式，也完全有可能在很多时候使用它，因为它完美地体现了依赖倒转原则和迪米特法则的思想，所以是非常常用的模式之一。

其次，在开发阶段，子系统往往因为不断的重构演化而变得越来越复杂，

增加外观Facade可以提供一个简单的接口，减少它们之间的依赖。第三，在维护一个遗留的大型系统时，可能这个系统已经非常难以维护和扩展了，

为新系统开发一个外观Facade类，来提供设计粗糙或高度复杂的遗留代码的比较清晰简单的接口，让新系统与Facade对象交互，Facade与遗留代码交互所有复杂的工作。[R2P]

依赖倒转原则？抽象不应该依赖细节，细节应该依赖于抽象，由于我们要吃的菜都依赖于厨师这样的细节，所以我们就很被动。

代码 80. facade/Client.java 类

```java
package com.diguage.didp.facade;

/**
 * Client 类
 *
 */
public class Client {
  public static void main(String[] args) {
    Facade facade = new Facade();
    facade.methodA();
    facade.methodB();
  }
}
```

代码 81. facade/Facade.java 类

```java
package com.diguage.didp.facade;

/**
 * Facade 类
 *
 */
public class Facade {
  private SubSystemOne systemOne;
  private SubSystemTwo systemTwo;
  private SubSystemThree systemThree;
  private SubSystemFour systemFour;

  public Facade() {
    systemOne = new SubSystemOne();
    systemTwo = new SubSystemTwo();
    systemThree = new SubSystemThree();
    systemFour = new SubSystemFour();
  }

  public void methodA() {
    System.out.println("方法A() ---");
    systemOne.methodOne();
    systemTwo.methodTwo();
  }

  public void methodB() {
    System.out.println("方法B() ---");
    systemFour.methodFour();
    systemThree.methodThree();
  }
}
```

代码 82. facade/SubSystemFour.java 类

```java
package com.diguage.didp.facade;

/**
 * SubSystemFour 类
 *
 */
public class SubSystemFour {
  public void methodFour() {
    System.out.println("子系统四的方法");
  }
}
```

代码 83. facade/SubSystemOne.java 类

```java
package com.diguage.didp.facade;

/**
 * SubSystemOne 类
 *
 */
public class SubSystemOne {
  public void methodOne() {
    System.out.println("子系统一的方法");
  }
}
```

代码 84. facade/SubSystemThree.java 类

```java
package com.diguage.didp.facade;

/**
 * SubSystemThree 类
 *
 */
public class SubSystemThree {
  public void methodThree() {
    System.out.println("子系统三的方法");
  }
}
```

代码 85. facade/SubSystemTwo.java 类

```java
package com.diguage.didp.facade;

/**
 * SubSystemTwo 类
 *
 */
public class SubSystemTwo {
  public void methodTwo() {
    System.out.println("子系统二的方法");
  }
}
```

## 14. 工厂方法模式

![image-20220123213358983](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123213358983-3227a7.png)

简单工厂模式的最大优点在于工厂类中包含了必要的逻辑判断，根据客户端的选择条件动态实例化相关的类，对于客户端来说，去除了与具体产品的依赖。

既然这个工厂类与分支耦合，那么我就对它下手，根据依赖倒转原则，我们把工厂类抽象出一个接口，这个接口只有一个方法，就是创建抽象产品的工厂方法。然后，所有的要生产具体类的工厂，就去实现这个接口，这样，一个简单工厂模式的工厂类，变成了一个工厂抽象接口和多个具体生成对象的工厂，

工厂方法模式实现时，客户端需要决定实例化哪一个工厂来实现运算类，选择判断的问题还是存在的，也就是说，工厂方法把简单工厂的内部逻辑判断移到了客户端代码来进行。你想要加功能，本来是改工厂类的，而现在是修改客户端！

工厂方法克服了简单工厂违背开放-封闭原则的缺点，又保持了封装对象创建过程的优点。

它们都是集中封装了对象的创建，使得要更换对象时，不需要做大的改动就可实现，降低了客户程序与产品对象的耦合。工厂方法模式是简单工厂模式的进一步抽象和推广。由于使用了多态性，工厂方法模式保持了简单工厂模式的优点，而且克服了它的缺点。但缺点是由于每加一个产品，就需要加一个产品工厂的类，增加了额外的开发量。

利用‘反射’可以解决避免分支判断的问题。

代码 86. factorymethod/ConcreteFactory.java 类

```java
package com.diguage.didp.factorymethod;

/**
 * ConcreteFactory 类
 *
 */
public class ConcreteFactory extends Factory {
  public Product factoryMethod() {
    return new ConcreteProduct();
  }
}
```

代码 87. factorymethod/ConcreteProduct.java 类

```java
package com.diguage.didp.factorymethod;

/**
 * ConcreteProduct 类
 *
 */
public class ConcreteProduct extends Product {
}
```

代码 88. factorymethod/Factory.java 类

```java
package com.diguage.didp.factorymethod;

/**
 * Factory 类
 *
 */
public abstract class Factory {
  public abstract Product factoryMethod();
}
```

代码 89. factorymethod/Product.java 类

```java
package com.diguage.didp.factorymethod;

/**
 * Product 类
 *
 */
public abstract class Product {
}
```

代码 90. factorymethod/Client.java 类

```java
package com.diguage.didp.factorymethod;


public class Client {
  public static void main(String[] args) {
    Factory factory = new ConcreteFactory();
    Product product = factory.factoryMethod();
    System.out.println(product.getClass().getName());
  }
}
```

## 15. 享元模式

![image-20220123213615050](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123213615050-8a50f9.png)

Flyweight类，它是所有具体享元类的超类或接口，通过这个接口，Flyweight可以接受并作用于外部状态。

ConcreteFlyweight是继承Flyweight超类或实现Flyweight接口，并为内部状态增加存储空间。

UnsharedConcreteFlyweight是指那些不需要共享的Flyweight子类。因为Flyweight接口共享成为可能，但它并不强制共享。

FlyweightFactory，是一个享元工厂，用来创建并管理Flyweight对象。它主要是用来确保合理地共享Flyweight，当用户请求一个Flyweight时，FlyweightFactory对象提供一个已创建的实例或者创建一个（如果不存在的话）。

尽管我们大部分时间都需要共享对象来降低内存的损耗，但个别时候也有可能不需要共享的，那么此时的UnsharedConcreteFlyweight子类就有存在的必要了，它可以解决那些不需要共享对象的问题。

在享元对象内部并且不会随环境改变而改变的共享部分，可以称为是享元对象的内部状态，而随环境改变而改变的、不可以共享的状态就是外部状态了。事实上，享元模式可以避免大量非常相似类的开销。在程序设计中，有时需要生成大量细粒度的类实例来表示数据。如果能发现这些实例除了几个参数外基本上都是相同的，有时就能够受大幅度地减少需要实例化的类的数量。如果能把那些参数移到类实例的外面，在方法调用时将它们传递进来，就可以通过共享大幅度地减少单个实例的数目。也就是说，享元模式Flyweight执行时所需的状态是有内部的也可能有外部的，内部状态存储于ConcreteFlyweight对象之中，而外部对象则应该考虑由客户端对象存储或计算，当调用Flyweight对象的操作时，将该状态传递给它。

如果一个应用程序使用了大量的对象，而大量的这些对象造成了很大的存储开销时就应该考虑使用；还有就是对象的大多数状态可以外部状态，如果删除对象的外部状态，那么可以用相对较少的共享对象取代很多组对象，此时可以考虑使用享元模式。

用了享元模式，所以有了共享对象，实例总数就大大减少了，如果共享的对象越多，存储节约也就越多，节约量随着共享状态的增多而增大。

实际上在.NET中，字符串string就是运用了Flyweight模式。举个例子吧。Object.ReferenceEquals（object objA,object objB）方法是用来确定objA与objB是否是相同的实例，返回值为bool值

享元模式更多的时候是一种底层的设计模式

在某些情况下，对象的数量可能会太多，从而导致了运行时的资源与性能损耗。那么我们如何去避免大量细粒度的对象，同时又不影响客户程序，是一个值得去思考的问题，享元模式，可以运用共享技术有效地支持大量细粒度的对象。

使用享元模式需要维护一个记录了系统已有的所有享元的列表，而这本身需要耗费资源，另外享元模式使得系统更加复杂。为了使对象可以共享，需要将一些状态外部化，这使得程序的逻辑复杂化。因此，应当在有足够多的对象实例可供共享时才值得使用享元模式。

代码 91. flyweight/Client.java 类

```java
package com.diguage.didp.flyweight;

/**
 * Client 类
 *
 */
public class Client {
  public static void main(String[] args) {
    int extrinsticState = 22;
    FlyweightFactory factory = new FlyweightFactory();

    FlyWeight fx = factory.getFlyWeight("X");
    fx.operation(--extrinsticState);

    FlyWeight fy = factory.getFlyWeight("Y");
    fy.operation(--extrinsticState);

    FlyWeight fz = factory.getFlyWeight("Z");
    fz.operation(--extrinsticState);

    UnsharedConcreteFlyWeight uf = new UnsharedConcreteFlyWeight();
    uf.operation(--extrinsticState);
  }
}
```

代码 92. flyweight/ConcreteFlyWeight.java 类

```java
package com.diguage.didp.flyweight;

/**
 * ConcreteFlyWeight 类
 *
 */
public class ConcreteFlyWeight extends FlyWeight {
  @Override
  public void operation(int extrinsticState) {
    System.out.println("具体 FlyWeight：" + extrinsticState);
  }
}
```

代码 93. flyweight/FlyWeight.java 类

```java
package com.diguage.didp.flyweight;

/**
 * FlyWeight 类
 *
 */
public abstract class FlyWeight {
  public abstract void operation(int extrinsticState);
}
```

代码 94. flyweight/FlyweightFactory.java 类

```java
package com.diguage.didp.flyweight;

import java.util.HashMap;
import java.util.Map;

/**
 * FlyweightFactory 类
 *
 */
public class FlyweightFactory {
  private Map<String, FlyWeight> flyWeights = new HashMap<>();

  public FlyweightFactory() {
    flyWeights.put("X", new ConcreteFlyWeight());
    flyWeights.put("Y", new ConcreteFlyWeight());
    flyWeights.put("Z", new ConcreteFlyWeight());
  }

  public FlyWeight getFlyWeight(String key) {
    return flyWeights.get(key);
  }
}
```

代码 95. flyweight/UnsharedConcreteFlyWeight.java 类

```java
package com.diguage.didp.flyweight;

/**
 * UnsharedConcreteFlyWeight 类
 *
 */
public class UnsharedConcreteFlyWeight extends FlyWeight {
  @Override
  public void operation(int extrinsticState) {
    System.out.println("不共享的具体 FlyWeight：" + extrinsticState);
  }
}
```

## 16. 解释器模式



![image-20220123232824470](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123232824470-73575f.png)

解释器模式需要解决的是，如果一种特定类型的问题发生的频率足够高，那么可能就值得将该问题的各个实例表述为一个简单语言中的句子。这样就可以构建一个解释器，该解释器通过解释这些句子来解决该问题[DP]。

因为这个匹配字符的需求在软件的很多地方都会使用，而且行为之间都非常类似，过去的做法是针对特定的需求，编写特定的函数，比如判断Email、匹配电话号码等等，与其为每一个特定需求都写一个算法函数，不如使用一种通用的搜索算法来解释执行一个正则表达式，该正则表达式定义了待匹配字符串的集合[DP]。而所谓的解释器模式，正则表达式就是它的一种应用，解释器为正则表达式定义了一个文法，如何表示一个特定的正则表达式，以及如何解释这个正则表达式。

AbstractExpression（抽象表达式），声明一个抽象的解释操作，这个接口为抽象语法树中所有的节点所共享。

TerminalExpression（终结符表达式），实现与文法中的终结符相关联的解释操作。实现抽象表达式中所要求的接口，主要是一个interpret()方法。文法中每一个终结符都有一个具体终结表达式与之相对应。

NonterminalExpression（非终结符表达式），为文法中的非终结符实现解释操作。对文法中每一条规则R1、R2……Rn都需要一个具体的非终结符表达式类。通过实现抽象表达式的interpret()方法实现解释操作。解释操作以递归方式调用上面所提到的代表R1、R2……Rn中各个符号的实例变量。

Context，包含解释器之外的一些全局信息。

客户端代码，构建表示该文法定义的语言中一个特定的句子的抽象语法树。调用解释操作。

代码 96. interpreter/AbstractExpression.java 类

```java
package com.diguage.didp.interpreter;

/**
 * AbstractExpression 类
 *
 */
public abstract class AbstractExpression {
  public abstract void interpret(Context context);
}
```

代码 97. interpreter/Client.java 类

```java
package com.diguage.didp.interpreter;

import java.util.ArrayList;
import java.util.List;

/**
 * Client 类
 *
 */
public class Client {
  public static void main(String[] args) {
    Context context = new Context();
    List<AbstractExpression> expressions = new ArrayList<>();
    expressions.add(new TerminalExpression());
    expressions.add(new NonterminalExpression());
    expressions.add(new TerminalExpression());
    expressions.add(new TerminalExpression());

    expressions.forEach(e -> e.interpret(context));
  }
}
```

代码 98. interpreter/Context.java 类

```java 
package com.diguage.didp.interpreter;

/**
 * Context 类
 *
 */
public class Context {
  private String input;
  private String output;

  public String getInput() {
    return input;
  }

  public void setInput(String input) {
    this.input = input;
  }

  public String getOutput() {
    return output;
  }

  public void setOutput(String output) {
    this.output = output;
  }
}
```

代码 99. interpreter/NonterminalExpression.java 类

```java
package com.diguage.didp.interpreter;

/**
 * NonterminalExpression 类
 *
 */
public class NonterminalExpression extends AbstractExpression {
  @Override
  public void interpret(Context context) {
    System.out.println("非终端解释器！");
  }
}
```

代码 100. interpreter/TerminalExpression.java 类

```java
package com.diguage.didp.interpreter;

/**
 * TerminalExpression 类
 *
 */
public class TerminalExpression extends AbstractExpression {
  @Override
  public void interpret(Context context) {
    System.out.println("终端解释器！");
  }
}
```

## 17. 迭代器模式

![image-20220123233008125](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123233008125-c97773.png)



迭代器模式 注: 结合 Java 中 Iterator 和 Iteratable ，具体分析一下迭代器在 Java 中的实现。另外，重点看看内迭代器和外迭代器之间的不同实现。

迭代器模式。

当你需要访问一个聚集对象，而且不管这些对象是什么都需要遍历的时候，你就应该考虑用迭代器模式。

你需要对聚集有多种方式遍历时，可以考虑用迭代器模式。

为遍历不同的聚集结构提供如开始、下一个、是否结束、当前哪一项等统一的接口。

像IEnumerable接口也是为迭代器模式而准备的。不管如何，学习一下GoF的迭代器模式的基本结构，还是很有学习价值的。研究历史是为了更好地迎接未来。

Iterator迭代器抽象类

Aggregate聚集抽象类

ConcreteIterator具体迭代器类，继承Iterator

ConcreteAggregate具体聚集类继承Aggregate

当你需要对聚集有多种方式遍历时，可以考虑用迭代器模式

尽管我们不需要显式的引用迭代器，但系统本身还是通过迭代器来实现遍历的。总地来说，迭代器（Iterator）模式就是分离了集合对象的遍历行为，抽象出一个迭代器类来负责，这样既可以做到不暴露集合的内部结构，又可让外部代码透明地访问集合内部的数据。

代码 101. iterator/Aggregate.java 类

```java
package com.diguage.didp.iterator;

/**
 * Aggregate 类
 *
 */
public abstract class Aggregate {
  public abstract Iterator createIterator();
}
```

代码 102. iterator/Client.java 类

```java
package com.diguage.didp.iterator;

/**
 * Client 类
 *
 */
public class Client {
  public static void main(String[] args) {
    ConcreteAggregate aggregate = new ConcreteAggregate();
    int i = 0;
    aggregate.set(i++, "大鸟");
    aggregate.set(i++, "小菜");
    aggregate.set(i++, "行李");
    aggregate.set(i++, "老外");
    aggregate.set(i++, "公交内部员工");
    aggregate.set(i++, "小偷");

    Iterator iterator = new ConcreteIterator(aggregate);
    Object first = iterator.first();
    while (!iterator.isDone()) {
      System.out.printf("%s 请买车票！", iterator.currentItem());
      iterator.next();
    }
  }
}
```

代码 103. iterator/ConcreteAggregate.java 类

```java
package com.diguage.didp.iterator;

import java.util.ArrayList;
import java.util.List;

/**
 * ConcreteAggregate 类
 *
 */
public class ConcreteAggregate extends Aggregate {
  private List<Object> items = new ArrayList<>();

  public Iterator createIterator() {
    return new ConcreteIterator(this);
  }

  public Object get(int index) {
    return items.get(index);
  }

  public void set(int index, Object item) {
    items.set(index, item);
  }

  public int count() {
    return items.size();
  }
}
```

代码 104. iterator/ConcreteIterator.java 类

```java
package com.diguage.didp.iterator;

/**
 * ConcreteIterator 类
 *
 */
public class ConcreteIterator extends Iterator {
  private ConcreteAggregate aggregate;
  private int current = 0;

  public ConcreteIterator(ConcreteAggregate aggregate) {
    this.aggregate = aggregate;
  }

  @Override
  public Object first() {
    return aggregate.get(0);
  }

  @Override
  public Object next() {
    Object ret = null;
    current++;
    if (current < aggregate.count()) {
      ret = aggregate.get(current);
    }
    return ret;
  }

  @Override
  public boolean isDone() {
    return current >= aggregate.count();
  }

  @Override
  public Object currentItem() {
    return aggregate.get(current);
  }
}
```

代码 105. iterator/Iterator.java 类

```java 
package com.diguage.didp.iterator;

/**
 * Iterator 类
 *
 */
public abstract class Iterator {
  public abstract Object first();

  public abstract Object next();

  public abstract boolean isDone();

  public abstract Object currentItem();
}
```

## 18. 中介者模式



![image-20220123233144844](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123233144844-da1b62.png)

中介者模式又叫做调停者模式。其实就是中间人或者调停者的意思。

尽管将一个系统分割成许多对象通常可以增加其可复用性，但是对象间相互连接的激增又会降低其可复用性了。

大量的连接使得一个对象不可能在没有其他对象的支持下工作，系统表现为一个不可分割的整体，所以，对系统的行为进行任何较大的改动就十分困难了。

‘迪米特法则’，如果两个类不必彼此直接通信，那么这两个类就不应当发生直接的相互作用。如果其中一个类需要调用另一个类的某一个方法的话，可以通过第三者转发这个调用。

通过中介者对象，可以将系统的网状结构变成以中介者为中心的星形结构，每个具体对象不再通过直接的联系与另一个对象发生相互作用，而是通过‘中介者’对象与另一个对象发生相互作用。中介者对象的设计，使得系统的结构不会因为新对象的引入造成大量的修改工作。

Colleague叫做抽象同事类，而ConcreteColleague是具体同事类，每个具体同事只知道自己的行为，而不了解其他同事类的情况，但它们却都认识中介者对象，Mediator是抽象中介者，定义了同事对象到中介者对象的接口，ConcreteMediator是具体中介者对象，实现抽象类的方法，它需要知道所有具体同事类，并从具体同事接收消息，向具体同事对象发出命令。

Mediator类抽象中介者类

Colleague类抽象同事类

ConcreteMediator类具体中介者类

由于有了Mediator，使得ConcreteColleague1和ConcreteColleague2在发送消息和接收信息时其实是通过中介者来完成的，这就减少了它们之间的耦合度了。

中介者模式很容易在系统中应用，也很容易在系统中误用。当系统出现了‘多对多’交互复杂的对象群时，不要急于使用中介者模式，而要先反思你的系统在设计上是不是合理。

中介者模式的优点首先是Mediator的出现减少了各个Colleague的耦合，使得可以独立地改变和复用各个Colleague类和Mediator，

其次，由于把对象如何协作进行了抽象，将中介作为一个独立的概念并将其封装在一个对象中，这样关注的对象就从对象各自本身的行为转移到它们之间的交互上来，也就是站在一个更宏观的角度去看待系统。

由于ConcreteMediator控制了集中化，于是就把交互复杂性变为了中介者的复杂性，这就使得中介者会变得比任何一个ConcreteColleague都复杂。

中介者模式的优点来自集中控制，其缺点也是它，使用时是要考虑清楚哦。

中介者模式一般应用于一组对象以定义良好但是复杂的方式进行通信的场合，比如刚才得到的窗体Form对象或Web页面aspx，以及想定制一个分布在多个类中的行为，而又不想生成太多的子类的场合。

代码 106. mediator/Colleague.java 类

```java
package com.diguage.didp.mediator;

/**
 * Colleague 类
 *
 */
public abstract class Colleague {
  protected Mediator mediator;

  public Colleague(Mediator mediator) {
    this.mediator = mediator;
  }

  public abstract void notify(String message);

  public void send(String message) {
    mediator.send(message, this);
  }
}
```

代码 107. mediator/ConcreteColleague1.java 类

```java
package com.diguage.didp.mediator;

/**
 * ConcreteColleague1 类
 *
 */
public class ConcreteColleague1 extends Colleague {
  public ConcreteColleague1(Mediator mediator) {
    super(mediator);
  }

  @Override
  public void notify(String message) {
    System.out.println("同事1得到消息：" + message);
  }
}
```

代码 108. mediator/ConcreteColleague2.java 类

```java
package com.diguage.didp.mediator;

/**
 * ConcreteColleague2 类
 *
 */
public class ConcreteColleague2 extends Colleague {
  public ConcreteColleague2(Mediator mediator) {
    super(mediator);
  }

  @Override
  public void notify(String message) {
    System.out.println("同事2得到消息：" + message);
  }
}
```

代码 109. mediator/ConcreteMediator.java 类

```java
package com.diguage.didp.mediator;

/**
 * ConcreteMediator 类
 *
 */
public class ConcreteMediator extends Mediator {
  private ConcreteColleague1 colleague1;
  private ConcreteColleague2 colleague2;

  public void setColleague1(ConcreteColleague1 colleague1) {
    this.colleague1 = colleague1;
  }

  public void setColleague2(ConcreteColleague2 colleague2) {
    this.colleague2 = colleague2;
  }

  @Override
  public void send(String message, Colleague colleague) {
    if (colleague == colleague1) {
      colleague2.notify(message);
    } else {
      colleague1.notify(message);
    }
  }
}
```

代码 110. mediator/Mediator.java 类

```java
package com.diguage.didp.mediator;

/**
 * Mediator 类
 *
 */
public abstract class Mediator {
  public abstract void send(String message, Colleague colleague);
}
```

代码 111. mediator/Client.java 类

```java
package com.diguage.didp.mediator;


public class Client {
  public static void main(String[] args) {
    ConcreteMediator mediator = new ConcreteMediator();

    ConcreteColleague1 colleague1 = new ConcreteColleague1(mediator);
    ConcreteColleague2 colleague2 = new ConcreteColleague2(mediator);

    mediator.setColleague1(colleague1);
    mediator.setColleague2(colleague2);

    colleague1.send("吃过饭了吗？");
    colleague2.send("没有呢，你打算请客？");
  }
}
```

## 19. 备忘录模式

![image-20220123233344238](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123233344238-2fd3d1.png)

**代码无错未必优。**

Originator（发起人）：负责创建一个备忘录Memento，用以记录当前时刻它的内部状态，并可使用备忘录恢复内部状态。Originator可根据需要决定Memento存储Originator的哪些内部状态。

Memento（备忘录）：负责存储Originator对象的内部状态，并可防止Originator以外的其他对象访问备忘录Memento。备忘录有两个接口，Caretaker只能看到备忘录的窄接口，它只能将备忘录传递给其他对象。Originator能够看到一个宽接口，允许它访问返回到先前状态所需的所有数据。

Caretaker（管理者）：负责保存好备忘录Memento，不能对备忘录的内容进行操作或检查。 “

要保存的细节给封装在了Memento中了，哪一天要更改保存的细节也不用影响客户端了。

Memento模式比较适用于功能比较复杂的，但需要维护或记录属性历史的类，或者需要保存的属性只是众多属性中的一小部分时，Originator可以根据保存的Memento信息还原到前一状态。

如果在某个系统中使用命令模式时，需要实现命令的撤销功能，那么命令模式可以使用备忘录模式来存储可撤销操作的状态[DP]

有时一些对象的内部信息必须保存在对象以外的地方，但是必须要由对象自己读取，这时，使用备忘录可以把复杂的对象内部信息对其他的对象屏蔽起来[DP]，从而可以恰当地保持封装的边界。

最大的作用还是在当角色的状态改变的时候，有可能这个状态无效，这时候就可以使用暂时存储起来的备忘录将状态复原[DP]

整体与部分可以被一致对待的问题。

代码 112. memento/Caretaker.java 类

```java
package com.diguage.didp.memento;

/**
 * Caretaker 类
 *
 */
public class Caretaker {
  private Memento memento;

  public Memento getMemento() {
    return memento;
  }

  public void setMemento(Memento memento) {
    this.memento = memento;
  }
}
```

代码 113. memento/Memento.java 类

```java
package com.diguage.didp.memento;

/**
 * Memento 类
 *
 */
public class Memento {
  private String state;

  public Memento(String state) {
    this.state = state;
  }

  public String getState() {
    return state;
  }
}
```

代码 114. memento/Originator.java 类

```java 
package com.diguage.didp.memento;

/**
 * Originator 类
 *
 */
public class Originator {
  private String state;

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public void setMemento(Memento memento) {
    state = memento.getState();
  }

  public Memento createMemento() {
    return new Memento(state);
  }

  public void show() {
    System.out.println("State=" + state);
  }
}
```

代码 115. memento/Client.java 类

```java
package com.diguage.didp.memento;


public class Client {
  public static void main(String[] args) {
    Originator originator = new Originator();
    originator.setState("On");
    originator.show();


    Caretaker caretaker = new Caretaker();
    caretaker.setMemento(originator.createMemento());

    originator.setState("Off");
    originator.show();

    originator.setMemento(caretaker.getMemento());
    originator.show();
  }
}
```

## 20. 观察者模式

- 观察者模式的使用案例
- 观察者模式的定义以及实力代码
- JDK 对观察者模式的支持
- 在 Spring 中使用观察者模式来实现事件发布

![image-20220123233500056](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123233500056-a40916.png)

观察者模式 注: JDK中内置对观察者模式的支持，查看一下具体实现。以及如何应用案例。

观察者模式又叫做发布-订阅（Publish/Subscribe）模式。

Subject类，可翻译为主题或抽象通知者,一般用一个抽象类或者一个接口实现。它把所有对观察者对象的引用保存在一个聚集里，每个主题都可以有任何数量的观察者。抽象主题提供一个接口，可以增加和删除观察者对象。

Observer类，抽象观察者，为所有的具体观察者定义一个接口，在得到主题的通知时更新自己。这个接口叫做更新接口。抽象观察者一般用一个抽象类或者一个接口实现。更新接口通常包含一个Update()方法，这个方法叫做更新方法。

ConcreteSubject类，叫做具体主题或具体通知者，将有关状态存入具体观察者对象；在具体主题的内部状态改变时，给所有登记过的观察者发出通知。具体主题角色通常用一个具体子类实现。

ConcreteObserver类，具体观察者，实现抽象观察者角色所要求的更新接口，以便使本身的状态与主题的状态相协调。具体观察者角色可以保存一个指向具体主题对象的引用。具体观察者角色通常用一个具体子类实现。

用观察者模式的动机是什么呢？

将一个系统分割成一系列相互协作的类有一个很不好的副作用，那就是需要维护相关对象间的一致性。我们不希望为了维持一致性而使各类紧密耦合，这样会给维护、扩展和重用都带来不便[DP]。而观察者模式的关键对象是主题Subject和观察者Observer，一个Subject可以有任意数目的依赖它的Observer，一旦Subject的状态发生了改变，所有的Observer都可以得到通知。Subject发出通知时并不需要知道谁是它的观察者，也就是说，具体观察者是谁，它根本不需要知道。而任何一个具体观察者不知道也不需要知道其他观察者的存在。

什么时候考虑使用观察者模式呢？

当一个对象的改变需要同时改变其他对象的时候。

而且它不知道具体有多少对象有待改变时，应该考虑使用观察者模式。

当一个抽象模型有两个方面，其中一方面依赖于另一方面，这时用观察者模式可以将这两者封装在独立的对象中使它们各自独立地改变和复用。” “非常好，总的来讲，观察者模式所做的工作其实就是在解除耦合。让耦合的双方都依赖于抽象，而不是依赖于具体。从而使得各自的变化都不会影响另一边的变化。

事件委托实现 注: Java中如何实现委托？

“抽象通知者”由于不希望依赖“抽象观察者”，所以“增加”和“减少”的方法也就没有必要了（抽象观察者已经不存在了）。

委托就是一种引用方法的类型。一旦为委托分配了方法，委托将与该方法具有完全相同的行为。委托方法的使用可以像其他任何方法一样，具有参数和返回值。委托可以看作是对函数的抽象，是函数的‘类’，委托的实例将代表一个具体的函数。

一旦为委托分配了方法，委托将与该方法具有完全相同的行为。而且，一个委托可以搭载多个方法，所有方法被依次唤起。更重要的是，它可以使得委托对象所搭载的方法并不需要属于同一个类。

委托对象所搭载的所有方法必须具有相同的原形和形式，也就是拥有相同的参数列表和返回值类型。

代码 116. observer/ConcreteObserver.java 类

```java
package com.diguage.didp.observer;

/**
 * ConcreteObserver 类
 *
 */
public class ConcreteObserver implements Observer {
  private String name;
  private String observerState;
  private ConcreteSubject subject;

  public ConcreteObserver(ConcreteSubject subject, String name) {
    this.name = name;
    this.subject = subject;
  }

  @Override
  public void update() {
    observerState = subject.getSubjectState();
    System.out.printf("观察者 %s 的新状态是 %s%n", name, observerState);
  }

  public ConcreteSubject getSubject() {
    return subject;
  }

  public void setSubject(ConcreteSubject subject) {
    this.subject = subject;
  }
}
```

代码 117. observer/ConcreteSubject.java 类

```java
package com.diguage.didp.observer;

/**
 * ConcreteSubject 类
 *
 */
public class ConcreteSubject extends Subject {
  private String subjectState;

  public String getSubjectState() {
    return subjectState;
  }

  public void setSubjectState(String subjectState) {
    this.subjectState = subjectState;
  }
}
```

代码 118. observer/Observer.java 类

```java
package com.diguage.didp.observer;

/**
 * Observer 类
 *
 */
public interface Observer {
  void update();
}
```

代码 119. observer/Subject.java 类

```java
package com.diguage.didp.observer;

import java.util.ArrayList;
import java.util.List;

/**
 * Subject 类
 *
 */
public abstract class Subject {
  private List<Observer> observers = new ArrayList<>();

  public void attach(Observer observer) {
    observers.add(observer);
  }

  public void detach(Observer observer) {
    observers.remove(observer);
  }

  public void doNotify() {
    observers.forEach(Observer::update);
  }
}
```

代码 120. observer/Client.java 类

```java
package com.diguage.didp.observer;


public class Client {
  public static void main(String[] args) {
    ConcreteSubject subject = new ConcreteSubject();
    subject.attach(new ConcreteObserver(subject, "X"));
    subject.attach(new ConcreteObserver(subject, "Y"));
    subject.attach(new ConcreteObserver(subject, "Z"));
    subject.setSubjectState("ABC");
    subject.doNotify();
  }
}
```

## 21. 原型模式

![image-20220123233630485](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123233630485-f8ec26.png)

原型模式其实就是从一个对象再创建另外一个可定制的对象，而且不需知道任何创建的细节。

NET在System命名空间中提供了ICloneable接口，其中就是唯一的一个方法Clone()，这样你就只需要实现这个接口就可以完成原型模式了。

一般在初始化的信息不发生变化的情况下，克隆是最好的办法。这既隐藏了对象创建的细节，又对性能是大大的提高，

不用重新初始化对象，而是动态地获得对象运行时的状态。

MemberwiseClone()方法是这样，如果字段是值类型的，则对该字段执行逐位复制，如果字段是引用类型，则复制引用但不复制引用的对象；因此，原始对象及其复本引用同一对象。

‘浅复制’，被复制对象的所有变量都含有与原来的对象相同的值，而所有的对其他对象的引用都仍然指向原来的对象。

把要复制的对象所引用的对象都复制一遍。

叫这种方式为‘深复制’，深复制把引用对象的变量指向复制过的新对象，而不是原有的被引用的对象。

深复制要深入到多少层，需要事先就考虑好，而且要当心出现循环引用的问题，需要小心处理，这里比较复杂，可以慢慢研究。

代码 121. prototype/Client.java 类

```java
package com.diguage.didp.prototype;

/**
 * Client 类
 *
 */
public class Client {
  public static void main(String[] args) {
    Prototype p1 = new ConcretePrototype1();
    Prototype p2 = p1.clone();
  }
}
```

代码 122. prototype/ConcretePrototype1.java 类

```java
package com.diguage.didp.prototype;

/**
 * ConcretePrototype1 类
 *
 */
public class ConcretePrototype1 extends Prototype {
  public Prototype clone() {
    return this;
  }
}
```

代码 123. prototype/ConcretePrototype2.java 类

```java
package com.diguage.didp.prototype;

/**
 * ConcretePrototype2 类
 *
 */
public class ConcretePrototype2 extends Prototype {
  public Prototype clone() {
    return this;
  }
}
```

代码 124. prototype/Prototype.java 类

```java
package com.diguage.didp.prototype;

/**
 * Prototype 类
 *
 */
public abstract class Prototype {
  protected int id = 0;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public abstract Prototype clone();
}
```

## 22. 状态模式



![image-20220123233824587](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123233824587-631c6b.png)

MartinFowler曾在《重构》中写过一个很重要的代码坏味道，叫做‘Long Method’，方法如果过长其实极有可能是有坏味道了。

面向对象设计其实就是希望做到**代码的责任分解。**

状态模式主要解决的是当控制一个对象状态转换的条件表达式过于复杂时的情况。把状态的判断逻辑转移到表示不同状态的一系列类当中，可以把复杂的判断逻辑简化。

State类，抽象状态类，定义一个接口以封装与Context的一个特定状态相关的行为。

ConcreteState类，具体状态，每一个子类实现一个与Context的一个状态相关的行为。

Context类，维护一个ConcreteState子类的实例，这个实例定义当前的状态。

状态模式的好处是将与特定状态相关的行为局部化，并且将不同状态的行为分割开来[DP]。

将特定的状态相关的行为都放入一个对象中，由于所有与状态相关的代码都存在于某个ConcreteState中，所以通过定义新的子类可以很容易地增加新的状态和转换[DP]。

这样做的目的就是为了消除庞大的条件分支语句，大的分支判断会使得它们难以修改和扩展，就像我们最早说的刻版印刷一样，任何改动和变化都是致命的。状态模式通过把各种状态转移逻辑分布到State的子类之间，来减少相互间的依赖，好比把整个版面改成了一个又一个的活字，此时就容易维护和扩展了。

当一个对象的行为取决于它的状态，并且它必须在运行时刻根据状态改变它的行为时，就可以考虑使用状态模式了。

代码 125. state/ConcreteStateA.java 类

```java
package com.diguage.didp.state;

/**
 * ConcreteStateA 类
 */
public class ConcreteStateA extends State {
  @Override
  public void handle(Context context) {
    context.setState(new ConcreteStateB());
  }
}
```

代码 126. state/ConcreteStateB.java 类

```java
package com.diguage.didp.state;

/**
 * ConcreteStateB 类
 *
 */
public class ConcreteStateB extends State {
  @Override
  public void handle(Context context) {
    context.setState(new ConcreteStateC());
  }
}
```

代码 127. state/ConcreteStateC.java 类

```java
package com.diguage.didp.state;

/**
 * ConcreteStateC 类
 *
 */
public class ConcreteStateC extends State {
  @Override
  public void handle(Context context) {
    context.setState(new ConcreteStateA());
  }
}
```

代码 128. state/Context.java 类

```java
package com.diguage.didp.state;

/**
 * Context 类
 *
 */
public class Context {
  private State state;

  public Context(State state) {
    this.state = state;
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
    System.out.println("当前状态：" + state.getClass().getName());
  }

  public void request() {
    state.handle(this);
  }
}
```

代码 129. state/State.java 类

```java
package com.diguage.didp.state;

/**
 * State 类
 *
 */
public abstract class State {
  public abstract void handle(Context context);
}
```

代码 130. state/Client.java 类

```java
package com.diguage.didp.state;


 
public class Client {
  public static void main(String[] args) {
    Context context = new Context(new ConcreteStateA());
    context.request();
    context.request();
    context.request();
    context.request();
  }
}
```

## 23. 策略模式

![image-20220123234013432](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123234013432-bcc1b9.png)

面向对象的编程，并不是类越多越好，类的划分是为了封装，但分类的基础是抽象，具有相同属性和功能的对象的抽象集合才是类。

策略模式定义了算法家族，分别封装起来，让它们之间可以互相替换，此模式让算法的变化，不会影响到使用算法的客户。

商场收银时如何促销，用打折还是返利，其实都是一些算法，用工厂来生成算法对象，这没有错，但算法本身只是一种策略，最重要的是这些算法是随时都可能互相替换的，这就是变化点，而封装变化点是我们面向对象的一种很重要的思维方式。

Strategy类，定义所有支持的算法的公共接口

ConcreteStrategy，封装了具体的算法或行为，继承于Strategy

Context，用一个ConcreteStrategy来配置，维护一个对Strategy对象的引用。

策略模式是一种定义一系列算法的方法，从概念上来看，所有这些算法完成的都是相同的工作，只是实现不同，它可以以相同的方式调用所有的算法，减少了各种算法类与使用算法类之间的耦合[DPE]。

策略模式的Strategy类层次为Context定义了一系列的可供重用的算法或行为。继承有助于析取出这些算法中的公共功能[DP]。

策略模式的优点是简化了单元测试，因为每个算法都有自己的类，可以通过自己的接口单独测试[DPE]。

当不同的行为堆砌在一个类中时，就很难避免使用条件语句来选择合适的行为。将这些行为封装在一个个独立的Strategy类中，可以在使用这些行为的类中消除条件语句[DP]。

策略模式封装了变化。

策略模式就是用来封装算法的，但在实践中，我们发现可以用它来封装几乎任何类型的规则，只要在分析过程中听到需要在不同时间应用不同的业务规则，就可以考虑使用策略模式处理这种变化的可能性[DPE] 注: 我觉得可以使用策略模式、责任链模式和动态表单实现一个灵活的促销系统。思考一下如何实现？

在基本的策略模式中，选择所用具体实现的职责由客户端对象承担，并转给策略模式的Context对象[DPE]

任何需求的变更都是需要成本的。

高手和菜鸟的区别就是高手可以花同样的代价获得最大的收益或者说做同样的事花最小的代价。面对同样的需求，当然是改动越小越好。

代码 131. strategy/Client.java 类

```java
package com.diguage.didp.strategy;

/**
 * Client 类
 *
 */
public class Client {
  public static void main(String[] args) {
    Context context = new Context(new ConcreteStrategyA());
    context.contextInterface();

    context.setStrategy(new ConcreteStrategyB());
    context.contextInterface();

    context.setStrategy(new ConcreteStrategyC());
    context.contextInterface();
  }
}
```

代码 132. strategy/ConcreteStrategyA.java 类

```java
package com.diguage.didp.strategy;

/**
 * 具体算法 A
 *
 */
public class ConcreteStrategyA extends Strategy {
  public void algorithmInterface() {
    System.out.println("算法A实现");
  }
}
```

代码 133. strategy/ConcreteStrategyB.java 类

```java
package com.diguage.didp.strategy;

/**
 * 具体算法 B
 *
 */
public class ConcreteStrategyB extends Strategy {
  public void algorithmInterface() {
    System.out.println("算法B实现");
  }
}
```

代码 134. strategy/ConcreteStrategyC.java 类

```java
package com.diguage.didp.strategy;

/**
 * 具体算法 C
 *
 */
public class ConcreteStrategyC extends Strategy {
  public void algorithmInterface() {
    System.out.println("算法C实现");
  }
}
```

代码 135. strategy/Context.java 类

```java
package com.diguage.didp.strategy;

/**
 * Context 类
 *
 */
public class Context {
  private Strategy strategy;

  public Context(Strategy strategy) {
    this.strategy = strategy;
  }

  public void contextInterface() {
    strategy.algorithmInterface();
  }

  public void setStrategy(Strategy strategy) {
    this.strategy = strategy;
  }
}
```

代码 136. strategy/Strategy.java 类

```java
package com.diguage.didp.strategy;

/**
 * Strategy 类
 *
 */
public abstract class Strategy {
  public abstract void algorithmInterface();
}
```

## 24. 模版方法模式

![image-20220123234153130](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123234153130-65532e.png)

当我们要完成在某一细节层次一致的一个过程或一系列步骤，但其个别步骤在更详细的层次上的实现可能不同时，我们通常考虑用模板方法模式来处理。

AbstractClass是抽象类，其实也就是一抽象模板，定义并实现了一个模版方法。这个模版方法一般是一个具体方法，它给出了一个顶级逻辑的骨架，而逻辑的组成步骤在相应的抽象操作中，推迟到子类实现。顶级逻辑也有可能调用一些具体方法。

ConcreteClass，实现父类所定义的一个或多个抽象方法。每一个AbstractClass都可以有任意多个ConcreteClass与之对应，而每一个ConcreteClass都可以给出这些抽象方法（也就是顶级逻辑的组成步骤）的不同实现，从而使得顶级逻辑的实现各不相同。

模板方法模式是通过把不变行为搬移到超类，去除子类中的重复代码来体现它的优势。

模板方法模式就是提供了一个很好的代码复用平台。

因为有时候，我们会遇到由一系列步骤构成的过程需要执行。这个过程从高层次上看是相同的，但有些步骤的实现可能不同。这时候，我们通常就应该要考虑用模板方法模式了。

当不变的和可变的行为在方法的子类实现中混合在一起的时候，不变的行为就会在子类中重复出现。我们通过模板方法模式把这些行为搬移到单一的地方，这样就帮助子类摆脱重复的不变行为的纠缠。

代码 137. templatemethod/AbstractClass.java 类

```java
package com.diguage.didp.templatemethod;

/**
 * AbstractClass 类
 *
 */
public abstract class AbstractClass {
  public final void templateMethod() {
    primitiveOperation1();
    System.out.println("这是模板方法！");
    primitiveOperation2();
  }

  public abstract void primitiveOperation1();

  public abstract void primitiveOperation2();
}
```

代码 138. templatemethod/Client.java 类

```java
package com.diguage.didp.templatemethod;


public class Client {
  public static void main(String[] args) {
    AbstractClass c;
    c = new ConcreteClassA();
    c.templateMethod();

    c = new ConcreteClassB();
    c.templateMethod();
  }
}
```

代码 139. templatemethod/ConcreteClassA.java 类

```java
package com.diguage.didp.templatemethod;

/**
 * ConcreteClassA 类
 *
 */
public class ConcreteClassA extends AbstractClass {
  public void primitiveOperation1() {
    System.out.println("具体类A方法1的实现");
  }

  public void primitiveOperation2() {
    System.out.println("具体类A方法2的实现");
  }
}
```

代码 140. templatemethod/ConcreteClassB.java 类

```java
package com.diguage.didp.templatemethod;

/**
 * ConcreteClassA 类
 */
public class ConcreteClassB extends AbstractClass {
  public void primitiveOperation1() {
    System.out.println("具体类B方法1的实现");
  }

  public void primitiveOperation2() {
    System.out.println("具体类B方法2的实现");
  }
}
```

## 25. 访问者模式

![image-20220123234312363](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123234312363-445491.png)



男人这本书的内容要比封面吸引人，女人这本书的封面通常是比内容更吸引人。 男人的青春表示一种肤浅，女人的青春标志一种价值。

访问者模式讲的是表示一个作用于某对象结构中的各元素的操作。它使你可以在不改变各元素的类的前提下定义作用于这些元素的新操作。

一种双分派的技术，首先在客户程序中将具体状态作为参数传递给“男人”类完成了一次分派，然后“男人”类调用作为参数的“具体状态”中的方法“男人反应”，同时将自己（this）作为参数传递进去。这便完成了第二次分派。双分派意味着得到执行的操作决定于请求的种类和两个接收者的类型。‘接受’方法就是一个双分派的操作，它得到执行的操作不仅决定于‘状态’类的具体状态，还决定于它访问的‘人’的类别。

访问者模式适用于数据结构相对稳定的系统

它把数据结构和作用于结构上的操作之间的耦合解脱开，使得操作集合可以相对自由地演化。

访问者模式的目的是要把处理从数据结构分离出来。很多系统可以按照算法和数据结构分开，如果这样的系统有比较稳定的数据结构，又有易于变化的算法的话，使用访问者模式就是比较合适的，因为访问者模式使得算法操作的增加变得容易。反之，如果这样的系统的数据结构对象易于变化，经常要有新的数据对象增加进来，就不适合使用访问者模式。

访问者模式的优点就是增加新的操作很容易，因为增加新的操作就意味着增加一个新的访问者。访问者模式将有关的行为集中到一个访问者对象中。

通常ConcreteVisitor可以单独开发，不必跟ConcreteElementA或ConcreteElementB写在一起。正因为这样，ConcreteVisitor能提高ConcreteElement之间的独立性，如果把一个处理动作设计成ConcreteElementA和ConcreteElementB类的方法，每次想新增“处理”以扩充功能时就得去修改ConcreteElementA和ConcreteElementB了。

那访问者的缺点其实也就是使增加新的数据结构变得困难了。

GoF四人中的一个作者就说过：‘大多时候你并不需要访问者模式，但当一旦你需要访问者模式时，那就是真的需要它了。’

Visitor类，为该对象结构中ConcreteElement的每一个类声明一个Visit操作。

ConcreteVisitor1和ConcreteVisitor2类，具体访问者，实现每个由Visitor声明的操作。每个操作实现算法的一部分，而该算法片断乃是对应于结构中对象的类。

Element类，定义一个Accept操作，它以一个访问者为参数。

ConcreteElementA和ConcreteElementB类，具体元素，实现Accept操作。

ObjectStructure类，能枚举它的元素，可以提供一个高层的接口以允许访问者访问它的元素。

访问者模式的能力和复杂性是把双刃剑，只有当你真正需要它的时候，才考虑使用它。

代码 141. visitor/Client.java 类

```java
package com.diguage.didp.visitor;

/**
 * Client 类
 *
 */
public class Client {
  public static void main(String[] args) {
    ObjectStructure o = new ObjectStructure();
    o.attach(new ConcreteElementA());
    o.attach(new ConcreteElementB());

    ConcreteVisitor1 v1 = new ConcreteVisitor1();
    ConcreteVisitor2 v2 = new ConcreteVisitor2();

    o.accept(v1);
    o.accept(v2);
  }
}
```

代码 142. visitor/ConcreteElementA.java 类

```java
package com.diguage.didp.visitor;

/**
 * ConcreteElementA 类
 *
 */
public class ConcreteElementA extends Element {
  @Override
  public void accept(Vistor vistor) {
    vistor.visitConcreteElementA(this);
  }

  public void operatorA() {
    System.out.println("B其他操作");
  }
}
```

代码 143. visitor/ConcreteElementB.java 类

```java
package com.diguage.didp.visitor;

/**
 * ConcreteElementB 类
 *
 */
public class ConcreteElementB extends Element {
  @Override
  public void accept(Vistor vistor) {
    vistor.visitConcreteElementB(this);
  }

  public void operatorB() {
    System.out.println("B其他操作");
  }
}
```

代码 144. visitor/ConcreteVisitor1.java 类

```java
package com.diguage.didp.visitor;

/**
 * ConcreteVisitor1 类
 *
 */
public class ConcreteVisitor1 extends Vistor {
  @Override
  public void visitConcreteElementA(ConcreteElementA element) {
    System.out.printf("%s 被 %s 访问%n", //
        element.getClass().getName(), this.getClass().getName());
  }

  @Override
  public void visitConcreteElementB(ConcreteElementB element) {
    System.out.printf("%s 被 %s 访问%n", //
        element.getClass().getName(), this.getClass().getName());
  }
}
```

代码 145. visitor/ConcreteVisitor2.java 类

```java
package com.diguage.didp.visitor;

/**
 * ConcreteVisitor2 类
 *
 */
public class ConcreteVisitor2 extends Vistor {
  @Override
  public void visitConcreteElementA(ConcreteElementA element) {
    System.out.printf("%s 被 %s 访问%n", //
        element.getClass().getName(), this.getClass().getName());
  }

  @Override
  public void visitConcreteElementB(ConcreteElementB element) {
    System.out.printf("%s 被 %s 访问%n", //
        element.getClass().getName(), this.getClass().getName());
  }
}
```

代码 146. visitor/Element.java 类

```java
package com.diguage.didp.visitor;

/**
 * Element 类
 *
 */
public abstract class Element {
  public abstract void accept(Vistor vistor);
}
```

代码 147. visitor/ObjectStructure.java 类

```java
package com.diguage.didp.visitor;

import java.util.ArrayList;
import java.util.List;

/**
 * ObjectStructure 类
 *
 */
public class ObjectStructure {
  private List<Element> elements = new ArrayList<>();

  public void attach(Element element) {
    elements.add(element);
  }

  public void detach(Element element) {
    elements.remove(element);
  }

  public void accept(Vistor vistor) {
    elements.forEach(e -> e.accept(vistor));
  }
}
```

代码 148. visitor/Vistor.java 类

```java
package com.diguage.didp.visitor;

/**
 * Vistor 类
 *
 */
public abstract class Vistor {
  public abstract void visitConcreteElementA(ConcreteElementA element);

  public abstract void visitConcreteElementB(ConcreteElementB element);
}
```

## 26. 总结

面向过程关注业务流程，但无论多么努力工作，分析做得如何好，也是永远无法从用户那里获得所有的需求的，而业务流程却是需求中最可能变化的地方，业务流程的制定需要受到很多条件的限制，甚至程序的效率、运行方式都会反过来影响业务流程。有时候用户也会为了更好地实现商业目的，主动地改变业务流程，并且一个流程的变化经常会带来一系列的变化。这就使得按照业务流程设计的程序经常面临变化。

流程的易变性，使得把流程看得很重，并不能适应变化。

面向过程通过划分功能模块，通过函数相互间的调用来实现，但需求变化时，就需要更改函数。而你改动的函数有多少的地方在调用它，关联多少数据，这是很不容易弄得清楚的地方。

与其抱怨需求总是变化，不如改变开发过程，从而更有效地应对变化。面向对象的编程方式诞生，就是为解决变化带来的问题。

面向对象关注的是对象，对象的优点在于，可以定义自己负责的事物，做要求它自己做的事情。对象应该自己负责自己，而且应该清楚地定义责任。

需求变化是必然的，那么尽管无法预测会发生什么变化，但是通常可以预测哪里会发生变化。面向对象的优点之一，就是可以封装这些变化区域，从而更容易地将代码与变化产生的影响隔离开来。代码可以设计得使需求的变化不至于产生太大的影响。代码可以逐步演进，新代码可以影响较少地加入。

设计模式是面向对象技术的最新进展之一。由于面向对象设计的复杂性，所以我们都希望能做出应对变化，提高复用的设计方案，而设计模式就能帮助我们做到这样的结果。通过复用已经公认的设计，我们能够在解决问题时避免前人所犯的种种错误，可以从学习他人的经验中获益，用不着为那些总是会重复出现的问题再次设计解决方案。显然，设计模式有助于提高我们的思考层次。让我们能站在山顶而不是山脚，也就是更高的高度来俯视我们的设计。

抽象工厂小姐，她的口号是提供一个创建一系列或相关依赖对象的接口，而无需指定它们具体的类。[DP]

建造者小姐，她的口号是将一个复杂对象的构建与它的表示分离，使得同样的构建过程可以创建不同的表示。[DP]

工厂方法小姐向我们走来，她声称定义一个用于创建对象的接口，让子类决定实例化哪一个类，工厂模式使一个类的实例化延迟到其子类。[DP]

原型小姐，她的意图是用原型实例指定创建对象的种类，并且通过拷贝这些原型创建新的对象。[DP]

单例小姐，她提倡简捷就是美，保证一个类仅有一个实例，并提供一个访问它的全局访问点。[DP]

创建型模式隐藏了这些类的实例是如何被创建和放在一起，整个系统关于这些对象所知道的是由抽象类所定义的接口。这样，创建型模式在创建了什么、谁创建它、它是怎么被创建的，以及何时创建这些方面提供了很大的灵活性[DP]。

当一个系统应该独立于它的产品创建、构成和表示时，应该考虑用创建性模式。建立相应数目的原型并克隆它们通常比每次用合适的状态手工实例化该类更方便一些[DP]。

内聚性描述的是一个例程内部组成部分之间相互联系的紧密程度。而耦合性描述的是一个例程与其他例程之间联系的紧密程度。软件开发的目标应该是创建这样的例程：内部完整，也就是高内聚，而与其他例程之间的联系则是小巧、直接、可见、灵活的，这就是松耦合[DPE]。

将一个复杂对象的构建与它的表示分离，这就可以很容易地改变一个产品的内部表示，并且使得构造代码和表示代码分开。这样对于客户来说，它无需关心产品的创建过程，而只要告诉我需要什么，我就能用同样的构建过程创建不同的产品给客户[DP]。

对一些类来说，一个实例是很重要的。一个全局变量可以使得一个对象被访问，但它不能防止客户实例化多个对象。我的优势就是让类自身负责保存它的唯一实例。这个类可以保证没有其他实例可以被创建，并且我还提供了一个访问该实例的方法。这样就使得对唯一的实例可以严格地控制客户怎样以及何时访问它[DP]。

创建型模式抽象了实例化的过程。它们帮助一个系统独立于如何创建、组合和表示它的那些对象。创建型模式都会将关于该系统使用哪些具体的类的信息封装起来。允许客户用结构和功能差别很大的‘产品’对象配置一个系统。配置可以是静态的，即在编译时指定，也可以是动态的，就是运行时再指定。[DP]

通常设计应该是从我，也就是工厂方法开始，当设计者发现需要更大的灵活性时，设计便会向其他创建型模式演化。当设计者在设计标准之间进行权衡的时候，了解多个创建型模式可以给设计者更多的选择余地。[DP]

工厂方法能使得我们增加新的产品时，不需要去更改原有的产品体系和工厂类，只需扩展新的类就可以了。

适配器小姐，她的口号是将一个类的接口转换成客户希望的另外一个接口。适配器模式使得原本由于接口不兼容而不能一起工作的那些类可以一起工作。[DP]

桥接。桥接小姐提倡的是将抽象部分与它的实现部分分离，使它们都可以独立地变化。[DP]

组合小姐，一个非常美丽的姑娘，她的口号是将对象组合成树形结构以表示‘部分-整体’的层次结构，组合模式使得用户对单个对象和组合对象的使用具有一致性。[DP]

装饰小姐，她的意图非常简单，就是动态地给一个对象添加一些额外的职责。就增加功能来说，装饰模式相比生成子类更加灵活[DP]。

外观小姐，她的形象如她的名字一样的棒，她说为子系统中的一组接口提供一个一致的界面，外观模式定义了一个高层接口，这个接口使得这一子系统更加容易使用。[DP]

享元小姐，她的参赛宣言为运用共享技术有效地支持大量细粒度的对象。[DP]

代理小姐向我们走来，她声称为其他对象提供一种代理以控制对这个对象的访问。[DP]

面向对象的精神就是更好地应对需求的变化，而现实中往往会有下面这些情况，想使用一个已经存在的类，而它的接口不符合要求，或者希望创建一个可以复用的类，该类可以与其他不相关的类或不可预见的类协同工作。

让这些接口不同的类通过适配后，协同工作。[DP]

继承是好的东西，但往往会过度地使用，继承会导致类的结构过于复杂，关系太多，难以维护，而更糟糕的是扩展性非常差。而仔细研究如果能发现继承体系中，有两个甚至多个方向的变化，那么就解耦这些不同方向的变化，通过对象组合的方式，把两个角色之间的继承关系改为了组合的关系，从而使这两者可以应对各自独立的变化，事实上也就是合成聚合复用女士所提倡的原则，总之，面对变化，我主张‘找出变化并封装之’。[DPE]

面对变化，如果采用生成子类的方法进行扩充，为支持每一种扩展的组合，会产生大量的子类，使得子类数目呈爆炸性增长。这也是刚才桥接小姐所提到的继承所带来的灾难，而事实上，这些子类多半只是为某个对象增加一些职责，此时通过装饰的方式，可以更加灵活、以动态、透明的方式给单个对象添加职责，并在不需要时，撤销相应的职责。[DP]

希望用户忽略组合对象与单个对象的不同，用户将可以统一地使用组合结构中的所有对象。

用户使用组合类接口与组合结构中的对象进行交互，如果接收者是一个叶节点，则直接处理请求，如果接收者是组合对象，通常将请求发送给它的子部件，并在转发请求之前或之后可能执行一些辅助操作。组合模式的效果是客户可以一致地使用组合结构和单个对象。任何用到基本对象的地方都可以使用组合对象。[DP]

外观小姐，信息的隐藏促进了软件的复用[J&DP]

类之间的耦合越弱，越有利于复用，一个处在弱耦合的类被修改，不会对有关系的类造成波及。如果两个类不必彼此直接通信，那么就不要让这两个类发生直接的相互作用。如果实在需要调用，可以通过第三者来转发调用。[J&DP]

应该让一个软件中的子系统间的通信和相互依赖关系达到最小，而具体办法就是引入一个外观对象，它为子系统间提供了一个单一而简单的屏障[DP]。

通常企业软件的三层或N层架构，层与层之间地分离其实就是外观模式的体现。

对象使得内存占用过多，而且如果都是大量重复的对象，那就是资源的极大浪费[DP]

面向对象技术有时会因简单化的设计而代价极大。

代理与外观的主要区别在于，代理对象代表一个单一对象而外观对象代表一个子系统；代理的客户对象无法直接访问目标对象，由代理提供对单独的目标对象的访问控制，而外观的客户对象可以直接访问子系统中的各个对象，但通常由外观对象提供对子系统各元件功能的简化的共同层次的调用接口。[R2P]

代理是一种原来对象的代表，其他需要与这个对象打交道的操作都是和这个代表交涉。而适配器则不需要虚构出一个代表者，只需要为应付特定使用目的，将原来的类进行一些组合。[DP]

适配器说：“我主要是为了解决两个已有接口之间不匹配的问题，我不需要考虑这些接口是怎样实现的，也不考虑它们各自可能会如何演化。我的这种方式不需要对两个独立设计的类中任一个进行重新设计，就能够使它们协同工作。[DP]

我觉得我和适配器小姐具有一些共同的特征，就是给另一对象提供一定程度的间接性，这样可以有利于系统的灵活性。但正所谓未雨绸缪，我们不能等到问题发生了，再去考虑解决问题，而是更应该在设计之初就想好应该如何做来避免问题的发生，我通常是在设计之初，就对抽象接口与它的实现部分进行桥接，让抽象与实现两者可以独立演化。显然，我的优势更明显。[DP]

桥接和适配器是被用于软件生命周期的不同阶段，针对的是不同的问题，谈不上孰优孰劣。

然后，对于我来说，和适配器还有些近似，都是对现存系统的封装，有人说我其实就是另外一组对象的适配器，这种说法是不准确的，因为外观定义的是一个新的接口，而适配器则是复用一个原有的接口，适配器是使两个已有的接口协同工作，而外观则是为现存系统提供一个更为方便的访问接口。如果硬要说我是适配，那么适配器是用来适配对象的，而我则是用来适配整个子系统的。也就是说，我所针对的对象的粒度更大。[DP]

观察者小姐入场，它的口号是定义对象间的一种一对多的依赖关系，当一个对象的状态发生改变时，所有依赖于它的对象都得到通知并被自动更新。[DP]

模板方法小姐，她提倡定义一个操作的算法骨架，而将一些步骤延迟到子类中，模板方法使得子类可以不改变一个算法的结构即可重定义该算法的某些特定步骤。[DP]

命令小姐，它觉得应该将一个请求封装为一个对象，从而使你可用不同的请求对客户进行参数化；可以对请求排队或记录请求日志，以及支持可撤销的操作。[DP]

状态小姐，她说允许一个对象在其内部状态改变时改变它的行为，让对象看起来似乎修改了它的类。[DP]

职责链小姐，她一直认为使多个对象都有机会处理请求，从而避免请求的发送者和接收者之间的耦合关系。将这些对象连成一条链，并沿着这条链传递该请求，直到有一个对象处理它为止。[DP]

对象间，尤其是具体对象间，相互知道的越少越好，这样发生改变时才不至于互相影响。对于我来说，目标和观察者不是紧密耦合的，它们可以属于一个系统中的不同抽象层次，目标所知道的仅仅是它有一系列的观察者，每个观察者实现Observer的简单接口，观察者属于哪一个具体类，目标是不知道的。

代码重复是编程中最常见、最糟糕的‘坏味道’，如果我们在一个以上的地方看到相同的程序结构，那么可以肯定，设法将它们合而为一，程序会变得更好[RIDEC]。但是完全相同的代码当然存在明显的重复，而微妙的重复会出现在表面不同但是本质相同的结构或处理步骤中[R2P]，这使得我们一定要小心处理。继承的一个非常大的好处就是你能免费地从基类获取一些东西，当你继承一个类时，派生类马上就可以获得基类中所有的功能，你还可以在它的基础上任意增加新的功能。模板方法模式由一个抽象类组成，这个抽象类定义了需要覆盖的可能有不同实现的模板方法，每个从这个抽象类派生的具体类将为此模板实现新方法[DPE]。这样就使得，所有可重复的代码都提炼到抽象类中了，这就实现了代码的重用。

将调用操作的对象与知道如何实现该操作的对象解耦

在不同的时刻指定、排列和执行请求。

支持取消/重做的操作。我还可以记录整个操作的日志

支持事务

有多个对象可以处理一个请求，哪个对象处理该请求事先并不知道，要在运行时刻自动确定，此时，最好的办法就是让请求发送者与具体处理者分离，让客户在不明确指定接收者的情况下，提交一个请求，然后由所有能处理这请求的对象连成一条链，并沿着这条链传递该请求，直到有一个对象处理它为止。”职责链

状态模式提供了一个更好的办法来组织与特定状态相关的代码，决定状态转移的逻辑不在单块的if或switch中，而是分布在各个状态子类之间，由于所有与状态相关的代码都存在于某个状态子类中，所以通过定义新的子类可以很容易地增加新的状态和转换。[DP]

MVC是包括三类对象，Model是应用对象，View是它在屏幕上的表示，Controller定义用户界面对用户输入的响应方式。如果不使用MVC，则用户界面设计往往将这些对象混在一起，而MVC则将它们分离以提高灵活性和复用性[DP]。

MVC是多种模式的综合应用，应该算是一种架构模式

解释器小姐，它声称给定一个语言，定义它的文法的一种表示，并定义一个解释器，这个解释器使用该表示来解释语言中的句子。[DP]

中介者小姐，她说她是用一个中介对象来封装一系列的对象交互。中介者使各对像不需要显式地相互引用，从而使其耦合松散，而且可以独立地改变它们之间的交互。[DP]

访问者小姐，她表示一个作用于某对象结构中的各元素的操作。它使你可以在不改变各元素的类的前提下定义作用于这些元素的新操作。[DP]

策略，一个可爱的姑娘，她的意图是定义一系列的算法，把它们一个个封装起来，并且使它们可相互替换。本模式使得算法可独立于使用它的客户而变化。[DP]

备忘录小姐，她说在不破坏封装性的前提下，捕获一个对象的内部状态，并在该对象之外保存这个状态。这样以后就可将该对象恢复到原先保存的状态。[DP]

迭代器小姐，她说，提供一种方法顺序访问一个聚合对象中各个元素，而又不需暴露该对象的内部表示。[DP]

如果一种特定类型的问题发生的频率足够高，那么就可以考虑将该问题的各个实例表述为一个简单语言中的句子。也就是说，通过构建一个解释器，该解释器解释这些句子来解决该问题[DP]。

比如正则表达式就是描述字符串模式的一种标准语言，与其为每一个字符串模式都构造一个特定的算法，不如使用一种通用的搜索算法来解释执行一个正则表达式，该正则表达式定义了待匹配字符器的集合[DP]。

面向对象设计鼓励将行为分布到各个对象中，这种分布可能会导致对象间有许多连接。也就是说，有可能每一个对象都需要知道其他许多对象。对象间的大量相互连接使得一个对象似乎不太可能在没有其他对象的支持下工作，这对于应对变化是不利的，任何较大的改动都很困难[DP]。

将集体行为封装一个单独的中介者对象来避免这个问题，中介者负责控制和协调一组对象间的交互。中介者充当一个中介以使组中的对象不再相互显式引用。这些对象仅知道中介者，从而减少了相互连接的数目[DP]。

最少知识原则，也就是如何减少耦合的问题，类之间的耦合越弱，越有利于复用[J&DP]。

访问者增加具体的Element是困难的，但增加依赖于复杂对象结构的构件的操作就变得容易。仅需增加一个新的访问者即可在一个对象结构上定义一个新的操作。

优先使用对象组合，而非类继承

继承提供了一种支持多种算法或行为的方法，我们可以直接生成一个类A的子类B、C、D，从而给它以不同的行为。但这样会将行为硬行编制到父类A当中，而将算法的实现与类A的实现混合起来，从而使得类A难以理解、难以维护和难以扩展，而且还不能动态地改变算法。仔细分析会发现，它们之间的唯一差别是它们所使用的算法或行为，将算法封装在独立的策略Strategy类中使得你可以独立于其类A改变它，使它易于切换、易于理解、易于扩展[DP]。

通常原对象A都有很多状态属性，保存对象的内部状态，其实也就是将这些状态属性的值可以记录到A对象外部的另一个对象B，但是，如果记录的过程是对外透明的，那就意味着保存过程耦合了对象状态细节。使用备忘录就不会出现这个问题，它可以避免暴露一些只应由对象A管理却又必须存储在对象A之外的信息。备忘录模式把可能很复杂的对象A的内部信息对其他对象屏蔽起来，从而保持了封装边界[DP]。

一个集合对象，它当中具体是些什么对象元素我并不知道，但不管如何，应该提供一种方法来让别人可以访问它的元素，而且可能要以不同的方式遍历这个集合。迭代器模式的关键思想是将对列表的访问和遍历从列表对象中分离出来并放入一个迭代器对象中，迭代器类定义了一个访问该列表元素的接口。迭代器对象负责跟踪当前的元素，并且知道哪些元素已经遍历过了[DP]。

只要是在做面向对象的开发，创建对象的工作不可避免。创建对象时，负责创建的实体通常需要了解要创建的是哪个具体的对象，以及何时创建这个而非那个对象的规则。而我们如果希望遵循开放-封闭原则、依赖倒转原则和里氏代换原则，那使用对象时，就不应该知道所用的是哪一个特选的对象。此时就需要‘对象管理者’工厂来负责此事[DPE]。

在创建对象时，使用抽象工厂、原型、建造者的设计比使用工厂方法要更灵活，但它们也更加复杂，通常，设计是以使用工厂方法开始，当设计者发现需要更大的灵活性时，设计便会向其他创建型模式演化[DP]。

工厂方法的实现并不能减少工作量，但是它能够在必须处理新情况时，避免使已经很复杂的代码更加复杂[DPE]。

面向对象设计模式体现的就是抽象的思想，类是什么，类是对对象的抽象，抽象类呢，其实就是对类的抽象，那接口呢，说白了就是对行为的抽象。

## 附录 A: 参考资料

- [大话设计模式](https://book.douban.com/subject/2334288/)
- [图说设计模式 — Graphic Design Patterns](http://design-patterns.readthedocs.io/zh_CN/latest/index.html)

## 附录 B: Plant UML 类图小技巧

### B.1. 改变剪头方向

```java
ClassA -right-> ClassB
// 可选的方向关键字： up right down left
```

### B.2. 隐藏剪头

```java
ClassA -[hidden]-> ClassB
```

### B.3. 组合

### B.4. 聚合

### B.5. 泛化

### B.6. 实现

### B.7. 添加标题

### B.8. 标题加粗

### B.9. 标题内换行

### B.10. 抽象方法

### B.11. 抽象类

### B.12. 接口
