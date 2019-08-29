<%
/*
 * Copyright (c) 2004-2016 Fred Hutchinson Cancer Research Center
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
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.PickViewBean bean = ((JspView<MS2Controller.PickViewBean>)HttpView.currentView()).getModelBean();
%>
<p><%=h(bean.viewInstructions)%></p>
<p>
    To create a view, go back to the
    <a href="<%= new ActionURL(MS2Controller.ShowListAction.class, getContainer())%>">list of MS2 runs</a>
    and click to view one of the individual runs. Apply the filters that you want. Click on the "Save View" button,
    and pick a name. The next time you do a comparison, your saved view will appear in the list below and you can
    select it to apply the same filter to your comparison.
</p>
<labkey:form method="get" action="<%=h(bean.nextURL)%>">
    <input type="hidden" name="runList" value="<%=bean.runList%>">
    <%=bean.select%><br/>
    <br/><br/>
    <% out.flush(); bean.extraOptionsView.render(request, response); %><br/>
    <%= button(bean.buttonText).submit(true).attributes("name=\"submit\"") %>
</labkey:form>
