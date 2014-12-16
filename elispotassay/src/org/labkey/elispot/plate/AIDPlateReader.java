package org.labkey.elispot.plate;

import org.labkey.api.query.ValidationException;
import org.labkey.api.study.assay.plate.TextPlateReader;

/**
 * Created by klum on 12/14/14.
 */
public class AIDPlateReader extends TextPlateReader
{
    public static final String TYPE = "aid_txt";

    public String getType()
    {
        return TYPE;
    }

    @Override
    protected double convertWellValue(String token) throws ValidationException
    {
        if ("TNTC".equalsIgnoreCase(token))
        {
            return WELL_NOT_COUNTED;
        }
        return super.convertWellValue(token);
    }
}
