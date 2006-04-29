<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet"%>
<portlet:defineObjects/>

<br />
<br />
<table width="100%">
    <tr class="DarkBackground">
        <th align="left">&nbsp;Component Name</th>
        <c:if test="${showWebInfo}"><th>URL</th></c:if>
        <th>&nbsp;State</th>
        <th align="center" colspan="2">Commands</th>
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
        <td class="${backgroundClass}">&nbsp;${moduleDetails.configId}</td>
        <c:if test="${showWebInfo}">
            <td class="${backgroundClass}">&nbsp;<c:if test="${moduleDetails.state.running}"><a href="${moduleDetails.urlFor}">${moduleDetails.contextPath}</a></c:if></td>
        </c:if>
        <td width="100" class="${backgroundClass}">&nbsp;${moduleDetails.state}</td>
        <td width="100" class="${backgroundClass}">
            <c:if test="${moduleDetails.state.running}">&nbsp;<a href="<portlet:actionURL><portlet:param name="configId" value="${moduleDetails.configId}"/><portlet:param name="action" value="stop"/></portlet:actionURL>">Stop</a></c:if>
            <c:if test="${moduleDetails.state.stopped && (moduleDetails.type.name ne 'CAR')}">&nbsp;<a href="<portlet:actionURL><portlet:param name="configId" value="${moduleDetails.configId}"/><portlet:param name="action" value="start"/></portlet:actionURL>">Start</a></c:if>
            <c:if test="${moduleDetails.state.failed}">&nbsp;<a href="<portlet:actionURL><portlet:param name="configId" value="${moduleDetails.configId}"/><portlet:param name="action" value="stop"/></portlet:actionURL>">Stop</a></c:if>
        </td>
        <td width="100" class="${backgroundClass}">
            <a href="<portlet:actionURL><portlet:param name="configId" value="${moduleDetails.configId}"/><portlet:param name="action" value="uninstall"/></portlet:actionURL>">Uninstall</a>
        </td>
    </tr>
  </c:forEach>
</table>

<p>${messageInstalled} ${messageStatus}</p>
