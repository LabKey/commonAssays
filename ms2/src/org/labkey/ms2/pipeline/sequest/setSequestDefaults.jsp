<%@ page import="org.labkey.api.pipeline.PipelineUrls"%>
<%@ page import="org.labkey.ms2.pipeline.PipelineController"%>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
PipelineController.SetDefaultsForm form = (PipelineController.SetDefaultsForm) getForm();
%>
<labkey:errors />
<form method="post" action="<%=urlFor(PipelineController.SetSequestDefaultsAction.class)%>">
<table border="0">
    <tr><td class='ms-searchform'>Sequest<br>Default XML:</td>
        <td class='ms-vb'><textarea name="configureXml" cols="90" rows="20"><%=form.getConfigureXml()%></textarea><br>
                    For detailed explanations of all available input parameters, see the
                    <a href="http://fields.scripps.edu/sequest/index.html">Sequest Documentation</a> on-line.</td></tr>
    <tr><td colspan="2"><labkey:button text="Set Defaults"/>&nbsp;<labkey:button text="Cancel" href="<%=urlProvider(PipelineUrls.class).urlReferer(getContainer())%>"/></td></tr>
</table>
</form>
<script for=window event=onload>
try {document.getElementById("analysisName").focus();} catch(x){}
</script>
