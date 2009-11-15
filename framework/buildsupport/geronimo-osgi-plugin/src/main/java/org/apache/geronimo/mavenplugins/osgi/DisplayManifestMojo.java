/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

package org.apache.geronimo.mavenplugins.osgi;

import java.io.File;

import org.apache.geronimo.mavenplugins.osgi.utils.BundleResolver;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.osgi.framework.BundleException;

/** 
 * @goal display-manifest
 */
public class DisplayManifestMojo extends AbstractLogEnabled implements Mojo {

    private Log log;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;
        
    /**
     * Output directory.
     *
     * @parameter expression="${project.build.directory}/classes"
     * @required
     */
    protected File targetDir = null;
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        
        if (!targetDir.exists()) {
            return;
        }
                
        BundleResolver stateController = new BundleResolver(getLogger());
                
        try {
            stateController.addBundle(targetDir);
        } catch (BundleException e) {
            log.error(e.getMessage(), e);
        }        
        stateController.resolveState();
        BundleDescription b = stateController.getBundleDescription(targetDir);                
        if (b != null) {
            displayImportExports(b);
        }    
    }

    private void displayImportExports(BundleDescription b) {
        System.out.println("Bundle: " + b.getSymbolicName());
        System.out.println();
        System.out.println("Imports:");
        ImportPackageSpecification[] importPackages = b.getImportPackages();
        if (importPackages != null) {
            for (ImportPackageSpecification importPackage : importPackages) {
                System.out.println(importPackage);
            }
        }
                    
        System.out.println();       
        System.out.println("Exports:");
        ExportPackageDescription[] exportPackages = b.getExportPackages();
        if (exportPackages != null) {
            for (ExportPackageDescription exportPackage : exportPackages) {
                System.out.println(exportPackage);
            }
        }
    }
    
    public void setLog(Log log) {
        this.log = log;
    }

    public Log getLog() {
        if (log == null) {
            setLog(new SystemStreamLog());
        }
        return log;
    }
           
}
