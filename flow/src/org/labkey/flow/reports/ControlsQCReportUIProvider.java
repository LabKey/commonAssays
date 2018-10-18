package org.labkey.flow.reports;

import org.labkey.api.data.Container;
import org.labkey.api.reports.ReportService;
import org.labkey.api.reports.report.view.DefaultReportUIProvider;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.flow.FlowModule;

import java.util.ArrayList;
import java.util.List;

public class ControlsQCReportUIProvider extends DefaultReportUIProvider
{
    @Override
    public List<ReportService.DesignerInfo> getDesignerInfo(ViewContext context)
    {
        List<ReportService.DesignerInfo> designers = new ArrayList<>();
        Container c = context.getContainer();
        if (c.hasActiveModuleByName(FlowModule.NAME))
        {
            ActionURL designerURL = ControlsQCReport.createURL(c, context.getActionURL(), null);
            DesignerInfoImpl info = new DesignerInfoImpl(ControlsQCReport.TYPE, "Flow QC Report", ControlsQCReport.DESC, designerURL, null);
            designers.add(info);
        }
        return designers;
    }
}
