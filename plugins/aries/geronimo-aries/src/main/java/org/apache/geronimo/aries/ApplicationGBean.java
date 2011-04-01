/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.aries;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.aries.application.ApplicationMetadataFactory;
import org.apache.aries.application.DeploymentContent;
import org.apache.aries.application.DeploymentMetadata;
import org.apache.aries.application.DeploymentMetadataFactory;
import org.apache.aries.application.management.AriesApplication;
import org.apache.aries.application.management.AriesApplicationContextManager;
import org.apache.aries.application.management.AriesApplicationResolver;
import org.apache.aries.application.management.BundleInfo;
import org.apache.aries.application.management.ManagementException;
import org.apache.aries.application.management.AriesApplicationContext.ApplicationState;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.gbean.annotation.GBean;
import org.apache.geronimo.gbean.annotation.ParamAttribute;
import org.apache.geronimo.gbean.annotation.ParamReference;
import org.apache.geronimo.gbean.annotation.ParamSpecial;
import org.apache.geronimo.gbean.annotation.SpecialAttributeType;
import org.apache.geronimo.kernel.Kernel;
import org.apache.xbean.osgi.bundle.util.BundleUtils;
import org.apache.geronimo.kernel.repository.Artifact;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Rev:385232 $ $Date$
 */
@GBean
public class ApplicationGBean implements GBeanLifecycle {
        
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationGBean.class);
    
    private final Bundle bundle;
    private final ApplicationInstaller installer;
    private final Artifact configId;
    private GeronimoApplication application;
    private ApplicationState applicationState;
    private Set<Bundle> applicationBundles;
    
    public ApplicationGBean(@ParamSpecial(type = SpecialAttributeType.kernel) Kernel kernel,
                            @ParamSpecial(type = SpecialAttributeType.bundle) Bundle bundle,
                            @ParamAttribute(name="configId") Artifact configId, 
                            @ParamReference(name="Installer") ApplicationInstaller installer) 
        throws Exception {
        this.bundle = bundle;
        this.installer = installer;
        this.configId = configId;
                
        BundleContext bundleContext = bundle.getBundleContext();

        DeploymentMetadataFactory deploymentFactory = null;
        ApplicationMetadataFactory applicationFactory  = null;
        
        ServiceReference deploymentFactoryReference = 
            bundleContext.getServiceReference(DeploymentMetadataFactory.class.getName());
        ServiceReference applicationFactoryReference =
            bundleContext.getServiceReference(ApplicationMetadataFactory.class.getName());
        
        try {
            deploymentFactory = getService(deploymentFactoryReference, DeploymentMetadataFactory.class);
            applicationFactory = getService(applicationFactoryReference, ApplicationMetadataFactory.class);
        
            this.application = new GeronimoApplication(bundle, applicationFactory, deploymentFactory);
        } finally {
            if (deploymentFactory != null) {
                bundleContext.ungetService(deploymentFactoryReference);
            }
            if (applicationFactory != null) {
                bundleContext.ungetService(applicationFactoryReference);
            }
        }
        
        install();
                        
        ServiceReference applicationManagerReference = 
            bundleContext.getServiceReference(AriesApplicationContextManager.class.getName());
        
        GeronimoApplicationContextManager applicationManager = 
            getService(applicationManagerReference, GeronimoApplicationContextManager.class);
        try {
            applicationManager.registerApplicationContext(new GeronimoApplicationContext(this));
        } finally {
            bundleContext.ungetService(applicationManagerReference);
        }
    }

    public long[] getApplicationContentBundleIds(){
        long[] ids = new long[applicationBundles.size()];
        int i =0;
        for (Bundle content : applicationBundles){
            ids[i++] = content.getBundleId();
        }
        return ids;
    }
    
    public String getApplicationContentBundleSymbolicName(long bundleId){
        for (Bundle content : applicationBundles){
            if (content.getBundleId()==bundleId){
                return content.getSymbolicName();
            }
        }
        return null;
    }
    
    public void updateApplicationContent(long bundleId, URI uri) throws FileNotFoundException, BundleException{
        Bundle targetBundle = null;
        for (Bundle content : applicationBundles){
            if (content.getBundleId()==bundleId){
                targetBundle = content;
                break;
            }
        }
        
        if (targetBundle!=null){
            BundleContext context = bundle.getBundleContext();
            ServiceReference reference = context.getServiceReference(PackageAdmin.class.getName());
            FileInputStream fi;
            try {
                // create file object from local uri
                fi = new FileInputStream(new File(uri));
                // update bundle
                targetBundle.update(fi);
                // refresh bundle
                PackageAdmin packageAdmin = (PackageAdmin) context.getService(reference);
                packageAdmin.refreshPackages(new Bundle[]{targetBundle});
            } finally{
                context.ungetService(reference);
            }
        } else {
            throw new IllegalArgumentException("Could not find the bundle with id: " + bundleId + "in the Application content");
        }
    }
    
    protected Bundle getBundle() {
        return bundle;
    }
    
    protected AriesApplication getAriesApplication() {
        return application;
    }
    
    protected Set<Bundle> getApplicationContent() {
        return new HashSet<Bundle>(applicationBundles);
    }

    protected ApplicationState getApplicationState() {
        return applicationState;
    }
    
    private void install() throws Exception {

        BundleContext bundleContext = bundle.getBundleContext();
        
        AriesApplicationResolver resolver = null;
        PackageAdmin packageAdmin = null;
        
        ServiceReference resolverRef = bundleContext.getServiceReference(AriesApplicationResolver.class.getName());
        ServiceReference packageAdminRef = bundleContext.getServiceReference(PackageAdmin.class.getName());
                
        DeploymentMetadata meta = application.getDeploymentMetadata();
        
        List<DeploymentContent> bundlesToInstall = new ArrayList<DeploymentContent>();
        bundlesToInstall.addAll(meta.getApplicationDeploymentContents());
        bundlesToInstall.addAll(meta.getApplicationProvisionBundles());
        
        applicationBundles = new HashSet<Bundle>();
        try {
            resolver = getService(resolverRef, AriesApplicationResolver.class);
            packageAdmin = getService(packageAdminRef, PackageAdmin.class);
            
            for (DeploymentContent content : bundlesToInstall) {
                String bundleSymbolicName = content.getContentName();
                Version bundleVersion = content.getExactVersion();

                // Step 1: See if bundle is already installed in the framework
                if (findBundleInFramework(packageAdmin, bundleSymbolicName, bundleVersion) != null) {
                    continue;
                }
                
                // Step 2: See if the bundle is included in the application
                BundleInfo bundleInfo = findBundleInfoInApplication(bundleSymbolicName, bundleVersion);
                if (bundleInfo == null) {
                    // Step 3: Lookup bundle location using the resolver
                    bundleInfo = findBundleInfoUsingResolver(resolver, bundleSymbolicName, bundleVersion);
                }
                
                if (bundleInfo == null) {
                    throw new ManagementException("Cound not find bundles: " + bundleSymbolicName + "_" + bundleVersion);
                }
                    
                Bundle bundle = bundleContext.installBundle(bundleInfo.getLocation());

                applicationBundles.add(bundle);
            }
        } catch (BundleException be) {
            for (Bundle bundle : applicationBundles) {
                bundle.uninstall();
            }

            applicationBundles.clear();

            throw be;
        } finally {
            if (resolver != null) {
                bundleContext.ungetService(resolverRef);
            }
            if (packageAdmin != null) {
                bundleContext.ungetService(packageAdminRef);
            }
        }

        applicationState = ApplicationState.INSTALLED;
    }
    
    private Bundle findBundleInFramework(PackageAdmin admin, String symbolicName, Version version) {
        String exactVersion = "[" + version + "," + version + "]";
        Bundle[] bundles = admin.getBundles(symbolicName, exactVersion);
        if (bundles != null && bundles.length == 1) {
            return bundles[0];
        } else {
            return null;
        }
    }
    
    private BundleInfo findBundleInfoInApplication(String symbolicName, Version version) {
        for (BundleInfo info : application.getBundleInfo()) {
            if (info.getSymbolicName().equals(symbolicName)
                && info.getVersion().equals(version)) {
                return info;
            }
        }
        return null;
    }
    
    private BundleInfo findBundleInfoUsingResolver(AriesApplicationResolver resolver, String symbolicName, Version version) {
        return resolver.getBundleInfo(symbolicName, version);
    }
    
    private <T> T getService(ServiceReference ref, Class<T> type) throws ManagementException {
        Object service = null;
        if (ref != null) {
            service = bundle.getBundleContext().getService(ref);
        }
        
        if (service == null) {
            throw new ManagementException(new ServiceException(type.getName(), ServiceException.UNREGISTERED));
        }
        
        return type.cast(service);
    }
    
    public void doStart() throws Exception {
        LOG.debug("Starting {}", application.getApplicationMetadata().getApplicationScope());
        
        applicationState = ApplicationState.STARTING;

        List<Bundle> bundlesWeStarted = new ArrayList<Bundle>();
        try {
            for (Bundle b : applicationBundles) {
                if (BundleUtils.canStart(b)) {
                    b.start(Bundle.START_TRANSIENT);
                    bundlesWeStarted.add(b);
                }
            }
        } catch (BundleException be) {
            for (Bundle b : bundlesWeStarted) {
                try {
                    b.stop();
                } catch (BundleException be2) {
                    // we are doing tidyup here, so we don't want to replace the
                    // bundle exception
                    // that occurred during start with one from stop. We also
                    // want to try to stop
                    // all the bundles we started even if some bundles wouldn't
                    // stop.
                }
            }

            applicationState = ApplicationState.INSTALLED;
            throw be;
        }
        applicationState = ApplicationState.ACTIVE;
    }    

    public void doStop() {
        LOG.debug("Stopping {}", application.getApplicationMetadata().getApplicationScope());
        
        for (Bundle bundle : applicationBundles) {
            try {
                bundle.uninstall();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        applicationBundles.clear();

        applicationState = ApplicationState.RESOLVED;
    }

    public void doFail() {
        doStop();
    }
   
    protected void applicationStart() throws BundleException {
        try {
            installer.getConfigurationManager().loadConfiguration(configId);
            installer.getConfigurationManager().startConfiguration(configId);
        } catch (Exception e) {
            throw new BundleException("Failed to start application", e);            
        }
    }
    
    protected void applicationStop() throws BundleException {
        try {
            installer.getConfigurationManager().unloadConfiguration(configId);
        } catch (Exception e) {
            throw new BundleException("Failed to start application", e);            
        }
    }
    
    protected void applicationUninstall() {
        LOG.debug("Uninstalling {}", application.getApplicationMetadata().getApplicationScope());

        try {
            installer.getConfigurationManager().unloadConfiguration(configId);
            installer.getConfigurationManager().uninstallConfiguration(configId);
        } catch (Exception e) {
            // ignore
        }
                     
        applicationState = ApplicationState.UNINSTALLED;
    }

}
