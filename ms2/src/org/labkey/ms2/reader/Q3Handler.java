/*
 * Copyright (c) 2006-2012 LabKey Corporation
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
package org.labkey.ms2.reader;

import org.apache.log4j.Logger;
import org.labkey.api.reader.SimpleXMLStreamReader;

import javax.xml.stream.XMLStreamException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * User: jeckels
 * Date: Dec 14, 2006
 */
public class Q3Handler extends PepXmlAnalysisResultHandler
{
    public static final String ANALYSIS_TYPE = "q3";

    static Logger _log = Logger.getLogger(PepXmlAnalysisResult.class);

    protected Q3Result getResult(SimpleXMLStreamReader parser) throws XMLStreamException
    {
        parser.skipToStart("q3ratio_result");
        Q3Result result = new Q3Result();

        result.setDecimalRatio(parseFloat(parser.getAttributeValue(null, "decimal_ratio")));
        result.setHeavyArea(parseFloat(parser.getAttributeValue(null, "heavy_area")));
        result.setHeavyFirstscan(Integer.parseInt(parser.getAttributeValue(null, "heavy_firstscan")));
        result.setHeavyLastscan(Integer.parseInt(parser.getAttributeValue(null, "heavy_lastscan")));
        result.setHeavyMass(parseFloat(parser.getAttributeValue(null, "heavy_mass")));
        result.setLightArea(parseFloat(parser.getAttributeValue(null, "light_area")));
        result.setLightFirstscan(Integer.parseInt(parser.getAttributeValue(null, "light_firstscan")));
        result.setLightLastscan(Integer.parseInt(parser.getAttributeValue(null, "light_lastscan")));
        result.setLightMass(parseFloat(parser.getAttributeValue(null, "light_mass")));
        return result;
    }

    private float parseFloat(String s)
    {
        // Deal with localized strings that use a comma as the decimal place
        s = s.replace(',', '.');
        return Float.parseFloat(s);
    }

    public String getAnalysisType()
    {
        return ANALYSIS_TYPE;
    }

    public static class Q3Result extends RelativeQuantAnalysisResult
    {
        public String getAnalysisType()
        {
            return ANALYSIS_TYPE;
        }

        @Override
        protected int setRatios(PreparedStatement stmt, int index) throws SQLException
        {
            stmt.setNull(index++, Types.VARCHAR);
            stmt.setNull(index++, Types.VARCHAR);
            return index;
        }
    }
}
