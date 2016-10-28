/*
 * Copyright (c) 2013-2016 LabKey Corporation
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
package org.labkey.ms2.pipeline.comet;

import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.ms2.pipeline.AbstractMS2SearchTask;
import org.labkey.ms2.pipeline.client.ParameterNames;
import org.labkey.ms2.pipeline.sequest.AbstractSequestParams;
import org.labkey.ms2.pipeline.sequest.BooleanParamsValidator;
import org.labkey.ms2.pipeline.sequest.ConverterFactory;
import org.labkey.ms2.pipeline.sequest.ListParamsValidator;
import org.labkey.ms2.pipeline.sequest.MultipleDoubleParamsValidator;
import org.labkey.ms2.pipeline.sequest.MultipleIntegerParamsValidator;
import org.labkey.ms2.pipeline.sequest.NaturalNumberParamsValidator;
import org.labkey.ms2.pipeline.sequest.NonNegativeIntegerParamsValidator;
import org.labkey.ms2.pipeline.sequest.Param;
import org.labkey.ms2.pipeline.sequest.ParamsValidatorFactory;
import org.labkey.ms2.pipeline.sequest.RealNumberParamsValidator;
import org.labkey.ms2.pipeline.sequest.SequestParam;
import org.labkey.ms2.pipeline.sequest.SequestParams;
import org.labkey.ms2.pipeline.sequest.SequestParamsBuilder;
import org.labkey.ms2.pipeline.sequest.SequestParamsException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Support for generating comet.params for 2014 releases of Comet
 * User: jeckels
 * Date: 9/16/13
 */
public class Comet2014ParamsBuilder extends SequestParamsBuilder
{
    private static final int MAX_VARIABLE_MODIFICATIONS = 6;
    private static final Map<String, Integer> COMET_ENZYME_MAP;

    static
    {
        Map<String, Integer> m = new CaseInsensitiveHashMap<>();

        m.put("[X]|[X]", 0);        // None
        m.put("[KR]|{P}", 1);       // Trypsin
        m.put("[KR]|[X]", 2);       // Trypsin/P
        m.put("[K]|{P}", 3);        // Lys_C
        m.put("[K]|[X]", 4);        // Lys_N
        m.put("[R]|{P}", 5);        // Arg_C
        m.put("[D]|[X]", 6);        // Arg_N
        m.put("[M]|{P}", 7);        // Cyanogen_Bromide
        m.put("[DE]|{P}", 8);       // Glu_C
        m.put("[FL]|{P}", 9);       // PepsinA
        m.put("[FWYL]|{P}", 10);    // Chymotrypsin

        COMET_ENZYME_MAP = Collections.unmodifiableMap(m);
    }

    public Comet2014ParamsBuilder(Map<String, String> sequestInputParams, File sequenceRoot)
    {
        super(sequestInputParams, sequenceRoot, SequestParams.Variant.comet);
    }

    protected AbstractSequestParams createSequestParams(AbstractSequestParams.Variant variant)
    {
        return new CometParams();
    }

    @Override
    protected List<String> initIonScoring()
    {
        return Collections.emptyList();
    }

    protected void initSubclass()
    {
        _params.initUWSequestAndCometProperties();

        _params.addProperty(new SequestParam(
                21,                                                       //sortOrder
                "0",                                            // default value of the property
                "decoy_search",                                // parameters file property name
                "0=no (default), 1=concatenated search, 2=separate search",       // comment in the parameters file
                ConverterFactory.getSequestBasicConverter(),                      
                null,
                true
        )).setInputXmlLabels("comet, decoy_search");
        _params.addProperty(new SequestParam(
                21,                                                       //sortOrder
                "DECOY_",                                            // default value of the property
                "decoy_prefix",                                // parameters file property name
                "decoy entries are denoted by this string which is pre-pended to each protein accession",       // comment in the parameters file
                ConverterFactory.getSequestBasicConverter(),                      
                null,
                true
        )).setInputXmlLabels("comet, decoy_prefix");

        _params.addProperty(new SequestParam(
                22,                                                       //sortOrder
                "0",                                            // default value of the property
                "search_enzyme_number",                                // parameters file property name
                "choose from list at end of this params file",       // comment in the parameters file
                ConverterFactory.getSequestBasicConverter(),                      
                null,
                false
        ));
        _params.addProperty(new SequestParam(
                23,                                                       //sortOrder
                "0",                                            // default value of the property
                "sample_enzyme_number",                                // parameters file property name
                "choose from list at end of this params file",       // comment in the parameters file
                ConverterFactory.getSequestBasicConverter(),                      
                null,
                false
        ));

        _params.addProperty(new SequestParam(
            150,                                                       //sortOrder
            "5",                                            // default value of the property
            "max_variable_mods_in_peptide",                                // parameters file property name
            "",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            ParamsValidatorFactory.getPositiveIntegerParamsValidator(),
            true
        )).setInputXmlLabels(_variant.getParamPrefix() + ", max_variable_mods_in_peptide");
        // Add 6 variable modifications
        for (int i = 1; i <= 6; i++)
        {
            _params.addProperty(new SequestParam(
                150 + i,                                                       //sortOrder
                "0.0 X 0 3",                                            // default value of the property
                "variable_mod" + i,                                // parameters file property name
                "",       // comment in the parameters file
                ConverterFactory.getSequestBasicConverter(),                      
                ParamsValidatorFactory.getPositiveIntegerParamsValidator(),
                false
            )).setInputXmlLabels(ParameterNames.DYNAMIC_MOD);
        }


        _params.addProperty(new SequestParam(
            250,                                                       //sortOrder
            "0",                                            // default value of the property
            "use_A_ions",                                // parameters file property name
            "",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            ParamsValidatorFactory.getBooleanParamsValidator(),
            true
        )).setInputXmlLabels("scoring, a ions");
        _params.addProperty(new SequestParam(
            251,                                                       //sortOrder
            "1",                                            // default value of the property
            "use_B_ions",                                // parameters file property name
            "",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            ParamsValidatorFactory.getBooleanParamsValidator(),
            true
        )).setInputXmlLabels("scoring, b ions");
        _params.addProperty(new SequestParam(
            252,                                                       //sortOrder
            "0",                                            // default value of the property
            "use_C_ions",                                // parameters file property name
            "",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            ParamsValidatorFactory.getBooleanParamsValidator(),
            true
        )).setInputXmlLabels("scoring, c ions");
        _params.addProperty(new SequestParam(
            253,                                                       //sortOrder
            "0",                                            // default value of the property
            "use_X_ions",                                // parameters file property name
            "",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            ParamsValidatorFactory.getBooleanParamsValidator(),
            true
        )).setInputXmlLabels("scoring, x ions");
        _params.addProperty(new SequestParam(
            254,                                                       //sortOrder
            "1",                                            // default value of the property
            "use_Y_ions",                                // parameters file property name
            "",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            ParamsValidatorFactory.getBooleanParamsValidator(),
            true
        )).setInputXmlLabels("scoring, y ions");
        _params.addProperty(new SequestParam(
            255,                                                       //sortOrder
            "0",                                            // default value of the property
            "use_Z_ions",                                // parameters file property name
            "",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            ParamsValidatorFactory.getBooleanParamsValidator(),
            true
        )).setInputXmlLabels("scoring, z ions");
        _params.addProperty(new SequestParam(
            256,                                                       //sortOrder
            "1",                                            // default value of the property
            "use_NL_ions",                                // parameters file property name
            "",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            ParamsValidatorFactory.getBooleanParamsValidator(),
            true
        )).setInputXmlLabels("comet, use_NL_ions");

        _params.addProperty(new SequestParam(
                260,                                                       //sortOrder
                "0",                                            // default value of the property
                "use_sparse_matrix",                                // parameters file property name
                "",       // comment in the parameters file
                ConverterFactory.getSequestBasicConverter(),                      
                ParamsValidatorFactory.getBooleanParamsValidator(),
                false
        ));

        _params.addProperty(new SequestParam(
                160,                                                       //sortOrder
                "0.0",                                            //The value of the property
                "variable_C_terminus",                                // the sequest.params property name
                "Apply this mass optional modification to the C-term of each peptide",                                                       // the sequest.params comment
                ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
                null,
                false
        ));

        _params.addProperty(new SequestParam(
                161,                                                       //sortOrder
                "0.0",                                            //The value of the property
                "variable_N_terminus",                                // the sequest.params property name
                "Apply this mass optional modification to the N-term of each peptide",                                                       // the sequest.params comment
                ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
                null,
                false
        ));

        _params.addProperty(new SequestParam(
                262,                                                       //sortOrder
                "-1",                                            //The value of the property
                "variable_C_terminus_distance",                                // the sequest.params property name
                "Apply based on distance from protein terminus. -1=all, N=no more than N residues from the protein terminus",       // the sequest.params comment
                ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                new RealNumberParamsValidator(),
                true
        )).setInputXmlLabels(_variant.getParamPrefix() + ", variable_C_terminus_distance" );

        _params.addProperty(new SequestParam(
                263,                                                       //sortOrder
                "-1",                                            //The value of the property
                "variable_N_terminus_distance",                                // the sequest.params property name
                "Apply based on distance from protein terminus. -1=all, N=no more than N residues from the protein terminus",       // the sequest.params comment
                ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                new RealNumberParamsValidator(),
                true
        )).setInputXmlLabels(_variant.getParamPrefix() + ", variable_N_terminus_distance" );


        _params.addProperty(new SequestParam(
                440,                                                       //sortOrder
                "0.0",                                            // default value of the property
                "add_X_user_amino_acid",                                // parameters file property name
                "added to X - avg. 0.0000, mono. 0.0000",       // comment in the parameters file
                ConverterFactory.getSequestBasicConverter(),                      
                null,
                false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.addProperty(new SequestParam(
                470,                                                       //sortOrder
                "0.0",                                            // default value of the property
                "add_B_user_amino_acid",                                // parameters file property name
                "added to B - avg. 0.0000, mono. 0.0000",       // comment in the parameters file
                ConverterFactory.getSequestBasicConverter(),                      
                null,
                false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.addProperty(new SequestParam(
            475,                                                       //sortOrder
            "0.0",                                            // default value of the property
            "add_U_user_amino_acid",                                // parameters file property name
            "added to U - avg. 0.0000, mono. 0.0000",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.addProperty(new SequestParam(
                476,                                                       //sortOrder
                "0.0",                                            // default value of the property
                "add_J_user_amino_acid",                                // parameters file property name
                "added to J - avg. 0.0000, mono. 0.0000",       // comment in the parameters file
                ConverterFactory.getSequestBasicConverter(),                      
                null,
                false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.addProperty(new SequestParam(
                500,                                                       //sortOrder
                "0.0",                                            // default value of the property
                "add_Z_user_amino_acid",                                // parameters file property name
                "added to Z - avg. 0.0000, mono. 0.0000",       // comment in the parameters file
                ConverterFactory.getSequestBasicConverter(),                      
                null,
                false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.addProperty(new SequestParam(
            71,                                                       //sortOrder
            "0.4",                                                    // default value of the property
            "fragment_bin_offset",                                 // parameters file property name
            "offset position to start the binning (0.0 to 1.0)",// comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                             
            ParamsValidatorFactory.getRealNumberParamsValidator(),
            true
        )).setInputXmlLabels("comet, fragment_bin_offset");

        _params.addProperty(new SequestParam(
            70,                                                       //sortOrder
            "1.0005",                                                    // default value of the property
            "fragment_bin_tol",                                 // parameters file property name
            "binning to use on fragment ions",// comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                             
            ParamsValidatorFactory.getRealNumberParamsValidator(),
            true
        )).setInputXmlLabels("spectrum, fragment mass error");

        _params.addProperty(new SequestParam(
            172,                                                       //sortOrder
            "0",                                            // default value of the property
            "spectrum_batch_size",                                // parameters file property name
            "max. # of spectra to search at a time; 0 to search the entire scan range in one loop", // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                              
            ParamsValidatorFactory.getPositiveIntegerParamsValidator(),
            true
        )).setInputXmlLabels("comet, spectrum_batch_size");

        _params.addProperty(new SequestParam(
              208,                                                       //sortOrder
              "10",                                                      // default value of the property
              "minimum_peaks",                                           // parameters file property name
              "minimum num. of peaks in spectrum to search (default 10)",                                                       // the input.xml label
               ConverterFactory.getSequestBasicConverter(),                              
               new NaturalNumberParamsValidator(),
               true
        )).setInputXmlLabels("comet, minimum_peaks");

        _params.addProperty(new SequestParam(
                211,                                                       //sortOrder
                "0.0 0.0",                                            // default value of the property
                "clear_mz_range",                                // parameters file property name
                "for iTRAQ/TMT type data; will clear out all peaks in the specified m/z range",       // comment in the parameters file
                ConverterFactory.getSequestBasicConverter(),                      
                new MultipleDoubleParamsValidator(0, Double.MAX_VALUE, 2),
                true
        )).setInputXmlLabels("comet, clear_mz_range");

        _params.addProperty(new SequestParam(
                230,                                                       //sortOrder
                "2",                                            // default value of the property
                "allowed_missed_cleavage",                                // parameters file property name
                "maximum value is 5; for enzyme search",       // comment in the parameters file
                ConverterFactory.getSequestBasicConverter(),                      
                ParamsValidatorFactory.getPositiveIntegerParamsValidator(),
                true
        ).setInputXmlLabels(AbstractMS2SearchTask.MAXIMUM_MISSED_CLEAVAGE_SITES));


        _params.addProperty(new SequestParam(
            259,                                                       //sortOrder
            "3",                                            // default value of the property
            "max_fragment_charge",                                // parameters file property name
            "set maximum fragment charge state to analyze (allowed max 5)",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            new NonNegativeIntegerParamsValidator(),
            true
        )).setInputXmlLabels("comet, max_fragment_charge" );

        _params.addProperty(new SequestParam(
            261,                                                       //sortOrder
            "6",                                            // default value of the property
            "max_precursor_charge",                                // parameters file property name
            "set maximum precursor charge state to analyze (allowed max 9)",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            new NaturalNumberParamsValidator(),
            true
        )).setInputXmlLabels("comet, max_precursor_charge" );

        _params.addProperty(new SequestParam(
            300,                                                       //sortOrder
            "0",                                            // default value of the property
            "output_outfiles",                                // parameters file property name
            "0=no, 1=yes  write .out files",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            new BooleanParamsValidator(),
            false
        ));
        _params.addProperty(new SequestParam(
            301,                                                       //sortOrder
            "1",                                            // default value of the property
            "output_pepxmlfile",                                // parameters file property name
            "0=no, 1=yes  write .pep.xml file",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            new BooleanParamsValidator(),
            false
        ));
        _params.addProperty(new SequestParam(
            302,                                                       //sortOrder
            "0",                                            // default value of the property
            "output_sqtfile",                                // parameters file property name
            "0=no, 1=yes  write sqt file",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            new BooleanParamsValidator(),
            false
        ));
        _params.addProperty(new SequestParam(
            303,                                                       //sortOrder
            "0",                                            // default value of the property
            "output_sqtstream",                                // parameters file property name
            "0=no, 1=yes  write sqt to standard output",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            new BooleanParamsValidator(),
            false
        ));
        _params.addProperty(new SequestParam(
            304,                                                       //sortOrder
            "0",                                            // default value of the property
            "output_txtfile",                                // parameters file property name
            "0=no, 1=yes  write .tab-delimited txt file",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            new BooleanParamsValidator(),
            false
        ));
        _params.addProperty(new SequestParam(
            305,                                                       //sortOrder
            "0",                                            // default value of the property
            "output_pinxmlfile",                                // parameters file property name
            "0=no, 1=yes  write pin.xml file",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            new BooleanParamsValidator(),
            false
        ));

        _params.addProperty(new SequestParam(
            400,                                                       //sortOrder
            "0 0",                                            // default value of the property
            "scan_range",                                // parameters file property name
            "start and scan scan range to search; 0 as 1st entry ignores parameter",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            new MultipleIntegerParamsValidator(0, Integer.MAX_VALUE, 2),
            false
        ).setInputXmlLabels("comet, scan_range"));
        _params.addProperty(new SequestParam(
            401,                                                       //sortOrder
            "0 0",                                            // default value of the property
            "precursor_charge",                                // parameters file property name
            "precursor charge range to analyze; does not override mzXML charge; 0 as 1st entry ignores parameter",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            new MultipleIntegerParamsValidator(0, Integer.MAX_VALUE, 2),
            true
        ).setInputXmlLabels("comet, precursor_charge"));
        _params.addProperty(new SequestParam(
            402,                                                       //sortOrder
            "2",                                            // default value of the property
            "ms_level",                                // parameters file property name
            "MS level to analyze, valid are levels 2 (default) or 3",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            new NonNegativeIntegerParamsValidator(),
            true
        ).setInputXmlLabels("comet, ms_level"));
        _params.addProperty(new SequestParam(
            403,                                                       //sortOrder
            "ALL",                                            // default value of the property
            "activation_method",                                // parameters file property name
            "activation method; used if activation method set; allowed ALL, CID, ECD, ETD, PQD, HCD, IRMPD",       // comment in the parameters file
            ConverterFactory.getSequestBasicConverter(),                      
            new ListParamsValidator("ALL", "CID", "ECD", "ETD", "PQD", "HCD", "IRMPD"),
            true
        ).setInputXmlLabels("comet, activation_method"));
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
        for(Map.Entry<String, Integer> entry : COMET_ENZYME_MAP.entrySet())
        {
            if(sameEnzyme(enzyme, entry.getKey()))
            {
                _params.getParam("sample_enzyme_number").setValue(entry.getValue().toString());
                _params.getParam("search_enzyme_number").setValue(entry.getValue().toString());
                return entry.getValue().toString();
            }
        }
        throw new SequestParamsException("Unsupported enzyme: " + enzyme);
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

    /** Comet wants separate parameters for each variable modification
     * Up to 6 variable modifications are supported
     * format:  <mass> <residues> <0=variable/1=binary> <max mods per a peptide>
     * e.g. 79.966331 STY 0 3
     * @return any errors
     */
    public List<String> initDynamicMods()
    {
        ArrayList<ResidueMod> workList = new ArrayList<>();

        String mods = sequestInputParams.get(ParameterNames.DYNAMIC_MOD);
        if (mods == null || mods.equals("")) return Collections.emptyList();
        mods = removeWhiteSpace(mods);
        ArrayList<Character> residues = new ArrayList<>();
        ArrayList<String> masses = new ArrayList<>();

        List<String> parserError = parseMods(mods, residues, masses);
        if (!parserError.isEmpty()) return parserError;

        for (int i = 0; i < masses.size(); i++)
        {
            char res = residues.get(i);
            if(res == '['||res == ']')
            {
                parserError = initDynamicTermMods(res, masses.get(i));
                if (parserError != null && !parserError.isEmpty()) return parserError;
            }
            else
            {
                workList.add(new ResidueMod(res, masses.get(i)));
            }
        }
        if(workList.size() > MAX_VARIABLE_MODIFICATIONS) Collections.singletonList("Comet will only accept a max of " + MAX_VARIABLE_MODIFICATIONS + " variable modifications.");
        int index = 1;
        for (ResidueMod mod : workList)
        {
            //parse mods function tested for NumberFormatException
            float weight = Float.parseFloat(mod.getWeight());

            Param modProp = _params.getParam("variable_mod" + index++);
            modProp.setValue(weight + " " + mod.getRes() + " 0 3");
        }
        return parserError;
    }

    public String getSequestParamsText() throws SequestParamsException
    {
        String result = super.getSequestParamsText();

        return "# comet_version 2014.01 rev. 0\n" +
                "\n" +
                result +
                "\n" +
                "#\n" +
                "# COMET_ENZYME_INFO _must_ be at the end of this parameters file\n" +
                "#\n" +
                "[COMET_ENZYME_INFO]\n" +
                "0.  No_enzyme              0      -           -\n" +
                "1.  Trypsin                1      KR          P\n" +
                "2.  Trypsin/P              1      KR          -\n" +
                "3.  Lys_C                  1      K           P\n" +
                "4.  Lys_N                  0      K           -\n" +
                "5.  Arg_C                  1      R           P\n" +
                "6.  Asp_N                  0      D           -\n" +
                "7.  CNBr                   1      M           -\n" +
                "8.  Glu_C                  1      DE          P\n" +
                "9.  PepsinA                1      FL          P\n" +
                "10. Chymotrypsin           1      FWYL        P\n";
    }

    public static class LimitedParseTestCase extends Assert
    {
        private final File _root = new File("fakeroot");

        @Test
        public void testGenerateFile() throws SequestParamsException
        {
            Map<String, String> paramMap = new HashMap<>();

            paramMap.put(ParameterNames.STATIC_MOD, "50.43@[,90.12@],100.12@C");
            paramMap.put(ParameterNames.SEQUENCE_DB, DUMMY_FASTA_NAME);
            paramMap.put("comet, digest_mass_range", "400.0 5943.0");
            paramMap.put("spectrum, parent monoisotopic mass error units", "mmu");
            Comet2014ParamsBuilder spb = new Comet2014ParamsBuilder(paramMap, _root);
            spb.initXmlValues();
            String text = spb.getSequestParamsText();
            assertTrue(text.contains("database_name ="));
            assertTrue(text.contains("num_threads = 0"));
            assertTrue(text.contains("digest_mass_range = 400.0 5943.0"));
            assertTrue(text.contains("peptide_mass_units = 1"));
            assertTrue(text.contains("decoy_search = 0"));
            assertTrue(text.contains("decoy_prefix = DECOY_"));
            // Be sure that the amino acid names start with a lower-case character
            assertTrue(text.contains("add_C_cysteine = 100.12"));
            assertTrue(text.contains("allowed_missed_cleavage = 2"));
            assertTrue(text.contains("search_enzyme_number = 0"));
            assertTrue(text.contains("0.  No_enzyme"));
        }

        @Test
        public void testDecoy() throws SequestParamsException
        {
            Map<String, String> paramMap = new HashMap<>();

            paramMap.put(ParameterNames.SEQUENCE_DB, DUMMY_FASTA_NAME);
            paramMap.put("comet, decoy_search", "1");
            paramMap.put("comet, decoy_prefix", "NEW_PREFIX_");
            Comet2014ParamsBuilder spb = new Comet2014ParamsBuilder(paramMap, _root);
            spb.initXmlValues();
            String text = spb.getSequestParamsText();
            assertTrue(text.contains("decoy_search = 1"));
            assertTrue(text.contains("decoy_prefix = NEW_PREFIX_"));
        }

        @Test
        public void testEnzymes() throws SequestParamsException
        {
            Comet2014ParamsBuilder spb = new Comet2014ParamsBuilder(Collections.singletonMap(ParameterNames.ENZYME, "[KR]|{P}"), _root);
            spb.initEnzymeInfo();
            String text = spb.getSequestParamsText();
            assertTrue(text.contains("search_enzyme_number = 1"));
            assertTrue(text.contains("sample_enzyme_number = 1"));
            assertTrue(text.contains("1.  Trypsin"));

            spb = new Comet2014ParamsBuilder(Collections.singletonMap(ParameterNames.ENZYME, "[KR]|[X]"), _root);
            spb.initEnzymeInfo();
            text = spb.getSequestParamsText();
            assertTrue(text.contains("search_enzyme_number = 2"));
            assertTrue(text.contains("sample_enzyme_number = 2"));
            assertTrue(text.contains("2.  Trypsin/P"));
        }
    }

    public static class FullParseTestCase extends AbstractSequestTestCase
    {
        @Override
        public SequestParamsBuilder createParamsBuilder()
        {
            return new Comet2014ParamsBuilder(ip.getInputParameters(), root);
        }

        @Test
        public void testInitDynamicModsDefault()
        {
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                "</bioml>");

            List<String> parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);
            for (int i = 1; i <= MAX_VARIABLE_MODIFICATIONS ; i++)
            {
                String paramName = "variable_mod" + i;
                Param sp = spb.getProperties().getParam(paramName);
                assertEquals(paramName, "0.0 X 0 3", sp.getValue());
            }
        }

        @Test
        public void testInitDynamicModsNormal()
        {
            parseParams("<?xml version=\"1.0\"?>" +
                "<bioml>" +
                    "<note type=\"input\" label=\"residue, potential modification mass\">+16@M,+9@C</note>" +
                "</bioml>");

            List<String> parserError = spb.initDynamicMods();
            if (!parserError.isEmpty()) fail(parserError);

            assertEquals("variable_mod1", "16.0 M 0 3", spb.getProperties().getParam("variable_mod1").getValue());
            assertEquals("variable_mod2", "9.0 C 0 3", spb.getProperties().getParam("variable_mod2").getValue());

            for (int i = 3; i <= MAX_VARIABLE_MODIFICATIONS ; i++)
            {
                String paramName = "variable_mod" + i;
                Param sp = spb.getProperties().getParam(paramName);
                assertEquals(paramName, "0.0 X 0 3", sp.getValue());
            }
        }
    }
}
