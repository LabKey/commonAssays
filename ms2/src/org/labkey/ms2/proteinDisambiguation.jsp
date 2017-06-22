<%
/*
 * Copyright (c) 2008-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.util.GUID" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.api.util.Pair" %>
<%@ page extends="org.labkey.api.jsp.JspBase"%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<script type="text/javascript">
    LABKEY.requiresCss("ProteinCoverageMap.css");
    LABKEY.requiresScript("util.js");

    function checkAll() {
        var checked = document.getElementById('checkallproteins').checked;
        var cbs = document.getElementsByName('targetSeqIds');
        for(var i=0; i < cbs.length; i++) {
            if(cbs[i].type == 'checkbox') {
                cbs[i].checked = checked;
            }
        }
    }

    function validate() {
        var cbs = document.getElementsByName('targetSeqIds'), checkedCount = 0;
        for(var i=0; i < cbs.length; i++) {
            if(cbs[i].type == 'checkbox' && cbs[i].checked) {
                checkedCount++;
            }
        }
        if (checkedCount == 0)
        {
            alert("Please choose at least one applicable protein!");
            return false;
        }
        else if (checkedCount > 1900)
        {
            alert("Exceeded maximum number of selection of 1900.");
            return false;
        }
        return true;
    }
</script>

<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("clientapi/ext3");
    }
%>

<%
    JspView<Pair<ActionURL, List<Protein>>> view = (JspView<Pair<ActionURL, List<Protein>>>) HttpView.currentView();
    Pair<ActionURL, List<Protein>> actionWithProteins = view.getModelBean();
    List<Protein> proteins = actionWithProteins.second;
    ActionURL baseUrl = actionWithProteins.first;

    ActionURL formUrl = baseUrl.clone();
    formUrl.deleteParameters();

    if (proteins.isEmpty()) { %>
        No proteins match. Please try another name. <%
    }
    else { %>
        Multiple proteins match your search. Please choose the applicable proteins below.<br>

<form action="<%=formUrl%>" method="get" onsubmit="return validate();">
    <% for (Pair<String, String> param : baseUrl.getParameters()) { %>
        <input type="hidden" name="<%= h(param.getKey()) %>" value="<%= h(param.getValue()) %>" />
    <% } %>
    <div style="margin-top: 10px;"><input type=checkbox name=checkall checked id=checkallproteins style="margin-right: 12px;" onclick="checkAll();">All</div>
    <%
        for (Protein protein : proteins) {
            String divId = GUID.makeGUID();
            JSONObject props = new JSONObject().put("width", 450).put("title", "Protein Details");
            JSONObject autoLoadProp = new JSONObject();
            ActionURL ajaxURL = new ActionURL(MS2Controller.ShowProteinAJAXAction.class, getContainer());
            ajaxURL.addParameter("seqId", protein.getSeqId());
            autoLoadProp.put("url", ajaxURL.toString());
            props.put("autoLoad", autoLoadProp);
            props.put("leftPlacement", true);
            props.put("target", divId);

            ActionURL proteinUrl = baseUrl.clone();
            proteinUrl.addParameter(MS2Controller.PeptideFilteringFormElements.targetSeqIds, protein.getSeqId());

    %>

    <div>
        <input type=checkbox name=targetSeqIds value="<%= h(protein.getSeqId())%>" checked>

        <span id="<%= h(divId) %>"></span>
        <span><a href="<%= h(proteinUrl) %>"><%= h(protein.getBestName())%></a></span>
    </div>

    <script type="text/javascript">
        Ext.onReady(function () {
            new LABKEY.ext.CalloutTip( <%= text(props.toString()) %> );
        });
    </script>

<%
            }
%>
<br>
<%= button("Continue").submit(true) %>&nbsp;<%=generateBackButton("Cancel")%>
</form>
<%
    }
%>

