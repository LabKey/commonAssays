package org.labkey.ms1;

import org.labkey.api.data.*;
import org.labkey.api.exp.api.ExpSchema;
import org.labkey.api.exp.api.ExpRunTable;
import org.labkey.api.query.*;
import org.labkey.api.security.User;

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

    public MS1Schema(User user, Container container)
    {
        super(SCHEMA_NAME, user, container, MS1Manager.get().getSchema());  //ExperimentService.get().getSchema());
        _expSchema = new ExpSchema(user, container);
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
        {
            // Start with a standard experiment run table
            ExpRunTable result = _expSchema.createRunsTable(alias);

            // Filter to just the runs with the MS1 protocol
            result.setProtocolPatterns("urn:lsid:%:Protocol.%:MS1.msInspectFeatureFindingAnalysis%");

            List<FieldKey> columns = new ArrayList(result.getDefaultVisibleColumns());
            columns.add(FieldKey.fromParts("Input", "msInspectDefFile"));
            columns.add(FieldKey.fromParts("Input", "mzXMLFile"));
            result.setDefaultVisibleColumns(columns);
            return result;
        }
        if(TABLE_FEATURES.equalsIgnoreCase(name))
        {
            FilteredTable ft = new FilteredTable(MS1Manager.get().getTable(MS1Manager.TABLE_FEATURES));

            //wrap all the columns
            ft.wrapAllColumns(true);

            //but only display a subset by default
            List<FieldKey> visibleColumns = new ArrayList(ft.getDefaultVisibleColumns());
            visibleColumns.remove(FieldKey.fromParts("FeatureID"));
            visibleColumns.remove(FieldKey.fromParts("FeaturesFileID"));
            visibleColumns.remove(FieldKey.fromParts("Description"));
            ft.setDefaultVisibleColumns(visibleColumns);

            //tell it that ms1.FeaturesFiles.FeaturesFileID is a foreign key to exp.Data.RowId
            TableInfo fftinfo = ft.getColumn("FeaturesFileID").getFkTableInfo();
            ColumnInfo ffid = fftinfo.getColumn("ExpDataFileID");
            ffid.setFk(new LookupForeignKey("RowId")
            {
                public TableInfo getLookupTableInfo()
                {
                    return _expSchema.createDatasTable(null);
                }
            });

            //add a condition that limits the features returned to just those existing in the
            //current container. The FilteredTable class supports this automatically only if
            //the underlying table contains a column named "Container," which our Features table
            //does not, so we need to use a SQL fragment here that uses a sub-select.
            SQLFragment sf = new SQLFragment("FeaturesFileID IN (SELECT FeaturesFileID FROM ms1.FeaturesFiles as f INNER JOIN Exp.Data as d ON (f.ExpDataFileID=d.RowId) WHERE d.Container=?)",
                                                getContainer().getId());
            ft.addCondition(sf, "FeaturesFileID");
            
            return ft;
        }
        return super.getTable(name, alias);
    }
}
