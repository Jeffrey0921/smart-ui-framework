package com.qa.framework.pagefactory.mobile.interceptor;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.qa.framework.library.webdriver.Action;
import com.qa.framework.pagefactory.mobile.ProxyUtil;
import com.qa.framework.pagefactory.mobile.ThrowableUtil;
import io.appium.java_client.MobileElement;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.pagefactory.ElementLocator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static com.qa.framework.pagefactory.TimeOutOfFindProcessor.getTimeOutOfFind;
import static io.appium.java_client.pagefactory.utils.ProxyFactory.getEnhancedProxy;

/**
 * Intercepts requests to the list of {@link MobileElement}
 */
public class ElementListInterceptor implements MethodInterceptor {
    protected final ElementLocator locator;
    protected final WebDriver driver;
    protected final Field field;
    protected final Action action;
    protected Logger logger = Logger.getLogger(ElementListInterceptor.class);
    private List<WebElement> proxyedElements;

    public ElementListInterceptor(ElementLocator locator, WebDriver driver, Field field) {
        this.locator = locator;
        this.driver = driver;
        this.field = field;
        this.action = new Action(driver);
    }

    public Object intercept(Object obj, Method method, Object[] args,
                            MethodProxy proxy) throws Throwable {
        if (Object.class.equals(method.getDeclaringClass())) {
            return proxy.invokeSuper(obj, args);
        }

        List<WebElement> realElements = null;
        int timeout = getTimeOutOfFind(field);
        long end = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < end) {
            realElements = locator.findElements();
            if (realElements != null && realElements.size() > 0) {
                break;
            }
            this.action.pause(500);
        }
        if (realElements == null) {
            logger.error("the " + this.field.getName()
                    + " element can't be found and the time(" + String.valueOf(timeout) + ") is out");
            throw new RuntimeException("the " + this.field.getName()
                    + " element can't be found and the time(" + String.valueOf(timeout) + ") is out");
        }

        try {
            int index = 0;
            for (WebElement element : realElements) {
                index++;
                SubElementInterceptor elementInterceptor = new SubElementInterceptor(element, driver, field, index);
                proxyedElements.add((WebElement) getEnhancedProxy(ProxyUtil.getTypeForProxy(driver.getClass()), elementInterceptor));
            }
            return method.invoke(proxyedElements, args);
        } catch (Throwable t) {
            throw ThrowableUtil.extractReadableException(t);
        }
    }

}
