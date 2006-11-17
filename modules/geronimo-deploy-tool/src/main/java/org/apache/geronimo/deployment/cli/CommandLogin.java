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
import org.apache.geronimo.util.SimpleEncryption;

import java.io.PrintWriter;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * The CLI deployer logic to start.
 *
 * @version $Rev$ $Date$
 */
public class CommandLogin extends AbstractCommand {
    public CommandLogin() {
        super("login", "1. Common Commands", "",
                "Saves the username and password for this connection to the "+
                "file .geronimo-deployer in the current user's home directory.  " +
                "Future connections to the same server will try to use this "+
                "saved authentication information instead of prompting where " +
                "possible.  This information is saved separately per connection " +
                "URL, so you can specify --url or --host and/or --port on the command " +
                "line to save a login to a different server.\n" +
                "WARNING: while the login information is not saved in " +
                "clear text, it is not secure either.  If you want to " +
                "save the authentication securely, you should change the " +
                ".geronimo-deployer file in your home directory so that nobody " +
                "else can read or write it.");
    }

    public CommandLogin(String command, String group, String helpArgumentList, String helpText) {
        super(command, group, helpArgumentList, helpText);
    }

    public void execute(PrintWriter out, ServerConnection connection, String[] args) throws DeploymentException {
        try {
            File authFile = new File(System.getProperty("user.home"), ".geronimo-deployer");
            if(!authFile.exists()) {
                if(!authFile.createNewFile()) {
                    throw new DeploymentException("Unable to create "+authFile.getAbsolutePath()+" to hold saved logins");
                }
            }
            if(!authFile.canRead() || !authFile.canWrite()) {
                throw new DeploymentException("Saved login file "+authFile.getAbsolutePath()+" is not readable or not writable");
            }
            Properties props = new Properties();
            InputStream in = new BufferedInputStream(new FileInputStream(authFile));
            props.load(in);
            in.close();
            props.setProperty("login."+connection.getServerURI(), "{Standard}"+SimpleEncryption.encrypt(connection.getAuthentication()));
            OutputStream save = new BufferedOutputStream(new FileOutputStream(authFile));
            props.store(save, "Saved authentication information to connect to Geronimo servers");
            save.flush();
            save.close();
            System.out.print(DeployUtils.reformat("Saved login for: "+connection.getServerURI(), 4, 72));
        } catch (IOException e) {
            throw new DeploymentException("Unable to save authentication to login file", e);
        }
    }
}
