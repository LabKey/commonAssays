/*
 * Copyright (c) 2014 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.security.User;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.WellGroup;
import org.labkey.api.study.actions.AssayRunUploadForm;
import org.labkey.api.study.actions.UploadWizardAction;
import org.labkey.api.study.assay.PlateSamplePropertyHelper;
import org.labkey.api.study.assay.SampleMetadataInputFormat;
import org.labkey.api.view.InsertView;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by klum on 7/6/2014.
 */
public class NabVirusPropertyHelper extends PlateSamplePropertyHelper
{
    private SampleMetadataInputFormat _metadataInputFormat;

    public NabVirusPropertyHelper(List<? extends DomainProperty> virusDomainProperties, PlateTemplate template, SampleMetadataInputFormat metadataInputFormat)
    {
        super(virusDomainProperties, template, WellGroup.Type.VIRUS);
        _metadataInputFormat = metadataInputFormat;
    }

    public Map<String, Map<DomainProperty, String>> getSampleProperties(HttpServletRequest request) throws ExperimentException
    {
        if (hasMultipleViruses())
        {
            return super.getSampleProperties(request);
        }
        else
        {
            // because of the optimization to display a single virus in the insert view, we need to
            // pull the posted values from the request instead of using the property helper
            String virusGroupName = (_sampleNames.size() == 1) ? _sampleNames.get(0) : null;

            Map<String, Map<DomainProperty, String>> result = new LinkedHashMap<>();
            Map<DomainProperty, String> sampleProperties = new HashMap<>();
            for (DomainProperty dp : getDomainProperties())
            {
                if (dp.isShownInInsertView())
                {
                    String inputName = UploadWizardAction.getInputName(dp, null);
                    sampleProperties.put(dp, request.getParameter(inputName));
                }
            }
            result.put(virusGroupName, sampleProperties);
            return result;
        }
    }

    public boolean hasMultipleViruses()
    {
        return _sampleNames.size() > 1;
    }

    @Override
    public void addSampleColumns(InsertView view, User user, @Nullable AssayRunUploadForm defaultValueContext, boolean errorReshow) throws ExperimentException
    {
        // In the case where the virus properties will be included with the sample metadata file upload (see PlateSampleFilePropertyHelper.addSampleColumns)
        // we only want to show the virus property input fields for a single virus
        if (hasMultipleViruses() && SampleMetadataInputFormat.MANUAL == _metadataInputFormat)
            super.addSampleColumns(view, user, defaultValueContext, errorReshow);
        else if (!hasMultipleViruses())
        {
            for (DomainProperty dp : getDomainProperties())
            {
                if (dp.isShownInInsertView())
                {
                    ColumnInfo info = dp.getPropertyDescriptor().createColumnInfo(ExperimentService.get().getTinfoExperimentRun(), "lsid", user, view.getViewContext().getContainer());
                    view.getDataRegion().addColumn(info);
                }
            }
        }
    }
}
