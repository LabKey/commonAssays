/*
 * Copyright (c) 2006-2018 Fred Hutchinson Cancer Research Center
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

import java.util.Date;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Helper class to pull events and rewrite them with modification or with new
 * elements interspersed. Override handleStartElement, handleEndElement, etc.,
 * instantiate, and call rewrite(). See the Q3.java code for an example of
 * rewriting a pepXML file.
 */
public class SimpleXMLEventRewriter
{
    private static final DatatypeFactory typeFactory;

    static
    {
        // Rethrow any DatatypeFactory setup errors
        try
        {
            typeFactory = DatatypeFactory.newInstance();
        }
        catch (DatatypeConfigurationException e)
        {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Convert an XML time element to a Date
     */
    public static Date convertXMLTimeToDate(String lexTime)
    {
        XMLGregorianCalendar xgc = typeFactory.newXMLGregorianCalendar(lexTime);
        return xgc.toGregorianCalendar().getTime();
    }
}
