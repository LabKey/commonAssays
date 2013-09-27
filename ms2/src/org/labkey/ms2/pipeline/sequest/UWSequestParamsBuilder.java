/*
 * Copyright (c) 2011-2013 LabKey Corporation
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
import org.junit.Test;
import org.labkey.api.collections.CaseInsensitiveHashMap;
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
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();

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
    }

    public UWSequestParamsBuilder(Map<String, String> sequestInputParams, File sequenceRoot, SequestParams.Variant variant, List<File> databaseFiles)
    {
        super(sequestInputParams, sequenceRoot, variant, databaseFiles);
    }

    protected void initSubclass()
    {
        _params.initUWSequestAndCometProperties();

        _params.addProperty(new SequestParam(
            71,                                                       //sortOrder
            "0.11",                                                    //The value of the property
            "fragment_bin_startoffset",                                 // the sequest.params property name
            "offset position to start the binning",// the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                             //converts the instance to a sequest.params line
            ParamsValidatorFactory.getRealNumberParamsValidator(),
            true
        )).setInputXmlLabels("spectrum, fragment_bin_startoffset");

        _params.addProperty(new SequestParam(
                  208,                                                       //sortOrder
                  "5",                                                      //The value of the property
                  "minimum_peaks",                                           // the sequest.params property name
                  "minimum num. of peaks in spectrum to search (default 5)",                                                       // the input.xml label
                   ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
                   new NaturalNumberParamsValidator(),
                   true
        )).setInputXmlLabels("sequest, minimum_peaks");

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
            151,                                                       //sortOrder
            "0 0 0 0 0 0",                                                        //The value of the property
            "diff_search_type",                                     // the sequest.params property name
            "0=variable mod, 1=binary mod",                                                       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
            new MultipleIntegerParamsValidator(0, 1, 6),
            true
        )).setInputXmlLabels("sequest, diff_search_type");

        _params.addProperty(new SequestParam(
            152,                                                       //sortOrder
            "4 4 4 4 4 4",                                                        //The value of the property
            "diff_search_count",                                     // the sequest.params property name
            "max num of modified AA per each variable mod in a peptide",                                                       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
            new MultipleIntegerParamsValidator(0, 50, 6),
            true
        )).setInputXmlLabels("sequest, diff_search_count");

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
                        "0=off, 1= on -1/0/1/2/3 (standard C13 error), 2= -8/-4/0/4/8 (for +4/+8 labeling)",       // the sequest.params comment
                        ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                        new ListParamsValidator("0", "1", "2"),
                        true
        )).setInputXmlLabels("sequest, isotope_error" );

        _params.addProperty(new SequestParam(
            571,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_U_user_amino_acid",                                // the sequest.params property name
            "added to U - avg.   0.0000, mono.   0.00000",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            new RealNumberParamsValidator(),
            true
        )).setInputXmlLabels().setInputXmlLabels("sequest, add_U_user_amino_acid");

        _params.addProperty(new SequestParam(
            572,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_J_user_amino_acid",                                // the sequest.params property name
            "added to J - avg.   0.0000, mono.   0.00000",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            new RealNumberParamsValidator(),
            true
        )).setInputXmlLabels().setInputXmlLabels("sequest, add_J_user_amino_acid");


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
    }

    @Override
    protected List<String> initDynamicTermMods(char term, String massString)
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
        private final File _root = new File("fakeroot");

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
            Map<String, String> params = new HashMap<>();
            params.put("pipeline, database", "Bovine_mini.fasta");
            params.put("sequest, max_precursor_charge", "4");
            UWSequestParamsBuilder spb = new UWSequestParamsBuilder(params, _root);
            spb.initPassThroughs();
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
            UWSequestParamsBuilder spb = new UWSequestParamsBuilder(Collections.singletonMap(ParameterNames.STATIC_MOD, "50.43@[,90.12@]"), _root);
            spb.initStaticMods();
            assertEquals("add_Nterm_peptide", "50.43", spb.getPropertyValue("add_Nterm_peptide"));
            assertEquals("add_Cterm_peptide", "90.12", spb.getPropertyValue("add_Cterm_peptide"));
        }

        @Test
        public void testGenerateFile() throws SequestParamsException
        {
            Map<String, String> paramMap = new HashMap<>();

            paramMap.put(ParameterNames.STATIC_MOD, "50.43@[,90.12@]");
            paramMap.put(ParameterNames.SEQUENCE_DB, DUMMY_FASTA_NAME);
            paramMap.put("sequest, digest_mass_range", "400.0 5900.0");
            paramMap.put("spectrum, parent monoisotopic mass error units", "mmu");
            UWSequestParamsBuilder spb = new UWSequestParamsBuilder(paramMap, _root);
            spb.initXmlValues();
            String text = spb.getSequestParamsText();
            assertTrue(text.contains("database_name ="));
            assertTrue(text.contains("num_threads = 0"));
            assertTrue(text.contains("digest_mass_range = 400.0 5900.0"));
            assertTrue(text.contains("peptide_mass_units = 1"));
        }

        @Test
        public void testAlternateXmlInputs() throws SequestParamsException
        {
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put(ParameterNames.SEQUENCE_DB, DUMMY_FASTA_NAME);
            paramMap.put("spectrum, fragment mass error", "500.0");
            UWSequestParamsBuilder spb = new UWSequestParamsBuilder(paramMap, _root);
            spb.initPassThroughs();
            assertEquals("fragment_ion_tolerance", "500.0", spb.getPropertyValue("fragment_ion_tolerance"));

            paramMap = new HashMap<>();
            paramMap.put(ParameterNames.SEQUENCE_DB, DUMMY_FASTA_NAME);
            paramMap.put("sequest, fragment_ion_tolerance", "500.0");
            spb = new UWSequestParamsBuilder(paramMap, _root);
            spb.initPassThroughs();
            assertEquals("fragment_ion_tolerance", "500.0", spb.getPropertyValue("fragment_ion_tolerance"));
        }
    }
}
