# Mybatis 的基础支持层源码

在 Mybatis 的基础支持层主要看一下支撑 ORM 实现 的底层代码。

# 1 反射工具包

### 1.1Reflector

Reflector 类 主要实现了对 JavaBean 的元数据属性的封装，比如：可读属性列表，可写属性列表；及反射操作的封装，如：属性对应的 setter 方法，getter 方法 的反射调用。源码实现如下：

```java
public class Reflector {

  /** JavaBean 的 Class类型，在调用 Reflector 的构造方法时初始化该值 */
  private final Class<?> type;

  /** 可读的属性列表 */
  private final String[] readablePropertyNames;
  private final String[] writablePropertyNames;

  /** key 属性名，value 该属性名对应的 setter方法调用器 */
  private final Map<String, Invoker> setMethods = new HashMap<>();
  private final Map<String, Invoker> getMethods = new HashMap<>();

  /** key 属性名称，value 该属性 setter方法的返回值类型 */
  private final Map<String, Class<?>> setTypes = new HashMap<>();
  private final Map<String, Class<?>> getTypes = new HashMap<>();

  /** type 的默认构造方法 */
  private Constructor<?> defaultConstructor;

  /** 所有属性名称的集合 */
  private Map<String, String> caseInsensitivePropertyMap = new HashMap<>();

  /**
   * 里面的大部分方法都是通过简单的 JDK反射操作 实现的
   * @param clazz
   */
  public Reflector(Class<?> clazz) {
    type = clazz;
    addDefaultConstructor(clazz);

    // 处理 clazz 中的 所有getter方法，填充 getMethods集合 和 getTypes集合
    addGetMethods(clazz);
    addSetMethods(clazz);

    // 处理没有 getter、setter方法 的字段
    addFields(clazz);

    // 根据 getMethods、setMethods集合 初始化可读、可写的属性
    readablePropertyNames = getMethods.keySet().toArray(new String[0]);
    writablePropertyNames = setMethods.keySet().toArray(new String[0]);

    // 初始化 caseInsensitivePropertyMap集合，key 属性名的大写，value 属性名
    for (String propName : readablePropertyNames) {
      caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
    }
    for (String propName : writablePropertyNames) {
      caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
    }
  }
}
```

### 1.2 ReflectorFactory

顾名思义，Reflector 的工厂模式，跟大部分工厂类一样，里面肯定有通过标识获取对象的方法。类的设计也遵照了 接口，实现类的模式，虽然本接口只有一个默认实现。

```java
public interface ReflectorFactory {

  boolean isClassCacheEnabled();

  void setClassCacheEnabled(boolean classCacheEnabled);

  /**
   * 主要看一下这个方法，通过 JavaBean 的 clazz 获取该 JavaBean 对应的 Reflector
   */
  Reflector findForClass(Class<?> type);
}

public class DefaultReflectorFactory implements ReflectorFactory {
  private boolean classCacheEnabled = true;

  /** 大部分容器及工厂设计模式的管用伎俩，key：JavaBean的clazz，value：JavaBean对应的Reflector实例 */
  private final ConcurrentMap<Class<?>, Reflector> reflectorMap = new ConcurrentHashMap<>();

  /**
   * 实例化一个 ConcurrentMap全局变量，然后暴露一个方法从 map 中获取目标对象，这种设计是很多框架都会用的
   */
  @Override
  public Reflector findForClass(Class<?> type) {
    if (classCacheEnabled) {
      // synchronized (type) removed see issue #461
      return reflectorMap.computeIfAbsent(type, Reflector::new);
    } else {
      return new Reflector(type);
    }
  }

  public DefaultReflectorFactory() {
  }

  @Override
  public boolean isClassCacheEnabled() {
    return classCacheEnabled;
  }

  @Override
  public void setClassCacheEnabled(boolean classCacheEnabled) {
    this.classCacheEnabled = classCacheEnabled;
  }
}

/**
 * 支持定制化 ReflectorFactory
 */
public class CustomReflectorFactory extends DefaultReflectorFactory {

}
```

### 1.3 ObjectFactory

该类也是接口加一个默认实现类，并且支持自定义扩展，Mybatis 中有很多这样的设计方式。

```java
/**
 * MyBatis uses an ObjectFactory to create all needed new Objects.
 */
public interface ObjectFactory {

  /**
   * Sets configuration properties.
   */
  default void setProperties(Properties properties) {
    // NOP
  }

  /**
   * Creates a new object with default constructor.
   */
  <T> T create(Class<T> type);

  /**
   * Creates a new object with the specified constructor and params.
   */
  <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);

  /**
   * Returns true if this object can have a set of other objects.
   * It's main purpose is to support non-java.util.Collection objects like Scala collections.
   */
  <T> boolean isCollection(Class<T> type);

}

/**
 * ObjectFactory接口 的唯一直接实现，反射工厂，根据传入的参数列表，选择
 * 合适的构造函数实例化对象，不传参数，则直接调用其无参构造方法
 */
public class DefaultObjectFactory implements ObjectFactory, Serializable {

  private static final long serialVersionUID = -8855120656740914948L;

  @Override
  public <T> T create(Class<T> type) {
    return create(type, null, null);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
    Class<?> classToCreate = resolveInterface(type);
    // we know types are assignable
    return (T) instantiateClass(classToCreate, constructorArgTypes, constructorArgs);
  }

  /**
   * 通过反射来实例化给定的类，如果调用无参构造方法，则直接 constructor.newInstance()
   * 如果有参，则根据参数类型和参数值进行调用
   */
  private  <T> T instantiateClass(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
    try {
      Constructor<T> constructor;
      if (constructorArgTypes == null || constructorArgs == null) {
        constructor = type.getDeclaredConstructor();
        try {
          return constructor.newInstance();
        } catch (IllegalAccessException e) {
          if (Reflector.canControlMemberAccessible()) {
            constructor.setAccessible(true);
            return constructor.newInstance();
          } else {
            throw e;
          }
        }
      }
      constructor = type.getDeclaredConstructor(constructorArgTypes.toArray(new Class[constructorArgTypes.size()]));
      try {
        return constructor.newInstance(constructorArgs.toArray(new Object[constructorArgs.size()]));
      } catch (IllegalAccessException e) {
        if (Reflector.canControlMemberAccessible()) {
          constructor.setAccessible(true);
          return constructor.newInstance(constructorArgs.toArray(new Object[constructorArgs.size()]));
        } else {
          throw e;
        }
      }
    } catch (Exception e) {
      String argTypes = Optional.ofNullable(constructorArgTypes).orElseGet(Collections::emptyList)
          .stream().map(Class::getSimpleName).collect(Collectors.joining(","));
      String argValues = Optional.ofNullable(constructorArgs).orElseGet(Collections::emptyList)
          .stream().map(String::valueOf).collect(Collectors.joining(","));
      throw new ReflectionException("Error instantiating " + type + " with invalid types (" + argTypes + ") or values (" + argValues + "). Cause: " + e, e);
    }
  }
}
```

## 2 类型转换

类型转换是实现 ORM 的重要一环，由于数据库中的数据类型与 Java 语言 的数据类型并不对等，所以在 PrepareStatement 为 sql 语句 绑定参数时，需要从 Java 类型 转换成 JDBC 类型，而从结果集获取数据时，又要将 JDBC 类型 转换成 Java 类型，Mybatis 使用 TypeHandler 完成了上述的双向转换。

### 2.1 JdbcType

Mybatis 通过 JdbcType 这个枚举类型代表了 JDBC 中的数据类型。

```java
/**
 * 该枚举类描述了 JDBC 中的数据类型
 */
public enum JdbcType {
  /*
   * This is added to enable basic support for the
   * ARRAY data type - but a custom type handler is still required
   */
  ARRAY(Types.ARRAY),
  BIT(Types.BIT),
  TINYINT(Types.TINYINT),
  SMALLINT(Types.SMALLINT),
  INTEGER(Types.INTEGER),
  BIGINT(Types.BIGINT),
  FLOAT(Types.FLOAT),
  REAL(Types.REAL),
  DOUBLE(Types.DOUBLE),
  NUMERIC(Types.NUMERIC),
  DECIMAL(Types.DECIMAL),
  CHAR(Types.CHAR),
  VARCHAR(Types.VARCHAR),
  LONGVARCHAR(Types.LONGVARCHAR),
  DATE(Types.DATE),
  TIME(Types.TIME),
  TIMESTAMP(Types.TIMESTAMP),
  BINARY(Types.BINARY),
  VARBINARY(Types.VARBINARY),
  LONGVARBINARY(Types.LONGVARBINARY),
  NULL(Types.NULL),
  OTHER(Types.OTHER),
  BLOB(Types.BLOB),
  CLOB(Types.CLOB),
  BOOLEAN(Types.BOOLEAN),
  CURSOR(-10), // Oracle
  UNDEFINED(Integer.MIN_VALUE + 1000),
  NVARCHAR(Types.NVARCHAR), // JDK6
  NCHAR(Types.NCHAR), // JDK6
  NCLOB(Types.NCLOB), // JDK6
  STRUCT(Types.STRUCT),
  JAVA_OBJECT(Types.JAVA_OBJECT),
  DISTINCT(Types.DISTINCT),
  REF(Types.REF),
  DATALINK(Types.DATALINK),
  ROWID(Types.ROWID), // JDK6
  LONGNVARCHAR(Types.LONGNVARCHAR), // JDK6
  SQLXML(Types.SQLXML), // JDK6
  DATETIMEOFFSET(-155), // SQL Server 2008
  TIME_WITH_TIMEZONE(Types.TIME_WITH_TIMEZONE), // JDBC 4.2 JDK8
  TIMESTAMP_WITH_TIMEZONE(Types.TIMESTAMP_WITH_TIMEZONE); // JDBC 4.2 JDK8

  public final int TYPE_CODE;

  /** 该静态集合维护了 常量编码 与  JdbcType 之间的关系 */
  private static Map<Integer,JdbcType> codeLookup = new HashMap<>();

  static {
    for (JdbcType type : JdbcType.values()) {
      codeLookup.put(type.TYPE_CODE, type);
    }
  }

  JdbcType(int code) {
    this.TYPE_CODE = code;
  }

  public static JdbcType forCode(int code)  {
    return codeLookup.get(code);
  }

}
```

### 2.2 TypeHandler

TypeHandler 是 Mybatis 中所有类型转换器的顶层接口，主要用于实现数据从 Java 类型 到 JdbcType 类型 的相互转换。

```java
public interface TypeHandler<T> {

  /** 通过 PreparedStatement 为 SQL语句 绑定参数时，将数据从 Java类型 转换为 JDBC类型 */
  void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;

  /** 从结果集获取数据时，将数据由 JDBC类型 转换成 Java类型 */
  T getResult(ResultSet rs, String columnName) throws SQLException;

  T getResult(ResultSet rs, int columnIndex) throws SQLException;

  T getResult(CallableStatement cs, int columnIndex) throws SQLException;

}

/**
 * 可用于实现自定义的 TypeHandler
 */
public abstract class BaseTypeHandler<T> extends TypeReference<T> implements TypeHandler<T> {

  /**
   * 只是处理了一些数据为空的特殊情况，非空数据的处理都交给子类去处理
   */
  @Override
  public void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
    if (parameter == null) {
      if (jdbcType == null) {
        throw new TypeException("JDBC requires that the JdbcType must be specified for all nullable parameters.");
      }
      try {
        ps.setNull(i, jdbcType.TYPE_CODE);
      } catch (SQLException e) {
        throw new TypeException("Error setting null for parameter #" + i + " with JdbcType " + jdbcType + " . "
              + "Try setting a different JdbcType for this parameter or a different jdbcTypeForNull configuration property. "
              + "Cause: " + e, e);
      }
    } else {
      try {
        setNonNullParameter(ps, i, parameter, jdbcType);
      } catch (Exception e) {
        throw new TypeException("Error setting non null for parameter #" + i + " with JdbcType " + jdbcType + " . "
              + "Try setting a different JdbcType for this parameter or a different configuration property. "
              + "Cause: " + e, e);
      }
    }
  }

  @Override
  public T getResult(ResultSet rs, String columnName) throws SQLException {
    try {
      return getNullableResult(rs, columnName);
    } catch (Exception e) {
      throw new ResultMapException("Error attempting to get column '" + columnName + "' from result set.  Cause: " + e, e);
    }
  }
}


public class IntegerTypeHandler extends BaseTypeHandler<Integer> {

  /**
   * NonNull 就是 NoneNull，非空的意思
   */
  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Integer parameter, JdbcType jdbcType)
      throws SQLException {
    // IntegerTypeHandler 就调用 PreparedStatement 的 setInt()方法
    // BooleanTypeHandler 就调用 PreparedStatement 的 setBoolean()方法
    // 其它的基本数据类型，以此类推
    ps.setInt(i, parameter);
  }

  @Override
  public Integer getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    int result = rs.getInt(columnName);
    return result == 0 && rs.wasNull() ? null : result;
  }

  @Override
  public Integer getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    int result = rs.getInt(columnIndex);
    return result == 0 && rs.wasNull() ? null : result;
  }

  @Override
  public Integer getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    int result = cs.getInt(columnIndex);
    return result == 0 && cs.wasNull() ? null : result;
  }
}
```

TypeHandler 主要用于单个参数的类型转换，如果要将多个列的值转换成一个 Java 对象，可以在映射文件中定义合适的映射规则 &lt;resultMap&gt; 完成映射。

### 2.3 TypeHandlerRegistry

TypeHandlerRegistry 主要负责管理所有已知的 TypeHandler，Mybatis 在初始化过程中会为所有已知的 TypeHandler 创建对象，并注册到 TypeHandlerRegistry。

```java
  // TypeHandlerRegistry 中的核心字段如下

  /** 该集合主要用于从结果集读取数据时，将数据从 JDBC类型 转换成 Java类型 */
  private final Map<JdbcType, TypeHandler<?>>  jdbcTypeHandlerMap = new EnumMap<>(JdbcType.class);

  /**
   * 记录了 Java类型 向指定 JdbcType 转换时，需要使用的 TypeHandler对象。
   * 如：String 可能转换成数据库的 char、varchar 等多种类型，所以存在一对多的关系
   */
  private final Map<Type, Map<JdbcType, TypeHandler<?>>> typeHandlerMap = new ConcurrentHashMap<>();

  /** key：TypeHandler 的类型；value：该 TypeHandler类型 对应的 TypeHandler对象 */
  private final Map<Class<?>, TypeHandler<?>> allTypeHandlersMap = new HashMap<>();
```

**1、注册 TypeHandler 对象**  
TypeHandlerRegistry 中的 register()方法 实现了注册 TypeHandler 对象 的功能，该方法存在多种重载，但大多数 register()方法 最终都会走 register(Type javaType, JdbcType jdbcType, TypeHandler<?> handler) 的处理逻辑，该重载方法中分别指定了 TypeHandler 能够处理的 Java 类型、JDBC 类型、TypeHandler 对象。

```java
  /**
   * TypeHandlerRegistry 中对 register()方法 实现了多种重载，本 register()方法
   * 被很多重载方法调用，用来完成注册功能。
   */
  private void register(Type javaType, JdbcType jdbcType, TypeHandler<?> handler) {
    if (javaType != null) {
      Map<JdbcType, TypeHandler<?>> map = typeHandlerMap.get(javaType);
      if (map == null || map == NULL_TYPE_HANDLER_MAP) {
        map = new HashMap<>();
        typeHandlerMap.put(javaType, map);
      }
      map.put(jdbcType, handler);
    }
    allTypeHandlersMap.put(handler.getClass(), handler);
  }
```

另外，TypeHandlerRegistry 还提供了扫描并注册指定包目录下 TypeHandler 实现类 的 register()方法 重载。

```java
  /**
   * 从指定 包名packageName 中获取自定义的 TypeHandler实现类
   */
  public void register(String packageName) {
    ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<>();
    // 查找指定包下的 TypeHandler接口实现类
    resolverUtil.find(new ResolverUtil.IsA(TypeHandler.class), packageName);
    Set<Class<? extends Class<?>>> handlerSet = resolverUtil.getClasses();
    for (Class<?> type : handlerSet) {
      // 忽略掉 内部类、接口 及 抽象类
      if (!type.isAnonymousClass() && !type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
        register(type);
      }
    }
  }
```

最后看一下 TypeHandlerRegistry 的构造方法，其通过多种 register()方法 重载，完成了所有已知的 TypeHandler 的重载。

```java
  /**
   * 进行 Java 及 JDBC基本数据类型 的 TypeHandler 注册
   * 除了注册 Mybatis 提供的 基本TypeHandler 外，我们也可以添加自定义的 TypeHandler
   * 接口实现，在 mybatis-config.xml配置文件 中 <typeHandlers>节点 下添加相应的
   * <typeHandlers>节点配置，并指定自定义的 TypeHandler实现类。Mybatis 在初始化时
   * 会解析该节点，并将 TypeHandler类型 的对象注册到 TypeHandlerRegistry 中供 Mybatis 后续使用
   */
  public TypeHandlerRegistry() {
    register(Boolean.class, new BooleanTypeHandler());
    register(boolean.class, new BooleanTypeHandler());
    register(JdbcType.BOOLEAN, new BooleanTypeHandler());
    register(JdbcType.BIT, new BooleanTypeHandler());

    register(Byte.class, new ByteTypeHandler());
    register(byte.class, new ByteTypeHandler());
    register(JdbcType.TINYINT, new ByteTypeHandler());

    register(Short.class, new ShortTypeHandler());
    register(short.class, new ShortTypeHandler());
    register(JdbcType.SMALLINT, new ShortTypeHandler());

    register(Integer.class, new IntegerTypeHandler());
    register(int.class, new IntegerTypeHandler());
    register(JdbcType.INTEGER, new IntegerTypeHandler());

    register(Long.class, new LongTypeHandler());
    register(long.class, new LongTypeHandler());

    register(Float.class, new FloatTypeHandler());
    register(float.class, new FloatTypeHandler());
    register(JdbcType.FLOAT, new FloatTypeHandler());

    register(Double.class, new DoubleTypeHandler());
    register(double.class, new DoubleTypeHandler());
    register(JdbcType.DOUBLE, new DoubleTypeHandler());

    register(Reader.class, new ClobReaderTypeHandler());
    register(String.class, new StringTypeHandler());
    register(String.class, JdbcType.CHAR, new StringTypeHandler());
    register(String.class, JdbcType.CLOB, new ClobTypeHandler());
    register(String.class, JdbcType.VARCHAR, new StringTypeHandler());
    register(String.class, JdbcType.LONGVARCHAR, new StringTypeHandler());
    register(String.class, JdbcType.NVARCHAR, new NStringTypeHandler());
    register(String.class, JdbcType.NCHAR, new NStringTypeHandler());
    register(String.class, JdbcType.NCLOB, new NClobTypeHandler());
    register(JdbcType.CHAR, new StringTypeHandler());
    register(JdbcType.VARCHAR, new StringTypeHandler());
    register(JdbcType.CLOB, new ClobTypeHandler());
    register(JdbcType.LONGVARCHAR, new StringTypeHandler());
    register(JdbcType.NVARCHAR, new NStringTypeHandler());
    register(JdbcType.NCHAR, new NStringTypeHandler());
    register(JdbcType.NCLOB, new NClobTypeHandler());

    register(Object.class, JdbcType.ARRAY, new ArrayTypeHandler());
    register(JdbcType.ARRAY, new ArrayTypeHandler());

    register(BigInteger.class, new BigIntegerTypeHandler());
    register(JdbcType.BIGINT, new LongTypeHandler());

    register(BigDecimal.class, new BigDecimalTypeHandler());
    register(JdbcType.REAL, new BigDecimalTypeHandler());
    register(JdbcType.DECIMAL, new BigDecimalTypeHandler());
    register(JdbcType.NUMERIC, new BigDecimalTypeHandler());



    register(String.class, JdbcType.SQLXML, new SqlxmlTypeHandler());

    register(Instant.class, new InstantTypeHandler());
    register(LocalDateTime.class, new LocalDateTimeTypeHandler());
    register(LocalDate.class, new LocalDateTypeHandler());
    register(LocalTime.class, new LocalTimeTypeHandler());
    register(OffsetDateTime.class, new OffsetDateTimeTypeHandler());
    register(OffsetTime.class, new OffsetTimeTypeHandler());
    register(ZonedDateTime.class, new ZonedDateTimeTypeHandler());
    register(Month.class, new MonthTypeHandler());
    register(Year.class, new YearTypeHandler());
    register(YearMonth.class, new YearMonthTypeHandler());
    register(JapaneseDate.class, new JapaneseDateTypeHandler());
  }
```

**2、查找 TypeHandler**  
TypeHandlerRegistry 其实就是一个容器，前面注册了一堆东西，也就是为了方便获取，其对应的方法为 getTypeHandler()，该方法也存在多种重载，其中最重要的一个重载为 getTypeHandler(Type type, JdbcType jdbcType)，它会根据指定的 Java 类型 和 JdbcType 类型 查找相应的 TypeHandler 对象。

```java
  /**
   * 获取 TypeHandler对象
   * getTypeHandler()方法 亦存在多种重载，而本重载方法被其它多个重载方法调用
   */
  private <T> TypeHandler<T> getTypeHandler(Type type, JdbcType jdbcType) {
    if (ParamMap.class.equals(type)) {
      return null;
    }
    // Java数据类型 与 JDBC数据类型 的关系往往是一对多，
    // 所以一般会先根据 Java数据类型 获取 Map<JdbcType, TypeHandler<?>>对象
    // 再根据 JDBC数据类型 获取对应的 TypeHandler对象
    Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = getJdbcHandlerMap(type);
    TypeHandler<?> handler = null;
    if (jdbcHandlerMap != null) {
      handler = jdbcHandlerMap.get(jdbcType);
      if (handler == null) {
        handler = jdbcHandlerMap.get(null);
      }
      if (handler == null) {
        // #591
        handler = pickSoleHandler(jdbcHandlerMap);
      }
    }
    // type drives generics here
    return (TypeHandler<T>) handler;
  }
```

除了 Mabatis 本身自带的 TypeHandler 实现，我们还可以添加自定义的 TypeHandler 实现类，在配置文件 mybatis-config.xml 中的 &lt;typeHandler&gt; 标签下配置好 自定义 TypeHandler，Mybatis 就会在初始化时解析该标签内容，完成 自定义 TypeHandler 的注册。

在数据持久层，数据源和事务是两个非常重要的组件，对数据持久层的影响很大，在实际开发中，一般会使用 Mybatis 集成第三方数据源组件，如：c3p0、Druid，另外，Mybatis 也提供了自己的数据库连接池实现，本文会通过 Mybatis 的源码实现来了解数据库连接池的设计。而事务方面，一般使用 Spring 进行事务的管理，这里不做详细分析。下面我们看一下 Mybatis 是如何对这两部分进行封装的。

# 1 DataSource

常见的数据源都会实现 javax.sql.DataSource 接口，Mybatis 中提供了两个该接口的实现类，分别是：PooledDataSource 和 UnpooledDataSource，并使用不同的工厂类分别管理这两个类的对象。

### 1.1 DataSourceFactory

DataSourceFactory 系列类 的设计比较简单，DataSourceFactory 作为顶级接口，UnpooledDataSourceFactory 实现了该接口，PooledDataSourceFactory 又继承了 UnpooledDataSourceFactory。

```java
public interface DataSourceFactory {

  // 设置 DataSource 的属性，一般紧跟在 DataSource 初始化之后
  void setProperties(Properties props);

  // 获取 DataSource对象
  DataSource getDataSource();
}


public class UnpooledDataSourceFactory implements DataSourceFactory {

  private static final String DRIVER_PROPERTY_PREFIX = "driver.";
  private static final int DRIVER_PROPERTY_PREFIX_LENGTH = DRIVER_PROPERTY_PREFIX.length();

  protected DataSource dataSource;

  // 在实例化该工厂时，就完成了 DataSource 的实例化
  public UnpooledDataSourceFactory() {
    this.dataSource = new UnpooledDataSource();
  }

  @Override
  public void setProperties(Properties properties) {
    Properties driverProperties = new Properties();
    // 创建 dataSource 对应的 MetaObject
    MetaObject metaDataSource = SystemMetaObject.forObject(dataSource);
    // 处理 properties 中配置的数据源信息
    for (Object key : properties.keySet()) {
      String propertyName = (String) key;
      if (propertyName.startsWith(DRIVER_PROPERTY_PREFIX)) {
        // 以 "driver." 开头的配置项是对 DataSource 的配置，将其记录到 driverProperties 中
        String value = properties.getProperty(propertyName);
        driverProperties.setProperty(propertyName.substring(DRIVER_PROPERTY_PREFIX_LENGTH), value);
      } else if (metaDataSource.hasSetter(propertyName)) {
        String value = (String) properties.get(propertyName);
        Object convertedValue = convertValue(metaDataSource, propertyName, value);
        metaDataSource.setValue(propertyName, convertedValue);
      } else {
        throw new DataSourceException("Unknown DataSource property: " + propertyName);
      }
    }
    if (driverProperties.size() > 0) {
      // 设置数据源 UnpooledDataSource 的 driverProperties属性，
      // PooledDataSource 中持有 UnpooledDataSource对象
      metaDataSource.setValue("driverProperties", driverProperties);
    }
  }

  @Override
  public DataSource getDataSource() {
    return dataSource;
  }
}


public class PooledDataSourceFactory extends UnpooledDataSourceFactory {

  // 与 UnpooledDataSourceFactory 的不同之处是，其初始化的 DataSource 为 PooledDataSource
  public PooledDataSourceFactory() {
    this.dataSource = new PooledDataSource();
  }
}
```

### 1.2 UnpooledDataSource

本实现类实现了 DataSource 接口 中的 getConnection() 及其重载方法，用于获取数据库连接。其中的主要属性及方法如下：

```java
public class UnpooledDataSource implements DataSource {

  // 加载 Driver驱动类 的类加载器
  private ClassLoader driverClassLoader;

  // 数据库连接驱动的相关配置，通过 UnpooledDataSourceFactory 的 setProperties()方法 设置进来的
  private Properties driverProperties;

  // 缓存所有已注册的 数据库连接驱动Driver
  private static Map<String, Driver> registeredDrivers = new ConcurrentHashMap<>();

  // 数据库连接驱动名称
  private String driver;
  // 数据库url
  private String url;
  // 用户名
  private String username;
  // 密码
  private String password;

  // 是否自动提交事务
  private Boolean autoCommit;
  // 默认的事务隔离级别
  private Integer defaultTransactionIsolationLevel;
  // 默认的网络连接超时时间
  private Integer defaultNetworkTimeout;

  /**
   * UnpooledDataSource 被加载时，会通过该静态代码块将已经在 DriverManager
   * 中注册的 JDBC Driver 注册到 registeredDrivers 中
   */
  static {
    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      registeredDrivers.put(driver.getClass().getName(), driver);
    }
  }

  // getConnection() 及其重载方法、doGetConnection(String username, String password)方法
  // 最终都会调用本方法
  private Connection doGetConnection(Properties properties) throws SQLException {
    // 初始化数据库驱动，该方法会创建配置中指定的 Driver对象，
    // 并将其注册到 DriverManager 和 registeredDrivers 中
    initializeDriver();
    Connection connection = DriverManager.getConnection(url, properties);
    // 配置数据库连接属性，如：连接超时时间、是否自动提交事务、事务隔离级别
    configureConnection(connection);
    return connection;
  }

  private synchronized void initializeDriver() throws SQLException {
    // 判断驱动是否已注册
    if (!registeredDrivers.containsKey(driver)) {
      Class<?> driverType;
      try {
        if (driverClassLoader != null) {
          // 注册驱动
          driverType = Class.forName(driver, true, driverClassLoader);
        } else {
          driverType = Resources.classForName(driver);
        }
        // 通过反射获取 Driver实例对象
        Driver driverInstance = (Driver)driverType.newInstance();
        // 注册驱动到 DriverManager，DriverProxy 是 UnpooledDataSource 的内部类
        // 也是 Driver 的静态代理类
        DriverManager.registerDriver(new DriverProxy(driverInstance));
        // 将 driver 缓存到 registeredDrivers
        registeredDrivers.put(driver, driverInstance);
      } catch (Exception e) {
        throw new SQLException("Error setting driver on UnpooledDataSource. Cause: " + e);
      }
    }
  }

  private void configureConnection(Connection conn) throws SQLException {
    // 连接超时时间
    if (defaultNetworkTimeout != null) {
      conn.setNetworkTimeout(Executors.newSingleThreadExecutor(), defaultNetworkTimeout);
    }
    // 是否自动提交事务
    if (autoCommit != null && autoCommit != conn.getAutoCommit()) {
      conn.setAutoCommit(autoCommit);
    }
    // 事务隔离级别
    if (defaultTransactionIsolationLevel != null) {
      conn.setTransactionIsolation(defaultTransactionIsolationLevel);
    }
  }
}
```

### 1.3 PooledDataSource

数据库建立连接是非常耗时的，且并发的连接数也非常有限。而数据库连接池可以实现数据库的重用、提高响应速度、防止数据库因连接过多而假死等。
**数据库连接池的设计思路一般为：**

1. **连接池初始化时创建一定数量的连接，并添加到连接池中备用；**
2. **当程序需要使用数据库连接时，从连接池中请求，用完后会将其返还给连接池，而不是直接关闭；**
3. **连接池会控制总连接上限及空闲连接上线，如果连接池中的连接总数已达上限，且都被占用，后续的连接请求会短暂阻塞后重新尝试获取连接，如此循环，直到有连接可用；**
4. **如果连接池中空闲连接较多，已达到空闲连接上限，则返回的连接会被关闭掉，以降低系统开销。**

PooledDataSource 实现了简易的数据库连接池功能，其创建数据库连接的功能依赖了上面的 UnpooledDataSource。

#### 1.3.1 PooledConnection

PooledDataSource 通过管理 PooledConnection 来实现对 java.sql.Connection 的管理。PooledConnection 封装了 java.sql.Connection 数据库连接对象 及其代理对象（JDK 动态代理生成的）。PooledConnection 继承了 JDK 动态代理 的 InvocationHandler 接口。

```java
class PooledConnection implements InvocationHandler {

  // 记录当前 PooledConnection对象 所属的 PooledDataSource对象
  // 当调用 close()方法 时会将 PooledConnection 放回该 PooledDataSource
  private final PooledDataSource dataSource;
  // 真正的数据库连接对象
  private final Connection realConnection;
  // 代理连接对象
  private final Connection proxyConnection;
  // 从连接池中取出该连接时的时间戳
  private long checkoutTimestamp;
  // 创建该连接时的时间戳
  private long createdTimestamp;
  // 最后一次使用的 时间戳
  private long lastUsedTimestamp;
  // 由 数据库URL、用户名、密码 计算出来的 hash值，可用于标识该连接所在的连接池
  private int connectionTypeCode;
  // 检测当前 PooledConnection连接池连接对象 是否有效，主要用于 防止程序通过 close()方法 将
  // 连接还给连接池之后，依然通过该连接操作数据库
  private boolean valid;

  /**
   * invoke()方法 是本类的重点实现，也是 proxyConnection代理连接对象 的代理逻辑实现
   * 它会对 close()方法 的调用进行处理，并在调用 realConnection对象 的方法之前进行校验
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();
    // 如果调用的是 close()方法，则将其放进连接池，而不是真的关闭连接
    if (CLOSE.hashCode() == methodName.hashCode() && CLOSE.equals(methodName)) {
      dataSource.pushConnection(this);
      return null;
    }
    try {
      if (!Object.class.equals(method.getDeclaringClass())) {
        // 通过上面的 valid字段 校验连接是否有效
        checkConnection();
      }
      // 调用 realConnection对象 的对应方法
      return method.invoke(realConnection, args);
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }

  }

  private void checkConnection() throws SQLException {
    if (!valid) {
      throw new SQLException("Error accessing PooledConnection. Connection is invalid.");
    }
  }
}
```

#### 1.3.2 PoolState

PoolState 主要用于管理 PooledConnection 对象状态，其通过持有两个 List&lt;PooledConnection&gt;集合 分别管理空闲状态的连接 和 活跃状态的连接。另外，PoolState 还定义了一系列用于统计的字段。

```java
public class PoolState {

  // 所属的连接池对象
  protected PooledDataSource dataSource;

  // 空闲的连接
  protected final List<PooledConnection> idleConnections = new ArrayList<>();
  // 活跃的连接
  protected final List<PooledConnection> activeConnections = new ArrayList<>();

  // 请求数据库连接的次数
  protected long requestCount = 0;
  // 获取连接的累计时间（accumulate累计）
  protected long accumulatedRequestTime = 0;
  // CheckoutTime = 记录 应用从连接池取出连接到归还连接的时长
  // accumulatedCheckoutTime = 所有连接累计的CheckoutTime
  protected long accumulatedCheckoutTime = 0;
  // 超时连接的个数（当连接长时间未归还给连接池时，会被认为连接超时）
  protected long claimedOverdueConnectionCount = 0;
  // 累计超时时间
  protected long accumulatedCheckoutTimeOfOverdueConnections = 0;
  // 累计等待时间
  protected long accumulatedWaitTime = 0;
  // 等待次数
  protected long hadToWaitCount = 0;
  // 无效的连接数
  protected long badConnectionCount = 0;

  public PoolState(PooledDataSource dataSource) {
    this.dataSource = dataSource;
  }

  public synchronized long getRequestCount() {
    return requestCount;
  }

  public synchronized long getAverageRequestTime() {
    return requestCount == 0 ? 0 : accumulatedRequestTime / requestCount;
  }

  public synchronized long getAverageWaitTime() {
    return hadToWaitCount == 0 ? 0 : accumulatedWaitTime / hadToWaitCount;

  }

  public synchronized long getHadToWaitCount() {
    return hadToWaitCount;
  }

  public synchronized long getBadConnectionCount() {
    return badConnectionCount;
  }

  public synchronized long getClaimedOverdueConnectionCount() {
    return claimedOverdueConnectionCount;
  }

  public synchronized long getAverageOverdueCheckoutTime() {
    return claimedOverdueConnectionCount == 0 ? 0 : accumulatedCheckoutTimeOfOverdueConnections / claimedOverdueConnectionCount;
  }

  public synchronized long getAverageCheckoutTime() {
    return requestCount == 0 ? 0 : accumulatedCheckoutTime / requestCount;
  }

  public synchronized int getIdleConnectionCount() {
    return idleConnections.size();
  }

  public synchronized int getActiveConnectionCount() {
    return activeConnections.size();
  }

  @Override
  public synchronized String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("\n===CONFINGURATION==============================================");
    builder.append("\n jdbcDriver                     ").append(dataSource.getDriver());
    builder.append("\n jdbcUrl                        ").append(dataSource.getUrl());
    builder.append("\n jdbcUsername                   ").append(dataSource.getUsername());
    builder.append("\n jdbcPassword                   ").append(dataSource.getPassword() == null ? "NULL" : "************");
    builder.append("\n poolMaxActiveConnections       ").append(dataSource.poolMaximumActiveConnections);
    builder.append("\n poolMaxIdleConnections         ").append(dataSource.poolMaximumIdleConnections);
    builder.append("\n poolMaxCheckoutTime            ").append(dataSource.poolMaximumCheckoutTime);
    builder.append("\n poolTimeToWait                 ").append(dataSource.poolTimeToWait);
    builder.append("\n poolPingEnabled                ").append(dataSource.poolPingEnabled);
    builder.append("\n poolPingQuery                  ").append(dataSource.poolPingQuery);
    builder.append("\n poolPingConnectionsNotUsedFor  ").append(dataSource.poolPingConnectionsNotUsedFor);
    builder.append("\n ---STATUS-----------------------------------------------------");
    builder.append("\n activeConnections              ").append(getActiveConnectionCount());
    builder.append("\n idleConnections                ").append(getIdleConnectionCount());
    builder.append("\n requestCount                   ").append(getRequestCount());
    builder.append("\n averageRequestTime             ").append(getAverageRequestTime());
    builder.append("\n averageCheckoutTime            ").append(getAverageCheckoutTime());
    builder.append("\n claimedOverdue                 ").append(getClaimedOverdueConnectionCount());
    builder.append("\n averageOverdueCheckoutTime     ").append(getAverageOverdueCheckoutTime());
    builder.append("\n hadToWait                      ").append(getHadToWaitCount());
    builder.append("\n averageWaitTime                ").append(getAverageWaitTime());
    builder.append("\n badConnectionCount             ").append(getBadConnectionCount());
    builder.append("\n===============================================================");
    return builder.toString();
  }
}
```

#### 1.3.3 PooledDataSource

PooledDataSource 管理的数据库连接对象 是由其持有的 UnpooledDataSource 对象 创建的，并由 PoolState 管理所有连接的状态。
PooledDataSource 的 getConnection()方法 会首先调用 popConnection()方法 获取 PooledConnection 对象，然后通过 PooledConnection 的 getProxyConnection()方法 获取数据库连接的代理对象。popConnection()方法 是 PooledDataSource 的核心逻辑之一，其整体的逻辑关系如下图：

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/%E6%95%B0%E6%8D%AE%E5%BA%93%E8%BF%9E%E6%8E%A5%E6%B1%A0%E6%B5%81%E7%A8%8B%E5%9B%BE-b52c7f.png)

```java
public class PooledDataSource implements DataSource {

  private static final Log log = LogFactory.getLog(PooledDataSource.class);

  // 管理连接池状态 并统计连接信息
  private final PoolState state = new PoolState(this);

  // 该对象用于生成真正的数据库连接对象，构造函数中会初始化该字段
  private final UnpooledDataSource dataSource;

  // 最大活跃连接数
  protected int poolMaximumActiveConnections = 10;
  // 最大空闲连接数
  protected int poolMaximumIdleConnections = 5;
  // 最大Checkout时长
  protected int poolMaximumCheckoutTime = 20000;
  // 在无法获取连接时，线程需要等待的时间
  protected int poolTimeToWait = 20000;
  // 本地坏连接最大数
  protected int poolMaximumLocalBadConnectionTolerance = 3;
  // 检测数据库连接是否可用时，给数据库发送的sql语句
  protected String poolPingQuery = "NO PING QUERY SET";
  // 是否允许发送上述语句
  protected boolean poolPingEnabled;
  // 当连接超过poolPingConnectionsNotUsedFor毫秒未使用，
  // 就发送一次上述sql，检测连接连接是否正常
  protected int poolPingConnectionsNotUsedFor;

  // 根据数据库URL、用户名、密码 生成的一个hash值，
  // 该hash值用于标记当前的连接池，在构造函数中初始化
  private int expectedConnectionTypeCode;

  /**
   * 下面的两个 getConnection()方法 都会调用 popConnection()
   * 获取 PooledConnection对象，然后调用该对象的 getProxyConnection()方法
   * 获取数据库连接的代理对象
   */
  @Override
  public Connection getConnection() throws SQLException {
    return popConnection(dataSource.getUsername(), dataSource.getPassword()).getProxyConnection();
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return popConnection(username, password).getProxyConnection();
  }

  /**
   * 本方法实现了连接池获取连接对象的具体逻辑，是 PooledDataSource 的核心逻辑之一
   */
  private PooledConnection popConnection(String username, String password) throws SQLException {
    boolean countedWait = false;
    PooledConnection conn = null;
    long t = System.currentTimeMillis();
    int localBadConnectionCount = 0;

    // 循环获取数据库连接对象，直到获取成功
    while (conn == null) {
      // 连接池的连接是公共资源，要对线程加锁
      synchronized (state) {
        // 如果连接池中有空闲的 数据库连接对象，就取出一个
        if (!state.idleConnections.isEmpty()) {
          conn = state.idleConnections.remove(0);
          if (log.isDebugEnabled()) {
            log.debug("Checked out connection " + conn.getRealHashCode() + " from pool.");
          }
        } else {
          // 没有空闲的连接对象，就判断一下 活跃的连接数是否已达 设定的峰值
          if (state.activeConnections.size() < poolMaximumActiveConnections) {
            // 还没达到峰值 就创建一个新的连接
            conn = new PooledConnection(dataSource.getConnection(), this);
            if (log.isDebugEnabled()) {
              log.debug("Created connection " + conn.getRealHashCode() + ".");
            }
          } else {
            // 如果活跃的连接已达上限，就取出最老的活跃连接对象，判断其是否超时
            PooledConnection oldestActiveConnection = state.activeConnections.get(0);
            long longestCheckoutTime = oldestActiveConnection.getCheckoutTime();
            if (longestCheckoutTime > poolMaximumCheckoutTime) {
              // 如果最老的连接超时了，就在 PoolState 中记录一下相关信息，然后将该连接对象释放掉
              state.claimedOverdueConnectionCount++;
              state.accumulatedCheckoutTimeOfOverdueConnections += longestCheckoutTime;
              state.accumulatedCheckoutTime += longestCheckoutTime;
              state.activeConnections.remove(oldestActiveConnection);
              // 如果最老的连接不是 自动提交事务的，就将事务回滚掉
              if (!oldestActiveConnection.getRealConnection().getAutoCommit()) {
                try {
                  oldestActiveConnection.getRealConnection().rollback();
                } catch (SQLException e) {
                  /*
                     Just log a message for debug and continue to execute the following
                     statement like nothing happened.
                     Wrap the bad connection with a new PooledConnection, this will help
                     to not interrupt current executing thread and give current thread a
                     chance to join the next competition for another valid/good database
                     connection. At the end of this loop, bad {@link @conn} will be set as null.
                   */
                  log.debug("Bad connection. Could not roll back");
                }
              }
              // 从最老连接中取出真正的 数据库连接对象及相关信息，用来构建新的 PooledConnection对象
              conn = new PooledConnection(oldestActiveConnection.getRealConnection(), this);
              conn.setCreatedTimestamp(oldestActiveConnection.getCreatedTimestamp());
              conn.setLastUsedTimestamp(oldestActiveConnection.getLastUsedTimestamp());
              // 将最老活跃连接设为无效
              oldestActiveConnection.invalidate();
              if (log.isDebugEnabled()) {
                log.debug("Claimed overdue connection " + conn.getRealHashCode() + ".");
              }
            } else {
              // 如果最老的连接对象也没超时，则进入阻塞等待，
              // 等待时间 poolTimeToWait 可自行设置
              try {
                if (!countedWait) {
                  // 等待次数加一
                  state.hadToWaitCount++;
                  countedWait = true;
                }
                if (log.isDebugEnabled()) {
                  log.debug("Waiting as long as " + poolTimeToWait + " milliseconds for connection.");
                }
                long wt = System.currentTimeMillis();
                // native方法，使执行到这里的线程阻塞等待 poolTimeToWait毫秒
                state.wait(poolTimeToWait);
                // 统计累计等待的时间
                state.accumulatedWaitTime += System.currentTimeMillis() - wt;
              } catch (InterruptedException e) {
                break;
              }
            }
          }
        }
        // 到了这里 基本上就获取到连接对象咯，但我们还要确认一下该连接对象是否是有效的 可用的
        if (conn != null) {
          // ping一下数据库服务器，确认该连接对象是否有效
          if (conn.isValid()) {
            // 如果事务提交配置为手动的，则先让该连接回滚一下事务，防止脏数据的出现
            if (!conn.getRealConnection().getAutoCommit()) {
              conn.getRealConnection().rollback();
            }
            // 设置 由数据库URL、用户名、密码 计算出来的hash值，可用于标识该连接所在的连接池
            conn.setConnectionTypeCode(assembleConnectionTypeCode(dataSource.getUrl(), username, password));
            // 设置 从连接池中取出该连接时的时间戳
            conn.setCheckoutTimestamp(System.currentTimeMillis());
            // 设置 最后一次使用的时间戳
            conn.setLastUsedTimestamp(System.currentTimeMillis());
            // 将该连接加入活跃的连接对象列表
            state.activeConnections.add(conn);
            // 请求数据库连接的次数加一
            state.requestCount++;
            // 计算 获取连接的累计时间（accumulate累计）
            state.accumulatedRequestTime += System.currentTimeMillis() - t;
          // 如果获取到的连接无效
          } else {
            if (log.isDebugEnabled()) {
              log.debug("A bad connection (" + conn.getRealHashCode() + ") was returned from the pool, getting another connection.");
            }
            // 对无效连接进行统计
            state.badConnectionCount++;
            localBadConnectionCount++;
            conn = null;
            // 如果无效连接超出 阈值，则抛出异常
            if (localBadConnectionCount > (poolMaximumIdleConnections + poolMaximumLocalBadConnectionTolerance)) {
              if (log.isDebugEnabled()) {
                log.debug("PooledDataSource: Could not get a good connection to the database.");
              }
              throw new SQLException("PooledDataSource: Could not get a good connection to the database.");
            }
          }
        }
      }

    }

    // 如果到了这里 连接还为空，则抛出一个未知的服务异常
    if (conn == null) {
      if (log.isDebugEnabled()) {
        log.debug("PooledDataSource: Unknown severe error condition.  The connection pool returned a null connection.");
      }
      throw new SQLException("PooledDataSource: Unknown severe error condition.  The connection pool returned a null connection.");
    }

    // 返回数据库连接对象
    return conn;
  }

  /**
   * 看一下之前讲过的 PooledConnection 中的 动态代理方法invoke()，可以发现
   * 当调用数据库连接代理对象的 close()方法 时，并未关闭真正的数据库连接，
   * 而是调用了本方法，将连接对象归还给连接池，方便后续使用，本方法也是 PooledDataSource 的核心逻辑之一
   */
  protected void pushConnection(PooledConnection conn) throws SQLException {
    // 国际惯例，操作公共资源先上个锁
    synchronized (state) {
      // 先将该连接从活跃的连接对象列表中剔除
      state.activeConnections.remove(conn);
      // 如果该连接有效
      if (conn.isValid()) {
        // 如果连接池中的空闲连接数未达到阈值 且 该连接确实属于
        // 本连接池（通过之前获取的 expectedConnectionTypeCode 进行校验）
        if (state.idleConnections.size() < poolMaximumIdleConnections && conn.getConnectionTypeCode() == expectedConnectionTypeCode) {
          // CheckoutTime = 应用从连接池取出连接到归还连接的时长
          // accumulatedCheckoutTime = 所有连接累计的CheckoutTime
          state.accumulatedCheckoutTime += conn.getCheckoutTime();
          // 不是自动提交事务的连接 先回滚一波
          if (!conn.getRealConnection().getAutoCommit()) {
            conn.getRealConnection().rollback();
          }
          // 从 conn 中取出真正的 数据库连接对象，重新封装成 PooledConnection
          PooledConnection newConn = new PooledConnection(conn.getRealConnection(), this);
          // 将 newConn 放进空闲连接对象列表
          state.idleConnections.add(newConn);
          // 设置 newConn 的相关属性
          newConn.setCreatedTimestamp(conn.getCreatedTimestamp());
          newConn.setLastUsedTimestamp(conn.getLastUsedTimestamp());
          // 将原本的 conn 作废
          conn.invalidate();
          if (log.isDebugEnabled()) {
            log.debug("Returned connection " + newConn.getRealHashCode() + " to pool.");
          }
          // 唤醒阻塞等待的线程
          state.notifyAll();
        } else {
          // 如果空闲连接已达阈值 或 该连接对象不属于本连接池，则做好统计数据
          // 回滚连接的事务，关闭真正的连接，最后作废 该conn
          state.accumulatedCheckoutTime += conn.getCheckoutTime();
          if (!conn.getRealConnection().getAutoCommit()) {
            conn.getRealConnection().rollback();
          }
          conn.getRealConnection().close();
          if (log.isDebugEnabled()) {
            log.debug("Closed connection " + conn.getRealHashCode() + ".");
          }
          conn.invalidate();
        }
      // 如果该连接是无效的，则记录一下无效的连接数
      } else {
        if (log.isDebugEnabled()) {
          log.debug("A bad connection (" + conn.getRealHashCode() + ") attempted to return to the pool, discarding connection.");
        }
        state.badConnectionCount++;
      }
    }
  }

  /**
   * 关闭连接池中 所有活跃的 及 空闲的连接
   * 当修改连接池的配置（如：用户名、密码、URL等），都会调用本方法
   */
  public void forceCloseAll() {
    // 日常上锁
    synchronized (state) {
      // 更新当前连接池的标识
      expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
      // 依次关闭活跃的连接对象
      for (int i = state.activeConnections.size(); i > 0; i--) {
        try {
          PooledConnection conn = state.activeConnections.remove(i - 1);
          conn.invalidate();

          Connection realConn = conn.getRealConnection();
          if (!realConn.getAutoCommit()) {
            realConn.rollback();
          }
          realConn.close();
        } catch (Exception e) {
          // ignore
        }
      }
      // 依次关闭空闲的连接对象
      for (int i = state.idleConnections.size(); i > 0; i--) {
        try {
          PooledConnection conn = state.idleConnections.remove(i - 1);
          conn.invalidate();

          Connection realConn = conn.getRealConnection();
          if (!realConn.getAutoCommit()) {
            realConn.rollback();
          }
          realConn.close();
        } catch (Exception e) {
          // ignore
        }
      }
    }
    if (log.isDebugEnabled()) {
      log.debug("PooledDataSource forcefully closed/removed all connections.");
    }
  }
}
```

最后，我们来看一下 popConnection() 和 pushConnection() 都调用了的 isValid()方法，该方法除了检测 PooledConnection 中的 valid 字段 外 还还会调用 PooledDataSource 中的 pingConnection()方法，让数据库连接对象 执行指定的 sql 语句，检测连接是否正常。

```java
class PooledConnection implements InvocationHandler {
  /**
   * 检测 PooledConnection对象 的有效性
   */
  public boolean isValid() {
    return valid && realConnection != null && dataSource.pingConnection(this);
  }
}


public class PooledDataSource implements DataSource {
  /**
   * ping 一下数据库，检测数据库连接是否正常
   */
  protected boolean pingConnection(PooledConnection conn) {
    boolean result = true;

    try {
      result = !conn.getRealConnection().isClosed();
    } catch (SQLException e) {
      if (log.isDebugEnabled()) {
        log.debug("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
      }
      result = false;
    }

    if (result) {
      // 是否允许发送检测语句，检测数据库连接是否正常，poolPingEnabled 可自行配置
      // 该检测会牺牲一定的系统资源，以提高安全性
      if (poolPingEnabled) {
        // 超过 poolPingConnectionsNotUsedFor毫秒 未使用的连接 才会检测其连接状态
        if (poolPingConnectionsNotUsedFor >= 0 && conn.getTimeElapsedSinceLastUse() > poolPingConnectionsNotUsedFor) {
          try {
            if (log.isDebugEnabled()) {
              log.debug("Testing connection " + conn.getRealHashCode() + " ...");
            }
            // 获取真正的连接对象，执行 poolPingQuery = "NO PING QUERY SET" sql语句
            Connection realConn = conn.getRealConnection();
            try (Statement statement = realConn.createStatement()) {
              statement.executeQuery(poolPingQuery).close();
            }
            if (!realConn.getAutoCommit()) {
              realConn.rollback();
            }
            result = true;
            if (log.isDebugEnabled()) {
              log.debug("Connection " + conn.getRealHashCode() + " is GOOD!");
            }
            // 如果上面这段代码抛出异常，则说明数据库连接有问题，将该连接关闭，返回false
          } catch (Exception e) {
            log.warn("Execution of ping query '" + poolPingQuery + "' failed: " + e.getMessage());
            try {
              conn.getRealConnection().close();
            } catch (Exception e2) {
              //ignore
            }
            result = false;
            if (log.isDebugEnabled()) {
              log.debug("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
            }
          }
        }
      }
    }
    return result;
  }
}
```

## 2 Transaction

遵循 “接口-实现类” 的设计原则，Mybatis 也是先使用 Transaction 接口 对数据库事务做了抽象，而实现类则只提供了两个，即：JdbcTransaction 和 ManagedTransaction。这两种对象的获取，使用了两个对应的工厂类 JdbcTransactionFactory 和 ManagedTransactionFactory。
不过一般我们并不会使用 Mybatis 管理事务，而是将 Mybatis 集成到 Spring，由 Spring 进行事务的管理。细节部分会在后面的文章中详细讲解。

```java
public interface Transaction {

  /**
   * 获取连接对象
   */
  Connection getConnection() throws SQLException;

  /**
   * 提交事务
   */
  void commit() throws SQLException;

  /**
   * 回滚事务
   */
  void rollback() throws SQLException;

  /**
   * 关闭数据库连接
   */
  void close() throws SQLException;

  /**
   * 获取配置的事务超时时间
   */
  Integer getTimeout() throws SQLException;
}

public class JdbcTransaction implements Transaction {

  private static final Log log = LogFactory.getLog(JdbcTransaction.class);

  // 当前事务对应的数据库连接
  protected Connection connection;
  // 当前事务对应的数据源
  protected DataSource dataSource;
  // 事务隔离级别
  protected TransactionIsolationLevel level;
  // 是否自动提交
  protected boolean autoCommit;

  public JdbcTransaction(DataSource ds, TransactionIsolationLevel desiredLevel, boolean desiredAutoCommit) {
    dataSource = ds;
    level = desiredLevel;
    autoCommit = desiredAutoCommit;
  }

  public JdbcTransaction(Connection connection) {
    this.connection = connection;
  }

  @Override
  public Connection getConnection() throws SQLException {
    if (connection == null) {
      openConnection();
    }
    return connection;
  }

  // 提交、回滚、关闭等操作的代码都比较简单，只对原生的 JDBC操作 做了简单封装
  @Override
  public void commit() throws SQLException {
    if (connection != null && !connection.getAutoCommit()) {
      if (log.isDebugEnabled()) {
        log.debug("Committing JDBC Connection [" + connection + "]");
      }
      connection.commit();
    }
  }

  @Override
  public void rollback() throws SQLException {
    if (connection != null && !connection.getAutoCommit()) {
      if (log.isDebugEnabled()) {
        log.debug("Rolling back JDBC Connection [" + connection + "]");
      }
      connection.rollback();
    }
  }

  @Override
  public void close() throws SQLException {
    if (connection != null) {
      resetAutoCommit();
      if (log.isDebugEnabled()) {
        log.debug("Closing JDBC Connection [" + connection + "]");
      }
      connection.close();
    }
  }

  protected void setDesiredAutoCommit(boolean desiredAutoCommit) {
    try {
      if (connection.getAutoCommit() != desiredAutoCommit) {
        if (log.isDebugEnabled()) {
          log.debug("Setting autocommit to " + desiredAutoCommit + " on JDBC Connection [" + connection + "]");
        }
        connection.setAutoCommit(desiredAutoCommit);
      }
    } catch (SQLException e) {
      // Only a very poorly implemented driver would fail here,
      // and there's not much we can do about that.
      throw new TransactionException("Error configuring AutoCommit.  "
          + "Your driver may not support getAutoCommit() or setAutoCommit(). "
          + "Requested setting: " + desiredAutoCommit + ".  Cause: " + e, e);
    }
  }

  protected void resetAutoCommit() {
    try {
      if (!connection.getAutoCommit()) {
        // MyBatis does not call commit/rollback on a connection if just selects were performed.
        // Some databases start transactions with select statements
        // and they mandate a commit/rollback before closing the connection.
        // A workaround is setting the autocommit to true before closing the connection.
        // Sybase throws an exception here.
        if (log.isDebugEnabled()) {
          log.debug("Resetting autocommit to true on JDBC Connection [" + connection + "]");
        }
        connection.setAutoCommit(true);
      }
    } catch (SQLException e) {
      if (log.isDebugEnabled()) {
        log.debug("Error resetting autocommit to true "
            + "before closing the connection.  Cause: " + e);
      }
    }
  }

  protected void openConnection() throws SQLException {
    if (log.isDebugEnabled()) {
      log.debug("Opening JDBC Connection");
    }
    connection = dataSource.getConnection();
    if (level != null) {
      connection.setTransactionIsolation(level.getLevel());
    }
    setDesiredAutoCommit(autoCommit);
  }

  @Override
  public Integer getTimeout() throws SQLException {
    return null;
  }

}

public class ManagedTransaction implements Transaction {

  private static final Log log = LogFactory.getLog(ManagedTransaction.class);

  // 数据源
  private DataSource dataSource;
  // 事务隔离级别
  private TransactionIsolationLevel level;
  // 对应的数据库连接
  private Connection connection;
  // 控制是否关闭持有的连接，在 close()方法 中用其判断是否真的关闭连接
  private final boolean closeConnection;

  // 本类的实现也很简单，commit()、rollback()方法 都是空实现
  public ManagedTransaction(Connection connection, boolean closeConnection) {
    this.connection = connection;
    this.closeConnection = closeConnection;
  }

  public ManagedTransaction(DataSource ds, TransactionIsolationLevel level, boolean closeConnection) {
    this.dataSource = ds;
    this.level = level;
    this.closeConnection = closeConnection;
  }

  @Override
  public Connection getConnection() throws SQLException {
    if (this.connection == null) {
      openConnection();
    }
    return this.connection;
  }

  @Override
  public void commit() throws SQLException {
    // Does nothing
  }

  @Override
  public void rollback() throws SQLException {
    // Does nothing
  }

  @Override
  public void close() throws SQLException {
    if (this.closeConnection && this.connection != null) {
      if (log.isDebugEnabled()) {
        log.debug("Closing JDBC Connection [" + this.connection + "]");
      }
      this.connection.close();
    }
  }

  protected void openConnection() throws SQLException {
    if (log.isDebugEnabled()) {
      log.debug("Opening JDBC Connection");
    }
    this.connection = this.dataSource.getConnection();
    if (this.level != null) {
      this.connection.setTransactionIsolation(this.level.getLevel());
    }
  }

  @Override
  public Integer getTimeout() throws SQLException {
    return null;
  }

}

public interface TransactionFactory {

  /**
   * 配置 TransactionFactory对象，一般会在完成 TransactionFactory对象
   * 初始化之后 就进行自定义属性配置
   */
  default void setProperties(Properties props) {
    // NOP
  }

  /**
   * 在指定的数据库连接上创建 Transaction事务对象
   */
  Transaction newTransaction(Connection conn);

  /**
   * 从指定数据源获取数据库连接，并在此连接上创建 Transaction对象
   */
  Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit);
}

public class JdbcTransactionFactory implements TransactionFactory {

  @Override
  public Transaction newTransaction(Connection conn) {
    return new JdbcTransaction(conn);
  }

  @Override
  public Transaction newTransaction(DataSource ds, TransactionIsolationLevel level, boolean autoCommit) {
    return new JdbcTransaction(ds, level, autoCommit);
  }
}

public class ManagedTransactionFactory implements TransactionFactory {

  private boolean closeConnection = true;

  @Override
  public void setProperties(Properties props) {
    if (props != null) {
      String closeConnectionProperty = props.getProperty("closeConnection");
      if (closeConnectionProperty != null) {
        closeConnection = Boolean.valueOf(closeConnectionProperty);
      }
    }
  }

  @Override
  public Transaction newTransaction(Connection conn) {
    return new ManagedTransaction(conn, closeConnection);
  }

  @Override
  public Transaction newTransaction(DataSource ds, TransactionIsolationLevel level, boolean autoCommit) {
    // Silently ignores autocommit and isolation level, as managed transactions are entirely
    // controlled by an external manager.  It's silently ignored so that
    // code remains portable between managed and unmanaged configurations.
    return new ManagedTransaction(ds, level, closeConnection);
  }
}
```

binding 模块主要为了解决一个历史遗留问题，原先查询一个 VO 对象 时需要调用 SqlSession.queryForObject(“selectXXVOById”, primaryKey)方法，执行指定的 sql 语句，第一个参数 selectXXVOById 指定了执行的 sql 语句 id，如果我们不小心写错了参数，Mybatis 是无法在初始化时发现这个错误的，只会在实际调用 queryForObject(“selectXXVOById”, primaryKey)方法 时才会抛出异常，这对于工程师来说是非常难受的，就像泛型出来之前，很多类型转换不会在编译期发现错误一样。而 binding 模块 就像 Java 的泛型机制 一样，将程序的错误提前暴露出来，为开发人员省去不少排查问题的精力。

binding 模块 的解决方案是，定义一个 Mapper 接口，在接口中定义 sql 语句 对应的 方法名 Id 及 参数，这些方法在 Mybatis 的初始化过程中，会与该 Mapper 接口 对应的映射配置文件中的 sql 语句 相关联，如果存在无法关联的 sql 语句，Mybatis 就会抛出异常，帮助我们及时发现问题。示例代码如下：

```java
public interface HeroMapper {
    // 映射文件中会存在一个 <select>节点，id 为 “selectHeroVOById”
    public HeroVO selectHeroVOById(int id);
}

// 首先，获取 HeroMapper 对应的代理对象
HeroMapper heroMapper = session.getMapper(HeroMapper.class);
// 直接调用 HeroMapper接口 中的方法，获取结果集
HeroVO heroVO = heroMapper.selectHeroVOById("23333");
```

# 1 MapperRegistry 和 MapperProxyFactory

MapperRegistry 是 Mapper 接口 及其对应的代理对象工厂的注册中心。Configuration 是 Mybatis 中全局性的配置对象，根据 Mybatis 的核心配置文件 mybatis-config.xml 解析而成。Configuration 通过 mapperRegistry 属性 持有该对象。

Mybatis 在初始化过程中会读取映射配置文件和 Mapper 接口 中的注解信息，并调用 MapperRegistry 的 addMappers()方法 填充 knownMappers 集合，在需要执行某 sql 语句 时，会先调用 getMapper()方法 获取实现了 Mapper 接口 的动态代理对象。

```java
public class MapperRegistry {

  // Mybatis 全局唯一的配置对象，包含了几乎所有配置信息
  private final Configuration config;
  // key：Mapper接口，value：MapperProxyFactory 为 Mapper接口 创建代理对象的工厂
  private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<>();

  // 下面的两个重载方法 通过扫描指定的包目录，获取所有的 Mapper接口
  public void addMappers(String packageName) {
    addMappers(packageName, Object.class);
  }

  public void addMappers(String packageName, Class<?> superType) {
    ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<>();
    resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
    Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
    for (Class<?> mapperClass : mapperSet) {
      addMapper(mapperClass);
    }
  }

  public <T> void addMapper(Class<T> type) {
    // 该 type 是不是接口
    if (type.isInterface()) {
      // 是否已经加载过
      if (hasMapper(type)) {
        throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
      }
      boolean loadCompleted = false;
      try {
        // 将 Mapper接口 的 Class对象 和 对应的 MapperProxyFactory对象 添加到 knownMappers集合
        knownMappers.put(type, new MapperProxyFactory<>(type));
        // XML 解析和注解的处理
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

  @SuppressWarnings("unchecked")
  public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    // 获取 type 对应的 MapperProxyFactory对象
    final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
    if (mapperProxyFactory == null) {
      throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
    }
    try {
      // 根据 sqlSession 创建 type接口 的代理对象
      return mapperProxyFactory.newInstance(sqlSession);
    } catch (Exception e) {
      throw new BindingException("Error getting mapper instance. Cause: " + e, e);
    }
  }

  // 获取所有的 MapperProxyFactory
  public Collection<Class<?>> getMappers() {
    return Collections.unmodifiableCollection(knownMappers.keySet());
  }

  // 初始化的时候会持有 Configuration对象
  public MapperRegistry(Configuration config) {
    this.config = config;
  }

  // 是否存在指定的 MapperProxyFactory
  public <T> boolean hasMapper(Class<T> type) {
    return knownMappers.containsKey(type);
  }
}
```

MapperProxyFactory 主要负责创建代理对象。

```java
public class MapperProxyFactory<T> {

  // 要创建的动态代理对象 所实现的接口
  private final Class<T> mapperInterface;
  // 缓存 mapperInterface接口 中 Method对象 和其对应的 MapperMethod对象
  private final Map<Method, MapperMethod> methodCache = new ConcurrentHashMap<>();

  // 初始化时为 mapperInterface 注入值
  public MapperProxyFactory(Class<T> mapperInterface) {
    this.mapperInterface = mapperInterface;
  }

  public Class<T> getMapperInterface() {
    return mapperInterface;
  }

  public Map<Method, MapperMethod> getMethodCache() {
    return methodCache;
  }

  public T newInstance(SqlSession sqlSession) {
    // 每次都会创建一个新的 MapperProxy对象
    final MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface, methodCache);
    return newInstance(mapperProxy);
  }

  /**
   * 非常眼熟的 JDK动态代理 代码，创建了实现 mapperInterface接口 的代理对象
   * 根据国际惯例，mapperProxy对应的类 肯定实现了 InvocationHandler接口，
   * 为 mapperInterface接口方法的调用 织入统一处理逻辑
   */
  protected T newInstance(MapperProxy<T> mapperProxy) {
    return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
  }
}
```

## 2 MapperProxy

MapperProxy 实现了 InvocationHandler 接口，为 Mapper 接口 的方法调用织入了统一处理。

```java
public class MapperProxy<T> implements InvocationHandler, Serializable {

  private static final long serialVersionUID = -6424540398559729838L;
  // 记录关联的 sqlSession对象
  private final SqlSession sqlSession;
  // 对应的 Mapper接口 的 Class对象
  private final Class<T> mapperInterface;
  // 用于缓存 MapperMethod对象，key：Mapper接口 中方法对应的 Method对象，
  // value：MapperMethod对象（该对象会完成参数转换 及 sql语句 的执行功能）
  private final Map<Method, MapperMethod> methodCache;

  public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
    this.sqlSession = sqlSession;
    this.mapperInterface = mapperInterface;
    this.methodCache = methodCache;
  }

  // 为被代理对象的方法 织入统一处理
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      // 如果目标方法继承自 Object，则直接调用目标方法
      if (Object.class.equals(method.getDeclaringClass())) {
        return method.invoke(this, args);
      } else if (method.isDefault()) {
        return invokeDefaultMethod(proxy, method, args);
      }
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }
    // 从缓存中获取 mapperMethod对象，如果没有就创建新的
    final MapperMethod mapperMethod = cachedMapperMethod(method);
    // 执行 sql语句，返回结果集
    return mapperMethod.execute(sqlSession, args);
  }

  // 主要负责维护 methodCache 缓存
  private MapperMethod cachedMapperMethod(Method method) {
    // 这里用到了 Java8 的新特性，computeIfAbsent() 是 Java8 新增的方法，Lambda表达式 也是 Java8中 最重要的新特性之一
    // computeIfAbsent()方法 表示 当前map中，若 key 对应的 value 为空，则执行传入的 Lambda表达式，将 key 和表达式的 value
    // 存入 当前map，并返回 value值
    // 在这段代码中的意思是：若 methodCache 中没有 method 对应的 value，就执行右侧的 Lambda表达式，并将表达式的结果
    // 存入 methodCache 并返回
    return methodCache.computeIfAbsent(method, k -> new MapperMethod(mapperInterface, method, sqlSession.getConfiguration()));
  }
}
```

## 3 MapperMethod

MapperMethod 中封装了 Mapper 接口 中对应方法的信息，和对应 sql 语句 的信息，是连接 Mapper 接口 及映射配置文件中定义的 sql 语句 的桥梁。

MapperMethod 中持有两个非常重要的属性，这两个属性对应的类 都是 MapperMethod 中的静态内部类。另外，MapperMethod 在被实例化时就对这两个属性进行了初始化。

```java
public class MapperMethod {

  /** 下面这俩货都是内部类，而且还是 public static 的 */
  private final SqlCommand command;
  private final MethodSignature method;

  public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
    this.command = new SqlCommand(config, mapperInterface, method);
    this.method = new MethodSignature(config, mapperInterface, method);
  }
}
```

MapperMethod 中的核心方法 execute() 就主要用到了这两个类，所以我们先看一下 SqlCommand 和 MethodSignature 的源码。

### 3.1 SqlCommand

```java
  public static class SqlCommand {

    // sql语句的id
    private final String name;
    // sql语句的类型，SqlCommandType 是枚举类型，持有常用的 增、删、改、查等操作类型
    private final SqlCommandType type;

    public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
      // 方法名
      final String methodName = method.getName();
      // 该方法对应的类的 Class对象
      final Class<?> declaringClass = method.getDeclaringClass();
      // MappedStatement 封装了 sql语句 相关的信息，在 Mybatis初始化 时创建
      MappedStatement ms = resolveMappedStatement(mapperInterface, methodName, declaringClass, configuration);
      if (ms == null) {
        // 处理 Flush注解
        if (method.getAnnotation(Flush.class) != null) {
          name = null;
          type = SqlCommandType.FLUSH;
        } else {
          throw new BindingException("Invalid bound statement (not found): "
              + mapperInterface.getName() + "." + methodName);
        }
      } else {
        // 初始化 name 和 type
        name = ms.getId();
        type = ms.getSqlCommandType();
        if (type == SqlCommandType.UNKNOWN) {
          throw new BindingException("Unknown execution method for: " + name);
        }
      }
    }

    private MappedStatement resolveMappedStatement(Class<?> mapperInterface, String methodName,
                                                   Class<?> declaringClass, Configuration configuration) {
      // sql语句 的名称默认是由 Mapper接口方法 的 包名.类名.方法名
      String statementId = mapperInterface.getName() + "." + methodName;
      // 检测是否有该名称的 sql语句
      if (configuration.hasStatement(statementId)) {
        // 从 configuration 的 mappedStatements容器 中获取 statementId 对应的 MappedStatement对象
        return configuration.getMappedStatement(statementId);
        // 如果此方法不是 mapperInterface接口 定义的，则返回空
      } else if (mapperInterface.equals(declaringClass)) {
        return null;
      }
      // 对 mapperInterface 的父接口 进行递归处理
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

    public String getName() {
      return name;
    }

    public SqlCommandType getType() {
      return type;
    }
  }
```

### 3.2 MethodSignature

```java
  public static class MethodSignature {

    // 返回值类型是否为 集合 或 数组
    private final boolean returnsMany;
    // 返回值类型是否为 Map类型
    private final boolean returnsMap;
    // 返回值类型是否为 void
    private final boolean returnsVoid;
    // 返回值类型是否为 Cursor
    private final boolean returnsCursor;
    // 返回值类型是否为 Optional
    private final boolean returnsOptional;
    // 返回值类型的 Class对象
    private final Class<?> returnType;
    // 如果返回值类型为 Map，则用该字段记录了作为 key 的列名
    private final String mapKey;
    // 标记该方法参数列表中 ResultHandler类型参数 的位置
    private final Integer resultHandlerIndex;
    // 标记该方法参数列表中 RowBounds类型参数 的位置
    private final Integer rowBoundsIndex;

    /**
     * 顾名思义，这是一个处理 Mapper接口 中 方法参数列表的解析器，它使用了一个 SortedMap<Integer, String>
     * 类型的容器，记录了参数在参数列表中的位置索引 与 参数名之间的对应关系，key参数 在参数列表中的索引位置，
     * value参数名(参数名可用@Param注解指定，默认使用参数索引作为其名称)
     */
    private final ParamNameResolver paramNameResolver;

    /**
     * MethodSignature 的构造方法会解析对应的 method，并初始化上述字段
     */
    public MethodSignature(Configuration configuration, Class<?> mapperInterface, Method method) {
      // 获取 method方法 的返回值类型
      Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
      if (resolvedReturnType instanceof Class<?>) {
        this.returnType = (Class<?>) resolvedReturnType;
      } else if (resolvedReturnType instanceof ParameterizedType) {
        this.returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
      } else {
        this.returnType = method.getReturnType();
      }
      // 对 MethodSignature 持有的各属性 进行初始化
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
     * 查找指定类型的参数在参数列表中的位置，要查找的参数类型在参数列表中必须是唯一的
     * 如果参数列表中存在多个 要查找的参数类型，则会抛出异常
     */
    private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
      Integer index = null;
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
  }
```

### 3.3 execute()方法

execute()方法 会根据 sql 语句 的类型(CRUD)调用 SqlSession 对应的方法完成数据库操作，SqlSession 是 Mybatis 的核心组件之一，后面会详细解读。

```java
public class MapperMethod {
  public Object execute(SqlSession sqlSession, Object[] args) {
    Object result;
    // 根据 sql语句 的类型 调用 sqlSession 对应的方法
    switch (command.getType()) {
      case INSERT: {
        // 使用 ParamNameResolver 处理 args实参列表，将用户传入的实参与
        // 指定参数名称关联起来
        Object param = method.convertArgsToSqlCommandParam(args);
        // 获取返回结果，rowCountResult()方法 会根据 method属性 中的 returnType，
        // 对结果的类型进行转换
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
        // 处理返回值为 void 且 ResultSet 通过 ResultHandler 处理的方法
        if (method.returnsVoid() && method.hasResultHandler()) {
          executeWithResultHandler(sqlSession, args);
          result = null;
        // 处理返回值为集合 或 数组的方法
        } else if (method.returnsMany()) {
          result = executeForMany(sqlSession, args);
        // 处理返回值为 Map 的方法
        } else if (method.returnsMap()) {
          result = executeForMap(sqlSession, args);
        // 处理返回值为 Cursor 的方法
        } else if (method.returnsCursor()) {
          result = executeForCursor(sqlSession, args);
        } else {
        // 处理返回值为单一对象的方法
          Object param = method.convertArgsToSqlCommandParam(args);
          result = sqlSession.selectOne(command.getName(), param);
          // 处理返回值为 Optional 的方法
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

  /**
   * 当执行 insert、update、delete 类型的 sql语句 时，其执行结果都要经过本方法处理
   */
  private Object rowCountResult(int rowCount) {
    final Object result;
    // 方法的返回值为 void 时
    if (method.returnsVoid()) {
      result = null;
    // 方法的返回值为 Integer 时
    } else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {
      result = rowCount;
    // 方法的返回值为 Long 时
    } else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {
      result = (long)rowCount;
    // 方法的返回值为 Boolean 时
    } else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {
      result = rowCount > 0;
    } else {
      throw new BindingException("Mapper method '" + command.getName() + "' has an unsupported return type: " + method.getReturnType());
    }
    return result;
  }

  /**
   * 如果 Mapper接口 中定义的方法准备使用 ResultHandler 处理查询结果集，则通过此方法处理
   */
  private void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
    // 获取 sql语句 对应的 MappedStatement对象，该对象中记录了 sql语句 相关信息
    MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
    // 当使用 ResultHandler 处理结果集时，必须指定 ResultMap 或 ResultType
    if (!StatementType.CALLABLE.equals(ms.getStatementType())
        && void.class.equals(ms.getResultMaps().get(0).getType())) {
      throw new BindingException("method " + command.getName()
          + " needs either a @ResultMap annotation, a @ResultType annotation,"
          + " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
    }
    // 转换实参列表
    Object param = method.convertArgsToSqlCommandParam(args);
    // 如果实参列表中有 RowBounds类型参数
    if (method.hasRowBounds()) {
      // 从 args参数列表 中获取 RowBounds对象
      RowBounds rowBounds = method.extractRowBounds(args);
      // 执行查询，并用指定的 ResultHandler 处理结果对象
      sqlSession.select(command.getName(), param, rowBounds, method.extractResultHandler(args));
    } else {
      sqlSession.select(command.getName(), param, method.extractResultHandler(args));
    }
  }

  /**
   * 如果 Mapper接口 中对应方法的返回值为集合(Collection接口实现类) 或 数组，
   * 则调用本方法将结果集处理成 相应的集合或数组
   */
  private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
    List<E> result;
    // 参数列表转换
    Object param = method.convertArgsToSqlCommandParam(args);
    // 参数列表中是否有 RowBounds类型的参数
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      // 这里使用了 selectList()方法 进行查询，所以返回的结果集就是 List类型的
      result = sqlSession.selectList(command.getName(), param, rowBounds);
    } else {
      result = sqlSession.selectList(command.getName(), param);
    }
    // 将结果集转换为数组或 Collection集合
    if (!method.getReturnType().isAssignableFrom(result.getClass())) {
      if (method.getReturnType().isArray()) {
        return convertToArray(result);
      } else {
        return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
      }
    }
    return result;
  }

  /**
   * 将结果集转换成 Collection集合
   */
  private <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
    // 使用前面介绍的 ObjectFactory，通过反射方式创建集合对象
    Object collection = config.getObjectFactory().create(method.getReturnType());
    MetaObject metaObject = config.newMetaObject(collection);
    // 实际上就是调用了 Collection 的 addAll()方法
    metaObject.addAll(list);
    return collection;
  }

  /**
   * 本方法和上面的 convertToDeclaredCollection()功能 类似，主要负责将结果对象转换成数组
   */
  @SuppressWarnings("unchecked")
  private <E> Object convertToArray(List<E> list) {
    // 获取数组中元素的 类型Class
    Class<?> arrayComponentType = method.getReturnType().getComponentType();
    // 根据元素类型 和 元素数量 初始化数组
    Object array = Array.newInstance(arrayComponentType, list.size());
    // 将 List 转换成数组
    if (arrayComponentType.isPrimitive()) {
      for (int i = 0; i < list.size(); i++) {
        Array.set(array, i, list.get(i));
      }
      return array;
    } else {
      return list.toArray((E[])array);
    }
  }

  /**
   * 如果 Mapper接口 中对应方法的返回值为类型为 Map，则调用此方法执行 sql语句
   */
  private <K, V> Map<K, V> executeForMap(SqlSession sqlSession, Object[] args) {
    Map<K, V> result;
    // 转换实参列表
    Object param = method.convertArgsToSqlCommandParam(args);
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      // 注意这里调用的是 SqlSession 的 selectMap()方法，返回的是一个 Map类型结果集
      result = sqlSession.selectMap(command.getName(), param, method.getMapKey(), rowBounds);
    } else {
      result = sqlSession.selectMap(command.getName(), param, method.getMapKey());
    }
    return result;
  }

  /**
   * 本方法与上面的 executeForMap()方法 类似，只不过 sqlSession 调用的是 selectCursor()
   */
  private <T> Cursor<T> executeForCursor(SqlSession sqlSession, Object[] args) {
    Cursor<T> result;
    Object param = method.convertArgsToSqlCommandParam(args);
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      result = sqlSession.selectCursor(command.getName(), param, rowBounds);
    } else {
      result = sqlSession.selectCursor(command.getName(), param);
    }
    return result;
  }
}
```

MyBatis 中的缓存分为一级缓存、二级缓存，但在本质上是相同的，它们使用的都是 Cache 接口 的实现。MyBatis 缓存模块 的设计，使用了装饰器模式，这里不对此进行过多解析，以后会专门开一篇博文分析常用框架中使用到的设计模式。

# 1 Cache 组件

MyBatis 中缓存模块相关的代码位于 org.apache.ibatis.cache 包 下，其中 Cache 接口 是缓存模块中最核心的接口，它定义了所有缓存的基本行为。

```java
public interface Cache {

  /**
   * 获取当前缓存的 Id
   */
  String getId();

  /**
   * 存入缓存的 key 和 value，key 一般为 CacheKey对象
   */
  void putObject(Object key, Object value);

  /**
   * 根据 key 获取缓存值
   */
  Object getObject(Object key);

  /**
   * 删除指定的缓存项
   */
  Object removeObject(Object key);

  /**
   * 清空缓存
   */
  void clear();

  /**
   * 获取缓存的大小
   */
  int getSize();

  /**
   * ！！！！！！！！！！！！！！！！！！！！！！！！！！
   * 获取读写锁，可以看到，这个接口方法提供了默认的实现！！
   * 这是 Java8 的新特性！！只是平时开发时很少用到！！！
   * ！！！！！！！！！！！！！！！！！！！！！！！！！！
   */
  default ReadWriteLock getReadWriteLock() {
    return null;
  }
}
```

如下图所示，Cache 接口 的实现类有很多，但大部分都是装饰器，只有 PerpetualCache 提供了 Cache 接口 的基本实现。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/Cache%E7%BB%84%E4%BB%B6-771fe0.png)

### 1.1 PerpetualCache

PerpetualCache（Perpetual：永恒的，持续的）在缓存模块中扮演着被装饰的角色，其实现比较简单，底层使用 HashMap 记录缓存项，也是通过该 HashMap 对象 的方法实现的 Cache 接口 中定义的相应方法。

```java
public class PerpetualCache implements Cache {

  // Cache对象 的唯一标识
  private final String id;

  // 其所有的缓存功能实现，都是基于 JDK 的 HashMap 提供的方法
  private Map<Object, Object> cache = new HashMap<>();

  public PerpetualCache(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public int getSize() {
    return cache.size();
  }

  @Override
  public void putObject(Object key, Object value) {
    cache.put(key, value);
  }

  @Override
  public Object getObject(Object key) {
    return cache.get(key);
  }

  @Override
  public Object removeObject(Object key) {
    return cache.remove(key);
  }

  @Override
  public void clear() {
    cache.clear();
  }

  /**
   * 其重写了 Object 中的 equals() 和 hashCode()方法，两者都只关心 id字段
   */
  @Override
  public boolean equals(Object o) {
    if (getId() == null) {
      throw new CacheException("Cache instances require an ID.");
    }
    if (this == o) {
      return true;
    }
    if (!(o instanceof Cache)) {
      return false;
    }

    Cache otherCache = (Cache) o;
    return getId().equals(otherCache.getId());
  }

  @Override
  public int hashCode() {
    if (getId() == null) {
      throw new CacheException("Cache instances require an ID.");
    }
    return getId().hashCode();
  }
}
```

下面来看一下 cache.decorators 包 下提供的装饰器，它们都直接实现了 Cache 接口，扮演着装饰器的角色。这些装饰器会在 PerpetualCache 的基础上提供一些额外的功能，通过多个组合后满足一个特定的需求。

### 1.2 BlockingCache

BlockingCache 是阻塞版本的缓存装饰器，它会保证只有一个线程到数据库中查找指定 key 对应的数据。

```java
public class BlockingCache implements Cache {

  // 阻塞超时时长
  private long timeout;
  // 持有的被装饰者
  private final Cache delegate;
  // 每个 key 都有其对应的 ReentrantLock锁对象
  private final ConcurrentHashMap<Object, ReentrantLock> locks;

  // 初始化 持有的持有的被装饰者 和 锁集合
  public BlockingCache(Cache delegate) {
    this.delegate = delegate;
    this.locks = new ConcurrentHashMap<>();
  }
}
```

假设 线程 A 在 BlockingCache 中未查找到 keyA 对应的缓存项时，线程 A 会获取 keyA 对应的锁，这样，线程 A 在后续查找 keyA 时，其它线程会被阻塞。

```java
  // 根据 key 获取锁对象，然后上锁
  private void acquireLock(Object key) {
    // 获取 key 对应的锁对象
    Lock lock = getLockForKey(key);
    // 获取锁，带超时时长
    if (timeout > 0) {
      try {
        boolean acquired = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
        if (!acquired) { // 超时，则抛出异常
          throw new CacheException("Couldn't get a lock in " + timeout + " for the key " +  key + " at the cache " + delegate.getId());
        }
      } catch (InterruptedException e) {
        // 如果获取锁失败，则阻塞一段时间
        throw new CacheException("Got interrupted while trying to acquire lock for key " + key, e);
      }
    } else {
      // 上锁
      lock.lock();
    }
  }

  private ReentrantLock getLockForKey(Object key) {
    // Java8 新特性，Map系列类 中新增的方法
    // V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)
    // 表示，若 key 对应的 value 为空，则将第二个参数的返回值存入该 Map集合 并返回
    return locks.computeIfAbsent(key, k -> new ReentrantLock());
  }
```

假设 线程 A 从数据库中查找到 keyA 对应的结果对象后，将结果对象放入到 BlockingCache 中，此时 线程 A 会释放 keyA 对应的锁，唤醒阻塞在该锁上的线程。其它线程即可从 BlockingCache 中获取 keyA 对应的数据，而不是再次访问数据库。

```java
  @Override
  public void putObject(Object key, Object value) {
    try {
      // 存入 key 和其对应的缓存项
      delegate.putObject(key, value);
    } finally {
      // 最后释放锁
      releaseLock(key);
    }
  }

  private void releaseLock(Object key) {
    ReentrantLock lock = locks.get(key);
    // 锁是否被当前线程持有
    if (lock.isHeldByCurrentThread()) {
      // 是，则释放锁
      lock.unlock();
    }
  }
```

### 1.3 FifoCache 和 LruCache

在很多场景中，为了控制缓存的大小，系统需要按照一定的规则清理缓存。FifoCache 是先入先出版本的装饰器，当向缓存添加数据时，如果缓存项的个数已经达到上限，则会将缓存中最老（即最早进入缓存）的缓存项删除。

```java
public class FifoCache implements Cache {

  // 被装饰对象
  private final Cache delegate;
  // 用一个 FIFO 的队列记录 key 的顺序，其具体实现为 LinkedList
  private final Deque<Object> keyList;
  // 决定了缓存的容量上限
  private int size;

  // 国际惯例，通过构造方法初始化自己的属性，缓存容量上限默认为 1024个
  public FifoCache(Cache delegate) {
    this.delegate = delegate;
    this.keyList = new LinkedList<>();
    this.size = 1024;
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  public void setSize(int size) {
    this.size = size;
  }

  @Override
  public void putObject(Object key, Object value) {
    // 存储缓存项之前，先在 keyList 中注册
    cycleKeyList(key);
    // 存储缓存项
    delegate.putObject(key, value);
  }

  private void cycleKeyList(Object key) {
    // 在 keyList队列 中注册要添加的 key
    keyList.addLast(key);
    // 如果注册这个 key 会超出容积上限，则把最老的一个缓存项清除掉
    if (keyList.size() > size) {
      Object oldestKey = keyList.removeFirst();
      delegate.removeObject(oldestKey);
    }
  }

  @Override
  public Object getObject(Object key) {
    return delegate.getObject(key);
  }

  @Override
  public Object removeObject(Object key) {
    return delegate.removeObject(key);
  }

  // 除了清理缓存项，还要清理 key 的注册列表
  @Override
  public void clear() {
    delegate.clear();
    keyList.clear();
  }

}
```

LruCache 是按照"近期最少使用算法"（Least Recently Used, LRU）进行缓存清理的装饰器，在需要清理缓存时，它会清除最近最少使用的缓存项。

```java
public class LruCache implements Cache {

  // 被装饰者
  private final Cache delegate;
  // 这里使用的是 LinkedHashMap，它继承了 HashMap，但它的元素是有序的
  private Map<Object, Object> keyMap;
  // 最近最少被使用的缓存项的 key
  private Object eldestKey;

  // 国际惯例，构造方法中进行属性初始化
  public LruCache(Cache delegate) {
    this.delegate = delegate;
    // 这里初始化了 keyMap，并定义了 eldestKey 的取值规则
    setSize(1024);
  }

  public void setSize(final int size) {
    // 初始化 keyMap，同时指定该 Map 的初始容积及加载因子，第三个参数true 表示 该LinkedHashMap
    // 记录的顺序是 accessOrder，即，LinkedHashMap.get()方法 会改变其中元素的顺序
    keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {
      private static final long serialVersionUID = 4267176411845948333L;

      // 当调用 LinkedHashMap.put()方法 时，该方法会被调用
      @Override
      protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
        boolean tooBig = size() > size;
        if (tooBig) {
          // 当已达到缓存上限，更新 eldestKey字段，后面将其删除
          eldestKey = eldest.getKey();
        }
        return tooBig;
      }
    };
  }

  // 存储缓存项
  @Override
  public void putObject(Object key, Object value) {
    delegate.putObject(key, value);
    // 记录缓存项的 key，超出容量则清除最久未使用的缓存项
    cycleKeyList(key);
  }

  private void cycleKeyList(Object key) {
    keyMap.put(key, key);
    // eldestKey 不为空，则表示已经达到缓存上限
    if (eldestKey != null) {
      // 清除最久未使用的缓存
      delegate.removeObject(eldestKey);
      // 制空
      eldestKey = null;
    }
  }

  @Override
  public Object getObject(Object key) {
    // 访问 key元素 会改变该元素在 LinkedHashMap 中的顺序
    keyMap.get(key); //touch
    return delegate.getObject(key);
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  @Override
  public Object removeObject(Object key) {
    return delegate.removeObject(key);
  }

  @Override
  public void clear() {
    delegate.clear();
    keyMap.clear();
  }

}
```

### 1.4 SoftCache 和 WeakCache

在分析 SoftCache 和 WeakCache 实现之前，我们再温习一下 Java 提供的 4 种引用类型，强引用 StrongReference、软引用 SoftReference、弱引用 WeakReference 和虚引用 PhantomReference。

- 强引用
  平时用的最多的，如 Object obj ＝ new Object()，新建的 Object 对象 就是被强引用的。如果一个对象被强引用，即使是 JVM 内存空间不足，要抛出 OutOfMemoryError 异常，GC 也绝不会回收该对象。
- 软引用
  仅次于强引用的一种引用，它使用类 SoftReference 来表示。当 JVM 内存不足时，GC 会回收那些只被软引用指向的对象，从而避免内存溢出。软引用适合引用那些可以通过其他方式恢复的对象，例如， 数据库缓存中的对象就可以从数据库中恢复，所以软引用可以用来实现缓存，下面要介绍的 SoftCache 就是通过软引用实现的。  
  另外，由于在程序使用软引用之前的某个时刻，其所指向的对象可能己经被 GC 回收掉了，所以通过 Reference.get()方法 来获取软引用所指向的对象时，总是要通过检查该方法返回值是否为 null，来判断被软引用的对象是否还存活。
- 弱引用
  弱引用使用 WeakReference 表示，它不会阻止所引用的对象被 GC 回收。在 JVM 进行垃圾回收时，如果指向一个对象的所有引用都是弱引用，那么该对象会被回收。
  所以，只被弱引用所指向的对象，其生存周期是 两次 GC 之间 的这段时间，而只被软引用所指向的对象可以经历多次 GC，直到出现内存紧张的情况才被回收。
- 虚引用
  最弱的一种引用类型，由类 PhantomReference 表示。虚引用可以用来实现比较精细的内存使用控制，但很少使用。
- 引用队列（ReferenceQueue )
  很多场景下，我们的程序需要在一个对象被 GC 时得到通知，引用队列就是用于收集这些信息的队列。在创建 SoftReference 对象 时，可以为其关联一个引用队列，当 SoftReference 所引用的对象被 GC 时， JVM 就会将该 SoftReference 对象 添加到与之关联的引用队列中。当需要检测这些通知信息时，就可以从引用队列中获取这些 SoftReference 对象。不仅是 SoftReference，弱引用和虚引用都可以关联相应的队列。

现在来看一下 SoftCache 的具体实现。

```java
public class SoftCache implements Cache {

  // 这里使用了 LinkedList 作为容器，在 SoftCache 中，最近使用的一部分缓存项不会被 GC
  // 这是通过将其 value 添加到 hardLinksToAvoidGarbageCollection集合 实现的（即，有强引用指向其value）
  private final Deque<Object> hardLinksToAvoidGarbageCollection;
  // 引用队列，用于记录已经被 GC 的缓存项所对应的 SoftEntry对象
  private final ReferenceQueue<Object> queueOfGarbageCollectedEntries;
  // 持有的被装饰者
  private final Cache delegate;
  // 强连接的个数，默认为 256
  private int numberOfHardLinks;

  // 构造方法进行属性的初始化
  public SoftCache(Cache delegate) {
    this.delegate = delegate;
    this.numberOfHardLinks = 256;
    this.hardLinksToAvoidGarbageCollection = new LinkedList<>();
    this.queueOfGarbageCollectedEntries = new ReferenceQueue<>();
  }

  private static class SoftEntry extends SoftReference<Object> {
    private final Object key;

    SoftEntry(Object key, Object value, ReferenceQueue<Object> garbageCollectionQueue) {
      // 指向 value 的引用是软引用，并且关联了 引用队列
      super(value, garbageCollectionQueue);
      // 强引用
      this.key = key;
    }
  }

  @Override
  public void putObject(Object key, Object value) {
    // 清除已经被 GC 的缓存项
    removeGarbageCollectedItems();
    // 添加缓存
    delegate.putObject(key, new SoftEntry(key, value, queueOfGarbageCollectedEntries));
  }

  private void removeGarbageCollectedItems() {
    SoftEntry sv;
    // 遍历 queueOfGarbageCollectedEntries集合，清除已经被 GC 的缓存项 value
    while ((sv = (SoftEntry) queueOfGarbageCollectedEntries.poll()) != null) {
      delegate.removeObject(sv.key);
    }
  }

  @Override
  public Object getObject(Object key) {
    Object result = null;
    @SuppressWarnings("unchecked") // assumed delegate cache is totally managed by this cache
      // 用一个软引用指向 key 对应的缓存项
      SoftReference<Object> softReference = (SoftReference<Object>) delegate.getObject(key);
    // 检测缓存中是否有对应的缓存项
    if (softReference != null) {
      // 获取 softReference 引用的 value
      result = softReference.get();
      // 如果 softReference 引用的对象已经被 GC，则从缓存中清除对应的缓存项
      if (result == null) {
        delegate.removeObject(key);
      } else {
        synchronized (hardLinksToAvoidGarbageCollection) {
          // 将缓存项的 value 添加到 hardLinksToAvoidGarbageCollection集合 中保存
          hardLinksToAvoidGarbageCollection.addFirst(result);
          // 如果 hardLinksToAvoidGarbageCollection 的容积已经超过 numberOfHardLinks
          // 则将最老的缓存项从 hardLinksToAvoidGarbageCollection 中清除，FIFO
          if (hardLinksToAvoidGarbageCollection.size() > numberOfHardLinks) {
            hardLinksToAvoidGarbageCollection.removeLast();
          }
        }
      }
    }
    return result;
  }

  @Override
  public Object removeObject(Object key) {
    // 清除指定的缓存项之前，也会先清理被 GC 的缓存项
    removeGarbageCollectedItems();
    return delegate.removeObject(key);
  }


  @Override
  public void clear() {
    synchronized (hardLinksToAvoidGarbageCollection) {
      // 清理强引用集合
      hardLinksToAvoidGarbageCollection.clear();
    }
    // 清理被 GC 的缓存项
    removeGarbageCollectedItems();
    // 清理最底层的缓存项
    delegate.clear();
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    removeGarbageCollectedItems();
    return delegate.getSize();
  }

  public void setSize(int size) {
    this.numberOfHardLinks = size;
  }

}
```

WeakCache 的实现与 SoftCache 基本类似，唯一的区别在于其中使用 WeakEntry（继承了 WeakReference）封装真正的 value 对象，其他实现完全一样。

另外，还有 ScheduledCache、LoggingCache、SynchronizedCache、SerializedCache 等。ScheduledCache 是周期性清理缓存的装饰器，它的 clearInterval 字段 记录了两次缓存清理之间的时间间隔，默认是一小时，lastClear 字段 记录了最近一次清理的时间戳。 ScheduledCache 的 getObject()、putObject()、removeObject() 等核心方法，在执行时都会根据这两个字段检测是否需要进行清理操作，清理操作会清空缓存中所有缓存项。

LoggingCache 在 Cache 的基础上提供了日志功能，它通过 hit 字段 和 request 字段 记录了 Cache 的命中次数和访问次数。在 LoggingCache.getObject()方法 中，会统计命中次数和访问次数 这两个指标，井按照指定的日志输出方式输出命中率。

SynchronizedCache 通过在每个方法上添加 synchronized 关键字，为 Cache 添加了同步功能，有点类似于 JDK 中 Collections 的 SynchronizedCollection 内部类。

SerializedCache 提供了将 value 对象 序列化的功能。SerializedCache 在添加缓存项时，会将 value 对应的 Java 对象 进行序列化，井将序列化后的 byte[]数组 作为 value 存入缓存 。 SerializedCache 在获取缓存项时，会将缓存项中的 byte[]数组 反序列化成 Java 对象。不使用 SerializedCache 装饰器 进行装饰的话，每次从缓存中获取同一 key 对应的对象时，得到的都是同一对象，任意一个线程修改该对象都会影响到其他线程，以及缓存中的对象。而使用 SerializedCache 每次从缓存中获取数据时，都会通过反序列化得到一个全新的对象。 SerializedCache 使用的序列化方式是 Java 原生序列化。

## 2 CacheKey

在 Cache 中唯一确定一个缓存项，需要使用缓存项的 key 进行比较，MyBatis 中因为涉及 动态 SQL 等多方面因素， 其缓存项的 key 不能仅仅通过一个 String 表示，所以 MyBatis 提供了 CacheKey 类 来表示缓存项的 key，在一个 CacheKey 对象 中可以封装多个影响缓存项的因素。 CacheKey 中可以添加多个对象，由这些对象共同确定两个 CacheKey 对象 是否相同。

```java
public class CacheKey implements Cloneable, Serializable {

  private static final long serialVersionUID = 1146682552656046210L;

  public static final CacheKey NULL_CACHE_KEY = new NullCacheKey();

  private static final int DEFAULT_MULTIPLYER = 37;
  private static final int DEFAULT_HASHCODE = 17;

  // 参与计算hashcode，默认值DEFAULT_MULTIPLYER = 37
  private final int multiplier;
  // 当前CacheKey对象的hashcode，默认值DEFAULT_HASHCODE = 17
  private int hashcode;
  // 校验和
  private long checksum;
  private int count;

  // 由该集合中的所有元素 共同决定两个CacheKey对象是否相同，一般会使用一下四个元素
  // MappedStatement的id、查询结果集的范围参数（RowBounds的offset和limit）
  // SQL语句（其中可能包含占位符"?"）、SQL语句中占位符的实际参数
  private List<Object> updateList;

  // 构造方法初始化属性
  public CacheKey() {
    this.hashcode = DEFAULT_HASHCODE;
    this.multiplier = DEFAULT_MULTIPLYER;
    this.count = 0;
    this.updateList = new ArrayList<>();
  }

  public CacheKey(Object[] objects) {
    this();
    updateAll(objects);
  }

  public void update(Object object) {
    int baseHashCode = object == null ? 1 : ArrayUtil.hashCode(object);
    // 重新计算count、checksum和hashcode的值
    count++;
    checksum += baseHashCode;
    baseHashCode *= count;
    hashcode = multiplier * hashcode + baseHashCode;
    // 将object添加到updateList集合
    updateList.add(object);
  }

  public int getUpdateCount() {
    return updateList.size();
  }

  public void updateAll(Object[] objects) {
    for (Object o : objects) {
      update(o);
    }
  }

  /**
   * CacheKey重写了 equals() 和 hashCode()方法，这两个方法使用上面介绍
   * 的 count、checksum、hashcode、updateList 比较两个 CacheKey对象 是否相同
   */
  @Override
  public boolean equals(Object object) {
    // 如果为同一对象，直接返回 true
    if (this == object) {
      return true;
    }
    // 如果 object 都不是 CacheKey类型，直接返回 false
    if (!(object instanceof CacheKey)) {
      return false;
    }

    // 类型转换一下
    final CacheKey cacheKey = (CacheKey) object;

    // 依次比较 hashcode、checksum、count，如果不等，直接返回 false
    if (hashcode != cacheKey.hashcode) {
      return false;
    }
    if (checksum != cacheKey.checksum) {
      return false;
    }
    if (count != cacheKey.count) {
      return false;
    }

    // 比较 updateList 中的元素是否相同，不同直接返回 false
    for (int i = 0; i < updateList.size(); i++) {
      Object thisObject = updateList.get(i);
      Object thatObject = cacheKey.updateList.get(i);
      if (!ArrayUtil.equals(thisObject, thatObject)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return hashcode;
  }

  @Override
  public String toString() {
    StringJoiner returnValue = new StringJoiner(":");
    returnValue.add(String.valueOf(hashcode));
    returnValue.add(String.valueOf(checksum));
    updateList.stream().map(ArrayUtil::toString).forEach(returnValue::add);
    return returnValue.toString();
  }

  @Override
  public CacheKey clone() throws CloneNotSupportedException {
    CacheKey clonedCacheKey = (CacheKey) super.clone();
    clonedCacheKey.updateList = new ArrayList<>(updateList);
    return clonedCacheKey;
  }

}
```

## 3 小结

至此 Mybatis 的基础支持层的主要模块就分析完了。本模块首先介绍了 MyBatis 对 Java 反射机制的封装；然后分析了类型转换 TypeHandler 组件，了解了 MyBatis 如何实现数据在 Java 类型 与 JDBC 类型 之间的转换。

之后分析了 MyBatis 提供的 DataSource 模块 的实现和原理，深入解析了 MyBatis 自带的连接池 PooledDataSource 的详细实现；后面紧接着介绍了 Transaction 模块 的功能。然后分析了 binding 模块 如何将 Mapper 接口 与映射配置信息相关联，以及其中的原理。最后介绍了 MyBatis 的缓存模块，分析了 Cache 接口 以及多个实现类的具体实现，它们是 Mybatis 中一级缓存和二级缓存的基础。

# mybatis 缓存

- Author: [HuiFer](https://github.com/huifer)
- Description: 该文介绍 mybatis Cache 源码
- 源码阅读工程: [SourceHot-Mybatis](https://github.com/SourceHot/mybatis-read.git)

- `org.apache.ibatis.cache.Cache`

```java
public interface Cache {

    String getId();

    /**
     * 放入数据
     */
    void putObject(Object key, Object value);

    /**
     * 获取数据
     */
    Object getObject(Object key);

    /**
     * 移除数据
     */
    Object removeObject(Object key);

    /**
     * 清空数据
     */
    void clear();

    /**
     * 有多少缓存数据
     */
    int getSize();

    /**
     * 重入锁
     * @return A ReadWriteLock
     */
    default ReadWriteLock getReadWriteLock() {
        return null;
    }

}
```

- BlockingCache: 阻塞的缓存
- FifoCache: 按对象进入缓存的顺序来移除它们。
- LruCache: 最近最少使用的：移除最长时间不被使用的对象。
- SoftCache: 软引用：移除基于垃圾回收器状态和软引用规则的对象
- WeakCache: 弱引用：更积极地移除基于垃圾收集器状态和弱引用规则的对象。

## BlockingCache

- BlockingCache 内部使用了`ReentrantLock`来进行加锁开锁这个操作.在插入缓存时上锁,插入缓存后释放.请求缓存值得时候同理

```java
public class BlockingCache implements Cache {

    private final Cache delegate;
    /**
     * 线程安全的map
     */
    private final ConcurrentHashMap<Object, ReentrantLock> locks;
    private long timeout;

    public BlockingCache(Cache delegate) {
        this.delegate = delegate;
        this.locks = new ConcurrentHashMap<>();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public int getSize() {
        return delegate.getSize();
    }

    @Override
    public void putObject(Object key, Object value) {
        try {
            delegate.putObject(key, value);
        } finally {
            releaseLock(key);
        }
    }

    @Override
    public Object getObject(Object key) {
        acquireLock(key);
        Object value = delegate.getObject(key);
        if (value != null) {
            // 释放锁
            releaseLock(key);
        }
        return value;
    }

    @Override
    public Object removeObject(Object key) {
        // despite of its name, this method is called only to release locks
        releaseLock(key);
        return null;
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    private ReentrantLock getLockForKey(Object key) {
        return locks.computeIfAbsent(key, k -> new ReentrantLock());
    }

    /**
     * 请求锁
     * @param key
     */
    private void acquireLock(Object key) {
        Lock lock = getLockForKey(key);
        if (timeout > 0) {
            try {
                // 上锁
                boolean acquired = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
                if (!acquired) {
                    throw new CacheException("Couldn't get a lock in " + timeout + " for the key " + key + " at the cache " + delegate.getId());
                }
            } catch (InterruptedException e) {
                throw new CacheException("Got interrupted while trying to acquire lock for key " + key, e);
            }
        } else {
            lock.lock();
        }
    }

    /**
     * 释放锁
     * @param key
     */
    private void releaseLock(Object key) {
        ReentrantLock lock = locks.get(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
```

## FifoCache

- 存储结构是`java.util.LinkedList`

```java
public class FifoCache implements Cache {

    private final Cache delegate;
    /**
     * 队列
     */
    private final Deque<Object> keyList;
    private int size;

    public FifoCache(Cache delegate) {
        this.delegate = delegate;
        this.keyList = new LinkedList<>();
        this.size = 1024;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public int getSize() {
        return delegate.getSize();
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public void putObject(Object key, Object value) {
        cycleKeyList(key);
        delegate.putObject(key, value);
    }

    @Override
    public Object getObject(Object key) {
        return delegate.getObject(key);
    }

    @Override
    public Object removeObject(Object key) {
        return delegate.removeObject(key);
    }

    @Override
    public void clear() {
        delegate.clear();
        keyList.clear();
    }

    /**
     * 添加 key 删除最开始的一个
     *
     * @param key
     */
    private void cycleKeyList(Object key) {
        keyList.addLast(key);
        if (keyList.size() > size) {
            Object oldestKey = keyList.removeFirst();
            delegate.removeObject(oldestKey);
        }
    }

}
```

## LruCache

- 存储结构是`java.util.LinkedHashMap`

```java
/**
 * Lru (least recently used) cache decorator.
 * LRU  緩存策略 最近最少使用的：移除最长时间不被使用的对象。
 *
 * @author Clinton Begin
 */
public class LruCache implements Cache {

    private final Cache delegate;
    /**
     * {@link  LinkedHashMap}
     */
    private Map<Object, Object> keyMap;
    private Object eldestKey;

    public LruCache(Cache delegate) {
        this.delegate = delegate;
        setSize(1024);
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public int getSize() {
        return delegate.getSize();
    }

    /**
     * 设置大小
     *
     * @param size
     */
    public void setSize(final int size) {
        keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {
            private static final long serialVersionUID = 4267176411845948333L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
                // 数量超出预设值 执行
                boolean tooBig = size() > size;
                if (tooBig) {
//                    获取被移除的key
                    eldestKey = eldest.getKey();
                }
                return tooBig;
            }
        };
    }

    @Override
    public void putObject(Object key, Object value) {
        delegate.putObject(key, value);
        cycleKeyList(key);
    }

    @Override
    public Object getObject(Object key) {
        keyMap.get(key); //touch
        return delegate.getObject(key);
    }

    @Override
    public Object removeObject(Object key) {
        return delegate.removeObject(key);
    }

    @Override
    public void clear() {
        delegate.clear();
        keyMap.clear();
    }

    /**
     * 删除最早的一个key
     * @param key
     */
    private void cycleKeyList(Object key) {
        keyMap.put(key, key);
        if (eldestKey != null) {
            delegate.removeObject(eldestKey);
            eldestKey = null;
        }
    }

}
```

# mybatis 日志源码

- Author: [HuiFer](https://github.com/huifer)
- Description: 该文介绍 mybatis 日志相关源码
- 源码阅读工程: [SourceHot-Mybatis](https://github.com/SourceHot/mybatis-read.git)

## 核心类

- `org.apache.ibatis.logging.Log`
- `org.apache.ibatis.logging.LogFactory`
- 多个日志实现
  - `org.apache.ibatis.logging.log4j2.Log4j2Impl`
  - `org.apache.ibatis.logging.slf4j.Slf4jLocationAwareLoggerImpl`
  - ...

## 源码流程

- mybatis 提供了一个日志接口,内容如下.

```java
/**
 * mybatis 的日志接口,提供日志级别
 * <ol>
 *     <li>error</li>
 *     <li>debug</li>
 *     <li>trace</li>
 *     <li>warn</li>
 * </ol>
 * <p>通过自己定义的接口来实现各大日志框架的内容达到高可用</p>
 * @author Clinton Begin
 */
public interface Log {

    boolean isDebugEnabled();

    boolean isTraceEnabled();

    void error(String s, Throwable e);

    void error(String s);

    void debug(String s);

    void trace(String s);

    void warn(String s);
}
```

- 有了日志接口必然有实现类, mybatis 有`log4j2` 、 `slf4j` 等日志的相关实现 , 下面是`Slf4jImpl`的代码,其他代码也是一样的模式进行初始化就不再重复贴代码了.

```java
public class Slf4jImpl implements Log {

    private Log log;

    /**
     * 创建日志实例
     * @param clazz
     */
    public Slf4jImpl(String clazz) {
        Logger logger = LoggerFactory.getLogger(clazz);

        if (logger instanceof LocationAwareLogger) {
            try {
                // check for slf4j >= 1.6 method signature
                logger.getClass().getMethod("log", Marker.class, String.class, int.class, String.class, Object[].class, Throwable.class);
                log = new Slf4jLocationAwareLoggerImpl((LocationAwareLogger) logger);
                return;
            } catch (SecurityException | NoSuchMethodException e) {
                // fail-back to Slf4jLoggerImpl
            }
        }

        // Logger is not LocationAwareLogger or slf4j version < 1.6
        log = new Slf4jLoggerImpl(logger);
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    @Override
    public void error(String s, Throwable e) {
        log.error(s, e);
    }

    @Override
    public void error(String s) {
        log.error(s);
    }

    @Override
    public void debug(String s) {
        log.debug(s);
    }

    @Override
    public void trace(String s) {
        log.trace(s);
    }

    @Override
    public void warn(String s) {
        log.warn(s);
    }

}

```

- 通过上述方法来达到统一接口多个实现,这个在开发中也经常使用.多日志的实现方法有了还缺一个创建方法,创建方法由`org.apache.ibatis.logging.LogFactory`提供

```java
/**
 * <p>日志工厂，实现内容:</p>
 * <ol>
 *     <li>org.slf4j.Logger 日志框架 slf4j</li>
 *     <li>org.apache.commons.logging.Log 日志框架 apache</li>
 *     <li>org.apache.logging.log4j.Logger 日志框架 log4j2</li>
 *     <li>org.apache.log4j.Logger 日志框架 log4j </li>
 *     <li>java.util.logging.Logger 日志框架,JDK的logger</li>
 *
 * </ol>
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public final class LogFactory {

    /**
     * Marker to be used by logging implementations that support markers.
     */
    public static final String MARKER = "MYBATIS";

    private static Constructor<? extends Log> logConstructor;

    /**
     * 日志的实现类的具体选择
     */
    static {
        // slf4j 日志
        tryImplementation(LogFactory::useSlf4jLogging);
        // apache 日志
        tryImplementation(LogFactory::useCommonsLogging);
        // log4j2 日志
        tryImplementation(LogFactory::useLog4J2Logging);
        // log4 日志
        tryImplementation(LogFactory::useLog4JLogging);
        // JDK 日志
        tryImplementation(LogFactory::useJdkLogging);
        // 空 日志
        tryImplementation(LogFactory::useNoLogging);
    }

    /**
     * 私有化构造方法,这是一个单例
     */
    private LogFactory() {
        // disable construction
    }

    public static Log getLog(Class<?> aClass) {
        return getLog(aClass.getName());
    }

    public static Log getLog(String logger) {
        try {
            return logConstructor.newInstance(logger);
        } catch (Throwable t) {
            throw new LogException("Error creating logger for logger " + logger + ".  Cause: " + t, t);
        }
    }

    public static synchronized void useCustomLogging(Class<? extends Log> clazz) {
        setImplementation(clazz);
    }

    public static synchronized void useSlf4jLogging() {
        setImplementation(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);
    }

    public static synchronized void useCommonsLogging() {
        setImplementation(org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl.class);
    }

    public static synchronized void useLog4JLogging() {
        setImplementation(org.apache.ibatis.logging.log4j.Log4jImpl.class);
    }

    public static synchronized void useLog4J2Logging() {
        setImplementation(org.apache.ibatis.logging.log4j2.Log4j2Impl.class);
    }

    public static synchronized void useJdkLogging() {
        setImplementation(org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl.class);
    }

    public static synchronized void useStdOutLogging() {
        setImplementation(org.apache.ibatis.logging.stdout.StdOutImpl.class);
    }

    public static synchronized void useNoLogging() {
        setImplementation(org.apache.ibatis.logging.nologging.NoLoggingImpl.class);
    }

    /**
     * 选择具体的日志实现
     */
    private static void tryImplementation(Runnable runnable) {
        if (logConstructor == null) {
            try {
                // run()? 似乎违背了代码的语义, 看静态方法.静态方法多行同类型的操作我认为是一个多线程
                runnable.run();
            } catch (Throwable t) {
                // ignore
            }
        }
    }

    /**
     * 选择具体的日志实现
     */
    private static void setImplementation(Class<? extends Log> implClass) {
        try {
            Constructor<? extends Log> candidate = implClass.getConstructor(String.class);
            Log log = candidate.newInstance(LogFactory.class.getName());
            if (log.isDebugEnabled()) {
                log.debug("Logging initialized using '" + implClass + "' adapter.");
            }
            logConstructor = candidate;
        } catch (Throwable t) {
            throw new LogException("Error setting Log implementation.  Cause: " + t, t);
        }
    }

}

```

- `LogFactory`是一个单例对象,对外公开`getLog`方法在使用时直接`private static final Log log = LogFactory.getLog(CglibProxyFactory.class);`即可

- 在 `org.apache.ibatis.session.Configuration` 中可以看到下面这些注册方法

```java
        // 日志实现类
        typeAliasRegistry.registerAlias("SLF4J", Slf4jImpl.class);
        typeAliasRegistry.registerAlias("COMMONS_LOGGING", JakartaCommonsLoggingImpl.class);
        typeAliasRegistry.registerAlias("LOG4J", Log4jImpl.class);
        typeAliasRegistry.registerAlias("LOG4J2", Log4j2Impl.class);
        typeAliasRegistry.registerAlias("JDK_LOGGING", Jdk14LoggingImpl.class);
        typeAliasRegistry.registerAlias("STDOUT_LOGGING", StdOutImpl.class);
        typeAliasRegistry.registerAlias("NO_LOGGING", NoLoggingImpl.class);

```

# mybatis 反射

- Author: [HuiFer](https://github.com/huifer)
- Description: 该文介绍 mybatis 反射相关类的源码
- 源码阅读工程: [SourceHot-Mybatis](https://github.com/SourceHot/mybatis-read.git)

## addDefaultConstructor

- mybatis 的反射相关内容在`org.apache.ibatis.reflection` 下存放. 本片主要讲解`org.apache.ibatis.reflection.Reflector`类, 先看一下该类的属性

```java
public class Reflector {

     /**
        * 实体类.class
        */
       private final Class<?> type;
       /**
        * 可读 属性
        */
       private final String[] readablePropertyNames;
       /**
        * 可写 属性值
        */
       private final String[] writablePropertyNames;
       /**
        * set 方法列表
        */
       private final Map<String, Invoker> setMethods = new HashMap<>();
       /**
        * get 方法列表
        */
       private final Map<String, Invoker> getMethods = new HashMap<>();
       /**
        * set 的数据类型
        */
       private final Map<String, Class<?>> setTypes = new HashMap<>();
       /**
        * get 的数据类型
        */
       private final Map<String, Class<?>> getTypes = new HashMap<>();
       /**
        * 构造函数
        */
       private Constructor<?> defaultConstructor;

       /**
        * 缓存数据, 大写KEY
        */
       private Map<String, String> caseInsensitivePropertyMap = new HashMap<>();

}
```

- 构造方法, 构造方法传入一个类的字节码,在构造方法中设置相关的属性值

```java
public class Reflector {

/**
 * @param clazz 待解析类的字节码
 */
public Reflector(Class<?> clazz) {
    type = clazz;
    // 构造方法
    addDefaultConstructor(clazz);
    // get 方法
    addGetMethods(clazz);
    // set 方法
    addSetMethods(clazz);
    // 字段值
    addFields(clazz);
    readablePropertyNames = getMethods.keySet().toArray(new String[0]);
    writablePropertyNames = setMethods.keySet().toArray(new String[0]);
    for (String propName : readablePropertyNames) {
        // 循环操作设置到缓存中,
        caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
    }
    for (String propName : writablePropertyNames) {
        caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
    }
}
}
```

- `addDefaultConstructor` 方法 , 下面截图内容为 JDK8 mybatis 中 的内容

```java
    private void addDefaultConstructor(Class<?> clazz) {

        // 获取类里面的所有构造方法
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        // 过滤得到空参构造 constructor -> constructor.getParameterTypes().length == 0
        Arrays.stream(constructors).filter(constructor -> constructor.getParameterTypes().length == 0)
                .findAny().ifPresent(constructor -> {
            System.out.println("有空参构造");
            this.defaultConstructor = constructor;
        });
    }
```

- 创建一个测试类

```java
public class People {
    private String name;

    public People() {
    }

    public People(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "People{" +
                "name='" + name + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

```

```java
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

class HfReflectorTest {
    @Test
    void getDefaultConstructorTest() throws Exception {
        Reflector reflector = new Reflector(People.class);
        // 获取空参构造方法
        Constructor<?> defaultConstructor = reflector.getDefaultConstructor();
        People o = (People) defaultConstructor.newInstance();
        o.setName("hhh");

        System.out.println(o);
    }
}
```

- 准备工作完成了开始进行 debug , 在`org.apache.ibatis.reflection.Reflector#addDefaultConstructor`这个方法上打上断点

  ![1575890354400](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/1575890354400-90c78d.png)

  观察`constructors`属性存在两个方法,这两个方法就是我在`People`类中的构造方法.

  根据语法内容我们应该对`parameterTypes`属性进行查看

  ![1575890475839](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/1575890475839-30ba52.png)

可以发现空参构造的`parameterTypes`长度是 0.因此可以确认`org.apache.ibatis.reflection.Reflector#addDefaultConstructor`方法获取了空参构造

- 继续看`org.apache.ibatis.reflection.Reflector#getDefaultConstructor`方法, 该方法是获取构造函数的方法,如果构造函数没有就抛出异常,这也是为什么我们的实体类需要把空参构造写上去的原因。

  ```java
      public Constructor<?> getDefaultConstructor() {
          if (defaultConstructor != null) {
              return defaultConstructor;
          } else {
              // 如果没有空参构造抛出的异常
              throw new ReflectionException("There is no default constructor for " + type);
          }
      }
  ```

## addGetMethods

- 该方法获取了所有`get`和`is`开头的方法

  ```java
      private void addGetMethods(Class<?> clazz) {
          // 反射方法
          Map<String, List<Method>> conflictingGetters = new HashMap<>();
          Method[] methods = getClassMethods(clazz);
          // JDK8 filter 过滤get 开头的方法
          Arrays.stream(methods).filter(m -> m.getParameterTypes().length == 0 && PropertyNamer.isGetter(m.getName()))
                  .forEach(m -> addMethodConflict(conflictingGetters, PropertyNamer.methodToProperty(m.getName()), m));
          resolveGetterConflicts(conflictingGetters);
      }
  ```

- 该方法中依旧使用了 JDK8 语法通过`m.getParameterTypes().length == 0 && PropertyNamer.isGetter(m.getName())`来判断是否是`get`或·`is`开头的内容

- 调用`org.apache.ibatis.reflection.property.PropertyNamer`

  ```java
      public static boolean isGetter(String name) {
          // 在语义上 is 开头的也是get开头的
          return (name.startsWith("get") && name.length() > 3) || (name.startsWith("is") && name.length() > 2);
      }
  ```

- `resolveGetterConflicts`方法后续介绍

## getClassMethods

- `org.apache.ibatis.reflection.Reflector#getClassMethods`,该方法将传入对象的所有可见方法都获取到进行唯一标识处理成一个`Map`对象 添加方法为`org.apache.ibatis.reflection.Reflector#addUniqueMethods`

  ```java
      private Method[] getClassMethods(Class<?> clazz) {
          // 方法唯一标识: 方法
          Map<String, Method> uniqueMethods = new HashMap<>();
          Class<?> currentClass = clazz;
          while (currentClass != null && currentClass != Object.class) {
              // getDeclaredMethods 获取 public ,private , protcted 方法
              addUniqueMethods(uniqueMethods, currentClass.getDeclaredMethods());
  
              // we also need to look for interface methods -
              // because the class may be abstract
              // 当前类是否继承别的类(实现接口)如果继承则需要进行操作
              Class<?>[] interfaces = currentClass.getInterfaces();
              for (Class<?> anInterface : interfaces) {
                  // getMethods 获取本身和父类的 public 方法
                  addUniqueMethods(uniqueMethods, anInterface.getMethods());
              }
  
              // 循环往上一层一层寻找最后回到 Object 类 的上级为null 结束
              currentClass = currentClass.getSuperclass();
          }
  
          Collection<Method> methods = uniqueMethods.values();
  
          return methods.toArray(new Method[0]);
      }
  
  ```

- `org.apache.ibatis.reflection.Reflector#addUniqueMethods`

  ```java
      private void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
          for (Method currentMethod : methods) {
              // 桥接, 具体还不知道
              // TODO: 2019/12/9 JAVA 桥接方法
              if (!currentMethod.isBridge()) {
                  // 方法的唯一标识
                  String signature = getSignature(currentMethod);
                  // check to see if the method is already known
                  // if it is known, then an extended class must have
                  // overridden a method
                  if (!uniqueMethods.containsKey(signature)) {
                      uniqueMethods.put(signature, currentMethod);
                  }
              }
          }
      }
  ```

- 唯一标识方法`org.apache.ibatis.reflection.Reflector#getSignature`

  ```java
      /**
       * 方法唯一标识,返回值类型#方法名称：参数列表
       *
       * @param method
       * @return
       */
      private String getSignature(Method method) {
          StringBuilder sb = new StringBuilder();
          Class<?> returnType = method.getReturnType();
          if (returnType != null) {
              sb.append(returnType.getName()).append('#');
          }
          sb.append(method.getName());
          Class<?>[] parameters = method.getParameterTypes();
          for (int i = 0; i < parameters.length; i++) {
              sb.append(i == 0 ? ':' : ',').append(parameters[i].getName());
          }
          return sb.toString();
      }
  ```

- 照旧我们进行 debug 当前方法为`toString`方法

  ![1575891988804](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/1575891988804-b01ab1.png)

  从返回结果可以看到`sb.toString`返回的是： `返回值类型#方法名`

  ![1575892046692](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/1575892046692-5cbdd4.png)

  上图返回结果为`void#setName:java.lang.String` 命名规则：`返回值类型#方法名称:参数列表`

  回过头看看`uniqueMethods`里面是什么

  ![1575892167982](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/1575892167982-35eaba.png)

  方法签名:方法

  目前完成了一部分还有一个继承问题需要 debug 看一下, 编写一个`Man`继承`People` 还需要实现接口

  ```java
  public class Man extends People implements TestManInterface {
      @Override
      public Integer inte() {
          return 1;
      }
  
      public String hello() {
          return "hello";
      }
  }
  
  ```

  ```java
  public interface TestManInterface {
      public Integer inte();
  }
  ```

  目标明确了就直接在

  ![1575892414120](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/1575892414120-8acd36.png)

  这里打断点了

  ![1575892511471](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/1575892511471-f0a11b.png)

  在进入循环之前回率先加载本类的所有可见方法

  ```java
       if (!uniqueMethods.containsKey(signature)) {
           // 如果存在该方法唯一签名则不添加
                      uniqueMethods.put(signature, currentMethod);
                  }
  ```

  接下来断点继续往下走

  ![1575892645405](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/1575892645405-48fe7a.png)

  走到这一步我们来看看`currentClass.getSuperclass()`是不是上一级的类

  ![1575892687076](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/1575892687076-ed993e.png)

  通过断点可见这个`currentClass`现在是`People`类,根据之前所说的最终`uniqueMethods`应该存在父类的方法

  ![1575892763661](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/1575892763661-ed922b.png)

  可以看到父类的方法也都存在了

## resolveGetterConflicts

- `org.apache.ibatis.reflection.Reflector#resolveGetterConflicts`

  这个方法解决了`get`方法的冲突问题,同名方法不同返回值

  ```java
  private void resolveGetterConflicts(Map<String, List<Method>> conflictingGetters) {
          for (Entry<String, List<Method>> entry : conflictingGetters.entrySet()) {
              Method winner = null;
              String propName = entry.getKey();
              boolean isAmbiguous = false;
              for (Method candidate : entry.getValue()) {
                  if (winner == null) {
                      winner = candidate;
                      continue;
                  }
                  Class<?> winnerType = winner.getReturnType();
                  Class<?> candidateType = candidate.getReturnType();
                  if (candidateType.equals(winnerType)) {
                      if (!boolean.class.equals(candidateType)) {
                          isAmbiguous = true;
                          break;
                      } else if (candidate.getName().startsWith("is")) {
                          winner = candidate;
                      }
                  } else if (candidateType.isAssignableFrom(winnerType)) {
                      // OK getter type is descendant
                  } else if (winnerType.isAssignableFrom(candidateType)) {
                      winner = candidate;
                  } else {
                      isAmbiguous = true;
                      break;
                  }
              }
              addGetMethod(propName, winner, isAmbiguous);
          }
      }
  ```

## addFields

- `org.apache.ibatis.reflection.Reflector#addFields`

  获取类的所有字段没什么好说的直接递归就可以获取了.

  ```jade
      private void addFields(Class<?> clazz) {
          Field[] fields = clazz.getDeclaredFields();
          for (Field field : fields) {
              if (!setMethods.containsKey(field.getName())) {
                  // issue #379 - removed the check for final because JDK 1.5 allows
                  // modification of final fields through reflection (JSR-133). (JGB)
                  // pr #16 - final static can only be set by the classloader
                  int modifiers = field.getModifiers();
                  if (!(Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers))) {
                      addSetField(field);
                  }
              }
              if (!getMethods.containsKey(field.getName())) {
                  addGetField(field);
              }
          }
          if (clazz.getSuperclass() != null) {
              addFields(clazz.getSuperclass());
          }
      }
  ```

## 属性查看

- 下图为一个类的解析结果

![1575894218362](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/1575894218362-9a2991.png)