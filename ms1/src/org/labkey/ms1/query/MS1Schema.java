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

package org.labkey.ms1.query;

import org.labkey.api.module.Module;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.data.*;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.*;
import org.labkey.api.security.User;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.api.view.ActionURL;
import org.labkey.ms1.MS1Controller;
import org.labkey.ms1.MS1Manager;

import java.util.*;

/**
 * Provides a customized experiment run grid with features specific to MS1 runs.
 */
public class MS1Schema extends UserSchema
{
    public static final String SCHEMA_NAME = "ms1";
    public static final String SCHEMA_DESCR = "Contains data about MS1 runs, including detected features, scans, and peaks";
    public static final String TABLE_FEATURE_RUNS = "MSInspectFeatureRuns";
    public static final String TABLE_FEATURES = "Features";
    public static final String TABLE_FEATURES_SEARCH = "FeaturesSearch";
    public static final String TABLE_PEAKS = "Peaks";
    public static final String TABLE_FILES = "Files";
    public static final String TABLE_SCANS = "Scans";
    public static final String TABLE_COMPARE_PEP = "ComparePeptide";

    static public void register(Module module)
    {
        DefaultSchema.registerProvider(SCHEMA_NAME, new DefaultSchema.SchemaProvider(module) {
            @Override
            public QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                return new MS1Schema(schema.getUser(), schema.getContainer());
            }
        });
    }

    private ExpSchema _expSchema;
    private boolean _restrictContainer = true;
    private final ContainerFilter _containerFilter;

    public MS1Schema(User user, Container container)
    {
        this(user, container, true);
    }

    public MS1Schema(User user, Container container, boolean restrictContainer)
    {
        super(SCHEMA_NAME, SCHEMA_DESCR, user, container, MS1Manager.get().getSchema());
        _restrictContainer = restrictContainer;

        _expSchema = new ExpSchema(user, container);

        if (_restrictContainer)
        {
            _containerFilter = ContainerFilter.CURRENT;
        }
        else
        {
            _containerFilter = new ContainerFilter.CurrentAndSubfolders(user);
            _expSchema.setContainerFilter(_containerFilter);
        }
    }

    public boolean isRestrictContainer()
    {
        return _restrictContainer;
    }

    public Set<String> getTableNames()
    {
        HashSet<String> ret = new HashSet<>();
        ret.add(TABLE_FEATURE_RUNS);
        ret.add(TABLE_FEATURES);
        ret.add(TABLE_FEATURES_SEARCH);
        ret.add(TABLE_FILES);
        ret.add(TABLE_PEAKS);
        ret.add(TABLE_SCANS);
        return ret;
    }

    public TableInfo createTable(String name)
    {
        if (TABLE_FEATURE_RUNS.equalsIgnoreCase(name))
            return getMS1ExpRunsTableInfo();
        else if(TABLE_FEATURES.equalsIgnoreCase(name))
            return getFeaturesTableInfo();
        else if(TABLE_FEATURES_SEARCH.equalsIgnoreCase(name))
            return getFeaturesTableInfoSearch();
        else if(TABLE_PEAKS.equalsIgnoreCase(name))
            return getPeaksTableInfo();
        else if(TABLE_FILES.equalsIgnoreCase(name))
            return getFilesTableInfo();
        else if(TABLE_SCANS.equalsIgnoreCase(name))
            return getScansTableInfo();
        else if(TABLE_COMPARE_PEP.equalsIgnoreCase(name))
            return getComparePeptideTableInfo(null);

        return null;
    } //getTable()

    public CrosstabTableInfo getComparePeptideTableInfo(int[] runIds)
    {
        FeaturesTableInfo tinfo = getFeaturesTableInfo(true, true);
        ArrayList<FeaturesFilter> filters = new ArrayList<>();
        //OK if runIds is null
        RunFilter runFilter = new RunFilter(runIds);
        filters.add(runFilter);
        //filter out features that don't have an associated peptide
        filters.add(new PeptideNotNullFilter());
        tinfo.setBaseFilters(filters);

        ActionURL urlPepSearch = new ActionURL(MS1Controller.PepSearchAction.class, getContainer());
        urlPepSearch.addParameter(ProteinService.PeptideSearchForm.ParamNames.exact.name(), "on");
        urlPepSearch.addParameter(ProteinService.PeptideSearchForm.ParamNames.runIds.name(), runFilter.getRunIdString());

        CrosstabSettings settings = new CrosstabSettings(tinfo);

        CrosstabDimension rowDim = settings.getRowAxis().addDimension(FieldKey.fromParts(FeaturesTableInfo.COLUMN_PEPTIDE_INFO, "Peptide"));
        rowDim.setUrl(urlPepSearch.getLocalURIString() + "&pepSeq=${" + AliasManager.makeLegalName(FeaturesTableInfo.COLUMN_PEPTIDE_INFO + "/Peptide", getDbSchema().getSqlDialect()) + "}");

        CrosstabDimension colDim = settings.getColumnAxis().addDimension(FieldKey.fromParts("FileId", "ExpDataFileId", "Run", "RowId"));
        colDim.setUrl(new ActionURL(MS1Controller.ShowFeaturesAction.class, getContainer()).getLocalURIString() + "runId=" + CrosstabMember.VALUE_TOKEN);

        //setup the feature id column as an FK to itself so that the first feature measure will allow
        //users to add other info from the features table.
        ColumnInfo featureIdCol = tinfo.getColumn("FeatureId");
        featureIdCol.setFk(new LookupForeignKey("FeatureId", "FeatureId")
        {
            public TableInfo getLookupTableInfo()
            {
                FeaturesTableInfo table = getFeaturesTableInfo(false, Boolean.TRUE);
                //set include deleted true so that we don't include the files table in the join
                //without it, we get a too many tables exception from SQL Server
                table.setIncludeDeleted(true);
                return table;
            }
        });

        settings.addMeasure(FieldKey.fromParts("FeatureId"), CrosstabMeasure.AggregateFunction.COUNT, "Num Features");
        settings.addMeasure(FieldKey.fromParts("Intensity"), CrosstabMeasure.AggregateFunction.AVG);
        CrosstabMeasure firstFeature = settings.addMeasure(FieldKey.fromParts("FeatureId"), CrosstabMeasure.AggregateFunction.MIN, "First Feature");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(MS1Controller.ShowFeaturesForm.ParamNames.runId.name(), CrosstabMember.VALUE_NAME);
        parameters.put(MS1Controller.ShowFeaturesForm.ParamNames.pepSeq.name(), AliasManager.makeLegalName(FeaturesTableInfo.COLUMN_PEPTIDE_INFO + "/Peptide", getDbSchema().getSqlDialect()));
        for(CrosstabMeasure measure : settings.getMeasures())
            measure.setUrl(new DetailsURL(new ActionURL(MS1Controller.ShowFeaturesAction.class, getContainer()), parameters));

        settings.setInstanceCountCaption("Num Runs");
        settings.getRowAxis().setCaption("Peptide Information");
        settings.getColumnAxis().setCaption("Runs");

        CrosstabTableInfo cti;
        if(null != runIds)
        {
            ArrayList<CrosstabMember> members = new ArrayList<>();
            //build up the list of column members
            for(int runId : runIds)
            {
                ExpRun run = ExperimentService.get().getExpRun(runId);
                members.add(new CrosstabMember(Integer.valueOf(runId), colDim , null == run ? null : run.getName()));
            }
            cti = new CrosstabTable(settings, members);
        }
        else
            cti = new CrosstabTable(settings);

        List<FieldKey> defaultCols = new ArrayList<>(cti.getDefaultVisibleColumns());   // Make a copy so we can modify
        defaultCols.remove(FieldKey.fromParts(firstFeature.getName()));
        defaultCols.add(FieldKey.fromParts(firstFeature.getName(), "Time"));
        defaultCols.add(FieldKey.fromParts(firstFeature.getName(), "MZ"));
        cti.setDefaultVisibleColumns(defaultCols);
        
        return cti;
    }

    public FeaturesTableInfo getFeaturesTableInfo()
    {
        return getFeaturesTableInfo(true);
    } //getFeaturesTableInfo()

    public FeaturesTableInfo getFeaturesTableInfoSearch()
    {
        FeaturesTableInfo table = getFeaturesTableInfo(true);

        //change the default visible columnset
        ArrayList<FieldKey> visibleColumns = new ArrayList<>(table.getDefaultVisibleColumns());
        visibleColumns.add(2, FieldKey.fromParts("FileId","ExpDataFileId","Run"));
        visibleColumns.remove(FieldKey.fromParts("AccurateMz"));
        visibleColumns.remove(FieldKey.fromParts("Mass"));
        visibleColumns.remove(FieldKey.fromParts("Peaks"));
        visibleColumns.remove(FieldKey.fromParts("TotalIntensity"));
        table.setDefaultVisibleColumns(visibleColumns);

        return table;
    } //getFeaturesTableInfo()

    public FeaturesTableInfo getFeaturesTableInfo(boolean includePepFk)
    {
        return new FeaturesTableInfo(this, includePepFk);
    } //getFeaturesTableInfo()

    public FeaturesTableInfo getFeaturesTableInfo(boolean includePepFk, Boolean peaksAvailable)
    {
        return new FeaturesTableInfo(this, includePepFk, peaksAvailable);
    } //getFeaturesTableInfo()

    public PeaksTableInfo getPeaksTableInfo()
    {
        return new PeaksTableInfo(this);
    }

    public FilesTableInfo getFilesTableInfo()
    {
        return new FilesTableInfo(_expSchema, _containerFilter);
    }

    public ScansTableInfo getScansTableInfo()
    {
        return new ScansTableInfo(this);
    }

    public ExpRunTable getMS1ExpRunsTableInfo()
    {
        // Start with a standard experiment run table
        ExpRunTable result = _expSchema.getRunsTable();
        result.setDescription("Contains a row per MS1 experiment run imported into this folder.");

        // Filter to just the runs with the MS1 protocol
        result.setProtocolPatterns("urn:lsid:%:Protocol.%:MS1.%");

        //add a new column for the features link
        //that wraps the experiment run row id
        ColumnInfo rowIdCol = result.getColumn("RowId");
        if(rowIdCol != null)
        {
            ColumnInfo featuresLinkCol = result.addColumn(new ExprColumn(result, "Features Link",
                    new SQLFragment(rowIdCol.getValueSql(ExprColumn.STR_TABLE_ALIAS)), rowIdCol.getJdbcType(), rowIdCol));
            featuresLinkCol.setDescription("Link to the msInspect features found in each run");
            featuresLinkCol.setDisplayColumnFactory(new DisplayColumnFactory()
            {
                public DisplayColumn createRenderer(ColumnInfo colInfo)
                {
                    return new FeaturesLinkDisplayColumn(colInfo, getContainer());
                }
            });
        }

        //reset the URL on the name column to jump to our feature details view
        ColumnInfo nameCol = result.getColumn("Name");
        if(null != nameCol)
        {
            ActionURL featuresUrl = new ActionURL(MS1Controller.ShowFeaturesAction.class, getContainer());
            featuresUrl.addParameter("runId", "${rowId}");
            nameCol.setURL(StringExpressionFactory.createURL(featuresUrl));
        }

        //set the default visible columns list
        List<FieldKey> columns = new ArrayList<>(result.getDefaultVisibleColumns());

        //remove unecessary columns
        columns.remove(FieldKey.fromParts("CreatedBy"));
        columns.remove(FieldKey.fromParts("Protocol"));

        //move the Features link
        columns.remove(FieldKey.fromParts("Features Link"));
        columns.add(2, FieldKey.fromParts("Features Link"));

        columns.add(FieldKey.fromParts("Input", "Spectra"));
        result.setDefaultVisibleColumns(columns);

        return result;
    } //getMS1ExpRunsTableInfo()

    /**
     * Returns the list of the appropriate container ids suitable for use in an IN filter.
     * If this schema is limited to a single container, it will contain only that container id,
     * but if it's not, it will contain the ids of the current container and all children in
     * which the user has read permissions.
     *
     * @return A string suitable for wrapping with an "IN ()" filter.
     */
    public String getContainerInList()
    {
        List<Container> containers = isRestrictContainer() ? new ArrayList<Container>()
                : ContainerManager.getAllChildren(getContainer(), getUser());
        containers.add(getContainer());

        StringBuilder filterList = new StringBuilder();
        String sep = "";
        for(Container c : containers)
        {
            filterList.append(sep);
            filterList.append("'");
            filterList.append(c.getId()); //Container Ids are GUIDS, so no embedded quotes
            filterList.append("'");
            sep = ",";
        }
        return filterList.toString();
    }
} //class MS1Schema
