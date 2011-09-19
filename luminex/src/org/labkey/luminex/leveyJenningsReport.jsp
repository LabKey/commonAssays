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
* Date: Sept 19, 2011
*/

%>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.luminex.TitrationForm" %>

<%
    JspView<TitrationForm> me = (JspView<TitrationForm>) HttpView.currentView();
    TitrationForm bean = me.getModelBean();
%>

<div id="leveyJenningsGraphParamsPanel"></div>

<script type="text/javascript">
    LABKEY.requiresScript("LeveyJenningsGraphParamsPanel.js");

    Ext.onReady(init);
    function init()
    {
        if ("null" == "<%= bean.getTitration() %>")
        {
            Ext.get('leveyJenningsGraphParamsPanel').update("Error: no titration specified.");
            return;
        }
        if ("null" == "<%= bean.getProtocol() %>")
        {
            Ext.get('leveyJenningsGraphParamsPanel').update("Error: no protocol specified.");
            return;
        }

        // set the nav trail page title to include the tiration name
        LABKEY.NavTrail.setTrail('<%= bean.getTitration() %> Levey-Jennings Plots');

        // initialize the graph parameters selection panel
        new LABKEY.LeveyJenningsGraphParamsPanel({
            renderTo: 'leveyJenningsGraphParamsPanel',
            cls: 'extContainer',
            titration: '<%= bean.getTitration() %>',
            assayName: '<%= bean.getProtocol() %>',
            width: 225,
            height: 450,
            border: true
        });
    }
</script>
