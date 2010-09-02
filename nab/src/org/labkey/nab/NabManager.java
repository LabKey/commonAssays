/*
 * Copyright (c) 2006-2010 LabKey Corporation
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

package org.labkey.nab;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;
import org.apache.log4j.Logger;
import org.labkey.api.attachments.AttachmentFile;
import org.labkey.api.attachments.AttachmentService;
import org.labkey.api.data.*;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.OntologyObject;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.User;
import org.labkey.api.study.*;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * User: brittp
 * Date: Oct 26, 2006
 * Time: 4:13:59 PM
 */
public class NabManager extends AbstractNabManager
{
    private static Logger _log = Logger.getLogger(NabManager.class);
    private static NabManager _instance;

    public static NabManager get()
    {
        if (_instance == null)
            _instance = new NabManager();
        return _instance;
    }
    
    public void deleteContainerData(Container container) throws SQLException
    {
        PlateService.get().deleteAllPlateData(container);
    }

    public ExpRun getNAbRunByObjectId(int objectId)
    {
        OntologyObject dataRow = OntologyManager.getOntologyObject(objectId);
        if (dataRow != null)
        {
            OntologyObject dataRowParent = OntologyManager.getOntologyObject(dataRow.getOwnerObjectId().intValue());
            if (dataRowParent != null)
            {
                ExpData data = ExperimentService.get().getExpData(dataRowParent.getObjectURI());
                if (data != null)
                    return data.getRun();
            }
        }
        return null;
    }

    public Collection<Integer> getReadableStudyObjectIds(Container studyContainer, User user, int[] objectIds)
    {
        if (objectIds == null || objectIds.length == 0)
            throw new IllegalArgumentException("getReadableStudyObjectIds must be passed a non-empty list of object ids.");

        Study study = StudyService.get().getStudy(studyContainer);
        if (study == null)
            throw new IllegalArgumentException("getReadableStudyObjectIds must be passed a valid study folder.");

        List<? extends DataSet> dataSets = study.getDataSets();
        if (dataSets == null || dataSets.isEmpty())
            return Collections.emptySet();

        // Gather a list of readable study dataset TableInfos associated with NAb protocols (these are created when NAb data
        // is copied to a study).  We use an ArrayList, rather than a set or other dup-removing structure, because there
        // can only be one dataset/tableinfo per protocol.
        List<TableInfo> dataTables = new ArrayList<TableInfo>();
        for (DataSet dataset : dataSets)
        {
            if (dataset.getProtocolId() != null && dataset.canRead(user))
            {
                ExpProtocol protocol = ExperimentService.get().getExpProtocol(dataset.getProtocolId().intValue());
                if (protocol != null && AssayService.get().getProvider(protocol) instanceof NabAssayProvider)
                    dataTables.add(dataset.getTableInfo(user));
            }
        }

        Collection<Integer> allObjectIds = new HashSet<Integer>();
        for (int objectId : objectIds)
            allObjectIds.add(objectId);
        SimpleFilter filter = new SimpleFilter(new SimpleFilter.InClause("ObjectId", allObjectIds));

        Collection<Integer> readableObjectIds = new HashSet<Integer>();

        // For each readable study data table, find any NAb runs that match the requested objectIds, and add them to the run list:
        for (TableInfo dataTable : dataTables)
        {
            ResultSet rs = null;
            try
            {
                rs = Table.select(dataTable, Collections.singleton("ObjectId"), filter, null);

                while (rs.next())
                {
                    int objectId = rs.getInt("ObjectId");
                    readableObjectIds.add(objectId);
                }
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
            finally
            {
                if (rs != null)
                    try { rs.close(); } catch (SQLException e) { }
            }
        }
        return readableObjectIds;
    }
}
