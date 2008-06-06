package org.labkey.ms2.pipeline.sequest;

/**
 * User: billnelson@uky.edu
 * Date: May 16, 2008
 */

/**
 * <code>Out2XmlParams</code>
 */
public class Out2XmlParams extends Params
{
    public Out2XmlParams()
    {
        initProperties();
    }

    void initProperties()
    {
        _params.clear();
        _params.add(new Out2XmlParam(
            10,
            "",
            "-H",
            ConverterFactory.getOut2XmlConverter(),
            ParamsValidatorFactory.getPositiveIntegerParamsValidator()
        ).setInputXmlLabels("out2xml, top hits"));

        _params.add(new Out2XmlParam(
            20,
            "",
            "-E",
            ConverterFactory.getOut2XmlConverter(),
            null
        ).setInputXmlLabels("out2xml, enzyme"));

        _params.add(new Out2XmlParam(
            30,
            "",
            "-M",
            ConverterFactory.getOut2XmlConverter(),
            ParamsValidatorFactory.getBooleanParamsValidator()
        ).setInputXmlLabels("out2xml, maldi mode"));

        _params.add(new Out2XmlParam(
            40,
            "",
            "-pI",
            ConverterFactory.getOut2XmlConverter(),
            ParamsValidatorFactory.getBooleanParamsValidator()
        ).setInputXmlLabels("out2xml, pI"));

        _params.add(new Out2XmlParam(
            50,
            "",
            "-all",
            ConverterFactory.getOut2XmlConverter(),
            ParamsValidatorFactory.getBooleanParamsValidator()
        ).setInputXmlLabels("out2xml, all"));
    }

}
