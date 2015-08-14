package com.psddev.cms.db;

import com.psddev.dari.util.CollectionUtils;
import com.psddev.dari.util.DimsImageEditor;
import com.psddev.dari.util.ImageEditor;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StringUtils;
import com.psddev.image.HotSpotPoint;
import com.psddev.image.HotSpotRegion;
import com.psddev.image.HotSpots;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ImageHotSpot {

    public static List<Integer> crop(StorageItem item, Integer cropWidth, Integer cropHeight) {
        if (item != null
                && item.getMetadata().containsKey("height")
                && item.getMetadata().containsKey("width")
                && ((cropWidth != null && ObjectUtils.to(Integer.class, item.getMetadata().get("width")) > cropWidth)
                || cropHeight != null && ObjectUtils.to(Integer.class, item.getMetadata().get("height")) > cropHeight)) {

            List<HotSpotPoint> hotSpots = HotSpots.Data.getHotSpots(item);
            if (!ObjectUtils.isBlank(hotSpots)) {
                Integer x2 = null;
                Integer y2 = null;
                Integer imageHeight = ObjectUtils.to(Integer.class, item.getMetadata().get("height"));
                Integer imageWidth = ObjectUtils.to(Integer.class, item.getMetadata().get("width"));

                Integer originalHeight = CollectionUtils.getByPath(item.getMetadata(), ImageTag.ORIGINAL_HEIGHT_METADATA_PATH) != null
                        ? ObjectUtils.to(Integer.class, CollectionUtils.getByPath(item.getMetadata(), ImageTag.ORIGINAL_HEIGHT_METADATA_PATH))
                        : imageHeight;
                Integer originalWidth = CollectionUtils.getByPath(item.getMetadata(), ImageTag.ORIGINAL_WIDTH_METADATA_PATH) != null
                        ? ObjectUtils.to(Integer.class, CollectionUtils.getByPath(item.getMetadata(), ImageTag.ORIGINAL_WIDTH_METADATA_PATH))
                        : imageWidth;

                Integer angle = ObjectUtils.to(Integer.class, CollectionUtils.getByPath(item.getMetadata(), "cms.edits/rotate"));
                if (angle != null && (angle == 90 || angle == -90)) {
                    Integer temp = imageHeight;
                    imageHeight = imageWidth;
                    imageWidth = temp;

                    temp = originalHeight;
                    originalHeight = originalWidth;
                    originalWidth = temp;
                }

                if (cropWidth == null) {
                    double scale = (double) imageHeight / cropHeight;
                    cropWidth = ((Double) (cropWidth * scale)).intValue();
                } else if (cropHeight == null) {
                    double scale = (double) imageWidth / cropWidth;
                    cropHeight = ((Double) (cropHeight * scale)).intValue();
                } else {
                    double horzScale = (double) imageWidth / cropWidth;
                    double vertScale = (double) imageHeight / cropHeight;
                    if (vertScale < horzScale) {
                        cropHeight = imageHeight;
                        cropWidth = ((Double) (cropWidth * vertScale)).intValue();
                    } else {
                        cropWidth = imageWidth;
                        cropHeight = ((Double) (cropHeight * horzScale)).intValue();
                    }
                }

                double heightScaleFactor = originalHeight != null && originalHeight > 0 ? (double) imageHeight / originalHeight : 1.0;
                double widthScaleFactor = originalWidth != null && originalWidth > 0 ? (double) imageWidth / originalWidth : 1.0;

                //rotate bounding box
                if (CollectionUtils.getByPath(item.getMetadata(), "cms.edits/rotate") != null) {
                    if (angle != null && angle == 90) {
                        for (HotSpotPoint hotSpot : hotSpots) {
                            Integer x = hotSpot.as(HotSpotPoint.Data.class).getX();
                            Integer y = hotSpot.as(HotSpotPoint.Data.class).getY();

                            Integer width = (hotSpot instanceof HotSpotRegion) ? hotSpot.as(HotSpotRegion.Data.class).getWidth() : 0;

                            hotSpot.as(HotSpotPoint.Data.class).setX(originalHeight - y - width);
                            hotSpot.as(HotSpotPoint.Data.class).setY(x);

                            if (hotSpot instanceof HotSpotRegion) {
                                Integer temp = hotSpot.as(HotSpotRegion.Data.class).getWidth();
                                hotSpot.as(HotSpotRegion.Data.class).setWidth(hotSpot.as(HotSpotRegion.Data.class).getHeight());
                                hotSpot.as(HotSpotRegion.Data.class).setHeight(temp);
                            }
                        }
                    }

                }

                Boolean flipH = ObjectUtils.to(Boolean.class, CollectionUtils.getByPath(item.getMetadata(), "cms.edits/flipH"));
                Boolean flipV = ObjectUtils.to(Boolean.class, CollectionUtils.getByPath(item.getMetadata(), "cms.edits/flipV"));

                if (flipH != null && flipH) {
                    for (HotSpotPoint hotSpot : hotSpots) {
                        if (hotSpot instanceof HotSpotRegion) {
                            hotSpot.as(HotSpotPoint.Data.class).setX(originalWidth - hotSpot.as(HotSpotPoint.Data.class).getX() - hotSpot.as(HotSpotRegion.Data.class).getWidth());
                        } else {
                            hotSpot.as(HotSpotPoint.Data.class).setX(originalWidth - hotSpot.as(HotSpotPoint.Data.class).getX());
                        }
                    }
                }

                if (flipV != null && flipV) {
                    for (HotSpotPoint hotSpot : hotSpots) {
                        if (hotSpot instanceof HotSpotRegion) {
                            hotSpot.as(HotSpotPoint.Data.class).setY(originalHeight - hotSpot.as(HotSpotPoint.Data.class).getY() - hotSpot.as(HotSpotRegion.Data.class).getHeight());
                        } else {
                            hotSpot.as(HotSpotPoint.Data.class).setY(originalHeight - hotSpot.as(HotSpotPoint.Data.class).getY());
                        }
                    }
                }

                //bounding box of hotspots
                Integer x1 = null;
                Integer y1 = null;
                for (HotSpotPoint hotSpot : hotSpots) {
                    x1 = x1 == null || hotSpot.as(HotSpotPoint.Data.class).getX() < x1 ? hotSpot.as(HotSpotPoint.Data.class).getX() : x1;
                    y1 = y1 == null || hotSpot.as(HotSpotPoint.Data.class).getY() < y1 ? hotSpot.as(HotSpotPoint.Data.class).getY() : y1;
                    if (hotSpot instanceof HotSpotRegion) {
                        x2 = x2 == null || (hotSpot.as(HotSpotPoint.Data.class).getX() + hotSpot.as(HotSpotRegion.Data.class).getWidth()) > x2 ? (hotSpot.as(HotSpotPoint.Data.class).getX() +  hotSpot.as(HotSpotRegion.Data.class).getWidth()) : x2;
                        y2 = y2 == null || (hotSpot.as(HotSpotPoint.Data.class).getY() + hotSpot.as(HotSpotRegion.Data.class).getHeight()) > y2 ? (hotSpot.as(HotSpotPoint.Data.class).getY() +  hotSpot.as(HotSpotRegion.Data.class).getHeight()) : y2;
                    }
                }

                x1 = ((Double) (x1 * widthScaleFactor)).intValue();
                x2 = ((Double) (x2 * widthScaleFactor)).intValue();
                y1 = ((Double) (y1 * heightScaleFactor)).intValue();
                y2 = ((Double) (y2 * heightScaleFactor)).intValue();

                int centerX = (x1 + x2) / 2;
                int centerY = (y1 + y2) / 2;

                x1 = centerX - (cropWidth / 2);
                x2 = centerX + (cropWidth / 2);
                y1 = centerY - (cropHeight / 2);
                y2 = centerY + (cropHeight / 2);

                if (x1 < 0) {
                    x1 = 0;
                } else if (x2 > imageWidth) {
                    x1 = x1 - x2 + imageWidth;
                }

                if (y1 < 0) {
                    y1 = 0;
                } else if (y2 > cropHeight) {
                    y1 = y1 - y2 + cropHeight;
                }
                return Arrays.asList(x1, y1, cropWidth, cropHeight);
            }
        }
        return null;
    }

    public static List<HotSpotPoint> getReSizedHotSpots(StorageItem item, Object size) throws IOException {
        return getReSizedHotSpots(item, size, Boolean.FALSE);
    }

    public static List<HotSpotPoint> getReSizedHotSpots(StorageItem item, Object size, Boolean disableHotSpotCrop) throws IOException {
        StandardImageSize standardImageSize = null;
        if (size instanceof StandardImageSize) {
            standardImageSize = ((StandardImageSize) size);
        } else if (size instanceof String) {
            standardImageSize = ImageTag.getStandardImageSizeByName((String) size);
        }
        if (standardImageSize != null && standardImageSize.getId() != null) {
            return getReSizedHotSpots(item, standardImageSize.getWidth(), standardImageSize.getHeight(), standardImageSize.getCropOption(), standardImageSize.getResizeOption(), standardImageSize.getId().toString(), disableHotSpotCrop);
        }
        return null;
    }

    public static List<HotSpotPoint> getReSizedHotSpots(StorageItem item, Integer reSizedWidth, Integer reSizedHeight, CropOption cropOption, ResizeOption resizeOption, String standardImageSizeId, Boolean disableHotSpotCrop) throws IOException {
        List<HotSpotPoint> originalHotSpots = HotSpots.Data.getHotSpots(item);
        if (!ObjectUtils.isBlank(originalHotSpots)
                && item != null
                && item.getMetadata().containsKey("height")
                && item.getMetadata().containsKey("width")) {

            ImageCrop crop = null;
            Integer originalWidth = ImageTag.findDimension(item, "width");
            Integer originalHeight = ImageTag.findDimension(item, "height");
            Integer cropX = null;
            Integer cropY = null;
            Integer cropWidth = null;
            Integer cropHeight = null;
            boolean usingDimsImageEditor = ImageEditor.Static.getDefault() instanceof DimsImageEditor;

            Map<String, ImageCrop> crops = ImageTag.findImageCrops(item);
            if (crops != null
                    && !StringUtils.isBlank(standardImageSizeId)
                    && (crop = crops.get(standardImageSizeId)) != null
                    && originalWidth != null && originalHeight != null) {
                cropX = (int) (crop.getX() * originalWidth);
                cropY = (int) (crop.getY() * originalHeight);
                cropWidth = (int) (crop.getWidth() * originalWidth);
                cropHeight = (int) (crop.getHeight() * originalHeight);
            }

            Dimension originalDimension = new Dimension(originalWidth, originalHeight);
            if (resizeOption == ResizeOption.ONLY_SHRINK_LARGER) {
                if (reSizedWidth != null
                        && reSizedWidth > 0
                        && reSizedHeight != null && reSizedHeight > 0) {
                    reSizedWidth = originalDimension.width != null ? Math.min(originalDimension.width, reSizedWidth) : reSizedWidth;
                    reSizedHeight = originalDimension.height != null ? Math.min(originalDimension.height, reSizedHeight) : reSizedHeight;
                } else {
                    Dimension outputDimension = getResizeDimension(originalDimension.width, originalDimension.height, reSizedWidth, reSizedHeight);
                    reSizedWidth = outputDimension.width;
                    reSizedHeight = outputDimension.height;
                }
            } else if (resizeOption == ResizeOption.ONLY_ENLARGE_SMALLER) {
                if (reSizedWidth != null
                        && reSizedWidth > 0
                        && reSizedHeight != null && reSizedHeight > 0) {
                    reSizedWidth = originalDimension.width != null ? Math.max(originalDimension.width, reSizedWidth) : reSizedWidth;
                    reSizedHeight = originalDimension.height != null ? Math.max(originalDimension.height, reSizedHeight) : reSizedHeight;
                } else {
                    Dimension outputDimension = getResizeDimension(originalDimension.width, originalDimension.height, reSizedWidth, reSizedHeight);
                    reSizedWidth = outputDimension.width;
                    reSizedHeight = outputDimension.height;
                }
            }

            double vertScale = (double) originalHeight / reSizedHeight;
            double horzScale = (double) originalWidth / reSizedWidth;

            if (vertScale < horzScale) {
                cropHeight = originalHeight;
                cropWidth = ((Double) (reSizedWidth * vertScale)).intValue();
            } else {
                cropWidth = originalWidth;
                cropHeight = ((Double) (reSizedHeight * horzScale)).intValue();
            }

            if (cropX == null
                    && cropY  == null
                    && (cropOption == null || cropOption.equals(CropOption.AUTOMATIC))) {
                List<Integer> hotSpotCrop = crop(item, cropWidth, cropHeight);
                if (!ObjectUtils.isBlank(hotSpotCrop)
                        && hotSpotCrop.size() == 4) {
                    cropX = hotSpotCrop.get(0);
                    cropY = hotSpotCrop.get(1);
                    cropWidth = hotSpotCrop.get(2);
                    cropHeight = hotSpotCrop.get(3);
                }
            }

            if (resizeOption != null && resizeOption.equals(ResizeOption.IGNORE_ASPECT_RATIO)) {
                cropX = 0;
                cropY = 0;
                cropWidth = originalWidth;
                cropHeight = originalHeight;
            } else {
                if (cropX == null
                        && cropY == null
                        && cropOption == null
                        || cropOption == CropOption.AUTOMATIC) {
                    if (usingDimsImageEditor) {
                        cropX = 0;
                        cropY = 0;
                    } else if (vertScale < horzScale) {
                        //width is cut off
                        cropY = 0;
                        cropX = (originalWidth / 2) - (cropWidth / 2);
                    } else {
                        //height is cut off
                        cropX = 0;
                        cropY = (originalHeight / 2) - (cropHeight / 2);
                    }
                } else if (cropOption != null && cropOption == CropOption.NONE) {
                    cropWidth = null;
                    cropHeight = null;
                }
            }

            return getReSizedHotSpots(item, cropX, cropY, cropWidth, cropHeight, reSizedWidth, reSizedHeight);
        }
        return null;
    }

    public static List<HotSpotPoint> getReSizedHotSpots(StorageItem item, Integer cropX, Integer cropY, Integer cropWidth, Integer cropHeight, Integer reSizedWidth, Integer reSizedHeight) {
        List<HotSpotPoint> hotSpots = HotSpots.Data.getHotSpots(item);
        if (!ObjectUtils.isBlank(hotSpots)
                && item != null
                && item.getMetadata().containsKey("height")
                && item.getMetadata().containsKey("width")) {

            Integer imageHeight = ObjectUtils.to(Integer.class, item.getMetadata().get("height"));
            Integer imageWidth = ObjectUtils.to(Integer.class, item.getMetadata().get("width"));

            Integer originalHeight = CollectionUtils.getByPath(item.getMetadata(), ImageTag.ORIGINAL_HEIGHT_METADATA_PATH) != null
                    ? ObjectUtils.to(Integer.class, CollectionUtils.getByPath(item.getMetadata(), ImageTag.ORIGINAL_HEIGHT_METADATA_PATH))
                    : imageHeight;
            Integer originalWidth = CollectionUtils.getByPath(item.getMetadata(), ImageTag.ORIGINAL_WIDTH_METADATA_PATH) != null
                    ? ObjectUtils.to(Integer.class, CollectionUtils.getByPath(item.getMetadata(), ImageTag.ORIGINAL_WIDTH_METADATA_PATH))
                    : imageWidth;

            Boolean flipH = ObjectUtils.to(Boolean.class, CollectionUtils.getByPath(item.getMetadata(), "cms.edits/flipH"));
            Boolean flipV = ObjectUtils.to(Boolean.class, CollectionUtils.getByPath(item.getMetadata(), "cms.edits/flipV"));

            if (flipH != null && flipH) {
                for (HotSpotPoint hotSpot : hotSpots) {
                    if (hotSpot instanceof HotSpotRegion) {
                        hotSpot.as(HotSpotPoint.Data.class).setX(originalWidth - hotSpot.as(HotSpotPoint.Data.class).getX() - hotSpot.as(HotSpotRegion.Data.class).getWidth());
                    } else {
                        hotSpot.as(HotSpotPoint.Data.class).setX(originalWidth - hotSpot.as(HotSpotPoint.Data.class).getX());
                    }
                }
            }

            if (flipV != null && flipV) {
                for (HotSpotPoint hotSpot : hotSpots) {
                    if (hotSpot instanceof HotSpotRegion) {
                        hotSpot.as(HotSpotPoint.Data.class).setY(originalHeight - hotSpot.as(HotSpotPoint.Data.class).getY());
                    } else {
                        hotSpot.as(HotSpotPoint.Data.class).setY(originalHeight - hotSpot.as(HotSpotPoint.Data.class).getY() - hotSpot.as(HotSpotRegion.Data.class).getHeight());
                    }
                }
            }

            if (cropX != null && cropY != null) {
                for (HotSpotPoint hotSpot : hotSpots) {
                    hotSpot.as(HotSpotPoint.Data.class).setX(hotSpot.as(HotSpotPoint.Data.class).getX() - cropX);
                    hotSpot.as(HotSpotPoint.Data.class).setY(hotSpot.as(HotSpotPoint.Data.class).getY() - cropY);
                }
            }

            if (CollectionUtils.getByPath(item.getMetadata(), "cms.edits/rotate") != null) {
                Integer angle = ObjectUtils.to(Integer.class, CollectionUtils.getByPath(item.getMetadata(), "cms.edits/rotate"));
                if (angle != null) {
                    if (angle == 90) {
                        for (HotSpotPoint hotSpot : hotSpots) {
                            Integer originalX = hotSpot.as(HotSpotPoint.Data.class).getX();
                            hotSpot.as(HotSpotPoint.Data.class).setY(originalX);
                            if (hotSpot instanceof HotSpotRegion) {
                                hotSpot.as(HotSpotPoint.Data.class).setX(originalHeight - hotSpot.as(HotSpotPoint.Data.class).getY());
                            } else {
                                hotSpot.as(HotSpotPoint.Data.class).setX(originalHeight - hotSpot.as(HotSpotPoint.Data.class).getY() - hotSpot.as(HotSpotRegion.Data.class).getWidth());
                                hotSpot.as(HotSpotRegion.Data.class).setWidth(hotSpot.as(HotSpotRegion.Data.class).getHeight());
                                hotSpot.as(HotSpotRegion.Data.class).setHeight(hotSpot.as(HotSpotRegion.Data.class).getWidth());
                            }
                        }
                    } else if (angle == -90) {
                        for (HotSpotPoint hotSpot : hotSpots) {
                            Integer originalX = hotSpot.as(HotSpotPoint.Data.class).getX();
                            hotSpot.as(HotSpotPoint.Data.class).setX(hotSpot.as(HotSpotPoint.Data.class).getY());
                            if (hotSpot instanceof HotSpotRegion) {
                                hotSpot.as(HotSpotPoint.Data.class).setY(originalWidth - originalX);
                            } else {
                                hotSpot.as(HotSpotPoint.Data.class).setY(originalWidth - originalX - hotSpot.as(HotSpotRegion.Data.class).getHeight());
                                hotSpot.as(HotSpotRegion.Data.class).setWidth(hotSpot.as(HotSpotRegion.Data.class).getHeight());
                                hotSpot.as(HotSpotRegion.Data.class).setHeight(hotSpot.as(HotSpotRegion.Data.class).getWidth());
                            }
                        }
                    }
                }

            }

            if (reSizedHeight != null || reSizedWidth != null) {
                Double heightScaleFactor;
                Double widthScaleFactor;
                if (cropHeight == null && cropWidth == null) {
                    heightScaleFactor = (double) reSizedHeight / imageHeight;
                    widthScaleFactor = (double) reSizedWidth / imageWidth;

                    if (widthScaleFactor < heightScaleFactor) {
                        heightScaleFactor = widthScaleFactor;
                    } else {
                        widthScaleFactor = heightScaleFactor;
                    }
                } else {
                    heightScaleFactor = cropHeight != null && cropHeight > 0 ? (double) reSizedHeight / cropHeight : 1.0;
                    widthScaleFactor = cropWidth != null && cropWidth > 0 ? (double) reSizedWidth / cropWidth : 1.0;
                }

                for (HotSpotPoint hotSpot : hotSpots) {
                    hotSpot.as(HotSpotPoint.Data.class).setX(((Double) (hotSpot.as(HotSpotPoint.Data.class).getX() * widthScaleFactor)).intValue());
                    hotSpot.as(HotSpotPoint.Data.class).setY(((Double) (hotSpot.as(HotSpotPoint.Data.class).getY() * heightScaleFactor)).intValue());
                    if (hotSpot instanceof HotSpotRegion) {
                        hotSpot.as(HotSpotRegion.Data.class).setWidth(((Double) (hotSpot.as(HotSpotRegion.Data.class).getWidth() * widthScaleFactor)).intValue());
                        hotSpot.as(HotSpotRegion.Data.class).setHeight(((Double) (hotSpot.as(HotSpotRegion.Data.class).getHeight() * heightScaleFactor)).intValue());
                    }
                }
            }
        }

        return hotSpots;
    }

    private static class Dimension {
        public final Integer width;
        public final Integer height;
        public Dimension(Integer width, Integer height) {
            this.width = width;
            this.height = height;
        }
    }
    private static Dimension getResizeDimension(Integer originalWidth, Integer originalHeight, Integer requestedWidth, Integer requestedHeight) {
        Integer actualWidth = null;
        Integer actualHeight = null;

        if (originalWidth != null && originalHeight != null
                && (requestedWidth != null || requestedHeight != null)) {

            float originalRatio = (float) originalWidth / (float) originalHeight;
            if (requestedWidth != null && requestedHeight != null) {

                float requestedRatio = (float) requestedWidth / (float) requestedHeight;
                if (originalRatio > requestedRatio) {
                    actualWidth = requestedWidth;
                    actualHeight = (int) Math.round((float) requestedWidth * originalHeight / originalWidth);
                } else if (originalRatio < requestedRatio) {
                    actualWidth = (int) Math.round((float) requestedHeight * originalWidth / originalHeight);
                    actualHeight = requestedHeight;
                } else {
                    actualWidth = requestedWidth;
                    actualHeight = requestedHeight;
                }
            } else if (requestedWidth == null) {
                actualHeight = requestedHeight;
                actualWidth = Math.round((float) requestedHeight * originalRatio);
            } else if (requestedHeight == null) {
                actualWidth = requestedWidth;
                actualHeight = Math.round((float) requestedWidth / originalRatio);
            }
        }

        return new Dimension(actualWidth, actualHeight);
    }
}
