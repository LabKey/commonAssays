package org.labkey.ms1;

import org.labkey.api.exp.*;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.util.URLHelper;
import org.labkey.api.data.Container;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.security.User;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

/**
 * This data handler doesn't do much at this point, but provides
 * a place to put code to actually load the values out of a .tsv
 * file into the database, if desired.
 * User: jeckels
 * Date: Nov 4, 2006
 */
public class MSInspectFeaturesDataHandler extends AbstractExperimentDataHandler
{
    public void importFile(ExpData data, File dataFile, PipelineJob job, XarContext context) throws ExperimentException
    {
        // Implement loading the contents of the file into the database here
    }

    public URLHelper getContentURL(HttpServletRequest request, Container container, ExpData data) throws ExperimentException
    {
        ViewURLHelper url = new ViewURLHelper(request, "ms1", "showFeaturesFile.view", container);
        url.addParameter("dataRowId", Integer.toString(data.getRowId()));
        return url;
    }

    public void deleteData(Data data, Container container) throws ExperimentException
    {
        // Delete the database records for this features file here
    }

    public void runMoved(Data newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user) throws ExperimentException
    {

    }

    public Priority getPriority(File f)
    {
        if (f.getName().toLowerCase().endsWith(".tsv"))
        {
            return Priority.HIGH;
        }
        return null;
    }
}
