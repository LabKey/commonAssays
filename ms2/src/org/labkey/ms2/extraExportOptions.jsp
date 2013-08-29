<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.ms2.MS2ExportType" %>
<%
/*
 * Copyright (c) 2008-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
%>
<p>
    Choose an export format:<br/>
    <input type="radio" name="exportFormat" value="<%= PageFlowUtil.filter(MS2ExportType.Excel) %>" checked="checked">Excel (limited to 65,535 rows)<br/>
    <input type="radio" name="exportFormat" value="<%= PageFlowUtil.filter(MS2ExportType.TSV) %>">TSV<br/>
    <input type="radio" name="exportFormat" value="<%= PageFlowUtil.filter(MS2ExportType.DTA) %>">Spectra as DTA<br/>
    <input type="radio" name="exportFormat" value="<%= PageFlowUtil.filter(MS2ExportType.PKL) %>">Spectra as PKL<br/>
    <input type="radio" name="exportFormat" value="<%= PageFlowUtil.filter(MS2ExportType.AMT) %>">AMT (Accurate Mass &amp; Time) file<br/>
    <input type="radio" name="exportFormat" value="<%= PageFlowUtil.filter(MS2ExportType.Bibliospec) %>">BiblioSpec spectra library file<br/>
</p>