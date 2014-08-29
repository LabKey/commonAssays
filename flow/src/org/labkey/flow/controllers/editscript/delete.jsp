<%
/*
 * Copyright (c) 2006-2014 LabKey Corporation
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
<%@ page import="org.labkey.flow.data.FlowScript" %>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page extends="org.labkey.flow.controllers.editscript.ScriptController.Page" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%  FlowScript script = getForm().getFlowScript();
    int runCount = script.getRunCount();
%>
<labkey:errors/>
<% if (runCount > 0) { %>
<p>This analysis script cannot be deleted because it has been used by <%=runCount%> runs.  You must delete the runs that use this script before it can be deleted.<br>
<labkey:button text="Go Back" href="<%=script.urlShow()%>" />
</p>
<% } else { %>
<p>Are you sure that you want to delete the analysis script '<%=h(script.getName())%>'?<br>
    <labkey:form action="<%=script.urlFor(ScriptController.DeleteAction.class)%>" method="POST">
        <labkey:button text="OK" /> <labkey:button text="Cancel" href="<%=script.urlShow()%>" />
    </labkey:form>
</p>
<% } %>
