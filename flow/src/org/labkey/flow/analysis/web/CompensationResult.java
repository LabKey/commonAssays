package org.labkey.flow.analysis.web;

import org.labkey.flow.analysis.web.FCSAnalyzer;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.net.URI;

public class CompensationResult
{
    URI uri;
    CompSign sign;
    String channelName;

    List<FCSAnalyzer.Result> results;

    public CompensationResult(CompSign sign, String channelName, URI uri)
    {
        this.sign = sign;
        this.channelName = channelName;
        this.uri = uri;
        this.results = new ArrayList();
    }

    public CompSign getSign()
    {
        return sign;
    }

    public String getChannelName()
    {
        return channelName;
    }

    public List<FCSAnalyzer.Result> getResults()
    {
        return Collections.unmodifiableList(results);
    }

    public void addResult(FCSAnalyzer.Result result)
    {
        results.add(result);
    }

    public URI getURI()
    {
        return uri;
    }
}
