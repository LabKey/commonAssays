package org.labkey.flow.script;

import org.labkey.flow.data.*;
import org.labkey.flow.data.FlowDataType;
import org.labkey.flow.persist.AttributeSet;
import org.fhcrc.cpas.exp.xml.*;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.data.Container;

import java.util.*;
import java.sql.SQLException;
import java.io.File;

import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;

abstract public class BaseHandler
{
    protected ScriptJob _job;
    protected FlowProtocolStep _step;

    public BaseHandler(ScriptJob job, FlowProtocolStep step)
    {
        _job = job;
        _step = step;
    }

    public ProtocolApplicationBaseType addProtocolApplication(ExperimentRunType run)
    {
        ProtocolApplicationBaseType app = _job.addProtocolApplication(run);
        app.setName(_step.getName());
        app.setProtocolLSID(_step.getLSID(_job.getContainer()));
        app.setActionSequence(_step.getDefaultActionSequence());
        app.setCpasType("ProtocolApplication");
        if (_job._runAnalysisScript != null)
        {
            InputOutputRefsType.DataLSID dataLSID = app.getInputRefs().addNewDataLSID();
            dataLSID.setStringValue(_job._runAnalysisScript.getLSID());
            dataLSID.setRoleName(InputRole.AnalysisScript.toString());

        }
        return app;
    }

    public Container getContainer()
    {
        return _job.getContainer();
    }

    public DataBaseType duplicateWell(ProtocolApplicationBaseType app, FlowWell src, FlowDataType type) throws SQLException
    {
        _job.addInput(app, src, InputRole.FCSFile);
        DataBaseType ret = app.getOutputDataObjects().addNewData();
        ret.setName(src.getName());
        ret.setAbout(FlowDataObject.generateDataLSID(getContainer(), type));
        ret.setSourceProtocolLSID(_step.getLSID(_job.getContainer()));
        PropertyCollectionType pct = ret.addNewProperties();
        return ret;
    }

    synchronized public void addResults(DataBaseType dbt, AttributeSet attrs, List<? extends FCSAnalyzer.Result> results) throws Exception
    {
        for (FCSAnalyzer.Result result : results)
        {
            if (logException(dbt.getAbout(), result))
                continue;
            if (result instanceof FCSAnalyzer.StatResult)
            {
                FCSAnalyzer.StatResult statResult = (FCSAnalyzer.StatResult) result;
                attrs.setStatistic(statResult.spec, statResult.value);
            }
            else if (result instanceof FCSAnalyzer.GraphResult)
            {
                FCSAnalyzer.GraphResult graphResult = (FCSAnalyzer.GraphResult) result;
                attrs.setGraph(graphResult.spec, graphResult.bytes);
            }
        }
    }

    protected boolean logException(String lsid, FCSAnalyzer.Result res)
    {
        if (res.exception == null)
            return false;
        _job.addError(lsid, res.spec.toString(), res.exception.toString());
        return true;
    }

    abstract public void processRun(FlowRun srcRun, ExperimentRunType runElement, File workingDirectory) throws Exception;

    protected void addDataLSID(InputOutputRefsType refs, String lsid, InputRole role)
    {
        InputOutputRefsType.DataLSID dataLSID = refs.addNewDataLSID();
        dataLSID.setStringValue(lsid);
        if (role != null)
        {
            dataLSID.setRoleName(role.toString());
        }
    }
}
