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
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt"%>
<%@ taglib uri="http://portals.apache.org/pluto" prefix="pluto" %>
<fmt:setLocale value="<%=request.getLocale()%>" />
<fmt:setBundle basename="org.apache.geronimo.console.i18n.ConsoleResource"/>
<%@ page import="org.apache.geronimo.pluto.impl.PageConfig"%>

<body id="admin-console" marginwidth="0" marginheight="0" leftmargin="0" topmargin="0" rightmargin="0">


<script type="text/javascript" src="/console/dojo/dojo/dojo.js" djConfig="parseOnLoad: true"></script>
<script type="text/javascript" src="/console/dojo/dijit/dijit.js"></script>
<script type="text/javascript" src="/console/dojo/dojox/dojox.js" ></script>


<script>
//we have to use dojo.hash to maintain the hash change history because browser does not 
//recogonize a hash change when users click back/forward button.

dojo.require("dojo.hash");
dojo.require("dojox.collections.Dictionary");
dojo.require("dojo.io.iframe");

var hash_iframeSrc_map= new dojox.collections.Dictionary;

dojo.subscribe("/dojo/hashchange", this, this.onHashChange);

function onHashChange(current_hash) {

    if(!current_hash||current_hash.length==0) return;
    
    var currentIframeHref=document.getElementById("portletsFrame").contentWindow.location.href;
    
    if(hash_iframeSrc_map.containsKey(current_hash)){
    
        var HrefForCurrentHash=hash_iframeSrc_map.entry(current_hash).value;
    
        if(HrefForCurrentHash==currentIframeHref) return;

        dojo.io.iframe.setSrc(document.getElementById("portletsFrame"), hash_iframeSrc_map.entry(current_hash).value, true);
    
    } else {
    
        hash_iframeSrc_map.add(current_hash,currentIframeHref);
       
    }
}

//When there's hash in current page url, redirect the page with noxxsPage hash as the query string,
//the server side will get the real redirect target page based on the value of noxxsPage
if(document.location.hash!='') {
       var href = document.location.href;
       var newHref = href.substring(0,href.lastIndexOf("#"));
       if(newHref.indexOf("&noxssPage")>0){
            newHref = newHref.substring(0,href.indexOf("&noxssPage"));
       }
       document.location.href =  newHref + "&noxssPage=" +document.location.hash.substr(11,document.location.hash.length);
}

</script>

<!-- start accessibility prolog -->
<div class="skip"><a href="#left-nav" accesskey="1">Skip to navigation</a></div>
<div class="skip"><a href="#content" accesskey="2">Skip to main content</a></div>
<div id="access-info">
    <p class="access" >The access keys for this page are:</p>
    <ul class="access">
        <li>ALT plus 1 skips to navigation.</li>
        <li>ALT plus 2 skips to main content.</li>
    </ul>
</div>
<!-- end accessibility prolog -->

<!-- Header -->
<table width="100%" height="86"  border="0" cellpadding="0" cellspacing="0">
    <tr>
        <td height="86" class="Logo" border="0"></td>
        <td height="86" class="Top" border="0">&nbsp;</td>
        <td height="86" class="Top" border="0" width="40"></td>
        <td height="86" class="Top" border="0" width="40">
            <a href="<%=request.getContextPath()%>/logout.jsp"><img border="0" src="<%=request.getContextPath()%>/images/head_logout_63x86.gif" alt="Logout"/></a>
        </td>
    </tr>
</table>

<p style="margin-top:5px;margin-bottom:5px"></p>

<!-- Body -->
<table width="100%"  border="0" cellpadding="0" cellspacing="0">
    <tr>
        <!-- Spacer -->
        <td class="Gutter">&nbsp;</td> 
        
        <!-- Navigation Column -->
        <td width="260px" class="Selection" valign="top"> 
            <div id="left-nav"> 
                <!-- Include Navigation.jsp here -->
                <jsp:include page="navigation.jsp"/>
            </div>
        </td>
        
        <!-- Spacer -->
        <td class="Gutter">&nbsp;</td> 
        
        <!-- Portlet Section -->
        <td valign="top">
            <iframe src="" id="portletsFrame" width="100%" height="100%" scrolling="no" frameborder="0">
            </iframe>
        </td>

        <!-- Spacer -->
        <td class="Gutter">&nbsp;</td> 
    </tr>
</table>

</body>
<script type="text/javascript">
    <% 
    PageConfig pc=(PageConfig)request.getAttribute("currentPage");
    String pageID=pc.getName();
    String pageName=pageID.substring(pageID.lastIndexOf("/")+1,pageID.length());
    %>
    var pageName = "<fmt:message key="<%=pageName%>"/>";
    quickLaunchPortlets(pageName);
</script>
<script language="JavaScript">

function autoResizeIframe(){
    // reset the height of index page each time the new portlet is loaded
    document.body.height = 400; 
  
    try{
        var iframe = document.getElementById("portletsFrame");
        var iframeDocument = iframe.contentWindow.document;
        
        var toHeight; 
        toHeight = (iframeDocument.height) ? iframeDocument.height : iframeDocument.body.scrollHeight;
        
        (iframe.style) ? iframe.style.height = toHeight : iframe.height = toHeight; 
        
    }catch (ex){
        window.status = ex.message;
    }


}

function autoCheckIframe(){
    var iFrameDocument=document.getElementById("portletsFrame").contentWindow.document;
    var LoginForm=iFrameDocument.getElementsByName('login');

    if(LoginForm.length!=0){
        window.location.reload();
    }
}

//Ensure the iframe height could be adjusted according to the content
setInterval('autoResizeIframe()',500); 

//Ensure login page is not displayed in the iframe after the timeout
setInterval('autoCheckIframe()',500); 


</script>
