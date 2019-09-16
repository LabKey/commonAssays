<%
/*
 * Copyright (c) 2019 LabKey Corporation
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
<%@ page import="org.labkey.flow.controllers.attribute.AttributeController" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    AttributeController.CaseSensitivityForm form = (AttributeController.CaseSensitivityForm)getModelBean();
%>

When checked, an error will be raised when importing flow data containing an attribute
that has different casing than an existing attribute.<br>
When unchecked, attributes that differ by case is allowed.
<p>
<labkey:form method="POST" action="<%=new ActionURL(AttributeController.CaseSensitivityAction.class, getContainer())%>">
    <labkey:checkbox name="caseSensitiveKeywords" id="caseSensitiveKeywords"
                     checked="<%=form.isCaseSensitiveKeywords()%>" value="true"/>
    <label for="caseSensitiveKeywords">Case-sensitive keywords</label><br>

    <labkey:checkbox name="caseSensitiveStatsAndGraphs" id="caseSensitiveStatsAndGraphs"
                     checked="<%=form.isCaseSensitiveStatsAndGraphs()%>" value="true"/>
    <label for="caseSensitiveStatsAndGraphs">Case-sensitive statistic and graph specifications</label><br>

    <br>

    <%= button("Cancel").href(form.getReturnActionURL(new ActionURL(ProtocolController.ShowProtocolAction.class, getContainer()))) %>
    <%= button("Submit").submit(true) %>
</labkey:form>
