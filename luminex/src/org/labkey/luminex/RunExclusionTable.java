package org.labkey.luminex;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.MultiValuedForeignKey;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.view.UnauthorizedException;

import java.util.Map;

/**
 * User: jeckels
 * Date: Jun 29, 2011
 */
public class RunExclusionTable extends AbstractExclusionTable
{
    public RunExclusionTable(final LuminexSchema schema)
    {
        super(LuminexSchema.getTableInfoRunExclusion(), schema);

        getColumn("RunId").setLabel("Run");
        getColumn("RunId").setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                AssaySchema assaySchema = AssayService.get().createSchema(schema.getUser(), schema.getContainer());
                return assaySchema.getTable(AssaySchema.getRunsTableName(schema.getProtocol()));
            }
        });

        getColumn("Analytes").setFk(new MultiValuedForeignKey(new LookupForeignKey("RunId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return schema.createRunExclusionAnalyteTable();
            }
        }, "AnalyteId"));
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new ExclusionUpdateService(this, getRealTable(), LuminexSchema.getTableInfoRunExclusionAnalyte(), "RunId")
        {
            @NotNull
            @Override
            protected ExpRun resolveRun(Map<String, Object> rowMap) throws QueryUpdateServiceException
            {
                Integer runId = convertToInteger(rowMap.get("RunId"));
                if (runId == null)
                {
                    throw new QueryUpdateServiceException("No RunId specified");
                }
                ExpRun run = ExperimentService.get().getExpRun(runId);
                if (run == null)
                {
                    throw new QueryUpdateServiceException("No such run: " + runId);
                }
                return run;
            }

            @Override
            protected void checkPermissions(User user, Map<String, Object> rowMap, Class<? extends Permission > permission) throws QueryUpdateServiceException
            {
                ExpRun run = resolveRun(rowMap);
                if (!run.getContainer().hasPermission(user, permission))
                {
                    throw new UnauthorizedException();
                }
            }
        };
    }
}
