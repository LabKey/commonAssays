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
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.PlateTemplate;

import java.sql.SQLException;
import java.util.List;

/**
 * User: brittp
 * Date: Sep 2, 2010 11:10:01 AM
 */
public class AbstractNabManager extends DilutionManager
{
    public static final String DEFAULT_TEMPLATE_NAME = "NAb: 5 specimens in duplicate";

    public synchronized PlateTemplate ensurePlateTemplate(Container container, User user) throws SQLException
    {
        NabPlateTypeHandler nabHandler = new NabPlateTypeHandler();
        PlateTemplate template;
        List<? extends PlateTemplate> templates = PlateService.get().getPlateTemplates(container);
        if (templates.isEmpty())
        {
            template = nabHandler.createPlate(NabPlateTypeHandler.SINGLE_PLATE_TYPE, container, 8, 12);
            template.setName(DEFAULT_TEMPLATE_NAME);
            PlateService.get().save(container, user, template);
        }
        else
            template = templates.get(0);
        return template;
    }
}
