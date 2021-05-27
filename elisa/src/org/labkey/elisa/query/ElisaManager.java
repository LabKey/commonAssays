package org.labkey.elisa.query;

import org.labkey.api.assay.dilution.DilutionAssayProvider;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.elisa.ElisaProtocolSchema;

public class ElisaManager
{
    public static CurveFitDb saveCurveFit(Container c, User user, CurveFitDb bean)
    {
        if (bean.isNew())
        {
            bean.beforeInsert(user, c.getId());
            return Table.insert(user, ElisaProtocolSchema.getTableInfoCurveFit(), bean);
        }
        else
        {
            bean.beforeUpdate(user);
            return Table.update(user, ElisaProtocolSchema.getTableInfoCurveFit(), bean, bean.getRowId());
        }
    }

    public static CurveFitDb getCurveFit(Container c, ExpRun run, String plateName, Integer spot)
    {
        SimpleFilter filter = SimpleFilter.createContainerFilter(c);
        filter.addCondition(FieldKey.fromParts("RunId"), run.getRowId());
        filter.addCondition(FieldKey.fromParts("PlateName"), plateName);
        filter.addCondition(FieldKey.fromParts("Spot"), spot);
        return new TableSelector(ElisaProtocolSchema.getTableInfoCurveFit(), filter, null).getObject(CurveFitDb.class);

    }

    public static void deleteContainerData(Container container)
    {
        SimpleFilter filter = SimpleFilter.createContainerFilter(container);
        Table.delete(ElisaProtocolSchema.getTableInfoCurveFit(), filter);
    }

    public static StatsService.CurveFitType getRunCurveFitType(Domain runDomain, ExpRun run)
    {
        StatsService.CurveFitType curveFitType = StatsService.CurveFitType.LINEAR;
        DomainProperty curveFitPd = runDomain.getPropertyByName(DilutionAssayProvider.CURVE_FIT_METHOD_PROPERTY_NAME);
        if (curveFitPd != null)
        {
            Object value = run.getProperty(curveFitPd);
            if (value != null)
                curveFitType = StatsService.CurveFitType.fromLabel(String.valueOf(value));
        }
        return curveFitType;
    }
}
