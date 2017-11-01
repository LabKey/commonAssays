<%
/*
 * Copyright (c) 2006-2017 LabKey Corporation
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
<%@ page import="org.labkey.flow.ScriptParser"%>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController"%>
<%@ page import="org.labkey.flow.data.FlowScript"%>
<%@ page extends="org.labkey.flow.controllers.editscript.ScriptController.EditPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    FlowScript script = getScript();
    ScriptParser.Error error = scriptParseError;
%>
<labkey:form method="POST" action="<%=h(formAction(ScriptController.EditScriptAction.class))%>">
<% if (error != null) { %>
<p class="labkey-error"><%=h(error.getMessage(), true).replaceAll("\\n", "<br>")%></p>
<% if (error.getLine() != 0) { %>
<script>
    function findOffset(text, line, column)
    {
        var offset = 0;
        text = text.replace("\r\n", "\n");

        while (line > 0)
        {
            line --;
            offset = text.indexOf("\n", offset);
            if (offset < 0)
                return text.length();
            offset ++;
        }

        return offset + column;
    }

    function positionCursor()
    {
        var textArea = document.getElementById("scriptTextArea");
        var tr = textArea.createTextRange();
        tr.moveStart("character", findOffset(textArea.innerText, <%=error.getLine() - 1%>, <%= error.getColumn() - 1%>));
        tr.collapse(true);
        tr.select();
    }

    window.setTimeout(positionCursor, 1);
</script>
<%
    }
}
%>


<textarea id="scriptTextArea" wrap="off" rows="20" cols="80" name="script"><%=h(script.getAnalysisScript())%></textarea>
    <script type="text/javascript">
        LABKEY.requiresExt3ClientAPI(function() {
            Ext.EventManager.on('scriptTextArea', 'keydown', LABKEY.ext.Utils.handleTabsInTextArea);
        });
    </script>
<br>
<input class="labkey-button" type="Submit" value="Submit">
</labkey:form>
