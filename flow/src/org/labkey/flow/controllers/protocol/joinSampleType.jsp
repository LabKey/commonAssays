<%
/*
 * Copyright (c) 2007-2019 LabKey Corporation
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
<%@ page import="org.labkey.api.query.FieldKey"%>
<%@ page import="org.labkey.flow.controllers.protocol.JoinSampleTypeForm"%>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController"%>
<%@ page import="java.util.Map"%>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    JoinSampleTypeForm form = (JoinSampleTypeForm) __form;

    if (form.getProtocol().getSampleType(getUser()) == null)
    {
        %>
        <p>You must first upload a sample type before specifying how to match samples to FCS files.</p>
        <%=link("Upload Sample Descriptions", form.getProtocol().urlCreateSampleType())%>
        <%
    }
    else
    {
        Map<String, String> sampleKeyFields = form.getAvailableSampleKeyFields();
        Map<FieldKey, String> dataKeyFields = form.getAvailableDataKeyFields();
        %>

        <p>Use this page to set which properties of the sample need to match keywords of the FCS files.</p>

        <labkey:form action="<%=form.getProtocol().urlFor(ProtocolController.JoinSampleTypeAction.class)%>" method="POST">
            <table><tr><th>Sample Property</th><th>FCS Property</th></tr>
                <% for (int i = 0; i < form.ff_samplePropertyURI.length; i ++)
                { %>
                <tr>
                    <td>
                        <%=select().name("ff_samplePropertyURI").addOptions(sampleKeyFields).selected(form.ff_samplePropertyURI[i]).className(null)%>
                    </td>
                    <td>
                        <%=select().name("ff_dataField").addOptions(dataKeyFields).selected(form.ff_dataField[i]).className(null)%>
                    </td>
                </tr>
                <% } %>
            </table>
            <labkey:button text="update" /> <labkey:button text="cancel" href="<%=form.getProtocol().urlShow()%>" />
        </labkey:form>

        <%
    }
%>
