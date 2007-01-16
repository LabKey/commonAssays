<%@ page import="org.labkey.flow.util.PFUtil" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ page import="org.labkey.flow.controllers.compensation.CompensationController.Action" %>
<%@ page import="org.labkey.flow.controllers.compensation.UploadCompensationForm" %>
<%@ taglib prefix="cpas" uri="http://cpas.fhcrc.org/taglib/cpas" %>
<% UploadCompensationForm form = (UploadCompensationForm) __form; %>
<cpas:errors/>
<form method="POST" action="<%=PFUtil.urlFor(Action.upload, getContainer())%>" enctype="multipart/form-data">
    <p>Give your new compensation matrix a name.<br>
        <input type="text" name="ff_compensationMatrixName" value="<%=h(form.ff_compensationMatrixName)%>"/>
    </p>

    <p>You can upload a compensation matrix file that was saved from FlowJo.<br>
        <input type="file" name="ff_compensationMatrixFile">
    </p>
    <cpas:button text="submit"/>
</form>