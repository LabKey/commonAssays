/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class CometDefLoader
{
    // Would be nice to simply use props.load(), but COMET DEF files have 1) inline comments and 2) a funky enzyme section,
    // so we have to parse manually
    protected static Properties load(InputStream is) throws IOException
    {
        String line, param, value;
        String enzymeNum = "0.";
        boolean readingParams = true;

        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        Properties props = new Properties();

        while ((line = br.readLine()) != null)
        {
            int commentIdx, equalsIdx;

            commentIdx = line.indexOf('#');

            // Strip out any comments and trim
            if (-1 != commentIdx)
                line = line.substring(0, commentIdx).trim();
            else
                line = line.trim();

            // Skip blank lines
            if (0 == line.length())
                continue;

            // File starts with param = value, param = value, etc. and ends with enzyme definitions
            if (readingParams)
            {
                if ("[COMET_ENZYME_DEF]".equals(line))
                {
                    String en = (String) props.get("ENZYMENUM");
                    enzymeNum = (null != en ? en + "." : "0.");
                    readingParams = false;
                    continue;
                }

                equalsIdx = line.indexOf('=');

                if (-1 != equalsIdx)
                {
                    param = line.substring(0, equalsIdx).trim().toUpperCase();
                    value = line.substring(equalsIdx + 1).trim();
                    props.setProperty(param, value);
                }
            }
            else
            {
                if (line.startsWith(enzymeNum))
                {
                    String enzymeLine, enzyme;
                    int spaceIdx;

                    enzymeLine = line.substring(enzymeNum.length()).trim();
                    spaceIdx = enzymeLine.indexOf(' ');

                    if (-1 != spaceIdx)
                        enzyme = enzymeLine.substring(0, spaceIdx).trim();
                    else
                        enzyme = enzymeLine;

                    props.setProperty("SEARCHENZYME", enzyme);
                }
            }
        }

        return props;
    }
}
