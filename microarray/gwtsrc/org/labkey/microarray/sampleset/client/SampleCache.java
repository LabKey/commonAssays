/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.microarray.sampleset.client;

import org.labkey.microarray.sampleset.client.model.GWTSampleSet;
import org.labkey.microarray.sampleset.client.model.GWTMaterial;
import org.labkey.api.gwt.client.util.ExceptionUtil;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * User: jeckels
 * Date: Feb 8, 2008
 */
public class SampleCache
{
    /** String (SampleSet LSID) -> GWTSampleSet */
    private Map _sampleSets = new HashMap();
    /** String (SampleSet LSID) -> GWTMaterial[] */
    private Map _sampleSetMembers = new HashMap();

    private SampleInfo[] _sampleInfos;

    public SampleCache(SampleInfo[] sampleInfos)
    {
        _sampleInfos = sampleInfos;

        _sampleSets.put(SampleChooser.NONE_SAMPLE_SET.getLsid(), SampleChooser.NONE_SAMPLE_SET);

        SampleSetServiceAsync service = SampleSetService.App.getService();
        service.getSampleSets(new AsyncCallback()
        {
            public void onFailure(Throwable caught)
            {
                ExceptionUtil.showDialog(caught);
            }

            public void onSuccess(Object result)
            {
                GWTSampleSet[] sets = (GWTSampleSet[])result;

                for (int i = 0; i < sets.length; i++)
                {
                    _sampleSets.put(sets[i].getLsid(), sets[i]);
                }
                for (int i = 0; i < _sampleInfos.length; i++)
                {
                    _sampleInfos[i].updateSampleSets(sets);
                }
            }
        });
        
    }

    public GWTSampleSet getSampleSet(String lsid)
    {
        return (GWTSampleSet)_sampleSets.get(lsid);
    }

    public GWTMaterial[] getMaterials(final GWTSampleSet sampleSet)
    {
        if (!_sampleSetMembers.containsKey(sampleSet.getLsid()))
        {
            _sampleSetMembers.put(sampleSet.getLsid(), null);
            SampleSetService.App.getService().getMaterials(sampleSet, new AsyncCallback()
            {
                public void onFailure(Throwable caught)
                {
                    ExceptionUtil.showDialog(caught);
                }

                public void onSuccess(Object result)
                {
                    GWTMaterial[] materials = (GWTMaterial[]) result;
                    _sampleSetMembers.put(sampleSet.getLsid(), materials);

                    for (int i = 0; i < _sampleInfos.length; i++)
                    {
                        _sampleInfos[i].updateMaterials(sampleSet, materials);
                    }
                }
            });
        }
        return (GWTMaterial[])_sampleSetMembers.get(sampleSet.getLsid());
    }

    public void addSampleSet(GWTSampleSet sampleSet)
    {
        _sampleSets.put(sampleSet.getLsid(), sampleSet);
    }
}
