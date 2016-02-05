/*
 * Copyright (c) 2012-2016 LabKey Corporation
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
 * User: jeckels
 * Date: Jul 31, 2012
 */
public class MultipleDoubleParamsValidator extends AbstractMultipleValueParamsValidator
{
    private final double _minValue;
    private final double _maxValue;

    public MultipleDoubleParamsValidator(double minValue, double maxValue, int valueCount)
    {
        super(valueCount);
        _minValue = minValue;
        _maxValue = maxValue;
    }

    @Override
    protected String getValueDescription()
    {
        return "All values must be numbers between " + _minValue + " and " + _maxValue + ", inclusive";
    }

    @Override
    protected String parseValue(String value, Param spp)
    {
        double d = Double.parseDouble(value);
        if (d < _minValue || d > _maxValue)
        {
            return "Value \"" + value + "\" for parameter \"" + spp.getInputXmlLabels().get(0) + "\" is not between " + _minValue + " and " + _maxValue + ", inclusive.";
        }
        return null;
    }

    //JUnit TestCase
    public static class TestCase extends Assert
    {
        private SequestParam _property;

        @Before
        public void setUp() throws Exception
        {
            _property = new SequestParam(
                      91,                                                       //sortOrder
                      "600.0 5000.0",                                                      //The value of the property
                      "digest_mass_range",                                           // the sequest.params property name
                      "MH+ peptide mass range to analyze",                                                       // the input.xml label
                       ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
                       new MultipleDoubleParamsValidator(0, 1000000, 2),
                       true
            );

            _property.setInputXmlLabels("sequest, digest_mass_range");
        }

        @After
        public void tearDown()
        {
            _property = null;
        }

        @Test
        public void testValidateTooFewValues()
        {
            _property.setValue("1");
            String parserError = _property.validate();
            assertTrue(parserError.contains("must have 2 values"));
        }

        @Test
        public void testValidateMissingValue()
        {
            _property.setValue("");
            String parserError = _property.validate();
            assertTrue(parserError.contains("must have 2 values, but had 1. All values must be numbers between 0.0 and 1000000.0, inclusive"));
        }

        @Test
        public void testValidateNegative()
        {
            String value = "-4 -4";
            _property.setValue(value);
            String parserError = _property.validate();
            assertTrue(parserError.contains("is not between 0.0 and 1000000.0, inclusive"));
        }

        @Test
        public void testBadDelimiter()
        {
            String value = "400.0, 6000.0";
            _property.setValue(value);
            String parserError = _property.validate();
            assertTrue(parserError.contains("Could not parse value \"400.0,\""));
        }

        @Test
        public void testValidateGarbage()
        {
            _property.setValue("foo");
            String parserError = _property.validate();
            assertTrue(parserError.contains("must have 2 values, but had 1. All values must be numbers between 0.0 and 1000000.0, inclusive"));
        }

        @Test
        public void testValidateGood()
        {
            _property.setValue("200.2 2000.3432");
            String parserError = _property.validate();
            assertEquals("", parserError);
        }
    }

}
