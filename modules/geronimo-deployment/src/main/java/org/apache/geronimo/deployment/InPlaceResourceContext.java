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
package org.apache.geronimo.deployment;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.deployment.util.DeploymentUtil;
import org.apache.geronimo.deployment.util.NestedJarFile;
import org.apache.geronimo.kernel.config.Configuration;

class InPlaceResourceContext implements ResourceContext {
    private static final String PACKED_MODULED_SAVED_SUFFIX = ".saved";
    
    private final Configuration configuration;
    private final URI inPlaceBaseConfigurationUri;
    private final Set zipFilesToExpand = new HashSet();
    
    public InPlaceResourceContext(Configuration configuration, File inPlaceBaseConfigurationDir) throws DeploymentException {
        this.configuration = configuration;
        this.inPlaceBaseConfigurationUri = inPlaceBaseConfigurationDir.toURI();

        if (inPlaceBaseConfigurationDir.isFile()) {
            try {
                configuration.addToClassPath("");
            } catch (IOException e) {
                throw new DeploymentException(e);
            }
        }
    }

    public void addIncludeAsPackedJar(URI targetPath, JarFile jarFile) throws IOException {
        configuration.addToClassPath(targetPath.toString());
    }

    public void addInclude(URI targetPath, ZipFile zipFile, ZipEntry zipEntry) throws IOException {
        if (zipFile instanceof NestedJarFile) {
            NestedJarFile nestedJarFile = (NestedJarFile) zipFile;
            if (nestedJarFile.isPacked()) {
                zipFilesToExpand.add(zipFile);
            }
        }
        
        configuration.addToClassPath(targetPath.toString());
    }

    public void addInclude(URI targetPath, URL source) throws IOException {
        configuration.addToClassPath(targetPath.toString());
    }

    public void addInclude(URI targetPath, File source) throws IOException {
        configuration.addToClassPath(targetPath.toString());
    }

    public void addFile(URI targetPath, ZipFile zipFile, ZipEntry zipEntry) throws IOException {
        if (zipFile instanceof NestedJarFile) {
            NestedJarFile nestedJarFile = (NestedJarFile) zipFile;
            if (nestedJarFile.isPacked()) {
                zipFilesToExpand.add(zipFile);
            }
        }
    }

    public void addFile(URI targetPath, URL source) throws IOException {
    }

    public void addFile(URI targetPath, File source) throws IOException {
    }

    public void addFile(URI targetPath, String source) throws IOException {
    }
    
    public File getTargetFile(URI targetPath) {
        if (targetPath == null) throw new NullPointerException("targetPath is null");
        if (targetPath.isAbsolute()) throw new IllegalArgumentException("targetPath is absolute");
        if (targetPath.isOpaque()) throw new IllegalArgumentException("targetPath is opaque");
        return new File(inPlaceBaseConfigurationUri.resolve(targetPath));
    }
    
    public void flush() throws IOException {
        for (Iterator iter = zipFilesToExpand.iterator(); iter.hasNext();) {
            ZipFile zipFile = (ZipFile) iter.next();
            String name = zipFile.getName();
            zipFile.close();
            File srcFile = new File(name);
            File targetFile;
            if (!srcFile.isAbsolute()) {
                srcFile = new File(inPlaceBaseConfigurationUri.resolve(name));
                try {
                    targetFile = getTargetFile(new URI(name + PACKED_MODULED_SAVED_SUFFIX));
                } catch (URISyntaxException e) {
                    throw new AssertionError(e);
                }
            } else {
                targetFile = new File(name + PACKED_MODULED_SAVED_SUFFIX);                
            }
            boolean success = new File(name).renameTo(targetFile);
            if (!success) {
                throw new IOException("Cannot rename file " + 
                        name + " to " + targetFile.getAbsolutePath());
            }
            
            DeploymentUtil.unzipToDirectory(new ZipFile(targetFile), srcFile);
        }
    }
    
    
}