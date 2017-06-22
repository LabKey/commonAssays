/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.module.MultiPortalFolderType;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.Portal;
import org.labkey.api.view.WebPartFactory;
import org.labkey.ms2.protein.ProteinController;
import org.labkey.ms2.search.ProteinSearchWebPart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MS2FolderType extends MultiPortalFolderType
{
    public MS2FolderType(MS2Module module)
    {
        //TODO: Get rid of these strings.. Should be part of some service
        super("MS2",
                "Manage tandem mass spectrometry analyses using a variety of popular search engines, " +
                        "including Mascot, Sequest, and X!Tandem. " +
                        "Use existing analytic tools like PeptideProphet and ProteinProphet.",
            Arrays.asList(
                Portal.getPortalPart("Data Pipeline").createWebPart(),
                Portal.getPortalPart(MS2Module.MS2_RUNS_NAME).createWebPart()
            ),
            buildPreferredWebPartsList(),
            getDefaultModuleSet(module, getModule("MS1"), getModule("Pipeline"), getModule("Experiment")),
            module);
    }

    @NotNull
    private static List<Portal.WebPart> buildPreferredWebPartsList()
    {
        List<Portal.WebPart> webParts = new ArrayList<>(Arrays.asList(
                Portal.getPortalPart(ProteinSearchWebPart.NAME).createWebPart(),
                Portal.getPortalPart(MS2Module.MS2_SAMPLE_PREPARATION_RUNS_NAME).createWebPart(),
                Portal.getPortalPart("Run Groups").createWebPart(),
                Portal.getPortalPart("Run Types").createWebPart(),
                Portal.getPortalPart("Sample Sets").createWebPart(),
                Portal.getPortalPart("Protocols").createWebPart()
        ));
        WebPartFactory assayList = Portal.getPortalPart("Assay List");
        if (assayList != null)  // Study module might not be present, #29772
        {
            webParts.add(assayList.createWebPart());
        }

        return webParts;
    }

    @Override
    public HelpTopic getHelpTopic()
    {
        return new HelpTopic("ms2");
    }

    @Override
    public void addManageLinks(NavTree adminNavTree, Container container, User user)
    {
        super.addManageLinks(adminNavTree, container, user);

        if (container.hasPermission(user, ReadPermission.class))
            adminNavTree.addChild(new NavTree("Manage Custom Protein Lists", ProteinController.getBeginURL(container)));
    }
}
