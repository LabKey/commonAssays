/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.data.Container;
import org.labkey.api.util.URLHelper;
import org.labkey.api.util.URIUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.User;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.flow.data.FlowDataObject;
import org.labkey.flow.data.FlowScript;
import org.labkey.flow.flowdata.xml.FlowdataDocument;
import org.labkey.flow.flowdata.xml.FlowData;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;
import java.net.URI;

public class FlowDataHandler extends AbstractExperimentDataHandler
{
    static public final String EXT_DATA = "flowdata.xml";
    static public final String EXT_SCRIPT = "flowscript.xml";
    static public final FlowDataHandler instance = new FlowDataHandler();
    public void beforeDeleteData(List<ExpData> datas) throws ExperimentException
    {
        try
        {
            FlowManager.get().deleteData(datas);
        }
        catch (SQLException e)
        {
            throw new ExperimentException("Exception", e);
        }
    }

    public void exportFile(ExpData data, File dataFile, OutputStream out) throws ExperimentException
    {
        try
        {
            FlowDataObject obj = FlowDataObject.fromData(data);
            if (obj != null)
            {
                AttributeSet attrs = AttributeSet.fromData(data);
                PipelineService service = PipelineService.get();

                attrs.relativizeURI(service.findPipelineRoot(data.getContainer()).getUri());
                attrs.save(out);
            }
        }
        catch (Exception e)
        {
            throw UnexpectedException.wrap(e);
        }
    }

    public URLHelper getContentURL(Container container, ExpData data)
    {
        return null;
    }

    public Priority getPriority(ExpData data)
    {
        File object = data.getFile();
        if (object != null && (object.getName().endsWith("." + EXT_DATA)|| object.getName().endsWith("." + EXT_SCRIPT)))
            return Priority.HIGH;
        return null;
    }

    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        try
        {
            if (dataFile.getName().endsWith("." + EXT_DATA))
            {
                FlowdataDocument doc = FlowdataDocument.Factory.parse(dataFile);
                FlowData flowdata = doc.getFlowdata();
                URI uriFile = null;
                if (flowdata.getUri() != null)
                {
                    uriFile = new URI(flowdata.getUri());
                    if (!uriFile.isAbsolute())
                    {
                        URI uriPipelineRoot = PipelineService.get().findPipelineRoot(info.getContainer()).getUri(info.getContainer());
                        uriFile = URIUtil.resolve(uriPipelineRoot, uriPipelineRoot, flowdata.getUri());
                    }
                }
                AttributeSet attrSet = new AttributeSet(doc.getFlowdata(), uriFile);
                attrSet.save(info.getUser(), data);
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

    public void deleteData(ExpData data, Container container, User user) throws ExperimentException
    {
    }

    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {

    }
}
