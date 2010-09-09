/*
 * Copyright (c) 2006-2010 LabKey Corporation
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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * User: billnelson@uky.edu
 * Date: Oct 23, 2006
 * Time: 5:26:21 PM
 */
public class PositiveDoubleParamsValidator implements IParamsValidator
{
    public String validate(Param spp)
    {
        String parserError = "";
        double d;
        String value = spp.getValue();
        if (value == null)
        {
            parserError = spp.getInputXmlLabels().get(0) + ", " + "this value must be a positive number(" + value + ").\n";
            return parserError;
        }
        try
        {
            d = Double.parseDouble(value);
        }
        catch (NumberFormatException e)
        {
            parserError = spp.getInputXmlLabels().get(0) + ", " + "this value must be a positive number(" + value + ").\n";
            return parserError;
        }
        if (d < 0)
            parserError = spp.getInputXmlLabels().get(0) + ", " + "this value must be a positive number(" + value + ").\n";
        return parserError;
    }

    //JUnit TestCase
    public static class TestCase extends Assert
    {
        private SequestParam _property;

        @Before
        public void setUp() throws Exception
        {
            _property = new SequestParam(
                100,                                                       //sortOrder
                "5",                                                      //The value of the property
                "num_description_lines",                                  // the sequest.params property name
                "# full protein descriptions to show for top N peptides", // the sequest.params comment
                ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
                new PositiveDoubleParamsValidator(),
                true
            );
            _property.setInputXmlLabels("sequest, num_description_lines");
        }

        @After
        public void tearDown()
        {
            _property = null;
        }

        @Test
        public void testValidateNormal()
        {
            _property.setValue("1");
            String parserError = _property.validate();
            assertEquals("", parserError);
            assertEquals("1", _property.getValue());

            _property.setValue("0");
            parserError = _property.validate();
            assertEquals("", parserError);
            assertEquals("0", _property.getValue());

            _property.setValue("2.3");
            parserError = _property.validate();
            assertEquals("", parserError);
            assertEquals("2.3", _property.getValue());
        }

        @Test
        public void testValidateMissingValue()
        {
            _property.setValue("");
            String parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a positive number().\n", parserError);

            _property.setValue(null);
            parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a positive number(null).\n", parserError);
        }

        @Test
        public void testValidateNegative()
        {
            _property.setValue("-4");
            String parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a positive number(-4).\n", parserError);

            _property.setValue("-4.7");
            parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a positive number(-4.7).\n", parserError);
        }

        @Test
        public void testValidateGarbage()
        {
            _property.setValue("foo");
            String parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a positive number(foo).\n", parserError);

            _property.setValue("1. 2");
            parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a positive number(1. 2).\n", parserError);
        }
    }
}
