package org.labkey.microarray.sampleset.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.labkey.microarray.sampleset.client.model.GWTSampleSet;

/**
 * User: jeckels
 * Date: Feb 8, 2008
 */
public interface SampleSetServiceAsync
{
    void getSampleSets(AsyncCallback async);

    void getMaterials(GWTSampleSet sampleSet, AsyncCallback async);
}
