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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.management.ObjectName;

import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.deployment.plugin.TargetModuleIDImpl;
import org.apache.geronimo.deployment.util.DeploymentUtil;
import org.apache.geronimo.kernel.KernelMBean;

/**
 * @version $Rev$ $Date$
 */
public class DistributeCommand extends CommandSupport {
    private static final String[] DEPLOY_SIG = {File.class.getName(), File.class.getName()};
    private final KernelMBean kernel;
    private final Target[] targetList;
    private final boolean spool;
    private File moduleArchive;
    private File deploymentPlan;
    private InputStream moduleStream;
    private InputStream deploymentStream;

    public DistributeCommand(KernelMBean kernel, Target[] targetList, File moduleArchive, File deploymentPlan) {
        super(CommandType.DISTRIBUTE);
        this.kernel = kernel;
        this.targetList = targetList;
        this.moduleArchive = moduleArchive;
        this.deploymentPlan = deploymentPlan;
        spool = false;
    }

    public DistributeCommand(KernelMBean kernel, Target[] targetList, InputStream moduleStream, InputStream deploymentStream) {
        super(CommandType.DISTRIBUTE);
        this.kernel = kernel;
        this.targetList = targetList;
        this.moduleArchive = null ;
        this.deploymentPlan = null;
        this.moduleStream = moduleStream;
        this.deploymentStream = deploymentStream;
        spool = true;
    }

    public void run() {
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
            Set deployers = kernel.listGBeans(new ObjectName("*:role=Deployer,*"));
            if (deployers.isEmpty()) {
                fail("No deployer present in kernel");
                return;
            }
            Iterator i = deployers.iterator();
            ObjectName deployer = (ObjectName) i.next();
            if (i.hasNext()) {
                throw new UnsupportedOperationException("More than one deployer found");
            }

            Object[] args = {moduleArchive, deploymentPlan};
            List objectNames = (List) kernel.invoke(deployer, "deploy", args, DEPLOY_SIG);
            if (objectNames == null || objectNames.isEmpty()) {
                DeploymentException deploymentException = new DeploymentException("Got empty list");
                deploymentException.printStackTrace();
                throw deploymentException;
            }
            String parentName = (String) objectNames.get(0);
            String[] childIDs = new String[objectNames.size()-1];
            for (int j=0; j < childIDs.length; j++) {
                childIDs[j] = (String)objectNames.get(j+1);
            }

            TargetModuleID moduleID = new TargetModuleIDImpl(targetList[0], parentName.toString(), childIDs);
            addModule(moduleID);
            complete("Completed with id " + parentName);
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

    private void copyTo(File outfile, InputStream is) throws IOException {
        byte[] buffer = new byte[4096];
        int count;
        OutputStream os = new FileOutputStream(outfile);
        try {
            while ((count = is.read(buffer)) > 0) {
                os.write(buffer, 0, count);
            }
        } finally {
            os.close();
        }
    }
}
