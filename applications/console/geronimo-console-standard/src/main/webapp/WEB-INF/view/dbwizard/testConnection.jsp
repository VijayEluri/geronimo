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
<portlet:defineObjects/>

<p><b>Create Database Pool</b> -- Step 4: Test Connection</p>

<!--   FORM TO COLLECT DATA FOR THIS PAGE   -->
<form name="<portlet:namespace/>DatabaseForm" action="<portlet:actionURL/>" method="POST">
    <input type="hidden" name="mode" value="save" />
    <input type="hidden" name="user" value="${pool.user}" />
    <input type="hidden" name="name" value="${pool.name}" />
    <input type="hidden" name="dbtype" value="${pool.dbtype}" />
    <input type="hidden" name="password" value="${pool.password}" />
    <input type="hidden" name="driverClass" value="${pool.driverClass}" />
    <input type="hidden" name="url" value="${pool.url}" />
    <input type="hidden" name="urlPrototype" value="${pool.urlPrototype}" />
    <c:forEach var="jar" items="${pool.jars}">
     <input type="hidden" name="jars" value="${jar}" />
    </c:forEach>    <input type="hidden" name="adapterDisplayName" value="${pool.adapterDisplayName}" />
    <input type="hidden" name="minSize" value="${pool.minSize}" />
    <input type="hidden" name="maxSize" value="${pool.maxSize}" />
    <input type="hidden" name="idleTimeout" value="${pool.idleTimeout}" />
    <input type="hidden" name="blockingTimeout" value="${pool.blockingTimeout}" />
    <input type="hidden" name="adapterDisplayName" value="${pool.adapterDisplayName}" />
    <input type="hidden" name="adapterDescription" value="${pool.adapterDescription}" />
    <input type="hidden" name="rarPath" value="${pool.rarPath}" />
  <c:forEach var="prop" items="${pool.properties}">
    <input type="hidden" name="${prop.key}" value="${prop.value}" />
  </c:forEach>
  <c:forEach var="prop" items="${pool.urlProperties}">
    <input type="hidden" name="${prop.key}" value="${prop.value}" />
  </c:forEach>
    <table border="0">
    <!-- STATUS FIELD: Conection Result -->
      <tr>
        <th style="min-width: 140px"><div align="right">Test Result:</div></th>
        <td>
          <c:choose>
            <c:when test="${empty connectResult}">
              <font color="red"><i>Connection Error (see below)</i></font>
            </c:when><c:otherwise>
              Connected to ${connectResult}
            </c:otherwise>
          </c:choose>
        </td>
      </tr>
    <!-- STATUS FIELD: Connection Errors -->
    <c:if test="${!(empty connectError)}">
      <tr>
        <th><div align="right">Test Error:</div></th>
        <td><textarea rows="30" cols="60" readonly>${connectError}</textarea></td>
      </tr>
    </c:if>
    <!-- SUBMIT BUTTON -->
      <tr>
        <td></td>
        <td>
          <c:choose>
            <c:when test="${empty connectResult}">
<input type="submit" value="Deploy Anyway" />
<input type="button" value="Edit Settings" onclick="document.<portlet:namespace/>DatabaseForm.mode.value='edit';document.<portlet:namespace/>DatabaseForm.submit();return false;" />
<input type="button" value="Test Again" onclick="document.<portlet:namespace/>DatabaseForm.mode.value='process-url';document.<portlet:namespace/>DatabaseForm.submit();return false;" />
            </c:when><c:otherwise>
<input type="submit" value="Deploy" />
<input type="button" value="Show Plan" onclick="document.<portlet:namespace/>DatabaseForm.mode.value='plan';document.<portlet:namespace/>DatabaseForm.submit();return false;" />
            </c:otherwise>
          </c:choose>
        </td>
      </tr>
    </table>
</form>
<!--   END OF FORM TO COLLECT DATA FOR THIS PAGE   -->

<p><a href="<portlet:actionURL portletMode="view">
              <portlet:param name="mode" value="list" />
            </portlet:actionURL>">Cancel</a></p>
