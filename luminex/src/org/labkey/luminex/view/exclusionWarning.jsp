<%
/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
<%@ page import="org.labkey.api.data.ContainerFilter" %>
<%@ page import="org.labkey.api.util.StringUtilsLabKey" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.luminex.LuminexAssayProvider" %>
<%@ page import="org.labkey.luminex.LuminexRunUploadForm" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    JspView<LuminexRunUploadForm> me = (JspView<LuminexRunUploadForm>) HttpView.currentView();
    LuminexRunUploadForm bean = me.getModelBean();
    long exclusionCount = bean.getExclusionCount();
    long lostExclusions = bean.getLostExclusions();
%>

<span>The run you are replacing has
    <%=unsafe(StringUtilsLabKey.pluralize(exclusionCount, "exclusion", "exclusions"))%><%
        if (lostExclusions > 0)
    {
    %>, <span class="labkey-error">
        <%=unsafe(StringUtilsLabKey.pluralize(lostExclusions, "exclusion was", "exclusions were") + " not matched and will be lost")%></span><%
        }
    %>. Would you like to?

    <div style="margin-left: 25px;">
        <%--default to checked--%>
        <input type="checkbox" id="retainExclusions" name="retainExclusions"<%=checked(bean.getRetainExclusions())%>/>
        <%=h("Retain matched exclusion" + (exclusionCount == 1 ? "" :"s"))%>
        <br/>
    </div>
    <br/><%
        ActionURL url = LuminexAssayProvider.getExcludedReportUrl(bean.getContainer(), bean.getProtocol(), ContainerFilter.Type.Current, bean.getReRunId().toString());
    %>Please review the <%=link("exclusions report", url).target("_blank")%>for more information.
</span>

<script type="text/javascript" nonce="<%=getScriptNonce()%>">
    LABKEY.Utils.onReady(function() {
        document.getElementById("retainExclusions")['onchange'] = function() { retainExclusionsChanged(this) };
    });

    function retainExclusionsChanged(el) {
        var hiddenEl = getHiddenFormElement(el.name);
        if (hiddenEl != null)
            hiddenEl.value = el.checked;
    }

    //Depends on getHiddenFormElement from titrationWellRoles.jsp

</script>