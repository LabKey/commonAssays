package org.labkey.elispot.plate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by klum on 3/23/2015.
 */
public interface PlateInfo
{
    /**
     * Returns the type of measurement the corresponding plate contains
     * @return the measurement name
     */
    @NotNull
    public String getMeasurement();

    /**
     * Returns the name of the analyte used for a corresponding plate data
     * @return the name of the analyte
     */
    @Nullable
    public String getAnalyte();
}
