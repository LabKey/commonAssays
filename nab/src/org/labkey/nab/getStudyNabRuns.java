/*
 * Copyright (c) 2008-2010 LabKey Corporation
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

import org.labkey.api.action.ApiAction;
import org.labkey.api.action.ApiResponse;
import org.labkey.api.action.ApiVersion;
import org.labkey.api.data.*;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.OntologyObject;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.ActionNames;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.study.DataSet;
import org.labkey.api.study.Study;
import org.labkey.api.study.StudyService;
import org.labkey.api.study.assay.AssayService;
import org.springframework.validation.BindException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/*
 * User: brittp
 * Date: March 4, 2010
 * Time: 5:13:30 PM
 */

@ActionNames("getStudyNabRuns, getStudyNAbRuns")
@RequiresPermissionClass(ReadPermission.class)
@ApiVersion(10.1)
public class getStudyNabRuns extends ApiAction<getStudyNabRuns.GetStudyNabRunsForm>
{
    public static class GetStudyNabRunsForm extends GetNabRunsBaseForm
    {
        private int[] _objectIds;

        public int[] getObjectIds()
        {
            return _objectIds;
        }

        public void setObjectIds(int[] objectIds)
        {
            _objectIds = objectIds;
        }
    }

    @Override
    public ApiResponse execute(GetStudyNabRunsForm form, BindException errors) throws Exception
    {
        final Map<String, Object> properties = new HashMap<String, Object>();
        List<NabRunPropertyMap> runList = new ArrayList<NabRunPropertyMap>();
        properties.put("runs", runList);

        for (ExpRun run : getRuns(form, errors))
        {
            runList.add(new NabRunPropertyMap(NabDataHandler.getAssayResults(run, form.getViewContext().getUser()),
                    form.isIncludeStats(), form.isIncludeWells(), form.isCalculateNeut(), form.isIncludeFitParameters()));

        }

        if (errors.hasErrors())
            return null;
        else
        {
            return new ApiResponse()
            {
                public Map<String, Object> getProperties()
                {
                    return properties;
                }
            };
        }
    }

    protected Collection<ExpRun> getRuns(GetStudyNabRunsForm form, BindException errors)
    {
        if (form.getObjectIds() == null || form.getObjectIds().length == 0)
        {
            errors.reject(null, "ObjectIds parameter is required.");
            return Collections.emptyList();
        }

        Study study = StudyService.get().getStudy(getViewContext().getContainer());
        if (study == null)
        {
            errors.reject(null, "getNabRunsByObjectId must be called in a study folder.");
            return Collections.emptySet();
        }

        DataSet[] dataSets = study.getDataSets();
        if (dataSets == null || dataSets.length == 0)
            return Collections.emptySet();

        // Gather a list of readable study dataset TableInfos associated with NAb protocols (these are created when NAb data
        // is copied to a study).  We use an ArrayList, rather than a set or other dup-removing structure, because there
        // can only be one dataset/tableinfo per protocol.
        List<TableInfo> dataTables = new ArrayList<TableInfo>();
        for (DataSet dataset : dataSets)
        {
            if (dataset.getProtocolId() != null && dataset.canRead(getViewContext().getUser()))
            {
                ExpProtocol protocol = ExperimentService.get().getExpProtocol(dataset.getProtocolId().intValue());
                if (protocol != null && AssayService.get().getProvider(protocol) instanceof NabAssayProvider)
                    dataTables.add(dataset.getTableInfo(getViewContext().getUser()));
            }
        }

        Collection<Integer> objectIds = new HashSet<Integer>();
        for (int objectId : form.getObjectIds())
            objectIds.add(objectId);
        SimpleFilter filter = new SimpleFilter(new SimpleFilter.InClause("ObjectId", objectIds));

        Collection<ExpRun> runs = new ArrayList<ExpRun>();

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
                    // Note that we intentionally do NOT filter or check container.  If the user has access to the NAb
                    // data copied to the study (verified above), they can get the raw data via the APIs.  This is
                    // consistent with the role-based implementation which allows viewing the NAb details view for copied-
                    // to-study data even if the Nab details view data is in a folder the user cannot read.
                    OntologyObject dataRow = OntologyManager.getOntologyObject(objectId);
                    if (dataRow != null)
                    {
                        OntologyObject dataRowParent = OntologyManager.getOntologyObject(dataRow.getOwnerObjectId().intValue());
                        if (dataRowParent != null)
                        {
                            ExpData data = ExperimentService.get().getExpData(dataRowParent.getObjectURI());
                            if (data != null)
                            {
                                ExpRun run = data.getRun();
                                if (run != null)
                                    runs.add(run);
                            }
                        }
                    }
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
        return runs;
    }
}