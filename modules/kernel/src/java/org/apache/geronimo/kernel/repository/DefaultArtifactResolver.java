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
package org.apache.geronimo.kernel.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.kernel.config.Configuration;

/**
 * @version $Rev$ $Date$
 */
public class DefaultArtifactResolver implements ArtifactResolver {
    private final ArtifactManager artifactManager;
    private final Collection repositories;

    public DefaultArtifactResolver(ArtifactManager artifactManager, ListableRepository repository) {
        this.artifactManager = artifactManager;
        this.repositories = Collections.singleton(repository);
    }

    public DefaultArtifactResolver(ArtifactManager artifactManager, Collection repositories) {
        this.artifactManager = artifactManager;
        this.repositories = repositories;
    }

    public LinkedHashSet resolve(Collection artifacts) throws MissingDependencyException {
        return this.resolve(Collections.EMPTY_SET, artifacts);
    }

    public LinkedHashSet resolve(Collection parentConfigurations, Collection artifacts) throws MissingDependencyException {
        LinkedHashSet resolvedArtifacts = new LinkedHashSet();
        for (Iterator iterator = artifacts.iterator(); iterator.hasNext();) {
            Artifact artifact = (Artifact) iterator.next();
            if (!artifact.isResolved()) {
                artifact = resolve(parentConfigurations, artifact);
            }
            resolvedArtifacts.add(artifact);
        }
        return resolvedArtifacts;
    }

    public Artifact resolve(Artifact artifact) throws MissingDependencyException {
        return this.resolve(Collections.EMPTY_SET, artifact);
    }

    public Artifact resolve(Collection parentConfigurations, Artifact artifact) throws MissingDependencyException {
        if (artifact.getType() == null) {
            throw new IllegalArgumentException("Type not set " + artifact);
        }

        String groupId = artifact.getGroupId();
        if (groupId == null) {
            groupId = Artifact.DEFAULT_GROUP_ID;
        }

        Version version = artifact.getVersion();
        if (version == null) {
            // check if we have any existing artifacts loaded
            version = resolveVersion(parentConfigurations, groupId, artifact.getArtifactId(), artifact.getType());
            if (version == null) {
                throw new MissingDependencyException("Unable to resolve dependency " + artifact);
            }
        }

        return new Artifact(groupId, artifact.getArtifactId(), version, artifact.getType());
    }

    private Version resolveVersion(Collection parentConfigurations, String groupId, String artifactId, String type) {
        SortedSet existingArtifacts;
        if (artifactManager != null) {
            existingArtifacts = artifactManager.getLoadedArtifacts(groupId, artifactId, type);
        } else {
            existingArtifacts = new TreeSet();
        }

        // if we have exactly one artifact loaded use its' version
        if (existingArtifacts.size() == 1) {
            Artifact existingArtifact = (Artifact) existingArtifacts.first();
            return existingArtifact.getVersion();
        }

        // if we have no existing loaded artifacts grab the highest version from the repository
        if (existingArtifacts.size() == 0) {
            SortedSet list = new TreeSet();
            for (Iterator iterator = repositories.iterator(); iterator.hasNext();) {
                ListableRepository repository = (ListableRepository) iterator.next();
                list.addAll(repository.list(groupId, artifactId, type));
            }

            if (list.isEmpty()) {
                return null;
            }
            Artifact repositoryArtifact = (Artifact) list.last();
            return repositoryArtifact.getVersion();
        }

        // more than one version of the artifact was loaded...

        // if one of parents already loaded the artifact, use that version
        Version version = searchParents(parentConfigurations, groupId, artifactId, type);
        if (version != null) {
            return version;
        }

        // it wasn't declared by the parent so just use the highest verstion already loaded
        Artifact repositoryArtifact = (Artifact) existingArtifacts.last();
        return repositoryArtifact.getVersion();
    }

    private Version searchParents(Collection parentConfigurations, String groupId, String artifactId, String type) {
        for (Iterator iterator = parentConfigurations.iterator(); iterator.hasNext();) {
            Configuration configuration = (Configuration) iterator.next();

            // check if this parent matches the groupId, artifactId, and type
            if (matches(configuration.getId(), groupId, artifactId, type)) {
                return configuration.getId().getVersion();
            }

            Environment environment = configuration.getEnvironment();
            if (environment.isInverseClassLoading()) {
                // Search dependencies of the configuration before searching the parents
                Version version = getArtifactVersion(configuration.getDependencies(), groupId, artifactId, type);
                if (version != null) {
                    return version;
                }

                // wasn't declared in the dependencies, so search the parents of the configuration
                version = searchParents(configuration.getClassParents(), groupId, artifactId, type);
                if (version != null) {
                    return version;
                }

            } else {
                // Search the parents before the dependencies of the configuration
                Version version = searchParents(configuration.getClassParents(), groupId, artifactId, type);
                if (version != null) {
                    return version;
                }

                // wasn't declared in a parent check the dependencies of the configuration
                version = getArtifactVersion(configuration.getDependencies(), groupId, artifactId, type);
                if (version != null) {
                    return version;
                }
            }
        }
        return null;
    }

    private Version getArtifactVersion(Collection artifacts, String groupId, String artifactId, String type) {
        for (Iterator iterator = artifacts.iterator(); iterator.hasNext();) {
            Artifact artifact = (Artifact) iterator.next();
            if (matches(artifact, groupId, artifactId, type)) {
                return artifact.getVersion();
            }
        }
        return null;
    }

    private boolean matches(Artifact artifact, String groupId, String artifactId, String type) {
        return groupId.equals(artifact.getGroupId()) &&
                artifactId.equals(artifact.getArtifactId()) &&
                type.equals(artifact.getType());
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic(DefaultArtifactResolver.class, "ArtifactResolver");
        infoFactory.addReference("ArtifactManager", ArtifactManager.class, "ArtifactManager");
        infoFactory.addReference("Repositories", Repository.class, "Repository");
        infoFactory.addInterface(ArtifactResolver.class);

        infoFactory.setConstructor(new String[]{
                "ArtifactManager",
                "Repositories",
        });


        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
