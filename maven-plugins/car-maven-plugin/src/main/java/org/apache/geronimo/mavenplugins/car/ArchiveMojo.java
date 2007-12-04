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


package org.apache.geronimo.mavenplugins.car;

import java.io.File;
import java.io.IOException;

import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.system.plugin.ArchiverGBean;
import org.apache.geronimo.system.serverinfo.BasicServerInfo;
import org.apache.geronimo.system.serverinfo.ServerInfo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.mojo.pluginsupport.MojoSupport;
import org.codehaus.plexus.archiver.ArchiverException;

/**
 * @version $Rev:$ $Date:$
 * @goal archive
 */
public class ArchiveMojo extends MojoSupport {

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The target directory of the project.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    private File destDir;

    /**
     * The maven project's helper.
     *
     * @component
     * @required
     * @readonly
     */
    private MavenProjectHelper projectHelper;

    /**
     * The location of the server repository.
     *
     * @parameter expression="${project.build.directory}/assembly"
     * @required
     */
    private File targetServerDirectory;

    protected void doExecute() throws Exception {
        ServerInfo serverInfo = new BasicServerInfo(targetServerDirectory.getAbsolutePath(), false);
        ArchiverGBean archiver = new ArchiverGBean(serverInfo);
        archive("tar.gz", archiver);
        archive("zip", archiver);
    }

    private void archive(String type, ArchiverGBean archiver) throws ArchiverException, IOException {
        Artifact artifact1 = new Artifact(project.getArtifact().getGroupId(), project.getArtifact().getArtifactId(), project.getArtifact().getVersion(), type);
        File target1 = archiver.archive("", destDir.getAbsolutePath(), artifact1);
        projectHelper.attachArtifact( project, artifact1.getType(), "bin", target1 );
    }
}
