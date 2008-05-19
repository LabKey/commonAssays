<%
/*
 * Copyright (c) 2004-2008 Fred Hutchinson Cancer Research Center
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
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.PickViewBean bean = ((JspView<MS2Controller.PickViewBean>)HttpView.currentView()).getModelBean();
%>
<p><%=h(bean.viewInstructions)%></p>
<p>To define a view: display a run, filter the run, click "Save View", and pick a name.  The filter you've set up will be saved with the view and the name will then appear in the list below.</p>
<form method="get" action="<%=h(bean.nextURL)%>">
    <input type="hidden" name="runList" value="<%=bean.runList%>">
    <%=bean.select%><br/>
    <br/><br/>
    <% out.flush(); bean.extraOptionsView.render(request, response); %><br/>
    <input type="image" src="<%=PageFlowUtil.buttonSrc("Go")%>" name="submit">
</form>
