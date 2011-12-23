/*
 * Copyright (c) 2010-2011 LabKey Corporation
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

import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: brittp
 * Date: Apr 21, 2010 12:46:56 PM
 */

@RequiresPermissionClass(ReadPermission.class)
public class StudyNabGraphAction extends SimpleViewAction<NabAssayController.GraphSelectedForm>
{
    private int[] toArray(Collection<Integer> integerList)
    {
        int[] arr = new int[integerList.size()];
        int i = 0;
        for (Integer cutoff : integerList)
            arr[i++] = cutoff.intValue();
        return arr;
    }

    @Override
    public ModelAndView getView(NabAssayController.GraphSelectedForm graphForm, BindException errors) throws Exception
    {
        Map<Integer, ExpProtocol> ids = NabManager.get().getReadableStudyObjectIds(getViewContext().getContainer(), getViewContext().getUser(), graphForm.getId());
        if (ids.values().isEmpty())
            throw new NotFoundException("No IDs available for charting.");
        // We don't care which protocol we get- we just need any valid protocol to get to the provider (which should be
        // the same for all IDs)
        NabAssayProvider provider = null;
        for (ExpProtocol protocol : ids.values())
        {
            if (provider == null)
                provider = (NabAssayProvider) AssayService.get().getProvider(protocol);
            else
            {
                NabAssayProvider currentProvider = (NabAssayProvider) AssayService.get().getProvider(protocol);
                if (!provider.getName().equals(currentProvider.getName()))
                    throw new IllegalStateException("Cannot graph data from different providers on the same chart");
            }
        }
        Map<DilutionSummary, NabAssayRun> summaries = provider.getDataHandler().getDilutionSummaries(getViewContext().getUser(), graphForm.getFitTypeEnum(), toArray(ids.keySet()));
        Set<Integer> cutoffSet = new HashSet<Integer>();
        for (DilutionSummary summary : summaries.keySet())
        {
            for (int cutoff : summary.getAssay().getCutoffs())
                cutoffSet.add(cutoff);
        }
        NabGraph.Config config = new NabGraph.Config();
        config.setCutoffs(toArray(cutoffSet));
        config.setCaptionColumn(graphForm.getCaptionColumn());
        config.setChartTitle(graphForm.getChartTitle());
        if (graphForm.getHeight() > 0)
            config.setHeight(graphForm.getHeight());
        if (graphForm.getWidth() > 0)
            config.setWidth(graphForm.getWidth());
        NabGraph.renderChartPNG(getViewContext().getResponse(), summaries, config);
        return null;
    }

    public NavTree appendNavTrail(NavTree root)
    {
        throw new UnsupportedOperationException();
    }
}
