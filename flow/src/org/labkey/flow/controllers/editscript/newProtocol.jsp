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
<%@ page import="org.labkey.flow.controllers.editscript.NewProtocolForm" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    NewProtocolForm form = (NewProtocolForm) __form;
%>
<labkey:errors/>

<labkey:form method="post">
    <p>What do you want to call this analysis script?<br>
        <input type="text" id="ff_name" name="ff_name" value="<%=h(form.ff_name)%>">
    </p>

    <labkey:button text="Create Analysis Script" />
</labkey:form>