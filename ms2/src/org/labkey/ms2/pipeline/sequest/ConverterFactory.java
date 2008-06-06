/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
    private static Out2XmlConverter _out2Xml;

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

    public static IInputXMLConverter getOut2XmlConverter()
    {
        if (_out2Xml == null)
        {
            _out2Xml = new Out2XmlConverter();
        }
        return _out2Xml;
    }
}
