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
 * Date: Oct 23, 2006
 * Time: 3:49:38 PM
 */
public class BooleanParamsValidator implements IParamsValidator
{
    public String validate(Param spp)
    {
        String parserError = "";
        String value = spp.getValue();
        if (value == null)
        {
            parserError = spp.getInputXmlLabels().get(0) + ", " + "this value must be a 1 or a 0(" + value + ").\n";
            return parserError;
        }
        if (!value.equals("1") && !value.equals("0"))
        {
            parserError = spp.getInputXmlLabels().get(0) + ", " + "this value must be a 1 or a 0(" + value + ").\n";
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
                110,                                                       //sortOrder
                "0",                                                      //The value of the property
                "show_fragment_ions",                                     // the sequest.params property name
                "0=no, 1=yes",                                            // the sequest.params comment
                new SequestBasicConverter(),                              //converts the instance to a sequest.params line
                new BooleanParamsValidator(),
                true);
            _property.setInputXmlLabels("sequest, show_fragment_ions");
        }

        protected void tearDown()
        {
            _property = null;
        }

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
        }

        public void testValidateMissingValue()
        {
            _property.setValue("");
            String parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a 1 or a 0().\n", parserError);

            _property.setValue(null);
            parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a 1 or a 0(null).\n", parserError);
        }

        public void testValidateGarbage()
        {
            _property.setValue("foo");
            String parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a 1 or a 0(foo).\n", parserError);

        }
    }
}
