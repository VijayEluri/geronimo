/**
 *
 * Copyright 2003-2005 The Apache Software Foundation
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

package org.apache.geronimo.connector;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

/**
 * Dynamic GBean wrapper around a ResourceAdapter object, exposing the config-properties as
 * GBean attributes.
 *
 * @version $Rev$ $Date$
 */
public class ResourceAdapterWrapper implements ResourceAdapter {

    private final String resourceAdapterClass;

    private final BootstrapContext bootstrapContext;

    protected final ResourceAdapter resourceAdapter;


    /**
     *  default constructor for enhancement proxy endpoint
     */
    public ResourceAdapterWrapper() {
        this.resourceAdapterClass = null;
        this.bootstrapContext = null;
        this.resourceAdapter = null;
    }

    public ResourceAdapterWrapper(String resourceAdapterClass,
            BootstrapContext bootstrapContext,
            ClassLoader cl) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        this.resourceAdapterClass = resourceAdapterClass;
        this.bootstrapContext = bootstrapContext;
        Class clazz = cl.loadClass(resourceAdapterClass);
        resourceAdapter = (ResourceAdapter) clazz.newInstance();
    }
    
    public ResourceAdapterWrapper(ResourceAdapter resourceAdapter, BootstrapContext bootstrapContext) {
        this.resourceAdapterClass = resourceAdapter.getClass().getName();
        this.bootstrapContext = bootstrapContext;
        this.resourceAdapter = resourceAdapter;
    }

    public String getResourceAdapterClass() {
        return resourceAdapterClass;
    }

    public void registerResourceAdapterAssociation(final ResourceAdapterAssociation resourceAdapterAssociation) throws ResourceException {
        resourceAdapterAssociation.setResourceAdapter(resourceAdapter);
    }

    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        throw new IllegalStateException("Don't call this");
    }

    public void stop() {
        throw new IllegalStateException("Don't call this");
    }

    //endpoint handling
    public void endpointActivation(final MessageEndpointFactory messageEndpointFactory, final ActivationSpec activationSpec) throws ResourceException {
        resourceAdapter.endpointActivation(messageEndpointFactory, activationSpec);
    }

    public void endpointDeactivation(final MessageEndpointFactory messageEndpointFactory, final ActivationSpec activationSpec) {
        resourceAdapter.endpointDeactivation(messageEndpointFactory, activationSpec);
    }

    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        return resourceAdapter.getXAResources(specs);
    }

    public void doStart() throws Exception {
        resourceAdapter.start(bootstrapContext);
    }

    public void doStop() {
        resourceAdapter.stop();
    }

    public void doFail() {
        resourceAdapter.stop();
    }

}
