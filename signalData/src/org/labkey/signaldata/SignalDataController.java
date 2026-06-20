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

import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.labkey.api.action.ApiResponse;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.MutatingApiAction;
import org.labkey.api.action.ReadOnlyApiAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.files.FileContentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.AbstractContainerScopingTest;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.security.roles.EditorRole;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.Path;
import org.labkey.api.view.ActionURL;
import org.labkey.api.webdav.WebdavResource;
import org.labkey.api.webdav.WebdavService;
import org.labkey.signaldata.assay.SignalDataAssayDataHandler;
import org.labkey.vfs.FileLike;
import org.springframework.validation.BindException;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
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
    public static class getSignalDataPipelineContainerAction extends ReadOnlyApiAction<Object>
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
                webdavURL = webdavURL.resolve(SignalDataAssayDataHandler.NAMESPACE);

                //Create folder if needed
                FileLike sdFileRoot = root.getRootFileLike().resolveChild(SignalDataAssayDataHandler.NAMESPACE);
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
        private List<String> _paths;
        private List<String> _files;

        public List<String> getFiles()
        {
            return _files;
        }

        public void setFiles(List<String> files)
        {
            _files = files;
        }

        public List<String> getPaths()
        {
            return _paths;
        }

        public void setPaths(List<String> paths)
        {
            _paths = paths;
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public static class getSignalDataResourceAction extends MutatingApiAction<SignalDataResourceForm>
    {
        @Override
        public ApiResponse execute(SignalDataResourceForm form, BindException errors) throws Exception
        {
            List<Map<String, String>> results = new ArrayList<>();
            Container c = getContainer();
            FileContentService svc = FileContentService.get();
            TableInfo ti = new ExpSchema(getUser(), getContainer()).getDatasTable();
            QueryUpdateService qus = ti.getUpdateService();
            int maxUrlSize = ExperimentService.get().getTinfoData().getColumn(ExpDataTable.Column.DataFileUrl.name()).getScale();
            int idx = 0;

            try (DbScope.Transaction transaction = DbScope.getLabKeyScope().ensureTransaction())
            {
                for (String path : form.getPaths())
                {
                    WebdavResource resource = WebdavService.get().lookup(path);
                    String fileName = form.getFiles().get(idx++);

                    if (isAuthorizedResource(getUser(), c, resource))
                    {
                        ExpData data = svc.getDataObject(resource, c);
                        if (data == null)
                        {
                            // create the ExpData object if it doesn't already exist
                            File file = resource.getFile();
                            if (null != file)
                            {
                                data = ExperimentService.get().createData(c, UPLOADED_FILE);
                                data.setName(file.getName());
                                data.setDataFileURI(file.toURI());

                                String dataFileURL = data.getDataFileUrl();
                                if (dataFileURL == null || dataFileURL.length() <= maxUrlSize)
                                {
                                    data.save(getUser());
                                }
                                else
                                {
                                    throw new ValidationException(String.format("The data file URL is too long to store in the database (max %d).", maxUrlSize));
                                }
                            }
                        }

                        if (null != data)
                        {
                            File canonicalFile = FileUtil.getAbsoluteCaseSensitiveFile(resource.getFile());
                            String url = canonicalFile.toURI().toURL().toString();
                            List<Map<String, Object>> rows = qus.getRows(getUser(), c, Collections.singletonList(Map.of(ExpDataTable.Column.DataFileUrl.name(), url)));

                            if (rows.size() == 1)
                            {
                                Map<String, String> props = new HashMap<>();
                                props.put("FilePath", path);
                                props.put("FileName", fileName);
                                results.add(props);
                                for (Map.Entry<String, Object> entry : rows.get(0).entrySet())
                                {
                                    Object value = entry.getValue();
                                    if (null != value)
                                        props.put(entry.getKey(), String.valueOf(value));
                                }
                            }
                            else
                                throw new RuntimeException(String.format("Unexpected number of rows returned for DataFileUrl '%s': %d", url, rows.size()));
                        }
                    }
                }
                transaction.commit();
            }
            return new ApiSimpleResponse(Map.of("files", results));
        }
    }

    /**
     * GitHub Issue 1236: the requested path is client-supplied and WebdavService.lookup() resolves globally with no ACL
     * or container scoping. Before disclosing a resource's metadata or creating an exp.data row that references
     * it, require that (a) the caller can actually read the resolved resource and (b) it lives under this
     * container's pipeline root. Signal data files always reside under the container's own pipeline root, so a
     * path resolving outside it indicates a cross-container probe rather than a legitimate request.
     */
    static boolean isAuthorizedResource(User user, Container c, @Nullable WebdavResource resource)
    {
        if (null == resource || !resource.canRead(user, true))
            return false;

        File file = resource.getFile();
        if (null == file)
            return false;

        PipeRoot root = PipelineService.get().findPipelineRoot(c);
        return null != root && root.isUnderRoot(file);
    }

    /**
     * GitHub Issue 1236 regression test.
     */
    public static class ContainerScopingTestCase extends AbstractContainerScopingTest
    {
        private Container _folderA;
        private Container _folderB;
        private User _readerA;

        @Before
        public void setup() throws Exception
        {
            _folderA = createContainer("A");
            _folderB = createContainer("B");
            _readerA = createUserInRole(_folderA, EditorRole.class);
        }

        @Test
        public void testGetSignalDataResourceContainerScoping() throws Exception
        {
            File foreignFile = writeFileUnderFileRoot(_folderB, "foreign.txt");
            ExperimentService exp = ExperimentService.get();

            // Deny (read ACL): a caller who can read folder A but NOT folder B requests a file in B's pipeline root
            // with test=true. The action returns 200 with empty props either way, so we assert the real side effect:
            // no exp.data row is created in folder A referencing the foreign file. Pre-fix the global WebdavService
            // lookup resolved the file with no ACL check and createData() persisted a reference in folder A.
            assertStatus(HttpServletResponse.SC_OK, post(resourceUrl(_folderA, _folderB, "foreign.txt"), _readerA));
            assertNull("A foreign-folder file the caller cannot read must not be imported into folder A",
                    exp.getExpDataByURL(foreignFile, _folderA));

            // Deny (pipeline-root containment): even a site admin who CAN read folder B must not be able to pull a
            // file from B's pipeline root into folder A. This is the case the canRead check alone would miss.
            assertStatus(HttpServletResponse.SC_OK, post(resourceUrl(_folderA, _folderB, "foreign.txt"), getAdmin()));
            assertNull("A file outside the request container's pipeline root must not be imported, even for an admin",
                    exp.getExpDataByURL(foreignFile, _folderA));

            // Positive control: the same request for a file under folder A's own pipeline root still creates the
            // exp.data row, proving the fix did not break the legitimate in-container flow.
            File localFile = writeFileUnderFileRoot(_folderA, "local.txt");
            assertStatus(HttpServletResponse.SC_OK, post(resourceUrl(_folderA, _folderA, "local.txt"), getAdmin()));
            assertNotNull("A file under the request container's own pipeline root should still be imported",
                    exp.getExpDataByURL(localFile, _folderA));
        }

        /**
         * Build a getSignalDataResource URL in {@code requestContainer} whose path points at a file under {@code fileContainer}'s @files root.
         */
        private static ActionURL resourceUrl(Container requestContainer, Container fileContainer, String name)
        {
            String[] paths = { filesPath(fileContainer).append(name).toString() };
            String[] files = { name };
            return new ActionURL(getSignalDataResourceAction.class, requestContainer)
                    .addParameters(Map.of("paths", paths, "files", files));
        }

        private static Path filesPath(Container c)
        {
            return WebdavService.getPath().append(c.getParsedPath()).append(FileContentService.FILES_LINK);
        }

        private static File writeFileUnderFileRoot(Container c, String name) throws Exception
        {
            WebdavResource filesNode = WebdavService.get().lookup(filesPath(c));
            assertNotNull("Test requires a @files node for " + c.getName(), filesNode);
            File dir = filesNode.getFile();
            assertNotNull("Test requires a file root for " + c.getName(), dir);
            FileUtil.mkdirs(dir);

            File file = FileUtil.appendName(dir, name);
            if (!file.exists())
                Files.writeString(file.toPath(), "test");
            return file;
        }
    }
}