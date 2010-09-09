package org.labkey.nab;

import org.labkey.api.data.Container;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.security.User;
import org.labkey.api.study.PlateService;
import org.labkey.api.study.PlateTemplate;

import java.sql.SQLException;

/**
 * Copyright (c) 2010 LabKey Corporation
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * <p/>
 * User: brittp
 * Date: Sep 2, 2010 11:10:01 AM
 */
public class AbstractNabManager
{
    public static final String DEFAULT_TEMPLATE_NAME = "NAb: 5 specimens in duplicate";
    public static final String CELL_CONTROL_SAMPLE = "CELL_CONTROL_SAMPLE";
    public static final String VIRUS_CONTROL_SAMPLE = "VIRUS_CONTROL_SAMPLE";

    public enum SampleProperty
    {
        InitialDilution(PropertyType.DOUBLE, false),
        SampleId(PropertyType.STRING, false),
        SampleDescription(PropertyType.STRING, false),
        Factor(PropertyType.DOUBLE, false),
        Method(PropertyType.STRING, false),
        ReverseDilutionDirection(PropertyType.BOOLEAN, true),
        FitError(PropertyType.DOUBLE, false);

        private PropertyType _type;
        private boolean _isTemplateProperty;

        private SampleProperty(PropertyType type, boolean setInTemplateEditor)
        {
            _type = type;
            _isTemplateProperty = setInTemplateEditor;
        }

        public PropertyType getType()
        {
            return _type;
        }

        public boolean isTemplateProperty()
        {
            return _isTemplateProperty;
        }
    }

    public synchronized PlateTemplate ensurePlateTemplate(Container container, User user) throws SQLException
    {
        NabPlateTypeHandler nabHandler = new NabPlateTypeHandler();
        PlateTemplate template;
        PlateTemplate[] templates = PlateService.get().getPlateTemplates(container);
        if (templates == null || templates.length == 0)
        {
            template = nabHandler.createPlate(NabPlateTypeHandler.SINGLE_PLATE_TYPE, container, 8, 12);
            template.setName(DEFAULT_TEMPLATE_NAME);
            PlateService.get().save(container, user, template);
        }
        else
            template = templates[0];
        return template;
    }
}
