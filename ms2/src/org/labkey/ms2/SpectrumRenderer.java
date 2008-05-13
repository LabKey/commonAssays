/*
 * Copyright (c) 2006-2007 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
