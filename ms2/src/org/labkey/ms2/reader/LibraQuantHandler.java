/*
 * Copyright (c) 2011-2017 LabKey Corporation
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

/**
 * User: jeckels
 * Date: Aug 14, 2011
 */
public class LibraQuantHandler extends PepXmlAnalysisResultHandler
{
    public static final String ANALYSIS_TYPE = "libra";

    @Override
    protected String getAnalysisType()
    {
        return ANALYSIS_TYPE;
    }

    @Override
    protected PepXmlAnalysisResultHandler.PepXmlAnalysisResult getResult(SimpleXMLStreamReader parser) throws XMLStreamException
    {
        parser.skipToStart("libra_result");
        LibraQuantResult result = new LibraQuantResult();
        while (!parser.isEndElement() || !parser.getLocalName().equals("libra_result"))
        {
            parser.next();
            if (parser.isStartElement() && parser.getLocalName().equals("intensity"))
            {
                String channel = parser.getAttributeValue(null, "channel");
                switch (channel)
                {
                    case "1":
                        result.setTargetMass1(getTargetMass(parser));
                        result.setAbsoluteIntensity1(getAbsoluteMass(parser));
                        result.setNormalized1(getNormalized(parser));
                        break;
                    case "2":
                        result.setTargetMass2(getTargetMass(parser));
                        result.setAbsoluteIntensity2(getAbsoluteMass(parser));
                        result.setNormalized2(getNormalized(parser));
                        break;
                    case "3":
                        result.setTargetMass3(getTargetMass(parser));
                        result.setAbsoluteIntensity3(getAbsoluteMass(parser));
                        result.setNormalized3(getNormalized(parser));
                        break;
                    case "4":
                        result.setTargetMass4(getTargetMass(parser));
                        result.setAbsoluteIntensity4(getAbsoluteMass(parser));
                        result.setNormalized4(getNormalized(parser));
                        break;
                    case "5":
                        result.setTargetMass5(getTargetMass(parser));
                        result.setAbsoluteIntensity5(getAbsoluteMass(parser));
                        result.setNormalized5(getNormalized(parser));
                        break;
                    case "6":
                        result.setTargetMass6(getTargetMass(parser));
                        result.setAbsoluteIntensity6(getAbsoluteMass(parser));
                        result.setNormalized6(getNormalized(parser));
                        break;
                    case "7":
                        result.setTargetMass7(getTargetMass(parser));
                        result.setAbsoluteIntensity7(getAbsoluteMass(parser));
                        result.setNormalized7(getNormalized(parser));
                        break;
                    case "8":
                        result.setTargetMass8(getTargetMass(parser));
                        result.setAbsoluteIntensity8(getAbsoluteMass(parser));
                        result.setNormalized8(getNormalized(parser));
                        break;
                    case "9":
                        result.setTargetMass9(getTargetMass(parser));
                        result.setAbsoluteIntensity9(getAbsoluteMass(parser));
                        result.setNormalized9(getNormalized(parser));
                        break;
                    case "10":
                        result.setTargetMass10(getTargetMass(parser));
                        result.setAbsoluteIntensity10(getAbsoluteMass(parser));
                        result.setNormalized10(getNormalized(parser));
                        break;
                    default:
                        throw new IllegalStateException("Invalid iTRAQ index: " + channel + ". Supported values are 1-10.");
                }
            }
        }

        return result;
    }

    private double getTargetMass(SimpleXMLStreamReader parser)
    {
        return getDouble(parser, "target_mass");
    }

    private double getAbsoluteMass(SimpleXMLStreamReader parser)
    {
        return getDouble(parser, "absolute");
    }

    private double getNormalized(SimpleXMLStreamReader parser)
    {
        return getDouble(parser, "normalized");
    }

    private double getDouble(SimpleXMLStreamReader parser, String attributeName)
    {
        return Double.parseDouble(parser.getAttributeValue(null, attributeName));
    }

    public static RelativeQuantAnalysisSummary load(SimpleXMLStreamReader parser) throws XMLStreamException
    {
        // We must be on the analysis_summary start element when called.
        String analysisTime = parser.getAttributeValue(null, "time");

        RelativeQuantAnalysisSummary summary = new RelativeQuantAnalysisSummary();
        summary.setAnalysisType(parser.getAttributeValue(null, "analysis"));

        if (!parser.skipToStart("libra_summary"))
            throw new XMLStreamException("Did not find required q3ratio_summary tag in analysis result");

        summary.setMassTol(Q3AnalysisSummary.parseMassTol(parser.getAttributeValue(null, "mass_tolerance")));

        if (null != analysisTime)
            summary.setAnalysisTime(SimpleXMLEventRewriter.convertXMLTimeToDate(analysisTime));

        return summary;
    }
}
