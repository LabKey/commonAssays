package org.labkey.ms1.query;

import org.labkey.api.data.*;
import org.labkey.api.exp.api.ExpSchema;
import org.labkey.api.exp.api.ExpRunTable;
import org.labkey.api.query.*;
import org.labkey.api.security.User;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.api.view.ActionURL;
import org.labkey.ms1.query.FeaturesTableInfo;
import org.labkey.ms1.query.FilesTableInfo;
import org.labkey.ms1.MS1Manager;
import org.labkey.ms1.query.PeaksTableInfo;
import org.labkey.ms1.query.ScansTableInfo;
import org.labkey.ms1.MS1Module;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Provides a customized experiment run grid with features specific to MS1 runs.
 */
public class MS1Schema extends UserSchema
{
    public static final String SCHEMA_NAME = "ms1";
    public static final String TABLE_FEATURE_RUNS = "MSInspectFeatureRuns";
    public static final String TABLE_FEATURES = "Features";
    public static final String TABLE_PEAKS = "Peaks";
    public static final String TABLE_FILES = "Files";
    public static final String TABLE_SCANS = "Scans";

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
        return ret;
    }

    public TableInfo getTable(String name, String alias)
    {
        if (TABLE_FEATURE_RUNS.equalsIgnoreCase(name))
            return getMS1ExpRunsTableInfo(alias);
        else if(TABLE_FEATURES.equalsIgnoreCase(name))
            return getFeaturesTableInfo();
        else if(TABLE_PEAKS.equalsIgnoreCase(name))
            return getPeaksTableInfo();
        else if(TABLE_FILES.equalsIgnoreCase(name))
            return getFilesTableInfo();
        else if(TABLE_SCANS.equalsIgnoreCase(name))
            return getScansTableInfo();
        else
            return super.getTable(name, alias);
    } //getTable()

    public FeaturesTableInfo getFeaturesTableInfo()
    {
        return getFeaturesTableInfo(true);
    } //getFeaturesTableInfo()

    public FeaturesTableInfo getFeaturesTableInfo(boolean includePepFk)
    {
        return new FeaturesTableInfo(this, includePepFk);
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
        result.setProtocolPatterns("urn:lsid:%:Protocol.%:MS1.msInspectFeatureFindingAnalysis%");

        //add a new column info for the features link that uses a display column
        //factory to return a UrlColumn.
        //this depends on the RowId column, but that will always be selected
        //because it's a primary key
        ColumnInfo cinfo = new ColumnInfo("Features Link");
        cinfo.setDescription("Link to the msInspect features found in each run");
        cinfo.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                ActionURL url = new ActionURL(MS1Module.CONTROLLER_NAME, "showFeatures.view", getContainer());
                return new UrlColumn(StringExpressionFactory.create(url.getLocalURIString() + "runId=${RowId}", true), "features");
            }
        });
        result.addColumn(cinfo);

        //set the default visible columns list
        List<FieldKey> columns = new ArrayList<FieldKey>(result.getDefaultVisibleColumns());
        //move the Features link to position 1
        columns.remove(FieldKey.fromParts("Features Link"));
        columns.add(1, FieldKey.fromParts("Features Link"));

        //add the msInspect def file and mzXml file columns
        columns.add(FieldKey.fromParts("Input", "msInspectDefFile"));
        columns.add(FieldKey.fromParts("Input", "mzXMLFile"));
        result.setDefaultVisibleColumns(columns);

        return result;
    } //getMS1ExpRunsTableInfo()
} //class MS1Schema
