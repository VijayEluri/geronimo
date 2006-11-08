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
package org.apache.geronimo.console.databasemanager.wizard;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.Arrays;
import java.util.Comparator;
import javax.enterprise.deploy.model.DDBean;
import javax.enterprise.deploy.model.DDBeanRoot;
import javax.enterprise.deploy.spi.DeploymentConfiguration;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.status.ProgressObject;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.portlet.PortletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.connector.deployment.jsr88.ConfigPropertySetting;
import org.apache.geronimo.connector.deployment.jsr88.ConnectionDefinition;
import org.apache.geronimo.connector.deployment.jsr88.ConnectionDefinitionInstance;
import org.apache.geronimo.connector.deployment.jsr88.ConnectionManager;
import org.apache.geronimo.connector.deployment.jsr88.Connector15DCBRoot;
import org.apache.geronimo.connector.deployment.jsr88.ConnectorDCB;
import org.apache.geronimo.connector.deployment.jsr88.ResourceAdapter;
import org.apache.geronimo.connector.deployment.jsr88.SinglePool;
import org.apache.geronimo.connector.outbound.PoolingAttributes;
import org.apache.geronimo.console.BasePortlet;
import org.apache.geronimo.console.ajax.ProgressInfo;
import org.apache.geronimo.console.util.PortletManager;
import org.apache.geronimo.converter.DatabaseConversionStatus;
import org.apache.geronimo.converter.JDBCPool;
import org.apache.geronimo.converter.bea.WebLogic81DatabaseConverter;
import org.apache.geronimo.converter.jboss.JBoss4DatabaseConverter;
import org.apache.geronimo.deployment.service.jsr88.EnvironmentData;
import org.apache.geronimo.deployment.tools.loader.ConnectorDeployable;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.kernel.management.State;
import org.apache.geronimo.kernel.proxy.GeronimoManagedBean;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.repository.FileWriteMonitor;
import org.apache.geronimo.kernel.repository.ListableRepository;
import org.apache.geronimo.kernel.repository.Repository;
import org.apache.geronimo.kernel.repository.WriteableRepository;
import org.apache.geronimo.kernel.util.XmlUtil;
import org.apache.geronimo.management.geronimo.JCAManagedConnectionFactory;
import org.apache.geronimo.management.geronimo.ResourceAdapterModule;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * A portlet that lets you configure and deploy JDBC connection pools.
 *
 * @version $Rev$ $Date$
 */
public class DatabasePoolPortlet extends BasePortlet {
    private final static Log log = LogFactory.getLog(DatabasePoolPortlet.class);
    private final static String[] SKIP_ENTRIES_WITH = new String[]{"geronimo", "tomcat", "tranql", "commons", "directory", "activemq"};
    private final static String DRIVER_SESSION_KEY = "org.apache.geronimo.console.dbpool.Drivers";
    private final static String CONFIG_SESSION_KEY = "org.apache.geronimo.console.dbpool.ConfigParam";
    private final static String DRIVER_INFO_URL    = "http://people.apache.org/~ammulder/driver-downloads.properties";
    private static final String LIST_VIEW            = "/WEB-INF/view/dbwizard/list.jsp";
    private static final String EDIT_VIEW            = "/WEB-INF/view/dbwizard/edit.jsp";
    private static final String SELECT_RDBMS_VIEW    = "/WEB-INF/view/dbwizard/selectDatabase.jsp";
    private static final String BASIC_PARAMS_VIEW    = "/WEB-INF/view/dbwizard/basicParams.jsp";
    private static final String CONFIRM_URL_VIEW     = "/WEB-INF/view/dbwizard/confirmURL.jsp";
    private static final String TEST_CONNECTION_VIEW = "/WEB-INF/view/dbwizard/testConnection.jsp";
    private static final String DOWNLOAD_VIEW        = "/WEB-INF/view/dbwizard/selectDownload.jsp";
    private static final String DOWNLOAD_STATUS_VIEW = "/WEB-INF/view/dbwizard/downloadStatus.jsp";
    private static final String SHOW_PLAN_VIEW       = "/WEB-INF/view/dbwizard/showPlan.jsp";
    private static final String IMPORT_UPLOAD_VIEW   = "/WEB-INF/view/dbwizard/importUpload.jsp";
    private static final String IMPORT_STATUS_VIEW   = "/WEB-INF/view/dbwizard/importStatus.jsp";
    private static final String USAGE_VIEW           = "/WEB-INF/view/dbwizard/usage.jsp";
    private static final String LIST_MODE            = "list";
    private static final String EDIT_MODE            = "edit";
    private static final String SELECT_RDBMS_MODE    = "rdbms";
    private static final String BASIC_PARAMS_MODE    = "params";
    private static final String CONFIRM_URL_MODE     = "url";
    private static final String TEST_CONNECTION_MODE = "test";
    private static final String SHOW_PLAN_MODE       = "plan";
    private static final String DOWNLOAD_MODE        = "download";
    private static final String DOWNLOAD_STATUS_MODE = "downloadStatus";
    private static final String EDIT_EXISTING_MODE   = "editExisting";
    private static final String SAVE_MODE            = "save";
    private static final String IMPORT_START_MODE    = "startImport";
    private static final String IMPORT_UPLOAD_MODE   = "importUpload";
    private static final String IMPORT_STATUS_MODE   = "importStatus";
    private static final String IMPORT_COMPLETE_MODE = "importComplete";
    private static final String WEBLOGIC_IMPORT_MODE = "weblogicImport";
    private static final String USAGE_MODE           = "usage";
    private static final String IMPORT_EDIT_MODE   = "importEdit";
    private static final String MODE_KEY = "mode";

    private PortletRequestDispatcher listView;
    private PortletRequestDispatcher editView;
    private PortletRequestDispatcher selectRDBMSView;
    private PortletRequestDispatcher basicParamsView;
    private PortletRequestDispatcher confirmURLView;
    private PortletRequestDispatcher testConnectionView;
    private PortletRequestDispatcher downloadView;
    private PortletRequestDispatcher downloadStatusView;
    private PortletRequestDispatcher planView;
    private PortletRequestDispatcher importUploadView;
    private PortletRequestDispatcher importStatusView;
    private PortletRequestDispatcher usageView;

    public void init(PortletConfig portletConfig) throws PortletException {
        super.init(portletConfig);
        listView = portletConfig.getPortletContext().getRequestDispatcher(LIST_VIEW);
        editView = portletConfig.getPortletContext().getRequestDispatcher(EDIT_VIEW);
        selectRDBMSView = portletConfig.getPortletContext().getRequestDispatcher(SELECT_RDBMS_VIEW);
        basicParamsView = portletConfig.getPortletContext().getRequestDispatcher(BASIC_PARAMS_VIEW);
        confirmURLView = portletConfig.getPortletContext().getRequestDispatcher(CONFIRM_URL_VIEW);
        testConnectionView = portletConfig.getPortletContext().getRequestDispatcher(TEST_CONNECTION_VIEW);
        downloadView = portletConfig.getPortletContext().getRequestDispatcher(DOWNLOAD_VIEW);
        downloadStatusView = portletConfig.getPortletContext().getRequestDispatcher(DOWNLOAD_STATUS_VIEW);
        planView = portletConfig.getPortletContext().getRequestDispatcher(SHOW_PLAN_VIEW);
        importUploadView = portletConfig.getPortletContext().getRequestDispatcher(IMPORT_UPLOAD_VIEW);
        importStatusView = portletConfig.getPortletContext().getRequestDispatcher(IMPORT_STATUS_VIEW);
        usageView = portletConfig.getPortletContext().getRequestDispatcher(USAGE_VIEW);
    }

    public void destroy() {
        listView = null;
        editView = null;
        selectRDBMSView = null;
        basicParamsView = null;
        confirmURLView = null;
        testConnectionView = null;
        downloadView = null;
        downloadStatusView = null;
        planView = null;
        importUploadView = null;
        importStatusView = null;
        usageView = null;
        super.destroy();
    }

    public DriverDownloader.DriverInfo[] getDriverInfo(PortletRequest request) {
        PortletSession session = request.getPortletSession(true);
        DriverDownloader.DriverInfo[] results = (DriverDownloader.DriverInfo[]) session.getAttribute(DRIVER_SESSION_KEY, PortletSession.APPLICATION_SCOPE);
        if(results == null) {
            DriverDownloader downloader = new DriverDownloader();
            try {
                results = downloader.loadDriverInfo(new URL(DRIVER_INFO_URL));
                session.setAttribute(DRIVER_SESSION_KEY, results, PortletSession.APPLICATION_SCOPE);
            } catch (MalformedURLException e) {
                log.error("Unable to download driver data", e);
                results = new DriverDownloader.DriverInfo[0];
            }
        }
        return results;
    }

    /**
     * Loads data about a resource adapter.  Depending on what we already have, may load
     * the name and description, but always loads the config property descriptions.
     * @param request            Pass it or die
     * @param rarPath            If we're creating a new RA, the path to identify it
     * @param displayName        If we're editing an existing RA, its name
     * @param adapterAbstractName  If we're editing an existing RA, its AbstractName
     */
    public ResourceAdapterParams getRARConfiguration(PortletRequest request, String rarPath, String displayName, String adapterAbstractName) {
        PortletSession session = request.getPortletSession(true);
        if(rarPath != null && !rarPath.equals("")) {
            ResourceAdapterParams results = (ResourceAdapterParams) session.getAttribute(CONFIG_SESSION_KEY+"-"+rarPath, PortletSession.APPLICATION_SCOPE);
            if(results == null) {
                results = loadConfigPropertiesByPath(request, rarPath);
                session.setAttribute(CONFIG_SESSION_KEY+"-"+rarPath, results, PortletSession.APPLICATION_SCOPE);
                session.setAttribute(CONFIG_SESSION_KEY+"-"+results.displayName, results, PortletSession.APPLICATION_SCOPE);
            }
            return results;
        } else if(displayName != null && !displayName.equals("") && adapterAbstractName != null && !adapterAbstractName.equals("")) {
            ResourceAdapterParams results = (ResourceAdapterParams) session.getAttribute(CONFIG_SESSION_KEY+"-"+displayName, PortletSession.APPLICATION_SCOPE);
            if(results == null) {
                results = loadConfigPropertiesByAbstractName(request, adapterAbstractName);
                session.setAttribute(CONFIG_SESSION_KEY+"-"+displayName, results, PortletSession.APPLICATION_SCOPE);
            }
            return results;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void processAction(ActionRequest actionRequest,
            ActionResponse actionResponse) throws PortletException, IOException {
        String mode = actionRequest.getParameter(MODE_KEY);
        if(mode.equals(IMPORT_UPLOAD_MODE)) {
            processImportUpload(actionRequest, actionResponse);
            actionResponse.setRenderParameter(MODE_KEY, IMPORT_STATUS_MODE);
            return;
        }
        PoolData data = new PoolData();
        data.load(actionRequest);
        if(mode.equals("process-"+SELECT_RDBMS_MODE)) {
            DatabaseDriver info = null;
            info = getDatabaseInfo(actionRequest, data);
            if(info != null) {
                data.rarPath = info.getRAR().toString();
                if(info.isXA()) {
                    data.adapterDisplayName="Unknown"; // will pick these up when we process the RA type in the render request
                    data.adapterDescription="Unknown";
                    actionResponse.setRenderParameter(MODE_KEY, EDIT_MODE);
                } else {
                    if(data.getDbtype().equals("Other")) {
                        actionResponse.setRenderParameter(MODE_KEY, EDIT_MODE);
                    } else {
                        data.driverClass = info.getDriverClassName();
                        data.urlPrototype = info.getURLPrototype();
                        actionResponse.setRenderParameter(MODE_KEY, BASIC_PARAMS_MODE);
                    }
                }
            } else {
                actionResponse.setRenderParameter(MODE_KEY, SELECT_RDBMS_MODE);
            }
        } else if(mode.equals("process-"+DOWNLOAD_MODE)) {
            String name = actionRequest.getParameter("driverName");
            DriverDownloader.DriverInfo[] drivers = getDriverInfo(actionRequest);
            DriverDownloader.DriverInfo found = null;
            for (int i = 0; i < drivers.length; i++) {
                DriverDownloader.DriverInfo driver = drivers[i];
                if(driver.getName().equals(name)) {
                    found = driver;
                    break;
                }
            }
            if(found != null) {
                data.jars = new String[] {found.getRepositoryURI()};
                WriteableRepository repo = PortletManager.getCurrentServer(actionRequest).getWritableRepositories()[0];
                final PortletSession session = actionRequest.getPortletSession();
                ProgressInfo progressInfo = new ProgressInfo();
                progressInfo.setMainMessage("Downloading " + found.getName());
                session.setAttribute(ProgressInfo.PROGRESS_INFO_KEY, progressInfo, PortletSession.APPLICATION_SCOPE);
                // Start the download monitoring
                new Thread(new Downloader(found, progressInfo, repo)).start();
                actionResponse.setRenderParameter(MODE_KEY, DOWNLOAD_STATUS_MODE);
            } else {
                actionResponse.setRenderParameter(MODE_KEY, DOWNLOAD_MODE);
            }
        } else if(mode.equals("process-"+DOWNLOAD_STATUS_MODE)) {
            if(data.getDbtype() == null || data.getDbtype().equals("Other")) {
                actionResponse.setRenderParameter(MODE_KEY, EDIT_MODE);
            } else {
                actionResponse.setRenderParameter(MODE_KEY, BASIC_PARAMS_MODE);
            }
        } else if(mode.equals("process-"+BASIC_PARAMS_MODE)) {
            DatabaseDriver info = null;
            info = getDatabaseInfo(actionRequest, data);
            if(info != null) {
                data.url = populateURL(info.getURLPrototype(), info.getURLParameters(), data.getUrlProperties());
            }
            if(attemptDriverLoad(actionRequest, data) != null) {
                actionResponse.setRenderParameter(MODE_KEY, CONFIRM_URL_MODE);
            } else {
                actionResponse.setRenderParameter("driverError", "Unable to load driver "+data.driverClass);
                actionResponse.setRenderParameter(MODE_KEY, BASIC_PARAMS_MODE);
            }
        } else if(mode.equals("process-"+CONFIRM_URL_MODE)) {
            String test = actionRequest.getParameter("test");
            if(test == null || test.equals("true")) {
                String result = null;
                String stack = null;
                try {
                    result = attemptConnect(actionRequest, data);
                } catch (Exception e) {
                    StringWriter writer = new StringWriter();
                    PrintWriter temp = new PrintWriter(writer);
                    e.printStackTrace(temp);
                    temp.flush();
                    temp.close();
                    stack = writer.getBuffer().toString();
                }
                if(result != null) actionResponse.setRenderParameter("connectResult", result);
                actionRequest.getPortletSession(true).setAttribute("connectError", stack);
                actionResponse.setRenderParameter(MODE_KEY, TEST_CONNECTION_MODE);
            } else {
                save(actionRequest, actionResponse, data, false);
            }
        } else if(mode.equals(SAVE_MODE)) {
            save(actionRequest, actionResponse, data, false);
        } else if(mode.equals(SHOW_PLAN_MODE)) {
            String plan = save(actionRequest, actionResponse, data, true);
            actionRequest.getPortletSession(true).setAttribute("deploymentPlan", plan);
            actionResponse.setRenderParameter(MODE_KEY, SHOW_PLAN_MODE);
        } else if(mode.equals(EDIT_EXISTING_MODE)) {
            final String name = actionRequest.getParameter("adapterAbstractName");
            loadConnectionFactory(actionRequest, name, data.getAbstractName(), data);
            actionResponse.setRenderParameter("adapterAbstractName", name);
            actionResponse.setRenderParameter(MODE_KEY, EDIT_MODE);
        } else if(mode.equals(SELECT_RDBMS_MODE)) {
            if(data.getAdapterDisplayName() == null) { // Set a default for a new pool
                data.adapterDisplayName = "TranQL Generic JDBC Resource Adapter";
            }
            actionResponse.setRenderParameter(MODE_KEY, mode);
        } else if(mode.equals(WEBLOGIC_IMPORT_MODE)) {
            String domainDir = actionRequest.getParameter("weblogicDomainDir");
            String libDir = actionRequest.getParameter("weblogicLibDir");
            try {
                DatabaseConversionStatus status = WebLogic81DatabaseConverter.convert(libDir, domainDir);
                actionRequest.getPortletSession(true).setAttribute("ImportStatus", new ImportStatus(status));
                actionResponse.setRenderParameter(MODE_KEY, IMPORT_STATUS_MODE);
            } catch (Exception e) {
                log.error("Unable to import", e);
                actionResponse.setRenderParameter("from", actionRequest.getParameter("from"));
                actionResponse.setRenderParameter(MODE_KEY, IMPORT_START_MODE);
            }
        } else if(mode.equals(IMPORT_START_MODE)) {
            actionResponse.setRenderParameter("from", actionRequest.getParameter("from"));
            actionResponse.setRenderParameter(MODE_KEY, mode);
        } else if(mode.equals(IMPORT_EDIT_MODE)) {
            ImportStatus status = getImportStatus(actionRequest);
            int index = Integer.parseInt(actionRequest.getParameter("importIndex"));
            status.setCurrentPoolIndex(index);
            loadImportedData(actionRequest, data, status.getCurrentPool());
            actionResponse.setRenderParameter(MODE_KEY, EDIT_MODE);
        } else if(mode.equals(IMPORT_COMPLETE_MODE)) {
            ImportStatus status = getImportStatus(actionRequest);
            log.warn("Import Results:"); //todo: create a screen for this
            log.warn("  "+status.getSkippedCount()+" ignored");
            log.warn("  "+status.getStartedCount()+" reviewed but not deployed");
            log.warn("  "+status.getPendingCount()+" not reviewed");
            log.warn("  "+status.getFinishedCount()+" deployed");
            actionRequest.getPortletSession().removeAttribute("ImportStatus");
        } else {
            actionResponse.setRenderParameter(MODE_KEY, mode);
        }
        data.store(actionResponse);
    }

    private static class Downloader implements Runnable {
        private WriteableRepository repo;
        private DriverDownloader.DriverInfo driver;
        private ProgressInfo progressInfo;

        public Downloader(DriverDownloader.DriverInfo driver, ProgressInfo progressInfo, WriteableRepository repo) {
            this.driver = driver;
            this.progressInfo = progressInfo;
            this.repo = repo;
        }

        public void run() {
            DriverDownloader downloader = new DriverDownloader();
            try {
                downloader.loadDriver(repo, driver, new FileWriteMonitor() {
                    private int fileSize;

                    public void writeStarted(String fileDescription, int fileSize) {
                        this.fileSize = fileSize;
                        log.info("Downloading "+fileDescription);
                    }

                    public void writeProgress(int bytes) {
                        int kbDownloaded = (int)Math.floor(bytes/1024);
                        if (fileSize > 0) {
                            int percent = (bytes*100)/fileSize;
                            progressInfo.setProgressPercent(percent);
                            progressInfo.setSubMessage(kbDownloaded + " / " + fileSize/1024 + " Kb downloaded");
                        } else {
                            progressInfo.setSubMessage(kbDownloaded + " Kb downloaded");
                        }
                    }

                    public void writeComplete(int bytes) {
                        log.info("Finished downloading "+bytes+" b");
                    }
                });
            } catch (IOException e) {
                log.error("Unable to download database driver", e);
            } finally {
                progressInfo.setFinished(true);
            }
        }
    }

    private void loadImportedData(PortletRequest request, PoolData data, ImportStatus.PoolProgress progress) throws PortletException {
        if(!progress.getType().equals(ImportStatus.PoolProgress.TYPE_XA)) {
            JDBCPool pool = (JDBCPool) progress.getPool();
            data.dbtype = "Other";
            data.adapterDisplayName = "TranQL Generic JDBC Resource Adapter";
            data.blockingTimeout = getImportString(pool.getBlockingTimeoutMillis());
            data.driverClass = pool.getDriverClass();
            data.idleTimeout = pool.getIdleTimeoutMillis() == null ? null : Integer.toString(pool.getIdleTimeoutMillis().intValue() / (60 * 1000));
            data.maxSize = getImportString(pool.getMaxSize());
            data.minSize = getImportString(pool.getMinSize());
            data.name = pool.getName();
            data.password = pool.getPassword();
            data.url = pool.getJdbcURL();
            data.user = pool.getUsername();
            if(pool.getDriverClass() != null) {
                DatabaseDriver info = getDatabaseInfoFromDriver(request, data);
                if(info != null) {
                    data.rarPath = info.getRAR().toString();
                    data.urlPrototype = info.getURLPrototype();
                } else {
                    throw new PortletException("Don't recognize database driver "+data.driverClass+"!");
                }
            }
        } else {
            //todo: handle XA
        }
    }

    private static String getImportString(Integer value) {
        return value == null ? null : value.toString();
    }

    private boolean processImportUpload(ActionRequest request, ActionResponse response) throws PortletException {
        String type = request.getParameter("importSource");
        response.setRenderParameter("importSource", type);
        if (!PortletFileUpload.isMultipartContent(request)) {
            throw new PortletException("Expected file upload");
        }

        PortletFileUpload uploader = new PortletFileUpload(new DiskFileItemFactory());
        try {
            List items = uploader.parseRequest(request);
            for (Iterator i = items.iterator(); i.hasNext();) {
                FileItem item = (FileItem) i.next();
                if (!item.isFormField()) {
                    File file = File.createTempFile("geronimo-import", "");
                    file.deleteOnExit();
                    log.debug("Writing database pool import file to "+file.getAbsolutePath());
                    item.write(file);
                    DatabaseConversionStatus status = processImport(file, type);
                    request.getPortletSession(true).setAttribute("ImportStatus", new ImportStatus(status));
                    return true;
                } else {
                    throw new PortletException("Not expecting any form fields");
                }
            }
        } catch(PortletException e) {
            throw e;
        } catch(Exception e) {
            throw new PortletException(e);
        }
        return false;
    }

    private DatabaseConversionStatus processImport(File importFile, String type) throws PortletException, IOException {
        if(type.equals("JBoss 4")) {
            return JBoss4DatabaseConverter.convert(new FileReader(importFile));
        } else if(type.equals("WebLogic 8.1")) {
            return WebLogic81DatabaseConverter.convert(new FileReader(importFile));
        } else {
            throw new PortletException("Unknown import type '"+type+"'");
        }
    }

    private ResourceAdapterParams loadConfigPropertiesByPath(PortletRequest request, String rarPath) {
        DeploymentManager mgr = PortletManager.getDeploymentManager(request);
        try {
            URL url = getRAR(request, rarPath).toURL();
            ConnectorDeployable deployable = new ConnectorDeployable(url);
            final DDBeanRoot ddBeanRoot = deployable.getDDBeanRoot();
            String adapterName = null, adapterDesc = null;
            String[] test = ddBeanRoot.getText("connector/display-name");
            if(test != null && test.length > 0) {
                adapterName = test[0];
            }
            test = ddBeanRoot.getText("connector/description");
            if(test != null && test.length > 0) {
                adapterDesc = test[0];
            }
            DDBean[] definitions = ddBeanRoot.getChildBean("connector/resourceadapter/outbound-resourceadapter/connection-definition");
            List configs = new ArrayList();
            if(definitions != null) {
                for (int i = 0; i < definitions.length; i++) {
                    DDBean definition = definitions[i];
                    String iface = definition.getText("connectionfactory-interface")[0];
                    if(iface.equals("javax.sql.DataSource")) {
                        DDBean[] beans = definition.getChildBean("config-property");
                        for (int j = 0; j < beans.length; j++) {
                            DDBean bean = beans[j];
                            String name = bean.getText("config-property-name")[0].trim();
                            String type = bean.getText("config-property-type")[0].trim();
                            test = bean.getText("config-property-value");
                            String value = test == null || test.length == 0 ? null : test[0].trim();
                            test = bean.getText("description");
                            String desc = test == null || test.length == 0 ? null : test[0].trim();
                            configs.add(new ConfigParam(name, type, desc, value));
                        }
                    }
                }
            }
            return new ResourceAdapterParams(adapterName, adapterDesc, (ConfigParam[]) configs.toArray(new ConfigParam[configs.size()]));
        } catch (Exception e) {
            log.error("Unable to read configuration properties", e);
            return null;
        } finally {
            if(mgr != null) mgr.release();
        }
    }

    private ResourceAdapterParams loadConfigPropertiesByAbstractName(PortletRequest request, String abstractName) {
        ResourceAdapterModule module = (ResourceAdapterModule) PortletManager.getManagedBean(request, new AbstractName(URI.create(abstractName)));
        String dd = module.getDeploymentDescriptor();
        DocumentBuilderFactory factory = XmlUtil.newDocumentBuilderFactory();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            final StringReader reader = new StringReader(dd);
            Document doc = builder.parse(new InputSource(reader));
            reader.close();
            Element elem = doc.getDocumentElement(); // connector
            String displayName = getFirstText(elem.getElementsByTagName("display-name"));
            String description = getFirstText(elem.getElementsByTagName("description"));
            elem = (Element) elem.getElementsByTagName("resourceadapter").item(0);
            elem = (Element) elem.getElementsByTagName("outbound-resourceadapter").item(0);
            NodeList defs = elem.getElementsByTagName("connection-definition");
            List all = new ArrayList();
            for(int i=0; i<defs.getLength(); i++) {
                final Element def = (Element)defs.item(i);
                String iface = getFirstText(def.getElementsByTagName("connectionfactory-interface")).trim();
                if(iface.equals("javax.sql.DataSource")) {
                    NodeList configs = def.getElementsByTagName("config-property");
                    for(int j=0; j<configs.getLength(); j++) {
                        Element config = (Element) configs.item(j);
                        String name = getFirstText(config.getElementsByTagName("config-property-name")).trim();
                        String type = getFirstText(config.getElementsByTagName("config-property-type")).trim();
                        String test = getFirstText(config.getElementsByTagName("config-property-value"));
                        String value = test == null ? null : test.trim();
                        test = getFirstText(config.getElementsByTagName("description"));
                        String desc = test == null ? null : test.trim();
                        all.add(new ConfigParam(name, type, desc, value));
                    }
                }
            }
            return new ResourceAdapterParams(displayName, description, (ConfigParam[]) all.toArray(new ConfigParam[all.size()]));
        } catch (Exception e) {
            log.error("Unable to read resource adapter DD", e);
            return null;
        }
    }

    private String getFirstText(NodeList list) {
        if(list.getLength() == 0) {
            return null;
        }
        Element first = (Element) list.item(0);
        StringBuffer buf = new StringBuffer();
        NodeList all = first.getChildNodes();
        for(int i=0; i<all.getLength(); i++) {
            Node node = all.item(i);
            if(node.getNodeType() == Node.TEXT_NODE) {
                buf.append(node.getNodeValue());
            }
        }
        return buf.toString();
    }

    private void loadConnectionFactory(ActionRequest actionRequest, String adapterName, String factoryName, PoolData data) {
    	AbstractName abstractAdapterName = new AbstractName(URI.create(adapterName));
    	AbstractName abstractFactoryName = new AbstractName(URI.create(factoryName));
    	
        ResourceAdapterModule adapter = (ResourceAdapterModule) PortletManager.getManagedBean(actionRequest,abstractAdapterName);  
        JCAManagedConnectionFactory factory = (JCAManagedConnectionFactory) PortletManager.getManagedBean(actionRequest, abstractFactoryName);
        data.adapterDisplayName = adapter.getDisplayName();
        data.adapterDescription = adapter.getDescription();
        try {
        	data.name = (String)abstractFactoryName.getName().get("name");
            if(data.isGeneric()) {
                data.url = (String) factory.getConfigProperty("ConnectionURL");
                data.driverClass = (String) factory.getConfigProperty("Driver");
                data.user = (String) factory.getConfigProperty("UserName");
                data.password = (String) factory.getConfigProperty("Password");
            } else {
                ResourceAdapterParams params = getRARConfiguration(actionRequest, data.getRarPath(), data.getAdapterDisplayName(), adapterName);
                for(int i=0; i<params.getConfigParams().length; i++) {
                    ConfigParam cp = params.getConfigParams()[i];
                    Object value = factory.getConfigProperty(cp.getName());
                    data.properties.put("property-"+cp.getName(), value == null ? null : value.toString());
                }
            }
        } catch (Exception e) {
            log.error("Unable to look up connection property", e);
        }
        //todo: push the lookup into ManagementHelper
        PoolingAttributes pool = (PoolingAttributes) factory.getConnectionManager();
        data.minSize = Integer.toString(pool.getPartitionMinSize());
        data.maxSize = Integer.toString(pool.getPartitionMaxSize());
        data.blockingTimeout = Integer.toString(pool.getBlockingTimeoutMilliseconds());
        data.idleTimeout = Integer.toString(pool.getIdleTimeoutMinutes());

    }

    protected void doView(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException {
        if (WindowState.MINIMIZED.equals(renderRequest.getWindowState())) {
            return;
        }
        try {
            String mode = renderRequest.getParameter(MODE_KEY);
            PoolData data = new PoolData();
            data.load(renderRequest);
            renderRequest.setAttribute("pool", data);
            // If not headed anywhere in particular, send to list
            if(mode == null || mode.equals("")) {
                mode = LIST_MODE;
            }
            // If headed to list but there's an import in progress, redirect to import status
            if(mode.equals(LIST_MODE) && getImportStatus(renderRequest) != null) {
                mode = IMPORT_STATUS_MODE;
            }

            if(mode.equals(LIST_MODE)) {
                renderList(renderRequest, renderResponse);
            } else if(mode.equals(EDIT_MODE)) {
                renderEdit(renderRequest, renderResponse, data);
            } else if(mode.equals(SELECT_RDBMS_MODE)) {
                renderSelectRDBMS(renderRequest, renderResponse);
            } else if(mode.equals(DOWNLOAD_MODE)) {
                renderDownload(renderRequest, renderResponse);
            } else if(mode.equals(DOWNLOAD_STATUS_MODE)) {
                renderDownloadStatus(renderRequest, renderResponse);
            } else if(mode.equals(BASIC_PARAMS_MODE)) {
                renderBasicParams(renderRequest, renderResponse, data);
            } else if(mode.equals(CONFIRM_URL_MODE)) {
                renderConfirmURL(renderRequest, renderResponse);
            } else if(mode.equals(TEST_CONNECTION_MODE)) {
                renderTestConnection(renderRequest, renderResponse);
            } else if(mode.equals(SHOW_PLAN_MODE)) {
                renderPlan(renderRequest, renderResponse, data);
            } else if(mode.equals(IMPORT_START_MODE)) {
                renderImportUploadForm(renderRequest, renderResponse);
            } else if(mode.equals(IMPORT_STATUS_MODE)) {
                renderImportStatus(renderRequest, renderResponse);
            } else if(mode.equals(USAGE_MODE)) {
                renderUsage(renderRequest, renderResponse);
            }
        } catch (Throwable e) {
            log.error("Unable to render portlet", e);
        }
    }

    private void renderUsage(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        usageView.include(request, response);
    }

    private void renderImportStatus(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        request.setAttribute("status", getImportStatus(request));
        populatePoolList(request);
        importStatusView.include(request, response);
    }

    private void renderImportUploadForm(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        request.setAttribute("from", request.getParameter("from"));
        importUploadView.include(request, response);
    }

    private void renderList(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException {
        populatePoolList(renderRequest);
        listView.include(renderRequest, renderResponse);
    }

    private void populatePoolList(PortletRequest renderRequest) {
        ResourceAdapterModule[] modules = PortletManager.getOutboundRAModules(renderRequest, "javax.sql.DataSource");
        List list = new ArrayList();
        for (int i = 0; i < modules.length; i++) {
            ResourceAdapterModule module = modules[i];
            AbstractName moduleName = PortletManager.getManagementHelper(renderRequest).getNameFor(module);
            
            JCAManagedConnectionFactory[] databases = PortletManager.getOutboundFactoriesForRA(renderRequest, module, "javax.sql.DataSource");
            for (int j = 0; j < databases.length; j++) {
                JCAManagedConnectionFactory db = databases[j];
              	AbstractName dbName =  PortletManager.getManagementHelper(renderRequest).getNameFor(db);
                list.add(new ConnectionPool(moduleName, dbName, (String)dbName.getName().get(NameFactory.J2EE_NAME), ((GeronimoManagedBean)db).getState()));
            }
        }
        Collections.sort(list);
        renderRequest.setAttribute("pools", list);
    }

    private void renderEdit(RenderRequest renderRequest, RenderResponse renderResponse, PoolData data) throws IOException, PortletException {
        if(data.abstractName == null || data.abstractName.equals("")) {
            loadDriverJARList(renderRequest);
        }
        if(!data.isGeneric()) {
            ResourceAdapterParams params = getRARConfiguration(renderRequest, data.getRarPath(), data.getAdapterDisplayName(), renderRequest.getParameter("adapterAbstractName"));
            data.adapterDisplayName = params.getDisplayName();
            data.adapterDescription = params.getDescription();
            Map map = new HashMap();
            boolean more = false;
            for (int i = 0; i < params.getConfigParams().length; i++) {
                ConfigParam param = params.getConfigParams()[i];
                if(!data.properties.containsKey("property-"+param.getName())) {
                    data.properties.put("property-"+param.getName(), param.getDefaultValue());
                    more = true;
                }
                map.put("property-"+param.getName(), param);
            }
            if(more) {
                data.loadPropertyNames();
            }
            renderRequest.setAttribute("ConfigParams", map);
        }
        editView.include(renderRequest, renderResponse);
    }

    private void renderSelectRDBMS(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException {
        renderRequest.setAttribute("databases", getAllDrivers(renderRequest));
        selectRDBMSView.include(renderRequest, renderResponse);
    }

    private void renderDownload(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException {
        renderRequest.setAttribute("drivers", getDriverInfo(renderRequest));
        downloadView.include(renderRequest, renderResponse);
    }

    private void renderDownloadStatus(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException {
        downloadStatusView.include(renderRequest, renderResponse);
    }

    private void renderBasicParams(RenderRequest renderRequest, RenderResponse renderResponse, PoolData data) throws IOException, PortletException {
        loadDriverJARList(renderRequest);
        // Make sure all properties available for the DB are listed
        DatabaseDriver info = getDatabaseInfo(renderRequest, data);
        if(info != null) {
            String[] params = info.getURLParameters();
            for (int i = 0; i < params.length; i++) {
                String param = params[i];
                final String key = "urlproperty-"+param;
                if(!data.getUrlProperties().containsKey(key)) {
                    data.getUrlProperties().put(key, param.equalsIgnoreCase("port") && info.getDefaultPort() > 0 ? new Integer(info.getDefaultPort()) : null);
                }
            }
        }
        // Pass on errors
        renderRequest.setAttribute("driverError", renderRequest.getParameter("driverError"));

        basicParamsView.include(renderRequest, renderResponse);
    }

    private void loadDriverJARList(RenderRequest renderRequest) {
        // List the available JARs
        List list = new ArrayList();
        ListableRepository[] repos = PortletManager.getCurrentServer(renderRequest).getRepositories();
        for (int i = 0; i < repos.length; i++) {
            ListableRepository repo = repos[i];

            SortedSet artifacts = repo.list();
            outer:
            for (Iterator iterator = artifacts.iterator(); iterator.hasNext();) {
                Artifact artifact = (Artifact) iterator.next();
                String test = artifact.toString();
                // todo should only test groupId and should check for long (org.apache.geronimo) and short form
                for (int k = 0; k < SKIP_ENTRIES_WITH.length; k++) {
                    String skip = SKIP_ENTRIES_WITH[k];
                    if(test.indexOf(skip) > -1) {
                        continue outer;
                    }
                }
                list.add(test);
            }
    }
        Collections.sort(list);
        renderRequest.setAttribute("availableJars", list);
    }

    private void renderConfirmURL(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException {
        confirmURLView.include(renderRequest, renderResponse);
    }

    private void renderTestConnection(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException {
        // Pass on results
        renderRequest.setAttribute("connectResult", renderRequest.getParameter("connectResult"));
        renderRequest.setAttribute("connectError", renderRequest.getPortletSession().getAttribute("connectError"));
        testConnectionView.include(renderRequest, renderResponse);
    }

    private void renderPlan(RenderRequest renderRequest, RenderResponse renderResponse, PoolData data) throws IOException, PortletException {
        // Pass on results
        renderRequest.setAttribute("deploymentPlan", renderRequest.getPortletSession().getAttribute("deploymentPlan"));
        // Digest the RAR URI
        String path = PortletManager.getRepositoryEntry(renderRequest, data.getRarPath()).getPath();
        String base = PortletManager.getCurrentServer(renderRequest).getServerInfo().getCurrentBaseDirectory();
        if(base != null && path.startsWith(base)) {
            path = path.substring(base.length());
            if(path.startsWith("/")) {
                path = path.substring(1);
            }
        } else {
            int pos = path.lastIndexOf('/');
            path = path.substring(pos+1);
        }
        renderRequest.setAttribute("rarRelativePath", path);

        planView.include(renderRequest, renderResponse);
    }

    private static String attemptConnect(PortletRequest request, PoolData data) throws SQLException, IllegalAccessException, InstantiationException {
        Class driverClass = attemptDriverLoad(request, data);
        Driver driver = (Driver) driverClass.newInstance();
        if(driver.acceptsURL(data.url)) {
            Properties props = new Properties();
            if(data.user != null) {
                props.put("user", data.user);
            }
            if(data.password != null) {
                props.put("password", data.password);
            }
            Connection con = null;
            try {
                con = driver.connect(data.url, props);
                final DatabaseMetaData metaData = con.getMetaData();
                return metaData.getDatabaseProductName()+" "+metaData.getDatabaseProductVersion();
            } finally {
                if(con != null) try{con.close();}catch(SQLException e) {}
            }
        } else throw new SQLException("Driver "+data.getDriverClass()+" does not accept URL "+data.url);
    }

    private static String save(PortletRequest request, ActionResponse response, PoolData data, boolean planOnly) {
        ImportStatus status = getImportStatus(request);
        if(data.abstractName == null || data.abstractName.equals("")) { // we're creating a new pool
            data.name = data.name.replaceAll("\\s", "");
            DeploymentManager mgr = PortletManager.getDeploymentManager(request);
            try {
                File rarFile = getRAR(request, data.getRarPath());
                ConnectorDeployable deployable = new ConnectorDeployable(rarFile.toURL());
                DeploymentConfiguration config = mgr.createConfiguration(deployable);
                final DDBeanRoot ddBeanRoot = deployable.getDDBeanRoot();
                Connector15DCBRoot root = (Connector15DCBRoot) config.getDConfigBeanRoot(ddBeanRoot);
                ConnectorDCB connector = (ConnectorDCB) root.getDConfigBean(ddBeanRoot.getChildBean(root.getXpaths()[0])[0]);
                
                EnvironmentData environment = new EnvironmentData();
                connector.setEnvironment(environment);
                org.apache.geronimo.deployment.service.jsr88.Artifact configId = new org.apache.geronimo.deployment.service.jsr88.Artifact();
                environment.setConfigId(configId);
                configId.setGroupId("console.dbpool");
                String artifactId = data.name;
                if(artifactId.indexOf('/') != -1) {
                    // slash in artifact-id results in invalid configuration-id and leads to deployment errors
                    artifactId = artifactId.replaceAll("/", "%2F");
                }
                configId.setArtifactId(artifactId);
                configId.setVersion("1.0");
                configId.setType("rar");

                String[] jars = data.getJars();
                org.apache.geronimo.deployment.service.jsr88.Artifact[] dependencies = new org.apache.geronimo.deployment.service.jsr88.Artifact[jars.length];
                for (int i=0; i<dependencies.length; i++) {
                	dependencies[i] = new org.apache.geronimo.deployment.service.jsr88.Artifact();
                }
                environment.setDependencies(dependencies);
                for (int i=0; i<dependencies.length; i++) {
                        Artifact tmp = Artifact.create(jars[i]);
                        dependencies[i].setGroupId(tmp.getGroupId());
                        dependencies[i].setArtifactId(tmp.getArtifactId());
                        dependencies[i].setVersion(tmp.getVersion().toString());
                        dependencies[i].setType(tmp.getType());
                }
               
                ResourceAdapter adapter = connector.getResourceAdapter()[0];
                ConnectionDefinition definition = new ConnectionDefinition();
                adapter.setConnectionDefinition(new ConnectionDefinition[]{definition});
                definition.setConnectionFactoryInterface("javax.sql.DataSource");
                ConnectionDefinitionInstance instance = new ConnectionDefinitionInstance();
                definition.setConnectionInstance(new ConnectionDefinitionInstance[]{instance});
                instance.setName(data.getName());
                ConfigPropertySetting[] settings = instance.getConfigPropertySetting();
                if(data.isGeneric()) { // it's a generic TranQL JDBC pool
                    for (int i = 0; i < settings.length; i++) {
                        ConfigPropertySetting setting = settings[i];
                        if(setting.getName().equals("UserName")) {
                            setting.setValue(data.user);
                        } else if(setting.getName().equals("Password")) {
                            setting.setValue(data.password);
                        } else if(setting.getName().equals("ConnectionURL")) {
                            setting.setValue(data.url);
                        } else if(setting.getName().equals("Driver")) {
                            setting.setValue(data.driverClass);
                        }
                    }
                } else { // it's an XA driver or non-TranQL RA
                    for (int i = 0; i < settings.length; i++) {
                        ConfigPropertySetting setting = settings[i];
                        String value = (String) data.properties.get("property-"+setting.getName());
                        setting.setValue(value == null ? "" : value);
                    }
                }
                ConnectionManager manager = instance.getConnectionManager();
                manager.setTransactionLocal(true);
                SinglePool pool = new SinglePool();
                manager.setPoolSingle(pool);
                pool.setMatchOne(true);
                // Max Size needs to be set before the minimum.  This is because 
                // the connection manager will constrain the minimum based on the 
                // current maximum value in the pool.  We might consider adding a  
                // setPoolConstraints method to allow specifying both at the same time.
                if(data.maxSize != null && !data.maxSize.equals("")) {
                    pool.setMaxSize(new Integer(data.maxSize));
                }
                if(data.minSize != null && !data.minSize.equals("")) {
                    pool.setMinSize(new Integer(data.minSize));
                }
                if(data.blockingTimeout != null && !data.blockingTimeout.equals("")) {
                    pool.setBlockingTimeoutMillis(new Integer(data.blockingTimeout));
                }
                if(data.idleTimeout != null && !data.idleTimeout.equals("")) {
                    pool.setIdleTimeoutMinutes(new Integer(data.idleTimeout));
                }
                
                if(planOnly) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    config.save(out);
                    out.close();
                    return new String(out.toByteArray(), "US-ASCII");
                } else {
                    File tempFile = File.createTempFile("console-deployment",".xml");
                    tempFile.deleteOnExit();
                    log.debug("Writing database pool deployment plan to "+tempFile.getAbsolutePath());
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile));
                    config.save(out);
                    out.flush();
                    out.close();
                    Target[] targets = mgr.getTargets();
                    ProgressObject po = mgr.distribute(targets, rarFile, tempFile);
                    waitForProgress(po);
                    if(po.getDeploymentStatus().isCompleted()) {
                        TargetModuleID[] ids = po.getResultTargetModuleIDs();
                        po = mgr.start(ids);
                        waitForProgress(po);
                        if(po.getDeploymentStatus().isCompleted()) {
                            ids = po.getResultTargetModuleIDs();
                            if(status != null) {
                                status.getCurrentPool().setName(data.getName());
                                status.getCurrentPool().setConfigurationName(ids[0].getModuleID());
                                status.getCurrentPool().setFinished(true);
                                response.setRenderParameter(MODE_KEY, IMPORT_STATUS_MODE);
                            }
                            System.out.println("Deployment completed successfully!");
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Unable to save connection pool", e);
            } finally {
                if(mgr != null) mgr.release();
            }
        } else { // We're saving updates to an existing pool
            if(planOnly) {
                throw new UnsupportedOperationException("Can't update a plan for an existing deployment");
            }
            try {
                JCAManagedConnectionFactory factory = (JCAManagedConnectionFactory) PortletManager.getManagedBean(request, new AbstractName(URI.create(data.getAbstractName())));
                if(data.isGeneric()) {
                    factory.setConfigProperty("ConnectionURL", data.getUrl());
                    factory.setConfigProperty("UserName", data.getUser());
                    factory.setConfigProperty("Password", data.getPassword());
                } else {
                    for (Iterator it = data.getProperties().entrySet().iterator(); it.hasNext();) {
                        Map.Entry entry = (Map.Entry) it.next();
                        factory.setConfigProperty(((String) entry.getKey()).substring("property-".length()), entry.getValue());
                    }
                }
                //todo: push the lookup into ManagementHelper
                PoolingAttributes pool = (PoolingAttributes) factory.getConnectionManager();
                pool.setPartitionMinSize(data.minSize == null || data.minSize.equals("") ? 0 : Integer.parseInt(data.minSize));
                pool.setPartitionMaxSize(data.maxSize == null || data.maxSize.equals("") ? 10 : Integer.parseInt(data.maxSize));
                pool.setBlockingTimeoutMilliseconds(data.blockingTimeout == null || data.blockingTimeout.equals("") ? 5000 : Integer.parseInt(data.blockingTimeout));
                pool.setIdleTimeoutMinutes(data.idleTimeout == null || data.idleTimeout.equals("") ? 15 : Integer.parseInt(data.idleTimeout));
            } catch (Exception e) {
                log.error("Unable to save connection pool", e);
            }
        }
        return null;
    }

    private static void waitForProgress(ProgressObject po) {
        while(po.getDeploymentStatus().isRunning()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static ImportStatus getImportStatus(PortletRequest request) {
        return (ImportStatus) request.getPortletSession(true).getAttribute("ImportStatus");
    }

    private static File getRAR(PortletRequest request, String rarPath) {
        org.apache.geronimo.kernel.repository.Artifact artifact = org.apache.geronimo.kernel.repository.Artifact.create(rarPath);
        Repository[] repos = PortletManager.getCurrentServer(request).getRepositories();
        for (int i = 0; i < repos.length; i++) {
            Repository repo = repos[i];
            File url = repo.getLocation(artifact);
            if (url != null) {
                if (url.exists() && url.canRead() && !url.isDirectory()) {
                    return url;
                }
            }
        }
        return null;
    }

    /**
     * WARNING: This method relies on having access to the same repository
     * URLs as the server uses.
     */
    private static Class attemptDriverLoad(PortletRequest request, PoolData data) {
        List list = new ArrayList();
        try {
        	String[] jars = data.getJars();
            if(jars == null) {
                log.error("Driver load failed since no jar files were selected.");
                return null;
            }
            ListableRepository[] repos = PortletManager.getCurrentServer(request).getRepositories();
        	
        	for (int i=0; i<jars.length; i++) {
        		org.apache.geronimo.kernel.repository.Artifact artifact = org.apache.geronimo.kernel.repository.Artifact.create(jars[i]);
        		for (int j=0; j<repos.length; j++) {
                    ListableRepository repo = repos[j];
                    File url = repo.getLocation(artifact);
                    if (url != null) {
                    	list.add(url.toURL());
                    }
        		}
        	}
            URLClassLoader loader = new URLClassLoader((URL[]) list.toArray(new URL[list.size()]), DatabasePoolPortlet.class.getClassLoader());
            try {
                return loader.loadClass(data.driverClass);
            } catch (ClassNotFoundException e) {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String populateURL(String url, String[] keys, Map properties) {
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            String value = (String) properties.get("urlproperty-"+key);
            if(value == null || value.equals("")) {
                int begin = url.indexOf("{"+key+"}");
                int end = begin + key.length() + 2;
                for(int j=begin-1; j>=0; j--) {
                    char c = url.charAt(j);
                    if(c == ';' || c == ':') {
                        begin = j;
                        break;
                    } else if(c == '/') {
                        if(url.length() > end && url.charAt(end) == '/') {
                            begin = j; // Don't leave // if foo is null for /<foo>/
                        }
                        break;
                    }
                }
                url = url.substring(0, begin)+url.substring(end);
            } else {
                if(value.indexOf('\\') != -1 || value.indexOf('$') != -1) {
                    // value contains backslash or dollar sign and needs preprocessing for replaceAll to work properly
                    StringBuffer temp = new StringBuffer();
                    char[] valueChars = value.toCharArray();
                    for(int j = 0; j < valueChars.length; ++j) {
                        if(valueChars[j] == '\\' || valueChars[j] == '$') {
                            temp.append('\\');
                        }
                        temp.append(valueChars[j]);
                    }
                    value = temp.toString();
                }
                url = url.replaceAll("\\{"+key+"\\}", value);
            }
        }
        return url;
    }

    private static DatabaseDriver[] getAllDrivers(PortletRequest request) {
        DatabaseDriver[] result = (DatabaseDriver[]) PortletManager.getGBeansImplementing(request, DatabaseDriver.class);
        Arrays.sort(result, new Comparator() {
            public int compare(Object o1, Object o2) {
                String name1 = ((DatabaseDriver)o1).getName();
                String name2 = ((DatabaseDriver)o2).getName();
                if(name1.equals("Other")) name1 = "zzzOther";
                if(name2.equals("Other")) name2 = "zzzOther";
                return name1.compareTo(name2);
            }
        });
        return result;                
    }

    private static DatabaseDriver getDatabaseInfo(PortletRequest request, PoolData data) {
        DatabaseDriver info = null;
        DatabaseDriver[] all = getAllDrivers(request);
        for (int i = 0; i < all.length; i++) {
            DatabaseDriver next = all[i];
            if(next.getName().equals(data.getDbtype())) {
                info = next;
                break;
            }
        }
        return info;
    }

    private static DatabaseDriver getDatabaseInfoFromDriver(PortletRequest request, PoolData data) {
        DatabaseDriver info = null;
        DatabaseDriver[] all = getAllDrivers(request);
        for (int i = 0; i < all.length; i++) {
            DatabaseDriver next = all[i];
            if(next.getDriverClassName() != null && next.getDriverClassName().equals(data.getDriverClass())) {
                info = next;
                break;
            }
        }
        return info;
    }

    public static class PoolData implements Serializable {
        private String name;
        private String dbtype;
        private String user;
        private String password;
        private Map properties = new HashMap(); // Configuration for non-Generic drivers
        private Map urlProperties = new HashMap(); // URL substitution for Generic drivers
        private Map propertyNames; //todo: store these in the ConfigParam instead
        private String driverClass;
        private String url;
        private String urlPrototype;
        private String[] jars;
        private String minSize;
        private String maxSize;
        private String blockingTimeout;
        private String idleTimeout;
        private String abstractName;
        private String adapterDisplayName;
        private String adapterDescription;
        private String rarPath;
        private String importSource;
        private Map abstractNameMap; // generated as needed, don't need to read/write it

        public void load(PortletRequest request) {
            name = request.getParameter("name");
            if(name != null && name.equals("")) name = null;
            driverClass = request.getParameter("driverClass");
            if(driverClass != null && driverClass.equals("")) driverClass = null;
            dbtype = request.getParameter("dbtype");
            if(dbtype != null && dbtype.equals("")) dbtype = null;
            user = request.getParameter("user");
            if(user != null && user.equals("")) user = null;
            password = request.getParameter("password");
            if(password != null && password.equals("")) password = null;
            url = request.getParameter("url");
            if(url != null && url.equals("")) {
                url = null;
            } else if(url != null && url.startsWith("URLENCODED")) {
                try {
                    url = URLDecoder.decode(url.substring(10), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("Unable to decode URL", e);
                } catch(IllegalArgumentException e) { // not encoded after all??
                    url = url.substring(10);
                }
            }
            urlPrototype = request.getParameter("urlPrototype");
            if(urlPrototype != null && urlPrototype.equals("")) urlPrototype = null;
            jars = request.getParameterValues("jars");
            minSize = request.getParameter("minSize");
            if(minSize != null && minSize.equals("")) minSize = null;
            maxSize = request.getParameter("maxSize");
            if(maxSize != null && maxSize.equals("")) maxSize = null;
            blockingTimeout = request.getParameter("blockingTimeout");
            if(blockingTimeout != null && blockingTimeout.equals("")) blockingTimeout = null;
            idleTimeout = request.getParameter("idleTimeout");
            if(idleTimeout != null && idleTimeout.equals("")) idleTimeout = null;
            abstractName = request.getParameter("abstractName");
            if(abstractName != null && abstractName.equals("")) abstractName = null;
            adapterDisplayName = request.getParameter("adapterDisplayName");
            if(adapterDisplayName != null && adapterDisplayName.equals("")) adapterDisplayName = null;
            adapterDescription = request.getParameter("adapterDescription");
            if(adapterDescription != null && adapterDescription.equals("")) adapterDescription = null;
            rarPath = request.getParameter("rarPath");
            if(rarPath != null && rarPath.equals("")) rarPath = null;
            importSource = request.getParameter("importSource");
            if(importSource != null && importSource.equals("")) importSource = null;
            Map map = request.getParameterMap();
            propertyNames = new HashMap();
            for (Iterator it = map.keySet().iterator(); it.hasNext();) {
                String key = (String) it.next();
                if(key.startsWith("urlproperty-")) {
                    urlProperties.put(key, request.getParameter(key));
                } else if(key.startsWith("property-")) {
                    properties.put(key, request.getParameter(key));
                    propertyNames.put(key, getPropertyName(key));
                }
            }
        }

        public void loadPropertyNames() {
            propertyNames = new HashMap();
            for (Iterator it = properties.keySet().iterator(); it.hasNext();) {
                String key = (String) it.next();
                propertyNames.put(key, getPropertyName(key));
            }
        }

        private static String getPropertyName(String key) {
            int pos = key.indexOf('-');
            key = Character.toUpperCase(key.charAt(pos+1))+key.substring(pos+2);
            StringBuffer buf = new StringBuffer();
            pos = 0;
            for(int i=1; i<key.length(); i++) {
                if(Character.isUpperCase(key.charAt(i))) {
                    if(Character.isUpperCase(key.charAt(i-1))) { // ongoing capitalized word

                    } else { // start of a new word
                        buf.append(key.substring(pos, i)).append(" ");
                        pos = i;
                    }
                } else {
                    if(Character.isUpperCase(key.charAt(i-1)) && i-pos > 1) { // first lower-case after a series of caps
                        buf.append(key.substring(pos, i-1)).append(" ");
                        pos = i-1;
                    }
                }
            }
            buf.append(key.substring(pos));
            return buf.toString();
        }

        public void store(ActionResponse response) {
            if(name != null) response.setRenderParameter("name", name);
            if(dbtype != null) response.setRenderParameter("dbtype", dbtype);
            if(driverClass != null) response.setRenderParameter("driverClass", driverClass);
            if(user != null) response.setRenderParameter("user", user);
            if(password != null) response.setRenderParameter("password", password);
            if(url != null) { // attempt to work around Pluto/Tomcat error with ; in a stored value
                try {
                    response.setRenderParameter("url", "URLENCODED"+URLEncoder.encode(url, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("Unable to encode URL", e);
                }
            }
            if(urlPrototype != null) response.setRenderParameter("urlPrototype", urlPrototype);
            if(jars != null) response.setRenderParameter("jars", jars);
            if(minSize != null) response.setRenderParameter("minSize", minSize);
            if(maxSize != null) response.setRenderParameter("maxSize", maxSize);
            if(blockingTimeout != null) response.setRenderParameter("blockingTimeout", blockingTimeout);
            if(idleTimeout != null) response.setRenderParameter("idleTimeout", idleTimeout);
            if(abstractName != null) response.setRenderParameter("abstractName", abstractName);
            if(adapterDisplayName != null) response.setRenderParameter("adapterDisplayName", adapterDisplayName);
            if(adapterDescription != null) response.setRenderParameter("adapterDescription", adapterDescription);
            if(importSource != null) response.setRenderParameter("importSource", importSource);
            if(rarPath != null) response.setRenderParameter("rarPath", rarPath);
            for (Iterator it = urlProperties.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                if(entry.getValue() != null) {
                    response.setRenderParameter((String)entry.getKey(), (String)entry.getValue());
                }
            }
            for (Iterator it = properties.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                if(entry.getValue() != null) {
                    response.setRenderParameter((String)entry.getKey(), (String)entry.getValue());
                }
            }
        }

        public String getName() {
            return name;
        }

        public String getDbtype() {
            return dbtype;
        }

        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }

        public Map getProperties() {
            return properties;
        }

        public Map getPropertyNames() {
            return propertyNames;
        }

        public Map getUrlProperties() {
            return urlProperties;
        }

        public String getUrl() {
            return url;
        }

        public String[] getJars() {
        	return jars;
        }

        public String getMinSize() {
            return minSize;
        }

        public String getMaxSize() {
            return maxSize;
        }

        public String getBlockingTimeout() {
            return blockingTimeout;
        }

        public String getIdleTimeout() {
            return idleTimeout;
        }

        public String getDriverClass() {
            return driverClass;
        }

        public String getUrlPrototype() {
            return urlPrototype;
        }

        public String getAbstractName() {
            return abstractName;
        }

        public String getAdapterDisplayName() {
            return adapterDisplayName;
        }

        public String getAdapterDescription() {
            return adapterDescription;
        }

        public String getRarPath() {
            return rarPath;
        }

        public boolean isGeneric() {
            //todo: is there any better way to tell?
            return adapterDisplayName == null || adapterDisplayName.equals("TranQL Generic JDBC Resource Adapter");
        }

        public String getImportSource() {
            return importSource;
        }

        public Map getAbstractNameMap() {
            if(abstractName == null) return Collections.EMPTY_MAP;
            if(abstractNameMap != null) return abstractNameMap;
            AbstractName name = new AbstractName(URI.create(abstractName));
            abstractNameMap = new HashMap(name.getName());
            abstractNameMap.put("domain", name.getObjectName().getDomain());
            abstractNameMap.put("groupId", name.getArtifact().getGroupId());
            abstractNameMap.put("artifactId", name.getArtifact().getArtifactId());
            abstractNameMap.put("type", name.getArtifact().getType());
            abstractNameMap.put("version", name.getArtifact().getVersion().toString());
            return abstractNameMap;
        }
    }

    public static class ConnectionPool implements Serializable, Comparable {
        private final String adapterAbstractName;
        private final String factoryAbstractName;
        private final String name;
        private final String parentName;
        private final int state;

        public ConnectionPool(AbstractName adapterAbstractName, AbstractName factoryAbstractName, String name, int state) {
            this.adapterAbstractName = adapterAbstractName.toURI().toString();
            String parent = (String)adapterAbstractName.getName().get(NameFactory.J2EE_APPLICATION);
            if(parent != null && parent.equals("null")) {
                parent = null;
            }
            parentName = parent;
            this.factoryAbstractName = factoryAbstractName.toURI().toString();
            this.name = name;
            this.state = state;
        }

        public String getAdapterAbstractName() {
            return adapterAbstractName;
        }

        public String getFactoryAbstractName() {
            return factoryAbstractName;
        }

        public String getName() {
            return name;
        }

        public String getParentName() {
            return parentName;
        }

        public int getState() {
            return state;
        }

        public String getStateName() {
            return State.toString(state);
        }

        public int compareTo(Object o) {
            final ConnectionPool pool = (ConnectionPool)o;
            int names = name.compareTo(pool.name);
            if(parentName == null) {
                if(pool.parentName == null) {
                    return names;
                } else {
                    return -1;
                }
            } else {
                if(pool.parentName == null) {
                    return 1;
                } else {
                    int test = parentName.compareTo(pool.parentName);
                    if(test != 0) {
                        return test;
                    } else {
                        return names;
                    }
                }
            }
        }
    }

    public static class ResourceAdapterParams {
        private String displayName;
        private String description;
        private ConfigParam[] configParams;

        public ResourceAdapterParams(String displayName, String description, ConfigParam[] configParams) {
            this.displayName = displayName;
            this.description = description;
            this.configParams = configParams;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public ConfigParam[] getConfigParams() {
            return configParams;
        }
    }

    public static class ConfigParam {
        private String name;
        private String type;
        private String description;
        private String defaultValue;

        public ConfigParam(String name, String type, String description, String defaultValue) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }
}
