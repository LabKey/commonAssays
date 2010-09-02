package org.labkey.nab;

import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.study.DilutionCurve;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.view.NavTree;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        Collection<Integer> ids = NabManager.get().getReadableStudyObjectIds(getViewContext().getContainer(), getViewContext().getUser(), graphForm.getId());
        ExpProtocol protocol = ExperimentService.get().getExpProtocol(graphForm.getProtocolId());
        NabAssayProvider provider = (NabAssayProvider) AssayService.get().getProvider(protocol);
        Map<DilutionSummary, NabAssayRun> summaries = provider.getDataHandler().getDilutionSummaries(getViewContext().getUser(), graphForm.getFitTypeEnum(), toArray(ids));
        Set<Integer> cutoffSet = new HashSet<Integer>();
        for (DilutionSummary summary : summaries.keySet())
        {
            for (int cutoff : summary.getAssay().getCutoffs())
                cutoffSet.add(cutoff);
        }
        NabGraph.renderChartPNG(getViewContext().getResponse(), summaries, toArray(cutoffSet), false,
                graphForm.getCaptionColumn(), graphForm.getChartTitle(), graphForm.getHeight(), graphForm.getWidth());
        return null;
    }

    public NavTree appendNavTrail(NavTree root)
    {
        throw new UnsupportedOperationException();
    }
}
