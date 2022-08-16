# Spring Boot application 文件加载

- Author: [HuiFer](https://github.com/huifer)
- 源码阅读仓库: [SourceHot-spring-boot](https://github.com/SourceHot/spring-boot-read)

## 如何找到这个加载的过程

1. 创建配置文件`application.yml`

2. 全局搜索 yml

   ![image-20200319083048849](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200319083048849-6b1b47.png)

3. 换成`properties`搜索

   ![image-20200319083140225](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200319083140225-a9ebfa.png)

4. 我们以`yml`为例打上断点开始源码追踪

看到调用堆栈

![image-20200319083345067](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200319083345067-9a718e.png)

- 一步一步回上去看如何调用具体方法的

## ConfigFileApplicationListener

- 配置文件监听器

### 调用过程

![image-20200319082131146](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200319082131146-0470f3.png)

![image-20200319082544653](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200319082544653-598d87.png)

`org.springframework.boot.context.config.ConfigFileApplicationListener#addPropertySources`

```java
protected void addPropertySources(ConfigurableEnvironment environment, ResourceLoader resourceLoader) {
		RandomValuePropertySource.addToEnvironment(environment);
		// 加载器加载信息
		new Loader(environment, resourceLoader).load();
	}
```

### Loader

- 配置资源加载器

构造方法

```java
		Loader(ConfigurableEnvironment environment, ResourceLoader resourceLoader) {
		    // 环境配置
			this.environment = environment;
			// 占位符处理器
			this.placeholdersResolver = new PropertySourcesPlaceholdersResolver(this.environment);
			// 资源加载器
			this.resourceLoader = (resourceLoader != null) ? resourceLoader : new DefaultResourceLoader();
			// 配置信息加载器初始化
			this.propertySourceLoaders = SpringFactoriesLoader.loadFactories(PropertySourceLoader.class,
					getClass().getClassLoader());
		}

```

- 熟悉的老朋友`this.propertySourceLoaders = SpringFactoriesLoader.loadFactories(PropertySourceLoader.class, getClass().getClassLoader())`， 看看**`spring.factories`**有什么

  - 搜索目标: `org.springframework.boot.env.PropertySourceLoader`

    ![image-20200319084141748](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200319084141748-50ead4.png)

![image-20200319084151997](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200319084151997-03910f.png)

观察发现里面有一个`YamlPropertySourceLoader`和我们之前找 yml 字符串的时候找到的类是一样的。说明搜索方式没有什么问题。

![image-20200319084357652](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200319084357652-8f5482.png)

初始化完成，后续进行解析了

### load 方法

```java
		void load() {
			FilteredPropertySource.apply(this.environment, DEFAULT_PROPERTIES, LOAD_FILTERED_PROPERTY,
					(defaultProperties) -> {
						this.profiles = new LinkedList<>();
						this.processedProfiles = new LinkedList<>();
						this.activatedProfiles = false;
						this.loaded = new LinkedHashMap<>();
						// 初始化配置文件
						initializeProfiles();
						while (!this.profiles.isEmpty()) {
							Profile profile = this.profiles.poll();
							if (isDefaultProfile(profile)) {
								addProfileToEnvironment(profile.getName());
							}
							load(profile, this::getPositiveProfileFilter,
									addToLoaded(MutablePropertySources::addLast, false));
							this.processedProfiles.add(profile);
						}
						load(null, this::getNegativeProfileFilter, addToLoaded(MutablePropertySources::addFirst, true));
						addLoadedPropertySources();
						applyActiveProfiles(defaultProperties);
					});
		}

```

### initializeProfiles

- 初始化`private Deque<Profile> profiles;` 属性
- ![image-20200319084902957](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200319084902957-0d408f.png)

### load

- `org.springframework.boot.context.config.ConfigFileApplicationListener.Loader#load(org.springframework.boot.context.config.ConfigFileApplicationListener.Profile, org.springframework.boot.context.config.ConfigFileApplicationListener.DocumentFilterFactory, org.springframework.boot.context.config.ConfigFileApplicationListener.DocumentConsumer)`

```java
private void load(Profile profile, DocumentFilterFactory filterFactory, DocumentConsumer consumer) {
			getSearchLocations().forEach(
					// 本地路径
					(location) -> {
						// 是不是文件夹
						boolean isFolder = location.endsWith("/");
						// 文件名,默认application
						Set<String> names = isFolder ? getSearchNames() : NO_SEARCH_NAMES;
						// 循环加载
						names.forEach((name) -> {
							load(location, name, profile, filterFactory, consumer);
						});
					});
		}
```

- 资源路径可能性

![image-20200319085446640](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200319085446640-f0a3a5.png)

该方法采用循环每个路径下面都去尝试一遍

- 中间过程省略，我们直接看最后的加载行为
  - `org.springframework.boot.context.config.ConfigFileApplicationListener.Loader#loadDocuments`

```java
		private List<Document> loadDocuments(PropertySourceLoader loader, String name, Resource resource)
				throws IOException {
		    // 文档的缓存key
			DocumentsCacheKey cacheKey = new DocumentsCacheKey(loader, resource);
			// 文档信息
			List<Document> documents = this.loadDocumentsCache.get(cacheKey);
			if (documents == null) {
				// 执行加载，将配置文件读取返回
				List<PropertySource<?>> loaded = loader.load(name, resource);
				// 数据转换
				documents = asDocuments(loaded);
				// 缓存设置
				this.loadDocumentsCache.put(cacheKey, documents);
			}
			return documents;
		}

```

此处的`loader.load()`调用具体的 loader 实现类进行执行方法

### yml 解析

```java
	@Override
	public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
		if (!ClassUtils.isPresent("org.yaml.snakeyaml.Yaml", null)) {
			throw new IllegalStateException(
					"Attempted to load " + name + " but snakeyaml was not found on the classpath");
		}
		// 将资源转换成集合对象
		List<Map<String, Object>> loaded = new OriginTrackedYamlLoader(resource).load();
		if (loaded.isEmpty()) {
			return Collections.emptyList();
		}
		List<PropertySource<?>> propertySources = new ArrayList<>(loaded.size());
		for (int i = 0; i < loaded.size(); i++) {
			String documentNumber = (loaded.size() != 1) ? " (document #" + i + ")" : "";
			// 放入返回结果中
			propertySources.add(new OriginTrackedMapPropertySource(name + documentNumber,
					Collections.unmodifiableMap(loaded.get(i)), true));
		}
		return propertySources;
	}

```

![image-20200319090446231](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200319090446231-a9cd2a.png)

- `PropertiesPropertySourceLoader`解析同理不在次展开描述了

### asDocuments

```java
		/**
		 * 将 {@link PropertySource} 转换成 {@link Document}
		 * @param loaded
		 * @return
		 */
		private List<Document> asDocuments(List<PropertySource<?>> loaded) {
			if (loaded == null) {
				return Collections.emptyList();
			}
			return loaded.stream().map(
					// 循环创建新对象
					(propertySource) -> {
						// 对象创建
						Binder binder = new Binder(ConfigurationPropertySources.from(propertySource),
								this.placeholdersResolver);
						/**
						 * 通过 {@link Binder} 将数据进行绑定，创建 {@link Document}进行返回
						 */
						return new Document(propertySource, binder.bind("spring.profiles", STRING_ARRAY).orElse(null),
								getProfiles(binder, ACTIVE_PROFILES_PROPERTY),
								getProfiles(binder, INCLUDE_PROFILES_PROPERTY));
					}).collect(Collectors.toList());
		}

```

---

# SpringBoot ConditionalOnBean

- Author: [HuiFer](https://github.com/huifer)
- 源码阅读仓库: [SourceHot-spring-boot](https://github.com/SourceHot/spring-boot-read)

- 在 SpringBoot 中有下列当 XXX 存在或不存的时候执行初始化
  - ConditionalOnBean
    ConditionalOnClass
    ConditionalOnCloudPlatform
    ConditionalOnExpression
    ConditionalOnJava
    ConditionalOnJndi
    ConditionalOnMissingBean
    ConditionalOnMissingClass
    ConditionalOnNotWebApplication
    ConditionalOnProperty
    ConditionalOnResource
    ConditionalOnSingleCandidate
    ConditionalOnWebApplication

## ConditionalOnBean

```java
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnBeanCondition.class)
public @interface ConditionalOnBean {

   /**
    * 需要匹配的 bean 类型
    */
   Class<?>[] value() default {};

   /**
    * 需要匹配的 bean 类型
    */
   String[] type() default {};

   /**
    * 匹配的 bean 注解
    */
   Class<? extends Annotation>[] annotation() default {};

   /**
    * 需要匹配的 beanName
    */
   String[] name() default {};

   /**
    * 搜索策略
    */
   SearchStrategy search() default SearchStrategy.ALL;

   /**
    */
   Class<?>[] parameterizedContainer() default {};

}
```

## SearchStrategy

```java
public enum SearchStrategy {

   /**
    * 当前 上下文
    */
   CURRENT,

   /**
    * 找所有的父容器
    */
   ANCESTORS,

   /**
    * 当前上下文+父容器
    */
   ALL

}
```

## OnBeanCondition

- org.springframework.boot.autoconfigure.condition.OnBeanCondition

- 这个类是一个条件类,相关的还有

  ```properties
  org.springframework.boot.autoconfigure.AutoConfigurationImportFilter=\
  org.springframework.boot.autoconfigure.condition.OnBeanCondition,\
  org.springframework.boot.autoconfigure.condition.OnClassCondition,\
  org.springframework.boot.autoconfigure.condition.OnWebApplicationCondition
  ```

- 类图

  ![image-20200824085726621](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200824085726621-5e3878.png)

在看这部分源码之前需要先了解 `Conditional`和`Condition`的源码

- 简单描述

  通过实现`Condition` 来确认是否初始化 bean

- 从类图上我们可以看到 `condition` 的继承关系. 在这里需要去找到`SpringBootCondition`

- `org.springframework.boot.autoconfigure.condition.SpringBootCondition#matches(org.springframework.context.annotation.ConditionContext, org.springframework.core.type.AnnotatedTypeMetadata)`

  ```java
  @Override
  public final boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
     // 类名或者方法名标记
     String classOrMethodName = getClassOrMethodName(metadata);
     try {
        // 比较类,子类实现
        ConditionOutcome outcome = getMatchOutcome(context, metadata);
        // 日志输出
        logOutcome(classOrMethodName, outcome);
        // 报告记录
        recordEvaluation(context, classOrMethodName, outcome);
        // 返回匹配结果
        return outcome.isMatch();
     }
     catch (NoClassDefFoundError ex) {
        throw new IllegalStateException("Could not evaluate condition on " + classOrMethodName + " due to "
              + ex.getMessage() + " not found. Make sure your own configuration does not rely on "
              + "that class. This can also happen if you are "
              + "@ComponentScanning a springframework package (e.g. if you "
              + "put a @ComponentScan in the default package by mistake)", ex);
     }
     catch (RuntimeException ex) {
        throw new IllegalStateException("Error processing condition on " + getName(metadata), ex);
     }
  }
  ```

- `getOutcomes` 子类实现

  `org.springframework.boot.autoconfigure.condition.OnBeanCondition#getOutcomes`

  ```java
  String[] autoConfigurationClasses,
  			AutoConfigurationMetadata autoConfigurationMetadata
  ```

  - 第一个参数: 需要自动配置的类
  - 配置注解信息

### ConditionOutcome 和 ConditionMessage

```java
public class ConditionOutcome {

   /**
    * 是否匹配
    */
   private final boolean match;
   /**
    * 条件信息
    */
   private final ConditionMessage message;
}


public final class ConditionMessage {

	private String message;
}
```

- 造一个对象用来进行 debug

```java
@Component
public class Beans {


    @Bean
    public A a() {
        return new A();
    }


    @Bean
    @ConditionalOnBean(value = A.class)
    public B b() {
        return new B();
    }
}

```

## getMatchOutcome

```java
@Override
public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    // 条件信息
    ConditionMessage matchMessage = ConditionMessage.empty();
    // 获取注解求和
    MergedAnnotations annotations = metadata.getAnnotations();
    // 注解是否匹配
    if (annotations.isPresent(ConditionalOnBean.class)) {
        // 搜索 ConditionalOnBean 注解
        Spec<ConditionalOnBean> spec = new Spec<>(context, metadata, annotations,
                ConditionalOnBean.class);
        // 匹配结果
        MatchResult matchResult = getMatchingBeans(context, spec);
        if (!matchResult.isAllMatched()) {
            String reason = createOnBeanNoMatchReason(matchResult);
            return ConditionOutcome.noMatch(spec.message().because(reason));
        }
        // 把注解解析出来获得文本
        matchMessage = spec.message(matchMessage).found("bean", "beans").items(Style.QUOTE,
                matchResult.getNamesOfAllMatches());
    }
    if (metadata.isAnnotated(ConditionalOnSingleCandidate.class.getName())) {
        Spec<ConditionalOnSingleCandidate> spec = new SingleCandidateSpec(context, metadata, annotations);
        MatchResult matchResult = getMatchingBeans(context, spec);
        if (!matchResult.isAllMatched()) {
            return ConditionOutcome.noMatch(spec.message().didNotFind("any beans").atAll());
        }
        else if (!hasSingleAutowireCandidate(context.getBeanFactory(), matchResult.getNamesOfAllMatches(),
                spec.getStrategy() == SearchStrategy.ALL)) {
            return ConditionOutcome.noMatch(spec.message().didNotFind("a primary bean from beans")
                    .items(Style.QUOTE, matchResult.getNamesOfAllMatches()));
        }
        matchMessage = spec.message(matchMessage).found("a primary bean from beans").items(Style.QUOTE,
                matchResult.getNamesOfAllMatches());
    }
    if (metadata.isAnnotated(ConditionalOnMissingBean.class.getName())) {
        Spec<ConditionalOnMissingBean> spec = new Spec<>(context, metadata, annotations,
                ConditionalOnMissingBean.class);
        MatchResult matchResult = getMatchingBeans(context, spec);
        if (matchResult.isAnyMatched()) {
            String reason = createOnMissingBeanNoMatchReason(matchResult);
            return ConditionOutcome.noMatch(spec.message().because(reason));
        }
        matchMessage = spec.message(matchMessage).didNotFind("any beans").atAll();
    }
    return ConditionOutcome.match(matchMessage);
}
```

- 开始方法分析

### getMatchingBeans

- `org.springframework.boot.autoconfigure.condition.OnBeanCondition#getMatchingBeans`

```java
protected final MatchResult getMatchingBeans(ConditionContext context, Spec<?> spec) {
    // 获取上下文
       ClassLoader classLoader = context.getClassLoader();
       // 获取 IOC 容器
   ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
   // 扫描方式比较是否为当前上下文
       boolean considerHierarchy = spec.getStrategy() != SearchStrategy.CURRENT;

   Set<Class<?>> parameterizedContainers = spec.getParameterizedContainers();
   if (spec.getStrategy() == SearchStrategy.ANCESTORS) {
      BeanFactory parent = beanFactory.getParentBeanFactory();
      Assert.isInstanceOf(ConfigurableListableBeanFactory.class, parent,
            "Unable to use SearchStrategy.ANCESTORS");
      beanFactory = (ConfigurableListableBeanFactory) parent;
   }
   // 结果对象初始化
   MatchResult result = new MatchResult();
   Set<String> beansIgnoredByType = getNamesOfBeansIgnoredByType(classLoader, beanFactory, considerHierarchy,
         spec.getIgnoredTypes(), parameterizedContainers);
   for (String type : spec.getTypes()) {
      // 通过类型获取 beanName
      Collection<String> typeMatches = getBeanNamesForType(classLoader, considerHierarchy, beanFactory, type,
            parameterizedContainers);
      typeMatches.removeAll(beansIgnoredByType);
      if (typeMatches.isEmpty()) {
         result.recordUnmatchedType(type);
      }
      else {
         result.recordMatchedType(type, typeMatches);
      }
   }
   for (String annotation : spec.getAnnotations()) {
      Set<String> annotationMatches = getBeanNamesForAnnotation(classLoader, beanFactory, annotation,
            considerHierarchy);
      annotationMatches.removeAll(beansIgnoredByType);
      if (annotationMatches.isEmpty()) {
         result.recordUnmatchedAnnotation(annotation);
      }
      else {
         result.recordMatchedAnnotation(annotation, annotationMatches);
      }
   }
   for (String beanName : spec.getNames()) {
      if (!beansIgnoredByType.contains(beanName) && containsBean(beanFactory, beanName, considerHierarchy)) {
         result.recordMatchedName(beanName);
      }
      else {
         result.recordUnmatchedName(beanName);
      }
   }
   return result;
}
```

- 在`MatchResult result = new MatchResult()` 之前的代码作用是确认 ioc 容器

#### getNamesOfBeansIgnoredByType

```java
    /**
     * 获取忽略的beans(返回对象是 beanName)
     * 循环,忽略的类型, 将类型从 beanFactory 获取,返回
     */
	private Set<String> getNamesOfBeansIgnoredByType(ClassLoader classLoader, ListableBeanFactory beanFactory,
			boolean considerHierarchy, Set<String> ignoredTypes, Set<Class<?>> parameterizedContainers) {
		Set<String> result = null;
		for (String ignoredType : ignoredTypes) {
		    // 从 beanFactory 中获取忽略的beanNames
			Collection<String> ignoredNames = getBeanNamesForType(classLoader, considerHierarchy, beanFactory,
					ignoredType, parameterizedContainers);
			result = addAll(result, ignoredNames);
		}
		return (result != null) ? result : Collections.emptySet();
	}

```

#### getBeanNamesForType

```java
/**
 * 通过类型获取 beanName
 */
private Set<String> getBeanNamesForType(ClassLoader classLoader, boolean considerHierarchy,
      ListableBeanFactory beanFactory, String type, Set<Class<?>> parameterizedContainers) throws LinkageError {
   try {
       // 从beanFactory 中获取忽略的类 返回beanNanme
      return getBeanNamesForType(beanFactory, considerHierarchy, resolve(type, classLoader),
            parameterizedContainers);
   }
   catch (ClassNotFoundException | NoClassDefFoundError ex) {
      return Collections.emptySet();
   }
}
```

#### getBeanNamesForType

```java
/**
 * 通过类型获取 beanName
 */
private Set<String> getBeanNamesForType(ListableBeanFactory beanFactory, boolean considerHierarchy, Class<?> type,
      Set<Class<?>> parameterizedContainers) {
    // 获取beanName
   Set<String> result = collectBeanNamesForType(beanFactory, considerHierarchy, type, parameterizedContainers,
         null);
   return (result != null) ? result : Collections.emptySet();
}
```

#### collectBeanNamesForType

- 这里最终回到了 spring beanFactory 的方法 getBeanNamesForType

```java
private Set<String> collectBeanNamesForType(ListableBeanFactory beanFactory, boolean considerHierarchy,
      Class<?> type, Set<Class<?>> parameterizedContainers, Set<String> result) {
   result = addAll(result, beanFactory.getBeanNamesForType(type, true, false));
   for (Class<?> container : parameterizedContainers) {
      ResolvableType generic = ResolvableType.forClassWithGenerics(container, type);
      result = addAll(result, beanFactory.getBeanNamesForType(generic, true, false));
   }
   if (considerHierarchy && beanFactory instanceof HierarchicalBeanFactory) {
      BeanFactory parent = ((HierarchicalBeanFactory) beanFactory).getParentBeanFactory();
      if (parent instanceof ListableBeanFactory) {
         result = collectBeanNamesForType((ListableBeanFactory) parent, considerHierarchy, type,
               parameterizedContainers, result);
      }
   }
   return result;
}
```

到这里需要忽略的 beanName 就全部找出来了

```java
// 匹配类型在移除
for (String type : spec.getTypes()) {
    // 通过类型获取 beanName
    Collection<String> typeMatches = getBeanNamesForType(classLoader, considerHierarchy, beanFactory, type,
            parameterizedContainers);
    typeMatches.removeAll(beansIgnoredByType);
    if (typeMatches.isEmpty()) {
        result.recordUnmatchedType(type);
    }
    else {
        result.recordMatchedType(type, typeMatches);
    }
}


        // 注解匹配删除忽略的beanname
        for (String annotation : spec.getAnnotations()) {
            Set<String> annotationMatches = getBeanNamesForAnnotation(classLoader, beanFactory, annotation,
                    considerHierarchy);
            annotationMatches.removeAll(beansIgnoredByType);
            if (annotationMatches.isEmpty()) {
                result.recordUnmatchedAnnotation(annotation);
            }
            else {
                result.recordMatchedAnnotation(annotation, annotationMatches);
            }
        }
```

- 在忽略 bean 找到之后做一个类型移除的操作.

![image-20200825140750035](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200825140750035-0dc906.png)

### 返回值

- 在返回之前做一堆判断条件. 一旦符合条件这个地方会做一个 noMatch 的一个对象(`ConditionOutcome`) ，通过返回 match 对象`ConditionOutcome`

```java
public static ConditionOutcome noMatch(ConditionMessage message) {
   return new ConditionOutcome(false, message);
}
```

```java
        if (!matchResult.isAllMatched()) {
                String reason = createOnBeanNoMatchReason(matchResult);
                return ConditionOutcome.noMatch(spec.message().because(reason));
            }
            // 把注解解析出来获得文本
            matchMessage = spec.message(matchMessage).found("bean", "beans").items(Style.QUOTE,
                    matchResult.getNamesOfAllMatches());
        }
        if (metadata.isAnnotated(ConditionalOnSingleCandidate.class.getName())) {
            Spec<ConditionalOnSingleCandidate> spec = new SingleCandidateSpec(context, metadata, annotations);
            MatchResult matchResult = getMatchingBeans(context, spec);
            if (!matchResult.isAllMatched()) {
                return ConditionOutcome.noMatch(spec.message().didNotFind("any beans").atAll());
            }
            else if (!hasSingleAutowireCandidate(context.getBeanFactory(), matchResult.getNamesOfAllMatches(),
                    spec.getStrategy() == SearchStrategy.ALL)) {
                return ConditionOutcome.noMatch(spec.message().didNotFind("a primary bean from beans")
                        .items(Style.QUOTE, matchResult.getNamesOfAllMatches()));
            }
            matchMessage = spec.message(matchMessage).found("a primary bean from beans").items(Style.QUOTE,
                    matchResult.getNamesOfAllMatches());
        }
        if (metadata.isAnnotated(ConditionalOnMissingBean.class.getName())) {
            Spec<ConditionalOnMissingBean> spec = new Spec<>(context, metadata, annotations,
                    ConditionalOnMissingBean.class);
            MatchResult matchResult = getMatchingBeans(context, spec);
            if (matchResult.isAnyMatched()) {
                String reason = createOnMissingBeanNoMatchReason(matchResult);
                return ConditionOutcome.noMatch(spec.message().because(reason));
            }
            matchMessage = spec.message(matchMessage).didNotFind("any beans").atAll();
        }
        return ConditionOutcome.match(matchMessage);
```

![image-20200825141506531](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200825141506531-8446fa.png)

- 到此结果封装完毕.回到方法`org.springframework.boot.autoconfigure.condition.SpringBootCondition#matches(org.springframework.context.annotation.ConditionContext, org.springframework.core.type.AnnotatedTypeMetadata)` 继续进行
  - 再往后就继续执行 spring 的 bean 初始化咯

## MessageSourceAutoConfiguration

- 启动阶段的一个类运行解读

- `org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration`

  ```java
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingBean(name = AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME, search = SearchStrategy.CURRENT)
  @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
  @Conditional(ResourceBundleCondition.class)
  @EnableConfigurationProperties
  public class MessageSourceAutoConfiguration {}
  
  ```

  - 根据类的注解信息我们可以找到有`ResourceBundleCondition`

    ![image-20200825092343271](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200825092343271-8a1b76.png)

- 获取类名或者方法名的结果是`MessageSourceAutoConfiguration`全路径

- 继续往下是一个比较的方法(是否符合 match)

  `org.springframework.boot.autoconfigure.condition.SpringBootCondition#getMatchOutcome`这个方法是一个抽象方法子类实现

- 上图中红框内标注的类为`org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration.ResourceBundleCondition`

  同时继承`org.springframework.boot.autoconfigure.condition.SpringBootCondition`

  并且重写了方法`getMatchOutcome`

  ```java
  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
      // 从 容器中获取
      String basename = context.getEnvironment().getProperty("spring.messages.basename", "messages");
      // 从缓存中获取条件信息
      ConditionOutcome outcome = cache.get(basename);
      if (outcome == null) {
          // 生成条件信息对象
          outcome = getMatchOutcomeForBasename(context, basename);
          // 放入缓存
          cache.put(basename, outcome);
      }
      return outcome;
  }
  ```

  这个方法主要将比较信息放入,

- 后续的行为依然是判断是否匹配,匹配就创建.

## Spring Boot 启动阶段的自动注入

```java
org.springframework.boot.autoconfigure.AutoConfigurationImportSelector#filter
```

```java
private List<String> filter(List<String> configurations, AutoConfigurationMetadata autoConfigurationMetadata) {
   long startTime = System.nanoTime();
   String[] candidates = StringUtils.toStringArray(configurations);
   boolean[] skip = new boolean[candidates.length];
   boolean skipped = false;
   // 获取 AutoConfigurationImportFilter 相关配置
   for (AutoConfigurationImportFilter filter : getAutoConfigurationImportFilters()) {
      // 执行 aware 相关接口
      invokeAwareMethods(filter);
      // 是否可以初始化的结果
      boolean[] match = filter.match(candidates, autoConfigurationMetadata);
      for (int i = 0; i < match.length; i++) {
         if (!match[i]) {
            // 是否跳过
            skip[i] = true;
            candidates[i] = null;
            skipped = true;
         }
      }
   }
   if (!skipped) {
      return configurations;
   }
   List<String> result = new ArrayList<>(candidates.length);
   // 处理最终需要的类
   for (int i = 0; i < candidates.length; i++) {
      if (!skip[i]) {
         result.add(candidates[i]);
      }
   }
   if (logger.isTraceEnabled()) {
      int numberFiltered = configurations.size() - result.size();
      logger.trace("Filtered " + numberFiltered + " auto configuration class in "
            + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + " ms");
   }
   return new ArrayList<>(result);
}
```

- 在这里有一个关注点 循环方法`getAutoConfigurationImportFilters()`

```java
protected List<AutoConfigurationImportFilter> getAutoConfigurationImportFilters() {
   return SpringFactoriesLoader.loadFactories(AutoConfigurationImportFilter.class, this.beanClassLoader);
}
```

在`spring.factories`文件中找到`AutoConfigurationImportFilter`后面的值

```properties
org.springframework.boot.autoconfigure.AutoConfigurationImportFilter=\
org.springframework.boot.autoconfigure.condition.OnBeanCondition,\
org.springframework.boot.autoconfigure.condition.OnClassCondition,\
org.springframework.boot.autoconfigure.condition.OnWebApplicationCondition
```

- 此时我们可以和前文的源码分析连接起来有一个完整的认识了

  ![image-20200825142332485](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200825142332485-25d64d.png)

- 最后来看整体类图

  ![image-20200825142418115](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200825142418115-2e4ca1.png)

# SpringBoot ConfigurationProperties

- Author: [HuiFer](https://github.com/huifer)
- 源码阅读仓库: [SourceHot-spring-boot](https://github.com/SourceHot/spring-boot-read)

- 本文主要对`org.springframework.boot.context.properties.ConfigurationProperties`进行分析

## ConfigurationProperties

- 顶部注释

```java
 * @see ConfigurationPropertiesScan
 * @see ConstructorBinding
 * @see ConfigurationPropertiesBindingPostProcessor
 * @see EnableConfigurationProperties
```

看到`ConfigurationPropertiesScan` 去看看这个

## ConfigurationPropertiesScan

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ConfigurationPropertiesScanRegistrar.class)
@EnableConfigurationProperties
public @interface ConfigurationPropertiesScan {}
```

- 熟悉的**Import**注解

## ConfigurationPropertiesScanRegistrar

![image-20200323094446756](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323094446756-ea05fb.png)

- debug 没有抓到后续补充

## EnableConfigurationProperties

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EnableConfigurationPropertiesRegistrar.class)
public @interface EnableConfigurationProperties {
}
```

## EnableConfigurationPropertiesRegistrar

- 该类会读取**spring.factories**

- 中`org.springframework.boot.autoconfigure.EnableAutoConfiguration=\` 这样的

  ```
  org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration
  org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration$DispatcherServletConfiguration
  org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration$DispatcherServletRegistrationConfiguration
  org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
  org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
  org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration$WebMvcAutoConfigurationAdapter
  org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration
  org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration$Jackson2ObjectMapperBuilderCustomizerConfiguration
  org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration$StringHttpMessageConverterConfiguration
  org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration
  org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration
  org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration
  org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration
  org.springframework.boot.autoconfigure.web.servlet.HttpEncodingAutoConfiguration
  org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration
  ```

```java
	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
	    // 注册bean
		registerInfrastructureBeans(registry);
	  // 配置属性Bean注册器
		ConfigurationPropertiesBeanRegistrar beanRegistrar = new ConfigurationPropertiesBeanRegistrar(registry);
		// 循环注册
		getTypes(metadata).forEach(beanRegistrar::register);
	}


```

### registerInfrastructureBeans

```java
	static void registerInfrastructureBeans(BeanDefinitionRegistry registry) {
	    // 属性绑定后置处理器
		ConfigurationPropertiesBindingPostProcessor.register(registry);
		// 属性校验器
		ConfigurationPropertiesBeanDefinitionValidator.register(registry);
		ConfigurationBeanFactoryMetadata.register(registry);
	}
```

- 此处操作逻辑基本相同,是否存在这个 beanName 存在直接注册，不存在补充

#### ConfigurationPropertiesBindingPostProcessor.register(registry)

```java
	public static void register(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "Registry must not be null");
		// 是否存在
		if (!registry.containsBeanDefinition(BEAN_NAME)) {
			GenericBeanDefinition definition = new GenericBeanDefinition();
			definition.setBeanClass(ConfigurationPropertiesBindingPostProcessor.class);
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(BEAN_NAME, definition);
		}
		ConfigurationPropertiesBinder.register(registry);
	}

```

#### ConfigurationPropertiesBeanDefinitionValidator.register(registry)

```java
	static void register(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "Registry must not be null");
		if (!registry.containsBeanDefinition(BEAN_NAME)) {
			GenericBeanDefinition definition = new GenericBeanDefinition();
			definition.setBeanClass(ConfigurationPropertiesBeanDefinitionValidator.class);
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(BEAN_NAME, definition);
		}
		ConfigurationPropertiesBinder.register(registry);
	}

```

### getTypes(metadata).forEach(beanRegistrar::register)

- 先看输入参数 **metadata**

![image-20200323134135926](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323134135926-e693e3.png)

- getTypes 结果

![image-20200323134325955](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323134325955-7261f8.png)

- 源码开始，先找出刚才的对象`org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration`

  ```java
  @Configuration(proxyBeanMethods = false)
  @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
  @ConditionalOnClass(ServletRequest.class)
  @ConditionalOnWebApplication(type = Type.SERVLET)
  @EnableConfigurationProperties(ServerProperties.class)
  @Import({ ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class,
  		ServletWebServerFactoryConfiguration.EmbeddedTomcat.class,
  		ServletWebServerFactoryConfiguration.EmbeddedJetty.class,
  		ServletWebServerFactoryConfiguration.EmbeddedUndertow.class })
  public class ServletWebServerFactoryAutoConfiguration {}
  ```

```java
    /**
     * 找出 {@link EnableConfigurationProperties} 注解标记的中的属性值,并且返回值不是void
     * @param metadata
     * @return
     */
	private Set<Class<?>> getTypes(AnnotationMetadata metadata) {
		return

		 metadata.getAnnotations().stream(EnableConfigurationProperties.class)
				.flatMap((annotation) -> Arrays.stream(annotation.getClassArray(MergedAnnotation.VALUE)))
				.filter((type) -> void.class != type).collect(Collectors.toSet());
	}


```

- 这里我们可以直接知道返回的是`@EnableConfigurationProperties(ServerProperties.class)` 的数据值: `ServerProperties.class`

循环注册

```java
	void register(Class<?> type) {
		MergedAnnotation<ConfigurationProperties> annotation = MergedAnnotations
				.from(type, SearchStrategy.TYPE_HIERARCHY).get(ConfigurationProperties.class);
		register(type, annotation);
	}

```

---

## ConfigurationPropertiesBindingPostProcessor

![image-20200323095626953](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323095626953-e5c6de.png)

### postProcessBeforeInitialization

```java
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		// 绑定
		bind(ConfigurationPropertiesBean.get(this.applicationContext, bean, beanName));
		return bean;
	}

```

- get

  ```java
  	public static ConfigurationPropertiesBean get(ApplicationContext applicationContext, Object bean, String beanName) {
  		// 寻找工厂方法
  		Method factoryMethod = findFactoryMethod(applicationContext, beanName);
  		// 创建 ConfigurationPropertiesBean
  		return create(beanName, bean, bean.getClass(), factoryMethod);
  	}
  
  ```

```java
	private static Method findFactoryMethod(ConfigurableListableBeanFactory beanFactory, String beanName) {
		// 判断是否存在这个beanName
		if (beanFactory.containsBeanDefinition(beanName)) {
		    // 获取bean定义
			BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
			// 类型判断
			if (beanDefinition instanceof RootBeanDefinition) {
			    // 解析方法
				Method resolvedFactoryMethod = ((RootBeanDefinition) beanDefinition).getResolvedFactoryMethod();
				if (resolvedFactoryMethod != null) {
					return resolvedFactoryMethod;
				}
			}
			return findFactoryMethodUsingReflection(beanFactory, beanDefinition);
		}
		return null;
	}

```

```java
	private static Method findFactoryMethodUsingReflection(ConfigurableListableBeanFactory beanFactory,
			BeanDefinition beanDefinition) {
		// 工厂方法
		String factoryMethodName = beanDefinition.getFactoryMethodName();
		// 工厂bean
		String factoryBeanName = beanDefinition.getFactoryBeanName();
		if (factoryMethodName == null || factoryBeanName == null) {
			return null;
		}
		// 转换对象
		Class<?> factoryType = beanFactory.getType(factoryBeanName);
		if (factoryType.getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) {
			factoryType = factoryType.getSuperclass();
		}
		AtomicReference<Method> factoryMethod = new AtomicReference<>();
		ReflectionUtils.doWithMethods(factoryType, (method) -> {
			  // 判断是否是需要的方法
		    if (method.getName().equals(factoryMethodName)) {
				// 设置方法
				factoryMethod.set(method);
			}
		});
		// 返回方法
		return factoryMethod.get();
	}

```

### create

- `org.springframework.boot.context.properties.ConfigurationPropertiesBean#create`

```java
	private static ConfigurationPropertiesBean create(String name, Object instance, Class<?> type, Method factory) {
		// 找注解
		ConfigurationProperties annotation = findAnnotation(instance, type, factory, ConfigurationProperties.class);
		if (annotation == null) {
			return null;
		}
		// 找注解
		Validated validated = findAnnotation(instance, type, factory, Validated.class);
		// 注解列表
		Annotation[] annotations = (validated != null) ? new Annotation[] { annotation, validated }
				: new Annotation[] { annotation };
		// 类型解析
		ResolvableType bindType = (factory != null) ? ResolvableType.forMethodReturnType(factory)
				: ResolvableType.forClass(type);
		// 绑定结果对象
		Bindable<Object> bindTarget = Bindable.of(bindType).withAnnotations(annotations);
		if (instance != null) {
			bindTarget = bindTarget.withExistingValue(instance);
		}
		return new ConfigurationPropertiesBean(name, instance, annotation, bindTarget);
	}

```

- 第一个需要做的类: `org.springframework.boot.autoconfigure.web.ServerProperties`

- `annotation`

![image-20200323104711545](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323104711545-7f3250.png)

- `bindType`

![image-20200323104815305](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323104815305-77f2bc.png)

- 返回对象

![image-20200323105053757](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323105053757-f27c50.png)

- 此时数据还没有进去

#### bind

- 数据绑定

直接看结果

![image-20200323105155998](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323105155998-c4fef6.png)

- 上述配置和我在配置文件中写的配置一致

```yml
server:
  port: 9999
```

- 具体方法: `org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor#bind`

```java
	private void bind(ConfigurationPropertiesBean bean) {
		if (bean == null || hasBoundValueObject(bean.getName())) {
			return;
		}
		Assert.state(bean.getBindMethod() == BindMethod.JAVA_BEAN, "Cannot bind @ConfigurationProperties for bean '"
				+ bean.getName() + "'. Ensure that @ConstructorBinding has not been applied to regular bean");
		try {
			// 最终的绑定
			this.binder.bind(bean);
		}
		catch (Exception ex) {
			throw new ConfigurationPropertiesBindException(bean, ex);
		}
	}

```

```java
BindResult<?> bind(ConfigurationPropertiesBean propertiesBean) {
	    // 最后的结果
		Bindable<?> target = propertiesBean.asBindTarget();
		// 注解获取
		ConfigurationProperties annotation = propertiesBean.getAnnotation();
		// 获取处理器
		BindHandler bindHandler = getBindHandler(target, annotation);
		//
		return getBinder().bind(annotation.prefix(), target, bindHandler);
	}
```

![image-20200323105830138](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323105830138-08e6a6.png)

##### findProperty

```java
	private ConfigurationProperty findProperty(ConfigurationPropertyName name, Context context) {
		if (name.isEmpty()) {
			return null;
		}
		for (ConfigurationPropertySource source : context.getSources()) {
		    // 获取具体的一个属性值
			ConfigurationProperty property = source.getConfigurationProperty(name);
			if (property != null) {
				return property;
			}
		}
		return null;
	}

```

- `org.springframework.boot.context.properties.source.SpringConfigurationPropertySource#getConfigurationProperty`

  ```java
  	@Override
  	public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
  		PropertyMapping[] mappings = getMapper().map(name);
  		return find(mappings, name);
  	}
  
  ```

  ```java
  	protected final ConfigurationProperty find(PropertyMapping[] mappings, ConfigurationPropertyName name) {
  		for (PropertyMapping candidate : mappings) {
  			if (candidate.isApplicable(name)) {
  				ConfigurationProperty result = find(candidate);
  				if (result != null) {
  					return result;
  				}
  			}
  		}
  		return null;
  	}
  
  ```

  ```java
  	private ConfigurationProperty find(PropertyMapping mapping) {
  	    // 需要读取的配置信息的key
  		String propertySourceName = mapping.getPropertySourceName();
  		// 信息的value
  		Object value = getPropertySource().getProperty(propertySourceName);
  		if (value == null) {
  			return null;
  		}
  		// 创建对象
  		ConfigurationPropertyName configurationPropertyName = mapping.getConfigurationPropertyName();
  		Origin origin = PropertySourceOrigin.get(this.propertySource, propertySourceName);
  		// 包装返回
  		return ConfigurationProperty.of(configurationPropertyName, value, origin);
  	}
  
  ```

![image-20200323115408877](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323115408877-aaa8d1.png)

![image-20200323115701118](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323115701118-7056fb.png)

![image-20200323115711826](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323115711826-e96510.png)

##### getBindHandler

```java
private <T> BindHandler getBindHandler(Bindable<T> target, ConfigurationProperties annotation) {
		// 获取校验接口列表
		List<Validator> validators = getValidators(target);
		// 处理器
		BindHandler handler = new IgnoreTopLevelConverterNotFoundBindHandler();
		if (annotation.ignoreInvalidFields()) {
			// 忽略错误的绑定处理器
			handler = new IgnoreErrorsBindHandler(handler);
		}
		if (!annotation.ignoreUnknownFields()) {
			UnboundElementsSourceFilter filter = new UnboundElementsSourceFilter();
			// 未绑定元素处理器
			handler = new NoUnboundElementsBindHandler(handler, filter);
		}
		if (!validators.isEmpty()) {
			// 校验绑定处理器
			handler = new ValidationBindHandler(handler, validators.toArray(new Validator[0]));
		}
		for (ConfigurationPropertiesBindHandlerAdvisor advisor : getBindHandlerAdvisors()) {
			// handler
			handler = advisor.apply(handler);
		}
		return handler;
	}
```

- 最终获取得到的处理器

![image-20200323110603959](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323110603959-6900cf.png)

- 最后的 bind

```java
	private <T> Object bindObject(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler,
			Context context, boolean allowRecursiveBinding) {
	    // 获取属性
		ConfigurationProperty property = findProperty(name, context);
		if (property == null && containsNoDescendantOf(context.getSources(), name) && context.depth != 0) {
			return null;
		}
		AggregateBinder<?> aggregateBinder = getAggregateBinder(target, context);
		if (aggregateBinder != null) {
			return bindAggregate(name, target, handler, context, aggregateBinder);
		}
		if (property != null) {
			try {
				return bindProperty(target, context, property);
			}
			catch (ConverterNotFoundException ex) {
				// We might still be able to bind it using the recursive binders
				Object instance = bindDataObject(name, target, handler, context, allowRecursiveBinding);
				if (instance != null) {
					return instance;
				}
				throw ex;
			}
		}
		return bindDataObject(name, target, handler, context, allowRecursiveBinding);
	}

```

![image-20200323112945449](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323112945449-96fdcb.png)

配置信息到此绑定成功,关于如何处理集合相关的配置请各位读者自行学习

---

# SpringBoot 日志系统

- Author: [HuiFer](https://github.com/huifer)
- 源码阅读仓库: [SourceHot-spring-boot](https://github.com/SourceHot/spring-boot-read)

- 包路径: `org.springframework.boot.logging`

## 日志级别

- 日志级别: `org.springframework.boot.logging.LogLevel`

  ```java
  public enum LogLevel {
  	TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF
  }
  ```

## Java 日志实现

- `org.springframework.boot.logging.java.JavaLoggingSystem`

  ![image-20200323144523848](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323144523848-96b596.png)

  ```java
  static {
  	// KEY :  springBoot 定义的日志级别, value: jdk 定义的日志级别
  	LEVELS.map(LogLevel.TRACE, Level.FINEST);
  	LEVELS.map(LogLevel.DEBUG, Level.FINE);
  	LEVELS.map(LogLevel.INFO, Level.INFO);
  	LEVELS.map(LogLevel.WARN, Level.WARNING);
  	LEVELS.map(LogLevel.ERROR, Level.SEVERE);
  	LEVELS.map(LogLevel.FATAL, Level.SEVERE);
  	LEVELS.map(LogLevel.OFF, Level.OFF);
  }
  ```

- LEVELS 对象

  ```java
  protected static class LogLevels<T> {
  	/**
  	 * key ： SpringBoot 中定义的日志级别, value: 其他日志框架的日志级别
  	 */
  	private final Map<LogLevel, T> systemToNative;
  	/**
  	 * key : 其他日志框架的日志级别 , value: springBoot 中定义中定义的日志级别
  	 */
  	private final Map<T, LogLevel> nativeToSystem;
  }
  ```

## LoggingSystem

- 抽象类

- `org.springframework.boot.logging.LoggingSystem`

- 一个 map 对象: `SYSTEMS`

  ```java
  /**
   * key: 第三方日志框架的类 value: springBoot 中的处理类
   */
  private static final Map<String, String> SYSTEMS;
  
  static {
  	Map<String, String> systems = new LinkedHashMap<>();
  	systems.put("ch.qos.logback.core.Appender", "org.springframework.boot.logging.logback.LogbackLoggingSystem");
  	systems.put("org.apache.logging.log4j.core.impl.Log4jContextFactory",
  			"org.springframework.boot.logging.log4j2.Log4J2LoggingSystem");
  	systems.put("java.util.logging.LogManager", "org.springframework.boot.logging.java.JavaLoggingSystem");
  	SYSTEMS = Collections.unmodifiableMap(systems);
  }
  ```

- 各个抽象方法

  | 方法名称                | 作用                               |
  | ----------------------- | ---------------------------------- |
  | beforeInitialize        | 初始化之前调用，目的是减少日志输出 |
  | initialize              | 初始化日志                         |
  | cleanUp                 | 清除日志                           |
  | getShutdownHandler      |                                    |
  | getSupportedLogLevels   | 获取支持的日志级别                 |
  | setLogLevel             | 设置日志级别                       |
  | getLoggerConfigurations | 获取日志配置                       |

### get

```java
public static LoggingSystem get(ClassLoader classLoader) {
	// 获取系统属性
	String loggingSystem = System.getProperty(SYSTEM_PROPERTY);

	if (StringUtils.hasLength(loggingSystem)) {
		// 是不是NONE
		if (NONE.equals(loggingSystem)) {
			// 空的日志系统
			return new NoOpLoggingSystem();
		}
		return get(classLoader, loggingSystem);
	}
	// 循环所有日志,
	return SYSTEMS.entrySet().stream().filter((entry) -> ClassUtils.isPresent(entry.getKey(), classLoader))
			.map((entry) ->
			// 实例化具体日志
				get(classLoader, entry.getValue())).findFirst()
			.orElseThrow(() -> new IllegalStateException("No suitable logging system located"));
}
```

- 实例化日志系统

```java
private static LoggingSystem get(ClassLoader classLoader, String loggingSystemClass) {
	try {
		Class<?> systemClass = ClassUtils.forName(loggingSystemClass, classLoader);
		Constructor<?> constructor = systemClass.getDeclaredConstructor(ClassLoader.class);
		constructor.setAccessible(true);
		return (LoggingSystem) constructor.newInstance(classLoader);
	}
	catch (Exception ex) {
		throw new IllegalStateException(ex);
	}
}

```

![image-20200323151409473](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323151409473-f8626a.png)

- 默认日志: `org.springframework.boot.logging.logback.LogbackLoggingSystem`

### beforeInitialize

- 初始化之前

  ![image-20200323154205484](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323154205484-27f070.png)

- 链路

  1.  `org.springframework.boot.context.logging.LoggingApplicationListener#onApplicationEvent`
  2.  `org.springframework.boot.context.logging.LoggingApplicationListener#onApplicationStartingEvent`
  3.  `org.springframework.boot.logging.LoggingSystem#beforeInitialize`

- 因为前文中我们已知对象是：`org.springframework.boot.logging.logback.LogbackLoggingSystem` 直接看这个类的 `beforeInitialize` 方法

  ```java
  @Override
  public void beforeInitialize() {
  	// 日志上下文
  	LoggerContext loggerContext = getLoggerContext();
  	// 是否初始化
  	if (isAlreadyInitialized(loggerContext)) {
  		return;
  	}
  	// 父类方法
  	super.beforeInitialize();
  	// 添加过滤器
  	loggerContext.getTurboFilterList().add(FILTER);
  }
  ```

- 初始化之前的的操作完成了初始化方法开始

### initialize

- `org.springframework.boot.context.logging.LoggingApplicationListener#onApplicationEnvironmentPreparedEvent`

  ```java
  private void onApplicationEnvironmentPreparedEvent(ApplicationEnvironmentPreparedEvent event) {
  	if (this.loggingSystem == null) {
  		this.loggingSystem = LoggingSystem.get(event.getSpringApplication().getClassLoader());
  	}
  	initialize(event.getEnvironment(), event.getSpringApplication().getClassLoader());
  }
  
  ```

- `org.springframework.boot.context.logging.LoggingApplicationListener#initializeSystem`

  ```java
  protected void initialize(ConfigurableEnvironment environment, ClassLoader classLoader) {
  	new LoggingSystemProperties(environment).apply();
  	this.logFile = LogFile.get(environment);
  	if (this.logFile != null) {
  		this.logFile.applyToSystemProperties();
  	}
  	this.loggerGroups = new LoggerGroups(DEFAULT_GROUP_LOGGERS);
  	// 早期 的日志级别
  	initializeEarlyLoggingLevel(environment);
  	// 初始化日志系统
  	initializeSystem(environment, this.loggingSystem, this.logFile);
  	// 初始化日志级别
  	initializeFinalLoggingLevels(environment, this.loggingSystem);
  	registerShutdownHookIfNecessary(environment, this.loggingSystem);
  }
  
  ```

  ```java
  private void initializeSystem(ConfigurableEnvironment environment, LoggingSystem system, LogFile logFile) {
  	LoggingInitializationContext initializationContext = new LoggingInitializationContext(environment);
  	String logConfig = environment.getProperty(CONFIG_PROPERTY);
  	if (ignoreLogConfig(logConfig)) {
  		// 日志系统初始化
  		system.initialize(initializationContext, null, logFile);
  	}
  	else {
  		try {
  			ResourceUtils.getURL(logConfig).openStream().close();
  			system.initialize(initializationContext, logConfig, logFile);
  		}
  		catch (Exception ex) {
  			// NOTE: We can't use the logger here to report the problem
  			System.err.println("Logging system failed to initialize using configuration from '" + logConfig + "'");
  			ex.printStackTrace(System.err);
  			throw new IllegalStateException(ex);
  		}
  	}
  }
  ```

- `org.springframework.boot.logging.logback.LogbackLoggingSystem#initialize`

  ```java
  @Override
  public void initialize(LoggingInitializationContext initializationContext, String configLocation, LogFile logFile) {
  	LoggerContext loggerContext = getLoggerContext();
  	if (isAlreadyInitialized(loggerContext)) {
  		return;
  	}
  	// 日志初始化
  	super.initialize(initializationContext, configLocation, logFile);
  	loggerContext.getTurboFilterList().remove(FILTER);
  	markAsInitialized(loggerContext);
  	if (StringUtils.hasText(System.getProperty(CONFIGURATION_FILE_PROPERTY))) {
  		getLogger(LogbackLoggingSystem.class.getName()).warn("Ignoring '" + CONFIGURATION_FILE_PROPERTY
  				+ "' system property. Please use 'logging.config' instead.");
  	}
  }
  
  ```

- `org.springframework.boot.logging.AbstractLoggingSystem#initializeWithConventions`

  ```java
  private void initializeWithConventions(LoggingInitializationContext initializationContext, LogFile logFile) {
  	String config = getSelfInitializationConfig();
  	if (config != null && logFile == null) {
  		// self initialization has occurred, reinitialize in case of property changes
  		reinitialize(initializationContext);
  		return;
  	}
  	if (config == null) {
  		config = getSpringInitializationConfig();
  	}
  	if (config != null) {
  		loadConfiguration(initializationContext, config, logFile);
  		return;
  	}
  	// 加载默认配置
  	loadDefaults(initializationContext, logFile);
  }
  ```

- `org.springframework.boot.logging.logback.LogbackLoggingSystem#loadDefaults`

  ```java
  @Override
  protected void loadDefaults(LoggingInitializationContext initializationContext, LogFile logFile) {
  	LoggerContext context = getLoggerContext();
  	stopAndReset(context);
  	boolean debug = Boolean.getBoolean("logback.debug");
  	if (debug) {
  		StatusListenerConfigHelper.addOnConsoleListenerInstance(context, new OnConsoleStatusListener());
  	}
  	LogbackConfigurator configurator = debug ? new DebugLogbackConfigurator(context)
  			: new LogbackConfigurator(context);
  	Environment environment = initializationContext.getEnvironment();
  	context.putProperty(LoggingSystemProperties.LOG_LEVEL_PATTERN,
  			environment.resolvePlaceholders("${logging.pattern.level:${LOG_LEVEL_PATTERN:%5p}}"));
  	context.putProperty(LoggingSystemProperties.LOG_DATEFORMAT_PATTERN, environment.resolvePlaceholders(
  			"${logging.pattern.dateformat:${LOG_DATEFORMAT_PATTERN:yyyy-MM-dd HH:mm:ss.SSS}}"));
  	context.putProperty(LoggingSystemProperties.ROLLING_FILE_NAME_PATTERN, environment
  			.resolvePlaceholders("${logging.pattern.rolling-file-name:${LOG_FILE}.%d{yyyy-MM-dd}.%i.gz}"));
  	new DefaultLogbackConfiguration(initializationContext, logFile).apply(configurator);
  	context.setPackagingDataEnabled(true);
  }
  ```

  ```java
  @Override
  public void initialize(LoggingInitializationContext initializationContext, String configLocation, LogFile logFile) {
  	LoggerContext loggerContext = getLoggerContext();
  	// 是否加载过
  	if (isAlreadyInitialized(loggerContext)) {
  		return;
  	}
  	// 日志初始化
  	super.initialize(initializationContext, configLocation, logFile);
  	// 删除 FILTER
  	loggerContext.getTurboFilterList().remove(FILTER);
  	// 初始化标记
  	markAsInitialized(loggerContext);
  	if (StringUtils.hasText(System.getProperty(CONFIGURATION_FILE_PROPERTY))) {
  		getLogger(LogbackLoggingSystem.class.getName()).warn("Ignoring '" + CONFIGURATION_FILE_PROPERTY
  				+ "' system property. Please use 'logging.config' instead.");
  	}
  }
  ```

- 标记 `markAsInitialized`

  ```java
  private void markAsInitialized(LoggerContext loggerContext) {
  	loggerContext.putObject(LoggingSystem.class.getName(), new Object());
  }
  ```

此时日志初始化完成。

### 默认配置文件

- `getStandardConfigLocations` 这个方法定义了默认配置文件有哪些。

  ```java
  @Override
  protected String[] getStandardConfigLocations() {
  	return new String[] { "logback-test.groovy", "logback-test.xml", "logback.groovy", "logback.xml" };
  }
  ```

- 切回 `org.springframework.boot.logging.AbstractLoggingSystem#initializeWithConventions` 方法

- 添加依赖

  ```XML
  <dependency>
  	<groupId>org.springframework.boot</groupId>
  	<artifactId>spring-boot-starter-logging</artifactId>
  	<version>${revision}</version>
  </dependency>
  
  ```

- 添加配置文件

  ![image-20200323161442058](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323161442058-917c93.png)

  ![image-20200323161522570](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323161522570-caedf7.png)

- 此时配置文件地址出现了

  ```java
  protected String getSelfInitializationConfig() {
  	// 寻找配置文件
  	return findConfig(getStandardConfigLocations());
  }
  ```

  ```java
  @Override
  protected String[] getStandardConfigLocations() {
  	return new String[] { "logback-test.groovy", "logback-test.xml", "logback.groovy", "logback.xml" };
  }
  
  ```

  ```java
  private String findConfig(String[] locations) {
  	for (String location : locations) {
  		ClassPathResource resource = new ClassPathResource(location, this.classLoader);
  		if (resource.exists()) {
  			return "classpath:" + location;
  		}
  	}
  	return null;
  }
  ```

- 此时自定义配置文件如何获取的已经明了。

#### reinitialize

```java
@Override
protected void reinitialize(LoggingInitializationContext initializationContext) {
	// 日志上下文重新设置
	getLoggerContext().reset();
	getLoggerContext().getStatusManager().clear();
	// 加载配置文件
	loadConfiguration(initializationContext, getSelfInitializationConfig(), null);
}
```

```java
@Override
protected void loadConfiguration(LoggingInitializationContext initializationContext, String location,
		LogFile logFile) {
	// 父类方法
	super.loadConfiguration(initializationContext, location, logFile);
	// 获取上下文
	LoggerContext loggerContext = getLoggerContext();
	// 停止并且重启
	stopAndReset(loggerContext);
	try {
		// 配置文件加载
		configureByResourceUrl(initializationContext, loggerContext, ResourceUtils.getURL(location));
	}
	catch (Exception ex) {
		throw new IllegalStateException("Could not initialize Logback logging from " + location, ex);
	}
	List<Status> statuses = loggerContext.getStatusManager().getCopyOfStatusList();
	StringBuilder errors = new StringBuilder();
	for (Status status : statuses) {
		if (status.getLevel() == Status.ERROR) {
			errors.append((errors.length() > 0) ? String.format("%n") : "");
			errors.append(status.toString());
		}
	}
	if (errors.length() > 0) {
		throw new IllegalStateException(String.format("Logback configuration error detected: %n%s", errors));
	}
}

```

```java
private void configureByResourceUrl(LoggingInitializationContext initializationContext, LoggerContext loggerContext,
		URL url) throws JoranException {
	if (url.toString().endsWith("xml")) {
		// logback 日志操作
		JoranConfigurator configurator = new SpringBootJoranConfigurator(initializationContext);
		// 设置上下文
		configurator.setContext(loggerContext);
		// 执行配置
		configurator.doConfigure(url);
	}
	else {
		new ContextInitializer(loggerContext).configureByResource(url);
	}
}

```

执行配置属于 logback 操作源码不在此进行分析。

# SpringBoot 启动方法

- Author: [HuiFer](https://github.com/huifer)
- 源码阅读仓库: [SourceHot-spring-boot](https://github.com/SourceHot/spring-boot-read)

## 入口

- 通常一个简单的 SpringBoot 基础项目我们会有如下代码

```java
@SpringBootApplication
@RestController
@RequestMapping("/")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}

```

- 值得关注的有`SpringApplication.run`以及注解`@SpringBootApplication`

### run 方法

```java
	public ConfigurableApplicationContext run(String... args) {
	    // 秒表
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		ConfigurableApplicationContext context = null;
		Collection<SpringBootExceptionReporter> exceptionReporters = new ArrayList<>();
		configureHeadlessProperty();
		// 获取监听器
		SpringApplicationRunListeners listeners = getRunListeners(args);
		// 监听器启动
		listeners.starting();
		try {
		    // application 启动参数列表
			ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
			ConfigurableEnvironment environment = prepareEnvironment(listeners, applicationArguments);
			// 配置忽略的bean信息
			configureIgnoreBeanInfo(environment);
			Banner printedBanner = printBanner(environment);
			// 创建应用上下文
			context = createApplicationContext();
			exceptionReporters = getSpringFactoriesInstances(SpringBootExceptionReporter.class,
					new Class[] { ConfigurableApplicationContext.class }, context);
		    // 准备上下文，装配bean
			prepareContext(context, environment, listeners, applicationArguments, printedBanner);
			// 上下文刷新
			refreshContext(context);
			// 刷新后做什么
			afterRefresh(context, applicationArguments);
			stopWatch.stop();
			if (this.logStartupInfo) {
				new StartupInfoLogger(this.mainApplicationClass).logStarted(getApplicationLog(), stopWatch);
			}
			// 监听器开始了
			listeners.started(context);
			// 唤醒
			callRunners(context, applicationArguments);
		}
		catch (Throwable ex) {
			handleRunFailure(context, ex, exceptionReporters, listeners);
			throw new IllegalStateException(ex);
		}

		try {
		    // 监听器正式运行
			listeners.running(context);
		}
		catch (Throwable ex) {
			handleRunFailure(context, ex, exceptionReporters, null);
			throw new IllegalStateException(ex);
		}
		return context;
	}

```

### getRunListeners

- 获取监听器

```java
	private SpringApplicationRunListeners getRunListeners(String[] args) {
		Class<?>[] types = new Class<?>[] { SpringApplication.class, String[].class };
		// 获取  Spring Factory 实例对象
		return new SpringApplicationRunListeners(logger,
				getSpringFactoriesInstances(SpringApplicationRunListener.class, types, this, args));
	}


	private <T> Collection<T> getSpringFactoriesInstances(Class<T> type, Class<?>[] parameterTypes, Object... args) {
		ClassLoader classLoader = getClassLoader();
		// Use names and ensure unique to protect against duplicates
		// 读取 spring.factories
		Set<String> names = new LinkedHashSet<>(SpringFactoriesLoader.loadFactoryNames(type, classLoader));
		// 创建SpringFactory实例
		List<T> instances = createSpringFactoriesInstances(type, parameterTypes, classLoader, args, names);
		/**
		 * 排序 {@link Ordered}
		 */
		AnnotationAwareOrderComparator.sort(instances);
		return instances;
	}
```

### createSpringFactoriesInstances

```java
@SuppressWarnings("unchecked")
private <T> List<T> createSpringFactoriesInstances(Class<T> type, Class<?>[] parameterTypes,
		ClassLoader classLoader, Object[] args, Set<String> names) {
    // 初始化
	List<T> instances = new ArrayList<>(names.size());
	for (String name : names) {
		try {
		    // 通过名字创建类的class对象
			Class<?> instanceClass = ClassUtils.forName(name, classLoader);
			Assert.isAssignable(type, instanceClass);
			// 构造器获取
			Constructor<?> constructor = instanceClass.getDeclaredConstructor(parameterTypes);
			// 创建具体实例
			T instance = (T) BeanUtils.instantiateClass(constructor, args);
			// 加入实例表中
			instances.add(instance);
		}
		catch (Throwable ex) {
			throw new IllegalArgumentException("Cannot instantiate " + type + " : " + name, ex);
		}
	}
	return instances;
}

```

- `SpringFactoriesLoader.loadFactoryNames(type, classLoader)` 是 spring 提供的方法，主要目的是读取`spring.factories`文件
  - 读取需要创建的内容

![image-20200318080601725](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200318080601725-f3c029.png)

- 创建完成

  ![image-20200318080901881](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200318080901881-f65156.png)

- `AnnotationAwareOrderComparator.sort(instances)`排序

  - 通过 spring 的源码我们知道这个方法是根据`order`的数字大小进行排序，观察

    `SharedMetadataReaderFactoryContextInitializer`

    ![image-20200318081112670](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200318081112670-5135fb.png)

  - 同样的再找一个`DelegatingApplicationContextInitializer`

    ![image-20200318081322781](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200318081322781-cb3b39.png)

- 下图中的所有类都有 Order 数值返回

  排序前:

![image-20200318081352639](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200318081352639-ea7dad.png)

排序后：

![image-20200318081458019](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200318081458019-dccda8.png)

### listeners.starting()

- `SpringApplicationRunListeners` : `org.springframework.boot.SpringApplicationRunListeners` 这个类是`org.springframework.boot.SpringApplicationRunListener`的集合表现形式

  ```
  class SpringApplicationRunListeners {
  
  	private final List<SpringApplicationRunListener> listeners;
  SpringApplicationRunListeners(Log log, Collection<? extends SpringApplicationRunListener> listeners) {
  		this.log = log;
  		this.listeners = new ArrayList<>(listeners);
  	}
  
  	void starting() {
  		for (SpringApplicationRunListener listener : this.listeners) {
  			listener.starting();
  		}
  	}
  
  }
  ```

  - 这里主要是启动`org.springframework.boot.SpringApplicationRunListener#starting`方法，只有一个实现`org.springframework.boot.context.event.EventPublishingRunListener#starting`

### prepareEnvironment

```java
	private ConfigurableEnvironment prepareEnvironment(SpringApplicationRunListeners listeners,
			ApplicationArguments applicationArguments) {
		// Create and configure the environment
		// 得到一个环境
		ConfigurableEnvironment environment = getOrCreateEnvironment();
		// 配置环境
		configureEnvironment(environment, applicationArguments.getSourceArgs());
		ConfigurationPropertySources.attach(environment);
		listeners.environmentPrepared(environment);
		// 绑定springBoot应用
		bindToSpringApplication(environment);
		// 是否创建自定义环境
		if (!this.isCustomEnvironment) {
			environment = new EnvironmentConverter(getClassLoader()).convertEnvironmentIfNecessary(environment,
					deduceEnvironmentClass());
		}
		ConfigurationPropertySources.attach(environment);
		return environment;
	}

```

### configureIgnoreBeanInfo

- 获取`spring.beaninfo.ignore`并且设置到环境信息中

```java
	private void configureIgnoreBeanInfo(ConfigurableEnvironment environment) {
		if (System.getProperty(CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME) == null) {
			Boolean ignore = environment.getProperty("spring.beaninfo.ignore", Boolean.class, Boolean.TRUE);
			System.setProperty(CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME, ignore.toString());
		}
	}
```

### printBanner

```java
	private Banner printBanner(ConfigurableEnvironment environment) {
		if (this.bannerMode == Banner.Mode.OFF) {
			return null;
		}
		ResourceLoader resourceLoader = (this.resourceLoader != null) ? this.resourceLoader
				: new DefaultResourceLoader(getClassLoader());
		// 创建打印器
		SpringApplicationBannerPrinter bannerPrinter = new SpringApplicationBannerPrinter(resourceLoader, this.banner);
		if (this.bannerMode == Mode.LOG) {
		    // 输出
			return bannerPrinter.print(environment, this.mainApplicationClass, logger);
		}
        // 输出
		return bannerPrinter.print(environment, this.mainApplicationClass, System.out);
	}

```

```
	Banner print(Environment environment, Class<?> sourceClass, PrintStream out) {
		Banner banner = getBanner(environment);
		banner.printBanner(environment, sourceClass, out);
		return new PrintedBanner(banner, sourceClass);
	}

```

- 最终输出内容类：`org.springframework.boot.SpringBootBanner`

  ```java
  class SpringBootBanner implements Banner {
  
  	private static final String[] BANNER = { "", "  .   ____          _            __ _ _",
  			" /\\\\ / ___'_ __ _ _(_)_ __  __ _ \\ \\ \\ \\", "( ( )\\___ | '_ | '_| | '_ \\/ _` | \\ \\ \\ \\",
  			" \\\\/  ___)| |_)| | | | | || (_| |  ) ) ) )", "  '  |____| .__|_| |_|_| |_\\__, | / / / /",
  			" =========|_|==============|___/=/_/_/_/" };
  
  	private static final String SPRING_BOOT = " :: Spring Boot :: ";
  
  	private static final int STRAP_LINE_SIZE = 42;
  
  	@Override
  	public void printBanner(Environment environment, Class<?> sourceClass, PrintStream printStream) {
  		for (String line : BANNER) {
  			printStream.println(line);
  		}
  		String version = SpringBootVersion.getVersion();
  		version = (version != null) ? " (v" + version + ")" : "";
  		StringBuilder padding = new StringBuilder();
  		while (padding.length() < STRAP_LINE_SIZE - (version.length() + SPRING_BOOT.length())) {
  			padding.append(" ");
  		}
  
  		printStream.println(AnsiOutput.toString(AnsiColor.GREEN, SPRING_BOOT, AnsiColor.DEFAULT, padding.toString(),
  				AnsiStyle.FAINT, version));
  		printStream.println();
  	}
  
  }
  ```

### createApplicationContext

```java
	protected ConfigurableApplicationContext createApplicationContext() {
	    // 获取上下文类
		Class<?> contextClass = this.applicationContextClass;
		if (contextClass == null) {
			try {
				// 根据不同类型选择创建的实例
				switch (this.webApplicationType) {
				case SERVLET:
					contextClass = Class.forName(DEFAULT_SERVLET_WEB_CONTEXT_CLASS);
					break;
				case REACTIVE:
					contextClass = Class.forName(DEFAULT_REACTIVE_WEB_CONTEXT_CLASS);
					break;
				default:
					contextClass = Class.forName(DEFAULT_CONTEXT_CLASS);
				}
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException(
						"Unable create a default ApplicationContext, please specify an ApplicationContextClass", ex);
			}
		}
		return (ConfigurableApplicationContext) BeanUtils.instantiateClass(contextClass);
	}

```

- `this.applicationContextClass` 初始化方法

```java
	public SpringApplication(ResourceLoader resourceLoader, Class<?>... primarySources) {
		this.resourceLoader = resourceLoader;
		Assert.notNull(primarySources, "PrimarySources must not be null");
		this.primarySources = new LinkedHashSet<>(Arrays.asList(primarySources));
		// 设置 web应用类型
		this.webApplicationType = WebApplicationType.deduceFromClasspath();
		setInitializers((Collection) getSpringFactoriesInstances(ApplicationContextInitializer.class));
		setListeners((Collection) getSpringFactoriesInstances(ApplicationListener.class));
		this.mainApplicationClass = deduceMainApplicationClass();
	}
```

- `org.springframework.boot.WebApplicationType#deduceFromClasspath`

```java
	static WebApplicationType deduceFromClasspath() {
		if (ClassUtils.isPresent(WEBFLUX_INDICATOR_CLASS, null) && !ClassUtils.isPresent(WEBMVC_INDICATOR_CLASS, null)
				&& !ClassUtils.isPresent(JERSEY_INDICATOR_CLASS, null)) {
			return WebApplicationType.REACTIVE;
		}
		for (String className : SERVLET_INDICATOR_CLASSES) {
			if (!ClassUtils.isPresent(className, null)) {
				return WebApplicationType.NONE;
			}
		}
		return WebApplicationType.SERVLET;
	}

```

### exceptionReporters

![image-20200318085243888](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200318085243888-02c96c.png)

### prepareContext

```java
	private void prepareContext(ConfigurableApplicationContext context, ConfigurableEnvironment environment,
			SpringApplicationRunListeners listeners, ApplicationArguments applicationArguments, Banner printedBanner) {
	    // 上下文中设置环境
		context.setEnvironment(environment);
		// 上下文处理
		postProcessApplicationContext(context);
		// 初始化
		applyInitializers(context);
		// 监听器中放入上下文
		listeners.contextPrepared(context);
		if (this.logStartupInfo) {
			logStartupInfo(context.getParent() == null);
			logStartupProfileInfo(context);
		}
		// Add boot specific singleton beans
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		// 单例对象注册
		beanFactory.registerSingleton("springApplicationArguments", applicationArguments);
		if (printedBanner != null) {
			beanFactory.registerSingleton("springBootBanner", printedBanner);
		}
		if (beanFactory instanceof DefaultListableBeanFactory) {
			((DefaultListableBeanFactory) beanFactory)
					.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
		}
		if (this.lazyInitialization) {
			context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor());
		}
		// Load the sources
		Set<Object> sources = getAllSources();
		Assert.notEmpty(sources, "Sources must not be empty");
		// 加载上下文
		load(context, sources.toArray(new Object[0]));
		// 监听器做加载上下文操作
		listeners.contextLoaded(context);
	}

```

- `set`方法就不说了

### postProcessApplicationContext

```java
	protected void postProcessApplicationContext(ConfigurableApplicationContext context) {
		if (this.beanNameGenerator != null) {
		    // 注册 beanName 的生成器
			context.getBeanFactory().registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR,
					this.beanNameGenerator);
		}
		if (this.resourceLoader != null) {
			if (context instanceof GenericApplicationContext) {
			    // 设置资源加载器
				((GenericApplicationContext) context).setResourceLoader(this.resourceLoader);
			}
			if (context instanceof DefaultResourceLoader) {
			    // 设置类加载器
				((DefaultResourceLoader) context).setClassLoader(this.resourceLoader.getClassLoader());
			}
		}
		if (this.addConversionService) {
		    // 转换服务
			context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance());
		}
	}

```

- 看一下最终设置完成后的 context

  ```java
  context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance());
  ```

![image-20200318090128983](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200318090128983-b53e3f.png)

![image-20200318090312626](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200318090312626-1e0e5f.png)

### applyInitializers

- 初始化应用上下文

```java
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void applyInitializers(ConfigurableApplicationContext context) {
		for (ApplicationContextInitializer initializer : getInitializers()) {
			Class<?> requiredType = GenericTypeResolver.resolveTypeArgument(initializer.getClass(),
					ApplicationContextInitializer.class);
			Assert.isInstanceOf(requiredType, context, "Unable to call initializer.");
			initializer.initialize(context);
		}
	}

```

- 初始化 `List<ApplicationListener<?>> listeners`: `setInitializers((Collection) getSpringFactoriesInstances(ApplicationContextInitializer.class));`

- 获取 `List<ApplicationListener<?>> listeners`: `public Set<ApplicationListener<?>> getListeners() { return asUnmodifiableOrderedSet(this.listeners);}`

- 数据结果

![image-20200318090935285](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200318090935285-ed8e80.png)

- 子类的具体实现不展开了

### getAllSources

```java
	public Set<Object> getAllSources() {
		Set<Object> allSources = new LinkedHashSet<>();
		if (!CollectionUtils.isEmpty(this.primarySources)) {
			allSources.addAll(this.primarySources);
		}
		if (!CollectionUtils.isEmpty(this.sources)) {
			allSources.addAll(this.sources);
		}
		return Collections.unmodifiableSet(allSources);
	}

```

- `primarySources` 就是我们的项目启动类，在`SpringApplication`的构造器中有`this.primarySources = new LinkedHashSet<>(Arrays.asList(primarySources))`

![image-20200318091558233](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200318091558233-45bfd1.png)

### load

- 加载 bean 到应用上下文

```java
	protected void load(ApplicationContext context, Object[] sources) {
		if (logger.isDebugEnabled()) {
			logger.debug("Loading source " + StringUtils.arrayToCommaDelimitedString(sources));
		}
		// bean定义加载器
		BeanDefinitionLoader loader = createBeanDefinitionLoader(getBeanDefinitionRegistry(context), sources);
		if (this.beanNameGenerator != null) {
		    // 设置 beanName生成器
			loader.setBeanNameGenerator(this.beanNameGenerator);
		}
		if (this.resourceLoader != null) {
		    // 设置 资源加载器
			loader.setResourceLoader(this.resourceLoader);
		}
		if (this.environment != null) {
		    // 设置环境
			loader.setEnvironment(this.environment);
		}
		// 加载
		loader.load();
	}

```

```java
	int load() {
		int count = 0;
		for (Object source : this.sources) {
			count += load(source);
		}
		return count;
	}

```

```java
private int load(Object source) {
		Assert.notNull(source, "Source must not be null");
		if (source instanceof Class<?>) {
			return load((Class<?>) source);
		}
		if (source instanceof Resource) {
			return load((Resource) source);
		}
		if (source instanceof Package) {
			return load((Package) source);
		}
		if (source instanceof CharSequence) {
			return load((CharSequence) source);
		}
		throw new IllegalArgumentException("Invalid source type " + source.getClass());
	}

```

- 通过前文我们已经知道 `source`就是一个 class

  ![image-20200318092027020](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200318092027020-4d0e41.png)

```java
	private int load(Class<?> source) {
		if (isGroovyPresent() && GroovyBeanDefinitionSource.class.isAssignableFrom(source)) {
			// Any GroovyLoaders added in beans{} DSL can contribute beans here
			GroovyBeanDefinitionSource loader = BeanUtils.instantiateClass(source, GroovyBeanDefinitionSource.class);
			load(loader);
		}
		// 是否为组件
		if (isComponent(source)) {
			this.annotatedReader.register(source);
			return 1;
		}
		return 0;
	}

```

- 我们的启动类是一个组件，直接注册完成返回 1

### listeners.contextLoaded(context)

- 监听器行为: 在上下文资源加载后做一些事情

### refreshContext

- 上下文刷新

```java
	private void refreshContext(ConfigurableApplicationContext context) {
		refresh(context);
		if (this.registerShutdownHook) {
			try {
				context.registerShutdownHook();
			}
			catch (AccessControlException ex) {
				// Not allowed in some environments.
			}
		}
	}

```

```java
	/**
	 * Refresh the underlying {@link ApplicationContext}.
	 * @param applicationContext the application context to refresh
	 */
	protected void refresh(ApplicationContext applicationContext) {
		Assert.isInstanceOf(AbstractApplicationContext.class, applicationContext);
		((AbstractApplicationContext) applicationContext).refresh();
	}
```

- 最终来到了`org.springframework.context.support.AbstractApplicationContext#refresh`方法，此方法是 spring 的一个方法，此处不在阐述

### afterRefresh

- 刷新上下文之后做的事情,空的没有实现

  ```
  	protected void afterRefresh(ConfigurableApplicationContext context, ApplicationArguments args) {
  	}
  
  ```

### stopWatch.stop()

- 秒表结束

### listeners.started(context)

- 各类监听器启动

### callRunners

- 两种 runner 启动`ApplicationRunner` 和 `CommandLineRunner`

```java
	private void callRunners(ApplicationContext context, ApplicationArguments args) {
		List<Object> runners = new ArrayList<>();
		runners.addAll(context.getBeansOfType(ApplicationRunner.class).values());
		runners.addAll(context.getBeansOfType(CommandLineRunner.class).values());
		AnnotationAwareOrderComparator.sort(runners);
		for (Object runner : new LinkedHashSet<>(runners)) {
			if (runner instanceof ApplicationRunner) {
				callRunner((ApplicationRunner) runner, args);
			}
			if (runner instanceof CommandLineRunner) {
				callRunner((CommandLineRunner) runner, args);
			}
		}
	}

```

```java
private void callRunner(ApplicationRunner runner, ApplicationArguments args) {
		try {
			(runner).run(args);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to execute ApplicationRunner", ex);
		}
	}
```

### listeners.running(context)

- 监听器正式开始工作

# Spring Boot 自动装配

- Author: [HuiFer](https://github.com/huifer)
- 源码阅读仓库: [SourceHot-spring-boot](https://github.com/SourceHot/spring-boot-read)

- `org.springframework.boot.autoconfigure.SpringBootApplication`

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(excludeFilters = { @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
		@Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
public @interface SpringBootApplication {

	@AliasFor(annotation = EnableAutoConfiguration.class)
	Class<?>[] exclude() default {};

	@AliasFor(annotation = EnableAutoConfiguration.class)
	String[] excludeName() default {};

	@AliasFor(annotation = ComponentScan.class, attribute = "basePackages")
	String[] scanBasePackages() default {};

	@AliasFor(annotation = ComponentScan.class, attribute = "basePackageClasses")
	Class<?>[] scanBasePackageClasses() default {};

	@AliasFor(annotation = Configuration.class)
	boolean proxyBeanMethods() default true;

}

```

## EnableAutoConfiguration

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigurationPackage
@Import(AutoConfigurationImportSelector.class)
public @interface EnableAutoConfiguration {

}
```

## AutoConfigurationImportSelector

- 类图

![image-20200320150642022](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200320150642022-74fd2a.png)

## getAutoConfigurationMetadata()

```java
		@Override
		public void process(AnnotationMetadata annotationMetadata, DeferredImportSelector deferredImportSelector) {
			Assert.state(deferredImportSelector instanceof AutoConfigurationImportSelector,
					() -> String.format("Only %s implementations are supported, got %s",
							AutoConfigurationImportSelector.class.getSimpleName(),
							deferredImportSelector.getClass().getName()));
			AutoConfigurationEntry autoConfigurationEntry = ((AutoConfigurationImportSelector) deferredImportSelector)
					.getAutoConfigurationEntry(
							// 加载配置元数据
							getAutoConfigurationMetadata(), annotationMetadata);
			this.autoConfigurationEntries.add(autoConfigurationEntry);
			for (String importClassName : autoConfigurationEntry.getConfigurations()) {
				this.entries.putIfAbsent(importClassName, annotationMetadata);
			}
		}


		private AutoConfigurationMetadata getAutoConfigurationMetadata() {
			if (this.autoConfigurationMetadata == null) {
				// 加载配置信息
				this.autoConfigurationMetadata = AutoConfigurationMetadataLoader.loadMetadata(this.beanClassLoader);
			}
			return this.autoConfigurationMetadata;
		}

```

- `org.springframework.boot.autoconfigure.AutoConfigurationMetadataLoader#loadMetadata(java.lang.ClassLoader)`

  ```java
  	static AutoConfigurationMetadata loadMetadata(ClassLoader classLoader, String path) {
  		try {
  
  		    // 获取资源路径
  			Enumeration<URL> urls = (classLoader != null) ? classLoader.getResources(path)
  					: ClassLoader.getSystemResources(path);
  			Properties properties = new Properties();
  			while (urls.hasMoreElements()) {
  				properties.putAll(PropertiesLoaderUtils.loadProperties(new UrlResource(urls.nextElement())));
  			}
  			return loadMetadata(properties);
  		}
  		catch (IOException ex) {
  			throw new IllegalArgumentException("Unable to load @ConditionalOnClass location [" + path + "]", ex);
  		}
  	}
  
  ```

  ![image-20200320160423991](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200320160423991-cb93f3.png)

- `protected static final String PATH = "META-INF/spring-autoconfigure-metadata.properties";`

  注意： 这个文件在**target**编译后的文件夹中

  相关 Issues : https://github.com/spring-projects/spring-boot/issues/11282

- 自动装配

  `spring-boot-project/spring-boot-autoconfigure/src/main/resources/META-INF/spring.factories`

  该文件内存有:

  ```
  # Auto Configure
  org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  org.springframework.boot.autoconfigure.admin.SpringApplicationAdminJmxAutoConfiguration,\
  org.springframework.boot.autoconfigure.aop.AopAutoConfiguration,\
  org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,\
  org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration,\
  org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration,\
  ```

![image-20200320162835665](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200320162835665-757656.png)

同样找一下 redis

![image-20200320163001728](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200320163001728-94dadb.png)

- 仔细看`org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration`类

先说注解

```java
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RedisOperations.class)
@EnableConfigurationProperties(RedisProperties.class)
@Import({ LettuceConnectionConfiguration.class, JedisConnectionConfiguration.class })
```

### EnableConfigurationProperties

`自动映射一个POJO到Spring Boot配置文件（默认是application.properties文件）的属性集。`

- `org.springframework.boot.autoconfigure.data.redis.RedisProperties`
- 部分 redis 配置属性

```java
@ConfigurationProperties(prefix = "spring.redis")
public class RedisProperties {

	/**
	 * Database index used by the connection factory.
	 */
	private int database = 0;

	/**
	 * Connection URL. Overrides host, port, and password. User is ignored. Example:
	 * redis://user:password@example.com:6379
	 */
	private String url;

	/**
	 * Redis server host.
	 */
	private String host = "localhost";

	/**
	 * Login password of the redis server.
	 */
	private String password;

	/**
	 * Redis server port.
	 */
	private int port = 6379;

	/**
	 * Whether to enable SSL support.
	 */
	private boolean ssl;

	/**
	 * Connection timeout.
	 */
	private Duration timeout;

	/**
	 * Client name to be set on connections with CLIENT SETNAME.
	 */
	private String clientName;



}
```

- 找到一个我们用相同方式去寻找到别的一些属性处理如`org.springframework.boot.autoconfigure.jdbc.JdbcProperties` 具体展开请各位读者自行了解了

### AnnotationMetadata

回过头继续我们的主要流程

- `org.springframework.boot.autoconfigure.AutoConfigurationImportSelector.AutoConfigurationGroup#process`

  ![image-20200320163806852](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200320163806852-e587b4.png)

再此之前我们看过了`getAutoConfigurationMetadata()`的相关操作

关注 `AnnotationMetadata annotationMetadata` 存储了一些什么

![image-20200320164145286](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200320164145286-c55a17.png)

这里简单理解

1.  mergedAnnotations 类相关的注解信息
2.  annotationTypes 在启动类上的注解列表

### getAutoConfigurationEntry

```java
	protected AutoConfigurationEntry getAutoConfigurationEntry(AutoConfigurationMetadata autoConfigurationMetadata,
			AnnotationMetadata annotationMetadata) {
		if (!isEnabled(annotationMetadata)) {
			return EMPTY_ENTRY;
		}
		// 获取注解属性值
		AnnotationAttributes attributes = getAttributes(annotationMetadata);
		// 获取候选配置信息
		List<String> configurations = getCandidateConfigurations(annotationMetadata, attributes);
		// 删除重复配置
		configurations = removeDuplicates(configurations);
		// 获取 exclude 属性
		Set<String> exclusions = getExclusions(annotationMetadata, attributes);
		// 校验 exclude 类
		checkExcludedClasses(configurations, exclusions);
		// 配置中删除 exclude 的属性值
		configurations.removeAll(exclusions);
		// 过滤
		configurations = filter(configurations, autoConfigurationMetadata);
		// 触发自动配置事件
		fireAutoConfigurationImportEvents(configurations, exclusions);
		// 返回
		return new AutoConfigurationEntry(configurations, exclusions);
	}

```

### getAttributes

```java
	protected AnnotationAttributes getAttributes(AnnotationMetadata metadata) {
	    // name = org.springframework.boot.autoconfigure.EnableAutoConfiguration , 这是一个固定的值
		String name = getAnnotationClass().getName();
		// 获取注解的属性
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(name, true));
		Assert.notNull(attributes, () -> "No auto-configuration attributes found. Is " + metadata.getClassName()
				+ " annotated with " + ClassUtils.getShortName(name) + "?");
		return attributes;
	}

```

![image-20200320171138431](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200320171138431-73704e.png)

### getCandidateConfigurations

- 读取`spring.factories`数据

```java
	protected List<String> getCandidateConfigurations(AnnotationMetadata metadata, AnnotationAttributes attributes) {
	    // 读取 org.springframework.boot.autoconfigure.EnableAutoConfiguration 相关配置
		List<String> configurations = SpringFactoriesLoader.loadFactoryNames(getSpringFactoriesLoaderFactoryClass(),
				getBeanClassLoader());
		Assert.notEmpty(configurations, "No auto configuration classes found in META-INF/spring.factories. If you "
				+ "are using a custom packaging, make sure that file is correct.");
		return configurations;
	}

```

![image-20200320171734270](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200320171734270-ea919a.png)

- 第一个是我自己写的一个测试用

  ```properties
  org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
    org.sourcehot.service.HelloServiceAutoConfiguration
  ```

### removeDuplicates

- new 两个对象直接做数据转换，去重

```java
	protected final <T> List<T> removeDuplicates(List<T> list) {
		return new ArrayList<>(new LinkedHashSet<>(list));
	}

```

### getExclusions

```java
	protected Set<String> getExclusions(AnnotationMetadata metadata, AnnotationAttributes attributes) {
		Set<String> excluded = new LinkedHashSet<>();
		// 获取属性 exclude 值转换成list
		excluded.addAll(asList(attributes, "exclude"));
      // 获取属性 excludeName 值转换成list
		excluded.addAll(Arrays.asList(attributes.getStringArray("excludeName")));
		// 获取 SpringBoot 本身的忽略配置属性
		excluded.addAll(getExcludeAutoConfigurationsProperty());
		return excluded;
	}

```

### getExcludeAutoConfigurationsProperty

```java
	private List<String> getExcludeAutoConfigurationsProperty() {
		if (getEnvironment() instanceof ConfigurableEnvironment) {
			Binder binder = Binder.get(getEnvironment());
			// 取出 "spring.autoconfigure.exclude" 转换成list
			return binder.bind(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, String[].class).map(Arrays::asList)
					.orElse(Collections.emptyList());
		}
		String[] excludes = getEnvironment().getProperty(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, String[].class);
		return (excludes != null) ? Arrays.asList(excludes) : Collections.emptyList();
	}

```

![image-20200323080611527](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323080611527-2c2496.png)

- 修改启动类

  ```java
  @SpringBootApplication(excludeName =  { "org.sourcehot.service.HelloServiceAutoConfiguration" })
  
  ```

  ![image-20200323081009823](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323081009823-ed3af2.png)

### checkExcludedClasses

```java
	private void checkExcludedClasses(List<String> configurations, Set<String> exclusions) {
		List<String> invalidExcludes = new ArrayList<>(exclusions.size());
		for (String exclusion : exclusions) {
			//
			if (ClassUtils.isPresent(exclusion, getClass().getClassLoader()) && !configurations.contains(exclusion)) {
				invalidExcludes.add(exclusion);
			}
		}
		if (!invalidExcludes.isEmpty()) {
			// 处理忽略的类
			handleInvalidExcludes(invalidExcludes);
		}
	}

```

- `configurations.removeAll(exclusions)`

  移除忽略的类

### filter

```java
	private List<String> filter(List<String> configurations, AutoConfigurationMetadata autoConfigurationMetadata) {
		long startTime = System.nanoTime();
		String[] candidates = StringUtils.toStringArray(configurations);
		boolean[] skip = new boolean[candidates.length];
		boolean skipped = false;
		// 获取 AutoConfigurationImportFilter 相关配置
		for (AutoConfigurationImportFilter filter : getAutoConfigurationImportFilters()) {
		    // 执行 aware 相关接口
			invokeAwareMethods(filter);
			// 比较
			boolean[] match = filter.match(candidates, autoConfigurationMetadata);
			for (int i = 0; i < match.length; i++) {
				if (!match[i]) {
					skip[i] = true;
					candidates[i] = null;
					skipped = true;
				}
			}
		}
		if (!skipped) {
			return configurations;
		}
		List<String> result = new ArrayList<>(candidates.length);
		for (int i = 0; i < candidates.length; i++) {
			if (!skip[i]) {
				result.add(candidates[i]);
			}
		}
		if (logger.isTraceEnabled()) {
			int numberFiltered = configurations.size() - result.size();
			logger.trace("Filtered " + numberFiltered + " auto configuration class in "
					+ TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + " ms");
		}
		return new ArrayList<>(result);
	}

```

- `getAutoConfigurationImportFilters()` 从`spring.factories` 获取 `AutoConfigurationImportFilter`的接口

![image-20200323081903145](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323081903145-0c4e83.png)

- 循环内执行`Aware`系列接口

`match`方法: `org.springframework.boot.autoconfigure.AutoConfigurationImportFilter#match`

- `filter.match(candidates, autoConfigurationMetadata)` 比较判断哪些是需要自动注入的类

![image-20200323082553595](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323082553595-00cd5b.png)

### fireAutoConfigurationImportEvents

```java
	private void fireAutoConfigurationImportEvents(List<String> configurations, Set<String> exclusions) {
		// 获取自动配置的监听器列表
		List<AutoConfigurationImportListener> listeners = getAutoConfigurationImportListeners();
		if (!listeners.isEmpty()) {
			// 创建 自动配置事件
			AutoConfigurationImportEvent event = new AutoConfigurationImportEvent(this, configurations, exclusions);
			for (AutoConfigurationImportListener listener : listeners) {
				// 执行 Aware 相关接口
				invokeAwareMethods(listener);
				// 监听器执行自动配置事件
				listener.onAutoConfigurationImportEvent(event);
			}
		}
	}

```

![image-20200323083149737](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323083149737-c2f7eb.png)

- `AutoConfigurationImportEvent event = new AutoConfigurationImportEvent(this, configurations, exclusions);`

![image-20200323083247061](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323083247061-34d802.png)

- `org.springframework.boot.autoconfigure.AutoConfigurationImportListener#onAutoConfigurationImportEvent` 在执行自动配置时触发 , 实现类只有 **`ConditionEvaluationReportAutoConfigurationImportListener`**

  ```java
  	@Override
  	public void onAutoConfigurationImportEvent(AutoConfigurationImportEvent event) {
  		if (this.beanFactory != null) {
  			ConditionEvaluationReport report = ConditionEvaluationReport.get(this.beanFactory);
  			// 记录需要加载的配置
  			report.recordEvaluationCandidates(event.getCandidateConfigurations());
  			// 记录不需要加载的配置
  			report.recordExclusions(event.getExclusions());
  		}
  	}
  
  ```

![image-20200323083656670](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323083656670-392944.png)

- 初始化完

## process

- `org.springframework.boot.autoconfigure.AutoConfigurationImportSelector.AutoConfigurationGroup#process`

![image-20200323084922159](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20200323084922159-7a5bbd.png)

- 后续的一些行为相对简单，直接放个源码了.

```java
@Override
		public void process(AnnotationMetadata annotationMetadata, DeferredImportSelector deferredImportSelector) {
			Assert.state(deferredImportSelector instanceof AutoConfigurationImportSelector,
					() -> String.format("Only %s implementations are supported, got %s",
							AutoConfigurationImportSelector.class.getSimpleName(),
							deferredImportSelector.getClass().getName()));
			// 自动装配信息
			AutoConfigurationEntry autoConfigurationEntry = ((AutoConfigurationImportSelector) deferredImportSelector)
					.getAutoConfigurationEntry(
							// 加载配置元数据
							getAutoConfigurationMetadata(), annotationMetadata);
			this.autoConfigurationEntries.add(autoConfigurationEntry);
			// 循环需要自动注入的类
			for (String importClassName : autoConfigurationEntry.getConfigurations()) {
				// 继续放入k,v
				this.entries.putIfAbsent(importClassName, annotationMetadata);
			}
		}
```

## selectImports

```java
		@Override
		public Iterable<Entry> selectImports() {
			if (this.autoConfigurationEntries.isEmpty()) {
				return Collections.emptyList();
			}
			// 获取忽略的类
			Set<String> allExclusions = this.autoConfigurationEntries.stream()
					.map(AutoConfigurationEntry::getExclusions).flatMap(Collection::stream).collect(Collectors.toSet());

			// 获取需要注入的类
			Set<String> processedConfigurations = this.autoConfigurationEntries.stream()
					.map(AutoConfigurationEntry::getConfigurations).flatMap(Collection::stream)
					.collect(Collectors.toCollection(LinkedHashSet::new));

			// 把不需要自动注入的类从需要注入的类中移除
			processedConfigurations.removeAll(allExclusions);

			// 排序
			return sortAutoConfigurations(processedConfigurations, getAutoConfigurationMetadata()).stream()
					.map((importClassName) -> new Entry(this.entries.get(importClassName), importClassName))
					.collect(Collectors.toList());
		}

```

后续由 spring 进行不再继续跟踪