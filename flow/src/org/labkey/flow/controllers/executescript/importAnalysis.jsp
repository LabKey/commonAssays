<%
/*
 * Copyright (c) 2008-2014 LabKey Corporation
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
<%@ page import="org.labkey.api.view.template.ClientDependency" %>
<%@ page import="org.labkey.flow.controllers.executescript.AnalysisScriptController" %>
<%@ page import="org.labkey.flow.controllers.executescript.ImportAnalysisForm" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.LinkedHashSet" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%!
    public LinkedHashSet<ClientDependency> getClientDependencies()
    {
        // TODO: --Ext3-- This should be declared as part of the included views
        LinkedHashSet<ClientDependency> resources = new LinkedHashSet<>();
        resources.add(ClientDependency.fromFilePath("clientapi/ext3"));
        resources.add(ClientDependency.fromFilePath("File"));
        resources.add(ClientDependency.fromFilePath("FileUploadField.js"));
        resources.add(ClientDependency.fromFilePath("Ext4ClientApi"));
        return resources;
    }
%>
<%
    ImportAnalysisForm form = (ImportAnalysisForm)getModelBean();
    Container container = getContainer();
    ActionURL cancelUrl = urlProvider(ProjectUrls.class).getStartURL(container);
%>
<script type="text/javascript">
    function endsWith(a,b)
    {
        return a.length >= b.length && a.indexOf(b) == (a.length-b.length);
    }
</script>
<table border="0" style="border-collapse:collapse;" cellpadding="4">
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
            <td width="70" style="text-align:center;color:<%=text(futureStep ? "silver" : "black")%>" valign="top">
                <span style="font-size:1.1em;font-weight:<%=text(currentStep ? "bold":"normal")%>;"><%=step.getNumber()%></span><br/>
                <span style="font-weight:<%=text(currentStep ? "bold":"normal")%>"><%=h(step.getTitle())%></span>
                <%--
                <form name="step_<%=step.name()%>" action="<%=new ActionURL(AnalysisScriptController.ImportAnalysisAction.class, container)%>" method="POST" enctype="multipart/form-data">
                    <input type="hidden" name="step" value="<%=step.getNumber()-1%>">
                    <a href="javascript:void" onclick="document.step_<%=step.name()%>.submit()" style="text-decoration:none;color:inherit;">
                        <span style="font-size:2em;font-weight:<%=currentStep ? "bold":"normal"%>;<%=step.getNumber() == 0 ? "visibility:hidden;":""%>"><%=step.getNumber()%></span><br/>
                        <span style="font-weight:<%=currentStep ? "bold":"normal"%>"><%=step.getTitle()%></span>
                    </a>
                </form>
                --%>
            </td>
            <%
        }
        %>
    </tr>
</table>

<labkey:errors/>

<form name="<%=text(ImportAnalysisForm.NAME)%>" action="<%=new ActionURL(AnalysisScriptController.ImportAnalysisAction.class, container)%>" method="POST" enctype="multipart/form-data">
    <input type="hidden" name="step" value="<%=form.getStep()%>">
    <%
        Iterator i = form.getWorkspace().getHiddenFields().entrySet().iterator();
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

<% if (form.getWizardStep().getNumber() < AnalysisScriptController.ImportAnalysisStep.CONFIRM.getNumber()) { %>

    <%=generateBackButton()%>
    <%= button("Next").submit(true) %>
    <%= button("Cancel").href(cancelUrl) %>

    <%
        JspView<ImportAnalysisForm> view = null;
        switch (form.getWizardStep())
        {
            case SELECT_ANALYSIS:
                view = new JspView<>("/org/labkey/flow/controllers/executescript/importAnalysisSelectAnalysis.jsp", form);
                break;

            case SELECT_FCSFILES:
                view = new JspView<>("/org/labkey/flow/controllers/executescript/importAnalysisSelectFCSFiles.jsp", form);
                break;

            case REVIEW_SAMPLES:
                view = new JspView<>("/org/labkey/flow/controllers/executescript/importAnalysisReviewSamples.jsp", form);
                break;

            case ANALYSIS_ENGINE:
                view = new JspView<>("/org/labkey/flow/controllers/executescript/importAnalysisAnalysisEngine.jsp", form);
                break;

            case ANALYSIS_OPTIONS:
                view = new JspView<>("/org/labkey/flow/controllers/executescript/importAnalysisAnalysisOptions.jsp", form);
                break;

            case CHOOSE_ANALYSIS:
                view = new JspView<>("/org/labkey/flow/controllers/executescript/importAnalysisChooseAnalysis.jsp", form);
                break;
        }
        include(view, out);
    %>

    <%=generateBackButton()%>
    <%= button("Next").submit(true) %>
    <%= button("Cancel").href(cancelUrl) %>
    
<% } else { %>
    <%
    JspView<ImportAnalysisForm> view = new JspView<>("/org/labkey/flow/controllers/executescript/importAnalysisConfirm.jsp", form);
    include(view, out);
    %>
<% } %>
</form>
