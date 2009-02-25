/*
 * Copyright (c) 2008-2009 LabKey Corporation
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
import org.labkey.api.action.HasViewContext;
import org.labkey.api.security.ActionNames;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.ACL;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ActionURL;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.*;
import org.labkey.api.data.*;
import org.labkey.api.query.*;
import org.springframework.validation.BindException;

import javax.servlet.ServletException;
import java.util.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.IOException;/*
 * User: brittp
 * Date: Dec 9, 2008
 * Time: 5:13:30 PM
 */

@ActionNames("getNabRuns, getNAbRuns")
@RequiresPermission(ACL.PERM_READ)
@ApiVersion(9.1)
public class GetNabRunsAction extends ApiAction<GetNabRunsAction.GetNabRunsForm>
{

    
    public static class GetNabRunsForm implements HasViewContext
    {
        private ViewContext _viewContext;
        private String _assayName;
        private boolean _includeStats = true;
        private boolean _includeWells = true;
        private boolean _calculateNeut = true;
        private Integer _start;
        private Integer _limit;
        private String _sort;
        private String _dir;

        public ViewContext getViewContext()
        {
            return _viewContext;
        }

        public void setViewContext(ViewContext viewContext)
        {
            _viewContext = viewContext;
        }

        public String getAssayName()
        {
            return _assayName;
        }

        public void setAssayName(String assayName)
        {
            _assayName = assayName;
        }

        public boolean isIncludeStats()
        {
            return _includeStats;
        }

        public void setIncludeStats(boolean includeStats)
        {
            _includeStats = includeStats;
        }

        public boolean isIncludeWells()
        {
            return _includeWells;
        }

        public void setIncludeWells(boolean includeWells)
        {
            _includeWells = includeWells;
        }

        public boolean isCalculateNeut()
        {
            return _calculateNeut;
        }

        public void setCalculateNeut(boolean calculateNeut)
        {
            _calculateNeut = calculateNeut;
        }

        public Integer getStart()
        {
            return _start;
        }

        public void setStart(Integer start)
        {
            _start = start;
        }

        public Integer getLimit()
        {
            return _limit;
        }

        public void setLimit(Integer limit)
        {
            _limit = limit;
        }

        public String getSort()
        {
            return _sort;
        }

        public void setSort(String sort)
        {
            _sort = sort;
        }

        public String getDir()
        {
            return _dir;
        }

        public void setDir(String dir)
        {
            _dir = dir;
        }
    }

    private class PropertyNameMap extends HashMap<String, Object>
    {
        public PropertyNameMap(Map<PropertyDescriptor, Object> properties)
        {
            for (Map.Entry<PropertyDescriptor, Object> entry : properties.entrySet())
                put(entry.getKey().getName(), entry.getValue());
        }
    }

    private void addStandardWellProperties(WellGroup group, Map<String, Object> properties, boolean includeStats, boolean includeWells)
    {
        if (includeStats)
        {
            properties.put("min", group.getMin());
            properties.put("max", group.getMax());
            properties.put("mean", group.getMean());
            properties.put("stddev", group.getStdDev());
        }
        if (includeWells)
        {
            List<Map<String, Object>> wellList = new ArrayList<Map<String, Object>>();
            for (Position position : group.getPositions())
            {
                Map<String, Object> wellProps = new HashMap<String, Object>();
                Well well = group.getPlate().getWell(position.getRow(), position.getColumn());
                wellProps.put("row", well.getRow());
                wellProps.put("column", well.getColumn());
                wellProps.put("value", well.getValue());
                wellList.add(wellProps);
            }
            properties.put("wells", wellList);
        }
    }

    private class NabRun extends HashMap<String, Object>
    {
        public NabRun(NabAssayRun assay, boolean includeStats, boolean includeWells, boolean calculateNeut)
        {
            put("runId", assay.getRun().getRowId());
            put("properties", new PropertyNameMap(assay.getRunProperties()));
            put("containerPath", assay.getRun().getContainer().getPath());
            put("containerId", assay.getRun().getContainer().getId());
            put("cutoffs", assay.getCutoffs());
            List<Map<String, Object>> samples = new ArrayList<Map<String, Object>>();
            for (NabAssayRun.SampleResult result : assay.getSampleResults())
            {
                Map<String, Object> sample = new HashMap<String, Object>();
                sample.put("properties", new PropertyNameMap(result.getProperties()));
                DilutionSummary dilutionSummary = result.getDilutionSummary();
                sample.put("wellgroupName", dilutionSummary.getWellGroup().getName());
                try
                {
                    if (includeStats)
                    {
                        sample.put("minDilution", dilutionSummary.getMinDilution());
                        sample.put("maxDilution", dilutionSummary.getMaxDilution());
                    }
                    if (calculateNeut)
                    {
                        sample.put("fitError", dilutionSummary.getFitError());
                        for (int cutoff : assay.getCutoffs())
                        {
                            sample.put("curveIC" + cutoff, dilutionSummary.getCutoffDilution(cutoff/100.0));
                            sample.put("pointIC" + cutoff, dilutionSummary.getInterpolatedCutoffDilution(cutoff/100.0));
                        }
                    }
                    List<Map<String, Object>> replicates = new ArrayList<Map<String, Object>>();
                    for (WellGroup replicate : dilutionSummary.getWellGroup().getOverlappingGroups(WellGroup.Type.REPLICATE))
                    {
                        Map<String, Object> replicateProps = new HashMap<String, Object>();
                        replicateProps.put("dilution", replicate.getDilution());
                        if (calculateNeut)
                        {
                            replicateProps.put("neutPercent", dilutionSummary.getPercent(replicate));
                            replicateProps.put("neutPlusMinus", dilutionSummary.getPlusMinus(replicate));
                        }
                        addStandardWellProperties(replicate, replicateProps, includeStats, includeWells);
                        replicates.add(replicateProps);
                    }
                    sample.put("replicates", replicates);
                }
                catch (DilutionCurve.FitFailedException e)
                {
                    throw new RuntimeException(e);
                }

                samples.add(sample);
            }
            put("samples", samples);

            WellGroup cellControl = assay.getPlate().getWellGroup(WellGroup.Type.CONTROL, NabManager.CELL_CONTROL_SAMPLE);
            Map<String, Object> cellControlProperties = new HashMap<String, Object>();
            addStandardWellProperties(cellControl, cellControlProperties, includeStats, includeWells);
            put("cellControl", cellControlProperties);

            WellGroup virusControl = assay.getPlate().getWellGroup(WellGroup.Type.CONTROL, NabManager.VIRUS_CONTROL_SAMPLE);
            Map<String, Object> virusControlProperties = new HashMap<String, Object>();
            addStandardWellProperties(virusControl, virusControlProperties, includeStats, includeWells);
            put("virusControl", virusControlProperties);
        }
    }

    private List<ExpRun> getRuns(String tableName, GetNabRunsForm form)
    {
        QuerySettings settings = new QuerySettings(form.getViewContext(), QueryView.DATAREGIONNAME_DEFAULT);
        //show all rows by default
        if(null == form.getLimit())
            settings.setShowRows(ShowRows.ALL);
        else
            settings.setMaxRows(form.getLimit().intValue());

        if (form.getStart() != null)
            settings.setOffset(form.getStart().intValue());

        if (form.getSort() != null)
        {
            ActionURL sortFilterURL = getViewContext().getActionURL().clone();
            boolean desc = "DESC".equals(form.getDir());
            sortFilterURL.replaceParameter("query.sort", (desc ? "-" : "") + form.getSort());
            settings.setSortFilterURL(sortFilterURL); //this isn't working!
        }


        settings.setQueryName(tableName);

        UserSchema assaySchema = QueryService.get().getUserSchema(form.getViewContext().getUser(),
                form.getViewContext().getContainer(), AssayService.ASSAY_SCHEMA_NAME);

        QueryView view;
        try
        {
            view = QueryView.create(getViewContext(), assaySchema, settings);
        }
        catch (ServletException e)
        {
            throw new RuntimeException(e);
        }
        ResultSet rs = null;
        List<Integer> rowIds = new ArrayList<Integer>();
        try
        {
            rs = view.getResultset();
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
        List<NabRun> runList = new ArrayList<NabRun>();
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
        for (ExpRun run : getRuns(tableName, form))
        {
            runList.add(new NabRun(NabDataHandler.getAssayResults(run, form.getViewContext().getUser()),
                    form.isIncludeStats(), form.isIncludeWells(), form.isCalculateNeut()));

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