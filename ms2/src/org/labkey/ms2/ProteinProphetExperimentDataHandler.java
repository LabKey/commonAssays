/*
 * Copyright (c) 2006-2012 LabKey Corporation
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

package org.labkey.ms2;

import org.labkey.api.exp.*;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.security.User;
import org.labkey.ms2.pipeline.TPPTask;
import org.apache.log4j.Logger;

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
    public DataType getDataType()
    {
        return null;
    }

    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        try
        {
            ExpRun run = data.getRun();
            if (run != null)
            {
                ProteinProphetImporter importer = new ProteinProphetImporter(dataFile, run.getLSID(), context);
                importer.importFile(info, log);
            }
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

    public ActionURL getContentURL(Container container, ExpData data)
    {
        File dataFile = data.getFile();
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
            throw new RuntimeSQLException(e);
        }
        
        if (run == null)
        {
            return null;
        }
        ActionURL result = MS2Controller.MS2UrlsImpl.get().getShowRunUrl(null, run);
        result.addParameter("expanded", "1");
        result.addParameter("grouping", "proteinprophet");
        return result;
    }

    public void deleteData(ExpData data, Container container, User user)
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
        if (f != null && TPPTask.isProtXMLFile(f))
        {
            return Priority.HIGH;
        }
        return null;
    }

}
