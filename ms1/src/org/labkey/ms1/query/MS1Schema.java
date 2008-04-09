package org.labkey.ms1.query;

import org.labkey.api.data.*;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExpRunTable;
import org.labkey.api.exp.api.ExpSchema;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.*;
import org.labkey.api.security.ACL;
import org.labkey.api.security.User;
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
    public static final String TABLE_FEATURE_RUNS = "MSInspectFeatureRuns";
    public static final String TABLE_FEATURES = "Features";
    public static final String TABLE_FEATURES_SEARCH = "FeaturesSearch";
    public static final String TABLE_PEAKS = "Peaks";
    public static final String TABLE_FILES = "Files";
    public static final String TABLE_SCANS = "Scans";
    public static final String TABLE_COMPARE_PEP = "ComparePeptide";

    static public void register()
    {
        DefaultSchema.registerProvider(SCHEMA_NAME, new DefaultSchema.SchemaProvider() {
            public QuerySchema getSchema(DefaultSchema schema)
            {
                return new MS1Schema(schema.getUser(), schema.getContainer());
            }
        });
    }

    private ExpSchema _expSchema;
    private boolean _restrictContainer = true;

    public MS1Schema(User user, Container container)
    {
        this(user, container, true);
    }

    public MS1Schema(User user, Container container, boolean restrictContainer)
    {
        super(SCHEMA_NAME, user, container, MS1Manager.get().getSchema());
        _expSchema = new ExpSchema(user, container);
        _expSchema.setRestrictContainer(restrictContainer);
        _restrictContainer = restrictContainer;
    }

    public boolean isRestrictContainer()
    {
        return _restrictContainer;
    }

    public Set<String> getTableNames()
    {
        HashSet<String> ret = new HashSet<String>();
        ret.add(TABLE_FEATURE_RUNS);
        ret.add(TABLE_FEATURES);
        ret.add(TABLE_FEATURES_SEARCH);
        ret.add(TABLE_FILES);
        ret.add(TABLE_PEAKS);
        ret.add(TABLE_SCANS);
        return ret;
    }

    public TableInfo getTable(String name, String alias)
    {
        if (TABLE_FEATURE_RUNS.equalsIgnoreCase(name))
            return getMS1ExpRunsTableInfo(alias);
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
        else
            return super.getTable(name, alias);
    } //getTable()

    public CrosstabTableInfo getComparePeptideTableInfo(int[] runIds)
    {
        FeaturesTableInfo tinfo = getFeaturesTableInfo(true, true);
        ArrayList<FeaturesFilter> filters = new ArrayList<FeaturesFilter>();
        //OK if runIds is null
        RunFilter runFilter = new RunFilter(runIds);
        filters.add(runFilter);
        //filter out features that don't have an associated peptide
        filters.add(new PeptideNotNullFilter());
        tinfo.setBaseFilters(filters);

        ActionURL urlPepSearch = new ActionURL(MS1Controller.PepSearchAction.class, getContainer());
        urlPepSearch.addParameter(MS1Controller.PepSearchForm.ParamNames.exact.name(), "on");
        urlPepSearch.addParameter(MS1Controller.PepSearchForm.ParamNames.runIds.name(), runFilter.getRunIdString());

        CrosstabSettings settings = new CrosstabSettings(tinfo);

        CrosstabDimension rowDim = settings.getRowAxis().addDimension(FieldKey.fromParts(FeaturesTableInfo.COLUMN_PEPTIDE_INFO, "Peptide"));
        rowDim.setUrl(urlPepSearch.getLocalURIString() + "&pepSeq=${RelatedPeptide_Peptide}");

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

        String measureUrl = new ActionURL(MS1Controller.ShowFeaturesAction.class, getContainer()).getLocalURIString()
                + MS1Controller.ShowFeaturesForm.ParamNames.runId.name() + "=" + CrosstabMember.VALUE_TOKEN
                + "&" + MS1Controller.ShowFeaturesForm.ParamNames.pepSeq.name() + "=${RelatedPeptide_Peptide}";
        for(CrosstabMeasure measure : settings.getMeasures())
            measure.setUrl(measureUrl);

        settings.setInstanceCountCaption("Num Runs");
        settings.getRowAxis().setCaption("Peptide Information");
        settings.getColumnAxis().setCaption("Runs");

        CrosstabTableInfo cti;
        if(null != runIds)
        {
            ArrayList<CrosstabMember> members = new ArrayList<CrosstabMember>();
            //build up the list of column members
            for(int runId : runIds)
            {
                ExpRun run = ExperimentService.get().getExpRun(runId);
                members.add(new CrosstabMember(Integer.valueOf(runId), colDim , null == run ? null : run.getName()));
            }
            cti = new CrosstabTableInfo(settings, members);
        }
        else
            cti = new CrosstabTableInfo(settings);

        List<FieldKey> defaultCols = cti.getDefaultVisibleColumns();
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
        ArrayList<FieldKey> visibleColumns = new ArrayList<FieldKey>(table.getDefaultVisibleColumns());
        visibleColumns.add(2, FieldKey.fromParts("FileId","ExpDataFileId","Run","Name"));
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
        return new PeaksTableInfo(this, getContainer());
    }

    public FilesTableInfo getFilesTableInfo()
    {
        return new FilesTableInfo(_expSchema, _restrictContainer ? getContainer() : null);
    }

    public ScansTableInfo getScansTableInfo()
    {
        return new ScansTableInfo(this, getContainer());
    }

    public ExpRunTable getMS1ExpRunsTableInfo(String alias)
    {
        // Start with a standard experiment run table
        ExpRunTable result = _expSchema.createRunsTable(alias);

        // Filter to just the runs with the MS1 protocol
        result.setProtocolPatterns("urn:lsid:%:Protocol.%:MS1.%");

        //add a new column for the features link
        //that wraps the experiment run row id
        ColumnInfo rowIdCol = result.getColumn("RowId");
        if(rowIdCol != null)
        {
            ColumnInfo featuresLinkCol = result.addColumn(new ExprColumn(result, "Features Link",
                    new SQLFragment(rowIdCol.getValueSql()), rowIdCol.getSqlTypeInt(), rowIdCol));
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
            nameCol.setURL(featuresUrl.getLocalURIString() + "runId=${rowId}");
        }

        //set the default visible columns list
        List<FieldKey> columns = new ArrayList<FieldKey>(result.getDefaultVisibleColumns());

        //remove unecessary columns
        columns.remove(FieldKey.fromParts("CreatedBy"));
        columns.remove(FieldKey.fromParts("Protocol"));

        //move the Features link
        columns.remove(FieldKey.fromParts("Features Link"));
        columns.add(2, FieldKey.fromParts("Features Link"));

        columns.add(FieldKey.fromParts("Input", "mzXMLFile"));
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
        Set<Container> containers = isRestrictContainer() ? new HashSet<Container>()
                : ContainerManager.getAllChildren(getContainer(), getUser(), ACL.PERM_READ);
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
