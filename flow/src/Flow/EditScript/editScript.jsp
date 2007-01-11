<%@ page extends="Flow.EditScript.ScriptController.EditPage" %>
<%@ page import="org.fhcrc.cpas.util.PageFlowUtil"%>
<%@ page import="org.fhcrc.cpas.flow.ScriptParser"%>
<%@ page import="org.fhcrc.cpas.flow.data.FlowScript"%>
<%@ page import="Flow.EditScript.ScriptController"%>
<%=pageHeader(ScriptController.Action.editScript)%>
<% FlowScript script = getScript();
    ScriptParser.Error error = scriptParseError;
%>
<form method="POST" action="<%=h(formAction(ScriptController.Action.editScript))%>">
<% if (error != null) { %>
<p style="color:red"><%=PageFlowUtil.filter(error.getMessage(), true).replaceAll("\\n", "<br>")%></p>
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
<%}%>
<%}%>


<textarea id="scriptTextArea" wrap="off" rows="20" cols="80" name="script"><%=h(script.getAnalysisScript())%></textarea>
<br>
<input type="submit" value="Submit">
</form>
