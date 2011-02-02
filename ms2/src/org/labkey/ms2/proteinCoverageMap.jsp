<%
/*
 * Copyright (c) 2011 LabKey Corporation
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
<%@ page import="org.labkey.api.util.Formats" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="java.text.Format" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.ProteinViewBean bean = ((JspView<MS2Controller.ProteinViewBean>)HttpView.currentView()).getModelBean();
    ActionURL exportUrl = getViewContext().cloneActionURL();
    exportUrl.setAction(MS2Controller.ExportProteinCoverageMapAction.class);
    if (null!=exportUrl.getParameter("seqId"))
        exportUrl.deleteParameter("seqId");
    exportUrl.addParameter("seqId", bean.protein.getSeqId());

    String exportLink = "<a href=\"" + exportUrl.toString() + "\" target=\"_blank\" class=\"labkey-text-link\" >Export</a>";
    ActionURL showPeptidesPopupUrl = getViewContext().cloneActionURL();
    showPeptidesPopupUrl.setAction(MS2Controller.showPeptidePopupAction.class);
    if (null!=showPeptidesPopupUrl.getParameter("MS2Peptides.viewName"))
            showPeptidesPopupUrl.deleteParameter("MS2Peptides.viewName");
     showPeptidesPopupUrl.addParameter("MS2Peptides.viewName", "Standard");
    bean.showRunUrl = showPeptidesPopupUrl.toString();
  %>
<link rel="stylesheet" type="text/css" href="ProteinCoverageMap.css" />
<script type="text/javascript">
LABKEY.requiresCss("ProteinCoverageMap.css");
LABKEY.requiresScript("util.js");
</script>

<script type="text/javascript">
    LABKEY.requiresClientAPI(true);  
    Ext.QuickTips.init();
</script>

<div>
    <p>
    <%=exportLink%>
    </p>
     <%=bean.protein.getCoverageMap(bean.run, bean.showRunUrl) %>
</div>



<script language="javascript 1.1" type="text/javascript">
    function grabFocus()
    {
        self.focus();
    }
    window.onload = grabFocus;
</script>

