/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
package org.labkey.ms2.protein;

import org.labkey.api.protein.CoverageViewBean;
import org.labkey.ms2.MS2Run;

public class ProteinViewBean extends CoverageViewBean
{
    public static final String ALL_PEPTIDES_URL_PARAM = "allPeps";

    public MS2Run run = null;
    public Protein protein;
    public boolean showPeptides;
    public boolean enableAllPeptidesFeature;
}
