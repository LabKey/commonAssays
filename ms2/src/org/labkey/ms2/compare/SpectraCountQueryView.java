package org.labkey.ms2.compare;

import org.labkey.api.query.QueryView;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.CreateChartButton;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.ActionButton;
import org.labkey.api.view.ActionURL;
import org.labkey.api.reports.report.view.ChartUtil;
import org.labkey.api.reports.report.view.RReportBean;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.SpectraCountConfiguration;
import org.labkey.ms2.MS2Controller;

/**
 * User: jeckels
* Date: Jan 22, 2008
*/
public class SpectraCountQueryView extends QueryView
{
    private final MS2Schema _schema;
    private final SpectraCountConfiguration _config;
    private MS2Controller.SpectraCountForm _form;

    public SpectraCountQueryView(MS2Schema schema, QuerySettings settings, SpectraCountConfiguration config, MS2Controller.SpectraCountForm form)
    {
        super(schema, settings);
        _schema = schema;
        _config = config;
        _form = form;
    }

    protected TableInfo createTable()
    {
        return _schema.createSpectraCountTable(_config, getViewContext(), _form);
    }

    public ActionButton createReportButton()
    {
        RReportBean bean = new RReportBean();
        bean.setReportType(SpectraCountRReport.TYPE);
        bean.setSchemaName(getSchema().getSchemaName());
        bean.setQueryName(getSettings().getQueryName());
        bean.setViewName(getSettings().getViewName());
        bean.setDataRegionName(getDataRegionName());
	// the redirect after a save
        bean.setRedirectUrl(getViewContext().getActionURL().toString());

        ActionURL chartURL = ChartUtil.getRReportDesignerURL(_viewContext, bean);
        chartURL = SpectraCountRReport.addReportParameters(chartURL, getViewContext());

        return new CreateChartButton(chartURL.toString(), false, true,
                getSchema().getSchemaName(), getSettings().getQueryName(), getSettings().getViewName(), SpectraCountRReport.TYPE);
    }
}
