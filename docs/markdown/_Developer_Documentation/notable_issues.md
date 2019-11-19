# Notable Issues

This page is intended to be a guide to issues that we have seen while developing and running Chaos Engine, including how to identify, and how to resolve them. These may be persistent issues if not configured, or bugs that can be created from improper development/packaging.

## SpringBoot Illegal State Exception / Class Not Found

If a Bean Instantiation attempts to load a class that is not in the classpath, then a IllegalStateException is thrown, wrapping a lower ClassNotFoundException.This can happen if a dependency is not packaged in one of the built JARs, and also is not copied into the loaded classpath folders as part of the build process.

### Example

```
java.lang.IllegalStateException: Error processing condition on org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration.propertySourcesPlaceholderConfigurer
    at org.springframework.boot.autoconfigure.condition.SpringBootCondition.matches(SpringBootCondition.java:64)
    at org.springframework.context.annotation.ConditionEvaluator.shouldSkip(ConditionEvaluator.java:108)
    at org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader.loadBeanDefinitionsForBeanMethod(ConfigurationClassBeanDefinitionReader.java:181)
    at org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader.loadBeanDefinitionsForConfigurationClass(ConfigurationClassBeanDefinitionReader.java:141)
    at org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader.loadBeanDefinitions(ConfigurationClassBeanDefinitionReader.java:117)
    at org.springframework.context.annotation.ConfigurationClassPostProcessor.processConfigBeanDefinitions(ConfigurationClassPostProcessor.java:327)
    at org.springframework.context.annotation.ConfigurationClassPostProcessor.postProcessBeanDefinitionRegistry(ConfigurationClassPostProcessor.java:232)
    at org.springframework.context.support.PostProcessorRegistrationDelegate.invokeBeanDefinitionRegistryPostProcessors(PostProcessorRegistrationDelegate.java:275)
    at org.springframework.context.support.PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(PostProcessorRegistrationDelegate.java:95)
    at org.springframework.context.support.AbstractApplicationContext.invokeBeanFactoryPostProcessors(AbstractApplicationContext.java:705)
    at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:531)
    at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:142)
    at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:775)
    at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:397)
    at org.springframework.boot.SpringApplication.run(SpringApplication.java:316)
    at com.gemalto.chaos.ChaosEngine.main(ChaosEngine.java:17)
    at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
    at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
    at java.base/java.lang.reflect.Method.invoke(Method.java:566)
    at org.springframework.boot.loader.MainMethodRunner.run(MainMethodRunner.java:48)
    at org.springframework.boot.loader.Launcher.launch(Launcher.java:87)
    at org.springframework.boot.loader.Launcher.launch(Launcher.java:50)
    at org.springframework.boot.loader.PropertiesLauncher.main(PropertiesLauncher.java:593)
    Caused by: java.lang.IllegalStateException: Failed to introspect Class [com.gemalto.chaos.notification.services.DataDogNotificationService] from ClassLoader [org.springframework.boot.loader.LaunchedURLClassLoader@63c12fb0]
    at org.springframework.util.ReflectionUtils.getDeclaredMethods(ReflectionUtils.java:507)
    at org.springframework.util.ReflectionUtils.doWithMethods(ReflectionUtils.java:404)
    at org.springframework.util.ReflectionUtils.doWithMethods(ReflectionUtils.java:389)
    at org.springframework.util.ReflectionUtils.getUniqueDeclaredMethods(ReflectionUtils.java:447)
    at java.base/java.util.concurrent.ConcurrentHashMap.computeIfAbsent(ConcurrentHashMap.java:1705)
    at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.getTypeForFactoryMethod(AbstractAutowireCapableBeanFactory.java:738)
    at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.determineTargetType(AbstractAutowireCapableBeanFactory.java:679)
    at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.predictBeanType(AbstractAutowireCapableBeanFactory.java:647)
    at org.springframework.beans.factory.support.AbstractBeanFactory.isFactoryBean(AbstractBeanFactory.java:1518)
    at org.springframework.beans.factory.support.AbstractBeanFactory.isFactoryBean(AbstractBeanFactory.java:1023)
    at org.springframework.boot.autoconfigure.condition.BeanTypeRegistry.addBeanTypeForNonAliasDefinition(BeanTypeRegistry.java:195)
    at org.springframework.boot.autoconfigure.condition.BeanTypeRegistry.addBeanTypeForNonAliasDefinition(BeanTypeRegistry.java:159)
    at org.springframework.boot.autoconfigure.condition.BeanTypeRegistry.addBeanType(BeanTypeRegistry.java:152)
    at org.springframework.boot.autoconfigure.condition.BeanTypeRegistry.updateTypesIfNecessary(BeanTypeRegistry.java:140)
    at java.base/java.util.Iterator.forEachRemaining(Iterator.java:133)
    at org.springframework.boot.autoconfigure.condition.BeanTypeRegistry.updateTypesIfNecessary(BeanTypeRegistry.java:135)
    at org.springframework.boot.autoconfigure.condition.BeanTypeRegistry.getNamesForType(BeanTypeRegistry.java:97)
    at org.springframework.boot.autoconfigure.condition.OnBeanCondition.collectBeanNamesForType(OnBeanCondition.java:298)
    at org.springframework.boot.autoconfigure.condition.OnBeanCondition.getBeanNamesForType(OnBeanCondition.java:289)
    at org.springframework.boot.autoconfigure.condition.OnBeanCondition.getBeanNamesForType(OnBeanCondition.java:278)
    at org.springframework.boot.autoconfigure.condition.OnBeanCondition.getMatchingBeans(OnBeanCondition.java:189)
    at org.springframework.boot.autoconfigure.condition.OnBeanCondition.getMatchOutcome(OnBeanCondition.java:160)
    at org.springframework.boot.autoconfigure.condition.SpringBootCondition.matches(SpringBootCondition.java:47)
    ... 23 common frames omitted
    Caused by: java.lang.NoClassDefFoundError: com/timgroup/statsd/StatsDClient
    at java.base/java.lang.Class.getDeclaredMethods0(Native Method)
    at java.base/java.lang.Class.privateGetDeclaredMethods(Class.java:3166)
    at java.base/java.lang.Class.getDeclaredMethods(Class.java:2309)
    at org.springframework.util.ReflectionUtils.getDeclaredMethods(ReflectionUtils.java:489)
    ... 45 common frames omitted
    Caused by: java.lang.ClassNotFoundException: com.timgroup.statsd.StatsDClient
    at java.base/java.net.URLClassLoader.findClass(URLClassLoader.java:471)
    at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:588)
    at org.springframework.boot.loader.LaunchedURLClassLoader.loadClass(LaunchedURLClassLoader.java:93)
    at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:521)
    ... 49 common frames omitted
```

### Common Resolution

Review the Build instructions for the class attempting to load this class. It either needs to be configured to package dependencies with itself, or to copy dependent Jar\'s in such a way that they are packaged in the final image.

#### Check List:

1.  The module has build instructions to include dependencies either in itself or in a relative path.
2.  Relative path dependencies are copied into the final image
