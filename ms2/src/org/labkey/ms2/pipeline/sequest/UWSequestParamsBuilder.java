/*
 * Copyright (c) 2011-2012 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.settings.AppProps;
import org.labkey.ms2.pipeline.client.ParameterNames;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Jun 16, 2011
 */
public class UWSequestParamsBuilder extends SequestParamsBuilder
{
    private static final Map<String, Integer> UW_SEQUEST_ENZYME_MAP;

    static
    {
        Map<String, Integer> m = new CaseInsensitiveHashMap<Integer>();

        m.put("[X]|[X]", 0);        // None
        m.put("[KR]|{P}", 1);       // Trypsin
        m.put("[FMWY]|{P}", 2);     // Chymotrypsin
        m.put("[R]|[X]", 3);        // Clostripain
        m.put("[M]|{P}", 4);        // Cyanogen_Bromide
        m.put("[W]|[X]", 5);        // IodosoBenzoate
        m.put("[P]|[X]", 6);        // Proline_Endopept
        m.put("[E]|[X]", 7);        // Staph_Protease
        m.put("[K]|{P}", 8);        // Trypsin_K
        m.put("[R]|{P}", 8);        // Trypsin_R
        m.put("[X]|[D]", 10);       // AspN
//        "11. Cymotryp/Modified      1      FWYL        P\n" +
        m.put("[AGILV]|{P}", 12);
//        "13. Elastase/Tryp/Chymo    1      ALIVKRWFY   P\n";

        UW_SEQUEST_ENZYME_MAP = Collections.unmodifiableMap(m);
    }

    public UWSequestParamsBuilder(Map<String, String> sequestInputParams, File sequenceRoot)
    {
        super(sequestInputParams, sequenceRoot, SequestParams.Variant.uwsequest, null);
        init();
    }

    public UWSequestParamsBuilder(Map<String, String> sequestInputParams, File sequenceRoot, SequestParams.Variant variant, List<File> databaseFiles)
    {
        super(sequestInputParams, sequenceRoot, variant, databaseFiles);
        init();
    }

    private void init()
    {
        _params.addProperty(new SequestParam(
                  131,                                                       //sortOrder
                  "1",                                                      //The value of the property
                  "enzyme_number",                                           // the sequest.params property name
                  "",                                                       // the input.xml label
                   ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
                   null,
                   false
        )).setInputXmlLabels(ParameterNames.ENZYME);

        _params.addProperty(new SequestParam(
                        251,                                                       //sortOrder
                        "5",                                            //The value of the property
                        "min_peaks",                                // the sequest.params property name
                        "Only search spectra with at least this many peaks. Default 5.",       // the sequest.params comment
                        ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                        new NaturalNumberParamsValidator(),
                        true
        )).setInputXmlLabels("sequest, min_peaks" );

        _params.addProperty(new SequestParam(
                        252,                                                       //sortOrder
                        "2",                                            //The value of the property
                        "num_enzyme_termini",                                // the sequest.params property name
                        "Generate peptides with enzyme digestion sites at one or both termini. Default 2.",       // the sequest.params comment
                        ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                        new NonNegativeIntegerParamsValidator(),
                        true
        )).setInputXmlLabels("sequest, num_enzyme_termini" );

        _params.addProperty(new SequestParam(
                        253,                                                       //sortOrder
                        "0",                                            //The value of the property
                        "isotope_error",                                // the sequest.params property name
                        "When true, search multiple windows around -1, 0, +1, +2, +3 (da?) of parrent mass. Default 0 (false).",       // the sequest.params comment
                        ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                        new BooleanParamsValidator(),
                        true
        )).setInputXmlLabels("sequest, isotope_error" );

        _params.addProperty(new SequestParam(
                        254,                                                       //sortOrder
                        "Da",                                            //The value of the property
                        "mass_tolerance_units",                                // the sequest.params property name
                        "Define the mass window around the spectrum precursor mass in units of daltons or in parts-per-million. Default daltons.",       // the sequest.params comment
                        ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                        new ListParamsValidator("Da", "PPM"),
                        true
        )).setInputXmlLabels("sequest, mass_tolerance_units" );

        _params.addProperty(new SequestParam(
                        255,                                                       //sortOrder
                        "mass",                                            //The value of the property
                        "precursor_tolerance_type",                                // the sequest.params property name
                        "Define the mass window around the spectrum precursor mass in units of daltons or in parts-per-million. Default daltons.",       // the sequest.params comment
                        ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                        new ListParamsValidator("mass", "mz"),
                        true
        )).setInputXmlLabels("sequest, precursor_tolerance_type" );

        _params.addProperty(new SequestParam(
                        256,                                                       //sortOrder
                        "0",                                            //The value of the property
                        "highres_fragment_ions",                                // the sequest.params property name
                        "MS/MS spectra are high resolution and show isotopic peaks. Default false.",       // the sequest.params comment
                        ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                        new BooleanParamsValidator(),
                        true
        )).setInputXmlLabels("sequest, highres_fragment_ions" );

        _params.addProperty(new SequestParam(
                        257,                                                       //sortOrder
                        "0",                                            //The value of the property
                        "print_expect_score",                                // the sequest.params property name
                        "Replace Sp score with expectation score. Default false.",       // the sequest.params comment
                        ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                        new BooleanParamsValidator(),
                        true
        )).setInputXmlLabels("sequest, print_expect_score" );

        _params.addProperty(new SequestParam(
                        258,                                                       //sortOrder
                        "0",                                            //The value of the property
                        "output_format",                                // the sequest.params property name
                        "0=sqt stdout (default), 1=out files",       // the sequest.params comment
                        ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                        new BooleanParamsValidator(),
                        false
        ));

        _params.addProperty(new SequestParam(
                        259,                                                       //sortOrder
                        "0",                                            //The value of the property
                        "max_fragment_charge",                                // the sequest.params property name
                        "Set the maximum charge state of fragment ions automatically (based on the precursor charge) or set the maximum to the given charge. Default 0 (automatic).",       // the sequest.params comment
                        ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                        new NonNegativeIntegerParamsValidator(),
                        true
        )).setInputXmlLabels("sequest, max_fragment_charge" );

        _params.addProperty(new SequestParam(
                        261,                                                       //sortOrder
                        "5",                                            //The value of the property
                        "max_precursor_charge",                                // the sequest.params property name
                        "Analyze spectra with charge no higher than that given. Default 5.",       // the sequest.params comment
                        ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                        new NaturalNumberParamsValidator(),
                        true
        )).setInputXmlLabels("sequest, max_precursor_charge" );

        _params.addProperty(new SequestParam(
                        262,                                                       //sortOrder
                        "1",                                            //The value of the property
                        "variable_C_terminus_distance",                                // the sequest.params property name
                        "Apply c-term modifications to peptides based on their distance from the protein terminus. -1=all peptides, 0=peptide only at protein terminus, N=proteins no more than N residues from the protein terminus.",       // the sequest.params comment
                        ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                        new RealNumberParamsValidator(),
                        true
        )).setInputXmlLabels("sequest, variable_C_terminus_distance" );

        _params.addProperty(new SequestParam(
                        263,                                                       //sortOrder
                        "1",                                            //The value of the property
                        "variable_N_terminus_distance",                                // the sequest.params property name
                        "Apply n-term modifications to peptides based on their distance from the protein terminus. -1=all peptides, 0=peptide only at protein terminus, N=proteins no more than N residues from the protein terminus.",       // the sequest.params comment
                        ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                        new RealNumberParamsValidator(),
                        true
        )).setInputXmlLabels("sequest, variable_N_terminus_distance" );

        _params.addProperty(new SequestParam(
            160,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "variable_C_terminus",                                // the sequest.params property name
            "Apply this mass modification to the C-terminus of each peptide in addition to searching the unmodified peptide. Default no c-term mod.",                                                       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
            null,
            false
         ));

        _params.addProperty(new SequestParam(
            161,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "variable_N_terminus",                                // the sequest.params property name
            "Apply this mass modification to the N-terminus of each peptide in addition to searching the unmodified peptide. Default no n-term mod.",                                                       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
            null,
            false
         ));
    }

    @Override
    List<String> initDynamicTermMods(char term, String massString)
    {
        if (term != '[' && term != ']')
        {
            return Collections.singletonList("Invalid terminal modification: " + term);
        }

        try
        {
            double mass = Double.parseDouble(massString);
            Param termProp = _params.getParam(term == '[' ? "variable_N_terminus" : "variable_C_terminus");
            termProp.setValue(massString);
        }
        catch (NumberFormatException e)
        {
            return Collections.singletonList("Could not parse variable terminal modification: " + massString);
        }
        return Collections.emptyList();
    }

    @Override
    protected String getSupportedEnzyme(String enzyme) throws SequestParamsException
    {
        enzyme = removeWhiteSpace(enzyme);
        if (enzyme == null || enzyme.isEmpty())
        {
            throw new SequestParamsException("No enzyme specified");
        }

        enzyme = removeWhiteSpace(enzyme);
        enzyme = combineEnzymes(enzyme.split(","));
        for(Map.Entry<String, Integer> entry : UW_SEQUEST_ENZYME_MAP.entrySet())
        {
            if(sameEnzyme(enzyme, entry.getKey()))
            {
                _params.getParam("enzyme_number").setValue(entry.getValue().toString());
                return entry.getValue().toString();
            }
        }
        throw new SequestParamsException("Unsupported enzyme: " + enzyme);
    }



    @Override
    public String getSequestParamsText() throws SequestParamsException
    {
        String result = super.getSequestParamsText();

        return result +
                "print_duplicate_references = 1         ; 0=no, 1=yes\n" +
                "\n" +
                "[SEQUEST_ENZYME_INFO]\n" +
                "0.  No_Enzyme              0      -           -\n" +
                "1.  Trypsin                1      KR          P\n" +
                "2.  Chymotrypsin           1      FWY         P\n" +
                "3.  Clostripain            1      R           -\n" +
                "4.  Cyanogen_Bromide       1      M           -\n" +
                "5.  IodosoBenzoate         1      W           -\n" +
                "6.  Proline_Endopept       1      P           -\n" +
                "7.  Staph_Protease         1      E           -\n" +
                "8.  Trypsin_K              1      K           P\n" +
                "9.  Trypsin_R              1      R           P\n" +
                "10. AspN                   0      D           -\n" +
                "11. Cymotryp/Modified      1      FWYL        P\n" +
                "12. Elastase               1      ALIV        P\n" +
                "13. Elastase/Tryp/Chymo    1      ALIVKRWFY   P\n";
    }

    public static class TestCase extends Assert
    {
        private File _root;

        @Before
        public void setUp()
        {
            _root = new File("fakeroot");
        }

        public void fail(List<String> messages)
        {
            fail(StringUtils.join(messages, '\n'));
        }

        @Test
        public void testEnzyme()
        {
            UWSequestParamsBuilder spb = new UWSequestParamsBuilder(Collections.singletonMap("protein, cleavage site", "[X]|[X]"), _root);
            List<String> parserError = spb.initEnzymeInfo();
            if (!parserError.isEmpty()) fail(parserError);
            assertEquals("enzyme_number", "0", spb.getProperties().getParam("enzyme_number").getValue());

            // Bad enzyme
            spb = new UWSequestParamsBuilder(Collections.singletonMap("protein, cleavage site", "[AA]|[B]"), _root);
            parserError = spb.initEnzymeInfo();
            assertEquals("Should have one error", 1, parserError.size());

            // Typsin
            spb = new UWSequestParamsBuilder(Collections.singletonMap("protein, cleavage site", "[KR]|{P}"), _root);
            parserError = spb.initEnzymeInfo();
            if (!parserError.isEmpty()) fail(parserError);
            assertEquals("enzyme_number", "1", spb.getProperties().getParam("enzyme_number").getValue());

            // AspN
            spb = new UWSequestParamsBuilder(Collections.singletonMap("protein, cleavage site", "[X]|[D]"), _root);
            parserError = spb.initEnzymeInfo();
            if (!parserError.isEmpty()) fail(parserError);
            assertEquals("enzyme_number", "10", spb.getProperties().getParam("enzyme_number").getValue());
        }

        @Test
        public void testUWSpecificParams() throws SequestParamsException
        {
            Map<String, String> params = new HashMap<String, String>();
            params.put("pipeline, database", "Bovine_mini.fasta");
            params.put("sequest, min_peaks", "50");
            params.put("sequest, max_precursor_charge", "4");
            UWSequestParamsBuilder spb = new UWSequestParamsBuilder(params, _root);
            spb.initPassThroughs();
            assertEquals("min_peaks", "50", spb.getPropertyValue("min_peaks"));
            assertEquals("max_precursor_charge", "4", spb.getPropertyValue("max_precursor_charge"));
        }

        @Test
        public void testVariableTermModification() throws SequestParamsException
        {
            UWSequestParamsBuilder spb = new UWSequestParamsBuilder(Collections.singletonMap("residue, potential modification mass", "50.43@[,90.12@]"), _root);
            spb.initDynamicMods();
            assertEquals("variable_N_terminus", "50.43", spb.getPropertyValue("variable_N_terminus"));
            assertEquals("variable_C_terminus", "90.12", spb.getPropertyValue("variable_C_terminus"));
        }

        @Test
        public void testStaticTermModification() throws SequestParamsException
        {
            UWSequestParamsBuilder spb = new UWSequestParamsBuilder(Collections.singletonMap("residue, modification mass", "50.43@[,90.12@]"), _root);
            spb.initStaticMods();
            assertEquals("add_N_terminus", "50.43", spb.getPropertyValue("add_N_terminus"));
            assertEquals("add_C_terminus", "90.12", spb.getPropertyValue("add_C_terminus"));
        }
    }
}
