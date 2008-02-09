package org.labkey.microarray;

import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.view.ViewContext;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.security.ACL;
import org.labkey.microarray.sampleset.client.SampleSetService;
import org.labkey.microarray.sampleset.client.model.GWTSampleSet;
import org.labkey.microarray.sampleset.client.model.GWTMaterial;

/**
 * User: jeckels
 * Date: Feb 8, 2008
 */
public class SampleSetServiceImpl extends BaseRemoteService implements SampleSetService
{
    public SampleSetServiceImpl(ViewContext context)
    {
        super(context);
    }

    public GWTSampleSet[] getSampleSets()
    {
        ExpSampleSet[] sets = ExperimentService.get().getSampleSets(getContainer(), true);
        GWTSampleSet[] result = new GWTSampleSet[sets.length];
        for (int i = 0; i < sets.length; i++)
        {
            ExpSampleSet set = sets[i];
            GWTSampleSet gwtSet = new GWTSampleSet(set.getName(), set.getLSID());
            gwtSet.setRowId(set.getRowId());
            result[i] = gwtSet;
        }
        return result;
    }

    public GWTMaterial[] getMaterials(GWTSampleSet gwtSet)
    {
        ExpSampleSet set = ExperimentService.get().getSampleSet(gwtSet.getRowId());
        if (set == null)
        {
            return null;
        }

        if (!set.getContainer().hasPermission(getUser(), ACL.PERM_READ))
        {
            return null;
        }

        ExpMaterial[] materials = set.getSamples();
        GWTMaterial[] result = new GWTMaterial[materials.length];
        for (int i = 0; i < materials.length; i++)
        {
            ExpMaterial material = materials[i];
            GWTMaterial gwtMaterial = new GWTMaterial();
            result[i] = gwtMaterial;
            gwtMaterial.setLsid(material.getLSID());
            gwtMaterial.setRowId(material.getRowId());
            gwtMaterial.setName(material.getName());
        }
        return result;
    }
}
