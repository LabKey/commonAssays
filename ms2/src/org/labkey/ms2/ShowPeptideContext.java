/*
 * Copyright (c) 2007-2011 LabKey Corporation
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

package org.labkey.ms2;

import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.api.ms1.MS1Urls;
import org.labkey.api.services.ServiceRegistry;

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
    public ActionURL url;
    public ActionURL previousUrl;
    public ActionURL nextUrl;
    public ActionURL showGzUrl;
    public String modificationHref;
    public String pepSearchHref;

    ShowPeptideContext(MS2Controller.DetailsForm form, MS2Run run, MS2Peptide peptide, ActionURL url, ActionURL previousUrl, ActionURL nextUrl, ActionURL showGzUrl, String modHref, Container container, User user)
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

        MS1Urls ms1Urls = ServiceRegistry.get().getService(MS1Urls.class);
        if(null != ms1Urls)
            pepSearchHref = ms1Urls.getPepSearchUrl(container, peptide.getTrimmedPeptide()).getLocalURIString();
    }
}
