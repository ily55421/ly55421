# Mybatis 核心处理层 源码解析

# 1、MyBatis初始化

和 Spring 框架 的 IoC 容器初始化 一样，Mybatis 也会通过定位、解析相应的配置文件完成自己的初始化。Mybatis 的配置文件主要有 mybatis-config.xml 核心配置文件 及一系列映射配置文件，另外，Mybatis 也会根据注解进行配置。

## 1 BaseBuilder

Mybatis 初始化 的主要内容是加载并解析 mybatis-config.xml 配置文件、映射配置文件以及相关的注解信息。Mybatis 的初始化入口是 SqlSessionFactoryBuilder 的 build()方法。

```java
public class SqlSessionFactoryBuilder {

  public SqlSessionFactory build(Reader reader) {
    return build(reader, null, null);
  }

  public SqlSessionFactory build(Reader reader, String environment) {
    return build(reader, environment, null);
  }

  public SqlSessionFactory build(Reader reader, Properties properties) {
    return build(reader, null, properties);
  }

  /**
   * build()方法 的主要实现
   */
  public SqlSessionFactory build(Reader reader, String environment, Properties properties) {
    try {
      // SqlSessionFactory 会创建 XMLConfigBuilder对象 来解析 mybatis-config.xml配置文件
      // XMLConfigBuilder 继承自 BaseBuilder抽象类，顾名思义这一系的类使用了 建造者设计模式
      XMLConfigBuilder parser = new XMLConfigBuilder(reader, environment, properties);
      // 解析配置文件的内容 到 Configuration对象，根据 Configuration对象
      // 创建 DefaultSqlSessionFactory对象，然后返回
      return build(parser.parse());
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error building SqlSession.", e);
    } finally {
      ErrorContext.instance().reset();
      try {
        // 关闭配置文件输入流
        reader.close();
      } catch (IOException e) {
        // Intentionally ignore. Prefer previous error.
      }
    }
  }

  public SqlSessionFactory build(Configuration config) {
    return new DefaultSqlSessionFactory(config);
  }
```

BaseBuilder 中的核心字段如下：

```java
public abstract class BaseBuilder {

  // 保存了 Mybatis 的几乎所以核心配置信息，全局唯一
  protected final Configuration configuration;
  // 在 mybatis-config.xml 中可以通过 <typeAliases>标签 定义别名
  protected final TypeAliasRegistry typeAliasRegistry;
  // 在 mybatis-config.xml 中可以通过 <typeHandlers>标签 添加 自定义TypeHandler
  // TypeHandler 用于完成 JDBC数据类型 与 Java类型 的相互转换，所有的 TypeHandler
  // 都保存在 typeHandlerRegistry 中
  protected final TypeHandlerRegistry typeHandlerRegistry;

  public BaseBuilder(Configuration configuration) {
    this.configuration = configuration;
    this.typeAliasRegistry = this.configuration.getTypeAliasRegistry();
    this.typeHandlerRegistry = this.configuration.getTypeHandlerRegistry();
  }
}
```

BaseBuilder 中的 typeAliasRegistry 和 typeHandlerRegistry 字段 均来自于 configuration，通过 BaseBuilder 的构造方法可以看到详细内容。

## 2 XMLConfigBuilder

XMLConfigBuilder 是 BaseBuilder 的众多子类之一，主要负责解析 mybatis-config.xml 配置文件。它通过调用 parseConfiguration()方法 实现整个解析过程，其中，mybatis-config.xml 配置文件 中的每个节点都被封装成了一个个相应的解析方法，parseConfiguration()方法 只是依次调用了这些解析方法而已。

```java
public class XMLConfigBuilder extends BaseBuilder {

  // 标记是否解析过 mybatis-config.xml文件
  private boolean parsed;
  // 用于解析 mybatis-config.xml 的解析器
  private final XPathParser parser;
  // 标识 <environment>配置 的名称，默认读取 <environment>标签 的 default属性
  private String environment;
  // 创建并缓存 Reflector对象
  private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();

  /**
   * 解析的入口，调用了 parseConfiguration() 进行后续的解析
   */
  public Configuration parse() {
    // parsed标志位 的处理
    if (parsed) {
      throw new BuilderException("Each XMLConfigBuilder can only be used once.");
    }
    parsed = true;
    // 在 mybatis-config.xml配置文件 中查找 <configuration>节点，并开始解析
    parseConfiguration(parser.evalNode("/configuration"));
    return configuration;
  }

  private void parseConfiguration(XNode root) {
    try {
      // 根据 root.evalNode("properties") 中的值就可以知道具体是解析哪个标签的方法咯
      propertiesElement(root.evalNode("properties"));
      Properties settings = settingsAsProperties(root.evalNode("settings"));
      loadCustomVfs(settings);
      typeAliasesElement(root.evalNode("typeAliases"));
      pluginElement(root.evalNode("plugins"));
      objectFactoryElement(root.evalNode("objectFactory"));
      objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
      reflectorFactoryElement(root.evalNode("reflectorFactory"));
      settingsElement(settings);
      // read it after objectFactory and objectWrapperFactory issue #631
      environmentsElement(root.evalNode("environments"));
      databaseIdProviderElement(root.evalNode("databaseIdProvider"));
      typeHandlerElement(root.evalNode("typeHandlers"));
      mapperElement(root.evalNode("mappers"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
  }
```

Mybatis 中的标签很多，所以相对应的解析方法也很多，这里挑几个比较重要的标签进行分析。

### 2.1 解析&lt;typeHandlers&gt;标签

```java
  private void typeHandlerElement(XNode parent) throws Exception {
    if (parent != null) {
      // 处理 <typeHandlers> 下的所有子标签
      for (XNode child : parent.getChildren()) {
        // 处理 <package> 标签
        if ("package".equals(child.getName())) {
          // 获取指定的包名
          String typeHandlerPackage = child.getStringAttribute("name");
          // 通过 typeHandlerRegistry 的 register(packageName)方法
          // 扫描指定包中的所有 TypeHandler类，并进行注册
          typeHandlerRegistry.register(typeHandlerPackage);
        } else {
          // Java数据类型
          String javaTypeName = child.getStringAttribute("javaType");
          // JDBC数据类型
          String jdbcTypeName = child.getStringAttribute("jdbcType");
          String handlerTypeName = child.getStringAttribute("handler");
          Class<?> javaTypeClass = resolveClass(javaTypeName);
          JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
          Class<?> typeHandlerClass = resolveClass(handlerTypeName);
          // 注册
          if (javaTypeClass != null) {
            if (jdbcType == null) {
              typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
            } else {
              typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
            }
          } else {
            typeHandlerRegistry.register(typeHandlerClass);
          }
        }
      }
    }
  }
```

### 2.2 解析&lt;environments&gt;标签

```java
  /**
   * Mybatis 可以配置多个 <environment>环境，分别用于开发、测试及生产等，
   * 但每个 SqlSessionFactory实例 只能选择其一
   */
  private void environmentsElement(XNode context) throws Exception {
    if (context != null) {
      // 如果未指定 XMLConfigBuilder 的 environment字段，则使用 default属性 指定的 <environment>环境
      if (environment == null) {
        environment = context.getStringAttribute("default");
      }
      // 遍历 <environment>节点
      for (XNode child : context.getChildren()) {
        String id = child.getStringAttribute("id");
        if (isSpecifiedEnvironment(id)) {
          // 实例化 TransactionFactory
          TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
          // 创建 DataSourceFactory 和 DataSource
          DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
          DataSource dataSource = dsFactory.getDataSource();
          // 创建的 Environment对象 中封装了上面的 TransactionFactory对象 和 DataSource对象
          Environment.Builder environmentBuilder = new Environment.Builder(id)
              .transactionFactory(txFactory)
              .dataSource(dataSource);
          // 为 configuration 注入 environment属性值
          configuration.setEnvironment(environmentBuilder.build());
        }
      }
    }
  }
```

### 2.3 解析&lt;databaseIdProvider&gt;标签

Mybatis 不像 Hibernate 那样，通过 HQL 的方式直接帮助开发人员屏蔽不同数据库产品在 sql 语法 上的差异，针对不同的数据库产品， Mybatis 往往要编写不同的 sql 语句。但在 mybatis-config.xml 配置文件 中，可以通过 &lt;databaseIdProvider&gt; 定义所有支持的数据库产品的 databaseId，然后在映射配置文件中定义 sql 语句节点 时，通过 databaseId 指定该 sql 语句 应用的数据库产品，也可以达到类似的屏蔽数据库产品的功能。

Mybatis 初始化时，会根据前面解析到的 DataSource 来确认当前使用的数据库产品，然后在解析映射文件时，加载不带 databaseId 属性 的 sql 语句 及带有 databaseId 属性 的 sql 语句，其中，带有 databaseId 属性 的 sql 语句 优先级更高，会被优先选中。

```java
  /**
   * 解析 <databaseIdProvider>节点，并创建指定的 DatabaseIdProvider对象，
   * 该对象会返回 databaseId的值，Mybatis 会根据 databaseId 选择对应的 sql语句 去执行
   */
  private void databaseIdProviderElement(XNode context) throws Exception {
    DatabaseIdProvider databaseIdProvider = null;
    if (context != null) {
      String type = context.getStringAttribute("type");
      // 为了保证兼容性，修改 type取值
      if ("VENDOR".equals(type)) {
          type = "DB_VENDOR";
      }
      // 解析相关配置信息
      Properties properties = context.getChildrenAsProperties();
      // 创建 DatabaseIdProvider对象
      databaseIdProvider = (DatabaseIdProvider) resolveClass(type).newInstance();
      // 配置 DatabaseIdProvider，完成初始化
      databaseIdProvider.setProperties(properties);
    }
    Environment environment = configuration.getEnvironment();
    if (environment != null && databaseIdProvider != null) {
      // 根据前面解析到的 DataSource 获取 databaseId，并记录到 configuration 的 configuration属性 上
      String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
      configuration.setDatabaseId(databaseId);
    }
  }
```

Mybatis 提供了 DatabaseIdProvider 接口，该接口的核心方法为 getDatabaseId(DataSource dataSource)，主要根据 dataSource 查找对应的 databaseId 并返回。该接口的主要实现类为 VendorDatabaseIdProvider。

```java
public class VendorDatabaseIdProvider implements DatabaseIdProvider {

  private static final Log log = LogFactory.getLog(VendorDatabaseIdProvider.class);

  private Properties properties;

  @Override
  public void setProperties(Properties p) {
    this.properties = p;
  }

  @Override
  public String getDatabaseId(DataSource dataSource) {
    if (dataSource == null) {
      throw new NullPointerException("dataSource cannot be null");
    }
    try {
      return getDatabaseName(dataSource);
    } catch (Exception e) {
      log.error("Could not get a databaseId from dataSource", e);
    }
    return null;
  }

  private String getDatabaseName(DataSource dataSource) throws SQLException {
    // 解析到数据库产品名
    String productName = getDatabaseProductName(dataSource);
    if (this.properties != null) {
      // 根据 <databaseIdProvider>子节点 配置的数据库产品和 databaseId 之间的对应关系，
      // 确定最终使用的 databaseId
      for (Map.Entry<Object, Object> property : properties.entrySet()) {
        if (productName.contains((String) property.getKey())) {
          return (String) property.getValue();
        }
      }
      // 没有合适的 databaseId，则返回 null
      return null;
    }
    return productName;
  }

  // 根据 dataSource 获取 数据库产品名的具体实现
  private String getDatabaseProductName(DataSource dataSource) throws SQLException {
    Connection con = null;
    try {
      con = dataSource.getConnection();
      DatabaseMetaData metaData = con.getMetaData();
      return metaData.getDatabaseProductName();
    } finally {
      if (con != null) {
        try {
          con.close();
        } catch (SQLException e) {
          // ignored
        }
      }
    }
  }
}
```

### 2.4 解析&lt;mappers&gt;标签

Mybatis 初始化时，除了加载 mybatis-config.xml 文件，还会加载全部的映射配置文件，mybatis-config.xml 文件的 &lt;mapper&gt;节点 会告诉 Mybatis 去哪里查找映射配置文件，及使用了配置注解标识的接口。

```java
  /**
   * 解析 <mappers>节点，本方法会创建 XMLMapperBuilder对象 加载映射文件，如果映射配置文件存在
   * 相应的 Mapper接口，也会加载相应的 Mapper接口，解析其中的注解 并完成向 MapperRegistry 的注册
   */
  private void mapperElement(XNode parent) throws Exception {
    if (parent != null) {
      // 处理 <mappers> 的子节点
      for (XNode child : parent.getChildren()) {
        if ("package".equals(child.getName())) {
          // 获取 <package>子节点 中的包名
          String mapperPackage = child.getStringAttribute("name");
          // 扫描指定的包目录，然后向 MapperRegistry 注册 Mapper接口
          configuration.addMappers(mapperPackage);
        } else {
          // 获取 <mapper>节点 的 resource、url、mapperClass属性，这三个属性互斥，只能有一个不为空
          // Mybatis 提供了通过包名、映射文件路径、类全名、URL 四种方式引入映射器。
          // 映射器由一个接口和一个 XML配置文件 组成，XML文件 中定义了一个 命名空间namespace，
          // 它的值就是接口对应的全路径。
          String resource = child.getStringAttribute("resource");
          String url = child.getStringAttribute("url");
          String mapperClass = child.getStringAttribute("class");
          // 如果 <mapper>节点 指定了 resource 或是 url属性，则创建 XMLMapperBuilder对象 解析
          // resource 或是 url属性 指定的 Mapper配置文件
          if (resource != null && url == null && mapperClass == null) {
            ErrorContext.instance().resource(resource);
            InputStream inputStream = Resources.getResourceAsStream(resource);
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
            mapperParser.parse();
          } else if (resource == null && url != null && mapperClass == null) {
            ErrorContext.instance().resource(url);
            InputStream inputStream = Resources.getUrlAsStream(url);
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
            mapperParser.parse();
          } else if (resource == null && url == null && mapperClass != null) {
            // 如果 <mapper>节点 指定了 class属性，则向 MapperRegistry 注册 该Mapper接口
            Class<?> mapperInterface = Resources.classForName(mapperClass);
            configuration.addMapper(mapperInterface);
          } else {
            throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
          }
        }
      }
    }
  }
```

## 3 XMLMapperBuilder

和 XMLConfigBuilder 一样，XMLMapperBuilder 也继承了 BaseBuilder，其主要负责解析映射配置文件，其解析配置文件的入口方法也是 parse()，另外，XMLMapperBuilder 也将各个节点的解析过程拆分成了一个个小方法，然后由 configurationElement()方法 统一调用。

```java
public class XMLMapperBuilder extends BaseBuilder {
  public void parse() {
    // 是否已经加载过该配置文件
    if (!configuration.isResourceLoaded(resource)) {
      // 解析 <mapper>节点
      configurationElement(parser.evalNode("/mapper"));
      // 将 resource 添加到 configuration 的 loadedResources属性 中，
      // 该属性是一个 HashSet<String>类型的集合，其中记录了已经加载过的映射文件
      configuration.addLoadedResource(resource);
      // 注册 Mapper接口
      bindMapperForNamespace();
    }
    // 处理 configurationElement()方法 中解析失败的 <resultMap>节点
    parsePendingResultMaps();
    // 处理 configurationElement()方法 中解析失败的 <cacheRef>节点
    parsePendingCacheRefs();
    // 处理 configurationElement()方法 中解析失败的 <statement>节点
    parsePendingStatements();
  }

  private void configurationElement(XNode context) {
    try {
      // 获取 <mapper>节点 的 namespace属性
      String namespace = context.getStringAttribute("namespace");
      if (namespace == null || namespace.equals("")) {
        throw new BuilderException("Mapper's namespace cannot be empty");
      }
      // 使用 MapperBuilderAssistant对象 的 currentNamespace属性 记录 namespace命名空间
      builderAssistant.setCurrentNamespace(namespace);
      // 解析 <cache-ref>节点，后面的解析方法 也都见名知意
      cacheRefElement(context.evalNode("cache-ref"));
      cacheElement(context.evalNode("cache"));
      parameterMapElement(context.evalNodes("/mapper/parameterMap"));
      resultMapElements(context.evalNodes("/mapper/resultMap"));
      sqlElement(context.evalNodes("/mapper/sql"));
      buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e, e);
    }
  }
}
```

XMLMapperBuilder 也根据配置文件进行了一系列节点解析，我们着重分析一下比较重要且常见的 &lt;resultMap&gt;节点 和 &lt;sql&gt;节点

### 3.1 解析&lt;resultMap&gt;节点

select 语句 查询得到的结果是一张二维表，水平方向上是一个个字段，垂直方向上是一条条记录。而 Java 是面向对象的程序设计语言，对象是根据类的定义创建的，类之间的引用关系可以认为是嵌套结构。JDBC 编程 中，为了将结果集中的数据映射成 VO 对象，我们需要自己写代码从结果集中获取数据，然后将数据封装成对应的 VO 对象，并设置好对象之间的关系，这种 ORM 的过程中存在大量重复的代码。

Mybatis 通过 &lt;resultMap&gt;节点 定义了 ORM 规则，可以满足大部分的映射需求，减少重复代码，提高开发效率。

在分析 &lt;resultMap&gt;节点 的解析过程之前，先看一下该过程使用的数据结构。每个 ResultMapping 对象 记录了结果集中的一列与 JavaBean 中一个属性之间的映射关系。&lt;resultMap&gt;节点 下除了 &lt;discriminator&gt;子节点 的其它子节点，都会被解析成对应的 ResultMapping 对象。

```java
public class ResultMapping {

  private Configuration configuration;
  // 对应节点的 property属性，表示 该列进行映射的属性
  private String property;
  // 对应节点的 column属性，表示 从数据库中得到的列名或列名的别名
  private String column;
  // 表示 一个 JavaBean 的完全限定名，或一个类型别名
  private Class<?> javaType;
  // 进行映射列的 JDBC类型
  private JdbcType jdbcType;
  // 类型处理器
  private TypeHandler<?> typeHandler;
  // 该属性通过 id 引用了另一个 <resultMap>节点，它负责将结果集中的一部分列映射成
  // 它所关联的结果对象。这样我们就可以通过 join方式 进行关联查询，然后直接映射成
  // 多个对象，并同时设置这些对象之间的组合关系(nested嵌套的)
  private String nestedResultMapId;
  // 该属性通过 id 引用了另一个 <select>节点，它会把指定的列值传入 select属性 指定的
  // select语句 中作为参数进行查询。使用该属性可能会导致 ORM 中的 N+1问题，请谨慎使用
  private String nestedQueryId;
  private Set<String> notNullColumns;
  private String columnPrefix;
  // 处理后的标志，共有两个：id 和 constructor
  private List<ResultFlag> flags;
  private List<ResultMapping> composites;
  private String resultSet;
  private String foreignColumn;
  // 是否延迟加载
  private boolean lazy;
}
```

另一个比较重要的类是 ResultMap，每个 &lt;resultMap&gt;节点 都会被解析成一个 ResultMap 对象，其中每个节点所定义的映射关系，则使用 ResultMapping 对象 表示。

```java
public class ResultMap {
  private Configuration configuration;

  // 这些属性一一对应了 <resultMap> 中的属性
  private String id;
  private Class<?> type;
  // 记录了除 <discriminator>节点 之外的其它映射关系(即，ResultMapping对象集合)
  private List<ResultMapping> resultMappings;
  // 记录了映射关系中带有 ID标志 的映射关系，如：<id>节点 和 <constructor>节点 的 <idArg>子节点
  private List<ResultMapping> idResultMappings;
  // 记录了映射关系中带有 Constructor标志 的映射关系，如：<constructor>所有子元素
  private List<ResultMapping> constructorResultMappings;
  // 记录了映射关系中不带有 Constructor标志 的映射关系
  private List<ResultMapping> propertyResultMappings;
  // 记录了所有映射关系中涉及的 column属性 的集合
  private Set<String> mappedColumns;
  // 记录了所有映射关系中涉及的 property属性 的集合
  private Set<String> mappedProperties;
  // 鉴别器，对应 <discriminator>节点
  private Discriminator discriminator;
  // 是否含有嵌套的结果映射，如果某个映射关系中存在 resultMap属性，
  // 且不存在 resultSet属性，则为true
  private boolean hasNestedResultMaps;
  // 是否含有嵌套查询，如果某个属性映射存在 select属性，则为true
  private boolean hasNestedQueries;
  // 是否开启自动映射
  private Boolean autoMapping;
}
```

了解了 ResultMapping 和 ResultMap 记录的信息之后，下面开始介绍 &lt;resultMap&gt;节点 的解析过程。在 XMLMapperBuilder 中通过 resultMapElements()方法 解析映射配置文件中的全部 &lt;resultMap&gt;节点，该方法会循环调用 resultMapElement()方法 处理每个 &lt;resultMap&gt; 节点。

```java
  private ResultMap resultMapElement(XNode resultMapNode) throws Exception {
    return resultMapElement(resultMapNode, Collections.<ResultMapping> emptyList());
  }

  private ResultMap resultMapElement(XNode resultMapNode, List<ResultMapping> additionalResultMappings) throws Exception {
    ErrorContext.instance().activity("processing " + resultMapNode.getValueBasedIdentifier());
    // <resultMap> 的 id属性，默认值会拼装所有父节点的 id 或 value 或 property属性值
    String id = resultMapNode.getStringAttribute("id",
        resultMapNode.getValueBasedIdentifier());
    // <resultMap> 的 type属性，表示结果集将被映射成 type 指定类型的对象
    String type = resultMapNode.getStringAttribute("type",
        resultMapNode.getStringAttribute("ofType",
            resultMapNode.getStringAttribute("resultType",
                resultMapNode.getStringAttribute("javaType"))));
    // 该属性指定了该 <resultMap>节点 的继承关系
    String extend = resultMapNode.getStringAttribute("extends");
    // 为 true 则启动自动映射功能，该功能会自动查找与列明相同的属性名，并调用 setter方法，
    // 为 false，则需要在 <resultMap>节点 内注明映射关系才会调用对应的 setter方法
    Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");
    // 解析 type类型
    Class<?> typeClass = resolveClass(type);
    Discriminator discriminator = null;
    // 该集合用来记录解析结果
    List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
    resultMappings.addAll(additionalResultMappings);
    // 获取并处理 <resultMap> 的子节点
    List<XNode> resultChildren = resultMapNode.getChildren();
    // child 单数形式，children 复数形式
    for (XNode resultChild : resultChildren) {
      // 处理 <constructor>节点
      if ("constructor".equals(resultChild.getName())) {
        processConstructorElement(resultChild, typeClass, resultMappings);
      // 处理 <discriminator>节点
      } else if ("discriminator".equals(resultChild.getName())) {
        discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
      } else {
        // 处理 <id>, <result>, <association>, <collection> 等节点
        List<ResultFlag> flags = new ArrayList<ResultFlag>();
        if ("id".equals(resultChild.getName())) {
          flags.add(ResultFlag.ID);
        }
        // 创建 ResultMapping对象，并添加到 resultMappings集合
        resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
      }
    }
    ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend, discriminator, resultMappings, autoMapping);
    try {
      return resultMapResolver.resolve();
    } catch (IncompleteElementException  e) {
      configuration.addIncompleteResultMap(resultMapResolver);
      throw e;
    }
  }
```

从上面的代码我们可以看到，Mybatis 从 &lt;resultMap&gt;节点 获取到 id 属性 和 type 属性值 之后，就会通过 XMLMapperBuilder 的 buildResultMappingFromContext()方法 为 &lt;result&gt;节点 创建对应的 ResultMapping 对象。

```java
  /**
   * 根据上下文环境构建 ResultMapping
   */
  private ResultMapping buildResultMappingFromContext(XNode context, Class<?> resultType, List<ResultFlag> flags) throws Exception {
    // 获取各个节点的属性，见文知意
    String property;
    if (flags.contains(ResultFlag.CONSTRUCTOR)) {
      property = context.getStringAttribute("name");
    } else {
      property = context.getStringAttribute("property");
    }
    String column = context.getStringAttribute("column");
    String javaType = context.getStringAttribute("javaType");
    String jdbcType = context.getStringAttribute("jdbcType");
    String nestedSelect = context.getStringAttribute("select");
    String nestedResultMap = context.getStringAttribute("resultMap",
        processNestedResultMappings(context, Collections.<ResultMapping> emptyList()));
    String notNullColumn = context.getStringAttribute("notNullColumn");
    String columnPrefix = context.getStringAttribute("columnPrefix");
    String typeHandler = context.getStringAttribute("typeHandler");
    String resultSet = context.getStringAttribute("resultSet");
    String foreignColumn = context.getStringAttribute("foreignColumn");
    boolean lazy = "lazy".equals(context.getStringAttribute("fetchType", configuration.isLazyLoadingEnabled() ? "lazy" : "eager"));
    Class<?> javaTypeClass = resolveClass(javaType);
    @SuppressWarnings("unchecked")
    Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
    // 创建 ResultMapping对象 并返回
    return builderAssistant.buildResultMapping(resultType, property, column, javaTypeClass, jdbcTypeEnum, nestedSelect, nestedResultMap, notNullColumn, columnPrefix, typeHandlerClass, flags, resultSet, foreignColumn, lazy);
  }
```

得到 ResultMapping 对象集合 之后，会调用 ResultMapResolver 的 resolve()方法，该方法会调用 MapperBuilderAssistant 的 addResultMap()方法 创建 ResultMap 对象，并将 ResultMap 对象 添加到 Configuration 的 resultMaps 集合 中保存。

```java
public class MapperBuilderAssistant extends BaseBuilder {
  public ResultMap addResultMap(String id, Class<?> type, String extend,
      Discriminator discriminator, List<ResultMapping> resultMappings, Boolean autoMapping) {
    // ResultMap 的 完整id 是 "namespace.id" 的格式
    id = applyCurrentNamespace(id, false);
    // 获取 父ResultMap 的 完整id
    extend = applyCurrentNamespace(extend, true);

    // 针对 extend属性 进行的处理
    if (extend != null) {
      if (!configuration.hasResultMap(extend)) {
        throw new IncompleteElementException("Could not find a parent resultmap with id '" + extend + "'");
      }
      // 父ResultMap对象
      ResultMap resultMap = configuration.getResultMap(extend);
      // 父ResultMap对象 的 ResultMapping集合
      List<ResultMapping> extendedResultMappings = new ArrayList<ResultMapping>(resultMap.getResultMappings());
      // 删除需要覆盖的 ResultMapping集合
      extendedResultMappings.removeAll(resultMappings);
      // Remove parent constructor if this resultMap declares a constructor.
      boolean declaresConstructor = false;
      for (ResultMapping resultMapping : resultMappings) {
        if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
          declaresConstructor = true;
          break;
        }
      }
      if (declaresConstructor) {
        Iterator<ResultMapping> extendedResultMappingsIter = extendedResultMappings.iterator();
        while (extendedResultMappingsIter.hasNext()) {
          if (extendedResultMappingsIter.next().getFlags().contains(ResultFlag.CONSTRUCTOR)) {
            extendedResultMappingsIter.remove();
          }
        }
      }
      // 添加需要被继承下来的 ResultMapping集合
      resultMappings.addAll(extendedResultMappings);
    }
    ResultMap resultMap = new ResultMap.Builder(configuration, id, type, resultMappings, autoMapping)
        .discriminator(discriminator)
        .build();
    configuration.addResultMap(resultMap);
    return resultMap;
  }
}
```

### 3.2 解析&lt;sql&gt;节点

在映射配置文件中，可以使用 &lt;sql&gt;节点 定义可重用的 SQL 语句片段，当需要重用 &lt;sql&gt;节点 中定义的 SQL 语句片段 时，只需要使用 &lt;include&gt;节点 引入相应的片段即可，这样，在编写 SQL 语句 以及维护这些 SQL 语句 时，都会比较方便。XMLMapperBuilder 的 sqlElement()方法 负责解析映射配置文件中定义的 全部&lt;sql&gt;节点。

```java
  private void sqlElement(List<XNode> list) throws Exception {
    if (configuration.getDatabaseId() != null) {
      sqlElement(list, configuration.getDatabaseId());
    }
    sqlElement(list, null);
  }

  private void sqlElement(List<XNode> list, String requiredDatabaseId) throws Exception {
    // 遍历 <sql>节点
    for (XNode context : list) {
      String databaseId = context.getStringAttribute("databaseId");
      String id = context.getStringAttribute("id");
      // 为 id 添加命名空间
      id = builderAssistant.applyCurrentNamespace(id, false);
      // 检测 <sql> 的 databaseId 与当前 Configuration 中记录的 databaseId 是否一致
      if (databaseIdMatchesCurrent(id, databaseId, requiredDatabaseId)) {
        // 记录到 sqlFragments(Map<String, XNode>) 中保存
        sqlFragments.put(id, context);
      }
    }
  }
```

## 4 XMLStatementBuilder

## 5 绑定 Mapper 接口

通过之前对 binding 模块 的解析可知，每个映射配置文件的命名空间可以绑定一个 Mapper 接口，并注册到 MapperRegistry 中。XMLMapperBuilder 的 bindMapperForNamespace()方法 中，完成了映射配置文件与对应 Mapper 接口 的绑定。

```java
public class XMLMapperBuilder extends BaseBuilder {
  private void bindMapperForNamespace() {
    // 获取映射配置文件的命名空间
    String namespace = builderAssistant.getCurrentNamespace();
    if (namespace != null) {
      Class<?> boundType = null;
      try {
        // 解析命名空间对应的类型
        boundType = Resources.classForName(namespace);
      } catch (ClassNotFoundException e) {
        //ignore, bound type is not required
      }
      if (boundType != null) {
        // 是否已加载 boundType接口
        if (!configuration.hasMapper(boundType)) {
          // 追加个 "namespace:" 的前缀，并添加到 Configuration 的 loadedResources集合 中
          configuration.addLoadedResource("namespace:" + namespace);
          // 添加到 Configuration的mapperRegistry集合 中，另外，往这个方法栈的更深处看 会发现
          // 其创建了 MapperAnnotationBuilder对象，并调用了该对象的 parse()方法 解析 Mapper接口
          configuration.addMapper(boundType);
        }
      }
    }
  }
}

public class MapperRegistry {
  public <T> void addMapper(Class<T> type) {
    if (type.isInterface()) {
      if (hasMapper(type)) {
        throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
      }
      boolean loadCompleted = false;
      try {
        knownMappers.put(type, new MapperProxyFactory<T>(type));
        // 解析 Mapper接口 type 中的信息
        MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
        parser.parse();
        loadCompleted = true;
      } finally {
        if (!loadCompleted) {
          knownMappers.remove(type);
        }
      }
    }
  }
}

public class MapperAnnotationBuilder {
  public void parse() {
    String resource = type.toString();
    // 是否已经加载过该接口
    if (!configuration.isResourceLoaded(resource)) {
      // 检查是否加载过该接口对应的映射文件，如果未加载，则创建 XMLMapperBuilder对象
      // 解析对应的映射文件，该过程就是前面介绍的映射配置文件解析过程
      loadXmlResource();
      configuration.addLoadedResource(resource);
      assistant.setCurrentNamespace(type.getName());
      // 解析 @CacheNamespace注解
      parseCache();
      // 解析 @CacheNamespaceRef注解
      parseCacheRef();
      // type接口 的所有方法
      Method[] methods = type.getMethods();
      for (Method method : methods) {
        try {
          if (!method.isBridge()) {
            // 解析 SelectKey、ResultMap 等注解，并创建 MappedStatement对象
            parseStatement(method);
          }
        } catch (IncompleteElementException e) {
          // 如果解析过程出现 IncompleteElementException异常，可能是因为引用了
          // 未解析的注解，这里将出现异常的方法记录下来，后面提供补偿机制，重新进行解析
          configuration.addIncompleteMethod(new MethodResolver(this, method));
        }
      }
    }
    // 遍历 configuration 中的 incompleteMethods集合，集合中记录了未解析的方法
    // 重新调用这些方法进行解析
    parsePendingMethods();
  }
}
```

另外，在 MapperAnnotationBuilder 的 parse()方法 中解析的注解，都能在映射配置文件中找到与之对应的 XML 节点，且两者的解析过程也非常相似。

StatementHandler 接口是 MyBatis 的核心接口之一，它完成了 MyBatis 中最核心的工作，也是 Executor 接口实现的基础。

StatementHandler 接口中的功能很多，例如创建 Statement 对象，为 SQL 语句绑定实参，执行 select、insert、update、delete 等多种类型的 SQL 语句，批量执行 SQL 语句，将结果集映射成结果对象。

```java
public interface StatementHandler {

  // 从连接中获取一个Statement
  Statement prepare(Connection connection, Integer transactionTimeout)
      throws SQLException;

  // 绑定statement执行时所需的实参
  void parameterize(Statement statement)
      throws SQLException;

  // 批量执行SQL语句
  void batch(Statement statement)
      throws SQLException;

  // 执行update/insert/delete语句
  int update(Statement statement)
      throws SQLException;

  // 执行select语句
  <E> List<E> query(Statement statement, ResultHandler resultHandler)
      throws SQLException;

  <E> Cursor<E> queryCursor(Statement statement)
      throws SQLException;

  BoundSql getBoundSql();

  // 获取参数处理器
  ParameterHandler getParameterHandler();

}
```

# RoutingStatementHandler

RoutingStatementHandler 使用了策略模式，RoutingStatementHandler 是策略类，而 SimpleStatementHandler、PreparedStatementHandler、CallableStatementHandler 则是实现了具体算法的实现类，RoutingStatementHandler 对象会根据 MappedStatement 对象的 StatementType 属性值选择使用相应的策略去执行。

```java
public class RoutingStatementHandler implements StatementHandler {

  // 持有的真正实现StatementHandler接口功能的对象
  private final StatementHandler delegate;

  public RoutingStatementHandler(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    // RoutingStatementHandler的作用就是根据ms的配置，生成一个相对应的StatementHandler对象
    // 并设置到持有的delegate属性中，本对象的所有方法都是通过调用delegate的相应方法实现的
    switch (ms.getStatementType()) {
      case STATEMENT:
        delegate = new SimpleStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        break;
      case PREPARED:
        delegate = new PreparedStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        break;
      case CALLABLE:
        delegate = new CallableStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        break;
      default:
        throw new ExecutorException("Unknown statement type: " + ms.getStatementType());
    }

  }

  @Override
  public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
    return delegate.prepare(connection, transactionTimeout);
  }

  @Override
  public void parameterize(Statement statement) throws SQLException {
    delegate.parameterize(statement);
  }

  @Override
  public void batch(Statement statement) throws SQLException {
    delegate.batch(statement);
  }

  @Override
  public int update(Statement statement) throws SQLException {
    return delegate.update(statement);
  }

  @Override
  public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
    return delegate.query(statement, resultHandler);
  }

  @Override
  public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
    return delegate.queryCursor(statement);
  }

  @Override
  public BoundSql getBoundSql() {
    return delegate.getBoundSql();
  }

  @Override
  public ParameterHandler getParameterHandler() {
    return delegate.getParameterHandler();
  }
}
```

## BaseStatementHandler

看它以 Base 开头，就可以猜到 它是一个实现了 StatementHandler 接口的抽象类，这个类只提供了一些参数绑定相关的方法，并没有实现操作数据库的方法。

```java
public abstract class BaseStatementHandler implements StatementHandler {

  // 持有的这些属性都是通过构造方法完成初始化的，typeHandlerRegistry、
  // objectFactory、parameterHandler等则是通过configuration属性获得的
  protected final Configuration configuration;
  protected final ObjectFactory objectFactory;
  protected final TypeHandlerRegistry typeHandlerRegistry;
  protected final ResultSetHandler resultSetHandler;
  // parameterHandler的功能主要是为SQL语句绑定实参，也就是使用传入的实参
  // 替换SQL语句中的占位符"?"
  protected final ParameterHandler parameterHandler;

  // 用来执行SQL语句的执行器
  protected final Executor executor;
  protected final MappedStatement mappedStatement;
  // 记录了用户设置的offset和limit，用于在结果集中定位
  // 映射的起始位置和结束位置
  protected final RowBounds rowBounds;

  protected BoundSql boundSql;

  // BaseStatementHandler的构造方法主要用于属性的初始化
  protected BaseStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    this.configuration = mappedStatement.getConfiguration();
    this.executor = executor;
    this.mappedStatement = mappedStatement;
    this.rowBounds = rowBounds;

    this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
    this.objectFactory = configuration.getObjectFactory();

    if (boundSql == null) { // issue #435, get the key before calculating the statement
      // 其中调用了KeyGenerator的processBefore()方法
      // 用于初始化SQL语句的主键
      generateKeys(parameterObject);
      boundSql = mappedStatement.getBoundSql(parameterObject);
    }

    this.boundSql = boundSql;

    this.parameterHandler = configuration.newParameterHandler(mappedStatement, parameterObject, boundSql);
    this.resultSetHandler = configuration.newResultSetHandler(executor, mappedStatement, rowBounds, parameterHandler, resultHandler, boundSql);
  }

  protected void generateKeys(Object parameter) {
    KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
    ErrorContext.instance().store();
    keyGenerator.processBefore(executor, mappedStatement, null, parameter);
    ErrorContext.instance().recall();
  }

  @Override
  public BoundSql getBoundSql() {
    return boundSql;
  }

  @Override
  public ParameterHandler getParameterHandler() {
    return parameterHandler;
  }

  @Override
  public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
    ErrorContext.instance().sql(boundSql.getSql());
    Statement statement = null;
    try {
      // 这是一个抽象方法，用于初始化java.sql.Statement对象
      statement = instantiateStatement(connection);
      // 为Statement对象设置超时时间及fetchSize
      setStatementTimeout(statement, transactionTimeout);
      setFetchSize(statement);
      return statement;
    } catch (SQLException e) {
      closeStatement(statement);
      throw e;
    } catch (Exception e) {
      closeStatement(statement);
      throw new ExecutorException("Error preparing statement.  Cause: " + e, e);
    }
  }

  protected abstract Statement instantiateStatement(Connection connection) throws SQLException;

  protected void setStatementTimeout(Statement stmt, Integer transactionTimeout) throws SQLException {
    Integer queryTimeout = null;
    if (mappedStatement.getTimeout() != null) {
      queryTimeout = mappedStatement.getTimeout();
    } else if (configuration.getDefaultStatementTimeout() != null) {
      queryTimeout = configuration.getDefaultStatementTimeout();
    }
    if (queryTimeout != null) {
      stmt.setQueryTimeout(queryTimeout);
    }
    StatementUtil.applyTransactionTimeout(stmt, queryTimeout, transactionTimeout);
  }

  protected void setFetchSize(Statement stmt) throws SQLException {
    Integer fetchSize = mappedStatement.getFetchSize();
    if (fetchSize != null) {
      stmt.setFetchSize(fetchSize);
      return;
    }
    Integer defaultFetchSize = configuration.getDefaultFetchSize();
    if (defaultFetchSize != null) {
      stmt.setFetchSize(defaultFetchSize);
    }
  }

  protected void closeStatement(Statement statement) {
    try {
      if (statement != null) {
        statement.close();
      }
    } catch (SQLException e) {
      //ignore
    }
  }

}
```

BaseStatementHandler 主要实现了 StatementHandler 接口中的 prepare()方法，BaseStatementHandler 依赖两个重要的组件，ParameterHandler 和 ResultSetHandler。

## ParameterHandler 系列组件

我们要执行的 SQL 语句中可能包含占位符"?"，而每个"?"都对应了 BoundSql 中 parameterMappings 集合中的一个元素，在该 ParameterMapping 对象中记录了对应的参数名称以及该参数的相关属性。ParameterHandler 接口定义了一个非常重要的方法 setParameters()，该方法主要负责调用 PreparedStatement 的 set＊()系列方法，为 SQL 语句绑定实参。MyBatis 只为 ParameterHandler 接口提供了唯一一个实现类 DefaultParameterHandler。

```java
public interface ParameterHandler {

  // 获取用户传入的实参对象
  Object getParameterObject();

  // 本方法主要负责调用PreparedStatement.set*()方法，为SQL语句绑定实参。
  void setParameters(PreparedStatement ps)
      throws SQLException;

}


public class DefaultParameterHandler implements ParameterHandler {

  // 管理mybatis中所有的TypeHandler对象
  private final TypeHandlerRegistry typeHandlerRegistry;

  // 其中记录了SQL节点相应的配置信息
  private final MappedStatement mappedStatement;
  // 用户传入的实参对象
  private final Object parameterObject;
  // 其中记录了要执行的SQL语句，及参数信息
  private final BoundSql boundSql;
  private final Configuration configuration;

  // 构造方法主要为持有的属性 进行初始化
  public DefaultParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
    this.mappedStatement = mappedStatement;
    this.configuration = mappedStatement.getConfiguration();
    this.typeHandlerRegistry = mappedStatement.getConfiguration().getTypeHandlerRegistry();
    this.parameterObject = parameterObject;
    this.boundSql = boundSql;
  }

  @Override
  public Object getParameterObject() {
    return parameterObject;
  }

  // 为PreparedStatement对象要执行的SQL语句中的占位符 设置对应的参数值
  @Override
  public void setParameters(PreparedStatement ps) {
    ErrorContext.instance().activity("setting parameters").object(mappedStatement.getParameterMap().getId());
    // 获取参数列表
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    if (parameterMappings != null) {
      for (int i = 0; i < parameterMappings.size(); i++) {
        ParameterMapping parameterMapping = parameterMappings.get(i);
        // 过滤掉存储过程中的输出参数
        if (parameterMapping.getMode() != ParameterMode.OUT) {
          // 记录绑定的实参
          Object value;
          // 获取参数对应的属性名
          String propertyName = parameterMapping.getProperty();
          // 根据属性名 获取 实参值
          if (boundSql.hasAdditionalParameter(propertyName)) { // issue #448 ask first for additional params
            value = boundSql.getAdditionalParameter(propertyName);
          // 整个实参为空
          } else if (parameterObject == null) {
            value = null;
          // 如果实参可以直接通过TypeHandler转换成JdbcType
          } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
            value = parameterObject;
          } else {
            // 获取对象中相应的属性值 或查找Map对象中的值
            MetaObject metaObject = configuration.newMetaObject(parameterObject);
            value = metaObject.getValue(propertyName);
          }
          // 获取当前parameterMapping中的TypeHandler对象 及JdbcType对象
          TypeHandler typeHandler = parameterMapping.getTypeHandler();
          JdbcType jdbcType = parameterMapping.getJdbcType();
          if (value == null && jdbcType == null) {
            jdbcType = configuration.getJdbcTypeForNull();
          }
          try {
            // TypeHandler的setParameter()方法会调用PreparedStatement对象的
            // set*()系列方法，为SQL语句绑定相应的实参
            typeHandler.setParameter(ps, i + 1, value, jdbcType);
          } catch (TypeException | SQLException e) {
            throw new TypeException("Could not set parameters for mapping: " + parameterMapping + ". Cause: " + e, e);
          }
        }
      }
    }
  }

}
```

为 SQL 语句绑定完实参之后，就可以调用 Statement 对象 相应的 execute 方法，将 SQL 语句交给数据库执行了。

## SimpleStatementHandler

SimpleStatementHandler 继承了 BaseStatementHandler 抽象类。其底层使用 java.sql.Statement 来完成数据库的相关操作，所以 SQL 语句中不存在占位符，所以 SimpleStatementHandler 的 parameterize()方法是空实现。SimpleStatementHandler 的 instantiateStatement()方法直接通过 JDBC Connection 创建 Statement 对象。

```java
public class SimpleStatementHandler extends BaseStatementHandler {

  // 构造方法主要用于属性的初始化
  public SimpleStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
  }

  // 直接通过Connection创建Statement对象
  @Override
  protected Statement instantiateStatement(Connection connection) throws SQLException {
    if (mappedStatement.getResultSetType() == ResultSetType.DEFAULT) {
      // 如果结果集类型是DEFAULT默认的，则直接用connection创建Statement对象
      return connection.createStatement();
    } else {
      // 否则，设置结果集类型，设置结果集 只读
      return connection.createStatement(mappedStatement.getResultSetType().getValue(), ResultSet.CONCUR_READ_ONLY);
    }
  }

  // 上面创建的Statement对象会被本方法用于完成数据库查询操作
  @Override
  public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
    // 获取SQL语句
    String sql = boundSql.getSql();
    // 发送请求 执行SQL语句
    statement.execute(sql);
    // 从statement中获取结果集，并进行映射处理
    return resultSetHandler.handleResultSets(statement);
  }

  // 下面的batch()及queryCursor()方法的实现与上面的query()方法非常类似
  @Override
  public void batch(Statement statement) throws SQLException {
    String sql = boundSql.getSql();
    statement.addBatch(sql);
  }

  @Override
  public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
    String sql = boundSql.getSql();
    statement.execute(sql);
    return resultSetHandler.handleCursorResultSets(statement);
  }

  // 本方法用于执行insert、delete、update等类型的SQL语句，并且会根据配置的
  // KeyGenerator获取数据库生成的主键
  @Override
  public int update(Statement statement) throws SQLException {
    // 获取SQL语句 及parameterObject
    String sql = boundSql.getSql();
    Object parameterObject = boundSql.getParameterObject();
    // 获取配置的KeyGenerator 数据库主键生成器
    KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
    int rows;
    if (keyGenerator instanceof Jdbc3KeyGenerator) {
      // 执行SQL语句
      statement.execute(sql, Statement.RETURN_GENERATED_KEYS);
      // 获取更新的条数
      rows = statement.getUpdateCount();
      // 将数据库生成的主键添加到parameterObject中
      keyGenerator.processAfter(executor, mappedStatement, statement, parameterObject);
    } else if (keyGenerator instanceof SelectKeyGenerator) {
      // 执行SQL语句
      statement.execute(sql);
      // 获取更新的条数
      rows = statement.getUpdateCount();
      // 执行<selectKey>节点中配置的SQL语句，将从数据库获取到的主键 添加到parameterObject中
      keyGenerator.processAfter(executor, mappedStatement, statement, parameterObject);
    } else {
      statement.execute(sql);
      rows = statement.getUpdateCount();
    }
    return rows;
  }

  @Override
  public void parameterize(Statement statement) {
    // N/A
  }

}
```

## PreparedStatementHandler

PreparedStatementHandler 底层依赖于 java.sql.PreparedStatement 来完成数据库的相关操作。其中的 parameterize()方法中，会调用前面介绍的 ParameterHandler 的 setParameters()方法 完成 SQL 语句的参数绑定。instantiateStatement()方法直接调用 JDBC Connection 的 prepareStatement()方法创建 PreparedStatement 对象。

```java
public class PreparedStatementHandler extends BaseStatementHandler {

  // 构造方法主要用于属性的初始化
  public PreparedStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
  }

  @Override
  protected Statement instantiateStatement(Connection connection) throws SQLException {
    // 获取SQL语句
    String sql = boundSql.getSql();
    // 根据mappedStatement持有的KeyGenerator的类型进行不同的处理
    if (mappedStatement.getKeyGenerator() instanceof Jdbc3KeyGenerator) {
      // 获取主键列
      String[] keyColumnNames = mappedStatement.getKeyColumns();
      if (keyColumnNames == null) {
        // 返回数据库生成的主键
        return connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
      } else {
        // 在insert语句执行完后，会将keyColumnNames指定的列返回
        return connection.prepareStatement(sql, keyColumnNames);
      }
    } else if (mappedStatement.getResultSetType() == ResultSetType.DEFAULT) {
      // 如果结果集类型是DEFAULT默认的，则直接通过connection获取PreparedStatement对象
      return connection.prepareStatement(sql);
    } else {
      // 否则，设置结果集类型，设置结果集为只读
      return connection.prepareStatement(sql, mappedStatement.getResultSetType().getValue(), ResultSet.CONCUR_READ_ONLY);
    }
  }

  // 因为是PrepareStatement对象，所以需要处理占位符"?"
  // 使用了前面介绍的ParameterHandler组件完成
  @Override
  public void parameterize(Statement statement) throws SQLException {
    parameterHandler.setParameters((PreparedStatement) statement);
  }

  // 下面的这些方法，除了多了一步 将Statement对象强转成PreparedStatement对象
  // 其它的几乎与SimpleStatementHandler一样
  @Override
  public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
    PreparedStatement ps = (PreparedStatement) statement;
    ps.execute();
    return resultSetHandler.handleResultSets(ps);
  }

  @Override
  public void batch(Statement statement) throws SQLException {
    PreparedStatement ps = (PreparedStatement) statement;
    ps.addBatch();
  }

  @Override
  public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
    PreparedStatement ps = (PreparedStatement) statement;
    ps.execute();
    return resultSetHandler.handleCursorResultSets(ps);
  }

  @Override
  public int update(Statement statement) throws SQLException {
    PreparedStatement ps = (PreparedStatement) statement;
    ps.execute();
    int rows = ps.getUpdateCount();
    Object parameterObject = boundSql.getParameterObject();
    KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
    keyGenerator.processAfter(executor, mappedStatement, ps, parameterObject);
    return rows;
  }

}
```

另外，StatementHandler 接口还有一个 CallableStatementHandler 的实现。其底层依赖于 java.sql.CallableStatement 调用指定的存储过程，其 parameterize()方法也会调用 ParameterHandler 的 setParameters()方法完成 SQL 语句的参数绑定，并指定输出参数的索引位置和 JDBC 类型。其余方法与前面介绍的 ResultSetHandler 实现类似，唯一区别是会调用 ResultSetHandler 的 handleOutputParameters()方法 处理输出参数。

看到这里，我们可以发现 StatementHandler 组件依赖 ParameterHandler 组件 和 ResultSetHandler 组件 完成了 MyBatis 的核心功能，它控制着参数绑定、SQL 语句执行、结果集映射等一系列核心流程。

# Executor组件

Executor 是 MyBatis 的核心接口之一，其中定义了数据库操作的基本方法。在实际应用中经常涉及的 SqISession 接口的功能，都是基于 Executor 接口实现的。

```java
public interface Executor {
  ResultHandler NO_RESULT_HANDLER = null;

  // 执行update、insert、delete三种类型的SQL语句
  int update(MappedStatement ms, Object parameter) throws SQLException;

  // 执行select类型的SQL语句，返回值分为结果对象列表或游标对象
  <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql) throws SQLException;

  <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException;

  <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException;

  // 批量执行SQL语句
  List<BatchResult> flushStatements() throws SQLException;

  // 提交事务
  void commit(boolean required) throws SQLException;

  // 回滚事务
  void rollback(boolean required) throws SQLException;

  // 创建缓存中用到的CacheKey对象
  CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql);

  // 根据CacheKey对象查找缓存
  boolean isCached(MappedStatement ms, CacheKey key);

  // 清空一级缓存
  void clearLocalCache();

  // 延迟加载一级缓存中的数据
  void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType);

  // 获取事务
  Transaction getTransaction();

  // 关闭事务
  void close(boolean forceRollback);

  // 是否关闭
  boolean isClosed();
}
```

## 1 BaseExecutor

BaseExecutor 是一个实现了 Executor 接口的抽象类，它实现了 Executor 接口的大部分方法。BaseExecutor 中主要提供了缓存管理和事务管理的基本功能，继承 BaseExecutor 的子类只要实现四个基本方法来完成数据库的相关操作即可，这四个方法分别是：doUpdate()方法、doQuery()方法、doQueryCursor()方法、doFlushStatement()方法。

```java
public abstract class BaseExecutor implements Executor {

  private static final Log log = LogFactory.getLog(BaseExecutor.class);

  // 事务对象，用于实现事务的提交、回滚和关闭
  protected Transaction transaction;
  // 其中封装的Executor对象
  protected Executor wrapper;

  // 延迟加载队列
  protected ConcurrentLinkedQueue<DeferredLoad> deferredLoads;
  // 一级缓存，用于缓存该Executor对象查询结果集映射得到的结果对象
  protected PerpetualCache localCache;
  // 一级缓存，用于缓存输出类型的参数
  protected PerpetualCache localOutputParameterCache;
  protected Configuration configuration;

  // 记录嵌套查询的层数
  protected int queryStack;
  // 是否关闭
  private boolean closed;
}
```

### 1.1 一级缓存简介

常见的应用系统中，数据库是比较珍贵的资源，很容易成为整个系统的瓶颈。在设计和维护系统时，会进行多方面的权衡，并且利用多种优化手段，减少对数据库的直接访问。

使用缓存是一种比较有效的优化手段，使用缓存可以减少应用系统与数据库的网络交互、减少数据库访问次数、降低数据库的负担、降低重复创建和销毁对象等一系列开销，从而提高整个系统的性能。

MyBatis 提供的缓存功能，分别为一级缓存和二级缓存。BaseExecutor 主要实现了一级缓存的相关内容。一级缓存是会话级缓存，在 MyBatis 中每创建一个 SqlSession 对象，就表示开启一次数据库会话。在一次会话中，应用程序可能会在短时间内(一个事务内)，反复执行完全相同的查询语句，如果不对数据进行缓存，那么每一次查询都会执行一次数据库查询操作，而多次完全相同的、时间间隔较短的查询语句得到的结果集极有可能完全相同，这会造成数据库资源的浪费。

为了避免上述问题，MyBatis 会在 Executor 对象中建立一个简单的一级缓存，将每次查询的结果集缓存起来。在执行查询操作时，会先查询一级缓存，如果存在完全一样的查询情况，则直接从一级缓存中取出相应的结果对象并返回给用户，减少数据库访问次数，从而减小了数据库的压力。

一级缓存的生命周期与 SqlSession 相同，其实也就与 SqISession 中封装的 Executor 对象的生命周期相同。当调用 Executor 对象的 close()方法时（断开连接），该 Executor 对象对应的一级缓存就会被废弃掉。一级缓存中对象的存活时间受很多方面的影响，例如，在调用 Executor 的 update()方法时，也会先请空一级缓存。一级缓存默认是开启的，一般情况下，不需要用户进行特殊配置。

### 1.2 一级缓存的管理

BaseExecutor 的 query()方法会首先创建 CacheKey 对象，并根据该 CacheKey 对象查找一级缓存，如果缓存命中则返回缓存中记录的结果对象，如果缓存未命中则查询数据库得到结果集，之后将结果集映射成结果对象并保存到一级缓存中，同时返回结果对象。

```java
public abstract class BaseExecutor implements Executor {
  @Override
  public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
    // 获取BoundSql对象
    BoundSql boundSql = ms.getBoundSql(parameter);
    // 创建CacheKey对象，该对象由多个参数组装而成
    CacheKey key = createCacheKey(ms, parameter, rowBounds, boundSql);
    // query方法的重载，进行后续处理
    return query(ms, parameter, rowBounds, resultHandler, key, boundSql);
 }

  @Override
  public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
    if (closed) {
      throw new ExecutorException("Executor was closed.");
    }
    // 可以看到CacheKey对象由MappedStatement的id、RowBounds的offset和limit
    // sql语句(包含占位符"?")、用户传递的实参组成
    CacheKey cacheKey = new CacheKey();
    cacheKey.update(ms.getId());
    cacheKey.update(rowBounds.getOffset());
    cacheKey.update(rowBounds.getLimit());
    cacheKey.update(boundSql.getSql());
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    TypeHandlerRegistry typeHandlerRegistry = ms.getConfiguration().getTypeHandlerRegistry();
    // 获取用户传入的实参，并添加到CacheKey对象中
    for (ParameterMapping parameterMapping : parameterMappings) {
      // 过滤掉输出类型的参数
      if (parameterMapping.getMode() != ParameterMode.OUT) {
        Object value;
        String propertyName = parameterMapping.getProperty();
        if (boundSql.hasAdditionalParameter(propertyName)) {
          value = boundSql.getAdditionalParameter(propertyName);
        } else if (parameterObject == null) {
          value = null;
        } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
          value = parameterObject;
        } else {
          MetaObject metaObject = configuration.newMetaObject(parameterObject);
          value = metaObject.getValue(propertyName);
        }
        // 将实参添加到CacheKey对象中
        cacheKey.update(value);
      }
    }
    // 如果configuration的environment不为空，则将该environment的id
    // 添加到CacheKey对象中
    if (configuration.getEnvironment() != null) {
      cacheKey.update(configuration.getEnvironment().getId());
    }
    return cacheKey;
  }

  @Override
  public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
    // 检查当前Executor是否已关闭
    ErrorContext.instance().resource(ms.getResource()).activity("executing a query").object(ms.getId());
    if (closed) {
      throw new ExecutorException("Executor was closed.");
    }
    if (queryStack == 0 && ms.isFlushCacheRequired()) {
      // 非嵌套查询，且<select>节点配置的flushCache属性为true时，才会清空一级缓存
      clearLocalCache();
    }
    List<E> list;
    try {
      // 增加查询层数
      queryStack++;
      // 根据传入的CacheKey对象 查询一级缓存
      list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
      if (list != null) {
        // 针对存储过程调用的处理，在一级缓存命中时，获取缓存中保存的输出类型参数
        // 并设置到用户传入的实参parameter对象中
        handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
      } else {
        // 缓存未命中，则从数据库查询结果集，其中会调用doQuery()方法完成数据库查询操作，
        // 该方法为抽象方法，由BaseExecutor的子类实现
        list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
      }
    } finally {
      // 当前查询完成，查询层数减少
      queryStack--;
    }
    if (queryStack == 0) {
      // 延迟加载的相关内容
      for (DeferredLoad deferredLoad : deferredLoads) {
        deferredLoad.load();
      }
      deferredLoads.clear();
      if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
        // issue #482
        clearLocalCache();
      }
    }
    return list;
  }
}
```

从上面的代码中可以看到，BaseExecutor 的 query()方法会根据 flushCache 属性和 localCacheScope 配置 决定是否清空一级缓存。

另外，BaseExecutor 的 update()方法在调用 doUpdate()方法之前，也会清除一级缓存。update()方法负责执行 insert、update、delete 三类 SQL 语句，它是调用 doUpdate()方法实现的。

```java
  @Override
  public int update(MappedStatement ms, Object parameter) throws SQLException {
    // 判断当前的Executor是否已经关闭
    ErrorContext.instance().resource(ms.getResource()).activity("executing an update").object(ms.getId());
    if (closed) {
      throw new ExecutorException("Executor was closed.");
    }
    // 清除一级缓存，该方法会调用localCache和localOutputParameterCache
    // 的clear()方法清除缓存
    clearLocalCache();
    // 抽象方法，交由子类实现
    return doUpdate(ms, parameter);
  }

  @Override
  public void clearLocalCache() {
    if (!closed) {
      localCache.clear();
      localOutputParameterCache.clear();
    }
  }
```

### 1.3 事务相关操作

在 BatchExecutor 实现中，可以缓存多条 SQL 语句，等待合适时机将缓存的多条 SQL 语句一并发送到数据库执行。Executor 的 flushStatements()方法主要是针对批处理多条 SQL 语句的，它会调用 doFlushStatements()这个基本方法处理 Executor 中缓存的多条 SQL 语句。在 BaseExecutor 的 commit()及 rollback()等方法中都会首先调用 flushStatements()方法，然后再执行相关事务操作。

```java
  @Override
  public void commit(boolean required) throws SQLException {
    // 检查当前连接是否已关闭
    if (closed) {
      throw new ExecutorException("Cannot commit, transaction is already closed");
    }
    // 清除一级缓存
    clearLocalCache();
    // 不执行Executor中缓存的SQL语句
    flushStatements();
    // 根据参数required决定是否提交事务
    if (required) {
      transaction.commit();
    }
  }

  @Override
  public List<BatchResult> flushStatements() throws SQLException {
    return flushStatements(false);
  }

  public List<BatchResult> flushStatements(boolean isRollBack) throws SQLException {
    if (closed) {
      throw new ExecutorException("Executor was closed.");
    }
    // 这是一个交由子类实现的抽象方法，参数isRollBack表示
    // 是否执行Executor中缓存的SQL语句，false表示执行，true表示不执行
    return doFlushStatements(isRollBack);
  }

  @Override
  public void rollback(boolean required) throws SQLException {
    if (!closed) {
      try {
        // 清除一级缓存
        clearLocalCache();
        // 批量执行缓存的sql语句
        flushStatements(true);
      } finally {
        // 根据required决定是否回滚事务
        if (required) {
          transaction.rollback();
        }
      }
    }
  }

  @Override
  public void close(boolean forceRollback) {
    try {
      try {
        // 根据forceRollback参数决定 是否强制回滚该事务
        rollback(forceRollback);
      } finally {
        if (transaction != null) {
          transaction.close();
        }
      }
    } catch (SQLException e) {
      // Ignore.  There's nothing that can be done at this point.
      log.warn("Unexpected exception on closing transaction.  Cause: " + e);
    } finally {
      transaction = null;
      deferredLoads = null;
      localCache = null;
      localOutputParameterCache = null;
      closed = true;
    }
  }
```

## 2 SimpleExecutor

SimpleExecutor 继承了 BaseExecutor 抽象类，它是最简单的 Executor 接口实现。Executor 组件使用了模板方法模式，一级缓存等固定不变的操作都封装到了 BaseExecutor 中，在 SimpleExecutor 中就不必再关心一级缓存等操作，只需要专注实现 4 个基本方法的实现即可。

```java
public class SimpleExecutor extends BaseExecutor {

  public SimpleExecutor(Configuration configuration, Transaction transaction) {
    super(configuration, transaction);
  }

  @Override
  public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
    Statement stmt = null;
    try {
      // 获取配置对象
      Configuration configuration = ms.getConfiguration();
      // 创建StatementHandler对象
      StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
      // 完成Statement的创建和初始化，该方法首先会调用StatementHandler的prepare()方法
      // 创建Statement对象，然后调用StatementHandler的parameterize()方法处理占位符
      stmt = prepareStatement(handler, ms.getStatementLog());
      // 调用StatementHandler的query()方法，执行sql语句，并通过ResultSetHandler
      // 完成结果集的映射
      return handler.<E>query(stmt, resultHandler);
    } finally {
      // 关闭Statement对象
      closeStatement(stmt);
    }
  }

  private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
    Statement stmt;
    Connection connection = getConnection(statementLog);
    // 创建Statement对象
    stmt = handler.prepare(connection, transaction.getTimeout());
    // 处理占位符
    handler.parameterize(stmt);
    return stmt;
  }

  /**
   * 与前面doQuery()方法的实现非常类似
   */
  @Override
  public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
    Statement stmt = null;
    try {
      Configuration configuration = ms.getConfiguration();
      StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
      stmt = prepareStatement(handler, ms.getStatementLog());
      return handler.update(stmt);
    } finally {
      closeStatement(stmt);
    }
  }

  @Override
  protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql) throws SQLException {
    Configuration configuration = ms.getConfiguration();
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
    Statement stmt = prepareStatement(handler, ms.getStatementLog());
    return handler.<E>queryCursor(stmt);
  }

  @Override
  public List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
    // SimpleExecutor不提供sql语句批处理，所以直接返回空集合
    return Collections.emptyList();
  }

}
```

## 3 ReuseExecutor

在传统的 JDBC 编程中，复用 Statement 对象是常用的一种优化手段，该优化手段可以减少 SQL 预编译的开销以及创建和销毁 Statement 对象的开销，从而提高性能（Reuse，复用）。

ReuseExecutor 提供了 Statement 复用的功能，ReuseExecutor 中通过 statementMap 字段缓存使用过的 Statement 对象，key 是 SQL 语句，value 是 SQL 对应的 Statement 对象。

ReuseExecutor.doQuery()、doQueryCursor()、doUpdate()方法的实现与 SimpleExecutor 中对应方法的实现一样，区别在于其中调用的 prepareStatement()方法，SimpleExecutor 每次都会通过 JDBC 的 Connection 对象创建新的 Statement 对象，而 ReuseExecutor 则会先尝试重用 StaternentMap 中缓存的 Statement 对象。

```java
  // 本map用于缓存使用过的Statement，以提升本框架的性能
  // key SQL语句，value 该SQL语句对应的Statement
  private final Map<String, Statement> statementMap = new HashMap<String, Statement>();

  private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
    Statement stmt;
    BoundSql boundSql = handler.getBoundSql();
    // 获取要执行的sql语句
    String sql = boundSql.getSql();
    // 如果之前执行过该sql，则从缓存中取出对应的Statement对象
    // 不再创建新的Statement，减少系统开销
    if (hasStatementFor(sql)) {
      stmt = getStatement(sql);
      // 修改超时时间
      applyTransactionTimeout(stmt);
    } else {
      // 获取数据库连接
      Connection connection = getConnection(statementLog);
      // 从连接中获取Statement对象
      stmt = handler.prepare(connection, transaction.getTimeout());
      // 将sql语句 和 其对应的Statement对象缓存起来
      putStatement(sql, stmt);
    }
    // 处理占位符
    handler.parameterize(stmt);
    return stmt;
  }

  /**
   * 当事务提交或回滚、连接关闭时，都需要关闭这些缓存的Statement对象。前面分析的BaseExecutor的
   * commit()、rollback()和close()方法中都会调用doFlushStatements()方法，
   * 所以在该方法中关闭Statement对象的逻辑非常合适
   */
  @Override
  public List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
    // 遍历Statement对象集合，并依次关闭
    for (Statement stmt : statementMap.values()) {
      closeStatement(stmt);
    }
    // 清除对Statement对象的缓存
    statementMap.clear();
    // 返回一个空集合
    return Collections.emptyList();
  }
```

#### 拓展内容：SQL 预编译

**1、数据库预编译起源**

（1）数据库 SQL 语句编译特性

数据库接收到 sql 语句之后，需要词法和语义解析，以优化 sql 语句，制定执行计划。这需要花费一些时间。但是很多情况，我们的同一条 sql 语句可能会反复执行，或者每次执行的时候只有个别的值不同（比如：query 的 where 子句值不同，update 的 set 子句值不同，insert 的 values 值不同）。

（2）减少编译的方法

如果每次都需要经过上面的词法语义解析、语句优化、制定执行计划等，则效率就明显不行了。为了解决上面的问题，于是就有了预编译，预编译语句就是将这类语句中的值用占位符替代，可以视为将 sql 语句模板化或者说参数化。一次编译、多次运行，省去了解析优化等过程。

（3）缓存预编译

预编译语句被 DB 的编译器编译后的执行代码被缓存下来，那么下次调用时只要是相同的预编译语句就不需要重复编译，只要将参数直接传入编译过的语句执行代码中(相当于一个函数)就会得到执行。并不是所以预编译语句都一定会被缓存，数据库本身会用一种策略（内部机制）。

（4） 预编译的实现方法

预编译是通过 PreparedStatement 和占位符来实现的。

**2.预编译作用**

（1）减少编译次数 提升性能

预编译之后的 sql 多数情况下可以直接执行，DBMS（数据库管理系统）不需要再次编译。越复杂的 sql，往往编译的复杂度就越大。

（2）防止 SQL 注入

使用预编译，后面注入的参数将不会再次触发 SQL 编译。也就是说，对于后面注入的参数，系统将不会认为它会是一个 SQL 命令，而默认其是一个参数，参数中的 or 或 and 等（SQL 注入常用技俩）就不是 SQL 语法保留字了。

**3.mybatis 是如何实现预编译的**

mybatis 默认情况下，将对所有的 sql 进行预编译。mybatis 底层使用 PreparedStatement，过程是，先将带有占位符（即”?”）的 sql 模板发送至数据库服务器，由服务器对此无参数的 sql 进行编译后，将编译结果缓存，然后直接执行带有真实参数的 sql。核心是通过 “#{ }” 实现的。在预编译之前，#{ } 被解析为一个预编译语句（PreparedStatement）的占位符 ?。

```sql
// sqlMap 中如下的 sql 语句
select * from user where name = #{name};
// 解析成为预编译语句
select * from user where name = ?;
```

## 4 BatchExecutor

应用系统在执行一条 SQL 语句时，会将 SQL 语句以及相关参数通过网络发送到数据库系统。对于频繁操作数据库的应用系统来说，如果执行一条 SQL 语句就向数据库发送一次请求，很多时间会浪费在网络通信上。使用批量处理的优化方式可以在客户端缓存多条 SQL 语句，并在合适的时机将多条 SQL 语句打包发送给数据库执行，从而减少网络方面的开销，提升系统的性能。

需要注意的是，在批量执行多条 SQL 语句时，每次向数据库发送的 SQL 语句条数
是有上限的，若超出上限，数据库会拒绝执行这些 SQL 语句井抛出异常，所以批量发送 SQL 语句的时机很重要。

mybatis 的 BatchExecutor 实现了批处理多条 SQL 语句的功能。

```java
public class BatchExecutor extends BaseExecutor {

  public static final int BATCH_UPDATE_RETURN_VALUE = Integer.MIN_VALUE + 1002;
  // 缓存多个Statement对象，其中每个Statement对象中都可以缓存多条
  // 结构相同 但参数不同的sql语句
  private final List<Statement> statementList = new ArrayList<Statement>();
  // 记录批处理的结果，BatchResult中通过updateCounts字段
  // 记录每个Statement对象 执行批处理的结果
  private final List<BatchResult> batchResultList = new ArrayList<BatchResult>();
  // 记录当前执行的sql语句
  private String currentSql;
  // 记录当前执行的MappedStatement对象
  private MappedStatement currentStatement;

  /**
   * JDBC中的批处理只支持insert、update、delete等类型的SQL语句，不支持select类型的
   * SQL语句，所以doUpdate()方法是BatchExecutor中最重要的一个方法。
   * 本方法在添加一条SQL语句时，首先会将currentSql字段记录的SQL语句以及currentStatement字段
   * 记录的MappedStatement对象与当前添加的SQL以及MappedStatement对象进行比较，
   * 如果相同则添加到同一个Statement对象中等待执行，如果不同则创建新的Statement对象
   * 井将其缓存到statementList集合中等待执行
   */
  @Override
  public int doUpdate(MappedStatement ms, Object parameterObject) throws SQLException {
    // 获取configuration配置对象
    final Configuration configuration = ms.getConfiguration();
    // 实例化一个StatementHandler，并返回
    final StatementHandler handler = configuration.newStatementHandler(this, ms, parameterObject, RowBounds.DEFAULT, null, null);
    // 获取需要执行的sql语句
    final BoundSql boundSql = handler.getBoundSql();
    final String sql = boundSql.getSql();
    final Statement stmt;
    // 判断要执行的sql语句结构 及 MappedStatement对象 是否与上次的相同
    if (sql.equals(currentSql) && ms.equals(currentStatement)) {
      // 相同则添加到同一个Statement对象中等待执行
      // 首先获取statementList集合中最后一个Statement对象
      int last = statementList.size() - 1;
      stmt = statementList.get(last);
      // 重新设置事务超时时间
      applyTransactionTimeout(stmt);
      // 绑定实参，处理占位符？
      handler.parameterize(stmt);
      // 查找对应的BatchResult对象，并记录用户传入的实参
      BatchResult batchResult = batchResultList.get(last);
      batchResult.addParameterObject(parameterObject);
    } else {
      // 不同则创建新的Statement对象井将其缓存到statementList集合中等待执行
      Connection connection = getConnection(ms.getStatementLog());
      // 创建新的Statement对象
      stmt = handler.prepare(connection, transaction.getTimeout());
      // 绑定实参，处理占位符？
      handler.parameterize(stmt);
      // 记录本次的sql语句 及 Statement对象
      currentSql = sql;
      currentStatement = ms;
      // 将新创建的Statement对象添加到statementList集合
      statementList.add(stmt);
      // 添加新的BatchResult对象
      batchResultList.add(new BatchResult(ms, sql, parameterObject));
    }
    // 底层通过调用java.sql.Statement的addBatch()方法添加sql语句
    handler.batch(stmt);
    return BATCH_UPDATE_RETURN_VALUE;
  }

  /**
   * 上面的doUpdate()方法负责添加待执行的sql语句，
   * 而doFlushStatements()方法则将上面添加的sql语句进行批量处理
   */
  @Override
  public List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
    try {
      // 用于存储批处理结果的集合
      List<BatchResult> results = new ArrayList<BatchResult>();
      // 如果要回滚 则返回一个空集合
      if (isRollback) {
        return Collections.emptyList();
      }
      // 批处理statementList集合中的所以Statement对象
      for (int i = 0, n = statementList.size(); i < n; i++) {
        // 获取Statement对象 和其对应的 BatchResult对象
        Statement stmt = statementList.get(i);
        applyTransactionTimeout(stmt);
        BatchResult batchResult = batchResultList.get(i);
        try {
          // 调用Statement对象的executeBatch()方法，批量执行其中记录的sql语句
          // 将执行返回的int[]数组set进batchResult的updateCounts字段，
          // 其中的每一个int值都代表了对应的sql语句 影响的记录条数
          batchResult.setUpdateCounts(stmt.executeBatch());
          MappedStatement ms = batchResult.getMappedStatement();
          List<Object> parameterObjects = batchResult.getParameterObjects();
          // 获取配置的KeyGenerator对象
          KeyGenerator keyGenerator = ms.getKeyGenerator();
          if (Jdbc3KeyGenerator.class.equals(keyGenerator.getClass())) {
            Jdbc3KeyGenerator jdbc3KeyGenerator = (Jdbc3KeyGenerator) keyGenerator;
            // 获取数据库生成的主键 并设置到parameterObjects中
            jdbc3KeyGenerator.processBatch(ms, stmt, parameterObjects);
          } else if (!NoKeyGenerator.class.equals(keyGenerator.getClass())) {
            // 对于其它类型的KeyGenerator，则调用其processAfter进行处理
            for (Object parameter : parameterObjects) {
              keyGenerator.processAfter(this, ms, stmt, parameter);
            }
          }
          closeStatement(stmt);
        } catch (BatchUpdateException e) {
          StringBuilder message = new StringBuilder();
          message.append(batchResult.getMappedStatement().getId())
                  .append(" (batch index #")
                  .append(i + 1)
                  .append(")")
                  .append(" failed.");
          if (i > 0) {
            message.append(" ")
                    .append(i)
                    .append(" prior sub executor(s) completed successfully, but will be rolled back.");
          }
          throw new BatchExecutorException(message.toString(), e, results, batchResult);
        }
        // 添加处理完的BatchResult对象到要返回的List<BatchResult>集合中
        results.add(batchResult);
      }
      return results;
    } finally {
      // 关闭所有的Statement对象
      for (Statement stmt : statementList) {
        closeStatement(stmt);
      }
      // 清空currentSql、statementList、batchResultList对象
      currentSql = null;
      statementList.clear();
      batchResultList.clear();
    }
  }
}
```

通过了解 JDBC 的批处理功能 我们可以知道，Statement 中可以添加不同语句结构的 SQL，但是每添加一个新结构的 SQL 语句都会触发一次编译操作。而 PreparedStatement 中只能添加同一语句结构的 SQL 语句，只会触发一次编译操作，但是可以通过绑定多组不同的实参实现批处理。通过上面对 doUpdate()方法的分析可知，BatchExecutor 会将连续添加的、相同语句结构的 SQL 语句添加到同一个 Statement/PreparedStatement 对象中，这样可以有效地减少编译操作的次数。

BatchExecutor 中 doQuery()和 doQueryCursor()方法的实现与前面介绍的 SimpleExecutor 类似，主要区别就是 BatchExecutor 中的这两个方法在最开始都会先调用 flushStatements()方法，执行缓存的 SQL 语句，以保证 从数据库中查询到的数据是最新的。

CachingExecutor 中为 Executor 对象增加了二级缓存相关功能，而 mybatis 的二级缓存在实际使用中往往利大于弊，被 redis 等产品所替代，所以这里不做分析。

# Sqlsession组件

SqlSession 是 MyBatis 核心接口之一，也是 MyBatis 接口层的主要组成部分，对外提供 MyBatis 常用的 API。mybatis 提供了两个 SqlSession 接口的实现，分别为 DefaultSqlSession、SqlSessionManager，其中最常用的是 DefaultSqlSession。另外，跟前面分析过的源码 mybatis 的源码一样，mybatis 也为 SqlSession 提供了相应的工厂接口 SqlSessionFactory，及实现该接口的实现 DefaultSqlSessionFactory（SqlSessionManager 同时实现了 SqlSession 和 SqlSessionFactory 接口）。

## 1 SqlSession

在 SqlSession 中定义了常用的数据库操作以及事务的相关操作，为了方便用户使用，每种类型的操作都提供了多种重载。

```java
public interface SqlSession extends Closeable {
  // 泛型方法，参数是要执行查询的sql语句，返回值为查询的结果对象
  <T> T selectOne(String statement);

  // 第二个参数表示 需要用户传入的实参，即 sql语句绑定的实参
  <T> T selectOne(String statement, Object parameter);

  // 查询结果有多条记录，会封装成 结果对象列表 并返回
  <E> List<E> selectList(String statement);

  // 参数 + 多记录结果集
  <E> List<E> selectList(String statement, Object parameter);

  // 参数RowBounds主要用于逻辑分页，逻辑分页会将所有的结果都查询到，
  // 然后根据RowBounds中提供的offset和limit值来获取最后的结果
  <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds);

  // mapKey表示将结果集中的哪一列（如 主键列或编码列）作为Map的key，
  // value则为列值 对应的那条记录
  <K, V> Map<K, V> selectMap(String statement, String mapKey);

  // 多了个parameter参数，其它与上面相同
  <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey);

  // 多了个RowBounds参数，其它与上面相同
  <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds);

  // 除了返回值是Cursor对象，其它与selectList相同
  <T> Cursor<T> selectCursor(String statement);
  <T> Cursor<T> selectCursor(String statement, Object parameter);
  <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds);

  // 查询出的结果集 将由传入的ResultHandler对象处理，其它与selectList相同
  void select(String statement, Object parameter, ResultHandler handler);
  void select(String statement, ResultHandler handler);
  void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler);

  // 执行insert语句
  int insert(String statement);
  int insert(String statement, Object parameter);

  // 执行update语句
  int update(String statement);
  int update(String statement, Object parameter);

  // 执行delete语句
  int delete(String statement);
  int delete(String statement, Object parameter);

  // 提交事务
  void commit();
  void commit(boolean force);

  // 回滚事务
  void rollback();
  void rollback(boolean force);

  // 将对数据库的操作请求 刷到数据库
  List<BatchResult> flushStatements();

  // 关闭当前session
  void close();

  // 清空缓存
  void clearCache();

  // 获取Configuration对象
  Configuration getConfiguration();

  // 获取type对应的Mapper对象
  <T> T getMapper(Class<T> type);

  // 获取该SqlSession对应的数据库连接
  Connection getConnection();
}
```

### 1.1 DefaultSqlSession

DefaultSqlSession 是单独使用 MyBatis 进行开发时，最常用的 SqISession 接口实现。其实现了 SqISession 接口中定义的方法，及各方法的重载。select()系列方法、selectOne()系列方法、selectList()系列方法、selectMap()系列方法之间的调用关系如下图，殊途同归，它们最终都会调用 Executor 的 query()方法。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/DefaultSqlSession%E6%96%B9%E6%B3%95%E8%B0%83%E7%94%A8%E6%A0%88-8dc4a8.png)

上述重载方法最终都是通过调用 Executor 的 query(MappedStatement, Object, RowBounds,ResultHandler)方法实现数据库查询操作的，但各自对结果对象进行了相应的调整，例如：selectOne()方法是从结果对象集合中获取了第一个元素返回；selectMap()方法会将 List 类型的结果集 转换成 Map 类型集合返回；select()方法是将结果集交由用户指定的 ResultHandler 对象处理，且没有返回值；selectList()方法则是直接返回结果对象集合。
DefaultSqlSession 的 insert()方法、update()方法、delete()方法也有多个重载，它们最后都是通过调用 DefaultSqlSession 的 update(String, Object)方法实现的，该重载首先会将 dirty 字段置为 true，然后再通过 Executor 的 update()方法完成数据库修改操作。
DefaultSqlSession 的 commit()方法、rollback()方法以及 close()方法都会调用 Executor 中相应的方法，其中就会涉及清空缓存的操作，之后就会将 dirty 字段设置为 false。
上述的 dirty 字段主要在 isCommitOrRollbackRequired()方法中，与 autoCommit 字段以及用户传入的 force 参数共同决定是否提交/回滚事务。该方法的返回值将作为 Executor 的 commit()方法和 rollback()方法的参数。

```java
  private boolean isCommitOrRollbackRequired(boolean force) {
    return (!autoCommit && dirty) || force;
  }
```

## 2 SqlSessionFactory

SqlSessionFactory 负责创建 SqlSession 对象，其中包含了多个 openSession()方法的重载，可以通过其参数指定事务的隔离级别、底层使用 Executor 的类型、以及是否自动提交事务等方面的配置。

```java
public interface SqlSessionFactory {

  // 提供了openSession()方法的多种重载，根据相应的参数 可以指定事务的隔离级别、
  // 底层使用的Executor类型、以及是否自动提交事务等配置
  SqlSession openSession();
  SqlSession openSession(boolean autoCommit);
  SqlSession openSession(Connection connection);
  SqlSession openSession(TransactionIsolationLevel level);
  SqlSession openSession(ExecutorType execType);
  SqlSession openSession(ExecutorType execType, boolean autoCommit);
  SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level);
  SqlSession openSession(ExecutorType execType, Connection connection);

  Configuration getConfiguration();
}
```

### 2.1 DefaultSqlSessionFactory

DefaultSqlSessionFactory 是 SqlSessionFactory 接口的默认实现，主要提供了两种创建 DefaultSqlSession 对象的方式，一种方式是通过数据源获取数据库连接，并创建 Executor 对象以及 DefaultSqlSession 对象；另一种方式是用户提供数据库连接对象，DefaultSqlSessionFactory 根据该数据库连接对象获取 autoCommit 属性，创建 Executor 对象以及 DefaultSqlSession 对象。

DefaultSqISessionFactory 提供的所有 openSession()方法重载都是基于上述两种方式创建 DefaultSqlSession 对象的。

```java
public class DefaultSqlSessionFactory implements SqlSessionFactory {

  private final Configuration configuration;

  public DefaultSqlSessionFactory(Configuration configuration) {
    this.configuration = configuration;
  }

  private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
    Transaction tx = null;
    try {
      // 获取配置的Environment对象
      final Environment environment = configuration.getEnvironment();
      // 从environment中获取TransactionFactory对象，如果没有，就创建一个ManagedTransactionFactory实例并返回
      final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
      // 从事务工厂中获取一个事务对象
      tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
      // 根据事务对象tx和配置的Executor类型execType创建Executor实例
      // ExecutorType是个枚举类型，有三个值 SIMPLE, REUSE, BATCH，分别对应了
      // SimpleExecutor、ReuseExecutor、BatchExecutor
      final Executor executor = configuration.newExecutor(tx, execType);
      // 创建DefaultSqlSession对象
      return new DefaultSqlSession(configuration, executor, autoCommit);
    } catch (Exception e) {
      closeTransaction(tx); // may have fetched a connection so lets call close()
      throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  private SqlSession openSessionFromConnection(ExecutorType execType, Connection connection) {
    try {
      boolean autoCommit;
      try {
        // 根据当前连接对象获取autoCommit属性（是否自动提交事务）
        autoCommit = connection.getAutoCommit();
      } catch (SQLException e) {
        autoCommit = true;
      }
      // 除了获取autoCommit属性的方式和上面不一样外，下面的处理都与上面完全相同
      final Environment environment = configuration.getEnvironment();
      final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
      final Transaction tx = transactionFactory.newTransaction(connection);
      final Executor executor = configuration.newExecutor(tx, execType);
      return new DefaultSqlSession(configuration, executor, autoCommit);
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  private TransactionFactory getTransactionFactoryFromEnvironment(Environment environment) {
    if (environment == null || environment.getTransactionFactory() == null) {
      return new ManagedTransactionFactory();
    }
    return environment.getTransactionFactory();
  }

  private void closeTransaction(Transaction tx) {
    if (tx != null) {
      try {
        tx.close();
      } catch (SQLException ignore) {
        // Intentionally ignore. Prefer previous error.
      }
    }
  }

  @Override
  public SqlSession openSession() {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
  }

  @Override
  public SqlSession openSession(boolean autoCommit) {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, autoCommit);
  }

  @Override
  public SqlSession openSession(ExecutorType execType) {
    return openSessionFromDataSource(execType, null, false);
  }

  @Override
  public SqlSession openSession(TransactionIsolationLevel level) {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), level, false);
  }

  @Override
  public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
    return openSessionFromDataSource(execType, level, false);
  }

  @Override
  public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
    return openSessionFromDataSource(execType, null, autoCommit);
  }

  @Override
  public SqlSession openSession(Connection connection) {
    return openSessionFromConnection(configuration.getDefaultExecutorType(), connection);
  }

  @Override
  public SqlSession openSession(ExecutorType execType, Connection connection) {
    return openSessionFromConnection(execType, connection);
  }

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }
}
```

### 2.2 SqlSessionManager

SqlSessionManager 同时实现了 SqlSession 接口和 SqlSessionFactory 接口，所以同时提供了 SqlSessionFactory 创建 SqlSession 对象，以及 SqlSession 操纵数据库的功能。

SqlSessionManager 与 DefaultSqlSessionFactory 的主要不同点 SqlSessionManager 提供了两种模式，第一种模式与 DefaultSqlSessionFactory 的行为相同，同一线程每次通过 SqlSessionManager 对象访问数据库时，都会创建新的 SqlSession 对象完成数据库操作。第二种模式是 SqlSessionManager 通过 localSqlSession 这 ThreadLocal 变量，记录与当前线程绑定的 SqlSession 对象，供当前线程循环使用，从而避免在同一线程多次创建 SqlSession 对象带来的性能损失。

SqlSessionManager 的构造方法是唯一且私有的，如果要创建 SqlSessionManager 对象，需要调用其 newInstance()方法（但需要注意的是，这不是单例模式，因为每次调用 newInstance()方法都返回了一个新的对象）。

SqlSessionManager 的 openSession()系列方法，都是通过直接调用其持有的
DefaultSqlSessionFactory 实例来实现的。

```java
public class SqlSessionManager implements SqlSessionFactory, SqlSession {

  // 通过持有DefaultSqlSessionFactory对象 来产生SqlSession对象
  private final SqlSessionFactory sqlSessionFactory;

  // 用于记录一个与当前线程绑定的SqlSession对象
  private final ThreadLocal<SqlSession> localSqlSession = new ThreadLocal<SqlSession>();

  // localSqlSession中记录的SqlSession对象的代理对象（JDK动态代理）
  // SqlSessionManager初始化时 生成本代理对象，可以看下 下面的构造函数
  private final SqlSession sqlSessionProxy;

  // 私有的构造函数，也是SqlSessionManager唯一的构造函数
  private SqlSessionManager(SqlSessionFactory sqlSessionFactory) {
    // 传入的这个SqlSessionFactory对象 往往是DefaultSqlSessionFactory的实例
    this.sqlSessionFactory = sqlSessionFactory;
    // JDK动态代理生成代理对象，可以看得出，SqlSessionInterceptor一定实现了
    // InvocationHandler接口
    this.sqlSessionProxy = (SqlSession) Proxy.newProxyInstance(
        SqlSessionFactory.class.getClassLoader(),
        new Class[]{SqlSession.class},
        new SqlSessionInterceptor());
  }

  // 通过newInstance()方法创建SqlSessionManager对象，有多种重载，
  // 但最后都是new了一个DefaultSqlSessionFactory的实例
  public static SqlSessionManager newInstance(Reader reader) {
    return new SqlSessionManager(new SqlSessionFactoryBuilder().build(reader, null, null));
  }

  public static SqlSessionManager newInstance(Reader reader, String environment) {
    return new SqlSessionManager(new SqlSessionFactoryBuilder().build(reader, environment, null));
  }

  public static SqlSessionManager newInstance(Reader reader, Properties properties) {
    return new SqlSessionManager(new SqlSessionFactoryBuilder().build(reader, null, properties));
  }

  public static SqlSessionManager newInstance(InputStream inputStream) {
    return new SqlSessionManager(new SqlSessionFactoryBuilder().build(inputStream, null, null));
  }

  public static SqlSessionManager newInstance(InputStream inputStream, String environment) {
    return new SqlSessionManager(new SqlSessionFactoryBuilder().build(inputStream, environment, null));
  }

  public static SqlSessionManager newInstance(InputStream inputStream, Properties properties) {
    return new SqlSessionManager(new SqlSessionFactoryBuilder().build(inputStream, null, properties));
  }

  public static SqlSessionManager newInstance(SqlSessionFactory sqlSessionFactory) {
    return new SqlSessionManager(sqlSessionFactory);
  }

  // openSession()系列方法都是通过当前SqlSessionManager对象持有的
  // DefaultSqlSessionFactory实例的openSession()实现的
  @Override
  public SqlSession openSession() {
    return sqlSessionFactory.openSession();
  }

  @Override
  public SqlSession openSession(boolean autoCommit) {
    return sqlSessionFactory.openSession(autoCommit);
  }

  @Override
  public SqlSession openSession(Connection connection) {
    return sqlSessionFactory.openSession(connection);
  }

  @Override
  public SqlSession openSession(TransactionIsolationLevel level) {
    return sqlSessionFactory.openSession(level);
  }

  @Override
  public SqlSession openSession(ExecutorType execType) {
    return sqlSessionFactory.openSession(execType);
  }

  @Override
  public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
    return sqlSessionFactory.openSession(execType, autoCommit);
  }

  @Override
  public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
    return sqlSessionFactory.openSession(execType, level);
  }

  @Override
  public SqlSession openSession(ExecutorType execType, Connection connection) {
    return sqlSessionFactory.openSession(execType, connection);
  }
}
```

SqlSessionManager 中实现的 SqlSession 接口方法，例如 select ()系列方法、update()系列方法等，都是直接调用 sqlSessionProxy 代理对象对应的方法实现的。在创建该代理对象时使用的 InvocationHandler 对象是 SqlSessionlnterceptor，它是 SqISessionManager 的内部类。

```java
  private class SqlSessionInterceptor implements InvocationHandler {

    public SqlSessionInterceptor() { }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      // 获取 与当前线程绑定的SqlSession
      final SqlSession sqlSession = SqlSessionManager.this.localSqlSession.get();
      // 如果有绑定的SqlSession对象
      if (sqlSession != null) { // 模式二
        try {
          // 调用真正的sqlSession对象，完成数据库操作
          return method.invoke(sqlSession, args);
        } catch (Throwable t) {
          throw ExceptionUtil.unwrapThrowable(t);
        }
      // 如果没有绑定的SqlSession对象
      } else { // 模式一
        // 创建一个新的SqlSession对象
        final SqlSession autoSqlSession = openSession();
        try {
          // 通过反射调用该SqlSession对象的方法，完成数据库操作
          final Object result = method.invoke(autoSqlSession, args);
          // 提交事务
          autoSqlSession.commit();
          return result;
        } catch (Throwable t) {
          // 出异常就回滚
          autoSqlSession.rollback();
          throw ExceptionUtil.unwrapThrowable(t);
        } finally {
          // 关闭该SqlSession对象
          autoSqlSession.close();
        }
      }
    }
  }
```

通过对 SqlSessionlnterceptor 的分析可知，第一种模式中新建的 SqlSession 在使用完成后会立即关闭。在第二种模式中，与当前线程绑定的 SqISession 对象需要先通过 SqlSessionManager 的 startManagedSession()方法进行设置，此方法也存在多种重载，但都彼此相似 且简单。

```java
  public void startManagedSession() {
    this.localSqlSession.set(openSession());
  }

  public void startManagedSession(boolean autoCommit) {
    this.localSqlSession.set(openSession(autoCommit));
  }

  public void startManagedSession(Connection connection) {
    this.localSqlSession.set(openSession(connection));
  }

  public void startManagedSession(TransactionIsolationLevel level) {
    this.localSqlSession.set(openSession(level));
  }

  public void startManagedSession(ExecutorType execType) {
    this.localSqlSession.set(openSession(execType));
  }

  public void startManagedSession(ExecutorType execType, boolean autoCommit) {
    this.localSqlSession.set(openSession(execType, autoCommit));
  }

  public void startManagedSession(ExecutorType execType, TransactionIsolationLevel level) {
    this.localSqlSession.set(openSession(execType, level));
  }

  public void startManagedSession(ExecutorType execType, Connection connection) {
    this.localSqlSession.set(openSession(execType, connection));
  }

  public boolean isManagedSessionStarted() {
    return this.localSqlSession.get() != null;
  }
```

当需要提交/回滚事务，或关闭 IocalSqlSession 中记录的 SqlSession 对象时，需要通过 SqlSessionManager 的 commit()、rollback()以及 close()方法完成，其中会先检测当前线程是否绑定了 SqlSession 对象，如果未绑定则抛出异常，如果绑定了则调用该 SqlSession 对象的相应方法。

```java
  @Override
  public void clearCache() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot clear the cache.  No managed session is started.");
    }
    sqlSession.clearCache();
  }

  @Override
  public void commit() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot commit.  No managed session is started.");
    }
    sqlSession.commit();
  }

  @Override
  public void commit(boolean force) {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot commit.  No managed session is started.");
    }
    sqlSession.commit(force);
  }

  @Override
  public void rollback() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
    }
    sqlSession.rollback();
  }

  @Override
  public void rollback(boolean force) {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
    }
    sqlSession.rollback(force);
  }

  @Override
  public List<BatchResult> flushStatements() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
    }
    return sqlSession.flushStatements();
  }

  @Override
  public void close() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot close.  No managed session is started.");
    }
    try {
      sqlSession.close();
    } finally {
      localSqlSession.set(null);
    }
  }
```

# Mybatis Alias

- Author: [HuiFer](https://github.com/huifer)
- Description: 该文介绍 mybatis Alias 源码
- 源码阅读工程: [SourceHot-Mybatis](https://github.com/SourceHot/mybatis-read.git)
- 源码位置 :`org.apache.ibatis.type.Alias`
- 与 Alias 相关的一个方法`org.apache.ibatis.type.TypeAliasRegistry.registerAlias(java.lang.String, java.lang.Class<?>)`(别名注册)

```java
    /**
     * 别名注册,
     * typeAliases 是一个map key=>别名,value=>字节码
     *
     * @param alias 别名名称
     * @param value 别名的字节码
     */
    public void registerAlias(String alias, Class<?> value) {
        if (alias == null) {
            throw new TypeException("The parameter alias cannot be null");
        }
        // issue #748
        String key = alias.toLowerCase(Locale.ENGLISH);
        if (typeAliases.containsKey(key) && typeAliases.get(key) != null && !typeAliases.get(key).equals(value)) {
            throw new TypeException("The alias '" + alias + "' is already mapped to the value '" + typeAliases.get(key).getName() + "'.");
        }
        typeAliases.put(key, value);
    }

```

- registerAlias 操作的对象是一个`map`对象

```java
    /**
     * 别名存放仓库
     * 是一个map key=>别名,value=>字节码
     */
    private final Map<String, Class<?>> typeAliases = new HashMap<>();
```

不难看出这个对象存放的内容是 别名 -> clazz.

- 相关注解`Alias`

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Alias {
    /**
     * Return the alias name.
     *
     * @return the alias name
     */
    String value();
}
```

- 看一下实现方式

```java
    /**
     * 加载{@link Alias} 注解的内容
     *
     * @param type
     */
    public void registerAlias(Class<?> type) {
        String alias = type.getSimpleName();
        Alias aliasAnnotation = type.getAnnotation(Alias.class);
        if (aliasAnnotation != null) {
            // 获取 别名注解
            alias = aliasAnnotation.value();
        }
        // 转换为 别名,clazz
        registerAlias(alias, type);
    }
```

最后回到了`org.apache.ibatis.type.TypeAliasRegistry.registerAlias(java.lang.String, java.lang.Class<?>)`方法
我们可以简单编写一个测试类

```java
@Alias(value = "hc")
public class Hc {
}

    /**
     * 对注解 {@link Alias} 的测试用例
     */
    @Test
    void testAnnotation() {
        TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();
        typeAliasRegistry.registerAlias(Hc.class);
        assertEquals("org.apache.ibatis.type.Hc", typeAliasRegistry.resolveAlias("hc").getName());
    }

```

其他与`Alias`相关的测试类位于: `org.apache.ibatis.type.TypeAliasRegistryTest`

# Mybatis Cursor

- Author: [HuiFer](https://github.com/huifer)
- Description: 该文介绍 mybatis Cursor 源码
- 源码阅读工程: [SourceHot-Mybatis](https://github.com/SourceHot/mybatis-read.git)

## Cursor

- 源码位置:`org.apache.ibatis.cursor.Cursor`
- 继承`Iterable`说明是一个迭代器,继承`Closeable`说明有一个东西需要关闭

```java
public interface Cursor<T> extends Closeable, Iterable<T> {

    /**
     * 游标开始从数据库获取数据,返回true,反之false
     *
     * @return true if the cursor has started to fetch items from database.
     */
    boolean isOpen();

    /**
     * 数据库元素都被获取,返回true,反之false
     *
     * @return true if the cursor is fully consumed and has returned all elements matching the query.
     */
    boolean isConsumed();

    /**
     * 获取数据索引,从0开始,没有返回-1
     * Get the current item index. The first item has the index 0.
     *
     * @return -1 if the first cursor item has not been retrieved. The index of the current item retrieved.
     */
    int getCurrentIndex();
}
```

## DefaultCursor

```java
public class DefaultCursor<T> implements Cursor<T> {

    /**
     * 对象包装结果处理类
     */
    protected final ObjectWrapperResultHandler<T> objectWrapperResultHandler = new ObjectWrapperResultHandler<>();
    // ResultSetHandler stuff
    /**
     * ResultSet 处理器
     */
    private final DefaultResultSetHandler resultSetHandler;
    /**
     * 结果映射
     */
    private final ResultMap resultMap;
    /**
     * ResultSet 包装对象
     */
    private final ResultSetWrapper rsw;
    /**
     * 分页的
     */
    private final RowBounds rowBounds;
    /**
     * 游标的迭代器
     */
    private final CursorIterator cursorIterator = new CursorIterator();

    /**
     * 游标开启判断
     */
    private boolean iteratorRetrieved;

    /**
     * 游标状态,默认是创建未使用
     */
    private CursorStatus status = CursorStatus.CREATED;
    /**
     * 分页索引,默认-1
     */
    private int indexWithRowBound = -1;

    /**
     * 构造方法
     *
     * @param resultSetHandler
     * @param resultMap
     * @param rsw
     * @param rowBounds
     */
    public DefaultCursor(DefaultResultSetHandler resultSetHandler, ResultMap resultMap, ResultSetWrapper rsw, RowBounds rowBounds) {
        this.resultSetHandler = resultSetHandler;
        this.resultMap = resultMap;
        this.rsw = rsw;
        this.rowBounds = rowBounds;
    }


    @Override
    public boolean isOpen() {
        return status == CursorStatus.OPEN;
    }

    @Override
    public boolean isConsumed() {
        return status == CursorStatus.CONSUMED;
    }

    /**
     * 当前索引
     * @return
     */
    @Override
    public int getCurrentIndex() {
        return rowBounds.getOffset() + cursorIterator.iteratorIndex;
    }

    /**
     * 迭代器获取
     * @return
     */
    @Override
    public Iterator<T> iterator() {
        // 是否获取过
        if (iteratorRetrieved) {
            throw new IllegalStateException("Cannot open more than one iterator on a Cursor");
        }
        // 是否关闭
        if (isClosed()) {
            throw new IllegalStateException("A Cursor is already closed.");
        }
        iteratorRetrieved = true;
        return cursorIterator;
    }

    /**
     * {@link Closeable} 关闭{@link ResultSet}
     */
    @Override
    public void close() {
        // 判断是否关闭
        if (isClosed()) {
            return;
        }

        ResultSet rs = rsw.getResultSet();
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            // ignore
        } finally {
            // 设置游标状态
            status = CursorStatus.CLOSED;
        }
    }

    /**
     * 去到真正的数据行
     * @return
     */
    protected T fetchNextUsingRowBound() {
        T result = fetchNextObjectFromDatabase();
        while (objectWrapperResultHandler.fetched && indexWithRowBound < rowBounds.getOffset()) {
            result = fetchNextObjectFromDatabase();
        }
        return result;
    }

    /**
     * 从数据库获取数据
     * @return
     */
    protected T fetchNextObjectFromDatabase() {
        if (isClosed()) {
            return null;
        }

        try {
            objectWrapperResultHandler.fetched = false;
            // 游标状态设置
            status = CursorStatus.OPEN;
            if (!rsw.getResultSet().isClosed()) {
                // 处理数据结果放入，objectWrapperResultHandler
                resultSetHandler.handleRowValues(rsw, resultMap, objectWrapperResultHandler, RowBounds.DEFAULT, null);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // 获取处理结果
        T next = objectWrapperResultHandler.result;
        // 结果不为空
        if (objectWrapperResultHandler.fetched) {
            // 索引+1
            indexWithRowBound++;
        }
        // No more object or limit reached
        // 如果没有数据, 或者 当前读取条数= 偏移量+限额量
        if (!objectWrapperResultHandler.fetched || getReadItemsCount() == rowBounds.getOffset() + rowBounds.getLimit()) {
            // 关闭游标
            close();
            status = CursorStatus.CONSUMED;
        }
        // 设置结果为null
        objectWrapperResultHandler.result = null;

        return next;
    }

    /**
     * 是否关闭状态判断
     *
     * @return
     */
    private boolean isClosed() {
        return status == CursorStatus.CLOSED || status == CursorStatus.CONSUMED;
    }

    /**
     * 下一个索引
     * @return
     */
    private int getReadItemsCount() {
        return indexWithRowBound + 1;
    }

    /**
     * 游标的状态
     */
    private enum CursorStatus {

        /**
         * 新创建的游标, ResultSet 还没有使用过
         * A freshly created cursor, database ResultSet consuming has not started.
         */
        CREATED,
        /**
         * 游标使用过, ResultSet 被使用
         * A cursor currently in use, database ResultSet consuming has started.
         */
        OPEN,
        /**
         * 游标关闭, 可能没有被消费完全
         * A closed cursor, not fully consumed.
         */
        CLOSED,
        /**
         * 游标彻底消费完毕, 关闭了
         * A fully consumed cursor, a consumed cursor is always closed.
         */
        CONSUMED
    }

    /**
     * 对象处理结果的包装类
     * @param <T>
     */
    protected static class ObjectWrapperResultHandler<T> implements ResultHandler<T> {

        /**
         * 数据结果
         */
        protected T result;
        /**
         * 是否null
         */
        protected boolean fetched;

        /**
         * 从{@link ResultContext} 获取结果对象
         * @param context
         */
        @Override
        public void handleResult(ResultContext<? extends T> context) {
            this.result = context.getResultObject();
            context.stop();
            fetched = true;
        }
    }

    /**
     * 游标迭代器
     */
    protected class CursorIterator implements Iterator<T> {

        /**
         * 下一个数据
         * Holder for the next object to be returned.
         */
        T object;

        /**
         * 下一个的索引
         * Index of objects returned using next(), and as such, visible to users.
         */
        int iteratorIndex = -1;

        /**
         * 是否有下一个值
         * @return
         */
        @Override
        public boolean hasNext() {
            if (!objectWrapperResultHandler.fetched) {
                object = fetchNextUsingRowBound();
            }
            return objectWrapperResultHandler.fetched;
        }

        /**
         * 下一个值
         * @return
         */
        @Override
        public T next() {
            // Fill next with object fetched from hasNext()
            T next = object;

            if (!objectWrapperResultHandler.fetched) {
                next = fetchNextUsingRowBound();
            }

            if (objectWrapperResultHandler.fetched) {
                objectWrapperResultHandler.fetched = false;
                object = null;
                iteratorIndex++;
                return next;
            }
            throw new NoSuchElementException();
        }

        /**
         * 不可执行抛出异常
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException("Cannot remove element from Cursor");
        }
    }
}
```

# Mybatis DataSource

- Author: [HuiFer](https://github.com/huifer)
- Description: 该文介绍 mybatis DataSource 源码
- 源码阅读工程: [SourceHot-Mybatis](https://github.com/SourceHot/mybatis-read.git)

- `org.apache.ibatis.datasource.DataSourceFactory`

```java
/**
 * 数据源工厂
 * @author Clinton Begin
 */
public interface DataSourceFactory {

    /**
     * 设置 dataSource 属性
     * @param props
     */
    void setProperties(Properties props);

    /**
     * 获取 dataSource
     * @return {@link DataSource}
     */
    DataSource getDataSource();

}

```

类图如下

![image-20191223081023730](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191223081023730-bdcd63.png)

- `setProperties`会将下列标签放入`datasource`中

```java
      <dataSource type="POOLED">
        <property name="driver" value="com.mysql.jdbc.Driver"/>
        <property name="url" value="jdbc:mysql://localhost:3306/mybatis?useSSL=false"/>
        <property name="username" value="mybatis"/>
        <property name="password" value="mybatis"/>
      </dataSource>
```

- 在`org.apache.ibatis.session.Configuration`中有配置下面三个信息

```java
        typeAliasRegistry.registerAlias("JNDI", JndiDataSourceFactory.class);
        typeAliasRegistry.registerAlias("POOLED", PooledDataSourceFactory.class);
        typeAliasRegistry.registerAlias("UNPOOLED", UnpooledDataSourceFactory.class);

```

## JndiDataSourceFactory

```java
/**
 * @author Clinton Begin
 */
public class JndiDataSourceFactory implements DataSourceFactory {

    public static final String INITIAL_CONTEXT = "initial_context";
    public static final String DATA_SOURCE = "data_source";
    public static final String ENV_PREFIX = "env.";

    /**
     * 直接 java 数据源
     */
    private DataSource dataSource;

    /**
     * 获取数据源的配置信息
     * @param allProps
     * @return
     */
    private static Properties getEnvProperties(Properties allProps) {
        final String PREFIX = ENV_PREFIX;
        Properties contextProperties = null;
        for (Entry<Object, Object> entry : allProps.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            // 只获取前缀`env`
            if (key.startsWith(PREFIX)) {
                if (contextProperties == null) {
                    contextProperties = new Properties();
                }
                // 放入数据
                contextProperties.put(key.substring(PREFIX.length()), value);
            }
        }
        return contextProperties;
    }

    /**
     * 设置数据源属性
     * @param properties
     */
    @Override
    public void setProperties(Properties properties) {
        try {
            InitialContext initCtx;
            Properties env = getEnvProperties(properties);
            if (env == null) {
                initCtx = new InitialContext();
            } else {
                initCtx = new InitialContext(env);
            }

            if (properties.containsKey(INITIAL_CONTEXT)
                    && properties.containsKey(DATA_SOURCE)) {
                // 如果包含`initial_context`和`data_source`
                Context ctx = (Context) initCtx.lookup(properties.getProperty(INITIAL_CONTEXT));
                dataSource = (DataSource) ctx.lookup(properties.getProperty(DATA_SOURCE));
            } else if (properties.containsKey(DATA_SOURCE)) {
                dataSource = (DataSource) initCtx.lookup(properties.getProperty(DATA_SOURCE));
            }

        } catch (NamingException e) {
            throw new DataSourceException("There was an error configuring JndiDataSourceTransactionPool. Cause: " + e, e);
        }
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

}
```

## PooledDataSource

```java
	protected int poolMaximumActiveConnections = 10;
    protected int poolMaximumIdleConnections = 5;
    protected int poolMaximumCheckoutTime = 20000;
    protected int poolTimeToWait = 20000;
    protected int poolMaximumLocalBadConnectionTolerance = 3;
    protected String poolPingQuery = "NO PING QUERY SET";
    protected boolean poolPingEnabled;
    protected int poolPingConnectionsNotUsedFor;
```

## PooledDataSourceFactory

```java
public class PooledDataSourceFactory extends UnpooledDataSourceFactory {


    public PooledDataSourceFactory() {
        this.dataSource = new PooledDataSource();
    }

}

	// 初始化
    public PooledDataSource() {
        dataSource = new UnpooledDataSource();
    }
```

## UnpooledDataSourceFactory

```java
    @Override
    public void setProperties(Properties properties) {
        Properties driverProperties = new Properties();
        //metaDataSource 现在是一个dataSource
        MetaObject metaDataSource = SystemMetaObject.forObject(dataSource);
        for (Object key : properties.keySet()) {
            String propertyName = (String) key;
            if (propertyName.startsWith(DRIVER_PROPERTY_PREFIX)) {
                // 如果是 driver. 前缀开头
                String value = properties.getProperty(propertyName);
                driverProperties.setProperty(propertyName.substring(DRIVER_PROPERTY_PREFIX_LENGTH), value);
            } else if (metaDataSource.hasSetter(propertyName)) {
                String value = (String) properties.get(propertyName);
                Object convertedValue = convertValue(metaDataSource, propertyName, value);
                // 通过 metaDataSource 来对 dataSource 进行设置属性
                metaDataSource.setValue(propertyName, convertedValue);
            } else {
                throw new DataSourceException("Unknown DataSource property: " + propertyName);
            }
        }
        if (driverProperties.size() > 0) {
            metaDataSource.setValue("driverProperties", driverProperties);
        }
    }

```

## UnpooledDataSource

- `org.apache.ibatis.datasource.unpooled.UnpooledDataSource`主要定义数据库连接相关的一些属性,以及与数据库的链接对象创建

  ```java
      // 一些配置信息
      private ClassLoader driverClassLoader;
      private Properties driverProperties;
      private String driver;
      private String url;
      private String username;
      private String password;
      private Boolean autoCommit;
      private Integer defaultTransactionIsolationLevel;
      private Integer defaultNetworkTimeout;
  ```

- 初始化连接对象

  ```java
      /**
       * 加载链接驱动 如 mysql 链接驱动
       * @throws SQLException
       */
      private synchronized void initializeDriver() throws SQLException {
          if (!registeredDrivers.containsKey(driver)) {
              Class<?> driverType;
              try {
                  if (driverClassLoader != null) {
                      driverType = Class.forName(driver, true, driverClassLoader);
                  } else {
                      driverType = Resources.classForName(driver);
                  }
                  // DriverManager requires the driver to be loaded via the system ClassLoader.
                  // http://www.kfu.com/~nsayer/Java/dyn-jdbc.html
                  Driver driverInstance = (Driver) driverType.getDeclaredConstructor().newInstance();
                  DriverManager.registerDriver(new DriverProxy(driverInstance));
                  registeredDrivers.put(driver, driverInstance);
              } catch (Exception e) {
                  throw new SQLException("Error setting driver on UnpooledDataSource. Cause: " + e);
              }
          }
      }
  
  ```

- 设置连接对象的属性

  ```java
      /**
       * 设置连接对象 , 超时时间,是否自动提交事物
       * @param conn
       * @throws SQLException
       */
      private void configureConnection(Connection conn) throws SQLException {
          if (defaultNetworkTimeout != null) {
              conn.setNetworkTimeout(Executors.newSingleThreadExecutor(), defaultNetworkTimeout);
          }
          if (autoCommit != null && autoCommit != conn.getAutoCommit()) {
              conn.setAutoCommit(autoCommit);
          }
          if (defaultTransactionIsolationLevel != null) {
              conn.setTransactionIsolation(defaultTransactionIsolationLevel);
          }
      }
  
  ```

- 获取连接对象

  ```java
      /**
       * 获取链接对象
       * @param username
       * @param password
       * @return
       * @throws SQLException
       */
      private Connection doGetConnection(String username, String password) throws SQLException {
          Properties props = new Properties();
          if (driverProperties != null) {
              props.putAll(driverProperties);
          }
          if (username != null) {
              props.setProperty("user", username);
          }
          if (password != null) {
              props.setProperty("password", password);
          }
          return doGetConnection(props);
      }
  
  ```

## 解析流程

- 在 xml 解析的过程中会执行`DataSourceFactory`相关内容

```java
    /**
     * 解析 dataSourceElement 标签
     * <dataSource type="POOLED">
     * <property name="driver" value="com.mysql.jdbc.Driver"/>
     * <property name="url" value="jdbc:mysql://localhost:3306/mybatis"/>
     * <property name="username" value="root"/>
     * <property name="password" value="root"/>
     * </dataSource>
     *
     * @param context
     * @return
     * @throws Exception
     */
    private DataSourceFactory dataSourceElement(XNode context) throws Exception {
        if (context != null) {
            String type = context.getStringAttribute("type");
            Properties props = context.getChildrenAsProperties();
            //org.apache.ibatis.session.Configuration.Configuration()
            DataSourceFactory factory = (DataSourceFactory) resolveClass(type).getDeclaredConstructor().newInstance();

            // PooledDataSourceFactory -> UnpooledDataSourceFactory
            factory.setProperties(props);
            return factory;
        }
        throw new BuilderException("Environment declaration requires a DataSourceFactory.");
    }

```

从类图上或者代码中我们可以发现`PooledDataSourceFactory`是继承`UnpooledDataSourceFactory`那么方法应该也是`UnpooledDataSourceFactory`的。看看设置属性方法

![image-20191223083610214](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191223083610214-808408.png)

方法直接走完

![image-20191223083732972](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191223083732972-fd94b2.png)

# Mybatis DyanmicSqlSourcce

- Author: [HuiFer](https://github.com/huifer)
- 源码阅读工程: [SourceHot-Mybatis](https://github.com/SourceHot/mybatis-read.git)

- `org.apache.ibatis.scripting.xmltags.DynamicSqlSource`
- `org.apache.ibatis.scripting.xmltags.DynamicContext.DynamicContext`

```XML
    <select id="list" resultType="com.huifer.mybatis.entity.HsSell">
        select * from hs_sell
        <trim prefix="WHERE" prefixOverrides="AND |OR">
            <if test="ID != null">
                and ID = #{ID,jdbcType=INTEGER}
            </if>

        </trim>
    </select>

```

![image-20191219151247240](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219151247240-176b71.png)

![image-20191219151408597](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219151408597-9b5268.png)

```java
public class MixedSqlNode implements SqlNode {
    private final List<SqlNode> contents;

    public MixedSqlNode(List<SqlNode> contents) {
        this.contents = contents;
    }

    @Override
    public boolean apply(DynamicContext context) {
        // 调用 salNode 对象本身的 apply 方法解析 sql
        contents.forEach(node -> node.apply(context));
        return true;
    }
}
```

- 根据 mapper.xml 文件中的代码流程 需要走

  `org.apache.ibatis.scripting.xmltags.StaticTextSqlNode#apply`

  `org.apache.ibatis.scripting.xmltags.TrimSqlNode#apply`

  `org.apache.ibatis.scripting.xmltags.IfSqlNode#apply`

![image-20191219152254274](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219152254274-7de0bb.png)

```java
/**
 * @author Clinton Begin
 */
public class StaticTextSqlNode implements SqlNode {
    private final String text;

    public StaticTextSqlNode(String text) {
        this.text = text;
    }

    /**
     * 静态文本apply 方法
     * @param context
     * @return
     */
    @Override
    public boolean apply(DynamicContext context) {
        context.appendSql(text);
        return true;
    }

}
```

- `org.apache.ibatis.scripting.xmltags.DynamicContext#appendSql`

  ```java
    public void appendSql(String sql) {
        sqlBuilder.add(sql);
    }
  ```

- 解析`trim`标签

![image-20191219152502960](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219152502960-ce8db5.png)

- 在解析`trim`的时候会往下解析下级标签

  ```java
      @Override
      public boolean apply(DynamicContext context) {
          FilteredDynamicContext filteredDynamicContext = new FilteredDynamicContext(context);
         // 解析下级标签的入口
          boolean result = contents.apply(filteredDynamicContext);
          filteredDynamicContext.applyAll();
          return result;
      }
  ```

![image-20191219152655746](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219152655746-2fad6f.png)

```java
    @Override
    public boolean apply(DynamicContext context) {
        if (evaluator.evaluateBoolean(test, context.getBindings())) {
            contents.apply(context);
            return true;
        }
        return false;
    }

```

- `evaluator.evaluateBoolean(test, context.getBindings())`方法

```java
    /**
     * @param expression      判断语句,ID != null
     * @param parameterObject 参数列表
     * @return
     */
    public boolean evaluateBoolean(String expression, Object parameterObject) {
        Object value = OgnlCache.getValue(expression, parameterObject);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(String.valueOf(value)).compareTo(BigDecimal.ZERO) != 0;
        }
        return value != null;
    }

```

```java
    /**
     * 取值
     * @param expression  判断语句,ID=NULL
     * @param root 参数列表
     * @return
     */
    public static Object getValue(String expression, Object root) {
        try {
            Map context = Ognl.createDefaultContext(root, MEMBER_ACCESS, CLASS_RESOLVER, null);
            // 判断是否存在 expression 的判断内容 (判断ID是否存在)
            return Ognl.getValue(parseExpression(expression), context, root);
        } catch (OgnlException e) {
            throw new BuilderException("Error evaluating expression '" + expression + "'. Cause: " + e, e);
        }
    }

```

![image-20191219153341466](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219153341466-5fa842.png)

存在返回`true`

执行完成就得到了一个 sql

![image-20191219153553127](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219153553127-e9d6f3.png)

继续执行`org.apache.ibatis.scripting.xmltags.DynamicSqlSource#getBoundSql`方法

![image-20191219155129772](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219155129772-c740e5.png)

- 发送 sql`org.apache.ibatis.executor.SimpleExecutor#doQuery`

- 调用链路如下

- `org.apache.ibatis.executor.CachingExecutor#query(org.apache.ibatis.mapping.MappedStatement, java.lang.Object, org.apache.ibatis.session.RowBounds, org.apache.ibatis.session.ResultHandler)`

  - `org.apache.ibatis.executor.CachingExecutor#query(org.apache.ibatis.mapping.MappedStatement, java.lang.Object, org.apache.ibatis.session.RowBounds, org.apache.ibatis.session.ResultHandler, org.apache.ibatis.cache.CacheKey, org.apache.ibatis.mapping.BoundSql)`

  - `org.apache.ibatis.executor.Executor#query(org.apache.ibatis.mapping.MappedStatement, java.lang.Object, org.apache.ibatis.session.RowBounds, org.apache.ibatis.session.ResultHandler, org.apache.ibatis.cache.CacheKey, org.apache.ibatis.mapping.BoundSql)`

  - `org.apache.ibatis.executor.BaseExecutor#query(org.apache.ibatis.mapping.MappedStatement, java.lang.Object, org.apache.ibatis.session.RowBounds, org.apache.ibatis.session.ResultHandler, org.apache.ibatis.cache.CacheKey, org.apache.ibatis.mapping.BoundSql)`

    ```java
        @SuppressWarnings("unchecked")
        @Override
        public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
            ErrorContext.instance().resource(ms.getResource()).activity("executing a query").object(ms.getId());
            if (closed) {
                // 判断当前是否关闭
                throw new ExecutorException("Executor was closed.");
            }
            if (queryStack == 0 && ms.isFlushCacheRequired()) {
                // 查询堆栈==0 和 是否需要刷新缓存
                // 清理本地缓存
                clearLocalCache();
            }
            List<E> list;
            try {
    //            堆栈+1,防止重新清理缓存
                queryStack++;
                // 通过 缓存key 在本地缓存中获取
                list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
                if (list != null) {
                    // 通过缓存 key 查到后处理  localOutputParameterCache
                    handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
                } else {
                    // 没有查询到从数据库查询
                    list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
                }
            } finally {
                // 堆栈-1
                queryStack--;
            }
            if (queryStack == 0) {
                for (DeferredLoad deferredLoad : deferredLoads) {
                    deferredLoad.load();
                }
                // 清空线程安全队列(延迟队列)
                // issue #601
                deferredLoads.clear();
                if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
                    // STATEMENT 清空本地缓存
                    // issue #482
                    clearLocalCache();
                }
            }
            return list;
        }
    
    ```

    - `org.apache.ibatis.executor.BaseExecutor#queryFromDatabase`

      ```java
          private <E> List<E> queryFromDatabase(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
              List<E> list;
              localCache.putObject(key, EXECUTION_PLACEHOLDER);
              try {
                  list = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
              } finally {
                  localCache.removeObject(key);
              }
              localCache.putObject(key, list);
              if (ms.getStatementType() == StatementType.CALLABLE) {
                  localOutputParameterCache.putObject(key, parameter);
              }
              return list;
          }
      
      ```

      - `org.apache.ibatis.executor.BaseExecutor#doQuery`
      - `org.apache.ibatis.executor.SimpleExecutor#doQuery`

![image-20191219160832704](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219160832704-f5dc59.png)

```java
    private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
        Statement stmt;
        // 数据库连接
        Connection connection = getConnection(statementLog);
        // stms 创建
        // org.apache.ibatis.executor.statement.BaseStatementHandler.prepare
        stmt = handler.prepare(connection, transaction.getTimeout());
        // 参数放入
        handler.parameterize(stmt);
        return stmt;
    }

```

![image-20191219160908212](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219160908212-8a1ba7.png)

- `org.apache.ibatis.executor.statement.BaseStatementHandler#prepare`
  - `org.apache.ibatis.executor.statement.PreparedStatementHandler#instantiateStatement`

```java
    @Override
    public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
        ErrorContext.instance().sql(boundSql.getSql());
        Statement statement = null;
        try {
            statement = instantiateStatement(connection);
            setStatementTimeout(statement, transactionTimeout);
            setFetchSize(statement);
            return statement;
        } catch (SQLException e) {
            closeStatement(statement);
            throw e;
        } catch (Exception e) {
            closeStatement(statement);
            throw new ExecutorException("Error preparing statement.  Cause: " + e, e);
        }
    }

```

```java
    @Override
    protected Statement instantiateStatement(Connection connection) throws SQLException {
        String sql = boundSql.getSql();
        if (mappedStatement.getKeyGenerator() instanceof Jdbc3KeyGenerator) {
            String[] keyColumnNames = mappedStatement.getKeyColumns();
            if (keyColumnNames == null) {
                return connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            } else {
                return connection.prepareStatement(sql, keyColumnNames);
            }
        } else if (mappedStatement.getResultSetType() == ResultSetType.DEFAULT) {
            return connection.prepareStatement(sql);
        } else {
            return connection.prepareStatement(sql, mappedStatement.getResultSetType().getValue(), ResultSet.CONCUR_READ_ONLY);
        }
    }

```

- 这个方法都去了`java.sql.Connection#prepareStatement(java.lang.String, java.lang.String[])`

- 接下来需要考虑的问题是如何将`?`换成我们的参数`2`

  ![image-20191219161555793](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219161555793-c9f10d.png)

- `org.apache.ibatis.executor.statement.StatementHandler#parameterize`

  - `org.apache.ibatis.executor.statement.RoutingStatementHandler#parameterize`
    - `org.apache.ibatis.executor.statement.StatementHandler#parameterize`
      - `org.apache.ibatis.executor.statement.PreparedStatementHandler#parameterize`
        - `org.apache.ibatis.executor.parameter.ParameterHandler`
          - `org.apache.ibatis.scripting.defaults.DefaultParameterHandler#setParameters`

![image-20191219162258040](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219162258040-d4fd4c.png)

这样就拿到了`value`的值

![image-20191219162506920](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219162506920-fffac2.png)

准备工作就绪了发送就可以了

`doQuery`的工作完成了继续往下走

```java
    @Override
    public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        Statement stmt = null;
        try {
            Configuration configuration = ms.getConfiguration();
            StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
            stmt = prepareStatement(handler, ms.getStatementLog());
            return handler.query(stmt, resultHandler);
        } finally {
            closeStatement(stmt);
        }
    }

```

- `org.apache.ibatis.executor.statement.RoutingStatementHandler#query`
  - `org.apache.ibatis.executor.statement.PreparedStatementHandler#query`
    - `org.apache.ibatis.executor.resultset.ResultSetHandler#handleResultSets`
    - `org.apache.ibatis.executor.resultset.DefaultResultSetHandler#handleResultSets`

![image-20191219163628214](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219163628214-23690d.png)

![image-20191219163640968](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219163640968-3d5df7.png)

![image-20191219163957488](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219163957488-a75e48.png)

处理后结果如上

```java
    /**
     * 处理查询结果
     * @param stmt
     * @return
     * @throws SQLException
     */
    @Override
    public List<Object> handleResultSets(Statement stmt) throws SQLException {
        ErrorContext.instance().activity("handling results").object(mappedStatement.getId());

        final List<Object> multipleResults = new ArrayList<>();

        int resultSetCount = 0;
        ResultSetWrapper rsw = getFirstResultSet(stmt);

        List<ResultMap> resultMaps = mappedStatement.getResultMaps();
        int resultMapCount = resultMaps.size();
        validateResultMapsCount(rsw, resultMapCount);
        while (rsw != null && resultMapCount > resultSetCount) {
            ResultMap resultMap = resultMaps.get(resultSetCount);
            handleResultSet(rsw, resultMap, multipleResults, null);
            rsw = getNextResultSet(stmt);
            cleanUpAfterHandlingResultSet();
            resultSetCount++;
        }

        String[] resultSets = mappedStatement.getResultSets();
        if (resultSets != null) {
            while (rsw != null && resultSetCount < resultSets.length) {
                ResultMapping parentMapping = nextResultMaps.get(resultSets[resultSetCount]);
                if (parentMapping != null) {
                    String nestedResultMapId = parentMapping.getNestedResultMapId();
                    ResultMap resultMap = configuration.getResultMap(nestedResultMapId);
                    handleResultSet(rsw, resultMap, null, parentMapping);
                }
                rsw = getNextResultSet(stmt);
                cleanUpAfterHandlingResultSet();
                resultSetCount++;
            }
        }

        // 查询结果
        return collapseSingleResultList(multipleResults);
    }

```

# MapperMethod

- Author: [HuiFer](https://github.com/huifer)
- Description: 该文介绍 mybatis MapperMethod 源码
- 源码地址: `org.apache.ibatis.binding.MapperMethod`,核心方法是`execute`
- 源码阅读工程: [SourceHot-Mybatis](https://github.com/SourceHot/mybatis-read.git)

```java
    /**
     * CRUD 不同的执行处理
     *
     * @param sqlSession
     * @param args
     * @return
     */
    public Object execute(SqlSession sqlSession, Object[] args) {
        Object result;
        switch (command.getType()) {
            case INSERT: {
                Object param = method.convertArgsToSqlCommandParam(args);
                result = rowCountResult(sqlSession.insert(command.getName(), param));
                break;
            }
            case UPDATE: {
                Object param = method.convertArgsToSqlCommandParam(args);
                result = rowCountResult(sqlSession.update(command.getName(), param));
                break;
            }
            case DELETE: {
                Object param = method.convertArgsToSqlCommandParam(args);
                result = rowCountResult(sqlSession.delete(command.getName(), param));
                break;
            }
            case SELECT:
                if (method.returnsVoid() && method.hasResultHandler()) {
                    executeWithResultHandler(sqlSession, args);
                    result = null;
                } else if (method.returnsMany()) {
                    result = executeForMany(sqlSession, args);
                } else if (method.returnsMap()) {
                    result = executeForMap(sqlSession, args);
                } else if (method.returnsCursor()) {
                    result = executeForCursor(sqlSession, args);
                } else {
                    Object param = method.convertArgsToSqlCommandParam(args);
                    result = sqlSession.selectOne(command.getName(), param);
                    if (method.returnsOptional()
                            && (result == null || !method.getReturnType().equals(result.getClass()))) {
                        result = Optional.ofNullable(result);
                    }
                }
                break;
            case FLUSH:
                result = sqlSession.flushStatements();
                break;
            default:
                throw new BindingException("Unknown execution method for: " + command.getName());
        }
        if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
            throw new BindingException("Mapper method '" + command.getName()
                    + " attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
        }
        return result;
    }

```

```java
    /**
     * 根据 resultHandler 进行处理
     *
     * @param sqlSession
     * @param args
     */
    private void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
        MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
        if (!StatementType.CALLABLE.equals(ms.getStatementType())
                && void.class.equals(ms.getResultMaps().get(0).getType())) {
            throw new BindingException("method " + command.getName()
                    + " needs either a @ResultMap annotation, a @ResultType annotation,"
                    + " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
        }
        Object param = method.convertArgsToSqlCommandParam(args);
        // 判断是否有 RowBounds
        if (method.hasRowBounds()) {
            RowBounds rowBounds = method.extractRowBounds(args);
            sqlSession.select(command.getName(), param, rowBounds, method.extractResultHandler(args));
        } else {
            sqlSession.select(command.getName(), param, method.extractResultHandler(args));
        }
    }

```

- 返回值为多个的情况

```java
    /**
     * 针对多个查询结果进行 ,转换成不同的 list 或者数组
     *
     * @param sqlSession
     * @param args
     * @param <E>
     * @return
     */
    private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
        List<E> result;
        Object param = method.convertArgsToSqlCommandParam(args);
        if (method.hasRowBounds()) {
            RowBounds rowBounds = method.extractRowBounds(args);
            // 直接 list
            result = sqlSession.selectList(command.getName(), param, rowBounds);
        } else {
            result = sqlSession.selectList(command.getName(), param);
        }
        // issue #510 Collections & arrays support
        if (!method.getReturnType().isAssignableFrom(result.getClass())) {
            if (method.getReturnType().isArray()) {
                // 转换成 array
                return convertToArray(result);
            } else {
                // 转换成 collection
                return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
            }
        }
        return result;
    }

```

### convertToArray

```java
    /**
     * 转换为数组
     *
     * @param list 数据库查询结果
     * @param <E>
     * @return
     */
    @SuppressWarnings("unchecked")
    private <E> Object convertToArray(List<E> list) {
        // 获取返回类型
        Class<?> arrayComponentType = method.getReturnType().getComponentType();
        // new 一个 array
        Object array = Array.newInstance(arrayComponentType, list.size());
        if (arrayComponentType.isPrimitive()) {
            for (int i = 0; i < list.size(); i++) {
                Array.set(array, i, list.get(i));
            }
            return array;
        } else {
            // 通过  toArray方法转换
            return list.toArray((E[]) array);
        }
    }

```

### convertToDeclaredCollection

```java
    /**
     * 转换为不同的list对象
     *
     * @param config
     * @param list 数据库查询结果
     * @param <E>
     * @return
     */
    private <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
        //  mybatis ObjectFactory 创建mapper 的返回结果对象
        Object collection = config.getObjectFactory().create(method.getReturnType());
        MetaObject metaObject = config.newMetaObject(collection);
        // metaObject.objectWrapper =>  CollectionWrapper
        // MetaObject 对象的 objectWrapper 现在是 CollectionWrapper 它是 Collection 的包装
        metaObject.addAll(list);
        return collection;
    }

```

- 上述两个为转换的过程,其实质还是在 `org.apache.ibatis.session.SqlSession` 中做执行操作

## debug

- 修改 mapper 返回数组对`org.apache.ibatis.binding.MapperMethod#convertToArray`方法进行测试

```java
    HsSell[] list(@Param("ID") Integer id);
```

![image-20191219092442456](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219092442456-87e0a8.png)

- 修改 mapper,对`org.apache.ibatis.binding.MapperMethod#convertToDeclaredCollection`进行测试

  ```java
      LinkedList<HsSell> list(@Param("ID") Integer id);
  ```

![image-20191219093043035](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219093043035-1d4043.png)

# Mybatis MetaObject

- Author: [HuiFer](https://github.com/huifer)
- 源码阅读工程: [SourceHot-Mybatis](https://github.com/SourceHot/mybatis-read.git)
- 源码位于:`org.apache.ibatis.reflection.MetaObject`

```java
/**
 * @author Clinton Begin
 */
public class MetaObject {

    /**
     * 原始的数据对象,初始化时的对象
     */
    private final Object originalObject;
    /**
     * 对象包装
     */
    private final ObjectWrapper objectWrapper;
    /**
     * object 工厂
     */
    private final ObjectFactory objectFactory;
    /**
     * object
     */
    private final ObjectWrapperFactory objectWrapperFactory;
    /**
     * 反射工程
     */
    private final ReflectorFactory reflectorFactory;

    private MetaObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
        this.originalObject = object;
        this.objectFactory = objectFactory;
        this.objectWrapperFactory = objectWrapperFactory;
        this.reflectorFactory = reflectorFactory;

        // 根据object不同实例进行不同的实例化方式
        if (object instanceof ObjectWrapper) {
            this.objectWrapper = (ObjectWrapper) object;
        } else if (objectWrapperFactory.hasWrapperFor(object)) {
            this.objectWrapper = objectWrapperFactory.getWrapperFor(this, object);
        } else if (object instanceof Map) {
            this.objectWrapper = new MapWrapper(this, (Map) object);
        } else if (object instanceof Collection) {
            this.objectWrapper = new CollectionWrapper(this, (Collection) object);
        } else {
            this.objectWrapper = new BeanWrapper(this, object);
        }
    }

    public static MetaObject forObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
        if (object == null) {
            return SystemMetaObject.NULL_META_OBJECT;
        } else {
            return new MetaObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
        }
    }

    /**
     * 获取value
     * @param name 属性值名称
     * @return
     */
    public Object getValue(String name) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
            if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
                // 判断是否是空的metaObject
                return null;
            } else {
                return metaValue.getValue(prop.getChildren());
            }
        } else {
            return objectWrapper.get(prop);
        }
    }

    /**
     * metaObject 设置属性值方法
     * {name:value}
     *
     * @param name  属性值名称
     * @param value 属性值
     */
    public void setValue(String name, Object value) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            // 获取属性实例
            MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
            if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
                if (value == null) {
                    // value 空则返回
                    // don't instantiate child path if value is null
                    return;
                } else {
                    // 创建属性值
                    metaValue = objectWrapper.instantiatePropertyValue(name, prop, objectFactory);
                }
            }

            metaValue.setValue(prop.getChildren(), value);
        } else {
            objectWrapper.set(prop, value);
        }
    }


}

```

# MethodSignature

- Author: [HuiFer](https://github.com/huifer)
- Description: 该文介绍 mybatis MethodSignature 类
- 源码阅读工程: [SourceHot-Mybatis](https://github.com/SourceHot/mybatis-read.git)
- `org.apache.ibatis.binding.MapperMethod.MethodSignature`

```java
    /**
     * 方法签名
     */
    public static class MethodSignature {

        /**
         * 返回值是否多个
         */
        private final boolean returnsMany;
        /**
         * 返回值是不是map
         */
        private final boolean returnsMap;
        /**
         * 返回值是否 void
         */
        private final boolean returnsVoid;
        /**
         * 返回的是否是一个游标
         */
        private final boolean returnsCursor;
        /**
         * 返回值是否是 optional
         */
        private final boolean returnsOptional;
        /**
         * 返回类型
         */
        private final Class<?> returnType;
        /**
         * map key
         */
        private final String mapKey;
        private final Integer resultHandlerIndex;
        private final Integer rowBoundsIndex;
        /**
         * 参数解析
         */
        private final ParamNameResolver paramNameResolver;

        public MethodSignature(Configuration configuration, Class<?> mapperInterface, Method method) {
            Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
            if (resolvedReturnType instanceof Class<?>) {
                this.returnType = (Class<?>) resolvedReturnType;
            } else if (resolvedReturnType instanceof ParameterizedType) {
                this.returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
            } else {
                this.returnType = method.getReturnType();
            }
            this.returnsVoid = void.class.equals(this.returnType);
            this.returnsMany = configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray();
            this.returnsCursor = Cursor.class.equals(this.returnType);
            this.returnsOptional = Optional.class.equals(this.returnType);
            this.mapKey = getMapKey(method);
            this.returnsMap = this.mapKey != null;
            this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
            this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);
            this.paramNameResolver = new ParamNameResolver(configuration, method);
        }

        /**
         * 方法主要是把方法参数转换为SQL命令参数。
         *
         * @param args
         * @return
         */
        public Object convertArgsToSqlCommandParam(Object[] args) {
            return paramNameResolver.getNamedParams(args);
        }

        /**
         * 是否有 {@link RowBounds}
         *
         * @return
         */
        public boolean hasRowBounds() {
            return rowBoundsIndex != null;
        }

        public RowBounds extractRowBounds(Object[] args) {
            return hasRowBounds() ? (RowBounds) args[rowBoundsIndex] : null;
        }

        /**
         * 是否uresultHandler
         *
         * @return
         */
        public boolean hasResultHandler() {
            return resultHandlerIndex != null;
        }

        public ResultHandler extractResultHandler(Object[] args) {
            return hasResultHandler() ? (ResultHandler) args[resultHandlerIndex] : null;
        }

        public String getMapKey() {
            return mapKey;
        }

        public Class<?> getReturnType() {
            return returnType;
        }

        public boolean returnsMany() {
            return returnsMany;
        }

        public boolean returnsMap() {
            return returnsMap;
        }

        public boolean returnsVoid() {
            return returnsVoid;
        }

        public boolean returnsCursor() {
            return returnsCursor;
        }

        /**
         * return whether return type is {@code java.util.Optional}.
         *
         * @return return {@code true}, if return type is {@code java.util.Optional}
         * @since 3.5.0
         */
        public boolean returnsOptional() {
            return returnsOptional;
        }

        /**
         * 获取参数名
         * {@link RowBounds}
         *
         * @param method    mapper 方法
         * @param paramType
         * @return
         */
        private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
            Integer index = null;
            // 获取参数类型
            final Class<?>[] argTypes = method.getParameterTypes();
            for (int i = 0; i < argTypes.length; i++) {
                if (paramType.isAssignableFrom(argTypes[i])) {
                    if (index == null) {
                        index = i;
                    } else {
                        throw new BindingException(method.getName() + " cannot have multiple " + paramType.getSimpleName() + " parameters");
                    }
                }
            }
            return index;
        }

        /**
         * 获取 {@link MapKey} 注解数据
         *
         * @param method
         * @return
         */
        private String getMapKey(Method method) {
            String mapKey = null;
            if (Map.class.isAssignableFrom(method.getReturnType())) {
                final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
                if (mapKeyAnnotation != null) {
                    mapKey = mapKeyAnnotation.value();
                }
            }
            return mapKey;
        }
    }

```

# Mybatis ObjectWrapper

- Author: [HuiFer](https://github.com/huifer)
- 源码阅读工程: [SourceHot-Mybatis](https://github.com/SourceHot/mybatis-read.git)
- 源码位于: `org.apache.ibatis.reflection.wrapper.ObjectWrapper`‘

类图：

![image-20191223100956713](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191223100956713-77967e.png)

```java
public interface ObjectWrapper {

    /**
     * 根据 prop 获取属性值
     *
     * @param prop
     * @return
     */
    Object get(PropertyTokenizer prop);

    /**
     * 设置属性
     *
     * @param prop  属性值名称
     * @param value 属性值
     */
    void set(PropertyTokenizer prop, Object value);

    /**
     * 获取属性
     *
     * @param name
     * @param useCamelCaseMapping
     * @return
     */
    String findProperty(String name, boolean useCamelCaseMapping);

    /**
     * get 方法名,可读方法名
     *
     * @return
     */
    String[] getGetterNames();

    /**
     * set 方法名,可写方法名
     *
     * @return
     */
    String[] getSetterNames();

    /**
     * set 数据类型, 获取可写的数据类型
     *
     * @param name
     * @return
     */
    Class<?> getSetterType(String name);

    /**
     * get 数据类型, 获取可读的数据类型
     *
     * @param name
     * @return
     */
    Class<?> getGetterType(String name);

    /**
     * 判断是否包含set方法
     *
     * @param name
     * @return
     */
    boolean hasSetter(String name);

    /**
     * 判断是否包含get方法
     *
     * @param name
     * @return
     */
    boolean hasGetter(String name);

    /**
     * 初始化数据
     *
     * @param name
     * @param prop
     * @param objectFactory
     * @return
     */
    MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory);

    /**
     * 判断是不是 list
     *
     * @return
     */
    boolean isCollection();

    /**
     * list add
     *
     * @param element
     */
    void add(Object element);

    /**
     * list addAll
     *
     * @param element
     * @param <E>
     */
    <E> void addAll(List<E> element);

}
```

## BaseWrapper

```java
/**
 * @author Clinton Begin
 */
public abstract class BaseWrapper implements ObjectWrapper {

    protected static final Object[] NO_ARGUMENTS = new Object[0];
    protected final MetaObject metaObject;

    protected BaseWrapper(MetaObject metaObject) {
        this.metaObject = metaObject;
    }

    /**
     * 处理集合对象
     * @param prop
     * @param object
     * @return
     */
    protected Object resolveCollection(PropertyTokenizer prop, Object object) {
        if ("".equals(prop.getName())) {
            return object;
        } else {
            return metaObject.getValue(prop.getName());
        }
    }

    /**
     * 对象获取,根据index
     * map.get(index)
     * 数组 array[index]
     * list list.get(index)
     * @param prop
     * @param collection
     * @return
     */
    protected Object getCollectionValue(PropertyTokenizer prop, Object collection) {
        if (collection instanceof Map) {
            // 如果是Map类型，则index为key
            return ((Map) collection).get(prop.getIndex());
        } else {
            // index 作为下标直接获取
            int i = Integer.parseInt(prop.getIndex());
            if (collection instanceof List) {
                return ((List) collection).get(i);
            } else if (collection instanceof Object[]) {
                return ((Object[]) collection)[i];
            } else if (collection instanceof char[]) {
                return ((char[]) collection)[i];
            } else if (collection instanceof boolean[]) {
                return ((boolean[]) collection)[i];
            } else if (collection instanceof byte[]) {
                return ((byte[]) collection)[i];
            } else if (collection instanceof double[]) {
                return ((double[]) collection)[i];
            } else if (collection instanceof float[]) {
                return ((float[]) collection)[i];
            } else if (collection instanceof int[]) {
                return ((int[]) collection)[i];
            } else if (collection instanceof long[]) {
                return ((long[]) collection)[i];
            } else if (collection instanceof short[]) {
                return ((short[]) collection)[i];
            } else {
                throw new ReflectionException("The '" + prop.getName() + "' property of " + collection + " is not a List or Array.");
            }
        }
    }

    /**
     * 设置属性值 ,List , object[] , char[] boolean byte double float int long short
     * map -> put(index,value)
     * list -> list.set(index,value)
     * array -> array[index] = value
     * @param prop
     * @param collection
     * @param value
     */
    protected void setCollectionValue(PropertyTokenizer prop, Object collection, Object value) {
        if (collection instanceof Map) {
            // map -> index:value
            ((Map) collection).put(prop.getIndex(), value);
        } else {
            // 数组 -> array[index]=value
            int i = Integer.parseInt(prop.getIndex());
            if (collection instanceof List) {
                ((List) collection).set(i, value);
            } else if (collection instanceof Object[]) {
                ((Object[]) collection)[i] = value;
            } else if (collection instanceof char[]) {
                ((char[]) collection)[i] = (Character) value;
            } else if (collection instanceof boolean[]) {
                ((boolean[]) collection)[i] = (Boolean) value;
            } else if (collection instanceof byte[]) {
                ((byte[]) collection)[i] = (Byte) value;
            } else if (collection instanceof double[]) {
                ((double[]) collection)[i] = (Double) value;
            } else if (collection instanceof float[]) {
                ((float[]) collection)[i] = (Float) value;
            } else if (collection instanceof int[]) {
                ((int[]) collection)[i] = (Integer) value;
            } else if (collection instanceof long[]) {
                ((long[]) collection)[i] = (Long) value;
            } else if (collection instanceof short[]) {
                ((short[]) collection)[i] = (Short) value;
            } else {
                throw new ReflectionException("The '" + prop.getName() + "' property of " + collection + " is not a List or Array.");
            }
        }
    }

}
```

## BeanWrapper

```java
public class BeanWrapper extends BaseWrapper {

    private final Object object;

    private final MetaClass metaClass;

    /**
     * 构造
     * @param metaObject
     * @param object
     */
    public BeanWrapper(MetaObject metaObject, Object object) {
        super(metaObject);
        this.object = object;
        this.metaClass = MetaClass.forClass(object.getClass(), metaObject.getReflectorFactory());
    }

    @Override
    public Object get(PropertyTokenizer prop) {
        // 索引不为空
        if (prop.getIndex() != null) {
            // 实例化集合对象
            Object collection = resolveCollection(prop, object);
            return getCollectionValue(prop, collection);
        } else {
            // 没有索引
            return getBeanProperty(prop, object);
        }
    }

    @Override
    public void set(PropertyTokenizer prop, Object value) {
        // 是否存在索引
        if (prop.getIndex() != null) {
            Object collection = resolveCollection(prop, object);
            // 向上层调用 BaseWrapper
            setCollectionValue(prop, collection, value);
        } else {
            // 本类方法
            setBeanProperty(prop, object, value);
        }
    }

    @Override
    public String findProperty(String name, boolean useCamelCaseMapping) {
        return metaClass.findProperty(name, useCamelCaseMapping);
    }

    @Override
    public String[] getGetterNames() {
        return metaClass.getGetterNames();
    }

    @Override
    public String[] getSetterNames() {
        return metaClass.getSetterNames();
    }

    @Override
    public Class<?> getSetterType(String name) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {

            MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
            // 是否null
            if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
                return metaClass.getSetterType(name);
            } else {
                return metaValue.getSetterType(prop.getChildren());
            }
        } else {
            return metaClass.getSetterType(name);
        }
    }

    @Override
    public Class<?> getGetterType(String name) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
            if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
                return metaClass.getGetterType(name);
            } else {
                return metaValue.getGetterType(prop.getChildren());
            }
        } else {
            return metaClass.getGetterType(name);
        }
    }

    @Override
    public boolean hasSetter(String name) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            if (metaClass.hasSetter(prop.getIndexedName())) {
                MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
                if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
                    return metaClass.hasSetter(name);
                } else {
                    return metaValue.hasSetter(prop.getChildren());
                }
            } else {
                return false;
            }
        } else {
            return metaClass.hasSetter(name);
        }
    }

    /**
     * 是否包含 name 的get 方法
     * @param name
     * @return
     */
    @Override
    public boolean hasGetter(String name) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            if (metaClass.hasGetter(prop.getIndexedName())) {
                MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
                if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
                    return metaClass.hasGetter(name);
                } else {
                    return metaValue.hasGetter(prop.getChildren());
                }
            } else {
                return false;
            }
        } else {
            return metaClass.hasGetter(name);
        }
    }

    /**
     * 数据嵌套处理 a.b.c  需要处理成 a->b->c
     * @param name
     * @param prop
     * @param objectFactory
     * @return
     */
    @Override
    public MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory) {
        MetaObject metaValue;
        Class<?> type = getSetterType(prop.getName());
        try {
            Object newObject = objectFactory.create(type);
            // 出现嵌套处理 instantiatePropertyValue->set
            metaValue = MetaObject.forObject(newObject, metaObject.getObjectFactory(), metaObject.getObjectWrapperFactory(), metaObject.getReflectorFactory());
            set(prop, newObject);
        } catch (Exception e) {
            throw new ReflectionException("Cannot set value of property '" + name + "' because '" + name + "' is null and cannot be instantiated on instance of " + type.getName() + ". Cause:" + e.toString(), e);
        }
        return metaValue;
    }

    /**
     * 获取 object 的  prop 属性值
     * @param prop
     * @param object
     * @return
     */
    private Object getBeanProperty(PropertyTokenizer prop, Object object) {
        try {
            // 获取get 方法
            Invoker method = metaClass.getGetInvoker(prop.getName());
            try {
                // 获取属性值
                return method.invoke(object, NO_ARGUMENTS);
            } catch (Throwable t) {
                throw ExceptionUtil.unwrapThrowable(t);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new ReflectionException("Could not get property '" + prop.getName() + "' from " + object.getClass() + ".  Cause: " + t.toString(), t);
        }
    }

    /**
     * 设置 object 的属性 prop 值为 value
     * @param prop
     * @param object
     * @param value
     */
    private void setBeanProperty(PropertyTokenizer prop, Object object, Object value) {
        try {
            // 获取set 方法
            Invoker method = metaClass.getSetInvoker(prop.getName());
            Object[] params = {value};
            try {
                // 设置属性
                method.invoke(object, params);
            } catch (Throwable t) {
                throw ExceptionUtil.unwrapThrowable(t);
            }
        } catch (Throwable t) {
            throw new ReflectionException("Could not set property '" + prop.getName() + "' of '" + object.getClass() + "' with value '" + value + "' Cause: " + t.toString(), t);
        }
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public void add(Object element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <E> void addAll(List<E> list) {
        throw new UnsupportedOperationException();
    }

}
```

## MapWrapper

```java
public class MapWrapper extends BaseWrapper {

    private final Map<String, Object> map;

    public MapWrapper(MetaObject metaObject, Map<String, Object> map) {
        super(metaObject);
        this.map = map;
    }

    @Override
    public Object get(PropertyTokenizer prop) {
        // 是否有索引
        if (prop.getIndex() != null) {
            Object collection = resolveCollection(prop, map);
            // 嗲用 BaseWrapper
            return getCollectionValue(prop, collection);
        } else {
            // 获取
            return map.get(prop.getName());
        }
    }

    @Override
    public void set(PropertyTokenizer prop, Object value) {
        if (prop.getIndex() != null) {
            Object collection = resolveCollection(prop, map);
            setCollectionValue(prop, collection, value);
        } else {
            map.put(prop.getName(), value);
        }
    }

    @Override
    public String findProperty(String name, boolean useCamelCaseMapping) {
        return name;
    }

    @Override
    public String[] getGetterNames() {
        return map.keySet().toArray(new String[map.keySet().size()]);
    }

    @Override
    public String[] getSetterNames() {
        return map.keySet().toArray(new String[map.keySet().size()]);
    }

    @Override
    public Class<?> getSetterType(String name) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
            if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
                return Object.class;
            } else {
                return metaValue.getSetterType(prop.getChildren());
            }
        } else {
            if (map.get(name) != null) {
                return map.get(name).getClass();
            } else {
                return Object.class;
            }
        }
    }

    @Override
    public Class<?> getGetterType(String name) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
            if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
                return Object.class;
            } else {
                return metaValue.getGetterType(prop.getChildren());
            }
        } else {
            if (map.get(name) != null) {
                return map.get(name).getClass();
            } else {
                return Object.class;
            }
        }
    }

    @Override
    public boolean hasSetter(String name) {
        return true;
    }

    @Override
    public boolean hasGetter(String name) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            if (map.containsKey(prop.getIndexedName())) {
                MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
                if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
                    return true;
                } else {
                    return metaValue.hasGetter(prop.getChildren());
                }
            } else {
                return false;
            }
        } else {
            return map.containsKey(prop.getName());
        }
    }

    @Override
    public MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory) {
        HashMap<String, Object> map = new HashMap<>();
        set(prop, map);
        return MetaObject.forObject(map, metaObject.getObjectFactory(), metaObject.getObjectWrapperFactory(), metaObject.getReflectorFactory());
    }

    @Override
    public boolean isCollection() {
        return false;
    }

    @Override
    public void add(Object element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <E> void addAll(List<E> element) {
        throw new UnsupportedOperationException();
    }

}
```

## CollectionWrapper

```java
public class CollectionWrapper implements ObjectWrapper {

    private final Collection<Object> object;

    public CollectionWrapper(MetaObject metaObject, Collection<Object> object) {
        this.object = object;
    }

    @Override
    public Object get(PropertyTokenizer prop) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(PropertyTokenizer prop, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String findProperty(String name, boolean useCamelCaseMapping) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getGetterNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getSetterNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> getSetterType(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> getGetterType(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasSetter(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasGetter(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory) {
        throw new UnsupportedOperationException();
    }

    /**
     * 是否是list
     *
     * @return true
     */
    @Override
    public boolean isCollection() {
        return true;
    }

    /**
     * java {@link Collection#add(Object)}
     *
     * @param element
     */
    @Override
    public void add(Object element) {
        object.add(element);
    }

    /**
     * java {@link Collection#addAll(Collection)}
     *
     * @param element
     * @param <E>
     */
    @Override
    public <E> void addAll(List<E> element) {
        object.addAll(element);
    }

}
```

# ParamNameResolver 源码解析

- Author: [HuiFer](https://github.com/huifer)
- Description: 该文介绍 mybatis `@Param` 注解和`ParamNameResolver`
- 源码阅读工程: [SourceHot-Mybatis](https://github.com/SourceHot/mybatis-read.git)

## 源码

- `org.apache.ibatis.reflection.ParamNameResolver`

```java
/**
 * {@link Param} 注解的扫描工具和处理工具
 */
public class ParamNameResolver {

    public static final String GENERIC_NAME_PREFIX = "param";

    /**
     * <p>
     * The key is the index and the value is the name of the parameter.<br />
     * The name is obtained from {@link Param} if specified. When {@link Param} is not specified,
     * the parameter index is used. Note that this index could be different from the actual index
     * when the method has special parameters (i.e. {@link RowBounds} or {@link ResultHandler}).
     * </p>
     *
     * {@link ParamNameResolver#ParamNameResolver(org.apache.ibatis.session.Configuration, java.lang.reflect.Method)} 中的map 变量值转换而得
     * {参数索引: 参数名称(arg0,Param注解的value)}
     *
     */
    private final SortedMap<Integer, String> names;

    private boolean hasParamAnnotation;

    public ParamNameResolver(Configuration config, Method method) {
        // 方法参数类型
        final Class<?>[] paramTypes = method.getParameterTypes();
        // 参数上的注解
        final Annotation[][] paramAnnotations = method.getParameterAnnotations();
        //  参数索引和参数名称
        // {参数索引:参数名称}
        final SortedMap<Integer, String> map = new TreeMap<>();
        int paramCount = paramAnnotations.length;
        // get names from @Param annotations
        for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
            if (isSpecialParameter(paramTypes[paramIndex])) {
                // skip special parameters
                // 如果是特殊类型跳过
                continue;
            }
            String name = null;

            // 注解扫描@Param
            for (Annotation annotation : paramAnnotations[paramIndex]) {
                // 是否为 Param 注解的下级
                if (annotation instanceof Param) {
                    hasParamAnnotation = true;
                    // 获取 value 属性值
                    name = ((Param) annotation).value();
                    break;
                }
            }
            if (name == null) {
                // 如果没有写 @param 处理方式如下
                // @Param was not specified.
                if (config.isUseActualParamName()) {
                    name = getActualParamName(method, paramIndex);
                }
                if (name == null) {
                    // use the parameter index as the name ("0", "1", ...)
                    // gcode issue #71
                    name = String.valueOf(map.size());
                }
            }
            // 循环参数列表 放入map 对象
            map.put(paramIndex, name);
        }
        names = Collections.unmodifiableSortedMap(map);
    }


    /**
     * 是否为特殊参数 , 依据 是否是 {@link RowBounds} 或者 {@link  ResultHandler}
     * @param clazz
     * @return
     */
    private static boolean isSpecialParameter(Class<?> clazz) {
        return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
    }

    /**
     * 返回方法名  参数索引
     * @param method
     * @param paramIndex
     * @return
     */
    private String getActualParamName(Method method, int paramIndex) {
        return ParamNameUtil.getParamNames(method).get(paramIndex);
    }

    /**
     * Returns parameter names referenced by SQL providers.
     */
    public String[] getNames() {
        return names.values().toArray(new String[0]);
    }

    /**
     * <p>
     * A single non-special parameter is returned without a name.
     * Multiple parameters are named using the naming rule.
     * In addition to the default names, this method also adds the generic names (param1, param2,
     * ...).
     * </p>
     * <p>
     * 通常参数异常在这个地方抛出 param ... 异常
     * 获取参数名称,和参数传递的真实数据
     */
    public Object getNamedParams(Object[] args) {
        final int paramCount = names.size();
        if (args == null || paramCount == 0) {
            // 是否有参数
            return null;
        } else if (!hasParamAnnotation && paramCount == 1) {
            // 没有使用 @param 注解 参数只有一个
            return args[names.firstKey()];
        } else {
            // 根据索引创建
            final Map<String, Object> param = new ParamMap<>();
            int i = 0;
            for (Map.Entry<Integer, String> entry : names.entrySet()) {
                param.put(entry.getValue(), args[entry.getKey()]);
                // add generic param names (param1, param2, ...)
                // param + 当前索引位置
                final String genericParamName = GENERIC_NAME_PREFIX + (i + 1);
                // ensure not to overwrite parameter named with @Param
                if (!names.containsValue(genericParamName)) {
                    param.put(genericParamName, args[entry.getKey()]);
                }
                i++;
            }
            return param;
        }
    }
}

```

## debug 阶段

- 测试用例为同一个 ， 每次修改 mapper 方法参数来进行 debug

```java
@Test
    void testXmlConfigurationLoad() throws IOException {
        Reader reader = Resources.getResourceAsReader("mybatis-config-demo.xml");
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(reader);
        Configuration configuration = factory.getConfiguration();
        SqlSession sqlSession = factory.openSession();
        HsSellMapper mapper = sqlSession.getMapper(HsSellMapper.class);
        List<HsSell> list = mapper.list(2);
        List<Object> objects = sqlSession.selectList("com.huifer.mybatis.mapper.HsSellMapper.list");
        assertEquals(list.size(), objects.size());


    }
```

```java
    List<HsSell> list( Integer id);

```

如果不写`@Param`称则返回


```java
    List<HsSell> list(@Param("ID") Integer id);
```

- 写`@Param`返回

![image-20191219083344439](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219083344439-9b2a6b.png)

![image-20191219083354873](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219083354873-7de2b7.png)

- `org.apache.ibatis.reflection.ParamNameResolver#getNamedParams`

  ```java
      List<HsSell> list( Integer id);
  ```

![image-20191219084455292](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219084455292-84b8e9.png)

```java
    List<HsSell> list(@Param("ID") Integer id);
```

 写上`@Param`

![image-20191219084943102](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219084943102-581714.png)

![image-20191219085131167](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219085131167-939a53.png)

# sqlCommand

- Author: [HuiFer](https://github.com/huifer)
- Description: 该文介绍 mybatis sqlCommand 类的源码
- 源码阅读工程: [SourceHot-Mybatis](https://github.com/SourceHot/mybatis-read.git)

- `org.apache.ibatis.binding.MapperMethod.SqlCommand`

```java
    /**
     * 核心内容: sql id , Sql 类型
     */
    public static class SqlCommand {

        /**
         * sql id
         */
        private final String name;
        /**
         * sql  类型select|update|delete|insert|...
         */
        private final SqlCommandType type;

        /**
         * 根据传递的参数 设置sql的一些属性 , sql id , type .
         *
         * @param configuration
         * @param mapperInterface
         * @param method
         */
        public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
            // 方法名
            final String methodName = method.getName();
            final Class<?> declaringClass = method.getDeclaringClass();
            // Statement 实质是sql
            MappedStatement ms = resolveMappedStatement(mapperInterface, methodName, declaringClass,
                    configuration);
            if (ms == null) {
                if (method.getAnnotation(Flush.class) != null) {
                    name = null;
                    type = SqlCommandType.FLUSH;
                } else {
                    throw new BindingException("Invalid bound statement (not found): "
                            + mapperInterface.getName() + "." + methodName);
                }
            } else {
                name = ms.getId();
                type = ms.getSqlCommandType();
                if (type == SqlCommandType.UNKNOWN) {
                    throw new BindingException("Unknown execution method for: " + name);
                }
            }
        }

        public String getName() {
            return name;
        }

        public SqlCommandType getType() {
            return type;
        }

        /**
         * @param mapperInterface mapper.class
         * @param methodName      方法名
         * @param declaringClass  可能是 mapper.class
         * @param configuration
         * @return
         */
        private MappedStatement resolveMappedStatement(Class<?> mapperInterface, String methodName,
                                                       Class<?> declaringClass, Configuration configuration) {
//            接口名称+方法名
            String statementId = mapperInterface.getName() + "." + methodName;
            if (configuration.hasStatement(statementId)) {
                // 从 configuration 获取
                return configuration.getMappedStatement(statementId);
            } else if (mapperInterface.equals(declaringClass)) {
                return null;
            }

            // new 一个新的实例
            for (Class<?> superInterface : mapperInterface.getInterfaces()) {
                if (declaringClass.isAssignableFrom(superInterface)) {
                    MappedStatement ms = resolveMappedStatement(superInterface, methodName,
                            declaringClass, configuration);
                    if (ms != null) {
                        return ms;
                    }
                }
            }
            return null;
        }
    }

```

![image-20191218191512184](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191218191512184-422fe0.png)

![image-20191218191550550](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191218191550550-1d24b4.png)

# GenericTokenParser

- Author: [HuiFer](https://github.com/huifer)
- 源码阅读工程: [SourceHot-Mybatis](https://github.com/SourceHot/mybatis-read.git)

```java
/**
 * Copyright 2009-2019 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.parsing;

/**
 * @author Clinton Begin
 */
public class GenericTokenParser {

    /**
     * 开始标记
     */
    private final String openToken;
    /**
     * 结束标记
     */
    private final String closeToken;
    /**
     * 标记处理器
     */
    private final TokenHandler handler;

    public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
        this.openToken = openToken;
        this.closeToken = closeToken;
        this.handler = handler;
    }

    /**
     * 核心处理方法 , 看测试类{@link org.apache.ibatis.parsing.GenericTokenParserTest}
     * @param text
     * @return
     */
    public String parse(String text) {
        // 判断是否空
        if (text == null || text.isEmpty()) {
            return "";
        }
        // search open token
        // 判断 openToken 所在的位置-1不存在
        int start = text.indexOf(openToken);
        if (start == -1) {
            return text;
        }
        char[] src = text.toCharArray();
        int offset = 0;
        final StringBuilder builder = new StringBuilder();
        StringBuilder expression = null;

        // 循环处理         assertEquals("James T Kirk reporting.", parser.parse("${first_name} ${initial} ${last_name} reporting."));
        // 将${} 转换成正常文本
        while (start > -1) {
            if (start > 0 && src[start - 1] == '\\') {
                // `\` 忽略这个参数
                // this open token is escaped. remove the backslash and continue.
                builder.append(src, offset, start - offset - 1).append(openToken);
                // offset 重新计算进行下一步循环
                offset = start + openToken.length();
            } else {
                // found open token. let's search close token.
                if (expression == null) {
                    expression = new StringBuilder();
                } else {
                    expression.setLength(0);
                }
                builder.append(src, offset, start - offset);
                offset = start + openToken.length();
                int end = text.indexOf(closeToken, offset);
                while (end > -1) {
                    if (end > offset && src[end - 1] == '\\') {
                        // 遇到`\`该参数不需要处理
                        // this close token is escaped. remove the backslash and continue.
                        expression.append(src, offset, end - offset - 1).append(closeToken);
                        // 计算offset重新推算替换的字符串
                        offset = end + closeToken.length();
                        end = text.indexOf(closeToken, offset);
                    } else {
                        expression.append(src, offset, end - offset);
                        break;
                    }
                }
                if (end == -1) {
                    // end == -1 closeToken 不存在,获取后面的所有字符串, openToken - closeToken 之间的内容
                    // close token was not found.
                    builder.append(src, start, src.length - start);
                    offset = src.length;
                } else {
                    // closeToken存在 继续执行
                    builder.append(handler.handleToken(expression.toString()));
                    offset = end + closeToken.length();
                }
            }
            start = text.indexOf(openToken, offset);
        }
        if (offset < src.length) {
            builder.append(src, offset, src.length - offset);
        }
        // 返回的是一个替换后的sql脚本
        return builder.toString();
    }
}

```

- 一个具体的例子`org.apache.ibatis.builder.SqlSourceBuilder.ParameterMappingTokenHandler`
  - 具体类`org.apache.ibatis.builder.SqlSourceBuilder`

```java
        /**
         * ? 的来源
         *
         * @param content
         * @return
         */
        @Override
        public String handleToken(String content) {
            parameterMappings.add(buildParameterMapping(content));
            return "?";
        }

```

```java
    /**
     * sql 参数类型 ， 返回值
     *
     * <select id="selectByPrimaryKey" parameterType="java.lang.Integer" resultMap="BaseResultMap">
     * <!--@mbg.generated-->
     * select
     * <include refid="Base_Column_List" />
     * from hs_sell
     * where ID = #{id,jdbcType=INTEGER}
     * </select>
     * => 替换成问号
     * select
     * <p>
     * <p>
     * ID, USER_ID, GOOD_ID, PRICE, `SIZE`, COMPANY_ID, GROUP_ID, VERSION, DELETED, CREATE_USER,
     * CREATE_TIME, UPDATE_USER, UPDATE_TIME, WORK_ORDER_ID
     * <p>
     * from hs_sell
     * where ID = ?
     *
     * @param originalSql          sql文本
     * @param parameterType        默认 object
     * @param additionalParameters
     * @return
     */
    public SqlSource parse(String originalSql, Class<?> parameterType, Map<String, Object> additionalParameters) {
        ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler(configuration, parameterType, additionalParameters);
//        org.apache.ibatis.builder.SqlSourceBuilder.ParameterMappingTokenHandler.handleToken
        GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
        String sql = parser.parse(originalSql);
        return new StaticSqlSource(configuration, sql, handler.getParameterMappings());
    }

```

![image-20191219100446796](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20191219100446796-200df8.png)

