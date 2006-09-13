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

package org.apache.geronimo.mavenplugins.geronimo.server;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

import org.apache.maven.artifact.Artifact;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import org.codehaus.plexus.util.FileUtils;

import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Chmod;
import org.apache.geronimo.mavenplugins.geronimo.GeronimoMojoSupport;
import org.apache.geronimo.mavenplugins.geronimo.AssemblyConfig;

/**
 * Common assembly install support.
 *
 * @version $Rev$ $Date$
 */
public abstract class InstallerMojoSupport
    extends GeronimoMojoSupport
{
    /**
     * Enable forced install refresh.
     *
     * @parameter expression="${refresh}" default-value="false"
     */
    protected boolean refresh = false;

    /**
     * List of assembly artifact configurations.  Artifacts need to point to ZIP archives.
     *
     * @parameter
     */
    protected AssemblyConfig[] assemblies = null;

    /**
     * Identifer of the assembly configuration to use.
     *
     * @parameter expression="${assemblyId}"
     */
    protected String assemblyId = null;

    /**
     * The default assemblyId to use when no assemblyId configured.
     *
     * @parameter
     */
    protected String defaultAssemblyId = null;

    /**
     * A file which points to a specific assembly ZIP archive.
     * If this parameter is set, then it will be used instead of from the
     * assemblies configuration.
     *
     * @parameter expression="${assemblyArchive}"
     */
    protected File assemblyArchive = null;

    /**
     * Directory to install the assembly into.
     *
     * @parameter expression="${installDirectory}" default-value="${project.build.directory}"
     * @required
     */
    protected File installDirectory = null;

    /**
     * The directory where the assembly has been installed to.
     *
     * Normally this value is detected,
     * but if it is set, then it is assumed to be the location where a pre-installed assembly exists
     * and no installation will be done.
     *
     * @parameter expression="${geronimoHome}"
     */
    protected File geronimoHome;

    protected static final int INSTALL_FROM_ARTIFACT = 0;

    protected static final int INSTALL_FROM_FILE = 1;

    protected static final int INSTALL_ALREADY_EXISTS = 2;

    protected int installType;


    private File discoverGeronimoHome(final File archive) throws MojoExecutionException {
        log.debug("Attempting to discover geronimoHome...");

        File dir = null;

        try {
            ZipFile zipFile = new ZipFile(archive);

            Enumeration enum = zipFile.entries();
            while (enum.hasMoreElements()) {
                ZipEntry entry = (ZipEntry)enum.nextElement();
                if (entry.getName().endsWith("bin/server.jar")) {
                    File file = new File(installDirectory, entry.getName());
                    dir = file.getParentFile().getParentFile();
                    break;
                }
            }

            zipFile.close();
        }
        catch (IOException e) {
            throw new MojoExecutionException("Failed to determine geronimoHome while scanning archive for 'bin/server.jar'", e);
        }

        if (dir == null) {
            throw new MojoExecutionException("Archive does not contain a Geronimo assembly: " + archive);
        }

        return dir;
    }

    protected void init() throws MojoExecutionException, MojoFailureException {
        super.init();

        // First check if geronimoHome is set, if it is, then we can skip this
        if (geronimoHome != null) {
            // Quick sanity check
            File file = new File(geronimoHome, "bin/server.jar");
            if (!file.exists()) {
                throw new MojoExecutionException("When geronimoHome is set, it must point to a directory that contains 'bin/server.jar'");
            }
            log.info("Using pre-installed assembly: " + geronimoHome);

            installType = INSTALL_ALREADY_EXISTS;
        }
        else {
            if (assemblyArchive != null) {
                log.info("Using non-artifact based assembly archive: " + assemblyArchive);

                installType = INSTALL_FROM_FILE;
            }
            else {
                Artifact artifact = getAssemblyArtifact();

                if (!"zip".equals(artifact.getType())) {
                    throw new MojoExecutionException("Assembly file does not look like a ZIP archive");
                }

                log.info("Using assembly artifact: " + artifact);

                assemblyArchive = artifact.getFile();

                installType = INSTALL_FROM_ARTIFACT;
            }

            geronimoHome = discoverGeronimoHome(assemblyArchive);
            log.info("Using geronimoHome: " + geronimoHome);
        }
    }

    /**
     * Selects the assembly artifact tp be used for installation.
     *
     * @return The assembly artifact selected to be installed.
     *
     * @throws MojoExecutionException   Failed to select assembly artifact
     */
    protected Artifact getAssemblyArtifact() throws MojoExecutionException {
        AssemblyConfig config;

        if (assemblies == null || assemblies.length == 0) {
            throw new MojoExecutionException("At least one assembly configuration must be specified");
        }
        else if (assemblies.length > 1 && assemblyId == null && defaultAssemblyId == null) {
            throw new MojoExecutionException("Must specify assemblyId (or defaultAssemblyId) when more than on assembly configuration is given");
        }
        else if (assemblies.length == 1) {
            config = assemblies[0];
        }
        else {
            if (assemblyId == null) {
                assemblyId = defaultAssemblyId;
            }

            log.debug("Searching for assembly config for id: " + assemblyId);

            // Make sure there are no duplicate ids
            Map idMap = new HashMap();

            for (int i=0; i < assemblies.length; i++) {
                String id = assemblies[i].getId();

                if (id == null) {
                    throw new MojoExecutionException("Missing id for assembly configuration: " + assemblies[i]);
                }

                if (idMap.containsKey(id)) {
                    throw new MojoExecutionException("Duplicate assembly id: " + id);
                }

                idMap.put(id, assemblies[i]);
            }

            config = (AssemblyConfig) idMap.get(assemblyId);
            if (config == null) {
                throw new MojoExecutionException("Missing assembly configuration for id: " + assemblyId);
            }
        }

        log.info("Using assembly configuration: " + config.getId());
        Artifact artifact = getArtifact(config);

        if (artifact.getFile() == null) {
            throw new MojoExecutionException("Assembly artifact does not have an attached file: " + artifact);
        }

        return artifact;
    }

    /**
     * Performs assembly installation unless the install type is pre-existing.
     *
     * @throws Exception
     */
    protected void installAssembly() throws Exception {
        if (installType == INSTALL_ALREADY_EXISTS) {
            log.info("Installation type is pre-existing; skipping installation");
            return;
        }

        // Check if there is a newer archive or missing marker to trigger assembly install
        File installMarker = new File(geronimoHome, ".installed");

        if (!refresh) {
            if (!installMarker.exists()) {
                refresh = true;
            }
            else if (assemblyArchive.lastModified() > installMarker.lastModified()) {
                log.debug("Detected new assembly archive");
                refresh = true;
            }
        }
        else {
            log.debug("User requested installation refresh");
        }

        if (refresh) {
            if (geronimoHome.exists()) {
                log.info("Uninstalling: " + geronimoHome);
                FileUtils.forceDelete(geronimoHome);
            }
        }

        // Install the assembly
        if (!installMarker.exists()) {
            log.info("Installing assembly...");

            FileUtils.forceMkdir(geronimoHome);
            
            Expand unzip = (Expand)createTask("unzip");
            unzip.setSrc(assemblyArchive);
            unzip.setDest(installDirectory);
            unzip.execute();

            // Make scripts executable, since Java unzip ignores perms
            Chmod chmod = (Chmod)createTask("chmod");
            chmod.setPerm("ugo+rx");
            chmod.setDir(geronimoHome);
            chmod.setIncludes("bin/*.sh");
            chmod.execute();

            installMarker.createNewFile();
        }
        else {
            log.info("Re-using previously installed assembly");
        }
    }
}
