/*
 * Copyright (c) 2012 LabKey Corporation
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
public class MultipleIntegerParamsValidator extends AbstractMultipleValueParamsValidator
{
    private final int _minValue;
    private final int _maxValue;

    public MultipleIntegerParamsValidator(int minValue, int maxValue, int valueCount)
    {
        super(valueCount);
        _minValue = minValue;
        _maxValue = maxValue;
    }

    protected String getValueDescription()
    {
        return "All values must be integers between " + _minValue + " and " + _maxValue + ", inclusive";
    }

    @Override
    protected String parseValue(String value, Param spp)
    {
        int i = Integer.parseInt(value);
        if (i < _minValue || i > _maxValue)
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
                100,                                                       //sortOrder
                "0 0 0 0 0 0",                                                      //The value of the property
                "diff_search_count",                                  // the sequest.params property name
                "max num of modified AA per each variable mod in a peptide", // the sequest.params comment
                ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
                new MultipleIntegerParamsValidator(0, 10, 6),
                true
            );
            _property.setInputXmlLabels("sequest, diff_search_count");
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
            assertTrue(parserError.contains("must have 6 values"));
        }

        @Test
        public void testValidateMissingValue()
        {
            _property.setValue("");
            String parserError = _property.validate();
            assertTrue(parserError.contains("must have 6 values, but had 1. All values must be integers between 0 and 10, inclusive"));
        }

        @Test
        public void testValidateNegative()
        {
            String value = "-4 -4 -4 -4 -4 -4";
            _property.setValue(value);
            String parserError = _property.validate();
            assertTrue(parserError.contains("is not between 0 and 10, inclusive"));
        }

        @Test
        public void testValidateGarbage()
        {
            _property.setValue("foo");
            String parserError = _property.validate();
            assertTrue(parserError.contains("must have 6 values, but had 1. All values must be integers between 0 and 10, inclusive"));
        }

        @Test
        public void testValidateGood()
        {
            _property.setValue("2 2 2 2 2 2");
            String parserError = _property.validate();
            assertEquals("", parserError);
        }
    }
}
