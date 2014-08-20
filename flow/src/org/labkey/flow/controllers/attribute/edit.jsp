<%
/*
 * Copyright (c) 2014 LabKey Corporation
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
<%@ page import="org.labkey.flow.controllers.attribute.AttributeController" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="org.labkey.flow.persist.AttributeCache" %>
<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    AttributeController.EditAttributeForm form = (AttributeController.EditAttributeForm)getModelBean();
    AttributeCache.Entry entry = form.getEntry();
    String name = form.getName();
    if (StringUtils.isEmpty(name))
        name = entry.getName();

    ActionURL editURL = getActionURL();
    ActionURL returnURL = form.getReturnActionURL(new ActionURL(ProtocolController.BeginAction.class, getContainer()));
%>

<labkey:errors/>
<labkey:form action="<%=editURL%>" method="post">
    <table>
        <tr>
            <td>
                <label for="name">Name:</label>
            </td>
            <td>
                <input type="text" id="name" name="name" value="<%=h(name)%>" size="60" maxlength="255"/>
            </td>
        </tr>
        <tr>
            <td>&nbsp;</td>
            <td>
                <%= button("Submit").submit(true) %>
                <%= button("Cancel").href(returnURL) %>
            </td>
        </tr>
    </table>
</labkey:form>
