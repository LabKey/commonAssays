package org.labkey.elisa.query;

import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
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

    public static void deleteContainerData(Container container)
    {
        SimpleFilter filter = SimpleFilter.createContainerFilter(container);
        Table.delete(ElisaProtocolSchema.getTableInfoCurveFit(), filter);
    }
}
