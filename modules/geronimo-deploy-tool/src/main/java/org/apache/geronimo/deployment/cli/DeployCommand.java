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

package org.apache.geronimo.deployment.cli;

import org.apache.geronimo.common.DeploymentException;

import java.io.PrintWriter;

/**
 * An abstraction of various CLI deployer commands.  Holds metadata like help
 * text, and also the logic to validate arguments and execute the command.
 *
 * @version $Rev$ $Date$
 */
public interface DeployCommand {
    String getCommandGroup();
    String getCommandName();
    String getHelpArgumentList();
    String getHelpText();
    boolean isLocalOnly();
    void execute(PrintWriter out, ServerConnection connection, String[] args) throws DeploymentException;
}
