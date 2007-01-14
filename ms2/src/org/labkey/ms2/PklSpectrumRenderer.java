package org.labkey.ms2;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * User: adam
 * Date: May 10, 2006
 * Time: 12:51:22 PM
 */
public class PklSpectrumRenderer extends SpectrumRenderer
{
    public PklSpectrumRenderer(HttpServletResponse response, String filenamePrefix, String extension) throws IOException
    {
        super(response, filenamePrefix, extension);
    }

    public void renderFirstLine(Spectrum spectrum)
    {
        _out.println(df4.format(spectrum.getMZ()) + " 1 " + spectrum.getCharge());
    }
}
