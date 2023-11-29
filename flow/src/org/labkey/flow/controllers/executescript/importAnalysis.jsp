<%
/*
 * Copyright (c) 2008-2019 LabKey Corporation
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
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.portal.ProjectUrls" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="org.labkey.flow.controllers.executescript.ImportAnalysisForm" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Map" %>
<%@ page import="static org.labkey.flow.controllers.executescript.AnalysisScriptController.ImportAnalysisStep.CONFIRM" %>
<%@ page import="static org.labkey.flow.controllers.executescript.AnalysisScriptController.BACK_BUTTON_ACTION" %>
<%@ page import="org.labkey.api.util.StringUtilsLabKey" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        // TODO: --Ext3-- This should be declared as part of the included views
        dependencies.add("clientapi/ext3");
        dependencies.add("File");
        dependencies.add("FileUploadField.js");
        dependencies.add("Ext4ClientApi");
    }
%>
<%
    ImportAnalysisForm form = (ImportAnalysisForm)getModelBean();
    Container container = getContainer();
    ActionURL cancelUrl = urlProvider(ProjectUrls.class).getStartURL(container);
%>
<script type="text/javascript" nonce="<%=getScriptNonce()%>">
    function endsWith(a,b)
    {
        return a.length >= b.length && a.indexOf(b) === (a.length-b.length);
    }
</script>
<style type="text/css">
    .labkey-flow-import-steps {
        border-collapse:collapse;
    }
    .labkey-flow-import-steps td {
        padding: 4px;
    }
</style>
<table class="labkey-flow-import-steps">
    <tr>
        <% for (AnalysisScriptController.ImportAnalysisStep step : AnalysisScriptController.ImportAnalysisStep.values())
        {
            boolean currentStep = step.getNumber() == form.getStep();
            boolean futureStep = step.getNumber() > form.getStep();
            if (step.getNumber() > 1) {
                %>
                <td valign="middle">
                    <img src="<%=getWebappURL("_.gif")%>" style="background:<%=text(futureStep ? "silver" : "black")%>; width:30px; height:1px"/>
                </td>
                <%
            }
            %>
            <td width="70" style="text-align:center; opacity:<%=text(futureStep ? "0.6" : "1")%>" valign="top">
                <span style="font-size:1.1em;font-weight:<%=text(currentStep ? "bold":"normal")%>;"><%=step.getNumber()%></span><br/>
                <span style="font-weight:<%=text(currentStep ? "bold":"normal")%>"><%=h(step.getTitle())%></span>
            </td>
            <%
        }
        %>
    </tr>
</table>

<labkey:errors/>

<labkey:form name="<%=ImportAnalysisForm.NAME%>" action="<%=new ActionURL(AnalysisScriptController.ImportAnalysisAction.class, container)%>" method="POST" enctype="multipart/form-data">
    <input type="hidden" name="step" value="<%=form.getStep()%>">
    <%
        Iterator i = form.getWorkspace().getHiddenFields(getViewContext()).entrySet().iterator();
        while (i.hasNext())
        {
            Map.Entry entry = (Map.Entry)i.next();
            String key = "workspace." + entry.getKey();
            %><input type="hidden" id="<%=h(key)%>" name="<%=h(key)%>" value="<%=h(entry.getValue())%>"><%
        }

        if (form.getStep() > AnalysisScriptController.ImportAnalysisStep.REVIEW_SAMPLES.getNumber())
        {
            i = form.getSelectedSamples().getHiddenFields().entrySet().iterator();
            while (i.hasNext())
            {
                Map.Entry entry = (Map.Entry)i.next();
                String key = "selectedSamples." + entry.getKey();
            %><input type="hidden" id="<%=h(key)%>" name="<%=h(key)%>" value="<%=h(entry.getValue())%>"><%
            }
        }
    %>

<input type="hidden" name="goBack" id="goBack" value="false">
<% if (form.getWizardStep().getNumber() < CONFIRM.getNumber()) { %>
<p>
    <%= button("Cancel").href(cancelUrl) %>
    &nbsp;&nbsp;
    <%= button("Back").submit(true).primary(false).onClick(BACK_BUTTON_ACTION) %>
    <%= button("Next").submit(true) %>
</p>

    <%
        JspView<ImportAnalysisForm> view = form.getWizardStep().getJspView(form);
        include(view, out);
    %>

<p>
    <%= button("Cancel").href(cancelUrl) %>
    &nbsp;&nbsp;
    <%= button("Back").submit(true).primary(false).onClick(BACK_BUTTON_ACTION) %>
    <%= button("Next").submit(true) %>
</p>
    
<% } else { %>
    <%
    JspView<ImportAnalysisForm> view = CONFIRM.getJspView(form);
    include(view, out);
    %>
<% } %>
</labkey:form>
