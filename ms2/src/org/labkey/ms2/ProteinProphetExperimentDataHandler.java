package org.labkey.ms2;

import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.util.URLHelper;
import org.labkey.api.data.Container;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.ms2.pipeline.MS2PipelineManager;

import javax.servlet.http.HttpServletRequest;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Feb 22, 2006
 */
public class ProteinProphetExperimentDataHandler extends AbstractExperimentDataHandler
{
    public void importFile(ExpData data, File dataFile, PipelineJob job, XarContext context) throws ExperimentException
    {
        try
        {
            ProteinProphetImporter importer = new ProteinProphetImporter(dataFile, data.getRun().getLSID(), context);
            importer.importFile(job);
        }
        catch (SQLException e)
        {
            throw new ExperimentException(e);
        }
        catch (XMLStreamException e)
        {
            throw new ExperimentException(e);
        }
        catch (IOException e)
        {
            throw new ExperimentException(e);
        }
    }

    public URLHelper getContentURL(HttpServletRequest request, Container container, ExpData data) throws ExperimentException
    {
        File dataFile = data.getDataFile();
        MS2Run run = null;
        try
        {
            ProteinProphetFile ppFile = MS2Manager.getProteinProphetFile(dataFile, container);
            if (ppFile != null)
            {
                run = MS2Manager.getRun(ppFile.getRun());
            }
        }
        catch (SQLException e)
        {
            throw new ExperimentException(e);
        }
        catch (IOException e)
        {
            throw new ExperimentException(e);
        }
        if (run == null)
        {
            return null;
        }
        ViewURLHelper result = new ViewURLHelper(request, "MS2", "showRun", container.getPath());
        result.addParameter("run", Integer.toString(run.getRun()));
        result.addParameter("expanded", "1");
        result.addParameter("grouping", "proteinprophet");
        return result;
    }

    public void deleteData(Data data, Container container) throws ExperimentException
    {
        // For now, let the PepXML file control when the data is deleted
    }

    public Priority getPriority(File f)
    {
        if (MS2PipelineManager.isProtXMLFile(f))
        {
            return Priority.HIGH;
        }
        return null;
    }

}
