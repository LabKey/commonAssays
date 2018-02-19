/*
 * Copyright (c) 2006-2014 LabKey Corporation
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

package org.labkey.flow.persist;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URIUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.controllers.executescript.AnalysisScriptController;
import org.labkey.flow.controllers.run.RunController;
import org.labkey.flow.data.FlowDataObject;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.flowdata.xml.FlowData;
import org.labkey.flow.flowdata.xml.FlowdataDocument;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

public class FlowDataHandler extends AbstractExperimentDataHandler
{
    static public final String EXT_DATA = "flowdata.xml";
    static public final String EXT_SCRIPT = "flowscript.xml";

    static public final FlowDataHandler instance = new FlowDataHandler();

    @Override
    public DataType getDataType()
    {
        return null;
    }

    public void beforeDeleteData(List<ExpData> datas) throws ExperimentException
    {
        FlowManager.get().deleteData(datas);
    }

    public void exportFile(ExpData data, File dataFile, User user, OutputStream out) throws ExperimentException
    {
        try
        {
            FlowDataObject obj = FlowDataObject.fromData(data);
            if (obj != null)
            {
                // XXX: doesn't include graph bytes.
                AttributeSet attrs = AttributeSetHelper.fromData(data);
                PipelineService service = PipelineService.get();

                attrs.relativizeURI(service.findPipelineRoot(data.getContainer()).getUri());
                // XXX: perhaps save to more than one file for graphs?
                attrs.save(out);
            }
        }
        catch (Exception e)
        {
            throw UnexpectedException.wrap(e);
        }
    }

    public ActionURL getContentURL(ExpData data)
    {
        if (data == null)
            return null;

        String url = data.getDataFileUrl();
        if (url == null)
            return null;

        // NOTE: data.getRunId() may be null if the flow run was deleted but the exp.data was not.
        if (url.endsWith(EXT_DATA) && data.getRunId() != null)
            return new ActionURL(RunController.ShowRunAction.class, data.getContainer()).addParameter("runId", data.getRunId());
        if (url.endsWith((EXT_SCRIPT)))
            return new ActionURL(AnalysisScriptController.BeginAction.class, data.getContainer()).addParameter("scriptId", data.getRowId());
        
        return null;
        //http://localhost:8080/labkey/flow-run/DRT/Flow%20Verify%20Project/FlowTest/showRun.view?runId=15
    }

    public Priority getPriority(ExpData data)
    {
        String url = data.getDataFileUrl();
        if (url != null && (url.endsWith("." + EXT_DATA) || url.endsWith("." + EXT_SCRIPT)))
            return Priority.HIGH;
        return null;
    }

    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        try
        {
            if (dataFile.getName().endsWith("." + EXT_DATA))
            {
                if (AttributeSetHelper.fromData(data) == null)
                {
                    FlowdataDocument doc = FlowdataDocument.Factory.parse(dataFile);
                    FlowData flowdata = doc.getFlowdata();
                    URI uriFile = null;
                    if (flowdata.getUri() != null)
                    {
                        uriFile = new URI(flowdata.getUri());
                        if (!uriFile.isAbsolute())
                        {
                            URI uriPipelineRoot = PipelineService.get().findPipelineRoot(info.getContainer()).getUri();
                            uriFile = URIUtil.resolve(uriPipelineRoot, uriPipelineRoot, flowdata.getUri());
                        }
                    }
                    AttributeSet attrSet = new AttributeSet(doc.getFlowdata(), uriFile);
                    AttributeSetHelper.save(attrSet, info.getUser(), data);
                }
            }
            else if (dataFile.getName().endsWith("." + EXT_SCRIPT))
            {
                FlowScript script = new FlowScript(data);
                script.setAnalysisScript(info.getUser(), PageFlowUtil.getFileContentsAsString(dataFile));
            }
        }
        catch (Exception e)
        {
            throw new ExperimentException("Error loading file", e);
        }
    }

    public void deleteData(ExpData data, Container container, User user)
    {
        data.delete(user);
    }
}
