package org.labkey.ms2.pipeline.sequest;

/**
 * User: billnelson@uky.edu
 * Date: Jan 24, 2007
 * Time: 10:13:54 AM
 */
public class SequestParamsV2 extends SequestParams
{
    public SequestParamsV2()
    {
        super();
    }
    
    void initProperties()
    {
        super.initProperties();

        _params.add(new SequestParam(
                  130,                                                       //sortOrder
                  "trypsin 1 1 KR P",                                                      //The value of the property
                  "enzyme_info",                                           // the sequest.params property name
                  "",                                                       // the input.xml label
                   ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
                   null,
                   false
      ).setInputXmlLabels("protein, cleavage site"));
    }
}
