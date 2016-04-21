package com.psddev.cms.view.servlet;

import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StorageItemFilter;
import com.psddev.dari.util.StringUtils;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * Populates a StorageItem with the data from the query parameter from an HTTP request.
 * It can process a file input or a string input with json.
 * The parameter fetched has the same name as the field it populates unless
 * otherwise specified.
 */
@ServletViewRequestAnnotationProcessorClass(HttpStorageItemParameterProcessor.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface HttpStorageItemParameter {
    String value() default "";
    String storage() default "";
}

class HttpStorageItemParameterProcessor implements ServletViewRequestAnnotationProcessor<HttpStorageItemParameter> {

    @Override
    public List<StorageItem> getValue(HttpServletRequest request, String fieldName, HttpStorageItemParameter annotation) {
        String parameterName = annotation.value();
        String storageName = annotation.storage();
        if (StringUtils.isBlank(parameterName)) {
            parameterName = fieldName;
        }
        if (StringUtils.isBlank(storageName)) {
            storageName = null;
        }
        try {
            return StorageItemFilter.getParameters(request, parameterName, storageName);
        } catch (IOException e) {
            LoggerFactory.getLogger(HttpStorageItemParameterProcessor.class)
                    .error(e.getMessage());
            return null;
        }
    }
}
