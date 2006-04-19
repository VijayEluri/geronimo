/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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
package org.apache.geronimo.kernel.repository;

import java.util.SortedSet;

/**
 * For repositories that can provide a list of their contents.
 * Normally local ones can handle it, but remote ones may or may
 * not implement this.
 *
 * @version $Rev$ $Date$
 */
public interface ListableRepository extends Repository {
    /**
     * Gets a list of all the items available in the repository.
     */
    public SortedSet list();

    /**
     * Gets a list of all the available items matching the specified artifact,
     * which is normally not fully resolved (so the results all match whatever
     * fields are specified on the argument Artifact).
     */
    public SortedSet list(Artifact query);
}
