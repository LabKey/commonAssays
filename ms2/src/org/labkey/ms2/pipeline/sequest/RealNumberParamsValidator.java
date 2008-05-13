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

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * User: billnelson@uky.edu
 * Date: Oct 24, 2006
 * Time: 12:16:56 PM
 */
public class RealNumberParamsValidator implements IParamsValidator
{
    public String validate(Param spp)
    {
        String parserError = "";
        String value = spp.getValue();
        if (value == null)
        {
            parserError = spp.getInputXmlLabels().get(0) + ", " + "this value must be a real number(" + value + ").\n";
            return parserError;
        }
        try
        {
            Double.parseDouble(value);
        }
        catch (NumberFormatException e)
        {
            parserError = spp.getInputXmlLabels().get(0) + ", " + "this value must be a real number(" + value + ").\n";
            return parserError;
        }
        return parserError;
    }

    //JUnit TestCase
    public static class TestCase extends junit.framework.TestCase
    {

        private SequestParam _property;

        TestCase(String name)
        {
            super(name);
        }

        public static Test suite()
        {
            TestSuite suite = new TestSuite();
            suite.addTest(new TestCase("testValidateNormal"));
            suite.addTest(new TestCase("testValidateMissingValue"));
            suite.addTest(new TestCase("testValidateGarbage"));

            return suite;
        }

        protected void setUp() throws Exception
        {
            _property = new SequestParam(
                310,                                                       //sortOrder
                "0.0000",                                            //The value of the property
                "add_Cterm_peptide",                                // the sequest.params property name
                "added to each peptide C-terminus",       // the sequest.params comment
                new SequestBasicConverter(),                      //converts the instance to a sequest.params line
                new RealNumberParamsValidator(),
                true);
            _property.setInputXmlLabels().setInputXmlLabels("protein, cleavage C-terminal mass change");
        }

        protected void tearDown()
        {
            _property = null;
        }

        public void testValidateNormal()
        {
            String value = "1";
            _property.setValue(value);
            String parserError = _property.validate();
            assertEquals("", parserError);
            assertEquals(value, _property.getValue());

            value = "0";
            _property.setValue(value);
            parserError = _property.validate();
            assertEquals("", parserError);
            assertEquals(value, _property.getValue());

            value = "-1";
            _property.setValue(value);
            parserError = _property.validate();
            assertEquals("", parserError);
            assertEquals(value, _property.getValue());

            value = "-1.900";
            _property.setValue(value);
            parserError = _property.validate();
            assertEquals("", parserError);
            assertEquals(value, _property.getValue());

        }

        public void testValidateMissingValue()
        {
            String value = "";
            _property.setValue(value);
            String parserError = _property.validate();
            assertEquals("protein, cleavage C-terminal mass change, this value must be a real number(" + value + ").\n", parserError);

            value = null;
            _property.setValue(value);
            parserError = _property.validate();
            assertEquals("protein, cleavage C-terminal mass change, this value must be a real number(" + value + ").\n", parserError);
        }

        public void testValidateGarbage()
        {
            String value = "foo";
            _property.setValue(value);
            String parserError = _property.validate();
            assertEquals("protein, cleavage C-terminal mass change, this value must be a real number(" + value + ").\n", parserError);

            value = "- 1.2";
            _property.setValue(value);
            parserError = _property.validate();
            assertEquals("protein, cleavage C-terminal mass change, this value must be a real number(" + value + ").\n", parserError);
        }
    }
}
