/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
package org.labkey.nab.multiplate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.security.User;
import org.labkey.api.assay.query.ResultsQueryView;
import org.labkey.api.assay.query.RunListQueryView;
import org.labkey.api.assay.dilution.DilutionDataHandler;
import org.labkey.api.view.ViewContext;
import org.labkey.nab.query.NabProtocolSchema;
import org.springframework.validation.BindException;

import java.util.HashMap;
import java.util.Map;

/**
 * User: klum
 * Date: 2/24/13
 */
public class CrossPlateDilutionNabAssayProvider extends HighThroughputNabAssayProvider
{
    private static final String NAB_RUN_LSID_PREFIX = "HighThroughputNabAssayRun";
    private static final String NAB_ASSAY_PROTOCOL = "HighThroughputNabAssayProtocol";

    public CrossPlateDilutionNabAssayProvider()
    {
        super(NAB_ASSAY_PROTOCOL, NAB_RUN_LSID_PREFIX, null, CrossPlateDilutionNabDataHandler.NAB_HIGH_THROUGHPUT_DATA_TYPE);
    }

    @Override
    public String getName()
    {
        return "TZM-bl Neutralization (NAb), High-throughput (Cross Plate Dilution)";
    }

    @Override
    public String getResourceName()
    {
        return "HighThroughputNAb";
    }

    @Override
    public String getDescription()
    {
        return "Imports a specially formatted CSV or XLS file that contains data from multiple plates.  The high-throughput NAb " +
                "assay differs from the standard NAb assay in that dilutions are assumed to occur across plates, rather than " +
                "within a single plate.  Both NAb assay types measure neutralization in TZM-bl cells as a function of a " +
                "reduction in Tat-induced luciferase (Luc) reporter gene expression after a single round of infection. Montefiori, D.C. 2004";
    }

    @Override
    public DilutionDataHandler getDataHandler()
    {
        return new CrossPlateDilutionNabDataHandler();
    }

    @Override
    public NabProtocolSchema createProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        return generateAlternateProtocolSchema(user, container, protocol, targetStudy);
    }
}
