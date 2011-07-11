/*
 * Copyright (c) 2011 LabKey Corporation
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

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Jun 16, 2011
 */
public class UWSequestParamsBuilder extends SequestParamsBuilder
{
    public UWSequestParamsBuilder(Map<String, String> sequestInputParams, File sequenceRoot)
    {
        super(sequestInputParams, sequenceRoot);
    }

    public UWSequestParamsBuilder(Map<String, String> sequestInputParams, File sequenceRoot, SequestParams.Variant variant, List<File> databaseFiles)
    {
        super(sequestInputParams, sequenceRoot, variant, databaseFiles);
    }

    @Override
    public String getSequestParamsText() throws SequestParamsException
    {
        String result = super.getSequestParamsText();
        // TODO - insert correct enzyme index
        return result  + "enzyme_number = 1\n" +
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
}
