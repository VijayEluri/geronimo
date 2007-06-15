<%--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed  under the  License is distributed on an "AS IS" BASIS,
WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
implied.

See the License for the specific language governing permissions and
limitations under the License.
--%>
<HEAD>
    <TITLE>Geronimo Console</TITLE>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/main.css" type="text/css"/>
    <link rel="SHORTCUT ICON" href="<%=request.getContextPath()%>/favicon.ico" type="image/x-icon"/>
    <script language="Javascript" src="<%=request.getContextPath()%>/js/forms.js" type="text/javascript"></script>

    <script type="text/javascript">
    	// var djConfig = { isDebug: true, debugAtAllCosts: true };
    </script>
    
    <script type="text/javascript" src="/dojo/dojo.js"></script>
    
    <script type="text/javascript">
    	dojo.require("dojo.lang.*");
    	dojo.require("dojo.widget.*");
    	// Pane includes
    	dojo.require("dojo.widget.ContentPane");
    	dojo.require("dojo.widget.LayoutContainer"); // Before: LayoutPane
    	dojo.require("dojo.widget.SplitContainer"); // Before: SplitPane
        // Tree includes
    	dojo.require("dojo.widget.Tree");
    	dojo.require("dojo.widget.TreeBasicController");
    	dojo.require("dojo.widget.TreeContextMenu");
    	dojo.require("dojo.widget.TreeSelector");
        // Tab includes
        dojo.require("dojo.widget.TabContainer");
        // Etc includes
        dojo.require("dojo.widget.SortableTable");
        dojo.require("dojo.widget.ComboBox");
        dojo.require("dojo.widget.Tooltip");
        dojo.require("dojo.widget.validate");
    	// Includes Dojo source for debugging
    	// dojo.hostenv.writeIncludes();
    </script>
</HEAD>
