package org.labkey.luminex;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.MultiValuedForeignKey;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocolApplication;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.view.UnauthorizedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Jun 29, 2011
 */
public class WellExclusionTable extends AbstractExclusionTable
{
    public WellExclusionTable(final LuminexSchema schema, boolean filter)
    {
        super(LuminexSchema.getTableInfoWellExclusion(), schema, filter);

        getColumn("DataId").setLabel("Data File");
        getColumn("DataId").setFk(new ExpSchema(schema.getUser(), schema.getContainer()).getDataIdForeignKey());
        
        getColumn("Analytes").setFk(new MultiValuedForeignKey(new LookupForeignKey("WellExclusionId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return schema.createWellExclusionAnalyteTable();
            }
        }, "AnalyteId"));

        List<FieldKey> defaultCols = new ArrayList<FieldKey>(getDefaultVisibleColumns());
        defaultCols.add(FieldKey.fromParts("DataId", "Run"));
        setDefaultVisibleColumns(defaultCols);
    }

    @Override
    protected SQLFragment createContainerFilterSQL(Collection<String> ids)
    {
        SQLFragment sql = new SQLFragment("DataId IN (SELECT RowId FROM ");
        sql.append(ExperimentService.get().getTinfoData(), "d");
        sql.append(" WHERE Container IN (");
        sql.append(StringUtils.repeat("?", ", ", ids.size()));
        sql.append("))");
        sql.addAll(ids);
        return sql;
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
                ExpData data = getData(rowMap);
                if (!data.getContainer().hasPermission(user, permission))
                {
                    throw new UnauthorizedException();
                }
            }

            private ExpData getData(Map<String, Object> rowMap) throws QueryUpdateServiceException
            {
                Integer dataId = getDataId(rowMap);
                ExpData data = ExperimentService.get().getExpData(dataId);
                if (data == null)
                {
                    throw new QueryUpdateServiceException("No such data file: " + dataId);
                }
                return data;
            }

            @Override
            protected @NotNull ExpRun resolveRun(Map<String, Object> rowMap) throws QueryUpdateServiceException
            {
                ExpData data = getData(rowMap);
                ExpProtocolApplication protApp = data.getSourceApplication();
                if (protApp == null)
                {
                    throw new QueryUpdateServiceException("Unable to resolve run for data " + data.getRowId() + ", no source protocol application");
                }
                ExpRun run = protApp.getRun();
                if (run == null)
                {
                    throw new QueryUpdateServiceException("Unable to resolve run for data " + data.getRowId());
                }
                return run;
            }
        };
    }
}
