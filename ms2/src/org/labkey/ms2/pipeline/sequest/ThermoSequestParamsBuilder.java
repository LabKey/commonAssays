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

import org.junit.Test;
import org.labkey.ms2.pipeline.client.ParameterNames;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * User: jeckels
 * Date: Jul 27, 2012
 */
public class ThermoSequestParamsBuilder extends SequestParamsBuilder
{
    public ThermoSequestParamsBuilder(Map<String, String> sequestInputParams, File sequenceRoot)
    {
        super(sequestInputParams, sequenceRoot);
    }

    public ThermoSequestParamsBuilder(Map<String, String> sequestInputParams, File sequenceRoot, SequestParams.Variant variant, List<File> databaseFiles)
    {
        super(sequestInputParams, sequenceRoot, variant, databaseFiles);
    }

    protected void initSubclass()
    {
        _params.addProperty(new SequestParam(
            30,
            "",
            "second_database_name",
            "",
            ConverterFactory.getSequestBasicConverter(),
            null,
            false
        ));

        _params.addProperty(new SequestParam(
                  130,                                                       //sortOrder
                  "trypsin 1 1 KR P",                                                      //The value of the property
                  "enzyme_info",                                           // the sequest.params property name
                  "",                                                       // the input.xml label
                   ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
                   null,
                   false
        )).setInputXmlLabels(ParameterNames.ENZYME);

        _params.addProperty(new SequestParam(
          135,                                                       //sortOrder
          "4",                                                      //The value of the property
          "max_num_differential_AA_per_mod",                        // the sequest.params property name
          "max # of modified AA per diff",                        // the sequest.params comment
          ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
          null,
          false
      )).setInputXmlLabels("sequest, max_num_differential_AA_per_mod");

        //pass through- no Xtandem counterpart. The sequest params comment is  0=no, 1=yes but the bioworks default is 40.
        _params.addProperty(new SequestParam(
            120,                                                       //sortOrder
            "40",                                                      //The value of the property
            "print_duplicate_references",                                     // the sequest.params property name
            "",                                            // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
            ParamsValidatorFactory.getPositiveIntegerParamsValidator(),
            true
        )).setInputXmlLabels("sequest, print_duplicate_references");


        _params.addProperty(new SequestParam(
            160,                                                       //sortOrder
            "0.0 0.0",                                            //The value of the property
            "term_diff_search_options",                                // the sequest.params property name
            "",                                                       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
            null,
            false
         )).setInputXmlLabels(ParameterNames.DYNAMIC_MOD);

        _params.addProperty(new SequestParam(
            195,                                                       //sortOrder
            "1",                                            //The value of the property
            "use_mono/avg_masses",                                // the sequest.params property name
            "0=average masses, 1=monoisotopic masses",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            ParamsValidatorFactory.getBooleanParamsValidator(),
            true
        )).setInputXmlLabels(SequestSearchTask.MASS_TYPE_INDEX);

        _params.addProperty(new SequestParam(
            200,                                                       //sortOrder
            "0",                                            //The value of the property
            "normalize_xcorr",                                // the sequest.params property name
            "use normalized xcorr values in the out file",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            ParamsValidatorFactory.getBooleanParamsValidator(),
            true
        )).setInputXmlLabels("sequest, normalize_xcorr");

        _params.addProperty(new SequestParam(
            220,                                                       //sortOrder
            "0.0000",                                            //The value of the property
            "ion_cutoff_percentage",                                // the sequest.params property name
            "prelim. score cutoff % as a decimal number i.e. 0.30 for 30%",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            ParamsValidatorFactory.getPositiveDoubleParamsValidator(),
            true
        )).setInputXmlLabels("sequest, ion_cutoff_percentage");

        //not used in xtandem or Bioworks Browser. will leave at default setting.
        _params.addProperty(new SequestParam(
                        240,                                                       //sortOrder
                        "0 0",                                            //The value of the property
                        "protein_mass_filter",                                // the sequest.params property name
                        "enter protein mass min & max value ( 0 for both = unused)",       // the sequest.params comment
                        ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                        null,
                        false
                  )).setInputXmlLabels();

        _params.addProperty(new SequestParam(
                        250,                                                       //sortOrder
                        "0",                                            //The value of the property
                        "match_peak_count",                                // the sequest.params property name
                        "number of auto-detected peaks to try matching (max 5)",       // the sequest.params comment
                        ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                        null,
                        false
        )).setInputXmlLabels("sequest, match_peak_count");

        _params.addProperty(new SequestParam(
            260,                                                       //sortOrder
            "1",                                            //The value of the property
            "match_peak_allowed_error",                                // the sequest.params property name
            "number of allowed errors in matching auto-detected peaks",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            ParamsValidatorFactory.getPositiveIntegerParamsValidator(),
            true
        )).setInputXmlLabels("sequest, match_peak_allowed_error");

        _params.addProperty(new SequestParam(
            270,                                                       //sortOrder
            "1.0000",                                            //The value of the property
            "match_peak_tolerance",                                // the sequest.params property name
            "mass tolerance for matching auto-detected peaks",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            ParamsValidatorFactory.getPositiveDoubleParamsValidator(),
            true
        )).setInputXmlLabels("sequest, match_peak_tolerance");

        //Bioworks Browser doesn't use this; not making a input.xml tag
        _params.addProperty(new SequestParam(
            280,                                                       //sortOrder
            "",                                            //The value of the property
            "partial_sequence",                                // the sequest.params property name
            "",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        )).setInputXmlLabels();

        //needs to be yes
        _params.addProperty(new SequestParam(
            285,                                                       //sortOrder
            "1",                                            //The value of the property
            "create_output_files",                                // the sequest.params property name
            "0=no, 1=yes",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        )).setInputXmlLabels();

        //Bioworks Browser doesn't use this; not making a input.xml tag
        _params.addProperty(new SequestParam(
            290,                                                       //sortOrder
            "",                                            //The value of the property
            "sequence_header_filter",                                // the sequest.params property name
            "",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        )).setInputXmlLabels();



    }

    @Override
    protected List<String> initDynamicTermMods(char term, String mass)
    {
        Param termProp = _params.getParam("term_diff_search_options");
        String defaultTermMod = termProp.getValue();
        StringTokenizer st = new StringTokenizer(defaultTermMod);
        String defaultCTerm = st.nextToken();
        String defaultNTerm = st.nextToken();

        if (mass == null|| mass.length() == 0)
        {
            return Collections.singletonList("The mass value for term_diff_search_options is empty.");
        }
        if(term == '[') defaultNTerm = mass;
        else if(term == ']') defaultCTerm = mass;
        termProp.setValue(defaultCTerm + " " + defaultNTerm);
        return Collections.emptyList();
    }

    //JUnit TestCase
    public static class TestCase extends AbstractSequestTestCase
    {
        @Override
        public SequestParamsBuilder createParamsBuilder()
        {
            return new ThermoSequestParamsBuilder(ip.getInputParameters(), root);
        }

        @Test
        public void testInitDatabasesNormal() throws IOException
        {
            String value = "Bovine_mini1.fasta";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"pipeline, database\">" + value + "</note>" +
                "</bioml>");

            List<String> parserError = spb.initDatabases();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getFASTAParam();
            assertEquals(new File(dbPath + File.separator + value).getCanonicalPath(), new File(sp.getValue()).getCanonicalPath());
        }

        @Test
        public void testInitDatabasesMissingValue()
        {
            String value = "";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"pipeline, database\">" + value + "</note>" +
                "</bioml>");

            List<String> parserError = spb.initDatabases();
            if (parserError.isEmpty()) fail("Expected error.");
            assertEquals("pipeline, database; No value entered for database.", parserError.get(0));
        }

        @Test
        public void testInitDatabasesMissingInput()
        {
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

            List<String> parserError = spb.initDatabases();
            if (parserError.isEmpty()) fail("Expected error.");
            assertEquals("pipeline, database; No value entered for database.", parserError.get(0));
        }

        @Test
        public void testInitDatabasesGarbage()
        {
            String value = "garbage";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"pipeline, database\">" + value + "</note>" +
                "</bioml>");

            List<String> parserError = spb.initDatabases();
            if (parserError.isEmpty()) fail("Expected error.");
            assertTrue(parserError.get(0).contains("pipeline, database; The database does not exist"));
            assertTrue(parserError.get(0).contains("garbage"));

            value = "Bovine_mini1.fasta, garbage";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"pipeline, database\">" + value + "</note>" +
                "</bioml>");

            parserError = spb.initDatabases();
            if (parserError.isEmpty()) fail("Expected error.");
            assertTrue(parserError.get(0).contains("pipeline, database; The database does not exist"));
            assertTrue(parserError.get(0).contains("garbage"));

            value = "garbage, Bovine_mini1.fasta";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"pipeline, database\">" + value + "</note>" +
                "</bioml>");

            parserError = spb.initDatabases();
            if (parserError.isEmpty()) fail("Expected error.");
            assertTrue(parserError.get(0).contains("pipeline, database; The database does not exist"));
            assertTrue(parserError.get(0).contains("garbage"));
        }

        @Test
        public void testInitPeptideMassToleranceSymmetricWindow()
        {
            float expected = 30.0f;
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\">" + expected + "</note>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\">" + expected + "</note>" +
                "</bioml>");

            List<String> parserError = spb.initPeptideMassTolerance();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("peptide_mass_tolerance");
            float actual = Float.parseFloat(sp.getValue());
            assertEquals("peptide_mass_tolerance", expected, actual, 0.00);
        }

        @Test
        public void testInitPeptideMassToleranceSingleValue()
        {
            float expected = 30.0f;
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error\">" + expected + "</note>" +
                "</bioml>");

            List<String> parserError = spb.initPeptideMassTolerance();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("peptide_mass_tolerance");
            float actual = Float.parseFloat(sp.getValue());
            assertEquals("peptide_mass_tolerance", expected, actual, 0.00);
        }

        @Test
        public void testInitPeptideMassToleranceMissingValue()
        {
            String expected = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\"></note>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\">4.0</note>" +
                "</bioml>");

            List<String> parserError = spb.initPeptideMassTolerance();
            if (parserError.isEmpty()) fail("No error message.");
            String actual = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            assertEquals("peptide_mass_tolerance", expected, actual);
            assertEquals("Sequest does not support asymmetric parent error ranges (minus=4.0 plus=).", parserError.get(0));

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\">4.0</note>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\"></note>" +
                "</bioml>");
            parserError = spb.initPeptideMassTolerance();
            if (parserError.isEmpty()) fail("No error message.");
            actual = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            assertEquals("peptide_mass_tolerance", expected, actual);
            assertEquals("Sequest does not support asymmetric parent error ranges (minus= plus=4.0).", parserError.get(0));

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\"></note>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\"></note>" +
                "</bioml>");
            parserError = spb.initPeptideMassTolerance();
            if (parserError.isEmpty()) fail("No error message.");
            actual = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            assertEquals("peptide_mass_tolerance", expected, actual);
            assertEquals("No values were entered for spectrum, parent monoisotopic mass error minus/plus.", parserError.get(0));
        }

        @Test
        public void testInitPeptideMassToleranceNegative()
        {
            float expected = -30.0f;
            String defaultValue = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\">" + expected + "</note>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\">" + expected + "</note>" +
                "</bioml>");

            List<String> parserError = spb.initPeptideMassTolerance();
            Param sp = spb.getProperties().getParam("peptide_mass_tolerance");
            String actual = sp.getValue();
            assertEquals("parameter value changed", defaultValue, actual);
            assertEquals("Negative values not permitted for parent monoisotopic mass error(" + expected + ").", parserError.get(0));
        }

        @Test
        public void testInitPeptideMassToleranceInvalid()
        {
            String expected = "garbage";
            String defaultValue = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\">" + expected + "</note>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\">" + expected + "</note>" +
                "</bioml>");

            List<String> parserError = spb.initPeptideMassTolerance();
            Param sp = spb.getProperties().getParam("peptide_mass_tolerance");
            String actual = sp.getValue();
            assertEquals("parameter value changed", defaultValue, actual);
            assertEquals("Invalid value for value for  spectrum, parent monoisotopic mass error minus/plus (garbage).", parserError.get(0));
        }


        @Test
        public void testInitPeptideMassToleranceMissingInput()
        {
            String expected = spb.getProperties().getParam("peptide_mass_tolerance").getValue();

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error plus\">5.0</note>" +
                "</bioml>");
            List<String> parserError = spb.initPeptideMassTolerance();
            if (parserError.isEmpty()) fail("No error message.");
            String actual = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            assertEquals("peptide_mass_tolerance", expected, actual);
            assertEquals("Sequest does not support asymmetric parent error ranges (minus=null plus=5.0).", parserError.get(0));

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, parent monoisotopic mass error minus\">5.0</note>" +
                "</bioml>");
            parserError = spb.initPeptideMassTolerance();
            if (parserError.isEmpty()) fail("No error message.");
            actual = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            assertEquals("peptide_mass_tolerance", expected, actual);
            assertEquals("Sequest does not support asymmetric parent error ranges (minus=5.0 plus=null).", parserError.get(0));
        }

        @Test
        public void testInitPeptideMassToleranceDefault()
        {
            String expected = spb.getProperties().getParam("peptide_mass_tolerance").getValue();

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");
            List<String> parserError = spb.initPeptideMassTolerance();
            if (!parserError.isEmpty()) fail(parserError);
            String actual = spb.getProperties().getParam("peptide_mass_tolerance").getValue();
            assertEquals("peptide_mass_tolerance", expected, actual);
        }

        @Test
        public void testInitMassTypeNormal()
        {
            String expected = "AveRage";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, fragment mass type\">" + expected + "</note>" +
                "</bioml>");

            List<String> parserError = spb.initMassType();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("mass_type_fragment");
            String actual = sp.getValue();
            assertEquals("mass_type_fragment", "0", actual);

            expected = "MonoIsotopic";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, fragment mass type\">" + expected + "</note>" +
                "</bioml>");

            parserError = spb.initMassType();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("mass_type_fragment");
            actual = sp.getValue();
            assertEquals("mass_type_fragment", "1", actual);
        }

        @Test
        public void testInitMassTypeMissingValue()
        {
            String expected = "1";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, fragment mass type\"></note>" +
                "</bioml>");

            List<String> parserError = spb.initMassType();
            if (parserError.isEmpty()) fail("No error message.");
            Param sp = spb.getProperties().getParam("mass_type_fragment");
            String actual = sp.getValue();
            assertEquals("mass_type_fragment", expected, actual);
            assertEquals("mass_type_fragment", "\"spectrum, fragment mass type\" contains no value.", parserError.get(0));
        }

        @Test
        public void testInitMassTypeDefault()
        {
            String expected = "1";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

            List<String> parserError = spb.initMassType();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("mass_type_fragment");
            String actual = sp.getValue();
            assertEquals("mass_type_fragment", expected, actual);
        }

        @Test
        public void testInitMassTypeGarbage()
        {
            String expected = "1";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"spectrum, fragment mass type\">garbage</note>" +
                "</bioml>");

            List<String> parserError = spb.initMassType();
            if (parserError.isEmpty()) fail("No error message.");
            Param sp = spb.getProperties().getParam("mass_type_fragment");
            String actual = sp.getValue();
            assertEquals("mass_type_fragment", expected, actual);
            assertEquals("mass_type_fragment", "\"spectrum, fragment mass type\" contains an invalid value(garbage).", parserError.get(0));
        }

        @Test
        public void testInitIonScoringNormal()
        {
            String expected = "0 0 0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"sequest, a neutral loss\">no</note>" +
                "<note type=\"input\" label=\"sequest, b neutral loss\">No</note>" +
                "<note type=\"input\" label=\"sequest, y neutral loss\">NO</note>" +
                "<note type=\"input\" label=\"scoring, a ions\">nO</note>" +
                "<note type=\"input\" label=\"scoring, b ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, c ions\">NO</note>" +
                "<note type=\"input\" label=\"sequest, d ions\">No</note>" +
                "<note type=\"input\" label=\"sequest, v ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, w ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, x ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, y ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, z ions\">no</note>" +
                "</bioml>");

            List<String> parserError = spb.initIonScoring();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("ion_series");
            String actual = sp.getValue();
            assertEquals("ion_series", expected, actual);

            expected = "1 1 1 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"sequest, a neutral loss\">Yes</note>" +
                "<note type=\"input\" label=\"sequest, b neutral loss\">yEs</note>" +
                "<note type=\"input\" label=\"sequest, y neutral loss\">yeS</note>" +
                "<note type=\"input\" label=\"scoring, a ions\">YEs</note>" +
                "<note type=\"input\" label=\"scoring, b ions\">yES</note>" +
                "<note type=\"input\" label=\"scoring, c ions\">YeS</note>" +
                "<note type=\"input\" label=\"sequest, d ions\">YES</note>" +
                "<note type=\"input\" label=\"sequest, v ions\">yes</note>" +
                "<note type=\"input\" label=\"sequest, w ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, x ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, y ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, z ions\">yes</note>" +
                "</bioml>");

            parserError = spb.initIonScoring();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("ion_series");
            actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
        }

        @Test
        public void testInitIonScoringMissingValue()
        {
            String expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"sequest, a neutral loss\">no</note>" +
                "<note type=\"input\" label=\"sequest, b neutral loss\">No</note>" +
                "<note type=\"input\" label=\"sequest, y neutral loss\"></note>" +
                "<note type=\"input\" label=\"scoring, a ions\">nO</note>" +
                "<note type=\"input\" label=\"scoring, b ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, c ions\">NO</note>" +
                "<note type=\"input\" label=\"sequest, d ions\">No</note>" +
                "<note type=\"input\" label=\"sequest, v ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, w ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, x ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, y ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, z ions\">no</note>" +
                "</bioml>");

            List<String> parserError = spb.initIonScoring();
            if (parserError.isEmpty()) fail("Expected error");
            Param sp = spb.getProperties().getParam("ion_series");
            String actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
            assertEquals("ion_series", "sequest, y neutral loss did not contain a value.", parserError.get(0));

            expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"sequest, a neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"sequest, b neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"sequest, y neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"scoring, a ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, b ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, c ions\"></note>" +
                "<note type=\"input\" label=\"sequest, d ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, v ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, w ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, x ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, y ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, z ions\">no</note>" +
                "</bioml>");

            parserError = spb.initIonScoring();
            if (parserError.isEmpty()) fail("Expected error");
            sp = spb.getProperties().getParam("ion_series");
            actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
            assertEquals("ion_series", "scoring, c ions did not contain a value.", parserError.get(0));

            expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"sequest, a neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"sequest, b neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"sequest, y neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"scoring, a ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, b ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, c ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, d ions\"></note>" +
                "<note type=\"input\" label=\"sequest, v ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, w ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, x ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, y ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, z ions\">no</note>" +
                "</bioml>");

            parserError = spb.initIonScoring();
            if (parserError.isEmpty()) fail("Expected error");
            sp = spb.getProperties().getParam("ion_series");
            actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
            assertEquals("ion_series", "sequest, d ions did not contain a value.", parserError.get(0));
        }

        @Test
        public void testInitIonScoringMissingDefault()
        {
            String expected = "0 0 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"sequest, a neutral loss\">no</note>" +
                "<note type=\"input\" label=\"sequest, b neutral loss\">No</note>" +
                "<note type=\"input\" label=\"scoring, a ions\">nO</note>" +
                "<note type=\"input\" label=\"scoring, c ions\">NO</note>" +
                "<note type=\"input\" label=\"sequest, d ions\">No</note>" +
                "<note type=\"input\" label=\"sequest, v ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, x ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, y ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, z ions\">no</note>" +
                "</bioml>");

            List<String> parserError = spb.initIonScoring();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("ion_series");
            String actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
        }

        @Test
        public void testInitIonScoringDefault()
        {
            String expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

            List<String> parserError = spb.initIonScoring();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("ion_series");
            String actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
        }

        @Test
        public void testInitIonScoringGarbage()
        {
            String expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"sequest, a neutral loss\">no</note>" +
                "<note type=\"input\" label=\"sequest, b neutral loss\">No</note>" +
                "<note type=\"input\" label=\"sequest, y neutral loss\">garbage</note>" +
                "<note type=\"input\" label=\"scoring, a ions\">nO</note>" +
                "<note type=\"input\" label=\"scoring, b ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, c ions\">NO</note>" +
                "<note type=\"input\" label=\"sequest, d ions\">No</note>" +
                "<note type=\"input\" label=\"sequest, v ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, w ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, x ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, y ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, z ions\">no</note>" +
                "</bioml>");

            List<String> parserError = spb.initIonScoring();
            if (parserError.isEmpty()) fail("Expected error");
            Param sp = spb.getProperties().getParam("ion_series");
            String actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
            assertEquals("ion_series", "sequest, y neutral loss contained an invalid value(garbage).", parserError.get(0));

            expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"sequest, a neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"sequest, b neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"sequest, y neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"scoring, a ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, b ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, c ions\">garbage</note>" +
                "<note type=\"input\" label=\"sequest, d ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, v ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, w ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, x ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, y ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, z ions\">no</note>" +
                "</bioml>");

            parserError = spb.initIonScoring();
            if (parserError.isEmpty()) fail("Expected error");
            sp = spb.getProperties().getParam("ion_series");
            actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
            assertEquals("ion_series", "scoring, c ions contained an invalid value(garbage).", parserError.get(0));

            expected = "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"sequest, a neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"sequest, b neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"sequest, y neutral loss\">yes</note>" +
                "<note type=\"input\" label=\"scoring, a ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, b ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, c ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, d ions\">garbage</note>" +
                "<note type=\"input\" label=\"sequest, v ions\">no</note>" +
                "<note type=\"input\" label=\"sequest, w ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, x ions\">no</note>" +
                "<note type=\"input\" label=\"scoring, y ions\">yes</note>" +
                "<note type=\"input\" label=\"scoring, z ions\">no</note>" +
                "</bioml>");

            parserError = spb.initIonScoring();
            if (parserError.isEmpty()) fail("Expected error");
            sp = spb.getProperties().getParam("ion_series");
            actual = sp.getValue();
            assertEquals("ion_series", expected, actual);
            assertEquals("ion_series", "sequest, d ions contained an invalid value(garbage).", parserError.get(0));
        }


        @Test
        public void testInitEnzymeInfoNormal()
        {
            //Testing no enzyme
            String expected2 = "nonspecific 0 0 - -";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[X]|[X]</note>" +
                "</bioml>");

            List<String> parserError = spb.initEnzymeInfo();
            if (!parserError.isEmpty()) fail(parserError);

            Param sp = spb.getProperties().getParam("enzyme_info");
            String actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);

            expected2 = "trypsin_k 1 1 K P";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[K]|{P}</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (!parserError.isEmpty()) fail(parserError);

            sp = spb.getProperties().getParam("enzyme_info");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);

            expected2 = "pepsina 1 1 FL -";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[LF]|[X]</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (!parserError.isEmpty()) fail(parserError);

            sp = spb.getProperties().getParam("enzyme_info");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
        }

        @Test
        public void testInitEnzymeInfoDefault()
        {
//            String expected2 = "Trypsin(KR/P)\t\t\t\t1\tKR\t\tP";
            String expected2 = "trypsin 1 1 KR P";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

           List<String> parserError = spb.initEnzymeInfo();
           if (!parserError.isEmpty()) fail(parserError);
//            Param sp = spb.getProperties().getParam("enzyme_number");
//            String actual = sp.getValue();
//            assertEquals("enzyme_number", expected1, actual);
//
//            sp = spb.getProperties().getParam("enzyme1");
//            actual = sp.getValue();
//            assertEquals("enzyme_description", expected2, actual);

            Param sp = spb.getProperties().getParam("enzyme_info");
            String actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
        }

        @Test
        public void testInitEnzymeInfoMissingValue()
        {
            String expected2 = "trypsin 1 1 KR P";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\"></note>" +
                "</bioml>");

            List<String> parserError = spb.initEnzymeInfo();
            if (parserError.isEmpty()) fail("Expected error message");

            Param sp = spb.getProperties().getParam("enzyme_info");
            String actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("enzyme_description", "protein, cleavage site did not contain a value.", parserError.get(0));
        }

        @Test
        public void testInitEnzymeInfoGarbage()
        {
            String expected2 = "trypsin 1 1 KR P";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">foo</note>" +
                "</bioml>");

            List<String> parserError = spb.initEnzymeInfo();
            if (parserError.isEmpty()) fail("Expected error message");


            Param sp = spb.getProperties().getParam("enzyme_info");
            String actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("enzyme_description", "Invalid enzyme definition:foo", parserError.get(0));

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[CV]|{P},[KR]|{P}</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (parserError.isEmpty()) fail("Expected error message");

            sp = spb.getProperties().getParam("enzyme_info");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("[CV]|{P},[KR]|{P} is not a pipeline supported enzyme.", parserError.get(0));

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">{P}|[KR]</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();

            sp = spb.getProperties().getParam("enzyme_info");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("{P}|[KR] is not a pipeline supported enzyme.", parserError.get(0));

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[a]|[X]</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (parserError.isEmpty()) fail("Expected error message");

            sp = spb.getProperties().getParam("enzyme_info");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("[a]|[X] is not a pipeline supported enzyme.", parserError.get(0));

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[X]|[a]</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (parserError.isEmpty()) fail("Expected error message");

            sp = spb.getProperties().getParam("enzyme_info");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("[X]|[a] is not a pipeline supported enzyme.", parserError.get(0));

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"protein, cleavage site\">[X]|P</note>" +
                "</bioml>");

            parserError = spb.initEnzymeInfo();
            if (parserError.isEmpty()) fail("Expected error message");

            sp = spb.getProperties().getParam("enzyme_info");
            actual = sp.getValue();
            assertEquals("enzyme_description", expected2, actual);
            assertEquals("Invalid enzyme definition:[X]|P", parserError.get(0));
        }

        @Test
        public void testInitDynamicModsNormal()
        {
            String expected1 = "16.0 M 0.000000 S 0.000000 C 0.000000 X 0.000000 T 0.000000 Y";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">+16@M</note>" +
                "</bioml>");

            List<String> parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("diff_search_options");
            String actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);

            expected1 = "16.0 M 0.000000 S 0.000000 C 0.000000 X 0.000000 T 0.000000 Y";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">16@M</note>" +
                "</bioml>");

            parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("diff_search_options");
            actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);

            expected1 = "-16.0 M 0.000000 S 0.000000 C 0.000000 X 0.000000 T 0.000000 Y";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">- 16.0000 @ M</note>" +
                "</bioml>");

            parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("diff_search_options");
            actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);


            expected1 = "16.0 M 9.0 C 0.000000 S 0.000000 X 0.000000 T 0.000000 Y";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\"> 16@M,9@C </note>" +
                "</bioml>");

            parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("diff_search_options");
            actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);
        }

        @Test
        public void testInitDynamicModsMissingValue()
        {
            String expected1 = "0.000000 C 0.000000 M 0.000000 S 0.000000 T 0.000000 X 0.000000 Y";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\"></note>" +
                "</bioml>");

            List<String> parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("diff_search_options");
            String actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);
        }

        @Test
        public void testInitDynamicModsDefault()
        {
            String expected1 = "0.000000 C 0.000000 M 0.000000 S 0.000000 T 0.000000 X 0.000000 Y";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

            List<String> parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("diff_search_options");
            String actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);
        }

        @Test
        public void testInitDynamicModsGarbage()
        {
            String expected1 = "0.000000 C 0.000000 M 0.000000 S 0.000000 T 0.000000 X 0.000000 Y";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">16@J</note>" +
                "</bioml>");

            List<String> parserError = spb.initDynamicMods();
            if (parserError.isEmpty()) fail("Error expected.");
            Param sp = spb.getProperties().getParam("diff_search_options");
            String actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);
            assertEquals("diff_search_options", "modification mass contained an invalid residue(J).", parserError.get(0));

            expected1 = "0.000000 C 0.000000 M 0.000000 S 0.000000 T 0.000000 X 0.000000 Y";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">G@18</note>" +
                "</bioml>");

            parserError = spb.initDynamicMods();
            if (parserError.isEmpty()) fail("Error expected.");
            sp = spb.getProperties().getParam("diff_search_options");
            actual = sp.getValue();
            assertEquals("diff_search_options", expected1, actual);
            assertEquals("diff_search_options", "modification mass contained an invalid value(G@18).", parserError.get(0));

        }

        @Test
        public void testInitTermDynamicModsNormal()
        {
            Param sp = spb.getProperties().getParam("term_diff_search_options");
            String defaultValue = sp.getValue();
            String expected1 = "0.0 42.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">+42.0@[</note>" +
                "</bioml>");

            List<String> parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("term_diff_search_options");
            String actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);

            sp.setValue(defaultValue);
            expected1 = "-88 42.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">+ 42.0 @[,-88 @ ]</note>" +
                "</bioml>");

            parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("term_diff_search_options");
            actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);

            sp.setValue(defaultValue);
            expected1 = "-88 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">-88@]</note>" +
                "</bioml>");

            parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("term_diff_search_options");
            actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);

        }

        @Test
        public void testInitTermDynamicModsMissingValue()
        {
            Param sp = spb.getProperties().getParam("term_diff_search_options");
            String defaultValue = sp.getValue();
            String expected1 = "0.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">+0.0@[</note>" +
                "</bioml>");

            List<String> parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("term_diff_search_options");
            String actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);

            sp.setValue(defaultValue);
            expected1 = "0.0 42.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, potential modification mass\">+ 42.0 @[</note>" +
                "</bioml>");

            parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().getParam("term_diff_search_options");
            actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);
        }

        @Test
        public void testInitTermDynamicModsDefault()
        {
            String expected1 = "0.0 0.0";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

            List<String> parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().getParam("term_diff_search_options");
            String actual = sp.getValue();
            assertEquals("term_diff_search_options", expected1, actual);
        }

        @Test
        public void testInitStaticModsNormal()
        {
            char[] validResidues = spb.getValidResidues();
            for (char residue : validResidues)
            {
                String expected1 = "227";
                parseParams("<?xml version=\"1.0\"?>" +
                    "<bioml>" +
                    "<note type=\"input\" label=\"residue, modification mass\">+227@" + residue + "</note>" +
                    "</bioml>");

                List<String> parserError = spb.initStaticMods();
                if (!parserError.isEmpty()) fail(parserError);
                Param sp;
                if(residue == '[')
                {
                    sp = spb.getProperties().startsWith("add_Nterm_peptide");
                }
                else if(residue == ']')
                {
                    sp = spb.getProperties().startsWith("add_Cterm_peptide");
                }
                else
                {
                    sp = spb.getProperties().startsWith("add_" + residue + "_");
                }
                String actual = sp.getValue();
                assertEquals(ParameterNames.STATIC_MOD, expected1, actual);
            }

            for (char residue : validResidues)
            {
                String expected1 = "-9";
                parseParams("<?xml version=\"1.0\"?>" +
                    "<bioml>" +
                    "<note type=\"input\" label=\"residue, modification mass\">- 9 @ " + residue + "</note>" +
                    "</bioml>");


                List<String> parserError = spb.initStaticMods();
                if (!parserError.isEmpty()) fail(parserError);
                Param sp;
                if(residue == '[')
                {
                    sp = spb.getProperties().startsWith("add_Nterm_peptide");
                }
                else if(residue == ']')
                {
                    sp = spb.getProperties().startsWith("add_Cterm_peptide");
                }
                else
                {
                    sp = spb.getProperties().startsWith("add_" + residue + "_");
                }
                String actual = sp.getValue();
                assertEquals(ParameterNames.STATIC_MOD, expected1, actual);
            }

            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, modification mass\">227@C,16@M</note>" +
                "</bioml>");

            String expected1 = "16";
            List<String> parserError = spb.initStaticMods();
            if (!parserError.isEmpty()) fail(parserError);
            Param sp = spb.getProperties().startsWith("add_M_");
            String actual = sp.getValue();
            assertEquals(ParameterNames.STATIC_MOD, expected1, actual);

            expected1 = "227";
            parserError = spb.initStaticMods();
            if (!parserError.isEmpty()) fail(parserError);
            sp = spb.getProperties().startsWith("add_C_");
            actual = sp.getValue();
            assertEquals(ParameterNames.STATIC_MOD, expected1, actual);
        }


        @Test
        public void testInitStaticModsMissingValue()
        {
            char[] validResidues = spb.getValidResidues();
            for (char residue : validResidues)
            {
                String expected1 = "0.0";
                parseParams("<?xml version=\"1.0\"?>" +
                    "<bioml>" +
                    "<note type=\"input\" label=\"residue, modification mass\"></note>" +
                    "</bioml>");


                List<String> parserError = spb.initStaticMods();
                if (!parserError.isEmpty()) fail(parserError);
                Param sp;
                if(residue == '[')
                {
                    sp = spb.getProperties().startsWith("add_Nterm_peptide");
                }
                else if(residue == ']')
                {
                    sp = spb.getProperties().startsWith("add_Cterm_peptide");
                }
                else
                {
                    sp = spb.getProperties().startsWith("add_" + residue + "_");
                }
                String actual = sp.getValue();
                assertEquals(ParameterNames.STATIC_MOD, expected1, actual);
            }
        }

        @Test
        public void testInitStaticModsDefault()
        {
            char[] validResidues = spb.getValidResidues();
            for (char residue : validResidues)
            {
                String expected1 = "0.0";
                parseParams("<?xml version=\"1.0\"?>" +
                    "<bioml>" +
                    "</bioml>");

                List<String> parserError = spb.initStaticMods();
                if (!parserError.isEmpty()) fail(parserError);
                Param sp;
                if(residue == '[')
                {
                    sp = spb.getProperties().startsWith("add_Nterm_peptide");
                }
                else if(residue == ']')
                {
                    sp = spb.getProperties().startsWith("add_Cterm_peptide");
                }
                else
                {
                    sp = spb.getProperties().startsWith("add_" + residue + "_");
                }
                String actual = sp.getValue();
                assertEquals(ParameterNames.STATIC_MOD, expected1, actual);
            }
        }

        @Test
        public void testInitStaticModsGarbage()
        {
            String value = "garbage";
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "<note type=\"input\" label=\"residue, modification mass\">" + value + "</note>" +
                "</bioml>");

            List<String> parserError = spb.initStaticMods();
            if (parserError.isEmpty()) fail("Expected error.");
            assertEquals("modification mass contained an invalid value(" + value + ").", parserError.get(0));
        }

        @Test
        public void testInitPassThroughsNormal()
        {
            Collection<SequestParam> passThroughs = spb.getProperties().getPassThroughs();
            for (SequestParam passThrough : passThroughs)
            {
                if (passThrough.getValidator() == null)
                {
                    fail("null validator class.");
                }
                else if (passThrough.getValidator().getClass() == NaturalNumberParamsValidator.class)
                {
                    passThrough.setValue("1");
                    assertEquals("", passThrough.validate());
                    passThrough.setValue("22");
                    assertEquals("", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == PositiveDoubleParamsValidator.class)
                {
                    passThrough.setValue("0");
                    assertEquals("", passThrough.validate());
                    passThrough.setValue("2.2");
                    assertEquals("", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == BooleanParamsValidator.class)
                {
                    passThrough.setValue("0");
                    assertEquals("", passThrough.validate());
                    passThrough.setValue("1");
                    assertEquals("", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == RealNumberParamsValidator.class)
                {
                    passThrough.setValue("0");
                    assertEquals("", passThrough.validate());
                    passThrough.setValue("1.4");
                    assertEquals("", passThrough.validate());
                    passThrough.setValue("-2");
                    assertEquals("", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == NonNegativeIntegerParamsValidator.class)
                {
                    passThrough.setValue("0");
                    assertEquals("", passThrough.validate());

                    passThrough.setValue("2");
                    assertEquals("", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == ListParamsValidator.class)
                {
                    ((ListParamsValidator)passThrough.getValidator()).setList(new String[]{"0","1","2"});
                    passThrough.setValue("2");
                    assertEquals("", passThrough.validate());
                }
                else
                {
                    fail("Unknown validator class.");
                }
            }
        }

        @Test
        public void testInitPassThroughsMissingValue()
        {
            Collection<SequestParam> passThroughs = spb.getProperties().getPassThroughs();
            for (SequestParam passThrough : passThroughs)
            {
                if (passThrough.getValidator() == null)
                {
                    fail("null validator class.");
                }
                else if (passThrough.getValidator().getClass() == NaturalNumberParamsValidator.class)
                {
                    String value = "";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a natural number(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == PositiveDoubleParamsValidator.class)
                {
                    String value = "";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a positive number(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == BooleanParamsValidator.class)
                {
                    String value = "";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a 1 or a 0(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == RealNumberParamsValidator.class)
                {
                    String value = "";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a real number(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == NonNegativeIntegerParamsValidator.class)
                {
                    String value = "";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a non-negative integer(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == ListParamsValidator.class)
                {
                    String listValue = "";
                    passThrough.setValue(listValue);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", " + "this value is not set.\n", passThrough.validate());
                }
                else
                {
                    fail("Unknown validator class.");
                }
            }
        }

        @Test
        public void testInitPassThroughsNegative()
        {
            Collection<SequestParam> passThroughs = spb.getProperties().getPassThroughs();
            for (SequestParam passThrough : passThroughs)
            {
                if (passThrough.getValidator() == null)
                {
                    fail("null validator class.");
                }
                else if (passThrough.getValidator().getClass() == NaturalNumberParamsValidator.class)
                {
                    String value = "-3";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a natural number(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == PositiveDoubleParamsValidator.class)
                {
                    String value = "-3.4";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a positive number(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == NonNegativeIntegerParamsValidator.class)
                {
                    String value = "-3";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a non-negative integer(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == BooleanParamsValidator.class)
                {
                    String value = "-1";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a 1 or a 0(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == RealNumberParamsValidator.class)
                {
                    String value = "-1.3";
                    passThrough.setValue(value);
                    assertEquals("", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == ListParamsValidator.class)
                {
                    ((ListParamsValidator)passThrough.getValidator()).setList(new String[]{"a","b","c"});
                    passThrough.setValue("-1");
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", " + "this value (-1) is not in the valid list.\n", passThrough.validate());
                }
                else
                {
                    fail("Unknown validator class.");
                }
            }

        }

        @Test
        public void testInitPassThroughsGarbage()
        {
            Collection<SequestParam> passThroughs = spb.getProperties().getPassThroughs();
            for (SequestParam passThrough : passThroughs)
            {
                if (passThrough.getValidator() == null)
                {
                    fail("null validator class.");
                }
                else if (passThrough.getValidator().getClass() == NaturalNumberParamsValidator.class)
                {
                    String value = "foo";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a natural number(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == PositiveDoubleParamsValidator.class)
                {
                    String value = "bar";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a positive number(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == BooleanParamsValidator.class)
                {
                    String value = "true";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a 1 or a 0(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == RealNumberParamsValidator.class)
                {
                    String value = "blue";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a real number(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == NonNegativeIntegerParamsValidator.class)
                {
                    String value = "blue";
                    passThrough.setValue(value);
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", this value must be a non-negative integer(" + value + ").\n", passThrough.validate());
                }
                else if (passThrough.getValidator().getClass() == ListParamsValidator.class)
                {
                    ((ListParamsValidator)passThrough.getValidator()).setList(new String[]{"a","b","c"});
                    passThrough.setValue("foo");
                    assertEquals(passThrough.getInputXmlLabels().get(0) + ", " + "this value (foo) is not in the valid list.\n", passThrough.validate());
                }
                else
                {
                    fail("Unknown validator class.");
                }
            }
        }

        @Test
        public void testGenerateFile() throws SequestParamsException
        {
            Map<String, String> paramMap = new HashMap<>();

            paramMap.put(ParameterNames.STATIC_MOD, "50.43@[,90.12@]");
            paramMap.put(ParameterNames.SEQUENCE_DB, DUMMY_FASTA_NAME);
            // Value from UW version - make sure it doesn't get piped through
            paramMap.put("sequest, digest_mass_range", "400.0 5900.0");
            ThermoSequestParamsBuilder spb = new ThermoSequestParamsBuilder(paramMap, new File("fakeroot"));
            spb.initXmlValues();
            String text = spb.getSequestParamsText();
            assertTrue(text.contains("database_name ="));
            assertTrue(text.contains("max_num_differential_AA_per_mod ="));

            // Value from UW version - make sure it doesn't get piped through
            assertFalse(text.contains("num_threads = 0"));
            assertFalse(text.contains("digest_mass_range = 400.0 5900.0"));
        }
    }
}
