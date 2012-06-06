/*
 * Copyright (c) 2007-2012 LabKey Corporation
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
 * Date: Jan 22, 2007
 * Time: 12:52:51 PM
 */
public class ListParamsValidator implements IParamsValidator
{
    private String[] list;

    public ListParamsValidator(String... list)
    {
        this.list = list;
    }

    public String validate(Param spp)
    {
        String parserError = "";
        if(list == null || list.length < 1)
        {
            parserError = "The list for the " + this.getClass().getName() + " has not been set.\n";
            return parserError;
        }
        String value = spp.getValue();
        if (value == null ||value.length()< 1)
        {
            parserError = spp.getInputXmlLabels().get(0) + ", " + "this value is not set.\n";
            return parserError;
        }
        boolean isValid = true;
        for(String listEntry:list)
        {
            if(listEntry.equals(value))
            {
                isValid = true;
                break;
            }
            else
            {
                isValid = false;
            }

        }
        if(!isValid) parserError = spp.getInputXmlLabels().get(0) + ", " + "this value ("
                + value + ") is not in the valid list.\n";
        return parserError;
    }

    //JUnit TestCase
    public static class TestCase extends Assert
    {
        private Param _property;

        @Before
        public void setUp() throws Exception
        {
                          _property = new SequestParam(
                          50,                                                       //sortOrder
                          "0",                                                    //The value of the property
                          "peptide_mass_units",                                 // the sequest.params property name
                          "0=amu, 1=mmu, 2=ppm",                                // the sequest.params comment
                           ConverterFactory.getSequestBasicConverter(),                             //converts the instance to a sequest.params line
                           ParamsValidatorFactory.getListParamsValidator("0","1","2"),
                           true                                                    //is pass through
                  ).setInputXmlLabels("spectrum, parent monoisotopic mass error units");
        }

        @After
        public void tearDown()
        {
            _property = null;
        }

        @Test
        public void testValidateNormal()
        {
            _property.setValue("0");
            String parserError = _property.validate();
            assertEquals("", parserError);
            assertEquals("0", _property.getValue());

            _property.setValue("1");
            parserError = _property.validate();
            assertEquals("", parserError);
            assertEquals("1", _property.getValue());

            _property.setValue("2");
            parserError = _property.validate();
            assertEquals("", parserError);
            assertEquals("2", _property.getValue());

        }

        @Test
        public void testValidateMissingValue()
        {
            _property.setValue("");
            String parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", " + "this value is not set.\n", parserError);

            _property.setValue(null);
            parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", " + "this value is not set.\n", parserError);
        }

        @Test
        public void testValidateGarbage()
        {
            _property.setValue("foo");
            String parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", " + "this value (foo) is not in the valid list.\n", parserError);

            _property.setValue("9");
            parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", " + "this value (9) is not in the valid list.\n", parserError);
        }
    }


    protected  void setList(String[] list) 
    {
        this.list = list;
    }
}
