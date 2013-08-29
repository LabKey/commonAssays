/*
 * Copyright (c) 2006-2013 LabKey Corporation
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
import java.io.PrintWriter;
import java.text.DecimalFormat;

/**
 * A renderer of spectrum information that ends up as some type of text file
 * User: adam
 * Date: May 8, 2006
 * Time: 5:23:26 PM
 */
public abstract class AbstractTextSpectrumRenderer implements SpectrumRenderer
{
    protected static DecimalFormat df1 = new DecimalFormat("0.0");
    protected static DecimalFormat df4 = new DecimalFormat("0.0000");

    HttpServletResponse _response;
    PrintWriter _out;

    protected AbstractTextSpectrumRenderer(HttpServletResponse response, String filenamePrefix, String extension) throws IOException
    {
        // Flush any extraneous output (e.g., <CR><LF> from JSPs)
        response.reset();
        response.setContentType("text/plain");
        response.setHeader("Content-disposition", "attachment; filename=\"" + filenamePrefix + "_" + Math.round(Math.random() * 100000) + "." + extension + "\"");

        // Get the outputstream of the servlet (always get the outputstream AFTER you've set the content-disposition and content-type)
        _out = response.getWriter();
        _response = response;
    }

    @Override
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

    @Override
    public void close() throws IOException
    {
        // Flush the writer
        _out.flush();
        // Finally, close the writer
        _out.close();
    }

    protected abstract void renderFirstLine(Spectrum spectrum);
}
