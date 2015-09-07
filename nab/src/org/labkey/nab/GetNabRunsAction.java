/*
 * Copyright (c) 2008-2015 LabKey Corporation
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
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.ApiVersion;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.ShowRows;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.QueryParam;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.view.DataView;
import org.labkey.api.view.NotFoundException;
import org.springframework.validation.BindException;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
 * User: brittp
 * Date: Dec 9, 2008
 * Time: 5:13:30 PM
 */

@RequiresPermission(ReadPermission.class)
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

    private List<ExpRun> getRuns(ExpProtocol protocol, AssayProvider provider, GetNabRunsForm form, BindException errors)
    {
        User user = form.getViewContext().getUser();
        Container container = form.getViewContext().getContainer();
        AssayProtocolSchema assaySchema = provider.createProtocolSchema(user, container, protocol, null);

        QuerySettings settings = assaySchema.getSettings(form.getViewContext(), QueryView.DATAREGIONNAME_DEFAULT, AssayProtocolSchema.RUNS_TABLE_NAME);
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
        List<Integer> rowIds = new ArrayList<>();
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
        List<ExpRun> runs = new ArrayList<>();
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
            throw new IllegalArgumentException("Assay name is a required parameter.");
        final Map<String, Object> response = new HashMap<>();
        List<NabRunPropertyMap> runList = new ArrayList<>();
        response.put("runs", runList);
        Container container = form.getViewContext().getContainer();
        ExpProtocol protocol = null;
        for (Iterator<ExpProtocol> it = AssayService.get().getAssayProtocols(container).iterator(); it.hasNext() && protocol == null;)
        {
            ExpProtocol possibleProtocol = it.next();
            if (form.getAssayName().equals(possibleProtocol.getName()))
                protocol = possibleProtocol;
        }
        if (protocol == null)
            throw new NotFoundException("Assay " + form.getAssayName() + " was not found in current folder or project folder.");
        AssayProvider provider = AssayService.get().getProvider(protocol);
        if (!(provider instanceof NabAssayProvider))
            throw new IllegalArgumentException("Assay " + form.getAssayName() + " is not a NAb assay: it is of type " + provider.getName());

        response.put("assayName", protocol.getName());
        response.put("assayDescription", protocol.getDescription());
        response.put("assayId", protocol.getRowId());
        DilutionDataHandler dataHandler = ((NabAssayProvider) provider).getDataHandler();
        for (ExpRun run : getRuns(protocol, provider, form, errors))
        {
            runList.add(new NabRunPropertyMap(dataHandler.getAssayResults(run, form.getViewContext().getUser()),
                    form.isIncludeStats(), form.isIncludeWells(), form.isCalculateNeut(), form.isIncludeFitParameters()));

        }
        return new ApiSimpleResponse(response);
    }
}
