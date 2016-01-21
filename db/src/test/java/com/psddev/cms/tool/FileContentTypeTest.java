package com.psddev.cms.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import com.psddev.dari.db.State;
import com.psddev.dari.util.LocalStorageItem;
import com.psddev.dari.util.StorageItem;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class FileContentTypeTest {

    @Test
    public void nullStorageItemTest() {
        assertTrue(FileContentType.getFileContentType(null) == null);
    }

    @Test
    public void verifyGetFileContentType() {
        MultipleContentTypesSupported storageItem = mock(MultipleContentTypesSupported.class);

        FileContentType type = FileContentType.getFileContentType(storageItem);
        assertTrue(type instanceof HighPriority);
    }

    @Test
    public void verifyNoPreview() throws IOException, ServletException {
        ToolPageContext page = mock(ToolPageContext.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(page.getResponse()).thenReturn(response);

        FileContentType.writeFilePreview(page, null, null);

        verifyZeroInteractions(page);
    }

    @Test
    public void verifyDefaultPreview() throws IOException, ServletException {
        NoContentTypesSupported storageItem = mock(NoContentTypesSupported.class);
        ToolPageContext page = mock(ToolPageContext.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        StringWriter writer = new StringWriter();

        when(response.getWriter()).thenReturn(new PrintWriter(writer));
        when(page.getResponse()).thenReturn(response);

        FileContentType.writeFilePreview(page, null, storageItem);

        verify(page, atLeastOnce()).writeStart(anyString(), anyVararg());
        verify(page, atLeastOnce()).writeEnd();
    }

    @Test
    public void verifyWritePreview() throws IOException, ServletException {
        ToolPageContext page = mock(ToolPageContext.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        MultipleContentTypesSupported storageItem = mock(MultipleContentTypesSupported.class);

        StringWriter writer = new StringWriter();
        String path = "/path";

        when(response.getWriter()).thenReturn(new PrintWriter(writer));
        when(page.getResponse()).thenReturn(response);
        when(storageItem.getPath()).thenReturn(path);
        FileContentType.writeFilePreview(page, null, storageItem);

        assertEquals(writer.toString(), path);
    }

    public static class MultipleContentTypesSupported extends LocalStorageItem {

    }

    public static class NoContentTypesSupported extends LocalStorageItem {

    }

    public static class HighPriority implements FileContentType {

        @Override
        public double getPriority(StorageItem storageItem) {
            if (storageItem instanceof MultipleContentTypesSupported) {
                return DEFAULT_PRIORITY_LEVEL + 10;
            }
            return DEFAULT_PRIORITY_LEVEL - 1;
        }

        @Override
        public void writePreview(ToolPageContext page, State state, StorageItem storageItem) throws IOException, ServletException {
            page.getResponse().getWriter().write(storageItem.getPath());
        }
    }

    public static class LowPriority implements FileContentType {

        @Override
        public double getPriority(StorageItem storageItem) {

            if (storageItem instanceof MultipleContentTypesSupported) {
                return DEFAULT_PRIORITY_LEVEL + 1;
            }

            return DEFAULT_PRIORITY_LEVEL - 1;
        }

        @Override
        public void writePreview(ToolPageContext page, State state, StorageItem fieldValue) throws IOException, ServletException {

        }
    }
}
