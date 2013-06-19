/*
 * Copyright (c) 2006-2013 LabKey Corporation
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

import org.labkey.ms2.pipeline.AbstractMS2SearchTask;
import org.labkey.ms2.pipeline.client.ParameterNames;

import java.util.ArrayList;
import java.util.Collection;

/**
 * User: billnelson@uky.edu
 * Date: Sep 12, 2006
 * Time: 11:30:14 AM
 */
public class SequestParams extends Params
{
    private final Variant _variant;

    public enum Variant
    {
        thermosequest("[SEQUEST]", "first_database_name"),
        uwsequest("[SEQUEST]", "database_name"),
        sequest("[SEQUEST]", "database_name"),
        makedb("[MAKEDB]", "database_name");

        private final String _header;
        private final String _fastaDatabase;

        private Variant(String header, String fastaDatabase)
        {
            _header = header;
            _fastaDatabase = fastaDatabase;
        }
    }

    public SequestParams(Variant variant)
    {
        _variant = variant;
        initProperties();
    }

    public SequestParam addProperty(SequestParam param)
    {
        _params.add(param);
        return param;
    }

    void initProperties()
    {
        _params.add(new SequestParam(
            10,                            //sortOrder
            _variant._header,              //The value of the property
            "sequest header",              // the sequest.params property name
            "",                            // the sequest.params comment
            ConverterFactory.getSequestHeaderConverter(),  //converts the instance to a sequest.params line
            null,
            false
        ));

        _params.add(new SequestParam(
            20,
            "",
            _variant._fastaDatabase,
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

        /* specifies the ion series to be analyzed. The first 3 parameters of that line are integers (0 or 1) that
represents whether or not neutral losses (NH3 and H2)) for a-ions, b-ions and y-ions are considered (0=no, 1=yes)
in the correlation analysis. The last 9 parameters are floating point values representing a, b, c, d, v, w, x, y,
and z ions respectively. The values entered for these paramters should range from 0.0 (don't use the ion series)
to 1.0. The value entered represents the weighting that each ion series has (relative to the others). So an ion
series with 0.5 contains half the weighting or relevance of an ion series with a 1.0 parameter.    */

        _params.add(new SequestParam(
            60,                                                       //sortOrder
            "0 1 1 0.0 1.0 0.0 0.0 0.0 0.0 0.0 1.0 0.0",              //The value of the property
            "ion_series",                                             // the sequest.params property name
            "",                                                       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                             //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels("scoring, a ions",
            "scoring, b ions",
            "scoring, c ions",
            "scoring, x ions",
            "scoring, y ions",
            "scoring, z ions",
            "sequest, d ions",
            "sequest, v ions",
            "sequest, w ions",
            "sequest, a neutral loss",
            "sequest, b neutral loss",
            "sequest, y neutral loss"));
        /*The sequest.params comment on this property is   "leave at 0.0 unless you have real poor data"
but bioWorks browser default setting is 1.0. so the xtandem value will be passed through.*/
        _params.add(new SequestParam(
            70,                                                       //sortOrder
            "0.36",                                                    //The value of the property
            "fragment_ion_tolerance",                                 // the sequest.params property name
            "for trap data leave at 1.0, for accurate mass data use values < 1.0",// the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                             //converts the instance to a sequest.params line
            ParamsValidatorFactory.getRealNumberParamsValidator(),
            true
        ).setInputXmlLabels("spectrum, fragment mass error"));


        _params.add(new SequestParam(
            80,                                                     //sortOrder
            "10",                                                   //The value of the property
            "num_output_lines",                                     // the sequest.params property name
            "# peptide results to show",                            // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                            //converts the instance to a sequest.params line
            ParamsValidatorFactory.getNaturalNumberParamsValidator(),
            true
        ).setInputXmlLabels("sequest, num_output_lines"));


        _params.add(new SequestParam(
            90,                                                       //sortOrder
            "50", /** Changed to 50 to match up with UW Sequest default */                                                   //The value of the property
            "num_results",                                            // the sequest.params property name
            "# results to store",                                     // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
            ParamsValidatorFactory.getNaturalNumberParamsValidator(),
            true
        ).setInputXmlLabels("sequest, num_results"));

        //pass through- no Xtandem counterpart
        _params.add(new SequestParam(
            100,                                                       //sortOrder
            "5",                                                      //The value of the property
            "num_description_lines",                                  // the sequest.params property name
            "# full protein descriptions to show for top N peptides", // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
            ParamsValidatorFactory.getNaturalNumberParamsValidator(),
            true
        ).setInputXmlLabels("sequest, num_description_lines"));

        //pass through- no Xtandem counterpart
        _params.add(new SequestParam(
            110,                                                       //sortOrder
            "0",                                                      //The value of the property
            "show_fragment_ions",                                     // the sequest.params property name
            "0=no, 1=yes",                                            // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
            ParamsValidatorFactory.getBooleanParamsValidator(),
            true
        ).setInputXmlLabels("sequest, show_fragment_ions"));

        _params.add(new SequestParam(
            140,                                                       //sortOrder
            "10",     /** Changed to 10 to match up with UW Sequest default */      //The value of the property
            "max_num_differential_per_peptide",                        // the sequest.params property name
            "max # of diff. mod in a peptide",                        // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels("sequest, max_num_differential_per_peptide"));

        _params.add(new SequestParam(
            150,                                                       //sortOrder
            "0.000000 C 0.000000 M 0.000000 S 0.000000 T 0.000000 X 0.000000 Y",                                                        //The value of the property
            "diff_search_options",                                     // the sequest.params property name
            "",                                                       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels(ParameterNames.DYNAMIC_MOD));

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
        ).setInputXmlLabels(SequestSearchTask.MASS_TYPE_PARENT));

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
        ).setInputXmlLabels("sequest, remove_precursor_peak"));

        _params.add(new SequestParam(
            230,                                                       //sortOrder
            "2",                                            //The value of the property
            "max_num_internal_cleavage_sites",                                // the sequest.params property name
            "maximum value is 5",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            ParamsValidatorFactory.getPositiveIntegerParamsValidator(),
            true
        ).setInputXmlLabels(AbstractMS2SearchTask.MAXIMUM_MISSED_CLEAVAGE_SITES));

        if (_variant == Variant.makedb)
        {
            _params.add(new SequestParam(
                300,                                               //sortOrder
                "600.0",                          //The value of the property
                "min_peptide_mass",                     // the sequest.params property name
                "",       // the sequest.params comment
                ConverterFactory.getSequestBasicConverter(),      //converts the instance to a sequest.params line
                null,
                false
            ).setInputXmlLabels(AbstractMS2SearchTask.MINIMUM_PARENT_M_H));

            _params.add(new SequestParam(
                301,                                               //sortOrder
                "4200.0",                          //The value of the property
                "max_peptide_mass",                     // the sequest.params property name
                "",       // the sequest.params comment
                ConverterFactory.getSequestBasicConverter(),      //converts the instance to a sequest.params line
                null,
                false
            ).setInputXmlLabels(AbstractMS2SearchTask.MAXIMUM_PARENT_M_H));

            _params.add(new SequestParam(
                305,                                               //sortOrder
                "[STATIC MODIFICATIONS]",                          //The value of the property
                "static modifications header",                     // the sequest.params property name
                "",       // the sequest.params comment
                ConverterFactory.getSequestHeaderConverter(),      //converts the instance to a sequest.params line
                null,
                false
            ).setInputXmlLabels());
        }

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
            "add_G_Glycine",                                // the sequest.params property name
            "added to G - avg.  57.0519, mono.  57.02146",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            360,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_A_Alanine",                                // the sequest.params property name
            "added to A - avg.  71.0788, mono.  71.03711",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            370,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_S_Serine",                                // the sequest.params property name
            "added to S - avg.  87.0782, mono.  87.02303",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            380,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_P_Proline",                                // the sequest.params property name
            "added to P - avg.  97.1167, mono.  97.05276",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            390,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_V_Valine",                                // the sequest.params property name
            "added to V - avg.  99.1326, mono.  99.06841",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            400,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_T_Threonine",                                // the sequest.params property name
            "added to T - avg. 101.1051, mono. 101.04768",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            410,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_C_Cysteine",                                // the sequest.params property name
            "added to C - avg. 103.1388, mono. 103.00919",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            420,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_L_Leucine",                                // the sequest.params property name
            "added to L - avg. 113.1594, mono. 113.08406",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            430,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_I_Isoleucine",                                // the sequest.params property name
            "added to I - avg. 113.1594, mono. 113.08406",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            440,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_X_LorI",                                // the sequest.params property name
            "added to X - avg. 113.1594, mono. 113.08406",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            450,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_N_Asparagine",                                // the sequest.params property name
            "added to N - avg. 114.1038, mono. 114.04293",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            460,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_O_Ornithine",                                // the sequest.params property name
            "added to O - avg. 114.1472, mono  114.07931",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            470,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_B_avg_NandD",                                // the sequest.params property name
            "added to B - avg. 114.5962, mono. 114.53494",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            480,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_D_Aspartic_Acid",                                // the sequest.params property name
            "added to D - avg. 115.0886, mono. 115.02694",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            485,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_Q_Glutamine",                                // the sequest.params property name
            "added to Q - avg. 128.1307, mono. 128.05858",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            490,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_K_Lysine",                                // the sequest.params property name
            "added to K - avg. 128.1741, mono. 128.09496",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            500,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_Z_avg_QandE",                                // the sequest.params property name
            "added to Z - avg. 128.6231, mono. 128.55059",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            510,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_E_Glutamic_Acid",                                // the sequest.params property name
            "added to E - avg. 129.1155, mono. 129.04259",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            520,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_M_Methionine",                                // the sequest.params property name
            "added to M - avg. 131.1926, mono. 131.04049",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            530,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_H_Histidine",                                // the sequest.params property name
            "added to H - avg. 137.1411, mono. 137.05891",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            540,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_F_Phenylalanine",                                // the sequest.params property name
            "added to F - avg. 147.1766, mono. 147.06841",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            550,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_R_Arginine",                                // the sequest.params property name
            "added to R - avg. 156.1875, mono. 156.10111",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            560,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_Y_Tyrosine",                                // the sequest.params property name
            "added to Y - avg. 163.1760, mono. 163.06333",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

        _params.add(new SequestParam(
            570,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_W_Tryptophan",                                // the sequest.params property name
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
        return getParam(_variant._fastaDatabase);
    }
}
