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

<script>
// Check to see if a component is "safe" to stop within a running server.
// Service components with names that begin with "org.apache.geronimo.configs/", for example,
// may not be safe to stop because doing so might prevent other components
// that depend on them (like the console itself) from functioning properly.
// If the component is not safe to stop then prompt to make sure that
// the user really intends to stop the component prior to any action.
function promptIfUnsafeToStop(configId,expertConfig, type) {
    // if the component is a Geronimo "expert" service then provide a stern warning
    if ((type == 'SERVICE') && (expertConfig == 'true')) {
        return confirm( configId + " is an Apache Geronimo service.\r\n \r\n" +
                       "Stopping this component may prevent the server or the "+
                       "administration console from functioning properly. " +
                       "All dependent components and subsequent dependencies will also be stopped. " +
                       "Reference the 'Child Components' list in the view for directly affected components.\r\n \r\n" +
                       "Proceed with this action?");
    }
    // if the component is the web console provide an appropriate warning
    if (configId.indexOf("org.apache.geronimo.configs/webconsole-") == 0) {
        return confirm( configId + " provides the administration console interface " +
                       "that you are currently viewing.\r\n \r\n Stopping it will cause the interface " +
                       "to become unavailable and manual action will be required to restore the function.\r\n \r\n" +
                       "Proceed with this action?");
    }
    // if the component is any other Geronimo "expert" component provide an appropriate warning
    if (expertConfig == 'true') {
        return confirm( configId + " is provided by Apache Geronimo and may be required by other " +
                       "modules (reference the 'Child Components' listed in the view).\r\n \r\n " +
                       "All dependent components and subsequent dependencies will also be stopped. \r\n \r\n" +
                       "Proceed with this action?");
    }
    // otherwise don't challenge the stop operation
    return true;
}


// Check to see if a component is "safe" to stop within a running server.
// Service components with names that begin with "org.apache.geronimo.configs/", for example,
// may not be safe to stop because doing so might prevent other components
// that depend on them (like the console itself) from functioning properly.
// If the component is not safe to stop then prompt to make sure that
// the user really intends to stop the component prior to any action.
function promptIfUnsafeToRestart(configId,expertConfig, type) {
    // if the component is a Geronimo "expert" service then provide a stern warning
    if ((type == 'SERVICE') && (expertConfig == 'true')) {
        return confirm( configId + " is an Apache Geronimo service.\r\n \r\n " +
                       "Restarting this component may prevent the server or the "+
                       "administration console from functioning properly. " +
                       "As part of the stop action, all dependent components and subsequent dependencies will also be stopped. " +
                       "Only this component will be restarted. " +
                       "Reference the 'Child Components' list in the view for directly affected components.\r\n \r\n " +
                       "Proceed with this action?");
    }
    // if the component is the web console provide an appropriate warning
    if (configId.indexOf("org.apache.geronimo.configs/webconsole-") == 0) {
        return confirm( configId + " provides the administration console interface " +
                       "that you are currently viewing.\r\n \r\n  Restarting it will cause the interface " +
                       "to become unavailable and manual action may be necessary to restore the console function.\r\n \r\n" +
                       "Proceed with this action?");
    }
    // if the component is a Geronimo "expert" component then provide an appropriate warning
    if (expertConfig == 'true') {
        return confirm( configId + " is provided by Apache Geronimo and may be required by other " +
                       "modules (reference the 'Child Components' listed in the view).\r\n \r\n " +
                       "As part of the stop action, all dependent components and subsequent dependencies will also be stopped. \r\n \r\n" +
                       "Proceed with this action?");
    }
    // otherwise don't challenge the restart operation
    return true;
}


// Uninstall is always a potentially dangerous action, so we should prompt the
// the user to ensure that they really indent to do this.  Uninistalling
// some modules is more destructive than others (such as modules which are
// dependencies of the web console or dependencies of other core server 
// modules.  In such cases. it may leave the server in a state where it 
// cannot be restarted.  These situations require more stringent warnings.
function uninstallPrompt(configId,expertConfig, type) {
    // if the component is a geronimo "expert" service always provide the most stern warning
    if ((type == 'SERVICE') && (expertConfig == 'true')) {
        return confirm( configId + " is an Apache Geronimo service.\r\n \r\n" +
                       "Uninstalling this component may have unexpected results "+
                       "such as rendering the administration web console or even the "+
                       "server itself unstable.  Reference the 'Child Components' view " + 
                       "for directly affected components. \r\n \r\n" +
                       "Are you certain you wish to proceed with this uninstall?");
    }
    // if the component is a the web console itself then provide an appropriate warning
    if (configId.indexOf("org.apache.geronimo.configs/webconsole-") == 0) {
        return confirm( configId + " provides the administration console user interface " +
                       "that you are currently viewing.\r\n \r\n  Uninstalling it will cause the interface " +
                       "to become unavailable and manual action will be required to restore the function.\r\n \r\n " +
                       "Are you certain you wish to proceed with this uninstall?");
    }
    // if the component is any other Apache Geronimo "expert" component then provide an appropriate warning
    if (expertConfig == 'true') {
        return confirm( configId + " is provided by Apache Geronimo and may be required by other " +
                       "modules (reference the 'Child Components' listed in the view). \r\n \r\n" +
                       "Are you certain you wish to proceed with this uninstall?");
    }
    // if the component is none of the above provide a standard warning
    return confirm("Are you certain you wish to uninstall " + configId + " ?");
}

// Toggle expert mode on and off with onClick
function toggleExpertMode() {
    if (document.checkExpert.expertUser.checked) {
        var expertActions = getSpanElementsByName('expert');
        for( var i = 0; i < expertActions.length; ++i ) {
            expertActions[i].style.display='block' ;
        }
        var nonexpertActions = getSpanElementsByName('nonexpert');
        for( var i = 0; i < nonexpertActions.length; ++i ) {
            nonexpertActions[i].style.display='none' ;
        }
    }
    else {
        var expertActions = getSpanElementsByName('expert');
        for( var i = 0; i < expertActions.length; ++i ) {
            expertActions[i].style.display='none' ;
        }
        var nonexpertActions = getSpanElementsByName('nonexpert');
        for( var i = 0; i < nonexpertActions.length; ++i ) {
            nonexpertActions[i].style.display='block' ;
        }
    }
}


// work around since IE doesn't support document.getElementsByName
function getSpanElementsByName(name) {
    var results = new Array();
    var spans = document.getElementsByTagName("span");
    for(i = 0,j = 0; i < spans.length; i++) {
        nameValue = spans[i].getAttribute("name");
        if(nameValue == name) {
          results[j] = spans[i];
          j++;
        }
    }
    return results;
}
</script>


<br />
<form name="checkExpert">
<input type="checkbox" name="expertUser" onClick="toggleExpertMode();"/>&nbsp;Expert User (enable all actions on Geronimo Provided Components)   
</form>
<br />
<table width="100%">
    <tr class="DarkBackground">
        <th align="left">&nbsp;Component Name</th>
        <c:if test="${showWebInfo}"><th>URL</th></c:if>
        <th>&nbsp;State</th>
        <th align="center" colspan="3">Commands</th>
        <th align="left">Parent Components</th>
        <th align="left">Child Components</th>
    </tr>
  <c:set var="backgroundClass" value='MediumBackground'/>
  <c:forEach var="moduleDetails" items="${configurations}">
      <c:choose>
          <c:when test="${backgroundClass == 'MediumBackground'}" >
              <c:set var="backgroundClass" value='LightBackground'/>
          </c:when>
          <c:otherwise>
              <c:set var="backgroundClass" value='MediumBackground'/>
          </c:otherwise>
      </c:choose>
    <tr>
        <!-- module id -->
        <td class="${backgroundClass}">&nbsp;${moduleDetails.configId}&nbsp;

        <!-- context path -->
        <c:if test="${showWebInfo}">
            <td class="${backgroundClass}">&nbsp;<c:if test="${moduleDetails.state.running}"><a href="${moduleDetails.contextPath}">${moduleDetails.contextPath}</a></c:if></td>
        </c:if>

        <!-- state -->
        <td width="100" class="${backgroundClass}">&nbsp;${moduleDetails.state}</td>

        <!-- Start/Stop actions -->
        <td width="75" class="${backgroundClass}">
            <c:if test="${moduleDetails.state.running || moduleDetails.state.failed}">
                <span <c:if test="${moduleDetails.expertConfig}"> name=expert </c:if>> 
                    &nbsp;<a href="<portlet:actionURL><portlet:param name="configId" value="${moduleDetails.configId}"/><portlet:param name="action" value="stop"/></portlet:actionURL>" onClick="return promptIfUnsafeToStop('${moduleDetails.configId}','${moduleDetails.expertConfig}','${moduleDetails.type.name}');">Stop</a>
                </span>
            </c:if>
            <c:if test="${moduleDetails.expertConfig && (moduleDetails.state.running || moduleDetails.state.failed)}">
                <span name=nonexpert> 
                    &nbsp;<a>Stop</a>
                </span>
            </c:if>
            <c:if test="${moduleDetails.state.stopped && (moduleDetails.type.name ne 'CAR')}">
                &nbsp;<a href="<portlet:actionURL><portlet:param name="configId" value="${moduleDetails.configId}"/><portlet:param name="action" value="start"/></portlet:actionURL>">Start</a>
            </c:if>
        </td>

        <!-- Restart action -->
        <td width="75" class="${backgroundClass}">
            <c:if test="${moduleDetails.state.running}">
                <span <c:if test="${moduleDetails.expertConfig}"> name=expert </c:if>> 
                    &nbsp;<a href="<portlet:actionURL><portlet:param name="configId" value="${moduleDetails.configId}"/><portlet:param name="action" value="restart"/></portlet:actionURL>" onClick="return promptIfUnsafeToRestart('${moduleDetails.configId}','${moduleDetails.expertConfig}','${moduleDetails.type.name}');">Restart</a>
                </span>
            </c:if>
            <c:if test="${moduleDetails.expertConfig && moduleDetails.state.running}">
                <span name=nonexpert> 
                    &nbsp;<a>Restart</a>
                </span>
            </c:if>
            <!-- <c:if test="${moduleDetails.state.running}">&nbsp;<a <c:if test="${moduleDetails.expertConfig}"> name=expert </c:if>href="<portlet:actionURL><portlet:param name="configId" value="${moduleDetails.configId}"/><portlet:param name="action" value="restart"/></portlet:actionURL>" onClick="return promptIfUnsafeToRestart('${moduleDetails.configId}','${moduleDetails.expertConfig}','${moduleDetails.type.name}');">Restart</a></c:if> -->
            <!-- <c:if test="${moduleDetails.expertConfig}">&nbsp;<a name=nonexpert /> Restart</a> </c:if>  -->
        </td>

        <!-- Uninstall action -->
        <td width="75" class="${backgroundClass}">
            <span <c:if test="${moduleDetails.expertConfig}"> name=expert </c:if>> 
                &nbsp;<a href="<portlet:actionURL><portlet:param name="configId" value="${moduleDetails.configId}"/><portlet:param name="action" value="uninstall"/></portlet:actionURL>" onClick="return uninstallPrompt('${moduleDetails.configId}','${moduleDetails.expertConfig}','${moduleDetails.type.name}');">Uninstall</a>
            </span>
            <c:if test="${moduleDetails.expertConfig}">
                <span name=nonexpert> 
                    &nbsp;<a>Uninstall</a>
                </span>
            </c:if>
        </td>

        <!-- Parents -->
        <td class="${backgroundClass}">
            <c:forEach var="parent" items="${moduleDetails.parents}">
                ${parent} <br>
            </c:forEach>
        </td>

        <!-- Children -->
        <td class="${backgroundClass}">
        <c:forEach var="child" items="${moduleDetails.children}">
            ${child} <br>
        </c:forEach>
        </td>
    </tr>
  </c:forEach>
</table>

<br />
<p>${messageInstalled} ${messageStatus}</p>


<script>
// Call to set initial expert mode actions correctly 
toggleExpertMode();
</script>
