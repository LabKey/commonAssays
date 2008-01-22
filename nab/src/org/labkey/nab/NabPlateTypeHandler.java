package org.labkey.nab;

import org.labkey.api.study.*;
import org.labkey.api.security.User;
import org.labkey.api.data.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.sql.SQLException;


/**
 * User: jeckels
 * Date: Apr 23, 2007
 */
public class NabPlateTypeHandler implements PlateTypeHandler
{
    public String getAssayType()
    {
        return "NAb";
    }

    public List<String> getTemplateTypes()
    {
        List<String> names = new ArrayList<String>();
        names.add("Default");
        return names;
    }

    public PlateTemplate createPlate(String templateTypeName, Container container) throws SQLException
    {
        PlateTemplate template = PlateService.get().createPlateTemplate(container, getAssayType());
        for (NabManager.PlateProperty prop : NabManager.PlateProperty.values())
            template.setProperty(prop.name(), "");

        template.addWellGroup(NabManager.CELL_CONTROL_SAMPLE, WellGroup.Type.CONTROL,
                PlateService.get().createPosition(container, 0, 0),
                PlateService.get().createPosition(container, template.getRows() - 1, 0));
        template.addWellGroup(NabManager.VIRUS_CONTROL_SAMPLE, WellGroup.Type.CONTROL,
                PlateService.get().createPosition(container, 0, 1),
                PlateService.get().createPosition(container, template.getRows() - 1, 1));

        if (templateTypeName != null && templateTypeName.equalsIgnoreCase("Default"))
        {
            for (int sample = 0; sample < 5; sample++)
            {
                int firstCol = (sample * 2) + 2;
                // create the overall specimen group, consisting of two adjacent columns:
                WellGroupTemplate sampleGroup = template.addWellGroup("Specimen " + (sample + 1), WellGroup.Type.SPECIMEN,
                        PlateService.get().createPosition(container, 0, firstCol),
                        PlateService.get().createPosition(container, 7, firstCol + 1));
                for (NabManager.SampleProperty prop : NabManager.SampleProperty.values())
                    sampleGroup.setProperty(prop.name(), "");
                for (int replicate = 0; replicate < template.getRows(); replicate++)
                {
                    template.addWellGroup("Specimen " + (sample + 1) + ", Replicate " + (replicate + 1), WellGroup.Type.REPLICATE,
                            PlateService.get().createPosition(container, replicate, firstCol),
                            PlateService.get().createPosition(container, replicate, firstCol + 1));
                }
            }
        }
        return template;
    }

    public WellGroup.Type[] getWellGroupTypes()
    {
        return new WellGroup.Type[]{
                WellGroup.Type.CONTROL, WellGroup.Type.SPECIMEN,
                WellGroup.Type.REPLICATE, WellGroup.Type.OTHER};
    }
}
