/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.geronimo.openwebbeans;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.spi.SecurityService;
import org.apache.webbeans.spi.TransactionService;
import org.apache.webbeans.spi.plugins.AbstractOwbPlugin;
import org.apache.webbeans.spi.plugins.OpenWebBeansJavaEEPlugin;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.security.Principal;
import java.security.PrivilegedActionException;

import java.util.Properties;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import javax.jws.WebService;

import javax.servlet.AsyncListener;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionListener;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;


public class GeronimoWebBeansPlugin extends AbstractOwbPlugin
    implements OpenWebBeansJavaEEPlugin, TransactionService, SecurityService {
    //OpenWebBeansEjbPlugin,
    public <T> Bean<T> defineSessionBean(Class<T> clazz,
        ProcessAnnotatedType<T> processAnnotateTypeEvent) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getSessionBeanProxy(Bean<?> bean, Class<?> iface,
        CreationalContext<?> creationalContext) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isSessionBean(Class<?> clazz) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isSingletonBean(Class<?> clazz) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isStatefulBean(Class<?> clazz) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isStatelessBean(Class<?> clazz) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void isManagedBean(Class<?> clazz) {
        if (Servlet.class.isAssignableFrom(clazz) ||
                Filter.class.isAssignableFrom(clazz) ||
                ServletContextListener.class.isAssignableFrom(clazz) ||
                ServletContextAttributeListener.class.isAssignableFrom(clazz) ||
                HttpSessionActivationListener.class.isAssignableFrom(clazz) ||
                HttpSessionAttributeListener.class.isAssignableFrom(clazz) ||
                HttpSessionBindingListener.class.isAssignableFrom(clazz) ||
                HttpSessionListener.class.isAssignableFrom(clazz) ||
                ServletRequestListener.class.isAssignableFrom(clazz) ||
                ServletRequestAttributeListener.class.isAssignableFrom(clazz) ||
                AsyncListener.class.isAssignableFrom(clazz)) {
            throw new WebBeansConfigurationException("Given class  : " +
                clazz.getName() + " is not managed bean");
        }
    }

    @Override
    public boolean supportsJavaEeComponentInjections(Class<?> clazz) {
        if (Servlet.class.isAssignableFrom(clazz) ||
                Filter.class.isAssignableFrom(clazz) ||
                ServletContextListener.class.isAssignableFrom(clazz) ||
                ServletContextAttributeListener.class.isAssignableFrom(clazz) ||
                HttpSessionActivationListener.class.isAssignableFrom(clazz) ||
                HttpSessionAttributeListener.class.isAssignableFrom(clazz) ||
                HttpSessionBindingListener.class.isAssignableFrom(clazz) ||
                HttpSessionListener.class.isAssignableFrom(clazz) ||
                ServletRequestListener.class.isAssignableFrom(clazz) ||
                ServletRequestAttributeListener.class.isAssignableFrom(clazz) ||
                clazz.isAnnotationPresent(WebService.class) ||
                AsyncListener.class.isAssignableFrom(clazz)) {
            return true;
        }

        return false;
    }

    public Transaction getTransaction() {
        // TODO Auto-generated method stub
        return null;
    }

    public TransactionManager getTransactionManager() {
        // TODO Auto-generated method stub
        return null;
    }

    public UserTransaction getUserTransaction() {
        // TODO Auto-generated method stub
        return null;
    }

    public void registerTransactionSynchronization(TransactionPhase phase,
        ObserverMethod<?super Object> observer, Object event)
        throws Exception {
        // TODO Auto-generated method stub
    }

    public Principal getCurrentPrincipal() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> Constructor<T> doPrivilegedGetDeclaredConstructor(
        Class<T> clazz, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Override
    public <T> Constructor<?>[] doPrivilegedGetDeclaredConstructors(
        Class<T> clazz) {
        return clazz.getDeclaredConstructors();
    }

    @Override
    public <T> Method doPrivilegedGetDeclaredMethod(Class<T> clazz,
        String name, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Override
    public <T> Method[] doPrivilegedGetDeclaredMethods(Class<T> clazz) {
        return clazz.getDeclaredMethods();
    }

    @Override
    public <T> Field doPrivilegedGetDeclaredField(Class<T> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    @Override
    public <T> Field[] doPrivilegedGetDeclaredFields(Class<T> clazz) {
        return clazz.getDeclaredFields();
    }

    @Override
    public void doPrivilegedSetAccessible(AccessibleObject obj, boolean flag) {
        obj.setAccessible(flag);
    }

    @Override
    public boolean doPrivilegedIsAccessible(AccessibleObject obj) {
        return obj.isAccessible();
    }

    @Override
    public <T> T doPrivilegedObjectCreate(Class<T> clazz)
        throws PrivilegedActionException, IllegalAccessException,
            InstantiationException {
        return clazz.newInstance();
    }

    @Override
    public void doPrivilegedSetSystemProperty(String propertyName, String value) {
        System.setProperty(propertyName, value);
    }

    @Override
    public String doPrivilegedGetSystemProperty(String propertyName,
        String defaultValue) {
        return System.getProperty(propertyName, defaultValue);
    }

    @Override
    public Properties doPrivilegedGetSystemProperties() {
        return System.getProperties();
    }
}
