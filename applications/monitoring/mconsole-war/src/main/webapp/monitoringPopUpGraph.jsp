<%--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet"%>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.PreparedStatement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="org.apache.geronimo.monitoring.console.util.*" %>
<%@page import="org.apache.geronimo.monitoring.console.GraphsBuilder"%>
<%@page import="org.apache.geronimo.monitoring.console.StatsGraph"%>
<portlet:defineObjects/>
<script language="JavaScript" type="text/javascript">
</script>

<%
Connection con = (new DBManager()).getConnection();
GraphsBuilder graphBuilder = new GraphsBuilder(con);
String graph_id = request.getParameter("graph_id");
StatsGraph graph = graphBuilder.buildOneDB(Integer.parseInt(graph_id));
String graphString = graph.getJS();
String divImplString = graph.getDivImplement();
String divNameString = graph.getName();
%>

<style type="text/css">
    #graph0Container { margin: 0px; width: 670px; height: 240px; }
</style>
<script type='text/javascript' src='/dojo/dojo.js'></script>
<script type='text/javascript'>
    var dojoConfig = { isDebug:true };
    dojo.require("dojo.collections.Store")
    dojo.require("dojo.charting.Chart")
    dojo.require("dojo.json")
    <%=graphString%>
</script>
<div id="<%=divNameString%>Head" "style="background-color: #f2f2f2; border-top: 1px solid #2581c7; margin: 0px; width: 670px; height: 16px;"><div align="left" style="background-color: #f2f2f2; float:left; text-align:left; width:500px;"><%=divNameString%></div><div align=right style="background-color: #f2f2f2; float:left; width:170px; text-align:right;"><a href="#" onClick="hide(\'<%=divNameString%>\')"><img border=0 src="/monitoring/images/min-b.png"></a>&nbsp;<a href="#" onClick="show(\'<%=divNameString%>\')"><img border=0 src="/monitoring/images/max-b.png"></a>&nbsp;<a href="#" onClick="hide(\'<%=divNameString%>\');hide(\'<%=divNameString%>Head\');"><img border=0 src="/monitoring/images/close-b.png"> </a></div></div>
<%=divImplString %>