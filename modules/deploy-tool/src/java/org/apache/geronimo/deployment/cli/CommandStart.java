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

package org.apache.geronimo.deployment.cli;

import org.apache.geronimo.common.DeploymentException;

import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.TargetException;
import javax.enterprise.deploy.spi.status.ProgressObject;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * The CLI deployer logic to start.
 *
 * @version $Rev$ $Date$
 */
public class CommandStart extends AbstractCommand {
    public CommandStart() {
        super("start", "1. Common Commands", "[ModuleID|TargetModuleID]+",
                "Accepts the configId of a module, or the fully-qualified " +
                "TargetModuleID identifying both the module and the server or cluster it's " +
                "on, and starts that module.  The module should be available to the server " +
                "but not currently running.  If multiple modules are specified, they will " +
                "all be started.\n" +
                "If the server is not running, the module will be marked to start " +
                "next time the server is started.");
    }

    public CommandStart(String command, String group, String helpArgumentList, String helpText) {
        super(command, group, helpArgumentList, helpText);
    }

    public void execute(PrintWriter out, ServerConnection connection, String[] args) throws DeploymentException {
        if(args.length == 0) {
            throw new DeploymentSyntaxException("Must specify at least one module name or TargetModuleID");
        }
        DeploymentManager mgr = connection.getDeploymentManager();
        Target[] allTargets = mgr.getTargets();
        TargetModuleID[] allModules;
        try {
            allModules = mgr.getAvailableModules(null, allTargets);
        } catch(TargetException e) {
            throw new DeploymentException("Unable to load module list from server", e);
        }
        List modules = new ArrayList();
        for(int i=0; i<args.length; i++) {
            modules.addAll(identifyTargetModuleIDs(allModules, args[i]));
        }
        TargetModuleID[] ids = (TargetModuleID[]) modules.toArray(new TargetModuleID[modules.size()]);
        boolean multiple = isMultipleTargets(ids);
        ProgressObject po = runCommand(out, mgr, ids);
        TargetModuleID[] done = po.getResultTargetModuleIDs();
        out.println();
        for(int i = 0; i < done.length; i++) {
            TargetModuleID id = done[i];
            out.print(DeployUtils.reformat(getAction()+" "+id.getModuleID()+(multiple ? " on "+id.getTarget().getName() : "")+(id.getWebURL() == null || !getAction().equals("Started") ? "" : " @ "+id.getWebURL()),4, 72));
            if(id.getChildTargetModuleID() != null) {
                for (int j = 0; j < id.getChildTargetModuleID().length; j++) {
                    TargetModuleID child = id.getChildTargetModuleID()[j];
                    out.print(DeployUtils.reformat("  `-> "+child.getModuleID()+(child.getWebURL() == null || !getAction().equals("Started") ? "" : " @ "+child.getWebURL()),4, 72));
                }
            }
            out.println();
        }
        if(po.getDeploymentStatus().isFailed()) {
            throw new DeploymentException("Operation failed: "+po.getDeploymentStatus().getMessage());
        }
    }

    protected ProgressObject runCommand(PrintWriter out, DeploymentManager mgr, TargetModuleID[] ids) {
        ProgressObject po = mgr.start(ids);
        waitForProgress(out, po);
        return po;
    }

    protected String getAction() {
        return "Started";
    }

}
