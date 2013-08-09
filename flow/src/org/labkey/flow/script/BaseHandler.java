/*
 * Copyright (c) 2005-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.flow.script;

import org.fhcrc.cpas.exp.xml.*;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.flow.analysis.web.FCSAnalyzer;
import org.labkey.flow.data.*;
import org.labkey.flow.persist.AttributeSet;
import org.labkey.flow.persist.InputRole;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

abstract public class BaseHandler
{
    protected ScriptJob _job;
    protected FlowProtocolStep _step;

    public BaseHandler(ScriptJob job, FlowProtocolStep step)
    {
        _job = job;
        _step = step;
    }

    public ProtocolApplicationBaseType addProtocolApplication(ExperimentRunType run, String scriptLSID)
    {
        ProtocolApplicationBaseType app = _job.addProtocolApplication(run);
        app.setName(_step.getName());
        app.setProtocolLSID(_step.getLSID(_job.getContainer()));
        app.setActionSequence(_step.getDefaultActionSequence());
        app.setCpasType(ExpProtocol.ApplicationType.ProtocolApplication.toString());
        if (scriptLSID != null)
        {
            InputOutputRefsType.DataLSID dataLSID = app.getInputRefs().addNewDataLSID();
            dataLSID.setStringValue(scriptLSID);
            dataLSID.setRoleName(InputRole.AnalysisScript.toString());

        }
        return app;
    }

    public ProtocolApplicationBaseType addProtocolApplication(ExperimentRunType run)
    {
        return addProtocolApplication(run, _job._runAnalysisScript == null ? null : _job._runAnalysisScript.getLSID());
    }

    public Container getContainer()
    {
        return _job.getContainer();
    }

    public DataBaseType duplicateWell(ProtocolApplicationBaseType app, FlowWell src, FlowDataType type)
    {
        _job.addInput(app, src, InputRole.FCSFile);
        DataBaseType ret = app.getOutputDataObjects().addNewData();
        ret.setName(src.getName());
        ret.setAbout(FlowDataObject.generateDataLSID(getContainer(), type));
        PropertyCollectionType pct = ret.addNewProperties();
        return ret;
    }

    synchronized public void addResults(DataBaseType dbt, AttributeSet attrs, List<? extends FCSAnalyzer.Result> results)
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

        StringBuilder sb = new StringBuilder();
        sb.append("Error generating ");
        if (res instanceof FCSAnalyzer.StatResult)
            sb.append("statistic ");
        else if (res instanceof  FCSAnalyzer.GraphResult)
            sb.append("graph ");
        sb.append(res.spec.toString()).append(" ");
        sb.append(lsid).append(": ").append(res.exception.toString());

        _job.error(sb.toString(), res.exception);
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

    abstract public String getRunName(FlowRun srcRun);
}
