package com.psddev.cms.tool.file;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.psddev.cms.TestStorageItem;
import com.psddev.cms.db.ImageCrop;
import com.psddev.cms.db.StandardImageSize;
import com.psddev.cms.tool.FileContentType;
import com.psddev.cms.tool.TestToolPageContext;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ColorDistribution;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.DatabaseEnvironment;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ImageEditor;
import com.psddev.dari.util.JavaImageEditor;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.TypeReference;
import com.psddev.dari.util.UuidUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class ImageFileTypeTest {

    private static final String IMAGE_PATH = "/image/file.png";
    private static final String FIELD_NAME = "file";
    private static final Map<String, Object> EXPECTED_METADATA;
    private static final Map<String, String> EXPECTED_EDITS;
    private static final Map<String, ImageCrop> EXPECTED_CROPS;
    private static final Map<String, Double> EXPECTED_FOCUS;
    private static final ImageCrop EXPECTED_CROP_1;
    private static final ImageCrop EXPECTED_CROP_2;
    private static final List<StandardImageSize> SIZES;
    public static final StandardImageSize SIZE1;
    public static final StandardImageSize SIZE2;

    // Metadata key values
    private static final String BRIGHTNESS_KEY = "brightness";
    private static final String CONTRAST_KEY = "contrast";
    private static final String FLIP_H_KEY = "flipH";
    private static final String FLIP_V_KEY = "flipV";
    private static final String GRAYSCALE_KEY = "grayscale";
    private static final String INVERT_KEY = "invert";
    private static final String ROTATE_KEY = "rotate";
    private static final String SEPIA_KEY = "sepia";
    private static final String SHARPEN_KEY = "sharpen";

    // Sample image edit values
    private static final String BRIGHTNESS = "0.7";
    private static final String CONTRAST = "0.7";
    private static final String FLIP_H = "true";
    private static final String FLIP_V = "true";
    private static final String GRAYSCALE = "true";
    private static final String INVERT = "true";
    private static final String ROTATE = "90";
    private static final String SEPIA = "true";
    private static final Double FOCUS_X = 0.7;
    private static final Double FOCUS_Y = 0.7;

    // Set up expected values
    static {

        // Prepare expected edits
        EXPECTED_EDITS = new HashMap<>();
        EXPECTED_EDITS.put(BRIGHTNESS_KEY, BRIGHTNESS);
        EXPECTED_EDITS.put(CONTRAST_KEY, CONTRAST);
        EXPECTED_EDITS.put(FLIP_H_KEY, FLIP_H);
        EXPECTED_EDITS.put(FLIP_V_KEY, FLIP_V);
        EXPECTED_EDITS.put(GRAYSCALE_KEY, GRAYSCALE);
        EXPECTED_EDITS.put(INVERT_KEY, INVERT);
        EXPECTED_EDITS.put(ROTATE_KEY, ROTATE);
        EXPECTED_EDITS.put(SEPIA_KEY, SEPIA);

        // Prepare expected crops
        EXPECTED_CROPS = new LinkedHashMap<>();
        EXPECTED_CROP_1 = new ImageCrop();
        EXPECTED_CROP_1.setX(0.7);
        EXPECTED_CROP_1.setY(0.1);
        EXPECTED_CROP_1.setWidth(100);
        EXPECTED_CROP_1.setHeight(200);

        EXPECTED_CROP_2 = new ImageCrop();
        EXPECTED_CROP_2.setX(0.8);
        EXPECTED_CROP_2.setY(0.2);
        EXPECTED_CROP_2.setWidth(200);
        EXPECTED_CROP_2.setHeight(100);

        // Prepare expected focus
        EXPECTED_FOCUS = new HashMap<>();
        EXPECTED_FOCUS.put("x", FOCUS_X);
        EXPECTED_FOCUS.put("y", FOCUS_Y);

        // Prepare Sizes for Database
        SIZE1 = new StandardImageSize();
        SIZE1.setDisplayName("size1");
        SIZE1.setInternalName("size1");
        SIZE1.setWidth(100);
        SIZE1.setHeight(100);
        SIZE1.getState().setId(UuidUtils.createSequentialUuid());

        SIZE2 = new StandardImageSize();
        SIZE2.setDisplayName("size2");
        SIZE2.setInternalName("size2");
        SIZE2.setWidth(100);
        SIZE2.setHeight(100);
        SIZE2.getState().setId(UuidUtils.createSequentialUuid());

        SIZES = new ArrayList<>();
        SIZES.add(SIZE1);
        SIZES.add(SIZE2);

        EXPECTED_CROPS.put(SIZE1.getId().toString(), EXPECTED_CROP_1);
        EXPECTED_CROPS.put(SIZE2.getId().toString(), EXPECTED_CROP_2);

        EXPECTED_METADATA = new HashMap<>();
        EXPECTED_METADATA.put("cms.edits", EXPECTED_EDITS);
        EXPECTED_METADATA.put("cms.crops", EXPECTED_CROPS);
        EXPECTED_METADATA.put("cms.focus", EXPECTED_FOCUS);
    }

    @RunWith(MockitoJUnitRunner.class)
    public static class PriorityTest {

        ImageFileType imageFileType;

        @Mock
        StorageItem storageItem;

        @Before
        public void before() {
            imageFileType = new ImageFileType();
        }

        @Test
        public void defaultPriorityTest() {
            when(storageItem.getContentType()).thenReturn("image/png");
            assertTrue(imageFileType.getPriority(storageItem) == FileContentType.DEFAULT_PRIORITY_LEVEL);
        }

        @Test
        public void negativePriorityTest() {
            when(storageItem.getContentType()).thenReturn("video/mp4");
            assertTrue(imageFileType.getPriority(storageItem) < FileContentType.DEFAULT_PRIORITY_LEVEL);
        }
    }

    @RunWith(MockitoJUnitRunner.class)
    public static class ProcessTest {

        ToolPageContext page;

        @Spy
        TestStorageItem storageItem;

        @Mock
        HttpServletRequest request;
        @Mock
        Database database;
        @Mock
        DatabaseEnvironment databaseEnvironment;

        @Before
        public void before() {

            // Prepare ToolPageContext
            page = spy(new ToolPageContext((ServletContext) null, null, null));
            when(request.getParameter(eq("inputName"))).thenReturn(FIELD_NAME);
            doReturn(request).when(page).getRequest();

            // Prepare Mock Database
            doReturn(SIZES).when(database).readAll(anyObject());
            doReturn(databaseEnvironment).when(database).getEnvironment();
            Database.Static.overrideDefault(database);

            when(request.getParameter(eq("inputName"))).thenReturn(FIELD_NAME);

            for (Map.Entry<String, String> entry : EXPECTED_EDITS.entrySet()) {
                when(request.getParameter(eq(FIELD_NAME + "." + entry.getKey()))).thenReturn(entry.getValue());
            }

            String cropsName = FIELD_NAME + ".crops.";
            for (Map.Entry<String, ImageCrop> entry : EXPECTED_CROPS.entrySet()) {
                when(request.getParameter(eq(cropsName + entry.getKey() + ".x"))).thenReturn(ObjectUtils.to(String.class, entry.getValue().getX()));
                when(request.getParameter(eq(cropsName + entry.getKey() + ".y"))).thenReturn(ObjectUtils.to(String.class, entry.getValue().getY()));
                when(request.getParameter(eq(cropsName + entry.getKey() + ".width"))).thenReturn(ObjectUtils.to(String.class, entry.getValue().getWidth()));
                when(request.getParameter(eq(cropsName + entry.getKey() + ".height"))).thenReturn(ObjectUtils.to(String.class, entry.getValue().getHeight()));
            }

            when(request.getParameter(eq(FIELD_NAME + ".focusX"))).thenReturn(ObjectUtils.to(String.class, FOCUS_X));
            when(request.getParameter(eq(FIELD_NAME + ".focusY"))).thenReturn(ObjectUtils.to(String.class, FOCUS_Y));

            doReturn(request).when(page).getRequest();
        }

        @After
        public void after() {
            Database.Static.restoreDefault();
        }


        @Test
        public void noProcess() {
            // Test no NPE is thrown
            new ImageFileType().process(null, null);
        }

        @Test
        public void validateMetadata() {
            new ImageFileType().process(page, storageItem);

            Map<String, Object> metadata = storageItem.getMetadata();
            assertNotNull(metadata);

            Map<String, Object> edits = ObjectUtils.firstNonNull(
                    ObjectUtils.to(new TypeReference<Map<String, Object>>() {
                    }, metadata.get("cms.edits")),
                    new HashMap<String, Object>()
            );

            Map<String, Object> focusPoint = ObjectUtils.firstNonNull(
                    ObjectUtils.to(new TypeReference<Map<String, Object>>() {
                    }, metadata.get("cms.focus")),
                    new HashMap<String, Object>()
            );

            Map<String, ImageCrop> crops = ImageCrop.createCrops(metadata.get("cms.crops"));

            compareMaps(edits, ObjectUtils.to(new TypeReference<Map<String, Object>>() {
            }, EXPECTED_EDITS));
            compareMaps(focusPoint, ObjectUtils.to(new TypeReference<Map<String, Object>>() {
            }, EXPECTED_FOCUS));
            compareCropsMaps(crops, EXPECTED_CROPS);

        }

        private void compareMaps(Map<String, Object> actual, Map<String, Object> expected) {
            for (Map.Entry<String, Object> entry : actual.entrySet()) {
                String key = entry.getKey();
                compareValue(actual.get(key), expected.get(key));
            }
        }

        private void compareCropsMaps(Map<String, ImageCrop> actual, Map<String, ImageCrop> expected) {
            for (Map.Entry<String, ImageCrop> entry : actual.entrySet()) {
                String key = entry.getKey();
                ImageCrop actualCrop = actual.get(key);
                ImageCrop expectedCrop = expected.get(key);

                compareValue(actualCrop.getX(), expectedCrop.getX());
                compareValue(actualCrop.getY(), expectedCrop.getY());
                compareValue(actualCrop.getWidth(), expectedCrop.getWidth());
                compareValue(actualCrop.getHeight(), expectedCrop.getHeight());
            }
        }

        private void compareValue(Object actual, Object expected) {
            assertEquals(ObjectUtils.to(String.class, actual), ObjectUtils.to(String.class, expected));
        }
    }

    @RunWith(MockitoJUnitRunner.class)
    public static class PreviewTest {

        TestToolPageContext page;

        @Mock
        StorageItem storageItem;
        @Mock
        State state;
        @Mock
        HttpServletRequest request;
        @Mock
        Database database;
        @Mock
        DatabaseEnvironment databaseEnvironment;

        @Before
        public void before() throws IOException {
            when(storageItem.getContentType()).thenReturn("image/jpg");
            doReturn(IMAGE_PATH).when(storageItem).getPublicUrl();
            doReturn(EXPECTED_METADATA).when(storageItem).getMetadata();

            ColorDistribution.Data data = mock(ColorDistribution.Data.class);
            doReturn(null).when(data).getDistribution();
            doReturn(data).when(state).as(any());

            // Prepare Mock Database
            doReturn(SIZES).when(database).readAll(anyObject());
            doReturn(databaseEnvironment).when(database).getEnvironment();
            Database.Static.overrideDefault(database);

            // Override ImageEditor
            String imageEditorName = "test";
            Settings.setOverride(ImageEditor.DEFAULT_IMAGE_EDITOR_SETTING, imageEditorName);
            String imageEditorPrefix = ImageEditor.SETTING_PREFIX + "/" + imageEditorName + "/";
            Settings.setOverride(imageEditorPrefix + "class", TestImageEditor.class.getName());

            // Prepare ToolPageContext
            page = spy(new TestToolPageContext(null, null, null));

            when(request.getParameter(eq("inputName"))).thenReturn(FIELD_NAME);
            doReturn(request).when(page).getRequest();
        }

        @After
        public void after() {
            Database.Static.restoreDefault();
        }

        @Test
        public void noPreview() throws IOException, ServletException {
            new ImageFileType().writePreview(page, null, null);
            verify(page, never()).write();
            verify(page, never()).writeStart(any(), anyVararg());
            verify(page, never()).writeEnd();
        }

        @Test
        public void validatePreview() throws IOException, ServletException {

            // Writes image preview to writer
            new ImageFileType().writePreview(page, state, storageItem);

            Document doc = page.getDocument();

            // Verify Tools
            Element toolsContainer = doc.select(".imageEditor-tools").first();
            assertNotNull(toolsContainer);
            assertTrue(toolsContainer.select(".action-preview").size() == 1);
            assertTrue(toolsContainer.select(".icon-crop").size() == 1);
            assertTrue(toolsContainer.select(".icon-tint").size() == 0);

            // Verify Edit Inputs
            Element editsContainer = doc.select(".imageEditor-edit").first();
            assertNotNull(editsContainer);
            for (Map.Entry<String, String> edit : EXPECTED_EDITS.entrySet()) {
                validateEditInput(editsContainer, FIELD_NAME + "." + edit.getKey(), edit.getValue());
            }

            // Verify Image
            Element imageContainer = doc.select(".imageEditor-image").first();
            assertNotNull(imageContainer);
            assertTrue(imageContainer.select("img").size() == 1);

            validateEditInput(imageContainer, FIELD_NAME + ".focusX", FOCUS_X.toString());
            validateEditInput(imageContainer, FIELD_NAME + ".focusY", FOCUS_Y.toString());

            // Validate Sizes
            Element sizesContainer = doc.select(".imageEditor-sizes").first();
            assertNotNull(sizesContainer);
            Elements sizeRows = sizesContainer.select("tr");
            assertTrue(sizeRows.size() == 2);

            validateCropInputs(sizeRows.get(0), SIZE1, EXPECTED_CROP_1);
            validateCropInputs(sizeRows.get(1), SIZE2, EXPECTED_CROP_2);
        }

        private void validateEditInput(Element container, String inputName, String expectedValue) {
            Element input = container.select("input[name=" + inputName + "]").first();
            validateInput(input, expectedValue);
        }

        private void validateCropInputs(Element cropInputContainer, StandardImageSize expectedSize, ImageCrop expectedCrop) {
            validateInput(cropInputContainer.select("input[name$=" + expectedSize.getId() + ".x]").first(), Double.toString(expectedCrop.getX()));
            validateInput(cropInputContainer.select("input[name$=" + expectedSize.getId() + ".y]").first(), Double.toString(expectedCrop.getY()));
            validateInput(cropInputContainer.select("input[name$=" + expectedSize.getId() + ".width]").first(), Double.toString(expectedCrop.getWidth()));
            validateInput(cropInputContainer.select("input[name$=" + expectedSize.getId() + ".height]").first(), Double.toString(expectedCrop.getHeight()));

            //TODO add check for crop texts

        }

        private void validateInput(Element input, String expectedValue) {
            assertNotNull(input);
            assertEquals(expectedValue, input.attr("value"));
        }
    }

    @Ignore
    public static class TestImageEditor extends JavaImageEditor {

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void setName(String s) {

        }

        @Override
        public StorageItem edit(StorageItem storageItem, String s, Map<String, Object> map, Object... objects) {
            return storageItem;
        }

        @Override
        public void initialize(String s, Map<String, Object> map) {

        }
    }
}
