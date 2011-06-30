package org.labkey.luminex;

import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.view.UnauthorizedException;

import java.util.Map;

/**
 * User: jeckels
 * Date: Jun 29, 2011
 */
public class RunExclusionTable extends AbstractExclusionTable
{
    public RunExclusionTable(LuminexSchema schema)
    {
        super(LuminexSchema.getTableInfoRunExclusion(), schema);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new ExclusionUpdateService(this, getRealTable(), LuminexSchema.getTableInfoRunExclusionAnalyte(), "RunId")
        {
            private ExpRun getRun(Map<String, Object> rowMap) throws QueryUpdateServiceException
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
                ExpRun run = getRun(rowMap);
                if (!run.getContainer().hasPermission(user, permission))
                {
                    throw new UnauthorizedException();
                }
            }

            @Override
            protected void validateAnalyte(Map<String, Object> rowMap, Analyte analyte) throws QueryUpdateServiceException
            {
                for (ExpData data : getRun(rowMap).getAllDataUsedByRun())
                {
                    if (data.getRowId() == analyte.getDataId())
                    {
                        return;
                    }
                }
                throw new QueryUpdateServiceException("Attempting to reference analyte from another run");
            }
        };
    }
}
