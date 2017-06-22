/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
package org.labkey.viability;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.assay.viability.ViabilityService;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProvider;

/**
 * User: kevink
 * Date: 6/23/14
 */
public class ViabilityServiceImpl implements ViabilityService
{
    private static final ViabilityServiceImpl INSTANCE = new ViabilityServiceImpl();

    public static ViabilityService get()
    {
        return INSTANCE;
    }

    private ViabilityServiceImpl() { }

    @Override
    public void updateSpecimenAggregates(User user, Container c, AssayProvider provider, ExpProtocol protocol, @Nullable ExpRun run)
    {
        ViabilityManager.updateSpecimenAggregates(user, c, provider, protocol, run);
    }
}
