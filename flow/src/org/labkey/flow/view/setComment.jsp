<%
/*
 * Copyright (c) 2008 LabKey Corporation
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
<%@ page import="org.labkey.api.security.ACL"%>
<%@ page import="org.labkey.api.view.ActionURL"%>
<%@ page import="org.labkey.flow.data.FlowObject" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    FlowObject flowObj = (FlowObject)getModelBean();
    ActionURL setFlagUrl = new ActionURL("Experiment", "setFlag", "");
    setFlagUrl.addParameter("flagSessionId", request.getSession().getId());
    setFlagUrl.addParameter("lsid", flowObj.getLSID());
    setFlagUrl.addParameter("redirect", false);

    boolean canEdit = getViewContext().hasPermission(ACL.PERM_UPDATE);
%>
<% if (canEdit) { %>
<script type="text/javascript">
    LABKEY.requiresExtJs(false);
</script>
<input class="extContainer" type="text"
       id="comment" name="comment" size="65"
       value="<%=h(flowObj.getExpObject().getComment())%>" />
<script type="text/javascript">
var textField = new Ext.form.TextField({
   applyTo: 'comment',
   emptyText: "Type to enter a comment",
   labelLength: 65,
   listeners: {
       'change': function (self, newValue, oldValue) {
           self.doSubmit();
       }
   }
});
textField.doSubmit = function () {
    if (textField.originalValue !== textField.getValue())
    {
        Ext.Ajax.request({
            url: "<%=setFlagUrl%>&comment=" + textField.getValue()
        });
        textField.originalValue = textField.getValue();
    }
}
textField.el.on("keypress", function (e) {
    if (e.getKey() == Ext.EventObject.ENTER) {
        textField.doSubmit();
    }
}, textField);
</script>
<% } else { %>
<%=h(flowObj.getExpObject().getComment())%>
<% } %>
