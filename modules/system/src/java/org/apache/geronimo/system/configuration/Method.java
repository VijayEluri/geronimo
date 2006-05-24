/**
 *
 * Copyright 2006 The Apache Software Foundation
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

/*
 * This code has been borrowed from the Apache Xerces project. We're copying the code to
 * keep from adding a dependency on Xerces in the Geronimo kernel.
 */

package org.apache.geronimo.system.configuration;

/**
 * @version $Revision$ $Date$
 * @author <a href="mailto:arkin@intalio.com">Assaf Arkin</a>
 * @see OutputFormat
 */
public final class Method
{
    
    
    /**
     * The output method for XML documents.
     */
    public static final String XML = "xml";
    
    
    /**
     * The output method for HTML documents.
     */
    public static final String HTML = "html";
    
    
    /**
     * The output method for HTML documents as XHTML.
     */
    public static final String XHTML = "xhtml";
    
    
    /**
     * The output method for text documents.
     */
    public static final String TEXT = "text";
    
    
    /**
     * The output method for FO documents as PDF.
     */
    public static final String FOP = "fop";
    
    
}
