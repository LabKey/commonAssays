<%
/*
 * Copyright (c) 2008 LabKey Corporation
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
<%@ page import="org.labkey.api.data.CompareType" %>
<%@ page import="org.labkey.flow.controllers.protocol.EditICSMetadataForm" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%! void addCompare(Map<String, String> options, CompareType ct)
{
    options.put(ct.getUrlKey(), ct.getDisplayValue());
}%>
<%
    EditICSMetadataForm form = (EditICSMetadataForm) __form;
//    Map<FieldKey, String> fieldOptions = new LinkedHashMap();
//    fieldOptions.put(null, "");
//    fieldOptions.putAll(form.getKeywordFieldMap());
//    Map<String, String> opOptions = new LinkedHashMap();
//    addCompare(opOptions, CompareType.EQUAL);
//    addCompare(opOptions, CompareType.NEQ_OR_NULL);
//    addCompare(opOptions, CompareType.ISBLANK);
//    addCompare(opOptions, CompareType.NONBLANK);
//    addCompare(opOptions, CompareType.STARTS_WITH);
//    addCompare(opOptions, CompareType.CONTAINS);
%>
<labkey:errors />
<form action="<%=form.getProtocol().urlFor(ProtocolController.Action.editICSMetadata)%>" method="POST">
    <table>
        <tr class="labkey-wp-header"><th align="left">ICS Metadata:</th></tr>
        <tr>
            <td>
                <textarea name="metadata" rows="30" cols="120" style="width:100%"><%=form.getMetadata()%></textarea>
            </td>
        </tr>
    </table>
    <labkey:button text="Set ICS Metadata" /> <labkey:button text="Cancel" href="<%=form.getProtocol().urlShow()%>"/>
</form>