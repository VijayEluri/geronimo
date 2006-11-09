/**
 *
 * Copyright 2006 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.geronimo.persistence.mockjpa;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.EntityTransaction;

/**
 * @version $Rev$ $Date$
 */
public class MockEntityManager implements EntityManager {

    private final Map properties;
    private boolean closed = false;
    private boolean cleared = false;
    private boolean joined = false;

    public MockEntityManager() {
        properties = null;
    }

    public MockEntityManager(Map properties) {
        this.properties = properties;
    }

    public void persist(Object object) {
    }

    public <T> T merge(T t) {
        return null;
    }

    public void remove(Object object) {
    }

    public <T> T find(Class<T> aClass, Object object) {
        if (aClass == Map.class && "properties".equals(object)) {
            return (T)properties;
        }
        if (aClass == EntityManager.class && "this".equals(object)) {
            return (T)this;
        }
        return null;
   }

    public <T> T getReference(Class<T> aClass, Object object) {
        return null;
    }

    public void flush() {
    }

    public void setFlushMode(FlushModeType flushModeType) {
    }

    public FlushModeType getFlushMode() {
        return null;
    }

    public void lock(Object object, LockModeType lockModeType) {
    }

    public void refresh(Object object) {
    }

    public void clear() {
        cleared = true;
    }

    public boolean contains(Object object) {
        return false;
    }

    public Query createQuery(String string) {
        return null;
    }

    public Query createNamedQuery(String string) {
        return null;
    }

    public Query createNativeQuery(String string) {
        return null;
    }

    public Query createNativeQuery(String string, Class aClass) {
        return null;
    }

    public Query createNativeQuery(String string, String string1) {
        return null;
    }

    public void close() {
        closed = true;
    }

    public boolean isOpen() {
        return !closed;
    }

    public EntityTransaction getTransaction() {
        return null;
    }

    public void joinTransaction() {
        joined = true;
    }

    public Object getDelegate() {
        return null;
    }

    public Map getProperties() {
        return properties;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isCleared() {
        return cleared;
    }

    public boolean isJoined() {
        return joined;
    }
}
