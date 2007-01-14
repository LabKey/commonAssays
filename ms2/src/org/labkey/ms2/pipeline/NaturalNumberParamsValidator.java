package org.labkey.ms2.pipeline;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.StringTokenizer;

/**
 * User: billnelson@uky.edu
 * Date: Oct 23, 2006
 * Time: 4:55:01 PM
 */
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
            suite.addTest(new TestCase("testValidateNegative"));
            suite.addTest(new TestCase("testValidateGarbage"));

            return suite;
        }

        protected void setUp() throws Exception
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

        protected void tearDown()
        {
            _property = null;
        }

        public void testValidateNormal()
        {
            _property.setValue("1");
            String parserError = _property.validate();
            assertEquals("", parserError);

            _property.setValue("1,2,3");
            parserError = _property.validate();
            assertEquals("", parserError);

        }

        public void testValidateMissingValue()
        {
            _property.setValue("");
            String parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a natural number().\n", parserError);

            _property.setValue(null);
            parserError = _property.validate();
            assertEquals(_property.getInputXmlLabels().get(0) + ", this value must be a natural number(null).\n", parserError);
        }

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
