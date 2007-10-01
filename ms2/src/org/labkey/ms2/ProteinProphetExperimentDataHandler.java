package org.labkey.ms2;

import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.util.URLHelper;
import org.labkey.api.data.Container;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.security.User;
import org.labkey.ms2.pipeline.MS2PipelineManager;
import org.apache.log4j.Logger;

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
    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        try
        {
            ProteinProphetImporter importer = new ProteinProphetImporter(dataFile, data.getRun().getLSID(), context);
            importer.importFile(info, log);
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

    public void deleteData(ExpData data, Container container, User user) throws ExperimentException
    {
        // For now, let the PepXML file control when the data is deleted
    }

    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        // For now, let the PepXML file control when the data is moved
    }

    public Priority getPriority(ExpData data)
    {
        File f = data.getFile();
        if (f != null && MS2PipelineManager.isProtXMLFile(f))
        {
            return Priority.HIGH;
        }
        return null;
    }

}
