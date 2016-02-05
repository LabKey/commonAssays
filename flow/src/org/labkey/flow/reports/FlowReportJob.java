/*
 * Copyright (c) 2011-2016 LabKey Corporation
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
package org.labkey.flow.reports;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.query.ValidationException;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.reports.report.RReport;
import org.labkey.api.reports.report.RReportJob;
import org.labkey.api.reports.report.ReportDescriptor;
import org.labkey.api.reports.report.r.ParamReplacement;
import org.labkey.api.reports.report.r.view.TsvOutput;
import org.labkey.api.util.Tuple3;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ViewContext;
import org.labkey.flow.query.FlowTableType;
import org.labkey.flow.script.FlowPipelineProvider;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: kevink
 * Date: Jul 5, 2011
 */
public class FlowReportJob extends RReportJob
{
    FilterFlowReport _report;

    public FlowReportJob(FilterFlowReport report, ViewBackgroundInfo info, PipeRoot root)
            throws SQLException
    {
        super(FlowPipelineProvider.NAME, info, report.getReportId(), root);
        _report = report;
        init(this.getContainerId());
    }

    @Override
    protected RReport getReport()
    {
        if (_report == null)
            return null;

        try
        {
            return _report.getInnerReport();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Exception retrieving report", e);
        }
    }

    // UNDONE: be like the base RReport and write the input file before the report job runs
    @Override
    protected File inputFile(RReport report, @NotNull ViewContext context) throws Exception
    {
        File file = report.createInputDataFile(context);

        // debug logging
        debug("Executed query:");
        debug(_report._query);

        return file;
    }

    @Override
    protected void processOutputs(RReport report, List<ParamReplacement> outputSubst) throws Exception
    {
        super.processOutputs(report, outputSubst);

        List<Tuple3<TsvOutput, Domain, FlowTableType>> tuples = new ArrayList<>();

        // Create the domains in a separate transaction from saving the data.
        DbSchema schema = ExperimentService.get().getSchema();
        try (DbScope.Transaction transaction = schema.getScope().ensureTransaction())
        {
            for (ParamReplacement output : outputSubst)
            {
                if (TsvOutput.ID.equals(output.getId()))
                {
                    TsvOutput tsv = (TsvOutput)output;
                    FlowTableType tableType = tableTypeForOutput(tsv);
                    if (tableType == null)
                        continue;

                    Domain domain = ensureDomain(tableType);
                    if (domain != null)
                        tuples.add(Tuple3.of(tsv, domain, tableType));

                    if (getErrors() > 0)
                        return;
                }
            }

            transaction.commit();
        }

        if (getErrors() > 0)
            return;

        try (DbScope.Transaction transaction = schema.getScope().ensureTransaction())
        {
            deleteSavedResults();

            for (Tuple3<TsvOutput, Domain, FlowTableType> tuple : tuples)
            {
                saveTsvOutput(tuple.first, tuple.second, tuple.third);
                if (getErrors() > 0)
                    return;
            }

            transaction.commit();
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    private FlowTableType tableTypeForOutput(TsvOutput tsv)
    {
        try
        {
            return FlowTableType.valueOf(tsv.getName());
        }
        catch (IllegalArgumentException e)
        {
            error("Can't create FlowTableType for '" + tsv.getName());
        }

        return null;
    }

    private Domain ensureDomain(FlowTableType tableType)
    {
        Domain domain = _report.ensureDomain(getUser(), tableType);
        if (domain == null)
        {
            error("Failed to create domain");
            return null;
        }

        return domain;
    }

    private void saveTsvOutput(TsvOutput tsv, Domain domain, FlowTableType tableType) throws Exception
    {
        info("Importing tsv file '" + tsv + "' into domain " + domain.getName());
        for (File file : tsv.getFiles())
        {
            TabLoader loader = tsv.createTabLoader(file);
            mapTsvColumns(domain, loader);
            if (getErrors() > 0)
                return;

            save(domain, loader, tableType);
            info("Imported tsv file '" + tsv + "' into domain " + domain.getName());
        }
    }

    private void mapTsvColumns(Domain domain, TabLoader loader) throws IOException
    {
        ColumnDescriptor[] cols = loader.getColumns();

        boolean hasLSID = false;
        Map<String, DomainProperty> descriptorsByName = domain.createImportMap(true);
        for (ColumnDescriptor cd : cols)
        {
            if (cd.name == null || cd.name.length() == 0)
                continue;

            if (cd.name.equalsIgnoreCase("lsid"))
            {
                hasLSID = true;
                continue;
            }

            DomainProperty dp = descriptorsByName.get(cd.name);
            if (dp == null)
                continue;

            cd.name = dp.getPropertyURI();
            cd.clazz = dp.getPropertyDescriptor().getPropertyType().getJavaType();
        }

        if (!hasLSID)
            error("LSID column required to save tsv output file");
    }

    private void deleteSavedResults()
    {
        info("Deleting results from previous runs");
        // Delete any objects from a previous run, but don't delete the attached ExpData object
        //OntologyManager.deleteObjectsOfType(domain.getTypeURI(), getContainer());
        _report.deleteSavedResults(getContainer());
    }

    private void save(Domain domain, TabLoader loader, FlowTableType tableType) throws IOException, ValidationException, SQLException
    {
        OntologyManager.ImportHelper helper = new OntologyManager.ImportHelper()
        {
            @Override
            public String beforeImportObject(Map<String, Object> map) throws SQLException
            {
                Object o = map.get("lsid");
                if (o == null)
                    throw new IllegalStateException("Could not find LSID for row: " + map);

                String lsid = String.valueOf(o);

                // Combine the original exp.data LSID with the this report's id to
                // uniquely identify this report's property values.  Remember there is
                // a domain in the Shared project for each report TYPE (not instance)
                // and for each FlowTableType that the report emits tsv files for.
                // The exp.object on which we stash the report results is used for all
                // Domains of this report type.
                return FlowReportManager.getReportResultsLsid(_report, lsid);
            }

            @Override
            public void afterBatchInsert(int currentRow) throws SQLException { }

            @Override
            public void updateStatistics(int currentRow) throws SQLException { }
        };

        Integer ownerId = FlowReportManager.ensureReportOntologyObjectId(_report, getContainer());

        List<String> lsids = OntologyManager.insertTabDelimited(getContainer(), getUser(), ownerId, helper, domain, loader.load(), true);
    }

    @Override
    public String getDescription()
    {
        if (_report != null)
        {
            ReportDescriptor d = _report.getDescriptor();
            if (d != null)
                return d.getReportName();
        }

        return "Unknown Flow Report";
    }
}
