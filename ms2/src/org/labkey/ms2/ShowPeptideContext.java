package org.labkey.ms2;

import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.util.Formats;

/**
 * User: jeckels
* Date: Feb 6, 2007
*/
public class ShowPeptideContext
{
    public MS2Controller.DetailsForm form;
    public MS2Run run;
    public final Container container;
    public final User user;
    public MS2Fraction fraction;
    public MS2Peptide peptide;
    public ViewURLHelper url;
    public ViewURLHelper previousUrl;
    public ViewURLHelper nextUrl;
    public ViewURLHelper showGzUrl;
    public String actualXStart;
    public String actualXEnd;
    public String modificationHref;

    ShowPeptideContext(MS2Controller.DetailsForm form, MS2Run run, MS2Peptide peptide, ViewURLHelper url, ViewURLHelper previousUrl, ViewURLHelper nextUrl, ViewURLHelper showGzUrl, String modHref, Container container, User user)
    {
        this.form = form;
        this.run = run;
        this.container = container;
        this.user = user;
        this.fraction = MS2Manager.getFraction(peptide.getFraction());
        this.peptide = peptide;
        this.url = url;
        this.previousUrl = previousUrl;
        this.nextUrl = nextUrl;
        this.showGzUrl = showGzUrl;
        this.modificationHref = modHref;

        calcXRange();
    }


    private void calcXRange()
    {
        SpectrumGraph graph = new SpectrumGraph(peptide, form.getWidth(), form.getHeight(), form.getTolerance(), form.getxStart(),  form.getxEnd());

        actualXStart = Formats.f0.format(graph.getXStart());
        actualXEnd = Formats.f0.format(graph.getXEnd());
    }
}
