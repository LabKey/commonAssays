package org.labkey.ms2.pipeline.client;

import org.labkey.api.gwt.client.pipeline.GWTPipelineConfig;

/**
 * User: jeckels
 * Date: Feb 1, 2012
 */
public interface PipelineConfigCallback
{
    public void setPipelineConfig(GWTPipelineConfig result);
}
