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

import javax.enterprise.deploy.spi.status.ProgressObject;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.DeploymentManager;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Base class for CLI deployer commands.  Tracks some simple properties and
 * has common utility methods.
 *
 * @version $Rev$ $Date$
 */
public abstract class AbstractCommand implements DeployCommand {
    private String command;
    private String group;
    private String helpArgumentList;
    private String helpText;
    private PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out));

    public AbstractCommand(String command, String group, String helpArgumentList, String helpText) {
        this.command = command;
        this.group = group;
        this.helpArgumentList = helpArgumentList;
        this.helpText = helpText;
    }

    public String getCommandName() {
        return command;
    }

    public String getHelpArgumentList() {
        return helpArgumentList;
    }

    public String getHelpText() {
        return helpText;
    }

    public String getCommandGroup() {
        return group;
    }

    public boolean isLocalOnly() {
        return false;
    }

    public void setOut(PrintWriter out) {
        this.out = out;
    }

    protected void emit(String message) {
        out.println(DeployUtils.reformat(message,4,72));
        out.flush();
    }

    /**
     * Busy-waits until the provided <code>ProgressObject</code>
     * indicates that it's no longer running.
     *
     * @param out a <code>PrintWriter</code> value, only used in case
     * of an <code>InterruptedException</code> to output the stack
     * trace.
     * @param po a <code>ProgressObject</code> value
     */
    protected void waitForProgress(PrintWriter out, ProgressObject po) {
        po.addProgressListener(new ProgressListener() {
            String last = null;
            public void handleProgressEvent(ProgressEvent event) {
                String msg = event.getDeploymentStatus().getMessage();
                if(last != null && !last.equals(msg)) {
                    emit(last);
                }
                last = msg;
            }
        });
        while(po.getDeploymentStatus().isRunning()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace(out);
            }
        }
        return;
    }

    protected static boolean isMultipleTargets(TargetModuleID[] ids) {
        Set set = new HashSet();
        for(int i = 0; i < ids.length; i++) {
            TargetModuleID id = ids[i];
            set.add(id.getTarget().getName());
        }
        return set.size() > 1;
    }

    protected static Target[] identifyTargets(List targetNames, final DeploymentManager mgr) throws DeploymentException {
        Target[] tlist = new Target[targetNames.size()];
        Target[] all = mgr.getTargets();
        Set found = new HashSet();
        for (int i = 0; i < tlist.length; i++) {
            if(found.contains(targetNames.get(i))) {
                throw new DeploymentException("Target list should not contain duplicates ("+targetNames.get(i)+")");
            }
            for (int j = 0; j < all.length; j++) {
                Target server = all[j];
                if(server.getName().equals(targetNames.get(i))) {
                    tlist[i] = server;
                    found.add(server.getName());
                    break;
                }
            }
            if(tlist[i] == null) {
                throw new DeploymentException("No target named '"+targetNames.get(i)+"' was found");
            }
        }
        return tlist;
    }
}
