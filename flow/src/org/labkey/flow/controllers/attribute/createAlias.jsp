<%
/*
 * Copyright (c) 2014-2018 LabKey Corporation
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
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.controllers.attribute.AttributeController" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="org.labkey.flow.persist.AttributeCache" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    AttributeController.CreateAliasForm form = (AttributeController.CreateAliasForm)getModelBean();
    AttributeCache.Entry entry = form.getEntry(getContainer());

    ActionURL editURL = getActionURL();
    ActionURL returnURL = form.getReturnActionURL(new ActionURL(ProtocolController.BeginAction.class, getContainer()));
%>

Create alias for <%=h(entry.getType().name())%>: <%=h(entry.getName())%>
<p>

<div id="form-errors">
    <labkey:errors/>
</div>
<labkey:form onsubmit="return validateForm();" id="alias-form" action="<%=editURL%>" method="post">
    <table>
        <tr>
            <td>
                <label for="alias">Alias:</label>
            </td>
            <td>
                <input type="text" id="alias" name="alias" value="<%=h(form.getAlias())%>" size="60" maxlength="255"/>
            </td>
        </tr>
        <tr>
            <td>&nbsp;</td>
            <td>
                <%= button("Submit").submit(true) %>
                <%= button("Cancel").href(returnURL) %>
            </td>
        </tr>
    </table>
    <labkey:input type="hidden" id="allowCaseChangeAlias" name="allowCaseChangeAlias" value="false"/>
</labkey:form>

<script type="text/javascript" nonce="<%=getScriptNonce()%>">
    var errorEl = document.getElementById('form-errors');
    function showError(msg) {
        errorEl.innerHTML = '<div class="labkey-error">' + LABKEY.Utils.encodeHtml(msg) + '</div>';
    }

    var formEl = document.getElementById('alias-form');
    var aliasEl = document.getElementById('alias');
    var allowCaseChangeAliasEl = document.getElementById('allowCaseChangeAlias');

    function validateForm() {
        errorEl.innerHTML = '';

        var currentAttribute = <%=q(entry.getName())%>;
        var newAlias = aliasEl.value;

        if (!newAlias) {
            showError('Alias name must not be blank');
            return false;
        }

        if (newAlias === currentAttribute) {
            showError('Alias must not be identical to the original');
            return false;
        }

        if (newAlias.toLowerCase() === currentAttribute.toLowerCase()) {
            var type = <%=q(entry.getType().name())%>;
            var result = confirm("Current " + type + " '" + LABKEY.Utils.encodeHtml(currentAttribute) + "' and alias '" + newAlias + "' only differ by casing.\n\nWould you like to continue creating the alias?");
            if (!result)
                return false;

            allowCaseChangeAliasEl.value = true;
        }

        return true;
    };
</script>

