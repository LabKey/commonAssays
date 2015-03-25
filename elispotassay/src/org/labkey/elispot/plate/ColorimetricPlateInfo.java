package org.labkey.elispot.plate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.elispot.ElispotDataHandler;

/**
 * Created by klum on 3/23/2015.
 */
public class ColorimetricPlateInfo implements PlateInfo
{
    @NotNull
    @Override
    public String getMeasurement()
    {
        return ElispotDataHandler.SFU_PROPERTY_NAME;
    }

    @Nullable
    @Override
    public String getAnalyte()
    {
        return null;
    }
}
