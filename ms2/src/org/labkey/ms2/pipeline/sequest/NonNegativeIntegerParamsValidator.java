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

package org.labkey.ms2.pipeline.sequest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * User: billnelson@uky.edu
 * Date: Oct 26, 2006
 * Time: 4:21:53 PM
 */
public class NonNegativeIntegerParamsValidator implements IParamsValidator
{
    public String validate(Param spp)
    {
        int i;
        String value = spp.getValue();
        if (value == null)
        {
            return spp.getInputXmlLabels().get(0) + ", " + "this value must be a non-negative integer(" + value + ").\n";
        }
        try
        {
            i = Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            return spp.getInputXmlLabels().get(0) + ", " + "this value must be a non-negative integer(" + value + ").\n";
        }
        if (i < 0)
            return spp.getInputXmlLabels().get(0) + ", " + "this value must be a non-negative integer(" + value + ").\n";
        return "";
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
                new NonNegativeIntegerParamsValidator(),
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

        }

        @Test
        public void testValidateMissingValue()
        {
            _property.setValue("");
            String parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a non-negative integer().\n", parserError);

            _property.setValue(null);
            parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a non-negative integer(null).\n", parserError);
        }

        @Test
        public void testValidateNegative()
        {
            String value = "-4";
            _property.setValue(value);
            String parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a non-negative integer(" + value + ").\n", parserError);

            value = "-4.7";
            _property.setValue(value);
            parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a non-negative integer(" + value + ").\n", parserError);

        }

        @Test
        public void testValidateGarbage()
        {
            _property.setValue("foo");
            String parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a non-negative integer(foo).\n", parserError);

            _property.setValue("1. 2");
            parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a non-negative integer(1. 2).\n", parserError);
        }
    }
}
