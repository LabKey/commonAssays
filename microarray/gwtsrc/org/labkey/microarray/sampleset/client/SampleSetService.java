package org.labkey.microarray.sampleset.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.core.client.GWT;
import org.labkey.microarray.sampleset.client.model.GWTSampleSet;
import org.labkey.microarray.sampleset.client.model.GWTMaterial;
import org.labkey.api.gwt.client.util.ServiceUtil;

/**
 * User: jeckels
 * Date: Feb 8, 2008
 */
public interface SampleSetService extends RemoteService
{
    public GWTSampleSet[] getSampleSets();

    public GWTMaterial[] getMaterials(GWTSampleSet sampleSet);

    /**
     * Utility/Convenience class.
     * Use SampleSetService.App.getInstance() to access static instance of SampleSetServiceAsync
     */
    public static class App
    {
        private static SampleSetServiceAsync _service = null;

        public static SampleSetServiceAsync getService()
        {
            if (_service == null)
            {
                _service = (SampleSetServiceAsync) GWT.create(SampleSetService.class);
                ServiceUtil.configureEndpoint(_service, "sampleSetService");
            }
            return _service;
        }
    }
}
