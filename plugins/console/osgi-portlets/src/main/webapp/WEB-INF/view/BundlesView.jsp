<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
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

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="portlet" uri="http://java.sun.com/portlet"%>

<!-- set i18n properties file name -->
<fmt:setBundle basename="osgiportlets"/>
    
<portlet:defineObjects />

<!-- loading div -->
<script>
var loadingdiv_timeout;
var threshold_time = 60; //seconds
function showLoadingDIV(){
	document.getElementById("loadingdiv").style.display='block';
	loadingdiv_timeout = setTimeout ("showLoadingTimeOut()", threshold_time * 1000);
}
function showLoadingTimeOut(){
	if (document.getElementById("loadingdiv").style.display=='block'){	
		if (confirm("The action you are processing has cost " + threshold_time + " seconds. Will you continue waiting?")) {
			clearTimeout(loadingdiv_timeout);
			loadingdiv_timeout = setTimeout ("showLoadingTimeOut()",threshold_time * 1000);
		} else {
			hideLoadingDIV();
		}
	}
}
function hideLoadingDIV(){
	document.getElementById("loadingdiv").style.display='none';
	clearTimeout(loadingdiv_timeout);
}
</script>
<div id="loadingdiv" style="position:absolute;width:100%;height:90%;display:none;z-index:1000;text-align:center; background-color:#ffffff;filter:alpha(opacity=70);opacity:0.7;">
<p align="center">
	<br/>
	<img border="0" src="<%=request.getContextPath()%>/images/loading128.gif" style="width:36;height:36"/>
	<br/>
	Loading...
</p>
</div>


<div class="tundra">
<script type="text/javascript"
	src="/dojo/dojo/dojo.js" djConfig="parseOnLoad: true">	
</script>

<style type="text/css">
@import "/dojo/dojo/resources/dojo.css";
@import "/dojo/dijit/themes/tundra/tundra.css";
@import "/dojo/dijit/themes/tundra/document.css";
@import "/dojo/dojox/grid/enhanced/resources/EnhancedGrid_rtl.css";
@import "/dojo/dojox/grid/enhanced/resources/tundra/EnhancedGrid.css";
@import "/dojo/dojox/grid/enhanced/resources/tundraEnhancedGrid.css";

@import "/dojo/dijit/themes/claro/claro.css";		
@import "/dojo/dijit/themes/claro/document.css";
@import "/dojo/dojox/grid/enhanced/resources/claro/EnhancedGrid.css";


</style>





<script type="text/javascript">
	dojo.require("dojo.parser");
	dojo.require("dojo.io.iframe"); 
	dojo.require("dijit.TitlePane");
	dojo.require("dojo.data.ItemFileWriteStore");
	dojo.require("dojox.grid.EnhancedGrid");
	dojo.require("dijit.form.Button");
	dojo.require("dojox.grid.enhanced.plugins.IndirectSelection");
	dojo.require("dojox.grid._Grid");
	dojo.require("dojox.grid.cells.dijit");
	dojo.require("dojox.grid.enhanced.plugins.Menu");


    dojo.require("dijit.layout.ContentPane");
    dojo.require("dijit.layout.TabContainer");
	

	
    function actionPerform(actionName, id){
    	
    	showLoadingDIV();

        //construct action json string
    	var actionJsonStr='{action:'+actionName+',id:'+id+'}';
    		
    	//alert(actionJsonStr);
    	
    	//ajax call to portlet's serveResource()
    	var postArgs={
    		url:"${ajaxURL_BundlesAction}formId="+formID,
    		handleAs:"json",
    		content:{bundlesActionParam:actionJsonStr},
    		//sync:true,
    		load:function(response, ioArgs){
    			//update the items which done an action
    			var updatedItem = response;		

    			if(updatedItem != null){
    				if (updatedItem.state == "err") {
    					// need better way to show err msg
    					alert("Update bundle" + updatedItem.id + "'s state fail. Please refresh it manually.");
    					dijit.byId("usrBundlesGrid").store.fetchItemByIdentity({
    						identity:updatedItem.id,
    						onItem:function(item){
    							dijit.byId("usrBundlesGrid").store.setValue(item,"state","Unknown");
    						}
    					});
    				}else if(updatedItem.state == "Uninstalled") { // uninstall action results; refresh action may also go here
        				// should show a msg
    					dijit.byId("usrBundlesGrid").store.fetchItemByIdentity({
    						identity:updatedItem.id,
    						onItem:function(item){
    							dijit.byId("usrBundlesGrid").store.deleteItem(item);
    						}
    					});
    				}else { // start or stop action results; refresh action may also go here
        				// should show a msg
    					dijit.byId("usrBundlesGrid").store.fetchItemByIdentity({
    						identity:updatedItem.id,
    						onItem:function(item){
    							dijit.byId("usrBundlesGrid").store.setValue(item,"state",updatedItem.state);
    						}
    					});
    				}
    			}

    			hideLoadingDIV();
    		}
    	};

    	dojo.xhrPost(postArgs);			


    	
    }


	function showManifest(id,symbolicName){
		showLoadingDIV();

		manifestTitle.innerHTML = "<b>The Manifest of "+symbolicName+"</b>";
        
		//ajax call to portlet's serveResource()
		var postArgs={
			url:"${ajaxURL_ShowManifest}formId="+formID,
			handleAs:"text",
			content:{id:id},
			//sync:true,
			load:function(response, ioArgs){
				var headers=dojo.fromJson(response);
				var newStore = new dojo.data.ItemFileWriteStore({data:headers}); 
				dijit.byId("manifestGrid").setStore(newStore);

				dijit.byId("bundlesTab").selectChild(dijit.byId("manifestPane"));


				hideLoadingDIV();
			}
		};
		dojo.xhrPost(postArgs);	

			
	}

	function showWiredBundles(id,symbolicName){
		showLoadingDIV();

		wiredBundlesTitle.innerHTML = "<b>The Wired Bundles of "+symbolicName+"</b>";
        
		//ajax call to portlet's serveResource()
		var postArgs={
			url:"${ajaxURL_ShowWiredBundles}formId="+formID,
			handleAs:"json",
			content:{id:id},
			//sync:true,
			load:function(response, ioArgs){
				var importingBundles = response.importingBundles;
				var newImportingStore = new dojo.data.ItemFileWriteStore({data:importingBundles}); 
				dijit.byId("wiredBundlesImportingGrid").setStore(newImportingStore);

				var exportingBundles = response.exportingBundles;
				var newExportingStore = new dojo.data.ItemFileWriteStore({data:exportingBundles}); 
				dijit.byId("wiredBundlesExportingGrid").setStore(newExportingStore);
				
				dijit.byId("bundlesTab").selectChild(dijit.byId("wiredBundlesPane"));

				hideLoadingDIV();	
			}
		};
		dojo.xhrPost(postArgs);	

		
	}

	function showSysBundles(){
		
		if (dijit.byId("sysBundlesPane")!=null){
			var sysBndCP = dijit.byId("sysBundlesPane");
			dijit.byId("bundlesTab").selectChild(sysBndCP);
		} else {
			showLoadingDIV();
			
			// add new ContentPane to TabContainer
		    var bndTC = dijit.byId("bundlesTab");
			var sysBndCP = new dijit.layout.ContentPane({
									id: "sysBundlesPane",
									title: "Geronimo System Bundles",
									content: "<p id='sysfilter' style='margin-top:5px;margin-bottom:5px'>Filter by Symbolic Name:</p>",
									style: "height:500",
									closable: true
								});
			bndTC.addChild(sysBndCP, 1); // insert to the index "1"
			bndTC.selectChild(sysBndCP);
				
			// add filter TextBox
			var filteredSysName = new dijit.form.TextBox();
			filteredSysName.placeAt("sysfilter",1);		// position "0" is for the string "Filter by Symbolic Name:"
	
			// add DataGrid
			var jsonStore = new dojo.data.ItemFileWriteStore({data:{items:[]}});
		        
		    var layout = [{
		            field: 'id',
		            name: 'Id',
		            width: '50px'
		        },
		        {
		            field: 'symbolicName',
		            name: 'Symbolic Name',
		            width: 'auto'
		        },
		        {
		            field: 'version',
		            name: 'Version',
		            width: '120px'
		        },
		        {
		            field: 'state',
		            name: 'State',
		            width: '60px'
		        },
		        {
		            field: '_item',
		            name: 'Utilities',
		            width: '250px',
		            formatter: fmtUtilCell
		        }];
	
		    var sysBndGrid = new dojox.grid.DataGrid({
		            query: {
		                id: '*'
		            },
		            store: jsonStore,
		            clientSort: true,
		            //rowSelector: '20px',
		            structure: layout,
		            style: "height:450"
		        },
		        document.createElement('div')
		    );
		    
		    sysBndGrid.placeAt(sysBndCP.domNode,2);  //sysBndCP.set("content",sysBndGrid.domNode);  //will replace the content
		    sysBndGrid.startup();
		        
	
			//connect the event handler with the dijit
		    dojo.connect(filteredSysName, "onKeyUp", function(evt) {
		        var fitlerStr = filteredSysName.get("value") ;
		        sysBndGrid.filter({symbolicName: "*" + fitlerStr + "*"});       
				dojo.stopEvent(evt);
		    });
	
		    //ajax call to portlet's serveResource()
		    var postArgs={
				url:"${ajaxURL_ShowSysBundles}",
				handleAs:"text",
				//sync:true,
				load:function(response, ioArgs){
					var sysbundles = dojo.fromJson(response);
					var newStore = new dojo.data.ItemFileWriteStore({data:sysbundles}); 
					sysBndGrid.setStore(newStore);

					hideLoadingDIV();
				}
			};
			dojo.xhrPost(postArgs);	
		}
		
		
	}

	
	function installBundle(){

		if (dijit.byId("startLevel").get("value") < ${initStartLevel}) {
			if (!confirm("The start-level you are setting is less than the framework initial start-level(" + ${initStartLevel} + "). Will you continue?")) {
				return;
			}
		}	
		
		showLoadingDIV();
		
		dojo.io.iframe.send({
			url: "${ajaxURL_InstallAction}formId="+formID,
			method: "post",
			handleAs: "text",
			form: dojo.byId("installForm"),
			load: function(response, ioArgs){
				var bundle=dojo.fromJson(response);
				var item=dijit.byId("usrBundlesGrid").store.newItem(bundle);
				dijit.byId("usrBundlesGrid").sort();

				hideLoadingDIV();
			}
		});
		
		
	}
</script> 

<div>
This portlet shows the general user bundles installed in geronimo. To see all the system bundles, <a href="#" onclick="showSysBundles()">click here</a> (Loading might be slow).
</div>
<br/>

<!-- bundles tab container : Start -->
<div jsid="bundlesTab" id="bundlesTab" dojoType="dijit.layout.TabContainer" style="width: 100%" doLayout="false">

<div jsid="bundlesPane" id="bundlesPane" dojoType="dijit.layout.ContentPane" title="General Bundles" selected="true">   

	<!-- bundles bar -->
	<table border="0" style="width:100%;margin:0px 0px;">
	    <tr>
	    	
	        <td>
	        	<form id="installForm" enctype="multipart/form-data" method="POST">
	            Install New:
	            <input type="file" name="bundleFile" />
				&nbsp;
		        
		        <fmt:message key="osgi.bundle.install.startAfterInstalled" />:
		        <input type="checkbox" dojoType="dijit.form.CheckBox" name="startAfterInstalled" value="yes" />
		        &nbsp;
		        

		        <fmt:message key="osgi.bundle.startLevel" />:
		        <input type="text" id="startLevel" name="startLevel" dojoType="dijit.form.TextBox" displayedValue="${initStartLevel}" style="width: 3em;" size="4" />
		        &nbsp;

		        <!--  can not align in IE
		        <button dojoType="dijit.form.Button">
			    	<script type="dojo/method" event="onClick" args="evt">
						installBundle();
		    		</script>
		        </button> 
		        -->
		        <input type="button" style="width:80px" onClick="installBundle()" value="<fmt:message key='osgi.bundle.install.install'/>"></input>
	        	</form>
	        </td>
	        
	        
	    	<!-- filter -->
			<td align="right">
				<label>Filter by Symbolic Name: </label>
				<div type="text" jsid="filteredSymbolicName" id="filteredSymbolicName" dojoType="dijit.form.TextBox" style="width: 16em;">
					<script type="dojo/method" event="onKeyUp" args="evt">
					var fitlerStr = dojo.byId("filteredSymbolicName").value;
        			dijit.byId("usrBundlesGrid").filter({
            			symbolicName: "*" + fitlerStr + "*"     
        			});
					dojo.stopEvent(evt);
				</script>
				</div>
			</td>
			

		</tr> 
	</table>
	
	
	
	<!-- bundles grid -->
	<script>
		jsonData=${bundleGridJSONObject};
		usrBundlesStroe=new dojo.data.ItemFileWriteStore({data:jsonData});
		
	    function fmtActionsCell(item, idx){
			var innerhtml = "";
			if (item.state == "Installed" || item.state == "Resolved"){
				innerhtml += "<a href='#' onclick=actionPerform('start',"+item.id+")>start</a>&nbsp;";
			}
			if (item.state == "Active"){
				innerhtml += "<a href='#' onclick=actionPerform('stop',"+item.id+")>stop</a>&nbsp;";
			}
			if (item.state != "Unknown"){
				innerhtml += "<a href='#' onclick=actionPerform('uninstall',"+item.id+")>uninstall</a>&nbsp;";
			}
			innerhtml += "<a href='#' onclick=actionPerform('refresh',"+item.id+")>refresh</a>&nbsp;";
			
			return innerhtml;
		}
	    function fmtUtilCell(item, idx){
			var innerhtml = "";
			
			innerhtml += "<a href='#' onclick=showManifest("+item.id+",'"+item.symbolicName+"')>View Manifest</a>&nbsp;";
			if (item.id !=0){
				if (item.state == "Active"){
					innerhtml += "<a href='#' onclick=showWiredBundles("+item.id+",'"+item.symbolicName+"')>View Wired Bundles</a>&nbsp;";
				}
			}
			
			return innerhtml;
		}
	</script>
	
	<table jsid="usrBundlesGrid" id="usrBundlesGrid" dojoType="dojox.grid.DataGrid"
		store="usrBundlesStroe" selectable="true" 
		query="{ id: '*' }" queryOptions ="{ignoreCase: true}" sortFields="[{attribute:'id',descending:true}]" 
		style="width:100%;height:450;margin:0px 0px;">
		<thead>
			<tr>
				<th field="id" width="50px">Id</th>
				<th field="symbolicName" width="50%">Symbolic Name</th>
				<th field="version" width="120px">Version</th>
				<th field="state" width="60px">State</th>
				<th field="_item" formatter="fmtActionsCell" width="160px">Actions</th>  <!-- "_item" represents an item in store -->
				<th field="_item" formatter="fmtUtilCell" width="250px">Utilities</th>
			</tr>
		</thead>
	</table>


</div>



<!-- Bundle Manifest -->
<div jsid="manifestPane" id="manifestPane" dojoType="dijit.layout.ContentPane" title="Bundle Manifest">
	<script>
		manifestStore = new dojo.data.ItemFileWriteStore({data:{items:[]}});
	</script>
	<div id="manifestTitle"></div>
	<div>
	<table jsid="manifestGrid" id="manifestGrid" dojoType="dojox.grid.DataGrid"
		store="manifestStore" escapeHTMLInData="false" selectable="true" 
		style="width:100%;height:450;margin:0px 0px;">
		<thead>
			<tr>
				<th field="hkey" width="20%">Header Name</th>
				<th field="hvalue" width="80%">Header Value</th>
			</tr>
		</thead>
	</table>
	</div>
</div>


<!-- Wired Bundles -->
<div jsid="wiredBundlesPane" id="wiredBundlesPane" dojoType="dijit.layout.ContentPane" title="Wired Bundles">
	<script>
		wiredBundlesStore = new dojo.data.ItemFileWriteStore({data:{items:[]}});
	</script>
	<div id="wiredBundlesTitle"></div>
	<div>
	Importing Packages from Bundles:<br/>
	<table jsid="wiredBundlesImportingGrid" id="wiredBundlesImportingGrid" dojoType="dojox.grid.DataGrid"
		store="wiredBundlesStore" escapeHTMLInData="false" selectable="true" 
		style="width:100%;height:300;margin:0px 0px;">
		<thead>
			<tr>
				<th field="pname" width="50%">Importing Packages</th>
				<th field="bname" width="50%">From Bundles</th>
			</tr>
		</thead>
	</table>
	
	<br/>
	Exporting Packages to Bundles:<br/>
	<table jsid="wiredBundlesExportingGrid" id="wiredBundlesExportingGrid" dojoType="dojox.grid.DataGrid"
		store="wiredBundlesStore" escapeHTMLInData="false" selectable="true" 
		style="width:100%;height:300;margin:0px 0px;">
		<thead>
			<tr>
				<th field="pname" width="50%">Exporting Packages</th>
				<th field="bname" width="50%">To Bundles</th>
			</tr>
		</thead>
	</table>
	</div>
</div>

</div>
<!-- bundles tab container : End -->

</div>


