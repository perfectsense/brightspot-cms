package com.psddev.cms.tool.page.content.field;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.google.common.collect.ImmutableMap;
import com.psddev.cms.TestState;
import com.psddev.cms.TestStorageItem;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.TestToolPageContext;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.h2.H2Database;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.StorageItem;
import com.zaxxer.hikari.HikariDataSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class FileFieldTest {

    static final String INPUT_NAME = "file";
    static final String FIELD_NAME = "file";

    @RunWith(MockitoJUnitRunner.class)
    public static class PostRequestTest {

        private static final String STORAGE_NAME = "test";
        private static final String DATABASE_NAME = "test";

        TestState state;
        TestStorageItem storageItem;

        TestToolPageContext page;

        @Mock
        HttpServletRequest request;

        @Before
        public void before() {
            page = spy(new TestToolPageContext(null, null, null));
            when(request.getParameter(eq("inputName"))).thenReturn(INPUT_NAME);
            when(request.getParameter(eq("fieldName"))).thenReturn(FIELD_NAME);
            doReturn(request).when(page).getRequest();

            storageItem = new TestStorageItem();
            storageItem.setContentType("file/test");
            storageItem.setPath("/path/test.test");
            storageItem.setStorage(STORAGE_NAME);

            state = new TestState();

            when(request.getAttribute(eq("isFormPost"))).thenReturn(true);
            when(request.getAttribute("object")).thenReturn(state);


            Settings.setOverride(Database.DEFAULT_DATABASE_SETTING, DATABASE_NAME);

            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setJdbcUrl("jdbc:h2:mem:test" + UUID.randomUUID().toString().replaceAll("-", "") + ";DB_CLOSE_DELAY=-1");

            Settings.setOverride(Database.SETTING_PREFIX + "/" + DATABASE_NAME + "/class", H2Database.class.getName());
            Settings.setOverride(Database.SETTING_PREFIX + "/" + DATABASE_NAME + "/" + H2Database.DATA_SOURCE_SUB_SETTING, dataSource);
            Settings.setOverride(Database.SETTING_PREFIX + "/" + DATABASE_NAME + "/" + H2Database.INDEX_SPATIAL_SUB_SETTING, Boolean.TRUE);

            Settings.setOverride(StorageItem.DEFAULT_STORAGE_SETTING, STORAGE_NAME);
            Settings.setOverride(StorageItem.SETTING_PREFIX + "/" + STORAGE_NAME, ImmutableMap.of(
                    "class", TestStorageItem.class.getName(),
                    "rootPath", "/webapps/media-files",
                    "baseUrl", "http://testhost:8080/media-files"
            ));
        }

        @After
        public void after() {
            reset(request);
            reset(page);
        }

        @Test
        public void keep() throws IOException, ServletException {

            when(request.getParameter(eq(INPUT_NAME + ".action"))).thenReturn("keep");
            when(request.getParameterValues(INPUT_NAME + ".file.json"))
                    .thenReturn(new String[]{ObjectUtils.toJson(storageItem)});

            FileField.processField(page);

            compareStorageItem((StorageItem) state.getRawValue(FIELD_NAME), storageItem);
            verifyNoPageWrites(page);
        }

        @Test
        public void newUpload() throws IOException, ServletException {

            when(request.getParameter(eq(INPUT_NAME + ".action"))).thenReturn("newUpload");
            when(request.getParameterValues(INPUT_NAME + ".file"))
                    .thenReturn(new String[]{ObjectUtils.toJson(storageItem)});

            FileField.processField(page);

            compareStorageItem((StorageItem) state.getRawValue(FIELD_NAME), storageItem);
            verifyNoPageWrites(page);
        }

        @Test
        public void newUrl() throws IOException, ServletException {
            storageItem.setStorage("_url");
            when(request.getParameter(eq(INPUT_NAME + ".action"))).thenReturn("newUrl");
            when(request.getParameter(INPUT_NAME + ".url"))
                    .thenReturn(storageItem.getPath());

            FileField.processField(page);

            compareStorageItem((StorageItem) state.getRawValue(FIELD_NAME), storageItem);

            verifyNoPageWrites(page);
        }

        public void verifyNoPageWrites(ToolPageContext page) throws IOException {
            verify(page, never()).write();
            verify(page, never()).writeStart(any(), anyVararg());
            verify(page, never()).writeEnd();
        }

        public void compareStorageItem(StorageItem actual, StorageItem expected) {
            assertEquals(actual.getPath(), expected.getPath());
            assertEquals(actual.getStorage(), expected.getStorage());
        }
    }

    @RunWith(MockitoJUnitRunner.class)
    public static class GetRequestTest {

        private static final String STORAGE_NAME = "test";

        TestState state;
        TestStorageItem storageItem;

        TestToolPageContext page;

        @Mock
        HttpServletRequest request;
        @Mock
        ObjectField field;

        @Before
        public void before() throws IOException {
            page = spy(new TestToolPageContext(null, null, null));
            when(request.getParameter(eq("inputName"))).thenReturn(INPUT_NAME);
            when(request.getParameter(eq("fieldName"))).thenReturn(FIELD_NAME);

            when(page.getCmsTool()).thenReturn(new CmsTool());
            doReturn(request).when(page).getRequest();


            storageItem = new TestStorageItem();
            storageItem.setContentType("file/test");
            storageItem.setPath("/path/test.test");
            storageItem.setStorage(STORAGE_NAME);

            when(field.getInternalName()).thenReturn(INPUT_NAME);
            when(field.as(any())).thenReturn(new ToolUi());

            state = new TestState();
            when(request.getAttribute(eq("isFormPost"))).thenReturn(false);
            when(request.getAttribute("object")).thenReturn(state);
            when(request.getAttribute("field")).thenReturn(field);
        }

        @After
        public void after() {
            reset(request);
            reset(page);
        }

        @Test
        public void noFieldValue() throws IOException, ServletException {
            FileField.processField(page);

            Document doc = page.getDocument();
            Element inputWrapper = doc.select(".inputSmall").first();
            assertNotNull(inputWrapper);

            Element select = inputWrapper.select("select").first();
            verifySelectOptions(select, false, false);
            verifyHiddenInputs(inputWrapper, false, false);

            assertTrue(ObjectUtils.isBlank(inputWrapper.select(".filePreview")));
        }

        @Test
        public void existingFieldValue() throws IOException, ServletException {
            state.put(INPUT_NAME, storageItem);

            FileField.processField(page);

            Document doc = page.getDocument();
            Element inputWrapper = doc.select(".inputSmall").first();
            assertNotNull(inputWrapper);

            Element select = inputWrapper.select("select").first();
            verifySelectOptions(select, true, false);
            verifyHiddenInputs(inputWrapper, true, false);

            assertTrue(inputWrapper.select(".filePreview").size() == 1);
        }

        private void verifySelectOptions(Element select, boolean hasFieldValue, boolean showDropbox) {
            assertNotNull(select);

            Elements options = select.select("option");
            int expectedSize = 3;

            if (hasFieldValue) {
                expectedSize++;
            }

            if (showDropbox) {
                expectedSize++;
            }

            assertTrue(options.size() == expectedSize);

            assertTrue(select.select("option[value=none]").size() == 1);
            assertTrue(select.select("option[value=newUpload]").size() == 1);
            assertTrue(select.select("option[value=newUrl]").size() == 1);

            if (hasFieldValue) {
                assertTrue(select.select("option[value=keep]").size() == 1);
            }
        }

        private void verifyHiddenInputs(Element inputWrapper, boolean hasFieldValue, boolean showDropbox) {
            assertTrue(inputWrapper.select(".fileSelectorNewUpload").size() == 1);
            assertTrue(inputWrapper.select(".fileSelectorNewUrl").size() == 1);

            if (hasFieldValue) {
                assertTrue(inputWrapper.select("input[name=" + INPUT_NAME + ".file.json]").size() == 1);
            }
        }
    }
}
