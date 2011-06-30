package org.labkey.luminex;

import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.query.ExpSchema;
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
public class WellExclusionTable extends AbstractExclusionTable
{
    public WellExclusionTable(LuminexSchema schema)
    {
        super(LuminexSchema.getTableInfoWellExclusion(), schema);

        getColumn("DataId").setFk(new ExpSchema(schema.getUser(), schema.getContainer()).getDataIdForeignKey());
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        return new ExclusionUpdateService(this, getRealTable(), LuminexSchema.getTableInfoWellExclusionAnalyte(), "WellExclusionId")
        {
            private Integer getDataId(Map<String, Object> rowMap) throws QueryUpdateServiceException
            {
                Integer dataId = convertToInteger(rowMap.get("DataId"));
                if (dataId == null)
                {
                    throw new QueryUpdateServiceException("No DataId specified");
                }
                return dataId;
            }

            @Override
            protected void checkPermissions(User user, Map<String, Object> rowMap, Class<? extends Permission> permission) throws QueryUpdateServiceException
            {
                Integer dataId = getDataId(rowMap);
                ExpData data = ExperimentService.get().getExpData(dataId);
                if (data == null)
                {
                    throw new QueryUpdateServiceException("No such data file: " + dataId);
                }
                if (!data.getContainer().hasPermission(user, permission))
                {
                    throw new UnauthorizedException();
                }
            }

            @Override
            protected void validateAnalyte(Map<String, Object> rowMap, Analyte analyte) throws QueryUpdateServiceException
            {
                if (analyte.getDataId() != getDataId(rowMap))
                {
                    throw new QueryUpdateServiceException("Attempting to reference analyte from another run (dataId " + analyte.getDataId() + " vs " + getDataId(rowMap) + ")");
                }
            }
        };
    }
}
