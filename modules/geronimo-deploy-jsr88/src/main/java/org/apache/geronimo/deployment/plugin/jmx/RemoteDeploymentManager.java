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
package org.apache.geronimo.deployment.plugin.jmx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.security.auth.login.FailedLoginException;

import org.apache.geronimo.deployment.plugin.GeronimoDeploymentManager;
import org.apache.geronimo.deployment.plugin.local.AbstractDeployCommand;
import org.apache.geronimo.deployment.plugin.local.DistributeCommand;
import org.apache.geronimo.deployment.plugin.local.RedeployCommand;
import org.apache.geronimo.deployment.plugin.remote.RemoteDeployUtil;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.system.jmx.KernelDelegate;
import org.apache.geronimo.system.plugin.DownloadResults;
import org.apache.geronimo.system.plugin.PluginList;
import org.apache.geronimo.system.plugin.DownloadPoller;
import org.apache.geronimo.system.plugin.PluginMetadata;
import org.apache.geronimo.system.plugin.PluginInstaller;
import org.apache.geronimo.system.plugin.PluginRepositoryList;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Connects to a Kernel in a remote VM (may or many not be on the same machine).
 *
 * @version $Rev$ $Date$
 */
public class RemoteDeploymentManager extends JMXDeploymentManager implements GeronimoDeploymentManager {
    private static final Log log = LogFactory.getLog(RemoteDeploymentManager.class);

    private JMXConnector jmxConnector;
    private boolean isSameMachine;

    public RemoteDeploymentManager(JMXConnector jmxConnector, String hostname) throws IOException {
        this.jmxConnector = jmxConnector;
        MBeanServerConnection mbServerConnection = jmxConnector.getMBeanServerConnection();
        initialize(new KernelDelegate(mbServerConnection));
        checkSameMachine(hostname);
    }

    public boolean isSameMachine() {
        return isSameMachine;
    }

    private void checkSameMachine(String hostname) {
        isSameMachine = false;
        if(hostname.equals("localhost") || hostname.equals("127.0.0.1")) {
            isSameMachine = true;
            return;
        }
        try {
            InetAddress dest = InetAddress.getByName(hostname);
            Enumeration en = NetworkInterface.getNetworkInterfaces();
            while(en.hasMoreElements()) {
                NetworkInterface iface = (NetworkInterface) en.nextElement();
                Enumeration ine = iface.getInetAddresses();
                while (ine.hasMoreElements()) {
                    InetAddress address = (InetAddress) ine.nextElement();
                    if(address.equals(dest)) {
                        isSameMachine = true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Unable to look up host name '"+hostname+"'; assuming it is a different machine, but this may not get very far.", e);
        }
    }

    public void release() {
        super.release();
        try {
            jmxConnector.close();
            jmxConnector = null;
        } catch (IOException e) {
            throw (IllegalStateException) new IllegalStateException("Unable to close connection").initCause(e);
        }
    }

    protected DistributeCommand createDistributeCommand(Target[] targetList, File moduleArchive, File deploymentPlan) {
        if(isSameMachine) {
            return super.createDistributeCommand(targetList, moduleArchive, deploymentPlan);
        } else {
            return new org.apache.geronimo.deployment.plugin.remote.DistributeCommand(kernel, targetList, moduleArchive, deploymentPlan);
        }
    }

    protected DistributeCommand createDistributeCommand(Target[] targetList, InputStream moduleArchive, InputStream deploymentPlan) {
        if(isSameMachine) {
            return super.createDistributeCommand(targetList, moduleArchive, deploymentPlan);
        } else {
            return new org.apache.geronimo.deployment.plugin.remote.DistributeCommand(kernel, targetList, moduleArchive, deploymentPlan);
        }
    }

    protected RedeployCommand createRedeployCommand(TargetModuleID[] moduleIDList, File moduleArchive, File deploymentPlan) {
        if(isSameMachine) {
            return super.createRedeployCommand(moduleIDList, moduleArchive, deploymentPlan);
        } else {
            return new org.apache.geronimo.deployment.plugin.remote.RedeployCommand(kernel, moduleIDList, moduleArchive, deploymentPlan);
        }
    }

    protected RedeployCommand createRedeployCommand(TargetModuleID[] moduleIDList, InputStream moduleArchive, InputStream deploymentPlan) {
        if(isSameMachine) {
            return super.createRedeployCommand(moduleIDList, moduleArchive, deploymentPlan);
        } else {
            return new org.apache.geronimo.deployment.plugin.remote.RedeployCommand(kernel, moduleIDList, moduleArchive, deploymentPlan);
        }
    }

    public PluginList listPlugins(URL mavenRepository, String username, String password) throws FailedLoginException, IOException {
        Set set = kernel.listGBeans(new AbstractNameQuery(PluginInstaller.class.getName()));
        for (Iterator it = set.iterator(); it.hasNext();) {
            AbstractName name = (AbstractName) it.next();
            PluginInstaller installer = (PluginInstaller) kernel.getProxyManager().createProxy(name, PluginInstaller.class);
            PluginList results = installer.listPlugins(mavenRepository, username, password);
            kernel.getProxyManager().destroyProxy(installer);
            return results;
        }
        return null;
    }

    public DownloadResults install(PluginList installList, String username, String password) {
        Set set = kernel.listGBeans(new AbstractNameQuery(PluginInstaller.class.getName()));
        for (Iterator it = set.iterator(); it.hasNext();) {
            AbstractName name = (AbstractName) it.next();
            PluginInstaller installer = (PluginInstaller) kernel.getProxyManager().createProxy(name, PluginInstaller.class);
            DownloadResults results = installer.install(installList, username, password);
            kernel.getProxyManager().destroyProxy(installer);
            return results;
        }
        return null;
    }

    public void install(PluginList configsToInstall, String username, String password, DownloadPoller poller) {
        Set set = kernel.listGBeans(new AbstractNameQuery(PluginInstaller.class.getName()));
        for (Iterator it = set.iterator(); it.hasNext();) {
            AbstractName name = (AbstractName) it.next();
            PluginInstaller installer = (PluginInstaller) kernel.getProxyManager().createProxy(name, PluginInstaller.class);
            installer.install(configsToInstall, username, password, poller);
            kernel.getProxyManager().destroyProxy(installer);
            return;
        }
    }

    public Object startInstall(PluginList configsToInstall, String username, String password) {
        Set set = kernel.listGBeans(new AbstractNameQuery(PluginInstaller.class.getName()));
        for (Iterator it = set.iterator(); it.hasNext();) {
            AbstractName name = (AbstractName) it.next();
            PluginInstaller installer = (PluginInstaller) kernel.getProxyManager().createProxy(name, PluginInstaller.class);
            Object result = installer.startInstall(configsToInstall, username, password);
            kernel.getProxyManager().destroyProxy(installer);
            return result;
        }
        return null;
    }

    public Object startInstall(File carFile, String username, String password) {
        File[] args = new File[]{carFile};
        if(!isSameMachine) {
            AbstractDeployCommand progress = new AbstractDeployCommand(CommandType.DISTRIBUTE, kernel, null, null, null, null, false) {
                public void run() {
                }
            };
            progress.addProgressListener(new ProgressListener() {
                public void handleProgressEvent(ProgressEvent event) {
                    log.info(event.getDeploymentStatus().getMessage());
                }
            });
            RemoteDeployUtil.uploadFilesToServer(args, progress);
        }
        Set set = kernel.listGBeans(new AbstractNameQuery(PluginInstaller.class.getName()));
        for (Iterator it = set.iterator(); it.hasNext();) {
            AbstractName name = (AbstractName) it.next();
            PluginInstaller installer = (PluginInstaller) kernel.getProxyManager().createProxy(name, PluginInstaller.class);
            Object result = installer.startInstall(carFile, username, password);
            kernel.getProxyManager().destroyProxy(installer);
            return result;
        }
        return null;
    }

    public DownloadResults checkOnInstall(Object key) {
        Set set = kernel.listGBeans(new AbstractNameQuery(PluginInstaller.class.getName()));
        for (Iterator it = set.iterator(); it.hasNext();) {
            AbstractName name = (AbstractName) it.next();
            PluginInstaller installer = (PluginInstaller) kernel.getProxyManager().createProxy(name, PluginInstaller.class);
            DownloadResults result = installer.checkOnInstall(key);
            kernel.getProxyManager().destroyProxy(installer);
            return result;
        }
        return null;
    }

    public Map getInstalledPlugins() {
        Set set = kernel.listGBeans(new AbstractNameQuery(PluginInstaller.class.getName()));
        for (Iterator it = set.iterator(); it.hasNext();) {
            AbstractName name = (AbstractName) it.next();
            PluginInstaller installer = (PluginInstaller) kernel.getProxyManager().createProxy(name, PluginInstaller.class);
            Map result = installer.getInstalledPlugins();
            kernel.getProxyManager().destroyProxy(installer);
            return result;
        }
        return null;
    }

    public PluginMetadata getPluginMetadata(Artifact configId) {
        Set set = kernel.listGBeans(new AbstractNameQuery(PluginInstaller.class.getName()));
        for (Iterator it = set.iterator(); it.hasNext();) {
            AbstractName name = (AbstractName) it.next();
            PluginInstaller installer = (PluginInstaller) kernel.getProxyManager().createProxy(name, PluginInstaller.class);
            PluginMetadata result = installer.getPluginMetadata(configId);
            kernel.getProxyManager().destroyProxy(installer);
            return result;
        }
        return null;
    }

    public void updatePluginMetadata(PluginMetadata metadata) {
        Set set = kernel.listGBeans(new AbstractNameQuery(PluginInstaller.class.getName()));
        for (Iterator it = set.iterator(); it.hasNext();) {
            AbstractName name = (AbstractName) it.next();
            PluginInstaller installer = (PluginInstaller) kernel.getProxyManager().createProxy(name, PluginInstaller.class);
            installer.updatePluginMetadata(metadata);
            kernel.getProxyManager().destroyProxy(installer);
            return;
        }
    }

    public URL[] getRepositories() {
        List list = new ArrayList();
        Set set = kernel.listGBeans(new AbstractNameQuery(PluginRepositoryList.class.getName()));
        for (Iterator it = set.iterator(); it.hasNext();) {
            AbstractName name = (AbstractName) it.next();
            PluginRepositoryList repo = (PluginRepositoryList) kernel.getProxyManager().createProxy(name, PluginRepositoryList.class);
            list.addAll(Arrays.asList(repo.getRepositories()));
            kernel.getProxyManager().destroyProxy(repo);
        }
        return (URL[]) list.toArray(new URL[list.size()]);
    }
}
