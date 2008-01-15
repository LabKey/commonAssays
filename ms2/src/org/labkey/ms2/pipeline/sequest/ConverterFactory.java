package org.labkey.ms2.pipeline.sequest;

/**
 * User: billnelson@uky.edu
 * Date: Oct 26, 2006
 * Time: 11:30:06 AM
 */
public class ConverterFactory
{
    private static SequestBasicConverter _sequestBasic;
    private static SequestEnzymeConverter _sequestEnzyme;
    private static SequestHeaderConverter _sequestHeader;
    private static Mzxml2SearchConverter _mzxml2Search;

    public static IInputXMLConverter getSequestBasicConverter()
    {
        if (_sequestBasic == null)
        {
            _sequestBasic = new SequestBasicConverter();
        }
        return _sequestBasic;
    }

    public static IInputXMLConverter getSequestEnzymeConverter()
    {
        if (_sequestEnzyme == null)
        {
            _sequestEnzyme = new SequestEnzymeConverter();
        }
        return _sequestEnzyme;
    }

    public static IInputXMLConverter getSequestHeaderConverter()
    {
        if (_sequestHeader == null)
        {
            _sequestHeader = new SequestHeaderConverter();
        }
        return _sequestHeader;
    }

    public static IInputXMLConverter getMzxml2SearchConverter()
    {
        if (_mzxml2Search == null)
        {
            _mzxml2Search = new Mzxml2SearchConverter();
        }
        return _mzxml2Search;
    }
}
