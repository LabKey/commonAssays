/*
 * Copyright (c) 2010-2014 LabKey Corporation
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
package org.labkey.nab;

import org.labkey.api.assay.dilution.DilutionManager;
import org.labkey.api.assay.plate.Plate;
import org.labkey.api.assay.plate.PlateType;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.assay.plate.PlateService;

import java.util.List;

/**
 * User: brittp
 * Date: Sep 2, 2010 11:10:01 AM
 */
public class AbstractNabManager extends DilutionManager
{
    public static final String DEFAULT_TEMPLATE_NAME = "NAb: 5 specimens in duplicate";

    public synchronized Plate ensurePlateTemplate(Container container, User user) throws Exception
    {
        NabPlateLayoutHandler nabHandler = new NabPlateLayoutHandler();
        Plate template;
        List<Plate> templates = PlateService.get().getPlateTemplates(container);
        if (templates.isEmpty())
        {
            PlateType plateType = PlateService.get().getPlateType(8, 12);
            if (plateType != null)
            {
                template = nabHandler.createTemplate(NabPlateLayoutHandler.SINGLE_PLATE_TYPE, container, plateType);
                template.setName(DEFAULT_TEMPLATE_NAME);
                PlateService.get().save(container, user, template);
            }
            else
                throw new IllegalStateException("The plate type : 96 wells (8 x 12) does not exist");
        }
        else
            template = templates.get(0);
        return template;
    }
}
