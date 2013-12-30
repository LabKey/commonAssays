<%
/*
 * Copyright (c) 2009-2013 LabKey Corporation
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
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.metadata.FractionsDisplayColumn" %>
<%@ page import="org.labkey.ms2.metadata.MassSpecMetadataAssayForm" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MassSpecMetadataAssayForm form = ((JspView<MassSpecMetadataAssayForm>)HttpView.currentView()).getModelBean();
    boolean fractions = form.isFractions();
%>
<input type="checkbox" id="<%= FractionsDisplayColumn.FRACTIONS_FIELD_NAME %>" value="true"<%=checked(fractions)%>
       name="<%= FractionsDisplayColumn.FRACTIONS_FIELD_NAME %>"> All the files are fractions of a single sample<%= PageFlowUtil.helpPopup("Fractions", "<p>Check the box if the files in this directory are the results from fractionating a single sample and running the individual files through the mass spec.</p><p>Leave the box unchecked if the files are from unrelated samples.</p>", true)%>
