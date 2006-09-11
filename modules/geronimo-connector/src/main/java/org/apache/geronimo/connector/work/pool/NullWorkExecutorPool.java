/**
 *
 * Copyright 2004 The Apache Software Foundation
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

package org.apache.geronimo.connector.work.pool;

/**
 *
 *
 * @version $Rev$ $Date$
 *
 * */
public class NullWorkExecutorPool implements WorkExecutorPool {

    private int maxSize;

    public NullWorkExecutorPool(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getPoolSize() {
        return 0;
    }

    public int getMaximumPoolSize() {
        return maxSize;
    }

    public void setMaximumPoolSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public WorkExecutorPool start() {
        return new WorkExecutorPoolImpl(maxSize);
    }

    public WorkExecutorPool stop() {
        return this;
    }

    public void execute(Runnable command) {
        throw new IllegalStateException("Stopped");
    }
}
