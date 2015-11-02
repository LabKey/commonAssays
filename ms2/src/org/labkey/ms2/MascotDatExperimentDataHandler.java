package org.labkey.ms2;

import org.apache.commons.io.FilenameUtils;
import org.labkey.api.exp.api.ExpData;

/**
 * Created by susanh on 10/27/15.
 */
public class MascotDatExperimentDataHandler extends PepXmlExperimentDataHandler
{
    public Priority getPriority(ExpData data)
    {
        if (data != null && data.getFile() != null && FilenameUtils.isExtension(data.getFile().getName(), "dat"))
        {
            return Priority.HIGH;
        }
        return null;
    }
}
