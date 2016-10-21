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
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.Protein" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.api.util.GUID" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page extends="org.labkey.api.jsp.JspBase"%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<script type="text/javascript">
    LABKEY.requiresCss("ProteinCoverageMap.css");
    LABKEY.requiresScript("util.js")
</script>

<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("clientapi/ext3");
    }
%>

<%
    JspView<Map<Protein, ActionURL>> view = (JspView<Map<Protein, ActionURL>>) HttpView.currentView();
    Map<Protein, ActionURL> proteins = view.getModelBean();

    if (proteins.isEmpty()) { %>
        No proteins match. Please try another name. <%
    }
    else { %>
        Multiple proteins match your search. Please choose one below. <%
            for (Map.Entry<Protein, ActionURL> entry : proteins.entrySet()) {
                String divId = GUID.makeGUID();
                JSONObject props = new JSONObject().put("width", 450).put("title", "Protein Details");
                JSONObject autoLoadProp = new JSONObject();
                ActionURL ajaxURL = new ActionURL(MS2Controller.ShowProteinAJAXAction.class, getContainer());
                ajaxURL.addParameter("seqId", entry.getKey().getSeqId());
                autoLoadProp.put("url", ajaxURL.toString());
                props.put("autoLoad", autoLoadProp);
                props.put("leftPlacement", true);
                props.put("target", divId); %>

                <div></div><span id="<%= h(divId) %>"></span><span><a href="<%= h(entry.getValue()) %>"><%= h(entry.getKey().getBestName())%></a></span></div>

                <script type="text/javascript">
                    Ext.onReady(function () {
                        new LABKEY.ext.CalloutTip( <%= text(props.toString()) %> );
                    });
                </script><%
            }
    }
%>

