package cpas.ms1;

import org.fhcrc.cpas.data.Container;
import org.fhcrc.cpas.data.TableInfo;
import org.fhcrc.cpas.exp.ExperimentManager;
import org.fhcrc.cpas.exp.api.ExpSchema;
import org.fhcrc.cpas.exp.api.ExpRunTable;
import org.fhcrc.cpas.query.api.DefaultSchema;
import org.fhcrc.cpas.query.api.QuerySchema;
import org.fhcrc.cpas.query.api.UserSchema;
import org.fhcrc.cpas.security.User;
import org.fhcrc.cpas.util.PageFlowUtil;

import java.util.Set;

/**
 * Provides a customized experiment run grid with features specific to MS1 runs.
 */
public class MS1Schema extends UserSchema
{
    public static final String SCHEMA_NAME = "ms1";
    public static final String MSINSPECT_FEATURE_EXPERIMENT_RUNS_TABLE_NAME = "MSInspectFeatureRuns";

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
        super(SCHEMA_NAME, user, container, ExperimentManager.get().getExpSchema());
        _expSchema = new ExpSchema(user, container);
    }
    
    public Set<String> getTableNames()
    {
        // Define a single table in this schema, for msInspect runs
        return PageFlowUtil.set(MSINSPECT_FEATURE_EXPERIMENT_RUNS_TABLE_NAME);
    }

    public TableInfo getTable(String name, String alias)
    {
        if (MSINSPECT_FEATURE_EXPERIMENT_RUNS_TABLE_NAME.equalsIgnoreCase(name))
        {
            // Start with a standard experiment run table
            ExpRunTable result = _expSchema.createRunsTable(alias);

            // Filter to just the runs with the MS1 protocol
            result.setProtocolPatterns("urn:lsid:%:Protocol.%:MS1.msInspectFeatureFindingAnalysis%");

            // Add columns for the input files
            result.addDefaultVisibleColumn("Input/msInspectDefFile");
            result.addDefaultVisibleColumn("Input/mzXMLFile");
            return result;
        }
        return super.getTable(name, alias);
    }
}
