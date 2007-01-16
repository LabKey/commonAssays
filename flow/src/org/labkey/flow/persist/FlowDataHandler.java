package org.labkey.flow.persist;

import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.data.Container;
import org.labkey.api.util.URLHelper;
import org.labkey.api.util.URIUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.flow.data.FlowDataObject;
import org.labkey.flow.flowdata.xml.FlowdataDocument;
import org.labkey.flow.flowdata.xml.FlowData;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;
import java.net.URI;

public class FlowDataHandler extends AbstractExperimentDataHandler
{
    static public final String EXT_DATA = "flowdata.xml";
    static public final String EXT_SCRIPT = "flowscript.xml";
    public void beforeDeleteData(List<Data> datas) throws ExperimentException
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

                attrs.relativizeURI(service.getPipelineRoot(data.getContainer()));
                attrs.save(out);
            }
        }
        catch (Exception e)
        {
            throw UnexpectedException.wrap(e);
        }
    }

    public URLHelper getContentURL(HttpServletRequest request, Container container, ExpData data) throws ExperimentException
    {
        return null;
    }

    public Priority getPriority(File object)
    {
        if (object.getName().endsWith("." + EXT_DATA)|| object.getName().endsWith("." + EXT_SCRIPT))
            return Priority.HIGH;
        return null;
    }

    public void importFile(ExpData data, File dataFile, PipelineJob job, XarContext context) throws ExperimentException
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
                        URI uriPipelineRoot = PipelineService.get().getPipelineRoot(job.getContainer());
                        uriFile = URIUtil.resolve(uriPipelineRoot, uriPipelineRoot, flowdata.getUri());
                    }
                }
                AttributeSet attrSet = new AttributeSet(doc.getFlowdata(), uriFile);
                attrSet.save(job.getUser(), data);
            }
            else
            {

            }
        }
        catch (Exception e)
        {
            throw new ExperimentException("Error loading file", e);
        }
    }

    public void deleteData(Data data, Container container) throws ExperimentException
    {
    }
}
