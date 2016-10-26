<%
    /*
     * Copyright (c) 2014 LabKey Corporation
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
<%@ page import="org.apache.commons.beanutils.ConvertUtils"%>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.luminex.LuminexController" %>
<%@ page import="org.labkey.luminex.LuminexRunUploadForm" %>
<%@ page import="org.labkey.api.util.StringUtilsLabKey" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.study.assay.AssayUrls" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    JspView<LuminexRunUploadForm> me = (JspView<LuminexRunUploadForm>) HttpView.currentView();
    LuminexRunUploadForm bean = me.getModelBean();
    long exclusionCount = bean.getExclusionCount();
    long lostExclusions = bean.getLostExclusions();
%>

<span>The run you are replacing has
    <%=text(StringUtilsLabKey.pluralize(exclusionCount, "matching exclusion"))%><%
        if (lostExclusions > 0)
    {
    %>, <span class="labkey-error">
        <%=text(StringUtilsLabKey.pluralize(lostExclusions, "exclusion was", "exclusions were") + " not matched")%></span><%
        }
    %>. Would you like to?

    <div style="margin-left: 25px;">
        <%--default to checked--%>
        <label><input type="checkbox" name="retainExclusions" <%=checked(bean.getRetainExclusions())%> onchange="retainExclusionsChanged(this)" />
            <%=text("Retain matched exclusion" + (exclusionCount == 1 ? "" :"s"))%></label><br/>
    </div>
    <br/><%
        ActionURL url = PageFlowUtil.urlProvider(AssayUrls.class).getProtocolURL(bean.getContainer(), bean.getProtocol(), LuminexController.ExcludedDataAction.class);

        Integer currentRunId = bean.getReRunId();
        if (null != currentRunId)
        {
            url.addParameter("WellExclusion.DataId/Run/RowId~eq", currentRunId);
            url.addParameter("RunExclusion.RunId~eq", currentRunId);
            url.addParameter("TitrationExclusion.DataId/Run/RowId~eq", currentRunId);
        }

    %>Please review the <%=textLink("exclusions report", url, true)%>for more information.
</span>

<script type="text/javascript">

    function retainExclusionsChanged(el)
    {
        var hiddenEl = getHiddenFormElement(el.name);
        if (hiddenEl != null)
            hiddenEl.value = el.checked;
    }

    //Depends on getHiddenFormElement from titrationWellRoles.jsp

</script>