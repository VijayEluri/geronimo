/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.corba.deployment;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.jar.JarFile;
import java.io.File;
import java.net.URI;
import java.net.URL;

import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.corba.TSSLinkGBean;
import org.apache.geronimo.deployment.ModuleIDBuilder;
import org.apache.geronimo.deployment.service.EnvironmentBuilder;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.j2ee.deployment.EARContext;
import org.apache.geronimo.j2ee.deployment.Module;
import org.apache.geronimo.j2ee.deployment.ModuleBuilderExtension;
import org.apache.geronimo.j2ee.deployment.WebServiceBuilder;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.kernel.GBeanAlreadyExistsException;
import org.apache.geronimo.kernel.GBeanNotFoundException;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.Naming;
import org.apache.geronimo.kernel.config.ConfigurationModuleType;
import org.apache.geronimo.kernel.config.ConfigurationStore;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.naming.deployment.ENCConfigBuilder;
import org.apache.geronimo.openejb.deployment.EjbModule;
import org.apache.openejb.assembler.classic.EnterpriseBeanInfo;

import org.apache.geronimo.openejb.xbeans.ejbjar.OpenejbGeronimoEjbJarType;
import org.apache.geronimo.openejb.xbeans.ejbjar.OpenejbTssLinkType;

/**
 * @version $Rev$ $Date$
 */
public class CorbaModuleBuilderExtension implements ModuleBuilderExtension {
    // our default environment 
    protected Environment defaultEnvironment;

    public CorbaModuleBuilderExtension() throws Exception {
        this(null);
    }

    public CorbaModuleBuilderExtension(Environment defaultEnvironment) {
        this.defaultEnvironment = defaultEnvironment;
    }

    public void createModule(Module module, File plan, JarFile moduleFile, Naming naming, ModuleIDBuilder idBuilder) throws DeploymentException {
    }

    /**
     * Early module creation.  If this module contains
     * and CORBA enablement links, the corba default 
     * environment is merged in.
     * 
     * @param module     The module being deployed.
     * @param plan       The module plan
     * @param moduleFile The jar file containing the module.
     * @param targetPath The module path.
     * @param specDDUrl  The schema information.
     * @param environment
     *                   The current environment (used for the merge).
     * @param moduleContextInfo
     *                   The module context.
     * @param earName    The name of the ear file.
     * @param naming     The naming context.
     * @param idBuilder
     * 
     * @exception DeploymentException
     */
    public void createModule(Module module, Object plan, JarFile moduleFile, String targetPath, URL specDDUrl, Environment environment, Object moduleContextInfo, AbstractName earName, Naming naming, ModuleIDBuilder idBuilder) throws DeploymentException {
        if (module.getType() != ConfigurationModuleType.EJB) {
            return;
        }
        
        // if we have a default environment specified, we merge it in, but only if 
        // this module has tss links. 
        if (this.defaultEnvironment != null) {
            EjbModule ejbModule = (EjbModule) module;
            OpenejbGeronimoEjbJarType jarInfo = ejbModule.getVendorDD(); 

            OpenejbTssLinkType[] links = jarInfo.getTssLinkArray(); 
            if (links != null && links.length > 0) {
                EnvironmentBuilder.mergeEnvironments(environment, this.defaultEnvironment);
            }
        }        
    }

    public void installModule(JarFile earFile, EARContext earContext, Module module, Collection configurationStores, ConfigurationStore targetConfigurationStore, Collection repository) throws DeploymentException {
    }

    public void initContext(EARContext earContext, Module module, ClassLoader cl) throws DeploymentException {
    }

    /**
     * Add any GBeans to the deployment that might be 
     * required by the presence of tss-link definitions
     * in the ejb plans.
     * 
     * @param earContext The earContext of the module deployment.
     * @param module     The module being deployed.
     * @param cl         The module class loader instance.
     * @param repository The repository.
     * 
     * @exception DeploymentException
     *                   Thrown if any of the tss-link information cannot
     *                   be resolved (missing ejb or TSSBean).
     */
    public void addGBeans(EARContext earContext, Module module, ClassLoader cl, Collection repository) throws DeploymentException {

        if (module.getType() != ConfigurationModuleType.EJB) {
            return;
        }
        EjbModule ejbModule = (EjbModule) module;
        OpenejbGeronimoEjbJarType jarInfo = ejbModule.getVendorDD(); 

        OpenejbTssLinkType[] links = jarInfo.getTssLinkArray(); 
        // if there are no links, then there's nothing to do. 
        if (links == null || links.length == 0) {
            return; 
        }
        
        URI moduleURI = module.getModuleURI();
        String moduleString = moduleURI == null ? null : moduleURI.toString();
        
        for (OpenejbTssLinkType link : links) {
            AbstractName tssBeanName = resolveTssBean(earContext, link.getTssName(), moduleString); 
            AbstractName ejbName = resolveEjb(earContext, ejbModule, link.getEjbName());
            
            AbstractName tssLinkName = earContext.getNaming().createChildName(ejbName, link.getTssName(), NameFactory.CORBA_TSS_LINK);
            GBeanData tssLinkData = new GBeanData(tssLinkName, TSSLinkGBean.GBEAN_INFO);
            tssLinkData.setAttribute("jndiNames", link.getJndiNameArray());
            tssLinkData.setReferencePattern("EJB", ejbName);
            tssLinkData.setReferencePattern("TSSBean", tssBeanName);
            try {
                earContext.addGBean(tssLinkData);
            } catch (GBeanAlreadyExistsException e) {
                throw new DeploymentException("tss link gbean already present", e);
            }
        }
    }
    
    /**
     * Resolve a TSSBean name specified in a tss-link 
     * item to the bean's abstract name.  
     * 
     * @param context The ear context for the module were processing
     * @param name    The target name of the TSSBean.
     * @param module  The module name used to qualifiy the look ups.
     * 
     * @return An AbstractName for the target TSSBean. 
     * @exception DeploymentException
     *                   Thrown if the target TSSBean could not be located.
     */
    private AbstractName resolveTssBean(EARContext context, String name, String module) throws DeploymentException {
        AbstractNameQuery tssBeanName = ENCConfigBuilder.buildAbstractNameQuery(null, module, name, NameFactory.CORBA_TSS, NameFactory.EJB_MODULE);
        try {
            return context.findGBean(tssBeanName);
        } catch (GBeanNotFoundException e) {
            tssBeanName = ENCConfigBuilder.buildAbstractNameQuery(null, null, name, NameFactory.CORBA_TSS, null);
            try {
                return context.findGBean(tssBeanName);
            } catch (GBeanNotFoundException e1) {
                throw new DeploymentException("No tss bean " + name + " not found for module " + module, e);
            }
        }
    }
    
    /**
     * Resolve an EJB name used in a tss-link element to
     * the abstract name for that EJB.  The EJB must be part
     * of the current module bean set to be resolveable.
     * 
     * @param context   The ear context used for resolution.
     * @param ejbModule The EJBModule we're currently processing.
     * @param name      The name of the target EJB.
     * 
     * @return An AbstractName for the referenced EJB.
     * @exception DeploymentException
     *                   Thrown if the ejb does not exist in the current
     *                   module.
     */
    private AbstractName resolveEjb(EARContext earContext, EjbModule ejbModule, String name) throws DeploymentException {
        for (EnterpriseBeanInfo bean : ejbModule.getEjbJarInfo().enterpriseBeans) {
            // search for the target ejb 
            if (name.equals(bean.ejbName)) {
                switch (bean.type) {
                    case EnterpriseBeanInfo.STATELESS: {
                        return earContext.getNaming().createChildName(ejbModule.getModuleName(), name, NameFactory.STATELESS_SESSION_BEAN);
                    }
                    case EnterpriseBeanInfo.STATEFUL: {
                        return earContext.getNaming().createChildName(ejbModule.getModuleName(), name, NameFactory.STATEFUL_SESSION_BEAN);
                    }
                    case EnterpriseBeanInfo.ENTITY: {
                        return earContext.getNaming().createChildName(ejbModule.getModuleName(), name, NameFactory.ENTITY_BEAN);
                    }
                    case EnterpriseBeanInfo.MESSAGE: {
                        return earContext.getNaming().createChildName(ejbModule.getModuleName(), name, NameFactory.MESSAGE_DRIVEN_BEAN);
                    }
                }
            }
        }
        throw new DeploymentException("EJB " + name + " not found for module " + ejbModule.getModuleName());
    }


    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoBuilder = GBeanInfoBuilder.createStatic(CorbaModuleBuilderExtension.class, NameFactory.MODULE_BUILDER);
        infoBuilder.addInterface(ModuleBuilderExtension.class);
        infoBuilder.addAttribute("defaultEnvironment", Environment.class, true, true);

        infoBuilder.setConstructor(new String[]{"defaultEnvironment"});

        GBEAN_INFO = infoBuilder.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}

