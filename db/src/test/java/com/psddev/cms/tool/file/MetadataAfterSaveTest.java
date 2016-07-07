package com.psddev.cms.tool.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import com.psddev.dari.util.AbstractStorageItem;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MetadataAfterSaveTest {


    @Mock
    AbstractStorageItem item;

    MetadataAfterSave processor;

    @Before
    public void before() {
        processor = new MetadataAfterSave();
    }

    @Test
    public void nullStorgageItem() {
        processor.afterSave(null);
    }

    @Ignore
    @Test
    public void imageStorageItem() throws URISyntaxException, IOException {
        Map<String, Object> metadata = new HashMap<>();
        when(item.getMetadata()).thenReturn(metadata);
        when(item.getContentType()).thenReturn("image/png");

        File file = new File(getClass().getClassLoader().getResource("com/psddev/cms/tool/file/MetadataAfterSave_Test/test.png").toURI());
        when(item.getData()).thenReturn(new FileInputStream(file));

        processor.afterSave(item);

        verify(item, Mockito.times(1)).getData();
    }

    @Test
    public void metadataException() throws IOException {

        Map<String, Object> metadata = new HashMap<>();
        when(item.getMetadata()).thenReturn(metadata);
        when(item.getContentType()).thenReturn("image/png");
        when(item.getData()).thenThrow(new IOException());

        processor.afterSave(item);
    }
}