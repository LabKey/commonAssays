package org.labkey.ms2.pipeline.sequest;

import org.labkey.ms2.pipeline.sequest.ConverterFactory;
import org.labkey.ms2.pipeline.sequest.Mzxml2SearchParam;
import org.labkey.ms2.pipeline.sequest.Params;
import org.labkey.ms2.pipeline.sequest.ParamsValidatorFactory;

/**
 * User: billnelson@uky.edu
 * Date: Oct 26, 2006
 * Time: 10:46:50 AM
 */
public class Mzxml2SearchParams extends Params
{

    public Mzxml2SearchParams()
    {
        initProperties();
    }

    void initProperties()
    {
        _params.clear();
        _params.add(new Mzxml2SearchParam(
            10,
            "",
            "-F",
            ConverterFactory.getMzxml2SearchConverter(),
            ParamsValidatorFactory.getNaturalNumberParamsValidator()
        ).setInputXmlLabels("MzXML2Search, first scan"));

        _params.add(new Mzxml2SearchParam(
            20,
            "",
            "-L",
            ConverterFactory.getMzxml2SearchConverter(),
            ParamsValidatorFactory.getNaturalNumberParamsValidator()
        ).setInputXmlLabels("MzXML2Search, last scan"));

        _params.add(new Mzxml2SearchParam(
            30,
            "1,2,3",
            "-C",
            ConverterFactory.getMzxml2SearchConverter(),
            ParamsValidatorFactory.getNaturalNumberParamsValidator()
        ).setInputXmlLabels("MzXML2Search, charge"));

        _params.add(new Mzxml2SearchParam(
            40,
            "10",
            "-P",
            ConverterFactory.getMzxml2SearchConverter(),
            ParamsValidatorFactory.getNaturalNumberParamsValidator()
        ).setInputXmlLabels("spectrum, minimum peaks"));
    }
}
