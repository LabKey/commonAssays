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

import java.util.StringTokenizer;

/**
 * User: billnelson@uky.edu
 * Date: Oct 23, 2006
 * Time: 4:55:01 PM
 */


// Counting numbers 1,2,3,....
public class NaturalNumberParamsValidator implements IParamsValidator
{
    public String validate(Param spp)
    {
        String parserError = "";
        int i;
        String value = spp.getValue();
        if (value == null || value.equals(""))
        {
            parserError = spp.getInputXmlLabels().get(0) + ", " + "this value must be a natural number(" + value + ").\n";
            return parserError;
        }
        StringTokenizer st = new StringTokenizer(value, ",");
        while (st.hasMoreTokens())
        {
            String token = st.nextToken();
            try
            {
                i = Integer.parseInt(token);
            }
            catch (NumberFormatException e)
            {
                parserError = spp.getInputXmlLabels().get(0) + ", " + "this value must be a natural number(" + token + ").\n";
                return parserError;
            }
            if (i < 1)
                parserError = spp.getInputXmlLabels().get(0) + ", " + "this value must be a natural number(" + token + ").\n";
        }
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
                new SequestBasicConverter(),                              //converts the instance to a sequest.params line
                new NaturalNumberParamsValidator(),
                true);
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

            _property.setValue("1,2,3");
            parserError = _property.validate();
            assertEquals("", parserError);

        }

        @Test
        public void testValidateMissingValue()
        {
            _property.setValue("");
            String parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a natural number().\n", parserError);

            _property.setValue(null);
            parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a natural number(null).\n", parserError);
        }

        @Test
        public void testValidateNegative()
        {
            String value = "-1";
            _property.setValue(value);
            String parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a natural number(" + value + ").\n", parserError);

            value = "0";
            _property.setValue(value);
            parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a natural number(" + value + ").\n", parserError);

            value = "-1.4";
            _property.setValue(value);
            parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a natural number(" + value + ").\n", parserError);
        }

        @Test
        public void testValidateGarbage()
        {
            _property.setValue("foo");
            String parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a natural number(foo).\n", parserError);

            _property.setValue("1.2");
            parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a natural number(1.2).\n", parserError);
        }
    }
}
