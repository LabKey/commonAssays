package org.labkey.nab.qc;

import org.labkey.api.exp.ExpQCFlag;
import org.labkey.nab.NabAssayController;

/**
 * Created by klum on 12/23/2016.
 */
public class NabWellQCFlag extends ExpQCFlag
{
    public static final String FLAG_TYPE = "Well QC";

    public NabWellQCFlag(){}

    public NabWellQCFlag(int runId, NabAssayController.WellExclusion exclusion)
    {
        super(runId, FLAG_TYPE, getDescription(exclusion));
        setComment(exclusion.getComment());
        setEnabled(true);
        setKey1(exclusion.getKey());
    }

    private static String getDescription(NabAssayController.WellExclusion exclusion)
    {
        return String.format("Well group name : %s, location : %s%s plate: %s", exclusion.getSpecimen(), exclusion.getRowLabel(), exclusion.getCol(), exclusion.getPlate());
    }
}
