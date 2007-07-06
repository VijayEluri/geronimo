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
<%@ page import="javax.jms.Message" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Iterator" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<br>
<table cellpadding="1" width="100%">     
    <tr>
        <td class="DarkBackground" align="center"><b>Priority</b></td>
        <td class="DarkBackground" align="center"><b>MessageID</b></td>
        <td class="DarkBackground" align="center"><b>Destination</b></td>
        <td class="DarkBackground" align="center"><b>Timestamp</b></td>
        <td class="DarkBackground" align="center"><b>Expiration</b></td>            
        <td class="DarkBackground" align="center"><b>Type</b></td>
        <td class="DarkBackground" align="center"><b>ReplyTo</b></td>
        <td class="DarkBackground" align="center"><b>CorrelationID</b></td>
    </tr>
    <c:choose>
        <c:when test="${fn:length(messages) > 0}"> 
        <% 
        String[] styles = {"LightBackground","MediumBackground"};
        List messages = (List)request.getAttribute("messages");
        int n = 0;
        for(Iterator i = messages.iterator();i.hasNext();n++){
            Message message = (Message)i.next();
        %>

              <tr>
                  <td class="<%=styles[n%2]%>" align="center"><%=message.getJMSPriority()%></td>
                  <td class="<%=styles[n%2]%>" align="center"><%=message.getJMSMessageID()%></td>
                  <td class="<%=styles[n%2]%>" align="center"><%=message.getJMSDestination()%></td>
                  <td class="<%=styles[n%2]%>" align="center"><%=new java.util.Date(message.getJMSTimestamp())%></td>
                <td class="<%=styles[n%2]%>" align="center">
                <%=(message.getJMSExpiration()!=0)?new java.util.Date(message.getJMSExpiration()).toString():"No expiration"%>
                </td>
                  <td class="<%=styles[n%2]%>" align="center"><%=message.getJMSType()%></td>
                  <td class="<%=styles[n%2]%>" align="center"><%=message.getJMSReplyTo()%></td>
                  <td class="<%=styles[n%2]%>" align="center"><%=message.getJMSCorrelationID()%></td>
            </tr>
        <%}%>
           </c:when>
        <c:otherwise>
        <tr>    
            <td colspan="8" align="center">No messages in destination.</td>
        </tr>
           </c:otherwise>
    </c:choose>
        <tr>    
            <td colspan="8" align="center"><a href="<portlet:renderURL/>">Back</a> <a href="">Refresh</a></td>
        </tr>
</table>
