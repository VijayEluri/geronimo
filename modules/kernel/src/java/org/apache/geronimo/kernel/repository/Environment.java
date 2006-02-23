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

package org.apache.geronimo.kernel.repository;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * holds the data from the EnvironmentType xml while it is being resolved, transitively closed, etc.
 *
 * @version $Rev:$ $Date:$
 */
public class Environment implements Serializable {

    private Artifact configId;

    private final Map properties = new HashMap();

    private final LinkedHashSet imports = new LinkedHashSet();
    private final LinkedHashSet references = new LinkedHashSet();
    private final LinkedHashSet dependencies = new LinkedHashSet();
    private final LinkedHashSet includes = new LinkedHashSet();

    private final Set hiddenClasses = new HashSet();
    private final Set nonOverrideableClasses = new HashSet();

    private boolean inverseClassLoading;
    private boolean suppressDefaultEnvironment;

    public Environment() {
    }

    public Environment(Environment environment) {
        this.configId = environment.getConfigId();
        this.imports.addAll(environment.getImports());
        this.references.addAll(environment.getReferences());
        this.dependencies.addAll(environment.getDependencies());
        this.includes.addAll(environment.getIncludes());
        this.hiddenClasses.addAll(environment.getHiddenClasses());
        this.nonOverrideableClasses.addAll(environment.getNonOverrideableClasses());
        this.inverseClassLoading = environment.isInverseClassLoading();
        this.suppressDefaultEnvironment = environment.isSuppressDefaultEnvironment();
    }

    public Artifact getConfigId() {
        return configId;
    }

    public void setConfigId(Artifact configId) {
        this.configId = configId;
    }

    public Map getProperties() {
        return properties;
    }

    public void addProperties(Map nameKeys) {
        this.properties.putAll(nameKeys);
    }

    public void setProperties(Map properties) {
        this.properties.clear();
        addProperties(properties);
    }

    public LinkedHashSet getImports() {
        return imports;
    }

    public void addImport(Artifact importArtifact) {
        this.imports.add(importArtifact);
    }

    public void addImports(Collection imports) {
        this.imports.addAll(imports);
    }

    public void setImports(Collection imports) {
        this.imports.clear();
        addImports(imports);
    }

    public LinkedHashSet getReferences() {
        return references;
    }

    public void addReference(Artifact reference) {
        this.references.add(reference);
    }

    public void addReferences(Collection references) {
        this.references.addAll(references);
    }

    public void setReferences(Collection references) {
        this.references.clear();
        addReferences(references);
    }

    public LinkedHashSet getDependencies() {
        return dependencies;
    }

    public void addDependency(Artifact dependency) {
        this.dependencies.add(dependency);
    }

    public void addDependencies(Collection dependencies) {
        this.dependencies.addAll(dependencies);
    }

    public void setDependencies(Collection dependencies) {
        this.dependencies.clear();
        addDependencies(dependencies);
    }

    public LinkedHashSet getIncludes() {
        return includes;
    }

    public void addInclude(Artifact include) {
        this.includes.add(include);
    }

    public void addIncludes(Collection includes) {
        this.includes.addAll(includes);
    }

    public void setIncludes(Collection includes) {
        this.includes.clear();
        addIncludes(includes);
    }

    public Set getHiddenClasses() {
        return hiddenClasses;
    }

    public void addHiddenClasses(Collection hiddenClasses) {
        this.hiddenClasses.addAll(hiddenClasses);
    }

    public void setHiddenClasses(Collection hiddenClasses) {
        this.hiddenClasses.clear();
        addHiddenClasses(hiddenClasses);
    }

    public Set getNonOverrideableClasses() {
        return nonOverrideableClasses;
    }

    public void addNonOverrideableClasses(Collection nonOverrideableClasses) {
        this.nonOverrideableClasses.addAll(nonOverrideableClasses);
    }

    public void setNonOverrideableClasses(Collection nonOverrideableClasses) {
        this.nonOverrideableClasses.clear();
        addNonOverrideableClasses(nonOverrideableClasses);
    }

    public boolean isInverseClassLoading() {
        return inverseClassLoading;
    }

    public void setInverseClassLoading(boolean inverseClassLoading) {
        this.inverseClassLoading = inverseClassLoading;
    }

    public boolean isSuppressDefaultEnvironment() {
        return suppressDefaultEnvironment;
    }

    public void setSuppressDefaultEnvironment(boolean suppressDefaultEnvironment) {
        this.suppressDefaultEnvironment = suppressDefaultEnvironment;
    }
}
