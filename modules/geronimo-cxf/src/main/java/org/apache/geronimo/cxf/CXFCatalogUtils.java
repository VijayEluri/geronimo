/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.geronimo.cxf;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.xml.resolver.Catalog;

public class CXFCatalogUtils {
    
    private static final Log LOG = LogFactory.getLog(CXFCatalogUtils.class);
        
    private CXFCatalogUtils() {        
    }
    
    public static void loadOASISCatalog(Bus bus, URL baseURL, String catalogName) {
        URL catalogURL = null;
        try {
            catalogURL = new URL(baseURL, catalogName);
            LOG.debug("Checking for " + catalogURL + " catalog.");            
            catalogURL.openStream().close();
            loadOASISCatalog(bus, catalogURL);            
        } catch (MalformedURLException e) {
            LOG.warn("Error constructing catalog URL: " + baseURL + " " + catalogName);
        } catch (FileNotFoundException e) {
            LOG.debug("Catalog " + catalogURL + " is not present in the module");
        } catch (IOException e) {
            LOG.warn("Failed to load catalog file: " + catalogURL, e);
        }
    }
    
    private static void loadOASISCatalog(Bus bus, URL catalogURL) {
        OASISCatalogManager catalog = new OASISCatalogManager();
        try {
            catalog.loadCatalog(catalogURL);
            LOG.debug("Loaded " + catalogURL + " catalog.");
            bus.setExtension(catalog.getCatalog(), Catalog.class);
        } catch (IOException e) {
            LOG.warn("Failed to load catalog file: " + catalogURL, e);
        }        
    }
}
