/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.geronimo.system.configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.gbean.annotation.GBean;
import org.apache.geronimo.gbean.annotation.OsgiService;
import org.apache.geronimo.gbean.annotation.ParamReference;
import org.apache.geronimo.gbean.annotation.ParamSpecial;
import org.apache.geronimo.gbean.annotation.SpecialAttributeType;
import org.apache.geronimo.kernel.config.InvalidConfigException;
import org.apache.geronimo.kernel.config.NoSuchConfigException;
import org.apache.geronimo.kernel.repository.AbstractRepository;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.repository.ArtifactResolver;
import org.apache.geronimo.kernel.repository.MissingDependencyException;
import org.apache.geronimo.kernel.repository.Repository;
import org.apache.geronimo.kernel.util.IOUtils;
import org.apache.geronimo.system.plugin.model.DependencyType;
import org.apache.geronimo.system.plugin.model.ImportType;
import org.apache.geronimo.system.plugin.model.PluginArtifactType;
import org.apache.geronimo.system.plugin.model.PluginType;
import org.apache.geronimo.system.plugin.model.PluginXmlUtil;
import org.apache.xbean.osgi.bundle.util.BundleDescription.ExportPackage;
import org.apache.xbean.osgi.bundle.util.BundleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Rev: 1063963 $ $Date: 2011-01-27 10:41:13 +0800 (Thu, 27 Jan 2011) $
 */
@GBean
@OsgiService
public class DependencyManager implements SynchronousBundleListener, GBeanLifecycle {

    private static final Logger log = LoggerFactory.getLogger(DependencyManager.class);

    private final BundleContext bundleContext;

    private final Collection<Repository> repositories;

    private RepositoryAdmin repositoryAdmin;

    private final ArtifactResolver artifactResolver;

    private final Map<Long, PluginArtifactType> pluginMap = Collections.synchronizedMap(new WeakHashMap<Long, PluginArtifactType>());

    private final Map<Long, Set<Long>> dependentBundleIdsMap = new ConcurrentHashMap<Long, Set<Long>>();

    private final Map<Long, Set<Long>> fullDependentBundleIdsMap = new ConcurrentHashMap<Long, Set<Long>>();

    private final Map<Long, Set<ExportPackage>> bundleExportPackagesMap = new HashMap<Long, Set<ExportPackage>>();

    private final Map<Artifact, Bundle> artifactBundleMap = new ConcurrentHashMap<Artifact, Bundle>();

    private final Map<Long, Artifact> bundleIdArtifactMap = new ConcurrentHashMap<Long, Artifact>();

    private ServiceReference respositoryAdminReference;

    private ThreadLocal<List<Long>> threadInstalledBundles = new ThreadLocal<List<Long>>();

    private ThreadLocal<List<Long>> threadEagerStartBundles = new ThreadLocal<List<Long>>();

    public DependencyManager(@ParamSpecial(type = SpecialAttributeType.bundleContext) BundleContext bundleContext,
            @ParamReference(name = "Repositories", namingType = "Repository") Collection<Repository> repositories,
            @ParamReference(name = "ArtifactResolver", namingType = "ArtifactResolver") ArtifactResolver artifactResolver) {
        this.bundleContext = bundleContext;
        this.repositories = repositories;
        this.artifactResolver = artifactResolver;
    }

    public void bundleChanged(BundleEvent bundleEvent) {
        int eventType = bundleEvent.getType();
        Bundle bundle = bundleEvent.getBundle();
        if ((eventType == BundleEvent.INSTALLED || eventType == BundleEvent.RESOLVED) && !dependentBundleIdsMap.containsKey(bundle.getBundleId())) {
            installed(bundle);
        } else if (eventType == BundleEvent.STARTING) {
            starting(bundle);
        } else if (eventType == BundleEvent.UNINSTALLED) {
            uninstall(bundle);
        }
    }

    public Set<ExportPackage> getExportedPackages(Bundle bundle) {
        return getExportedPackages(bundle.getBundleId());
    }

    public Set<ExportPackage> getExportedPackages(Long bundleId) {
        synchronized (bundleExportPackagesMap) {
            Set<ExportPackage> exportPackages = bundleExportPackagesMap.get(bundleId);
            if (exportPackages == null) {
                exportPackages = getExportPackagesInternal(bundleContext.getBundle(bundleId));
                bundleExportPackagesMap.put(bundleId, exportPackages);
            }
            return exportPackages;
        }
    }

    public List<Bundle> getDependentBundles(Bundle bundle) {
        Set<Long> dependentBundleIds = getDependentBundleIds(bundle);
        if (dependentBundleIds.size() == 0) {
            return Collections.<Bundle> emptyList();
        }
        List<Bundle> dependentBundles = new ArrayList<Bundle>(dependentBundleIds.size());
        for (Long dependentBundleId : dependentBundleIds) {
            dependentBundles.add(bundleContext.getBundle(dependentBundleId));
        }
        return dependentBundles;
    }

    public Set<Long> getDependentBundleIds(Bundle bundle) {
        Set<Long> dependentBundleIds = dependentBundleIdsMap.get(bundle.getBundleId());
        return dependentBundleIds == null ? Collections.<Long> emptySet() : new HashSet<Long>(dependentBundleIds);
    }

    public List<Bundle> getFullDependentBundles(Bundle bundle) {
        return getFullDependentBundles(bundle.getBundleId());
    }

    public List<Bundle> getFullDependentBundles(Long bundleId) {
        Set<Long> fullDependentBundleIds = getFullDependentBundleIds(bundleId);
        if (fullDependentBundleIds.size() == 0) {
            return Collections.<Bundle> emptyList();
        }
        List<Bundle> dependentBundles = new ArrayList<Bundle>(fullDependentBundleIds.size());
        for (Long dependentBundleId : fullDependentBundleIds) {
            dependentBundles.add(bundleContext.getBundle(dependentBundleId));
        }
        return dependentBundles;
    }

    public Set<Long> getFullDependentBundleIds(Bundle bundle) {
        return getFullDependentBundleIds(bundle.getBundleId());
    }

    public Set<Long> getFullDependentBundleIds(Long bundleId) {
        Set<Long> fullDependentBundleIds = fullDependentBundleIdsMap.get(bundleId);
        return fullDependentBundleIds == null ? Collections.<Long> emptySet() : new HashSet<Long>(fullDependentBundleIds);
    }

    public Bundle getBundle(Artifact artifact) {
        try {
            if (artifactResolver != null) {
                artifact = artifactResolver.resolveInClassLoader(artifact);
            }
        } catch (MissingDependencyException e) {
        }
        return artifactBundleMap.get(artifact);
    }

    public Artifact getArtifact(long bundleId) {
        return bundleIdArtifactMap.get(bundleId);
    }

    public Artifact toArtifact(String installationLocation) {
        if (installationLocation == null) {
            return null;
        }
        if (installationLocation.startsWith("mvn:")) {
            String[] artifactFragments = installationLocation.substring(4).split("[/]");
            if (artifactFragments.length < 2) {
                return null;
            }
            return new Artifact(artifactFragments[0], artifactFragments[1], artifactFragments.length > 2 ? artifactFragments[2] : "",
                    artifactFragments.length > 3 && artifactFragments[3].length() > 0 ? artifactFragments[3] : "jar");
        } else if (installationLocation.startsWith("reference:file://")) {
            //TODO a better way for this ???
            installationLocation = installationLocation.substring("reference:file://".length());
            for (Repository repo : repositories) {
                if (repo instanceof AbstractRepository) {
                    File rootFile = ((AbstractRepository) repo).getRootFile();
                    if (installationLocation.startsWith(rootFile.getAbsolutePath())) {
                        String artifactString = installationLocation.substring(rootFile.getAbsolutePath().length());
                        if (artifactString.startsWith(File.separator)) {
                            artifactString = artifactString.substring(File.separator.length());
                        }
                        String[] filePathFragments = artifactString.split("[" + (File.separator.equals("\\") ? "\\\\" : File.separator) + "]");
                        if (filePathFragments.length >= 4) {
                            StringBuilder groupId = new StringBuilder(filePathFragments[0]);
                            for (int i = 1; i <= filePathFragments.length - 4; i++) {
                                groupId.append(".").append(filePathFragments[i]);
                            }
                            return new Artifact(groupId.toString(), filePathFragments[filePathFragments.length - 3], filePathFragments[filePathFragments.length - 2],
                                    filePathFragments[filePathFragments.length - 1].substring(filePathFragments[filePathFragments.length - 1].lastIndexOf('.') + 1));
                        }
                    }
                }
            }
        }
        return null;
    }

    private void addArtifactBundleEntry(Bundle bundle) {
        Artifact artifact = toArtifact(bundle.getLocation());
        if (artifact != null) {
            artifactBundleMap.put(artifact, bundle);
            bundleIdArtifactMap.put(bundle.getBundleId(), artifact);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("fail to resovle artifact from the bundle location " + bundle.getLocation());
            }
        }
    }

    private void removeArtifactBundleEntry(Bundle bundle) {
        Artifact artifact = toArtifact(bundle.getLocation());
        if (artifact != null) {
            artifactBundleMap.remove(artifact);
            bundleIdArtifactMap.remove(bundle.getBundleId());
        }
    }

    private Set<ExportPackage> getExportPackagesInternal(Bundle bundle) {
        ServiceReference reference = null;
        try {
            reference = bundleContext.getServiceReference(PackageAdmin.class.getName());
            if (reference == null) {
                log.warn("No PackageAdmin service is found, fail to get export packages of " + bundle.getLocation());
                return Collections.<ExportPackage> emptySet();
            }
            PackageAdmin packageAdmin = (PackageAdmin) bundleContext.getService(reference);
            ExportedPackage[] exportedPackages = packageAdmin.getExportedPackages(bundle);
            if (exportedPackages != null) {
                Set<ExportPackage> exportPackageNames = new HashSet<ExportPackage>();
                for (ExportedPackage exportedPackage : exportedPackages) {
                    Map<String, String> attributes = new HashMap<String, String>();
                    attributes.put(Constants.VERSION_ATTRIBUTE, exportedPackage.getVersion().toString());
                    exportPackageNames.add(new ExportPackage(exportedPackage.getName(), attributes, Collections.<String, String> emptyMap()));
                }
                return exportPackageNames;
            }
            return Collections.<ExportPackage> emptySet();
        } finally {
            if (reference != null) {
                bundleContext.ungetService(reference);
            }
        }
    }

    private PluginArtifactType getPluginMetadata(Bundle bundle) {
        PluginArtifactType pluginArtifactType = null;
        URL info = bundle.getEntry("META-INF/geronimo-plugin.xml");
        if (info != null) {
            if (log.isDebugEnabled()) {
                log.debug("found geronimo-plugin.xml for bundle " + bundle);
            }
            InputStream in = null;
            try {
                in = info.openStream();
                PluginType pluginType = PluginXmlUtil.loadPluginMetadata(in);
                pluginArtifactType = pluginType.getPluginArtifact().get(0);
            } catch (Throwable e) {
                log.warn("Could not read geronimo metadata for bundle: " + bundle, e);
            } finally {
                IOUtils.close(in);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("did not find geronimo-plugin.xml for bundle " + bundle);
            }
        }
        return pluginArtifactType;
    }

    private void uninstall(Bundle bundle) {
        removeArtifactBundleEntry(bundle);
        dependentBundleIdsMap.remove(bundle.getBundleId());
        fullDependentBundleIdsMap.remove(bundle.getBundleId());
        pluginMap.remove(bundle.getBundleId());
    }

    private void installRepository(Bundle bundle) {
        if (repositoryAdmin != null) {
            URL info = bundle.getEntry("OSGI-INF/obr/repository.xml");
            if (info != null) {
                if (log.isDebugEnabled()) {
                    log.debug("found repository.xml for bundle " + bundle);
                }
                try {
                    repositoryAdmin.addRepository(info);
                } catch (Exception e) {
                    log.info("Error adding respository.xml for bundle " + bundle, e);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("did not find respository.xml for bundle " + bundle);
                }
            }
        }
    }

    public PluginArtifactType getCachedPluginMetadata(Bundle bundle) {
        PluginArtifactType pluginArtifactType = pluginMap.get(bundle.getBundleId());
        if (pluginArtifactType == null) {
            pluginArtifactType = getPluginMetadata(bundle);
            if (pluginArtifactType != null) {
                pluginMap.put(bundle.getBundleId(), pluginArtifactType);
            }
            //take this opportunity to install obr repo fragment
            // installRepository(bundle);
        }
        return pluginArtifactType;
    }

    public void installed(Bundle bundle) {
        List<Long> installedBundles = threadInstalledBundles.get();
        List<Long> eagerStartBundles = threadEagerStartBundles.get();
        boolean created = false;
        if (installedBundles == null) {
            installedBundles = new LinkedList<Long>();
            threadInstalledBundles.set(installedBundles);
            eagerStartBundles = new LinkedList<Long>();
            threadEagerStartBundles.set(eagerStartBundles);
            created = true;
        }
        try {
            if (bundleIdArtifactMap.containsKey(bundle.getBundleId())) {
                return;
            }
            addArtifactBundleEntry(bundle);
            PluginArtifactType pluginArtifactType = getCachedPluginMetadata(bundle);
            if (pluginArtifactType == null) {
                return;
            }
            List<DependencyType> dependencies = pluginArtifactType.getDependency();
            Set<Long> dependentBundleIds = new HashSet<Long>();
            Set<Long> fullDependentBundleIds = new HashSet<Long>();

            for (DependencyType dependencyType : dependencies) {
                if (log.isDebugEnabled()) {
                    log.debug("Installing artifact: " + dependencyType);
                }
                Artifact artifact = dependencyType.toArtifact();
                if (artifactResolver != null) {
                    artifact = artifactResolver.resolveInClassLoader(artifact);
                }
                String location = locateBundle(artifact);
                try {
                    Bundle installedDependentBundle = bundleContext.installBundle(location);
                    long installedDependentBundleId = installedDependentBundle.getBundleId();
                    installedBundles.add(installedDependentBundleId);
                    dependentBundleIds.add(installedDependentBundleId);

                    if (dependencyType.isEagerStart() && BundleUtils.canStart(installedDependentBundle)) {
                        eagerStartBundles.add(installedDependentBundleId);
                    }

                    if (fullDependentBundleIds.add(installedDependentBundleId)) {
                        Set<Long> parentDependentBundleIds = fullDependentBundleIdsMap.get(installedDependentBundleId);
                        if (parentDependentBundleIds != null) {
                            fullDependentBundleIds.addAll(parentDependentBundleIds);
                        }
                    }
                } catch (BundleException e) {
                    log.warn("Could not install bundle for artifact: " + artifact, e);
                }
            }
            installedBundles.add(bundle.getBundleId());
            fullDependentBundleIdsMap.put(bundle.getBundleId(), fullDependentBundleIds);
            dependentBundleIdsMap.put(bundle.getBundleId(), dependentBundleIds);
        } catch (Exception e) {
            log.error("Could not install bundle", e);
        } finally {
            if (created) {
                threadInstalledBundles.remove();
                threadEagerStartBundles.remove();
                try {
                    for (Long bundleId : installedBundles) {
                        Bundle currentBundle = bundleContext.getBundle(bundleId);
                        if (!BundleUtils.isResolved(currentBundle)) {
                            BundleUtils.resolve(currentBundle);
                            if (!BundleUtils.isResolved(currentBundle)) {
                                log.error("Could not resolve the dependency bundle " + currentBundle.getLocation());
                                StringBuilder buf = new StringBuilder("known bundles: ");
                                for (Bundle b: bundleContext.getBundles()) {
                                    buf.append("\n   ").append(b.getLocation()).append("  state: ").append(b.getState());
                                }
                                log.error(buf.toString());
                            }
                        }
                    }
                    for (Long bundleId : eagerStartBundles) {
                        Bundle currentBundle = bundleContext.getBundle(bundleId);
                        if (BundleUtils.canStart(currentBundle)) {
                            currentBundle.start(Bundle.START_TRANSIENT);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Fail to resolve or start the bundle", e);
                }

            }
        }
    }

    private void starting(Bundle bundle) {
        PluginArtifactType pluginArtifactType = getCachedPluginMetadata(bundle);
        if (pluginArtifactType != null) {
            List<Bundle> bundles = new ArrayList<Bundle>();
            List<DependencyType> dependencies = pluginArtifactType.getDependency();
            try {
                for (DependencyType dependencyType : dependencies) {
                    if (log.isDebugEnabled()) {
                        log.debug("Starting artifact: " + dependencyType);
                    }
                    Artifact artifact = dependencyType.toArtifact();
                    if (artifactResolver != null) {
                        artifact = artifactResolver.resolveInClassLoader(artifact);
                    }
                    String location = locateBundle(artifact);
                    Bundle b = bundleContext.installBundle(location);
                    //Do not start the bundle of ImportType.CLASSES, for class loading use, they are only required to be in the RESOLVED status
                    if (b.getState() != Bundle.ACTIVE && (!ImportType.CLASSES.equals(dependencyType.getImport()) || dependencyType.isEagerStart())) {
                        bundles.add(b);
                    }
                }

                for (Bundle b : bundles) {
                    if (BundleUtils.canStart(b)) {
                        try {
                            b.start(Bundle.START_TRANSIENT);
                        } catch (BundleException e) {
                            log.warn("Could not start bundle: " + b, e);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Could not install bundle dependecy", e);
            }
        }
    }

    private String locateBundle(Artifact configurationId) throws NoSuchConfigException, IOException, InvalidConfigException {
        if (System.getProperty("geronimo.build.car") == null) {
            return "mvn:" + configurationId.getGroupId() + "/" + configurationId.getArtifactId() + "/" + configurationId.getVersion()
                    + ("jar".equals(configurationId.getType()) ? "" : "/" + configurationId.getType());
        }
        for (Repository repo : repositories) {
            if (repo.contains(configurationId)) {
                return "reference:file://" + repo.getLocation(configurationId).getAbsolutePath();
            }
        }
        throw new NoSuchConfigException(configurationId);
    }

    @Override
    public void doStart() throws Exception {
        bundleContext.addBundleListener(this);
        respositoryAdminReference = bundleContext.getServiceReference(RepositoryAdmin.class.getName());
        repositoryAdmin = respositoryAdminReference == null ? null : (RepositoryAdmin) bundleContext.getService(respositoryAdminReference);
        //init installed bundles
        for (Bundle bundle : bundleContext.getBundles()) {
            installed(bundle);
        }
        //Check the car who loads me ...
        try {
            PluginArtifactType pluginArtifact = getCachedPluginMetadata(bundleContext.getBundle());
            if (pluginArtifact != null) {
                Set<Long> dependentBundleIds = new HashSet<Long>();
                for (DependencyType dependency : pluginArtifact.getDependency()) {
                    Bundle dependentBundle = getBundle(dependency.toArtifact());
                    if (dependentBundle != null) {
                        dependentBundleIds.add(dependentBundle.getBundleId());
                    }
                }
                long bundleId = bundleContext.getBundle().getBundleId();
                dependentBundleIdsMap.put(bundleId, dependentBundleIds);
                fullDependentBundleIdsMap.put(bundleId, dependentBundleIds);
            }
        } catch (Exception e) {
            log.error("Fail to read the dependency info from bundle " + bundleContext.getBundle().getLocation());
        }
    }

    @Override
    public void doStop() throws Exception {
        if (respositoryAdminReference != null) {
            try {
                bundleContext.ungetService(respositoryAdminReference);
            } catch (Exception e) {
            }
        }
        bundleContext.removeBundleListener(this);
        //Some clean up work
        pluginMap.clear();
        dependentBundleIdsMap.clear();
        fullDependentBundleIdsMap.clear();
        bundleExportPackagesMap.clear();
        artifactBundleMap.clear();
        bundleIdArtifactMap.clear();
    }

    @Override
    public void doFail() {
        try {
            doStop();
        } catch (Exception e) {
        }
    }
}
