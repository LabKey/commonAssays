<%
/*
 * Copyright (c) 2007-2016 LabKey Corporation
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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.ms2.protein.CustomAnnotationType" %>
<%@ page import="org.labkey.ms2.protein.ProteinController" %>
<%@ page extends="org.labkey.api.jsp.JspBase"%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("clientapi/ext3");
    }
%>

<%
    JspView<ProteinController.UploadAnnotationsForm> me = (JspView<ProteinController.UploadAnnotationsForm>) HttpView.currentView();
    ProteinController.UploadAnnotationsForm bean = me.getModelBean();
%>

<labkey:errors/>

<labkey:form action="uploadCustomProteinAnnotations.post" name="proteinListForm" method="POST">
    <table>
        <tr>
            <td colspan="2">
                <p>
                    Upload your protein annotation set as tab-separated values (TSV). You can include additional values
                    associated with each protein, or just upload a list of proteins.
                </p>
                <p>
                    The first line of the file must be the column headings. The value in the first column must be the
                    name that refers to the protein, based on the type that you select. For example, if you choose IPI
                    as the type, the first column must be the IPI number (without version information). Each protein
                    must be on a separate line.
                </p>
                <p>
                    An easy way to copy a TSV to the clipboard is to use Excel or another spreadsheet program to enter
                    your data, select all the cells, and copy it. You can then paste into the textbox below.
                </p>
                <p>
                    You can download a <a href="http://www.labkey.org/download/ProteinAnnotationSet.tsv">sample file</a>
                    from the labkey.org server for an example of what a file should look like.<br/>
                </p>
            </td>
        </tr>
        <tr>
            <td>
                Name:
            </td>
            <td>
                <input type="text" name="name" size="50" value="<%= h(bean.getName()) %>" />
            </td>
        </tr>
        <tr>
            <td>
                Type:
            </td>
            <td>
                <select name="annotationType">
                    <% for (CustomAnnotationType type : CustomAnnotationType.values())
                    { %>
                        <option <% if (type.toString().equals(bean.getAnnotationType())) { %> selected <% } %> value="<%= type.toString() %>"><%= type.getDescription() %></option>
                    <% } %>
                </select>
            </td>
        </tr>
        <tr>
            <td valign="top">
                Annotations:
            </td>
            <td width="100%">
                <textarea id="annotationsText" style="width: 100%" rows="15" cols="50" name="annotationsText"><%= h(bean.getAnnotationsText()) %></textarea>
                <script type="text/javascript">
                    Ext.EventManager.on('annotationsText', 'keydown', LABKEY.ext.Utils.handleTabsInTextArea);
                </script>
            </td>
        </tr>
        <tr>
            <td></td>
            <td><labkey:button text="Submit" /></td>
        </tr>
    </table>
</labkey:form>

<script type="text/javascript">
    document.forms.proteinListForm.name.focus();
</script>