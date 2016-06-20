/*
 * Copyright (c) 2015-2016 LabKey Corporation
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
package org.labkey.ms2;

import org.apache.commons.io.FilenameUtils;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.ExpData;

/**
 * Created by susanh on 10/27/15.
 */
public class MascotDatExperimentDataHandler extends PepXmlExperimentDataHandler
{
    private static final String IMPORT_DAT_RESULTS = "mascot, import dat results";

    @Override
    protected boolean shouldImport(ExpData data, XarContext context)
    {
        return context.getJob() == null || !"false".equalsIgnoreCase(context.getJob().getParameters().get(IMPORT_DAT_RESULTS));
    }

    public Priority getPriority(ExpData data)
    {
        if (data != null && data.getFile() != null && FilenameUtils.isExtension(data.getFile().getName(), "dat"))
        {
            return Priority.HIGH;
        }
        return null;
    }
}
