package org.labkey.ms1;

import org.labkey.api.query.QueryView;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.view.ViewContext;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.DataRegion;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpRun;

/**
 * Implements a query view over the features data, allows filtering for features from a specific run
 *
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Oct 3, 2007
 * Time: 11:27:01 AM
 */
public class FeaturesView extends QueryView
{
    public FeaturesView(ViewContext ctx, int runId)
    {
        super(new MS1Schema(ctx.getUser(), ctx.getContainer()));

        //this is a bit hacky, but the call to the super's c-tor must be the first line
        //and the super stores the schema as a UserSchema, not a specific sub-type.
        //however, we need it as an MS1Schema so we can call getFeaturesTableInfo()
        _ms1Schema = (MS1Schema)getSchema();

        _runId = runId;

        QuerySettings settings = new QuerySettings(ctx.getViewURLHelper(), ctx.getRequest(), "Features");
        settings.setQueryName(MS1Schema.TABLE_FEATURES);
        settings.setAllowChooseQuery(false);
        setSettings(settings);

        ExpRun run = ExperimentService.get().getExpRun(runId);
        setTitle("MS1 Features from " + run.getName());
        setShowCustomizeViewLinkInButtonBar(true);
        setShowRecordSelectors(false);
        setShowExportButtons(false);
    }

    protected TableInfo createTable()
    {
        assert null != _ms1Schema : "MS1 Schema was not set in FeaturesView class!";

        FeaturesTableInfo tinfo = _ms1Schema.getFeaturesTableInfo();
        if(_runId >= 0)
            tinfo.addRunIdCondition(_runId);
        return tinfo;
    }

    protected DataRegion createDataRegion()
    {
        DataRegion ret = super.createDataRegion();
        ret.setShadeAlternatingRows(true);
        return ret;
    }

    private MS1Schema _ms1Schema;
    private int _runId = -1;
} //class FeaturesView
