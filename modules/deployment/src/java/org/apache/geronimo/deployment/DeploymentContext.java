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

package org.apache.geronimo.deployment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.management.ObjectName;

import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.deployment.util.DeploymentUtil;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.kernel.GBeanNotFoundException;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.config.Configuration;
import org.apache.geronimo.kernel.config.ConfigurationData;
import org.apache.geronimo.kernel.config.ConfigurationManager;
import org.apache.geronimo.kernel.config.ConfigurationModuleType;
import org.apache.geronimo.kernel.config.ConfigurationStore;
import org.apache.geronimo.kernel.config.ConfigurationUtil;
import org.apache.geronimo.kernel.config.InvalidConfigException;
import org.apache.geronimo.kernel.config.NoSuchConfigException;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.repository.ArtifactResolver;
import org.apache.geronimo.kernel.repository.DefaultArtifactResolver;
import org.apache.geronimo.kernel.repository.Dependency;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.kernel.repository.ImportType;

/**
 * @version $Rev$ $Date$
 */
public class DeploymentContext {

    private final Environment environment;
    private final ConfigurationModuleType moduleType;
    private final LinkedHashSet classpath = new LinkedHashSet();
    private final Collection repositories;
    private final Kernel kernel;
    private final GBeanDataRegistry gbeans = new GBeanDataRegistry();
    private final File baseDir;
    private final URI baseUri;
    private final byte[] buffer = new byte[4096];
    private final List loadedConfigurations = new ArrayList();
    private final List childConfigurationDatas = new ArrayList();
    private ConfigurationManager configurationManager;
    private final ArtifactResolver artifactResolver;


    public DeploymentContext(File baseDir, Environment environment, ConfigurationModuleType type, Collection repositories, Kernel kernel) throws DeploymentException {
        assert baseDir != null: "baseDir is null";
        assert environment != null: "environment is null";
        assert type != null: "type is null";

        this.environment = environment;
        this.moduleType = type;
        this.repositories = repositories;
        this.kernel = kernel;

        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        if (!baseDir.isDirectory()) {
            throw new DeploymentException("Base directory is not a directory: " + baseDir.getAbsolutePath());
        }
        this.baseDir = baseDir;
        this.baseUri = baseDir.toURI();

        if (kernel != null) {
            configurationManager = ConfigurationUtil.getConfigurationManager(kernel);
        }
        artifactResolver = new DefaultArtifactResolver(null, repositories);

        determineNaming();
        setupParents();
    }

    private void determineNaming() throws DeploymentException {
        if (environment.getProperties() != null && !environment.getProperties().isEmpty()) {
            return;
        }
        Collection dependencies = environment.getDependencies();
        if (kernel == null ||  dependencies.isEmpty()) {
            throw new DeploymentException("Neither domain and server nor any way to determine them was provided for configuration " + environment.getConfigId());
        }
        ConfigurationManager configurationManager = ConfigurationUtil.getConfigurationManager(kernel);

        try {
            boolean loaded = false;

            Artifact parent = null;
            for (Iterator iterator = dependencies.iterator(); iterator.hasNext();) {
                Dependency dependency = (Dependency) iterator.next();
                Artifact artifact = dependency.getArtifact();
                if (configurationManager.isConfiguration(artifact)) {
                    if (!configurationManager.isLoaded(artifact)) {
                        configurationManager.loadConfiguration(artifact);
                        loaded = true;
                    }
                    parent = artifact;
                    break;
                }
            }
            if (parent == null) {
                throw new DeploymentException("Neither domain and server nor any way to determine them was provided for configuration " + environment.getConfigId());
            }

            try {
                ObjectName parentName = Configuration.getConfigurationObjectName(parent);
                Environment environment = (Environment) kernel.getAttribute(parentName, "environment");
                this.environment.setProperties(environment.getProperties());
            } catch (Exception e) {
                throw new DeploymentException("Unable to copy domain and server from parent configuration", e);
            } finally {
                if (loaded) {
//                    we need to unload again so the loadedAncestors list will be in the correct order to start configs.
                    configurationManager.unloadConfiguration(parent);
                }
            }
        } catch (Exception e) {
            throw new DeploymentException("Unable to load first parent of configuration " + environment.getConfigId(), e);
        } finally {
            ConfigurationUtil.releaseConfigurationManager(kernel, configurationManager);
        }

        //check that domain and server are now known
        if (environment.getProperties() == null || environment.getProperties().isEmpty()) {
            throw new IllegalStateException("Properties not be determined from explicit args or parent configuration. ParentID: " + dependencies);
        }
    }

    public Artifact getConfigID() {
        return environment.getConfigId();
    }

    public ConfigurationModuleType getType() {
        return moduleType;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public void addGBean(GBeanData gbean) {
        assert gbean.getName() != null: "GBean name is null";
        gbeans.register(gbean);
    }

    public Set getGBeanNames() {
        return gbeans.getGBeanNames();
    }

    public Set listGBeans(ObjectName pattern) {
        return gbeans.listGBeans(pattern);
    }

    public GBeanData getGBeanInstance(ObjectName name) throws GBeanNotFoundException {
        return gbeans.getGBeanInstance(name);
    }

    /**
     * Copy a packed jar file into the deployment context and place it into the
     * path specified in the target path.  The newly added packed jar is added
     * to the classpath of the configuration.
     * <p/>
     * NOTE: The class loader that is obtained from this deployment context
     * may get out of sync with the newly augmented classpath; obtain a freshly
     * minted class loader by calling <code>getConfigurationClassLoader</code> method.
     *
     * @param targetPath where the packed jar file should be placed
     * @param jarFile    the jar file to copy
     * @throws IOException if there's a problem copying the jar file
     */
    public void addIncludeAsPackedJar(URI targetPath, JarFile jarFile) throws IOException {
        File targetFile = getTargetFile(targetPath);
        DeploymentUtil.copyToPackedJar(jarFile, targetFile);
        classpath.add(targetPath);
    }

    /**
     * Copy a ZIP file entry into the deployment context and place it into the
     * path specified in the target path.  The newly added entry is added
     * to the classpath of the configuration.
     * <p/>
     * NOTE: The class loader that is obtained from this deployment context
     * may get out of sync with the newly augmented classpath; obtain a freshly
     * minted class loader by calling <code>getConfigurationClassLoader</code> method.
     *
     * @param targetPath where the ZIP file entry should be placed
     * @param zipFile    the ZIP file
     * @param zipEntry   the ZIP file entry
     * @throws IOException if there's a problem copying the ZIP entry
     */
    public void addInclude(URI targetPath, ZipFile zipFile, ZipEntry zipEntry) throws IOException {
        File targetFile = getTargetFile(targetPath);
        addFile(targetFile, zipFile, zipEntry);
        classpath.add(targetPath);
    }

    /**
     * Copy a file into the deployment context and place it into the
     * path specified in the target path.  The newly added file is added
     * to the classpath of the configuration.
     * <p/>
     * NOTE: The class loader that is obtained from this deployment context
     * may get out of sync with the newly augmented classpath; obtain a freshly
     * minted class loader by calling <code>getConfigurationClassLoader</code> method.
     *
     * @param targetPath where the file should be placed
     * @param source     the URL of file to be copied
     * @throws IOException if there's a problem copying the ZIP entry
     */
    public void addInclude(URI targetPath, URL source) throws IOException {
        File targetFile = getTargetFile(targetPath);
        addFile(targetFile, source);
        classpath.add(targetPath);
    }

    /**
     * Copy a file into the deployment context and place it into the
     * path specified in the target path.  The newly added file is added
     * to the classpath of the configuration.
     * <p/>
     * NOTE: The class loader that is obtained from this deployment context
     * may get out of sync with the newly augmented classpath; obtain a freshly
     * minted class loader by calling <code>getConfigurationClassLoader</code> method.
     *
     * @param targetPath where the file should be placed
     * @param source     the file to be copied
     * @throws IOException if there's a problem copying the ZIP entry
     */
    public void addInclude(URI targetPath, File source) throws IOException {
        File targetFile = getTargetFile(targetPath);
        addFile(targetFile, source);
        classpath.add(targetPath);
    }

    /**
     * Import the classpath from a jar file's manifest.  The imported classpath
     * is crafted relative to <code>moduleBaseUri</code>.
     * <p/>
     * NOTE: The class loader that is obtained from this deployment context
     * may get out of sync with the newly augmented classpath; obtain a freshly
     * minted class loader by calling <code>getConfigurationClassLoader</code> method.
     *
     * @param moduleFile    the jar file from which the manifest is obtained.
     * @param moduleBaseUri the base for the imported classpath
     * @throws DeploymentException if there is a problem with the classpath in
     *                             the manifest
     */
    public void addManifestClassPath(JarFile moduleFile, URI moduleBaseUri) throws DeploymentException {
        Manifest manifest;
        try {
            manifest = moduleFile.getManifest();
        } catch (IOException e) {
            throw new DeploymentException("Could not read manifest: " + moduleBaseUri);
        }

        if (manifest == null) {
            return;
        }
        String manifestClassPath = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
        if (manifestClassPath == null) {
            return;
        }

        for (StringTokenizer tokenizer = new StringTokenizer(manifestClassPath, " "); tokenizer.hasMoreTokens();) {
            String path = tokenizer.nextToken();

            URI pathUri;
            try {
                pathUri = new URI(path);
            } catch (URISyntaxException e) {
                throw new DeploymentException("Invalid manifest classpath entry: module=" + moduleBaseUri + ", path=" + path);
            }

            if (!pathUri.getPath().endsWith(".jar")) {
                throw new DeploymentException("Manifest class path entries must end with the .jar extension (J2EE 1.4 Section 8.2): module=" + moduleBaseUri);
            }
            if (pathUri.isAbsolute()) {
                throw new DeploymentException("Manifest class path entries must be relative (J2EE 1.4 Section 8.2): moduel=" + moduleBaseUri);
            }

            URI targetUri = moduleBaseUri.resolve(pathUri);
            classpath.add(targetUri);
        }
    }

    public void addFile(URI targetPath, ZipFile zipFile, ZipEntry zipEntry) throws IOException {
        addFile(getTargetFile(targetPath), zipFile, zipEntry);
    }

    public void addFile(URI targetPath, URL source) throws IOException {
        addFile(getTargetFile(targetPath), source);
    }

    public void addFile(URI targetPath, File source) throws IOException {
        addFile(getTargetFile(targetPath), source);
    }

    public void addFile(URI targetPath, String source) throws IOException {
        addFile(getTargetFile(targetPath), new ByteArrayInputStream(source.getBytes()));
    }

    public void addClass(URI location, String fqcn, byte[] bytes) throws IOException, URISyntaxException {
        assert location.toString().endsWith("/");

        classpath.add(location);
        String classFileName = fqcn.replace('.', '/') + ".class";
        addFile(getTargetFile(new URI(location.toString() + classFileName)), new ByteArrayInputStream(bytes));
    }

    private void addFile(File targetFile, ZipFile zipFile, ZipEntry zipEntry) throws IOException {
        if (zipEntry.isDirectory()) {
            targetFile.mkdirs();
        } else {
            InputStream is = zipFile.getInputStream(zipEntry);
            try {
                addFile(targetFile, is);
            } finally {
                DeploymentUtil.close(is);
            }
        }
    }

    private void addFile(File targetFile, URL source) throws IOException {
        InputStream in = null;
        try {
            in = source.openStream();
            addFile(targetFile, in);
        } finally {
            DeploymentUtil.close(in);
        }
    }

    private void addFile(File targetFile, File source) throws IOException {
        InputStream in = null;
        try {
            in = new FileInputStream(source);
            addFile(targetFile, in);
        } finally {
            DeploymentUtil.close(in);
        }
    }

    private void addFile(File targetFile, InputStream source) throws IOException {
        targetFile.getParentFile().mkdirs();
        OutputStream out = null;
        try {
            out = new FileOutputStream(targetFile);
            int count;
            while ((count = source.read(buffer)) > 0) {
                out.write(buffer, 0, count);
            }
        } finally {
            DeploymentUtil.close(out);
        }
    }

    public File getTargetFile(URI targetPath) {
        assert !targetPath.isAbsolute() : "targetPath is absolute";
        assert !targetPath.isOpaque() : "targetPath is opaque";
        return new File(baseUri.resolve(targetPath));
    }

//    static interface ParentSource {
//        Collection getParents(Artifact point) throws DeploymentException;
//    }
//
//    List getExtremalSet(Collection points, ParentSource parentSource) throws DeploymentException {
//        LinkedHashMap pointToEnvelopeMap = new LinkedHashMap();
//        for (Iterator iterator = points.iterator(); iterator.hasNext();) {
//            Artifact newPoint = (Artifact) iterator.next();
//            Set newEnvelope = new HashSet();
//            getEnvelope(newPoint, parentSource, newEnvelope);
//            boolean useMe = true;
//            for (Iterator iterator1 = pointToEnvelopeMap.entrySet().iterator(); iterator1.hasNext();) {
//                Map.Entry entry = (Map.Entry) iterator1.next();
//                Set existingEnvelope = (Set) entry.getValue();
//                if (existingEnvelope.contains(newPoint)) {
//                    useMe = false;
//                } else if (newEnvelope.contains(entry.getKey())) {
//                    iterator1.remove();
//                }
//            }
//            if (useMe) {
//                pointToEnvelopeMap.put(newPoint, newEnvelope);
//            }
//        }
//        return new ArrayList(pointToEnvelopeMap.keySet());
//    }
//
//    private void getEnvelope(Artifact point, ParentSource parentSource, Set envelope) throws DeploymentException {
//        Collection newPoints = parentSource.getParents(point);
//        envelope.addAll(newPoints);
//        for (Iterator iterator = newPoints.iterator(); iterator.hasNext();) {
//            Artifact newPoint = (Artifact) iterator.next();
//            getEnvelope(newPoint, parentSource, envelope);
//        }
//    }
//
//    static class ConfigurationParentSource implements ParentSource {
//
//        private final Kernel kernel;
//
//        public ConfigurationParentSource(Kernel kernel) {
//            this.kernel = kernel;
//        }
//
//        public Collection getParents(Artifact configID) throws DeploymentException {
//            ObjectName configName;
//            try {
//                configName = Configuration.getConfigurationObjectName(configID);
//            } catch (InvalidConfigException e) {
//                throw new DeploymentException("Cannot convert ID to ObjectName: ", e);
//            }
//            try {
//                Environment environment = (Environment) kernel.getAttribute(configName, "environment");
//                return environment.getImports();
//            } catch (Exception e) {
//                throw new DeploymentException("Cannot find parents of alleged config: ", e);
//            }
//        }
//
//    }

    private void setupParents() throws DeploymentException {
        if (kernel != null) {
            try {
                List dependencies = new ArrayList(environment.getDependencies());
                for (ListIterator iterator = dependencies.listIterator(); iterator.hasNext();) {
                    Dependency dependency = (Dependency) iterator.next();
                    Artifact resolvedArtifact = artifactResolver.resolve(dependency.getArtifact());
                    if (configurationManager != null && configurationManager.isConfiguration(resolvedArtifact)) {
                        configurationManager.loadConfiguration(resolvedArtifact);

                        // update the dependency list to contain the resolved artifact
                        dependency = new Dependency(resolvedArtifact, dependency.getImportType());
                        iterator.set(dependency);
                    } else if (dependency.getImportType() == ImportType.SERVICES) {
                        // Service depdendencies require that the depdencency be a configuration
                        if (configurationManager == null) throw new NullPointerException("configurationManager is null");
                        throw new DeploymentException("Dependency does not have services: " + resolvedArtifact);
                    }
                }
                environment.setDependencies(dependencies);
            } catch (DeploymentException e) {
                throw e;
            } catch (Exception e) {
                throw new DeploymentException("Unable to load parents", e);
            }
        }
    }

    public ClassLoader getClassLoader() throws DeploymentException {
        return getConfiguration(null).getConfigurationClassLoader();
    }

    public ClassLoader getClassLoader(Configuration knownParent) throws DeploymentException {
        return getConfiguration(knownParent).getConfigurationClassLoader();
    }

    public Configuration getConfiguration(Configuration knownParent) throws DeploymentException {
        Environment environmentCopy = new Environment(environment);
        if (knownParent != null) {
            environmentCopy.addDependency(knownParent.getId(), ImportType.ALL);
        }
        try {
            List parents = createParentProxies(environmentCopy, knownParent);
            Configuration configuration = new Configuration(parents,
                    Configuration.getConfigurationObjectName(environmentCopy.getConfigId()).getCanonicalName(),
                    moduleType,
                    environmentCopy,
                    new ArrayList(classpath),
                    null,
                    repositories,
                    new ConfigurationStore() {

                        public void install(ConfigurationData configurationData) {
                        }

                        public void uninstall(Artifact configID) {
                        }

                        public GBeanData loadConfiguration(Artifact configId) {
                            return null;
                        }

                        public boolean containsConfiguration(Artifact configID) {
                            return false;
                        }

                        public String getObjectName() {
                            return null;
                        }

                        public List listConfigurations() {
                            return null;
                        }

                        public File createNewConfigurationDir(Artifact configId) {
                            return null;
                        }

                        public URL resolve(Artifact configId, URI uri) throws MalformedURLException {
                            return new File(baseDir, uri.toString()).toURL();
                        }
                    },
                    null,
                    null);
            return configuration;
        } catch (Exception e) {
            throw new DeploymentException("Could not construct configuration classloader for deployment context", e);
        }
    }

    private List createParentProxies(Environment environment, Configuration existingConfiguration) throws InvalidConfigException {
        List parents = new ArrayList();
        try {
            List dependencies = new ArrayList(environment.getDependencies());
            for (ListIterator iterator = dependencies.listIterator(); iterator.hasNext();) {
                Dependency dependency = (Dependency) iterator.next();
                Artifact resolvedArtifact = artifactResolver.resolve(dependency.getArtifact());
                if (configurationManager != null && isConfiguration(resolvedArtifact, existingConfiguration)) {
                    // add the parent configuration to the parents collection
                    Configuration parent = getLoadedConfiguration(resolvedArtifact, existingConfiguration);
                    parents.add(parent);

                    // update the dependency list to contain the resolved artifact
                    dependency = new Dependency(resolvedArtifact, dependency.getImportType());
                    iterator.set(dependency);
                } else if (dependency.getImportType() == ImportType.SERVICES) {
                    // Service depdendencies require that the depdencency be a configuration
                    if (configurationManager == null) throw new NullPointerException("configurationManager is null");
                    throw new InvalidConfigException("Dependency does not have services: " + resolvedArtifact);
                }
            }
            environment.setDependencies(dependencies);
        } catch (InvalidConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidConfigException("Unable to load parents", e);
        }
        return parents;
    }

    private boolean isConfiguration(Artifact artifact, Configuration existingConfiguration) {
        if (configurationManager.isConfiguration(artifact)) {
            return true;
        }
        return existingConfiguration != null && artifact.equals(existingConfiguration.getId());
    }

    private Configuration getLoadedConfiguration(Artifact artifact, Configuration existingConfiguration) {
        if (existingConfiguration != null && artifact.equals(existingConfiguration.getId())) {
            return existingConfiguration;
        } else {
            return configurationManager.getConfiguration(artifact);
        }
    }

    public void close() throws IOException, DeploymentException {
        if (kernel != null) {
            ConfigurationManager configurationManager = ConfigurationUtil.getConfigurationManager(kernel);
            try {
//                startedAncestors.clear();
                //TODO configid WE NEED REFERENCE COUNTING ON THIS STUFF!!!
                //right now it is impossible to deploy 2 app clients in an ear. 
//                Collections.reverse(loadedAncestors);
/*
                for (Iterator iterator = loadedAncestors.iterator(); iterator.hasNext();) {
                    Artifact configID = (Artifact) iterator.next();
                    if (configurationManager.isLoaded(configID)) {
                        try {
                            configurationManager.unload(configID);
                        } catch (NoSuchConfigException e) {
                            throw new DeploymentException("Could not find a configuration we previously loaded! " + configID, e);
                        }
                    }
                }
*/
                for (Iterator iterator = loadedConfigurations.iterator(); iterator.hasNext();) {
                    Artifact artifact = (Artifact) iterator.next();
                    try {
                        configurationManager.unloadConfiguration(artifact);
                    } catch (NoSuchConfigException ignored) {
                    }
                }
                loadedConfigurations.clear();
            } finally {
                ConfigurationUtil.releaseConfigurationManager(kernel, configurationManager);
            }
        }
    }

    public void addChildConfiguration(ConfigurationData configurationData) {
        childConfigurationDatas.add(configurationData);
    }

    public ConfigurationData getConfigurationData() {
        ConfigurationData configurationData = new ConfigurationData(moduleType, classpath, Arrays.asList(gbeans.getGBeans()), childConfigurationDatas, environment, baseDir);
        return configurationData;
    }

    public Object getAttribute(ObjectName name, String property) throws Exception {
        return kernel.getAttribute(name, property);
    }
}
