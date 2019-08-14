# Nepxion Discovery Automcation Test
[![Total lines](https://tokei.rs/b1/github/Nepxion/Discovery?category=lines)](https://tokei.rs/b1/github/Nepxion/Discovery?category=lines)  [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?label=license)](https://github.com/Nepxion/Discovery/blob/master/LICENSE)  [![Maven Central](https://img.shields.io/maven-central/v/com.nepxion/discovery.svg?label=maven%20central)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.nepxion%22%20AND%20discovery)  [![Javadocs](http://www.javadoc.io/badge/com.nepxion/discovery-plugin-framework.svg)](http://www.javadoc.io/doc/com.nepxion/discovery-plugin-framework)  [![Build Status](https://travis-ci.org/Nepxion/Discovery.svg?branch=master)](https://travis-ci.org/Nepxion/Discovery)  [![Codacy Badge](https://api.codacy.com/project/badge/Grade/8e39a24e1be740c58b83fb81763ba317)](https://www.codacy.com/project/HaojunRen/Discovery/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Nepxion/Discovery&amp;utm_campaign=Badge_Grade_Dashboard)

路过的同学，如果您觉得这个开源框架不错，顺手给它点个Star吧

![Alt text](https://github.com/Nepxion/Docs/raw/master/discovery-doc/Star2.jpg)

Nepxion Discovery Automcation Test是一款基于Spring Boot/Spring Cloud自动化测试，包括普通调用测试和灰度调用测试插件。通过注解形式，跟Spring Boot内置的测试机制集成，使用简单方便

## 请联系我
微信和公众号

![Alt text](https://github.com/Nepxion/Docs/raw/master/zxing-doc/微信-1.jpg)![Alt text](https://github.com/Nepxion/Docs/raw/master/zxing-doc/公众号-1.jpg)

## 测试用例

自动化测试代码参考[https://github.com/Nepxion/DiscoveryGray/tree/master/discovery-gray-test](https://github.com/Nepxion/DiscoveryGray/tree/master/discovery-gray-test)

- 自动化测试场景以API网关是测试的触发点，全链路如下：

```xml
API网关 -> 服务A（两个实例） -> 服务B（两个实例）
```

- 各个实例部署情况如下：

| 类名 | 微服务 | 服务端口 | 版本 | 区域 |
| --- | --- | --- | --- | --- |
| DiscoveryGrayServiceA1.java | A1 | 3001 | 1.0 | dev |
| DiscoveryGrayServiceA2.java | A2 | 3002 | 1.1 | qa |
| DiscoveryGrayServiceB1.java | B1 | 4001 | 1.0 | qa |
| DiscoveryGrayServiceB2.java | B2 | 4002 | 1.1 | dev |
| DiscoveryGrayGateway.java | Gateway | 5001 | 1.0 | 无 |
| DiscoveryGrayZuul.java | Zuul | 5002 | 1.0 | 无 |

### 普通调用测试
在测试方法上面增加注解@DTest，通过断言Assert来判断测试结果

代码如下：

```java
public class MyTestCases {
    @Autowired
    private TestRestTemplate testRestTemplate;

    @DTest
    public void testNoGray(String testUrl) {
        int noRepeatCount = 0;
        List<String> resultList = new ArrayList<String>();
        for (int i = 0; i < 4; i++) {
            String result = testRestTemplate.getForEntity(testUrl, String.class).getBody();

            LOG.info("Result{} : {}", i + 1, result);

            if (!resultList.contains(result)) {
                noRepeatCount++;
            }
            resultList.add(result);
        }

        Assert.assertEquals(noRepeatCount, 4);
    }
}
```

### 灰度调用测试
在测试方法上面增加注解@DTestGray，通过断言Assert来判断测试结果。注解@DTestGray包含三个参数：
1. group - 被测试服务所在的组
2. serviceId - 被测试服务的服务名
3. path - 灰度配置文件的路径，示例为xml格式

代码如下：
```java
public class MyTestCases {
    @Autowired
    private TestRestTemplate testRestTemplate;

    @DTestGray(group = "#group", serviceId = "#serviceId", path = "test-config-strategy-version-1.xml")
    public void testVersionStrategyGray(String group, String serviceId, String testUrl) {
        for (int i = 0; i < 4; i++) {
            String result = testRestTemplate.getForEntity(testUrl, String.class).getBody();

            LOG.info("Result{} : {}", i + 1, result);

            int index = result.indexOf("[V=1.0]");
            int lastIndex = result.lastIndexOf("[V=1.0]");

            Assert.assertNotEquals(index, -1);
            Assert.assertNotEquals(lastIndex, -1);
            Assert.assertNotEquals(index, lastIndex);
        }
    }
}
```

灰度配置文件test-config-version-1.xml的内容如下：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<rule>
    <strategy>
        <version>1.0</version>
    </strategy>
</rule>
```

灰度测试的注解支持Spel语法格式，即group = "#group", serviceId = "#serviceId"。当使用者希望用这种方式的时候，需要在IDE和Maven里设置"-parameters"的Compiler Argument：
1. IDE设置
   - Eclipse加"-parameters"参数：https://www.concretepage.com/java/jdk-8/java-8-reflection-access-to-parameter-names-of-method-and-constructor-with-maven-gradle-and-eclipse-using-parameters-compiler-argument
   - Idea加"-parameters"参数：http://blog.csdn.net/royal_lr/article/details/52279993
2. Maven设置
```xml
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <compilerArgs>
                        <arg>-parameters</arg>
                    </compilerArgs>
                    <encoding>${project.build.sourceEncoding}</encoding>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

## 配置文件

```xml
# 测试用例类的扫描路径
spring.application.test.scan.packages=com.nepxion.discovery.gray.test
# 测试用例的灰度配置推送到远程配置中心，还是到服务。缺失则默认为true
spring.application.test.gray.configcenter.enabled=true
# 测试用例的灰度配置清除时，Key保留同时内容为空（reset），还是直接删除Key（clear）。缺失则默认为true
spring.application.test.gray.reset.enabled=true
# 测试用例的灰度配置推送后，等待生效的时间。缺失则默认为1000
spring.application.test.gray.await.time=1000
# 测试用例的灰度配置推送的控制台地支
spring.application.test.console.url=http://localhost:2222/

gateway.group=discovery-gray-group
gateway.service.id=discovery-gray-gateway
gateway.test.url=http://localhost:5001/discovery-gray-service-a/invoke/gateway

zuul.group=discovery-gray-group
zuul.service.id=discovery-gray-zuul
zuul.test.url=http://localhost:5002/discovery-gray-service-a/invoke/zuul
```

## 启动灰度控制台

运行[https://github.com/Nepxion/Discovery](https://github.com/Nepxion/Discovery)下discovery-springcloud-example-console的应用程序

## 测试结果

```xml
---------- Run automation testcase :: testNoGray() ----------
Result1 : gateway -> discovery-gray-service-a[192.168.0.107:3001][V=1.0][R=dev][G=discovery-gray-group] -> discovery-gray-service-b[192.168.0.107:4001][V=1.0][R=qa][G=discovery-gray-group]
Result2 : gateway -> discovery-gray-service-a[192.168.0.107:3002][V=1.1][R=qa][G=discovery-gray-group] -> discovery-gray-service-b[192.168.0.107:4001][V=1.0][R=qa][G=discovery-gray-group]
Result3 : gateway -> discovery-gray-service-a[192.168.0.107:3001][V=1.0][R=dev][G=discovery-gray-group] -> discovery-gray-service-b[192.168.0.107:4002][V=1.1][R=dev][G=discovery-gray-group]
Result4 : gateway -> discovery-gray-service-a[192.168.0.107:3002][V=1.1][R=qa][G=discovery-gray-group] -> discovery-gray-service-b[192.168.0.107:4002][V=1.1][R=dev][G=discovery-gray-group]
* Passed
---------- Run automation testcase :: testVersionStrategyGray() ----------
Result1 : gateway -> discovery-gray-service-a[192.168.0.107:3001][V=1.0][R=dev][G=discovery-gray-group] -> discovery-gray-service-b[192.168.0.107:4001][V=1.0][R=qa][G=discovery-gray-group]
Result2 : gateway -> discovery-gray-service-a[192.168.0.107:3001][V=1.0][R=dev][G=discovery-gray-group] -> discovery-gray-service-b[192.168.0.107:4001][V=1.0][R=qa][G=discovery-gray-group]
Result3 : gateway -> discovery-gray-service-a[192.168.0.107:3001][V=1.0][R=dev][G=discovery-gray-group] -> discovery-gray-service-b[192.168.0.107:4001][V=1.0][R=qa][G=discovery-gray-group]
Result4 : gateway -> discovery-gray-service-a[192.168.0.107:3001][V=1.0][R=dev][G=discovery-gray-group] -> discovery-gray-service-b[192.168.0.107:4001][V=1.0][R=qa][G=discovery-gray-group]
* Passed
---------- Run automation testcase :: testRegionStrategyGray() ----------
Result1 : gateway -> discovery-gray-service-a[192.168.0.107:3001][V=1.0][R=dev][G=discovery-gray-group] -> discovery-gray-service-b[192.168.0.107:4002][V=1.1][R=dev][G=discovery-gray-group]
Result2 : gateway -> discovery-gray-service-a[192.168.0.107:3001][V=1.0][R=dev][G=discovery-gray-group] -> discovery-gray-service-b[192.168.0.107:4002][V=1.1][R=dev][G=discovery-gray-group]
Result3 : gateway -> discovery-gray-service-a[192.168.0.107:3001][V=1.0][R=dev][G=discovery-gray-group] -> discovery-gray-service-b[192.168.0.107:4002][V=1.1][R=dev][G=discovery-gray-group]
Result4 : gateway -> discovery-gray-service-a[192.168.0.107:3001][V=1.0][R=dev][G=discovery-gray-group] -> discovery-gray-service-b[192.168.0.107:4002][V=1.1][R=dev][G=discovery-gray-group]
* Passed
---------- Run automation testcase :: testVersionWeightStrategyGray() ----------
Total count=3000
A service desired : 1.0 version weight=90%, 1.1 version weight=10%
B service desired : 1.0 version weight=20%, 1.1 version weight=80%
Weight offset desired=2%
Result : A service 1.0 version weight=89.6%
Result : A service 1.1 version weight=10.4%
Result : B service 1.0 version weight=20.1333%
Result : B service 1.1 version weight=79.8667%
* Passed
---------- Run automation testcase :: testRegionWeightStrategyGray() ----------
Total count=3000
A service desired : dev region weight=95%, qa region weight=5%
B service desired : dev region weight=15%, qa region weight=85%
Weight offset desired=2%
Result : A service dev region weight=95.4333%
Result : A service qa region weight=4.5667%
Result : B service dev region weight=14.3667%
Result : B service qa region weight=85.6333%
* Passed
---------- Run automation testcase :: testStrategyCustomizationGray() ----------
Header : [a:"1", b:"2"]
Result1 : gateway -> discovery-gray-service-a[192.168.0.107:3002][V=1.1][R=qa][G=discovery-gray-group] -> discovery-gray-service-b[192.168.0.107:4002][V=1.1][R=dev][G=discovery-gray-group]
Result2 : gateway -> discovery-gray-service-a[192.168.0.107:3002][V=1.1][R=qa][G=discovery-gray-group] -> discovery-gray-service-b[192.168.0.107:4002][V=1.1][R=dev][G=discovery-gray-group]
Result3 : gateway -> discovery-gray-service-a[192.168.0.107:3002][V=1.1][R=qa][G=discovery-gray-group] -> discovery-gray-service-b[192.168.0.107:4002][V=1.1][R=dev][G=discovery-gray-group]
Result4 : gateway -> discovery-gray-service-a[192.168.0.107:3002][V=1.1][R=qa][G=discovery-gray-group] -> discovery-gray-service-b[192.168.0.107:4002][V=1.1][R=dev][G=discovery-gray-group]
* Passed
```

## Star走势图

[![Stargazers over time](https://starchart.cc/Nepxion/Discovery.svg)](https://starchart.cc/Nepxion/Discovery)