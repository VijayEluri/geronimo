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
package org.apache.geronimo.deployment.plugin.local;

import org.apache.geronimo.deployment.plugin.TargetImpl;
import org.apache.geronimo.deployment.plugin.TargetModuleIDImpl;
import org.apache.geronimo.deployment.util.DeploymentUtil;
import org.apache.geronimo.kernel.InternalKernelException;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.config.ConfigurationManager;
import org.apache.geronimo.kernel.config.ConfigurationUtil;
import org.apache.geronimo.kernel.config.NoSuchConfigException;
import org.apache.geronimo.gbean.AbstractName;

import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.management.ObjectName;
import java.io.File;
import java.io.InputStream;

/**
 * @version $Rev$ $Date$
 */
public class RedeployCommand extends AbstractDeployCommand {
    private static final String[] UNINSTALL_SIG = {Artifact.class.getName()};
    private final TargetModuleID[] modules;

    public RedeployCommand(Kernel kernel, TargetModuleID[] moduleIDList, File moduleArchive, File deploymentPlan) {
        super(CommandType.REDEPLOY, kernel, moduleArchive, deploymentPlan, null, null, false);
        this.modules = moduleIDList;
    }

    public RedeployCommand(Kernel kernel, TargetModuleID[] moduleIDList, InputStream moduleArchive, InputStream deploymentPlan) {
        super(CommandType.REDEPLOY, kernel, null, null, moduleArchive, deploymentPlan, true);
        this.modules = moduleIDList;
    }

    public void run() {
        if (deployer == null) {
            return;
        }

        try {
            if (spool) {
                if (moduleStream != null) {
                    moduleArchive = DeploymentUtil.createTempFile();
                    copyTo(moduleArchive, moduleStream);
                }
                if (deploymentStream != null) {
                    deploymentPlan = DeploymentUtil.createTempFile();
                    copyTo(deploymentPlan, deploymentStream);
                }
            }

            ConfigurationManager configurationManager = ConfigurationUtil.getConfigurationManager(kernel);
            try {
                for (int i = 0; i < modules.length; i++) {
                    TargetModuleIDImpl module = (TargetModuleIDImpl) modules[i];

                    Artifact configID = Artifact.create(module.getModuleID());
                    try {
                        configurationManager.stopConfiguration(configID);
                        updateStatus("Stopped "+configID);
                    } catch (InternalKernelException e) {
                        Exception cause = (Exception)e.getCause();
                        if(cause instanceof NoSuchConfigException) {
                            // The modules isn't loaded -- that's OK
                        } else {
                            throw cause;
                        }
                    }
                    try {
                        configurationManager.unloadConfiguration(configID);
                        updateStatus("Unloaded "+configID);
                    } catch(InternalKernelException e) {
                        Exception cause = (Exception)e.getCause();
                        if(cause instanceof NoSuchConfigException) {
                            // The modules isn't loaded -- that's OK
                        } else {
                            throw cause;
                        }
                    } catch (NoSuchConfigException e) {
                        // The modules isn't loaded -- that's OK
                    }

                    TargetImpl target = (TargetImpl) module.getTarget();
                    AbstractName storeName = target.getAbstractName();
                    kernel.invoke(storeName, "uninstall", new Object[]{configID}, UNINSTALL_SIG);
                    updateStatus("Uninstalled "+configID);

                    doDeploy(module.getTarget(), false);
                    updateStatus("Deployed "+configID);

                    configurationManager.loadConfiguration(configID);
                    configurationManager.startConfiguration(configID);
                    updateStatus("Started " + configID);
                }
            } finally {
                ConfigurationUtil.releaseConfigurationManager(kernel, configurationManager);
            }
            addWebURLs(kernel);
            complete("Completed");
        } catch (Exception e) {
            doFail(e);
        } finally {
            if (spool) {
                if (moduleArchive != null) {
                    moduleArchive.delete();
                }
                if (deploymentPlan != null) {
                    deploymentPlan.delete();
                }
            }
        }
    }
}
