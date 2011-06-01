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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
        MS2Controller.ProteinViewBean bean = ((JspView<MS2Controller.ProteinViewBean>)HttpView.currentView()).getModelBean();

        if (bean.showPeptides)
        {
            ActionURL showPeptidesPopupUrl = getViewContext().cloneActionURL();
            showPeptidesPopupUrl.setAction(MS2Controller.ShowPeptidePopupAction.class);
            if (null!=showPeptidesPopupUrl.getParameter("MS2Peptides.viewName"))
                showPeptidesPopupUrl.deleteParameter("MS2Peptides.viewName");
            showPeptidesPopupUrl.addParameter("MS2Peptides.viewName", "Standard");
            bean.showRunUrl = showPeptidesPopupUrl.toString();
        }
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
    <%
    if (bean.showPeptides)
    { %>
        <p><%
            ActionURL exportUrl = getViewContext().cloneActionURL();
            exportUrl.setAction(MS2Controller.ExportProteinCoverageMapAction.class);
            if (null==exportUrl.getParameter("seqId"))
                exportUrl.addParameter("seqId", bean.protein.getSeqId());
        %><%= org.labkey.api.util.PageFlowUtil.generateButton("Export", exportUrl)%></p>
        <p><%=bean.protein.getCoverageMap(bean.run, bean.showRunUrl) %></p><%
    }
    else
    {   %>
         <p><%=bean.protein.getFormattedSequence()%></p><%
    }
         %>

</div>
