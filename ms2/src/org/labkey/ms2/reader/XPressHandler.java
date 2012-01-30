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

import org.labkey.api.reader.SimpleXMLStreamReader;

import javax.xml.stream.XMLStreamException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DecimalFormat;

/**
 * User: arauch
 * Date: Feb 16, 2006
 * Time: 1:53:08 PM
 */
public class XPressHandler extends PepXmlAnalysisResultHandler
{
    public final static String analysisType = "xpress";

    protected XPressResult getResult(SimpleXMLStreamReader parser) throws XMLStreamException
    {
        parser.skipToStart("xpressratio_result");
        XPressResult result = new XPressResult();

        //"decimal_ratio" is a field known to have "inf" as a value representing infinity sometimes
        result.setDecimalRatio(parseFloatHandleInf(parser.getAttributeValue(null, "decimal_ratio")));
        result.setHeavy2lightRatio(parser.getAttributeValue(null, "heavy2light_ratio"));
        result.setHeavyArea(parseFloat(parser.getAttributeValue(null, "heavy_area")));
        result.setHeavyFirstscan(Integer.parseInt(parser.getAttributeValue(null, "heavy_firstscan")));
        result.setHeavyLastscan(Integer.parseInt(parser.getAttributeValue(null, "heavy_lastscan")));
        result.setHeavyMass(parseFloat(parser.getAttributeValue(null, "heavy_mass")));
        result.setLightArea(parseFloat(parser.getAttributeValue(null, "light_area")));
        result.setLightFirstscan(Integer.parseInt(parser.getAttributeValue(null, "light_firstscan")));
        result.setLightLastscan(Integer.parseInt(parser.getAttributeValue(null, "light_lastscan")));
        result.setLightMass(parseFloat(parser.getAttributeValue(null, "light_mass")));
        result.setRatio(parser.getAttributeValue(null, "ratio"));

        return result;
    }

    private float parseFloat(String stringValue)
    {
        // Float.parseFloat() is case sensitive when parsing "NaN", so be more tolerant
        if ("nan".equalsIgnoreCase(stringValue))
        {
            return Float.NaN;
        }
        return Float.parseFloat(stringValue);
    }

    protected String getAnalysisType()
    {
        return analysisType;
    }

    public static class XPressResult extends RelativeQuantAnalysisResult
    {
        private String ratio;
        private String heavy2lightRatio;

        public String getRatio()
        {
            return ratio;
        }

        public void setRatio(String ratio)
        {
            this.ratio = ratio;
        }

        public String getHeavy2lightRatio()
        {
            return heavy2lightRatio;
        }

        public void setHeavy2lightRatio(String heavy2lightRatio)
        {
            this.heavy2lightRatio = heavy2lightRatio;
        }

        public String getAnalysisType()
        {
            return "xpress";
        }

        /**
         * Limit the total length of the ratio to 20 characters or less to fit within our database field -
         * sometimes we get numbers with lots and lots of decimal places. Truncate at five decimals.
         * Also, replace very large numbers (>9999) with INF.
         */
        private String fixupStringRatio(String ratio)
        {
            String[] numbers = ratio.split(":");
            if (numbers.length == 2)
            {
                try
                {
                    double number1 = Double.parseDouble(numbers[0]);
                    double number2 = Double.parseDouble(numbers[1]);

                    DecimalFormat format = new DecimalFormat("0.#####");
                    String result = number1 > 9999.0 ? "INF" : format.format(number1);
                    result += ":";
                    result += number2 > 9999.0 ? "INF" : format.format(number2);
                    return result;
                }
                catch (NumberFormatException ignored) {}
            }
            return ratio;
        }

        @Override
        protected int setRatios(PreparedStatement stmt, int index) throws SQLException
        {
            if (getRatio() != null)
            {
                stmt.setString(index++, fixupStringRatio(getRatio()));
            }
            else
            {
                stmt.setNull(index++, Types.VARCHAR);
            }
            if (getHeavy2lightRatio() != null)
            {
                stmt.setString(index++, fixupStringRatio(getHeavy2lightRatio()));
            }
            else
            {
                stmt.setNull(index++, Types.VARCHAR);
            }
            return index;
        }
    }
}
