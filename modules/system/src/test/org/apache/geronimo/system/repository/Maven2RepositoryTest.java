/**
 *
 * Copyright 2005 The Apache Software Foundation
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
package org.apache.geronimo.system.repository;

import java.io.File;

/**
 * @version $Rev$ $Date$
 */
public class Maven2RepositoryTest extends AbstractRepositoryTest {
    private File basedir = new File(System.getProperty("basedir"));
    
    protected void setUp() throws Exception {
        rootRepoDir = new File(basedir, "target/m2");
        repository = new Maven2Repository(rootRepoDir);
        super.setUp();
    }
}
