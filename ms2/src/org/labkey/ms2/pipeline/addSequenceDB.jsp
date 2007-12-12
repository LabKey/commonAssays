<%@ page import="org.labkey.api.pipeline.PipelineService"%>
<%@ page import="org.labkey.api.util.PageFlowUtil"%>
<%@ page import="org.labkey.ms2.pipeline.PipelineController" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<form method="post" action="<%=urlFor(PipelineController.AddSequenceDBAction.class)%>" enctype="multipart/form-data">
<labkey:errors />
<table border="0">
    <tr><td class='ms-searchform'>FASTA File:</td>
        <td class='ms-vb'><input size="70" type="file" name="sequenceDBFile"></td></tr>
    <tr><td><labkey:button text="Add"/>&nbsp;<labkey:button text="Cancel" href="<%=PipelineService.get().urlReferer(getContainer())%>"/></td></tr>
</table>
</form>
<script for=window event=onload>
try {document.getElementById("sequenceDBFile").focus();} catch(x){}
</script>
