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
<p>This portlet displays the Geronimo server log and is helpful for 
  debugging problems with the Geronimo Console and with deployed
  applications.</p>
<table width="100%"  border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td width="150"  align="right" valign="top" class="MediumBackground" style="padding: 10px 10px 10px 5px; font-size: 10px; color: #546BC7; font-weight: bold; text-decoration: underline;">Refresh</td>
    <td class="LightBackground" style="padding: 10px 5px 10px 10px" >Resets the filtering criteria to Geronimo Console's defaults and displays the latest 11 lines in the Geronimo log file.</td>
  </tr>
  <tr>
    <td width="150" align="right" valign="top" class="MediumBackground"style="padding: 10px 10px 10px 5px; color: #1E1E52; font-weight: bold;">Filter results</td>
    <td class="LightBackground" style="padding: 10px 5px 10px 10px">Allows the user to customize the filtering criteria. The user can display any line of the log file starting at the first line of the log file through the end. Note: A browser may limit how many lines can be 
displayed at once. <br>
<br>
The user may filter on the log level to display only log lines at that level. <br>
<br>
The user may also enter a string of text to search for.</td>
  </tr>
</table>
