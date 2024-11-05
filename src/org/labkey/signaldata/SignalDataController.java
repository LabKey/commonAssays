/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

package org.labkey.signaldata;

import org.labkey.api.action.ApiResponse;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.MutatingApiAction;
import org.labkey.api.action.ReadOnlyApiAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.files.FileContentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.webdav.WebdavResource;
import org.labkey.api.webdav.WebdavService;
import org.labkey.signaldata.assay.SignalDataAssayDataHandler;
import org.springframework.validation.BindException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.labkey.api.files.FileContentService.UPLOADED_FILE;

public class SignalDataController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(SignalDataController.class);
    static final String NAME = "signaldata";

    public SignalDataController()
    {
        setActionResolver(_actionResolver);
    }


    /**
     * Meant to mimic PipelineController.getPipelineContainerAction but with the incorporated SignalData path context
     */
    @RequiresPermission(ReadPermission.class)
    public class getSignalDataPipelineContainerAction extends ReadOnlyApiAction
    {
        @Override
        public ApiResponse execute(Object form, BindException errors) throws Exception
        {
            ApiSimpleResponse resp = new ApiSimpleResponse();
            PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());

            String containerPath = null;
            URI webdavURL = null;

            if (null != root)
            {
                containerPath = root.getContainer().getPath();
                webdavURL = root.getWebdavURL();
                if (!SignalDataAssayDataHandler.NAMESPACE.isEmpty())
                    webdavURL = webdavURL.resolve(SignalDataAssayDataHandler.NAMESPACE);

                //Create folder if needed
                File sdFileRoot = new File(root.getRootPath(), SignalDataAssayDataHandler.NAMESPACE);
                if(!sdFileRoot.exists())
                    sdFileRoot.mkdirs();
            }

            resp.put("containerPath", containerPath);
            resp.put("webDavURL", Objects.toString(webdavURL, null));

            return resp;
        }
    }

    public static class SignalDataResourceForm
    {
        public String _path;
        public boolean _test = false;

        public String getPath()
        {
            return _path;
        }

        public void setPath(String path)
        {
            _path = path;
        }

        public boolean isTest()
        {
            return _test;
        }

        public void setTest(boolean test)
        {
            _test = test;
        }
    }


    @RequiresPermission(ReadPermission.class)
    public class getSignalDataResourceAction extends MutatingApiAction<SignalDataResourceForm>
    {
        @Override
        public ApiResponse execute(SignalDataResourceForm form, BindException errors)
        {
            String path = form.getPath();
            WebdavResource resource = WebdavService.get().lookup(path);
            Map<String, String> props = new HashMap<>();
            Container c = getContainer();

            if (null != resource)
            {
                FileContentService svc = FileContentService.get();
                ExpData data = svc.getDataObject(resource, c);

                if (form.isTest() && data == null)
                {
                    // Generate the data to help the test, replicating what DavController would do for us
                    File file = resource.getFile();
                    if (null != file)
                    {
                        data = ExperimentService.get().createData(c, UPLOADED_FILE);
                        data.setName(file.getName());
                        data.setDataFileURI(file.toURI());

                        int scale = ExperimentService.get().getTinfoData().getColumn("DataFileURL").getScale();
                        String dataFileURL = data.getDataFileUrl();
                        if (dataFileURL == null || dataFileURL.length() <= scale)
                        {
                            data.save(getUser());
                        }
                        else
                        {
                            // If the path is too long to store, bail out without creating an exp.data row
                        }
                    }
                }

                if (null != data)
                {
                    TableInfo ti = ExpSchema.TableType.Data.createTable(new ExpSchema(getUser(), c), ExpSchema.TableType.Data.toString(), null);
                    QueryUpdateService qus = ti.getUpdateService();

                    try
                    {
                        File canonicalFile = FileUtil.getAbsoluteCaseSensitiveFile(resource.getFile());
                        String url = canonicalFile.toURI().toURL().toString();
                        Map<String, Object> keys = Collections.singletonMap(ExpDataTable.Column.DataFileUrl.name(), url);
                        List<Map<String, Object>> rows = qus.getRows(getUser(), c, Collections.singletonList(keys));

                        if (rows.size() == 1)
                        {
                            for (Map.Entry<String, Object> entry : rows.get(0).entrySet())
                            {
                                Object value = entry.getValue();

                                if (null != value)
                                {
                                    props.put(entry.getKey(), String.valueOf(value));
                                }
                            }
                        }
                    }
                    catch (MalformedURLException e)
                    {
                        throw UnexpectedException.wrap(e);
                    }
                    catch (SQLException e)
                    {
                        throw new RuntimeSQLException(e);
                    }
                    catch (RuntimeException re)
                    {
                        throw re;
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }

            return new ApiSimpleResponse(props);
        }
    }
}