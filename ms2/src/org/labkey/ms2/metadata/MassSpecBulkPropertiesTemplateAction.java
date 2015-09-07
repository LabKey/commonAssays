/*
 * Copyright (c) 2009-2015 LabKey Corporation
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
package org.labkey.ms2.metadata;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.study.actions.BaseAssayAction;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.util.DateUtil;
import org.labkey.api.view.NotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: May 31, 2009
 */
@RequiresPermission(ReadPermission.class)
public class MassSpecBulkPropertiesTemplateAction extends BaseAssayAction<MassSpecMetadataAssayForm>
{
    public ModelAndView getView(MassSpecMetadataAssayForm form, BindException errors) throws Exception
    {
        ExpProtocol protocol = form.getProtocol(true);

        AssayProvider p = form.getProvider();
        if (!(p instanceof MassSpecMetadataAssayProvider))
        {
            throw new NotFoundException("Could not find mass spec metadata assay provider");
        }
        MassSpecMetadataAssayProvider provider = (MassSpecMetadataAssayProvider)p;

        getViewContext().getResponse().reset();

        // First, set the content-type, so that your browser knows which application to launch
        getViewContext().getResponse().setContentType("application/vnd.ms-excel");
        String filename = protocol.getName() + "Template" + DateUtil.formatDateISO8601() + ".xls";
        getViewContext().getResponse().setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        WritableWorkbook workbook = Workbook.createWorkbook(getViewContext().getResponse().getOutputStream());
        WritableSheet sheet = workbook.createSheet("MicroarrayTemplate", 0);
        int col = 0;

        WritableFont boldFont = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
        WritableCellFormat cellFormat = new WritableCellFormat(boldFont);
        cellFormat.setWrap(false);
        cellFormat.setVerticalAlignment(jxl.format.VerticalAlignment.TOP);

        sheet.addCell(new Label(col++, 0, "Filename", cellFormat));
        sheet.addCell(new Label(col++, 0, "Sample", cellFormat));
        sheet.addCell(new Label(col++, 0, "Sample2", cellFormat));

        Domain runDomain = provider.getRunDomain(protocol);
        for (DomainProperty property : runDomain.getProperties())
        {
            sheet.addCell(new Label(col++, 0, property.getName(), cellFormat));
        }

        Domain fractionDomain = provider.getFractionDomain(protocol);
        for (DomainProperty property : fractionDomain.getProperties())
        {
            sheet.addCell(new Label(col++, 0, property.getName(), cellFormat));
        }

        for (int i = 0; i < col; i++)
        {
            sheet.setColumnView(i, 15);
        }

        int row = 1;
        List<Map<String, File>> allFiles = form.getSelectedDataCollector().getFileQueue(form);
        for (Map<String, File> files : allFiles)
        {
            for (File file : files.values())
            {
                sheet.addCell(new Label(0, row++, file.getName()));
            }
        }

        workbook.write();
        getViewContext().getResponse().getOutputStream().flush();
        workbook.close();
        return null;
    }
}