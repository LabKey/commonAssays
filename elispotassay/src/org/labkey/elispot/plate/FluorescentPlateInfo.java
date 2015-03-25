package org.labkey.elispot.plate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.elispot.ElispotDataHandler;

/**
 * Created by klum on 3/23/2015.
 */
public class FluorescentPlateInfo implements PlateInfo
{
    private String _measurement;
    private String _analyte;

    private FluorescentPlateInfo(String measurement, String analyte)
    {
        _measurement = measurement;
        _analyte = analyte;
    }

    @NotNull
    @Override
    public String getMeasurement()
    {
        return _measurement;
    }

    @Nullable
    @Override
    public String getAnalyte()
    {
        return _analyte;
    }

    /**
     * Factory to parse annotations and create PlateInfo instances
     * @return
     */
    @Nullable
    public static FluorescentPlateInfo create(String annotation)
    {
        if (annotation != null)
        {
            String analyte = null;
            String measurement = null;

            int analyteIdx = annotation.indexOf(':');
            int parenIdx = annotation.indexOf(')');
            if (analyteIdx != -1 && parenIdx != -1)
            {
                analyte = annotation.substring(analyteIdx+1, parenIdx);
            }

            if (analyte != null)
            {
                String lcAnnotation = annotation.toLowerCase();

                if (lcAnnotation.contains("spots number"))
                    measurement = ElispotDataHandler.SFU_PROPERTY_NAME;
                else if (lcAnnotation.contains("activity"))
                    measurement = ElispotDataHandler.ACTIVITY_PROPERTY_NAME;
                else if (lcAnnotation.contains("intensity"))
                    measurement = ElispotDataHandler.INTENSITY_PROPERTY_NAME;
            }

            if (analyte != null && measurement != null)
                return new FluorescentPlateInfo(measurement, analyte);
        }
        return null;
    }
}
