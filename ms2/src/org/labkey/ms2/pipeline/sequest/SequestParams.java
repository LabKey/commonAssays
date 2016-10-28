/*
 * Copyright (c) 2006-2016 LabKey Corporation
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

/**
 * User: billnelson@uky.edu
 * Date: Sep 12, 2006
 * Time: 11:30:14 AM
 */
public class SequestParams extends AbstractSequestParams
{
    public SequestParams(Variant variant)
    {
        super(variant);
    }

    protected void initProperties()
    {
        super.initProperties();

        _params.add(new SequestParam(
            10,                            //sortOrder
            _variant.getHeader(),              //The value of the property
            "sequest header",              // the sequest.params property name
            "",                            // the sequest.params comment
            ConverterFactory.getSequestHeaderConverter(),  //converts the instance to a sequest.params line
            null,
            false
        ));

        /* specifies the ion series to be analyzed. The first 3 parameters of that line are integers (0 or 1) that
represents whether or not neutral losses (NH3 and H2)) for a-ions, b-ions and y-ions are considered (0=no, 1=yes)
in the correlation analysis. The last 9 parameters are floating point values representing a, b, c, d, v, w, x, y,
and z ions respectively. The values entered for these parameters should range from 0.0 (don't use the ion series)
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

        _params.add(new SequestParam(
            230,                                                       //sortOrder
            "2",                                            //The value of the property
            "max_num_internal_cleavage_sites",                                // the sequest.params property name
            "maximum value is 5",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            ParamsValidatorFactory.getPositiveIntegerParamsValidator(),
            true
        ).setInputXmlLabels(AbstractMS2SearchTask.MAXIMUM_MISSED_CLEAVAGE_SITES));

        addProperty(new SequestParam(
                160,                                                       //sortOrder
                "0.0",                                            //The value of the property
                "variable_C_terminus",                                // the sequest.params property name
                "Apply this mass optional modification to the C-term of each peptide",                                                       // the sequest.params comment
                ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
                null,
                false
        ));

        addProperty(new SequestParam(
                161,                                                       //sortOrder
                "0.0",                                            //The value of the property
                "variable_N_terminus",                                // the sequest.params property name
                "Apply this mass optional modification to the N-term of each peptide",                                                       // the sequest.params comment
                ConverterFactory.getSequestBasicConverter(),                              //converts the instance to a sequest.params line
                null,
                false
        ));

        addProperty(new SequestParam(
                262,                                                       //sortOrder
                "-1",                                            //The value of the property
                "variable_C_terminus_distance",                                // the sequest.params property name
                "Apply based on distance from protein terminus. -1=all, N=no more than N residues from the protein terminus",       // the sequest.params comment
                ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                new RealNumberParamsValidator(),
                true
        )).setInputXmlLabels(_variant.getParamPrefix() + ", variable_C_terminus_distance" );

        addProperty(new SequestParam(
                263,                                                       //sortOrder
                "-1",                                            //The value of the property
                "variable_N_terminus_distance",                                // the sequest.params property name
                "Apply based on distance from protein terminus. -1=all, N=no more than N residues from the protein terminus",       // the sequest.params comment
                ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
                new RealNumberParamsValidator(),
                true
        )).setInputXmlLabels(_variant.getParamPrefix() + ", variable_N_terminus_distance" );

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
            440,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_X_LorI",                                // the sequest.params property name
            "added to X - avg. 113.1594, mono. 113.08406",       // the sequest.params comment
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
            500,                                                       //sortOrder
            "0.0",                                            //The value of the property
            "add_Z_avg_QandE",                                // the sequest.params property name
            "added to Z - avg. 128.6231, mono. 128.55059",       // the sequest.params comment
            ConverterFactory.getSequestBasicConverter(),                      //converts the instance to a sequest.params line
            null,
            false
        ).setInputXmlLabels().setInputXmlLabels(ParameterNames.STATIC_MOD));

    }
}
