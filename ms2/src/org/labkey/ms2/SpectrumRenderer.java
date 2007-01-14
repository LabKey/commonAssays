package org.labkey.ms2;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DecimalFormat;

/**
 * User: adam
 * Date: May 8, 2006
 * Time: 5:23:26 PM
 */
public class SpectrumRenderer extends PlainTextRenderer
{
    protected static DecimalFormat df1 = new DecimalFormat("0.0");
    protected static DecimalFormat df4 = new DecimalFormat("0.0000");

    public SpectrumRenderer(HttpServletResponse response, String filenamePrefix, String extension) throws IOException
    {
        super(response, filenamePrefix, extension);
    }


    public void render(SpectrumIterator iter) throws IOException
    {
        while (iter.hasNext())
        {
            Spectrum spectrum = iter.next();
            renderFirstLine(spectrum);
            renderSpectrum(spectrum);
        }
    }


    protected void renderSpectrum(Spectrum spectrum)
    {
        float[] x = spectrum.getX();
        float[] y = spectrum.getY();

        for (int i = 0; i < x.length; i++)
        {
            _out.println(df1.format(x[i]) + " " + df1.format(y[i]));
        }

        _out.println();
    }


    protected void renderFirstLine(Spectrum spectrum)
    {
    }
}
