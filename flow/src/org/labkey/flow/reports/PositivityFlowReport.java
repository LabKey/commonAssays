package org.labkey.flow.reports;

import org.labkey.api.view.HttpView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.ViewContext;
import org.springframework.beans.PropertyValues;
import org.springframework.validation.BindException;

/**
 * User: kevink
 * Date: 5/26/11
 */
public class PositivityFlowReport extends FlowReport
{
    public static final String TYPE = "Flow.PositivityReport";
    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public String getTypeDescription()
    {
        return "Flow Positivity Call";
    }

    @Override
    public HttpView getConfigureForm()
    {
        return new JspView<PositivityFlowReport>(PositivityFlowReport.class, "editPositivityReport.jsp", this);
    }

    @Override
    public boolean updateProperties(PropertyValues pvs, BindException errors, boolean override)
    {
        super.updateBaseProperties(pvs, errors, override);
        //updateFormPropertyValues(pvs, "statistic");
        if (!override)
            ;//updateFilterProperties(pvs);
        return true;
    }

    @Override
    public HttpView renderReport(ViewContext context) throws Exception
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
