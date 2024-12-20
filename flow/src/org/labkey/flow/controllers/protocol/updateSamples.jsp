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
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController"%>
<%@ page import="org.labkey.flow.controllers.protocol.UpdateSamplesForm"%>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<% UpdateSamplesForm form = (UpdateSamplesForm) __form;%>
<labkey:errors />
<p>
    <%= form.fileCount%> FCS files were linked to samples in this sample type.
</p>
<p><a href="<%=h(form.getProtocol().urlShowSamples())%>">Show linked samples</a><br>
<a href="<%=h(form.getProtocol().getSampleType(getUser()).detailsURL())%>">Show all samples</a><br>
<a href="<%=h(form.getProtocol().urlFor(ProtocolController.JoinSampleTypeAction.class))%>">Edit join properties</a></p>