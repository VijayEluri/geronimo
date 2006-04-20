package org.apache.geronimo.tomcat.deployment;

import java.io.File;
import java.net.URL;
import java.util.Collections;

import junit.framework.TestCase;
import org.apache.geronimo.deployment.xbeans.ArtifactType;
import org.apache.geronimo.deployment.xbeans.EnvironmentType;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.j2ee.deployment.WebServiceBuilder;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.kernel.Jsr77Naming;
import org.apache.geronimo.kernel.Naming;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.schema.SchemaConversionUtils;
import org.apache.geronimo.xbeans.geronimo.naming.GerResourceRefType;
import org.apache.geronimo.xbeans.geronimo.web.GerWebAppDocument;
import org.apache.geronimo.xbeans.geronimo.web.GerWebAppType;
import org.apache.geronimo.xbeans.geronimo.web.tomcat.TomcatWebAppType;

/**
 */
public class PlanParsingTest extends TestCase {
    private ClassLoader classLoader = this.getClass().getClassLoader();

    private Naming naming = new Jsr77Naming();
    private Artifact baseId = new Artifact("test", "base", "1", "car");
    private AbstractName baseRootName = naming.createRootName(baseId, "root", NameFactory.SERVICE_MODULE);
    private AbstractNameQuery tomcatContainerObjectName = new AbstractNameQuery(naming.createChildName(baseRootName, "TomcatContainer", NameFactory.GERONIMO_SERVICE));
    private WebServiceBuilder webServiceBuilder = null;
    private Environment defaultEnvironment = new Environment();
    private TomcatModuleBuilder builder;

    protected void setUp() throws Exception {
        builder = new TomcatModuleBuilder(defaultEnvironment, false, tomcatContainerObjectName, Collections.singleton(webServiceBuilder), null);
    }

    public void testResourceRef() throws Exception {
        URL resourceURL = classLoader.getResource("plans/plan1.xml");
        File resourcePlan = new File(resourceURL.getFile());
        assertTrue(resourcePlan.exists());
        TomcatWebAppType tomcatWebApp = builder.getTomcatWebApp(resourcePlan, null, true, null, null);
        assertEquals(1, tomcatWebApp.getResourceRefArray().length);
    }

    public void testConstructPlan() throws Exception {
        GerWebAppDocument tomcatWebAppDoc = GerWebAppDocument.Factory.newInstance();
        GerWebAppType tomcatWebAppType = tomcatWebAppDoc.addNewWebApp();
        EnvironmentType environmentType = tomcatWebAppType.addNewEnvironment();
        ArtifactType artifactType = environmentType.addNewConfigId();
        artifactType.setArtifactId("foo");

        tomcatWebAppType.setContextPriorityClassloader(false);
        GerResourceRefType ref = tomcatWebAppType.addNewResourceRef();
        ref.setRefName("ref");
        ref.setResourceLink("target");

        SchemaConversionUtils.validateDD(tomcatWebAppType);
//        System.out.println(tomcatWebAppType.toString());
    }

}
