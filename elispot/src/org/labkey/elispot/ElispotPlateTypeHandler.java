package org.labkey.elispot;

import org.labkey.api.data.Container;
import org.labkey.api.study.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: Karl Lum
 * Date: Jan 14, 2008
 */
public class ElispotPlateTypeHandler implements PlateTypeHandler
{
    public String getAssayType()
    {
        return "ELISpot";
    }

    public List<String> getTemplateTypes()
    {
        return new ArrayList<String>();
    }

    public PlateTemplate createPlate(String templateTypeName, Container container) throws SQLException
    {
        return PlateService.get().createPlateTemplate(container, getAssayType());
    }

    public WellGroup.Type[] getWellGroupTypes()
    {
        return new WellGroup.Type[]{
                WellGroup.Type.CONTROL, WellGroup.Type.SPECIMEN,
                WellGroup.Type.REPLICATE, WellGroup.Type.ANTIGEN};
    }
}

