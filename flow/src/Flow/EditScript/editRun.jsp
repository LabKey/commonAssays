<%@ page import="Flow.EditScript.ScriptController" %>
<%@ page extends="Flow.EditScript.ScriptController.Page" %>
<script src="<%=request.getContextPath()%>/Flow/editScript.js"></script>
<%ScriptController.RunForm bean = (ScriptController.RunForm) form; %>
<%=pageHeader(ScriptController.Action.editRun)%>
<script>
    function addKeyword()
    {
        var selKeyword = document.getElementById("sel_keywords");

        var keyword = selKeyword.options[selKeyword.selectedIndex].value;
        var elKeywords = document.getElementsByName("keywords")[0];
        var keywords = elKeywords.value;
        appendLine(elKeywords, keyword);
    }
</script>

<form method="POST" action="<%=formAction(ScriptController.Action.editRun)%>">
    <p>
        Which FCS keyword identifies the name of the well?<br>
        <input type="text" name="wellNameKeyword" value="<%=h(bean.wellNameKeyword)%>"><br>
        <i>(This defaults to using the filename of the FCS file as the name of the well)</i>
    </p>

    <p> Which FCS keyword identifies the name of the run?<br>
        <input type="text" name="runNameKeyword" value="<%=h(bean.runNameKeyword)%>"><br>
        <i>(This defaults to using the name of the directory containing the FCS files as the name of the run)</i>
    </p>

    <p>
        What keywords are you interested in?<br>
        <textarea name="keywords" wrap="off" rows="10" cols="20"><%=h(bean.keywords)%></textarea><br>
        <i>(Leave this blank to get the default set of keywords, which excludes most of those that start with '$')</i><br>
        <i>(Enter one keyword per line. Include keywords which may not appear in the FCS files, but which will be
            entered later)
        </i>
    </p>
    <% String[] keywords = bean.getAvailableKeywords();
        if (keywords.length > 0)
        {
    %>
    <p>
        <select id="sel_keywords">
            <% for (String keyword : keywords)
            { %>
            <option value="<%=h(keyword)%>"><%=h(keyword)%></option>
            <% } %>
        </select>
        <input type="button" value="Add Keyword" onclick="addKeyword()">
    </p>
    <%
        }
    %>
    <input type="submit" value="Submit">
</form>