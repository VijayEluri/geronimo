/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Geronimo" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Geronimo", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * ====================================================================
 */
package org.apache.geronimo.ejb.cache;

import org.apache.geronimo.common.AbstractInterceptor;
import org.apache.geronimo.common.Invocation;
import org.apache.geronimo.common.InvocationResult;
import org.apache.geronimo.ejb.EJBInvocationUtil;
import org.apache.geronimo.ejb.EnterpriseContext;
import org.apache.geronimo.ejb.SynchronizationRegistry;

/**
 *
 *
 *
 * @version $Revision: 1.4 $ $Date: 2003/08/26 22:11:23 $
 */
public final class EntitySynchronizationInterceptor extends AbstractInterceptor {
    // todo find a home for me... should be a JMX object
    private static final SynchronizationRegistry synchronizationRegistry = new SynchronizationRegistry();

    public InvocationResult invoke(Invocation invocation) throws Throwable {
        // register the context for synchronization
        EnterpriseContext ctx = EJBInvocationUtil.getEnterpriseContext(invocation);
        Object id = EJBInvocationUtil.getId(invocation);
        if (id != null) {
            synchronizationRegistry.beginInvocation(ctx);
        }

        boolean threwException = true;
        try {
            // Note there is no need to lock on creation after the invoke because the EntityCreationInterceptor
            // will immedately do a second invoke down the chain for post create causing a lock
            InvocationResult result = getNext().invoke(invocation);
            threwException = false;
            return result;
        } finally {
            // notify the synchronization registry that the invocation has finished
            if (id != null) {
                synchronizationRegistry.endInvocation(threwException, id, ctx);
            }
        }
    }
}
