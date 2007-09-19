<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ page import="org.labkey.flow.controllers.compensation.CompensationController.Action" %>
<%@ page import="org.labkey.flow.controllers.compensation.UploadCompensationForm" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<% UploadCompensationForm form = (UploadCompensationForm) __form; %>
<labkey:errors/>
<form method="POST" action="<%=PageFlowUtil.urlFor(Action.upload, getContainer())%>" enctype="multipart/form-data">
    <p>Give your new compensation matrix a name.<br>
        <input type="text" name="ff_compensationMatrixName" value="<%=h(form.ff_compensationMatrixName)%>"/>
    </p>

    <p>You can upload a compensation matrix file that was saved from FlowJo.<br>
        <input type="file" name="ff_compensationMatrixFile">
    </p>
    <labkey:button text="submit"/>
</form>