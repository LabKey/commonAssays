<%
/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="java.util.Map" %>
<%
    MS2Controller.ModificationBean bean = ((JspView<MS2Controller.ModificationBean>)HttpView.currentView()).getModelBean();
    Map<String, String> fixed = bean.fixed;
    Map<String, String> var = bean.var;
%>
<table width="100%">
<%  if (0 == (var.size() + fixed.size()))
        out.print("<tr><td colspan=2><b>None</b></td></tr>\n");

    if (0 != fixed.size())
    {
        out.print("<tr><td colspan=2><b>Fixed</b></td></tr>\n");

        for (String key : fixed.keySet())
            out.print("<tr><td>" + key + "</td><td align=right>" + fixed.get(key) + "</td></tr>\n");
    }

    if (0 != var.size())
    {
        if (0 != fixed.size())
            out.print("<tr><td colspan=2>&nbsp;</td></tr>\n");

        out.print("<tr><td colspan=2><b>Variable</b></td></tr>\n");

        for (String key : var.keySet())
            out.print("<tr><td>" + key + "</td><td align=right>" + var.get(key) + "</td></tr>\n");
    }
%></table>