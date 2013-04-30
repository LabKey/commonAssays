package org.labkey.microarray.view;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.exp.api.ExpMaterial;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;

import java.util.Set;

public class SampleDisplayColumn extends DataColumn
{
    public SampleDisplayColumn(ColumnInfo col)
    {
        super(col);
    }

    @Override
    public String renderURL(RenderContext ctx)
    {
        Integer sampleId = (Integer) ctx.get("sampleid");
        if(sampleId == null)
            return null;

        ExpMaterial sample = ExperimentService.get().getExpMaterial(sampleId);
        ActionURL url = PageFlowUtil.urlProvider(ExperimentUrls.class).getMaterialDetailsURL(sample);

        return url.toString();
    }

    @Override
    public void addQueryFieldKeys(Set<FieldKey> keys)
    {
        super.addQueryFieldKeys(keys);
        FieldKey sampleIdFieldKey = new FieldKey(getBoundColumn().getFieldKey().getParent(), "SampleId");
        keys.add(sampleIdFieldKey);
    }
}
