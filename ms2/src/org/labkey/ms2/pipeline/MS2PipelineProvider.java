/*
 * Copyright (c) 2007-2019 LabKey Corporation
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
package org.labkey.ms2.pipeline;

import org.labkey.api.data.Container;
import org.labkey.api.module.Module;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineDirectory;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.LinkBuilder;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.api.writer.HtmlWriter;

import java.util.Arrays;

import static org.labkey.api.util.DOM.Attribute.style;
import static org.labkey.api.util.DOM.TABLE;
import static org.labkey.api.util.DOM.TD;
import static org.labkey.api.util.DOM.TR;
import static org.labkey.api.util.DOM.at;

public class MS2PipelineProvider extends PipelineProvider
{
    static String name = "MS2";

    public MS2PipelineProvider(Module owningModule)
    {
        super(name, owningModule);
    }

    @Override
    public HttpView<Object> getSetupWebPart(Container container)
    {
        return new SetupWebPart();
    }

    @Override
    public void updateFileProperties(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
        if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
        {
            return;
        }

        String actionId = createActionId(PipelineController.UploadAction.class, "Import Search Results");
        addAction(actionId, PipelineController.UploadAction.class, "Import Search Results",
                directory, directory.listPaths(MS2PipelineManager.getUploadFilter()), true, true, includeAll);
    }

    static class SetupWebPart extends WebPartView<Object>
    {
        public SetupWebPart()
        {
            super(FrameType.DIV);
        }

        @Override
        protected void renderView(Object model, HtmlWriter out)
        {
            ViewContext context = getViewContext();
            ActionURL buttonURL = new ActionURL(PipelineController.SetupClusterSequenceDBAction.class, context.getContainer());
            renderSettings(context, "MS2", out, new Setting(buttonURL, "Set FASTA root", "Specify the location on the web server where FASTA sequence files will be located."));
        }
    }

    public record Setting(ActionURL url, String text, String description)
    {
        // Standard "Set defaults" setting
        public Setting(ActionURL url, String name)
        {
            this(url, "Set defaults", "Specify the default XML parameters file for " + name + ".");
        }
    }

    public static void renderSettings(ViewContext context, String name, HtmlWriter out, Setting... settings)
    {
        if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
            return;

        TABLE(
            TR(TD(
                at(style, "font-weight:bold;"),
                name + "-specific settings:"
            )),
            Arrays.stream(settings)
                .map(setting ->
                    TR(TD(
                        HtmlString.NBSP,
                        HtmlString.NBSP,
                        HtmlString.NBSP,
                        HtmlString.NBSP,
                        LinkBuilder.simpleLink(setting.text(), setting.url()),
                        " - " + setting.description()
                    ))
                )
        ).appendTo(out);
    }
}
