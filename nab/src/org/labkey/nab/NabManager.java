/*
 * Copyright (c) 2006-2013 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.OntologyObject;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.User;
import org.labkey.api.study.DataSet;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.Study;
import org.labkey.api.study.StudyService;
import org.labkey.api.study.assay.AssayService;

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
    private static final Logger _log = Logger.getLogger(NabManager.class);
    private static final NabManager _instance = new NabManager();

    private NabManager()
    {
    }

    public static NabManager get()
    {
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

    public Map<Integer, ExpProtocol> getReadableStudyObjectIds(Container studyContainer, User user, int[] objectIds)
    {
        if (objectIds == null || objectIds.length == 0)
            throw new IllegalArgumentException("getReadableStudyObjectIds must be passed a non-empty list of object ids.");

        Study study = StudyService.get().getStudy(studyContainer);
        if (study == null)
            throw new IllegalArgumentException("getReadableStudyObjectIds must be passed a valid study folder.");

        List<? extends DataSet> dataSets = study.getDataSets();
        if (dataSets == null || dataSets.isEmpty())
            return Collections.emptyMap();

        // Gather a list of readable study dataset TableInfos associated with NAb protocols (these are created when NAb data
        // is copied to a study).  We use an ArrayList, rather than a set or other dup-removing structure, because there
        // can only be one dataset/tableinfo per protocol.
        Map<TableInfo, ExpProtocol> dataTables = new HashMap<TableInfo, ExpProtocol>();
        for (DataSet dataset : dataSets)
        {
            if (dataset.isAssayData() && dataset.canRead(user))
            {
                ExpProtocol protocol = dataset.getAssayProtocol();
                if (protocol != null && AssayService.get().getProvider(protocol) instanceof NabAssayProvider)
                    dataTables.put(dataset.getTableInfo(user), protocol);
            }
        }

        Collection<Integer> allObjectIds = new HashSet<Integer>();
        for (int objectId : objectIds)
            allObjectIds.add(objectId);
        SimpleFilter filter = new SimpleFilter(new SimpleFilter.InClause("ObjectId", allObjectIds));

        Map<Integer, ExpProtocol> readableObjectIds = new HashMap<Integer, ExpProtocol>();

        // For each readable study data table, find any NAb runs that match the requested objectIds, and add them to the run list:
        for (Map.Entry<TableInfo, ExpProtocol> entry : dataTables.entrySet())
        {
            TableInfo dataTable = entry.getKey();
            ExpProtocol protocol = entry.getValue();
            ResultSet rs = null;
            try
            {
                rs = Table.select(dataTable, Collections.singleton("ObjectId"), filter, null);

                while (rs.next())
                {
                    int objectId = rs.getInt("ObjectId");
                    readableObjectIds.put(objectId, protocol);
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
