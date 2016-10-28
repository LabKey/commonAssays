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
package org.labkey.ms2.pipeline.sequest;

import org.labkey.ms2.pipeline.client.ParameterNames;

import java.util.ArrayList;
import java.util.Collection;

/**
 * User: jeckels
 * Date: 9/24/13
 */
public abstract class AbstractSequestParams extends Params
{
    protected final Variant _variant;

    public enum Variant
    {
        thermosequest("[SEQUEST]", "sequest", "first_database_name"),
        uwsequest("[SEQUEST]", "sequest", "database_name"),
        sequest("[SEQUEST]", "sequest", "database_name"),
        makedb("[MAKEDB]", "sequest", "database_name"),
        comet("[COMET]", "comet", "database_name")
        {
            public String getAAStaticModParamName(String letter, String name)
            {
                return "add_" + letter + "_" + name.toLowerCase();
            }

            public String getCommentPrefix()
            {
                return "#";
            }
        };

        private final String _header;
        private final String _paramPrefix;
        private final String _fastaDatabase;

        private Variant(String header, String paramPrefix, String fastaDatabase)
        {
            _header = header;
            _fastaDatabase = fastaDatabase;
            _paramPrefix = paramPrefix;
        }

        public String getFastaDatabase()
        {
            return _fastaDatabase;
        }

        public String getHeader()
        {
            return _header;
        }

        public String getParamPrefix()
        {
            return _paramPrefix;
        }

        public String getAAStaticModParamName(String letter, String name)
        {
            return "add_" + letter + "_" + name;
        }

        public String getCommentPrefix()
        {
            return ";";
        }
    }

    public AbstractSequestParams(Variant variant)
    {
        _variant = variant;
        initProperties();
    }

    public SequestParam addProperty(SequestParam param)
    {
        _params.add(param);
        return param;
    }

    public void initUWSequestAndCometProperties()
    {
        addProperty(new SequestParam(
                20,
                "0",
                "num_threads",
                "0=poll CPU to set num threads; else specify num threads directly (max 32)",
                ConverterFactory.getSequestBasicConverter(),
                new NonNegativeIntegerParamsValidator(),
                true
        )).setInputXmlLabels(_variant.getParamPrefix() + ", num_threads");

        addProperty(new SequestParam(
                61,                                                       //sortOrder
                "0",                                                      //The value of the property
                "theoretical_fragment_ions",                                           // the sequest.params property name
                "0=default peak shape, 1=M peak only",                                                       // the input.xml label
                ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
                new BooleanParamsValidator(),
                true
        )).setInputXmlLabels(_variant.getParamPrefix() + ", theoretical_fragment_ions");

        addProperty(new SequestParam(
                92,                                                       //sortOrder
                "0",                                                      //The value of the property
                "skip_researching",                                           // the sequest.params property name
                "for '.out' file output only, 0=search everything again (default), 1=don't search if .out exists",                                                       // the input.xml label
                ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
                new BooleanParamsValidator(),
                true
        ));

        addProperty(new SequestParam(
            171,                                                       //sortOrder
            "0",                                            //The value of the property
            "clip_nterm_methionine",                                // the sequest.params property name
            "0=leave sequences as-is; 1=also consider sequence w/o N-term methionine", // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
            new BooleanParamsValidator(),
            true
        )).setInputXmlLabels(_variant.getParamPrefix() + ", clip_nterm_methionine");

        addProperty(new SequestParam(
                  91,                                                       //sortOrder
                  "600.0 5000.0",                                                      //The value of the property
                  "digest_mass_range",                                           // the sequest.params property name
                  "MH+ peptide mass range to analyze",                                                       // the input.xml label
                   ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
                   new MultipleDoubleParamsValidator(0, 1000000, 2),
                   true
        )).setInputXmlLabels(_variant.getParamPrefix() + ", digest_mass_range");

        addProperty(new SequestParam(
                        253,                                                       //sortOrder
                        "0",                                            //The value of the property
                        "isotope_error",                                // the sequest.params property name
                        "0=off, 1= on -1/0/1/2/3 (standard C13 error), 2= -8/-4/0/4/8 (for +4/+8 labeling)",       // the sequest.params comment
                        ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                        new ListParamsValidator("0", "1", "2"),
                        true
        )).setInputXmlLabels(_variant.getParamPrefix() + ", isotope_error" );

        addProperty(new SequestParam(
                209,                                                       //sortOrder
                "0",                                                      //The value of the property
                "minimum_intensity",                                           // the sequest.params property name
                "minimum intensity value to read in",                                                       // the input.xml label
                ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
                new NaturalNumberParamsValidator(),
                true
        )).setInputXmlLabels(_variant.getParamPrefix() + ", minimum_intensity");


        addProperty(new SequestParam(
            211,                                                       //sortOrder
            "1.5",                                            //The value of the property
            "remove_precursor_tolerance",                                // the sequest.params property name
            "+- Da tolerance for precursor removal",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            ParamsValidatorFactory.getRealNumberParamsValidator(),
            true
        )).setInputXmlLabels(_variant.getParamPrefix() + ", remove_precursor_tolerance");

        addProperty(new SequestParam(
            252,                                                       //sortOrder
            "2",                                            //The value of the property
            "num_enzyme_termini",                                // the sequest.params property name
            "Generate peptides with enzyme digestion sites at one or both termini. Default 2.",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            new NonNegativeIntegerParamsValidator(),
            true
        )).setInputXmlLabels(_variant.getParamPrefix() + ", num_enzyme_termini" );

        addProperty(new SequestParam(
            255,                                                       //sortOrder
            "0",                                            //The value of the property
            "precursor_tolerance_type",                                // the sequest.params property name
            "0=MH+ (default), 1=precursor m/z",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            new ListParamsValidator("0", "1"),
            true
        )).setInputXmlLabels(_variant.getParamPrefix() + ", precursor_tolerance_type" );

        addProperty(new SequestParam(
                257,                                                       //sortOrder
                "0",                                            //The value of the property
                "print_expect_score",                                // the sequest.params property name
                "Replace Sp score with expectation score. Default false.",       // the sequest.params comment
                ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                new BooleanParamsValidator(),
                true
        )).setInputXmlLabels(_variant + ", print_expect_score" );


    }

    @Override
    protected void initProperties()
    {
        _params.add(new SequestParam(
            20,
            "",
            _variant.getFastaDatabase(),
            "",
            ConverterFactory.getSequestBasicConverter(),
            null,
            false
        ).setInputXmlLabels("pipeline, database"));

        _params.add(new SequestParam(
            40,                                                       //sortOrder
            "2.0",                                                    //The value of the property
            "peptide_mass_tolerance",                                 // the sequest.params property name
            "",                                                       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                             //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels("spectrum, parent monoisotopic mass error plus",
            "spectrum, parent monoisotopic mass error minus"));

        _params.add(new SequestParam(
                   50,                                                       //sortOrder
                   "0",                                                    //The value of the property
                   "peptide_mass_units",                                 // the sequest.params property name
                   "0=amu, 1=mmu, 2=ppm",                                // the sequest.params comment
                    ConverterFactory.getSequestBasicConverter(),                             //converts the instance to a sequest.params line
                    null,
                    false                                                    //is pass through
           ).setInputXmlLabels("spectrum, parent monoisotopic mass error units"));

        _params.add(new SequestParam(
            80,                                                     //sortOrder
            "10",                                                   //The value of the property
            "num_output_lines",                                     // the sequest.params property name
            "# peptide results to show",                            // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                            //converts the instance to a sequest.params line
            ParamsValidatorFactory.getNaturalNumberParamsValidator(),
            true
        ).setInputXmlLabels(_variant.getParamPrefix() + ", num_output_lines"));

        _params.add(new SequestParam(
            90,                                                       //sortOrder
            "50", /** Changed to 50 to match up with UW Sequest default */                                                   //The value of the property
            "num_results",                                            // the sequest.params property name
            "# results to store",                                     // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
            ParamsValidatorFactory.getNaturalNumberParamsValidator(),
            true
        ).setInputXmlLabels(_variant.getParamPrefix() + ", num_results"));

                //pass through- no Xtandem counterpart
        _params.add(new SequestParam(
            110,                                                       //sortOrder
            "0",                                                      //The value of the property
            "show_fragment_ions",                                     // the sequest.params property name
            "0=no, 1=yes",                                            // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
            ParamsValidatorFactory.getBooleanParamsValidator(),
            true
        ).setInputXmlLabels(_variant.getParamPrefix() + ", show_fragment_ions"));

        //No xtandem element created for this property.
        _params.add(new SequestParam(
            170,                                                       //sortOrder
            "0",                                            //The value of the property
            "nucleotide_reading_frame",                                // the sequest.params property name
            "0=protein db, 1-6, 7 = forward three, 8-reverse three, 9=all six", // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
            null,
            false
        ));

        //It appears that xtandem doesn't have an average mass option
        _params.add(new SequestParam(
            180,                                                       //sortOrder
            "0",                                            //The value of the property
            "mass_type_parent",                                // the sequest.params property name
            "0=average masses, 1=monoisotopic masses",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            ParamsValidatorFactory.getBooleanParamsValidator(),
            true
        ).setInputXmlLabels(_variant.getParamPrefix() + SequestSearchTask.MASS_TYPE_PARENT_SUFFIX));

        _params.add(new SequestParam(
            190,                                                       //sortOrder
            "1",                                            //The value of the property
            "mass_type_fragment",                                // the sequest.params property name
            "0=average masses, 1=monoisotopic masses",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels("spectrum, fragment mass type"));

        _params.add(new SequestParam(
            210,                                                       //sortOrder
            "0",                                            //The value of the property
            "remove_precursor_peak",                                // the sequest.params property name
            "0=no, 1=yes, 2=all charge reduced precursor peaks (for ETD)",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            new ListParamsValidator("0", "1", "2"),
            true
        ).setInputXmlLabels(_variant.getParamPrefix() + ", remove_precursor_peak"));


        _params.add(new SequestParam(
            310,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_Cterm_peptide",                                // the sequest.params property name
            "added to each peptide C-terminus",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            320,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_Cterm_protein",                                // the sequest.params property name
            "added to each protein C-terminus",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            ParamsValidatorFactory.getRealNumberParamsValidator(),
            true
        ).setInputXmlLabels("protein, C-terminal residue modification mass"));

        _params.add(new SequestParam(
            330,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_Nterm_peptide",                                // the sequest.params property name
            "added to each peptide N-terminus",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            340,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_Nterm_protein",                                // the sequest.params property name
            "added to each protein N-terminus",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            ParamsValidatorFactory.getRealNumberParamsValidator(),
            true
        ).setInputXmlLabels().setInputXmlLabels("protein, N-terminal residue modification mass"));


        _params.add(new SequestParam(
            350,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("G", "Glycine"),                                // the sequest.params property name
            "added to G - avg.  57.0519, mono.  57.02146",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            360,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("A", "Alanine"),                                // the sequest.params property name
            "added to A - avg.  71.0788, mono.  71.03711",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            370,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("S", "Serine"),                                // the sequest.params property name
            "added to S - avg.  87.0782, mono.  87.02303",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            380,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("P", "Proline"),                                // the sequest.params property name
            "added to P - avg.  97.1167, mono.  97.05276",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            390,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("V", "Valine"),                                // the sequest.params property name
            "added to V - avg.  99.1326, mono.  99.06841",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            400,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("T", "Threonine"),                                // the sequest.params property name
            "added to T - avg. 101.1051, mono. 101.04768",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            410,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("C", "Cysteine"),                                // the sequest.params property name
            "added to C - avg. 103.1388, mono. 103.00919",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            420,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("L", "Leucine"),                                // the sequest.params property name
            "added to L - avg. 113.1594, mono. 113.08406",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            430,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("I", "Isoleucine"),                                // the sequest.params property name
            "added to I - avg. 113.1594, mono. 113.08406",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            450,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("N", "Asparagine"),                                // the sequest.params property name
            "added to N - avg. 114.1038, mono. 114.04293",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            460,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("O", "Ornithine"),                                // the sequest.params property name
            "added to O - avg. 114.1472, mono  114.07931",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            480,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("D", "Aspartic_Acid"),                                // the sequest.params property name
            "added to D - avg. 115.0886, mono. 115.02694",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            485,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("Q", "Glutamine"),                                // the sequest.params property name
            "added to Q - avg. 128.1307, mono. 128.05858",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            490,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("K", "Lysine"),                                // the sequest.params property name
            "added to K - avg. 128.1741, mono. 128.09496",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            510,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("E", "Glutamic_Acid"),                                // the sequest.params property name
            "added to E - avg. 129.1155, mono. 129.04259",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            520,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("M", "Methionine"),                                // the sequest.params property name
            "added to M - avg. 131.1926, mono. 131.04049",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            530,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("H", "Histidine"),                                // the sequest.params property name
            "added to H - avg. 137.1411, mono. 137.05891",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            540,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("F", "Phenylalanine"),                                // the sequest.params property name
            "added to F - avg. 147.1766, mono. 147.06841",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            550,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("R", "Arginine"),                                // the sequest.params property name
            "added to R - avg. 156.1875, mono. 156.10111",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            560,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("Y", "Tyrosine"),                                // the sequest.params property name
            "added to Y - avg. 163.1760, mono. 163.06333",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            570,                                                       //sortOrder
            "0.0",                                            //The value of the property
            _variant.getAAStaticModParamName("W", "Tryptophan"),                                // the sequest.params property name
            "added to W - avg. 186.2132, mono. 186.07931",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));
    }

    public Collection<SequestParam> getPassThroughs()
    {
        ArrayList<SequestParam> passThroughs = new ArrayList<>();
        for (Param prop : _params)
        {
            SequestParam castProp = (SequestParam) prop;
            if (castProp.isPassThrough()) passThroughs.add(castProp);
        }
        return passThroughs;
    }

    public Param getFASTAParam()
    {
        return getParam(_variant.getFastaDatabase());
    }
}
