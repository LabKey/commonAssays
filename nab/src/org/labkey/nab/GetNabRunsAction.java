/*
 * Copyright (c) 2008-2011 LabKey Corporation
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
import org.labkey.api.security.ActionNames;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.*;
import org.labkey.api.view.DataView;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.data.*;
import org.labkey.api.query.*;
import org.springframework.validation.BindException;

import javax.servlet.ServletException;
import java.util.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.IOException;

/*
 * User: brittp
 * Date: Dec 9, 2008
 * Time: 5:13:30 PM
 */

@ActionNames("getNabRuns, getNAbRuns")
@RequiresPermissionClass(ReadPermission.class)
@ApiVersion(9.1)
public class GetNabRunsAction extends ApiAction<GetNabRunsAction.GetNabRunsForm>
{
    public static class GetNabRunsForm extends GetNabRunsBaseForm
    {
        private String _assayName;
        private Integer _offset;
        private Integer _maxRows;
        private String _sort;
        private String _containerFilter;

        public String getAssayName()
        {
            return _assayName;
        }

        public void setAssayName(String assayName)
        {
            _assayName = assayName;
        }

        public Integer getOffset()
        {
            return _offset;
        }

        public void setOffset(Integer offset)
        {
            _offset = offset;
        }

        public Integer getMaxRows()
        {
            return _maxRows;
        }

        public void setMaxRows(Integer maxRows)
        {
            _maxRows = maxRows;
        }

        public String getSort()
        {
            return _sort;
        }

        public void setSort(String sort)
        {
            _sort = sort;
        }

        public String getContainerFilter()
        {
            return _containerFilter;
        }

        public void setContainerFilter(String containerFilter)
        {
            _containerFilter = containerFilter;
        }
    }

    private List<ExpRun> getRuns(String tableName, GetNabRunsForm form, BindException errors)
    {
        UserSchema assaySchema = QueryService.get().getUserSchema(form.getViewContext().getUser(),
                form.getViewContext().getContainer(), AssaySchema.NAME);

        QuerySettings settings = assaySchema.getSettings(form.getViewContext(), QueryView.DATAREGIONNAME_DEFAULT, tableName);
        //show all rows by default
       if (null == form.getMaxRows()
            && null == getViewContext().getRequest().getParameter(QueryView.DATAREGIONNAME_DEFAULT + "." + QueryParam.maxRows))
        {
            settings.setShowRows(ShowRows.ALL);
        }
        else if (form.getMaxRows() != null)
        {
            settings.setMaxRows(form.getMaxRows().intValue());
        }

        if (form.getOffset() != null)
            settings.setOffset(form.getOffset().intValue());

        // handle both sorts and filters:
        settings.setSortFilterURL(getViewContext().getActionURL());

        if (form.getContainerFilter() != null)
        {
            // If the user specified an incorrect filter, throw an IllegalArgumentException
            ContainerFilter.Type containerFilterType =
                ContainerFilter.Type.valueOf(form.getContainerFilter());
            settings.setContainerFilterName(containerFilterType.name());
        }

        QueryView queryView = QueryView.create(getViewContext(), assaySchema, settings, errors);
        DataView dataView = queryView.createDataView();
        ResultSet rs = null;
        List<Integer> rowIds = new ArrayList<Integer>();
        try
        {
            rs = dataView.getDataRegion().getResultSet(dataView.getRenderContext());
            while (rs.next())
                rowIds.add(rs.getInt("RowId"));
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (rs != null)
                try { rs.close(); } catch (SQLException e) {}
        }
        List<ExpRun> runs = new ArrayList<ExpRun>();
        for (Integer rowId : rowIds)
        {
            ExpRun run = ExperimentService.get().getExpRun(rowId.intValue());
            if (run != null)
                runs.add(run);
        }
        return runs;
    }

    public ApiResponse execute(GetNabRunsForm form, BindException errors) throws Exception
    {
        if (form.getAssayName() == null)
            throw new RuntimeException("Assay name is a required parameter.");
        final Map<String, Object> _properties = new HashMap<String, Object>();
        List<NabRunPropertyMap> runList = new ArrayList<NabRunPropertyMap>();
        _properties.put("runs", runList);
        Container container = form.getViewContext().getContainer();
        ExpProtocol protocol = null;
        for (Iterator<ExpProtocol> it = AssayService.get().getAssayProtocols(container).iterator(); it.hasNext() && protocol == null;)
        {
            ExpProtocol possibleProtocol = it.next();
            if (form.getAssayName().equals(possibleProtocol.getName()))
                protocol = possibleProtocol;
        }
        if (protocol == null)
            throw new RuntimeException("Assay " + form.getAssayName() + " was not found in current folder or project folder.");
        AssayProvider provider = AssayService.get().getProvider(protocol);
        if (!(provider instanceof NabAssayProvider))
            throw new RuntimeException("Assay " + form.getAssayName() + " is not a NAb assay: it is of type " + provider.getName());

        String tableName = AssayService.get().getRunsTableName(protocol);

        _properties.put("assayName", protocol.getName());
        _properties.put("assayDescription", protocol.getDescription());
        _properties.put("assayId", protocol.getRowId());
        NabDataHandler dataHandler = ((NabAssayProvider) provider).getDataHandler();
        for (ExpRun run : getRuns(tableName, form, errors))
        {
            runList.add(new NabRunPropertyMap(dataHandler.getAssayResults(run, form.getViewContext().getUser()),
                    form.isIncludeStats(), form.isIncludeWells(), form.isCalculateNeut(), form.isIncludeFitParameters()));

        }
        return new ApiResponse()
        {
            public Map<String, Object> getProperties()
            {
                return _properties;
            }
        };
    }
}
