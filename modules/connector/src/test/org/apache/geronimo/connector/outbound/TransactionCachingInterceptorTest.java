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

package org.apache.geronimo.connector.outbound;

import javax.resource.ResourceException;
import javax.transaction.TransactionManager;

import org.apache.geronimo.connector.outbound.connectiontracking.ConnectionTracker;
import org.apache.geronimo.transaction.ContainerTransactionContext;
import org.apache.geronimo.transaction.TransactionContext;
import org.apache.geronimo.transaction.UnspecifiedTransactionContext;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;

/**
 *
 *
 * @version $Revision: 1.7 $ $Date: 2004/05/06 03:59:56 $
 *
 * */
public class TransactionCachingInterceptorTest extends ConnectionInterceptorTestUtils
        implements ConnectionTracker {

    private TransactionManager transactionManager;
    private TransactionCachingInterceptor transactionCachingInterceptor;

    protected void setUp() throws Exception {
        super.setUp();
        transactionManager = new TransactionManagerImpl();
        transactionCachingInterceptor = new TransactionCachingInterceptor(this);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        transactionManager = null;
        transactionCachingInterceptor = null;
    }

    public void testGetConnectionInTransaction() throws Exception {
        ContainerTransactionContext transactionContext = new ContainerTransactionContext(transactionManager);
        TransactionContext.setContext(transactionContext);
        transactionContext.begin();
        ConnectionInfo connectionInfo1 = makeConnectionInfo();
        transactionCachingInterceptor.getConnection(connectionInfo1);
        assertTrue("Expected to get an initial connection", obtainedConnectionInfo != null);
        assertTrue("Expected nothing returned yet", returnedConnectionInfo == null);
        assertTrue("Expected the same ManagedConnectionInfo in the TransactionContext as was returned",
                connectionInfo1.getManagedConnectionInfo()
                == transactionContext.getManagedConnectionInfo(transactionCachingInterceptor));
        obtainedConnectionInfo = null;
        ConnectionInfo connectionInfo2 = new ConnectionInfo();
        transactionCachingInterceptor.getConnection(connectionInfo2);
        assertTrue("Expected to not get a second connection", obtainedConnectionInfo == null);
        assertTrue("Expected nothing returned yet", returnedConnectionInfo == null);
        assertTrue("Expected the same ManagedConnectionInfo in both ConnectionInfos",
                connectionInfo1.getManagedConnectionInfo() == connectionInfo2.getManagedConnectionInfo());
        assertTrue("Expected the same ManagedConnectionInfo in the TransactionContext as was returned",
                connectionInfo1.getManagedConnectionInfo() == transactionContext.getManagedConnectionInfo(transactionCachingInterceptor));
        //commit, see if connection returned.
        //we didn't create any handles, so the "ManagedConnection" should be returned.
        assertTrue("Expected TransactionContext to report active", transactionContext.isActive());
        transactionContext.commit();
        assertTrue("Expected connection to be returned", returnedConnectionInfo != null);
        assertTrue("Expected TransactionContext to report inactive", !transactionContext.isActive());

    }

    public void testGetConnectionOutsideTransaction() throws Exception {
        TransactionContext.setContext(new UnspecifiedTransactionContext());
        ConnectionInfo connectionInfo1 = makeConnectionInfo();
        transactionCachingInterceptor.getConnection(connectionInfo1);
        assertTrue("Expected to get an initial connection", obtainedConnectionInfo != null);
        assertTrue("Expected nothing returned yet", returnedConnectionInfo == null);
        assertTrue("Expected no ManagedConnectionInfo in the TransactionContext",
                null == TransactionContext.getContext().getManagedConnectionInfo(transactionCachingInterceptor));
        obtainedConnectionInfo = null;
        ConnectionInfo connectionInfo2 = makeConnectionInfo();
        transactionCachingInterceptor.getConnection(connectionInfo2);
        assertTrue("Expected to get a second connection", obtainedConnectionInfo != null);
        assertTrue("Expected nothing returned yet", returnedConnectionInfo == null);
        assertTrue("Expected different ManagedConnectionInfo in both ConnectionInfos",
                connectionInfo1.getManagedConnectionInfo() != connectionInfo2.getManagedConnectionInfo());
        assertTrue("Expected no ManagedConnectionInfo in the TransactionContext",
                null == TransactionContext.getContext().getManagedConnectionInfo(transactionCachingInterceptor));
        //we didn't create any handles, so the "ManagedConnection" should be returned.
        assertTrue("Expected TransactionContext to report inactive", !TransactionContext.getContext().isActive());
        transactionCachingInterceptor.returnConnection(connectionInfo1, ConnectionReturnAction.RETURN_HANDLE);
        assertTrue("Expected connection to be returned", returnedConnectionInfo != null);
        returnedConnectionInfo = null;
        transactionCachingInterceptor.returnConnection(connectionInfo2, ConnectionReturnAction.RETURN_HANDLE);
        assertTrue("Expected connection to be returned", returnedConnectionInfo != null);

        assertTrue("Expected TransactionContext to report inactive", !TransactionContext.getContext().isActive());

    }

    public void testTransactionIndependence() throws Exception {
        ContainerTransactionContext transactionContext1 = new ContainerTransactionContext(transactionManager);
        TransactionContext.setContext(transactionContext1);
        transactionContext1.begin();
        ConnectionInfo connectionInfo1 = makeConnectionInfo();
        transactionCachingInterceptor.getConnection(connectionInfo1);
        obtainedConnectionInfo = null;

        //start a second transaction
        transactionContext1.suspend();
        ContainerTransactionContext transactionContext2 = new ContainerTransactionContext(transactionManager);
        TransactionContext.setContext(transactionContext2);
        transactionContext2.begin();
        ConnectionInfo connectionInfo2 = makeConnectionInfo();
        transactionCachingInterceptor.getConnection(connectionInfo2);
        assertTrue("Expected to get a second connection", obtainedConnectionInfo != null);
        assertTrue("Expected nothing returned yet", returnedConnectionInfo == null);
        assertTrue("Expected different ManagedConnectionInfo in each ConnectionInfos",
                connectionInfo1.getManagedConnectionInfo() != connectionInfo2.getManagedConnectionInfo());
        assertTrue("Expected the same ManagedConnectionInfo in the TransactionContext as was returned",
                connectionInfo2.getManagedConnectionInfo() == transactionContext2.getManagedConnectionInfo(transactionCachingInterceptor));
        //commit 2nd transaction, see if connection returned.
        //we didn't create any handles, so the "ManagedConnection" should be returned.
        assertTrue("Expected TransactionContext to report active", transactionContext2.isActive());
        transactionContext2.commit();
        assertTrue("Expected connection to be returned", returnedConnectionInfo != null);
        assertTrue("Expected TransactionContext to report inactive", !transactionContext2.isActive());
        returnedConnectionInfo = null;
        //resume first transaction
        transactionContext1.resume();
        transactionContext1.commit();
        assertTrue("Expected connection to be returned", returnedConnectionInfo != null);
        assertTrue("Expected TransactionContext to report inactive", !transactionContext1.isActive());
    }

//interface implementations
    public void getConnection(ConnectionInfo connectionInfo) throws ResourceException {
        super.getConnection(connectionInfo);
        ManagedConnectionInfo managedConnectionInfo = connectionInfo.getManagedConnectionInfo();
        managedConnectionInfo.setConnectionEventListener(new GeronimoConnectionEventListener(null, managedConnectionInfo));
    }


    public void handleObtained(
            ConnectionTrackingInterceptor connectionTrackingInterceptor,
            ConnectionInfo connectionInfo) {
    }

    public void handleReleased(
            ConnectionTrackingInterceptor connectionTrackingInterceptor,
            ConnectionInfo connectionInfo) {
    }

}
