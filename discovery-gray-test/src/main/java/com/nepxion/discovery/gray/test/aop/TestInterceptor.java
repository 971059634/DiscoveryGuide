package com.nepxion.discovery.gray.test.aop;

/**
 * <p>Title: Nepxion Discovery</p>
 * <p>Description: Nepxion Discovery</p>
 * <p>Copyright: Copyright (c) 2017-2050</p>
 * <p>Company: Nepxion</p>
 * @author Haojun Ren
 * @version 1.0
 */

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.nepxion.discovery.gray.test.annotation.DTest;
import com.nepxion.discovery.gray.test.annotation.DTestGray;
import com.nepxion.discovery.gray.test.constant.TestConstant;
import com.nepxion.discovery.gray.test.gray.TestOperation;
import com.nepxion.matrix.proxy.aop.AbstractInterceptor;

public class TestInterceptor extends AbstractInterceptor {
    @Autowired
    private TestOperation testOperation;

    @Value("${" + TestConstant.SPRING_APPLICATION_TEST_GRAY_AWAIT_TIME + ":3000}")
    private Integer awaitTime;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        boolean isTestAnnotationPresent = method.isAnnotationPresent(DTest.class);
        boolean isTestGrayAnnotationPresent = method.isAnnotationPresent(DTestGray.class);
        if (isTestAnnotationPresent || isTestGrayAnnotationPresent) {
            String methodName = getMethodName(invocation);
            System.out.println("---------- Run testcase :: " + methodName + "() ----------");

            Object object = null;
            if (isTestAnnotationPresent) {
                object = invocation.proceed();
            } else {
                DTestGray testGrayAnnotation = method.getAnnotation(DTestGray.class);
                String group = convertSpel(invocation, testGrayAnnotation.group());
                String serviceId = convertSpel(invocation, testGrayAnnotation.serviceId());
                String path = convertSpel(invocation, testGrayAnnotation.path());

                testOperation.update(group, serviceId, path);

                Thread.sleep(awaitTime);

                try {
                    object = invocation.proceed();
                } finally {
                    testOperation.clear(group, serviceId);

                    Thread.sleep(awaitTime);
                }
            }

            System.out.println("* Passed");

            return object;
        }

        return invocation.proceed();
    }

    private String convertSpel(MethodInvocation invocation, String key) {
        String spelKey = null;
        try {
            spelKey = getSpelKey(invocation, key);
        } catch (Exception e) {
            spelKey = key;
        }

        return spelKey;
    }
}