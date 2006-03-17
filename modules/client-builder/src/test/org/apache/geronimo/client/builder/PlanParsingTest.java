package org.apache.geronimo.client.builder;

import java.io.File;

import junit.framework.TestCase;
import org.apache.geronimo.schema.SchemaConversionUtils;
import org.apache.geronimo.xbeans.geronimo.client.GerApplicationClientDocument;
import org.apache.geronimo.xbeans.geronimo.client.GerApplicationClientType;
import org.apache.geronimo.xbeans.geronimo.naming.GerResourceRefType;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.deployment.xbeans.EnvironmentType;
import org.apache.geronimo.deployment.xbeans.ArtifactType;

/**
 */
public class PlanParsingTest extends TestCase {

    private AppClientModuleBuilder builder;
    File basedir = new File(System.getProperty("basedir", "."));

    protected void setUp() throws Exception {
        builder = new AppClientModuleBuilder(new Environment(), null, null, null, null, null, null, null, null);
    }

    public void testResourceRef() throws Exception {
        File resourcePlan = new File(basedir, "src/test-resources/plans/plan1.xml");
        assertTrue(resourcePlan.exists());
        GerApplicationClientType appClient = builder.getGeronimoAppClient(resourcePlan, null, true, null, null, null);
        assertEquals(1, appClient.getResourceRefArray().length);
    }

    public void testConstructPlan() throws Exception {
        GerApplicationClientDocument appClientDoc = GerApplicationClientDocument.Factory.newInstance();
        GerApplicationClientType appClient = appClientDoc.addNewApplicationClient();
        EnvironmentType clientEnvironmentType = appClient.addNewClientEnvironment();
        ArtifactType clientId = clientEnvironmentType.addNewConfigId();
        clientId.setGroupId("group");
        clientId.setArtifactId("artifact");
        EnvironmentType serverEnvironmentType = appClient.addNewServerEnvironment();
        serverEnvironmentType.setConfigId(clientId);

        GerResourceRefType ref = appClient.addNewResourceRef();
        ref.setRefName("ref");
        ref.setResourceLink("target");

        SchemaConversionUtils.validateDD(appClient);
//        System.out.println(appClient.toString());
    }

    public void testConnectorInclude() throws Exception {
        File resourcePlan = new File(basedir, "src/test-resources/plans/plan2.xml");
        assertTrue(resourcePlan.exists());
        GerApplicationClientType appClient = builder.getGeronimoAppClient(resourcePlan, null, true, null, null, null);
        assertEquals(1, appClient.getResourceRefArray().length);
        assertEquals(1, appClient.getResourceArray().length);
    }
}
