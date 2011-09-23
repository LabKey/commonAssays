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

/**
 * User: cnathe
* Date: Sept 7, 2011
*/
    
%>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.luminex.GuideSetForm" %>

<%
    JspView<GuideSetForm> me = (JspView<GuideSetForm>) HttpView.currentView();
    GuideSetForm bean = me.getModelBean();
%>

<div id="manageGuideSetPanel"></div>

<script type="text/javascript">
    LABKEY.requiresScript("luminex/ManageGuideSetPanel.js");

    Ext.onReady(init);
    function init()
    {
        new LABKEY.ManageGuideSetPanel({
            renderTo: 'manageGuideSetPanel',
            cls: 'extContainer',
            guideSetId: <%= bean.getRowId() %>,
            assayName: '<%= bean.getProtocol() %>'
        });
    }
</script>
