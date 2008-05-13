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

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * User: adam
 * Date: May 6, 2006
 * Time: 4:35 pm
 */
public class PlainTextRenderer
{
    private static Logger _log = Logger.getLogger(PlainTextRenderer.class);

    HttpServletResponse _response;
    PrintWriter _out;

    public PlainTextRenderer(HttpServletResponse response, String filenamePrefix, String extension) throws IOException
    {
        // Flush any extraneous output (e.g., <CR><LF> from JSPs)
        response.reset();
        response.setContentType("text/plain");
        response.setHeader("Content-disposition", "attachment; filename=\"" + filenamePrefix + "_" + Math.round(Math.random() * 100000) + "." + extension + "\"");

        // Get the outputstream of the servlet (always get the outputstream AFTER you've set the content-disposition and content-type)
        _out = response.getWriter();
        _response = response;
    }


    public void close() throws IOException
    {
        // Flush the writer
        _out.flush();
        // Finally, close the writer
        _out.close();
    }
}
