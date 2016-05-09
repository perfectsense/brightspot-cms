package com.psddev.cms.view.servlet;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.LoggerFactory;

import com.psddev.cms.view.ViewModel;
import com.psddev.dari.util.Converter;
import com.psddev.dari.util.TypeDefinition;

public class ServletViewModelCreator extends ViewModel.DefaultCreator {

    private static final Converter CONVERTER = new Converter(); static {
        CONVERTER.putAllStandardFunctions();
    }

    private HttpServletRequest request;

    public ServletViewModelCreator(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    protected <M, VM extends ViewModel<? super M>> void beforeViewModelOnCreate(VM viewModel) {
        updateWithRequest(viewModel);
    }

    private <M, VM extends ViewModel<? super M>> void updateWithRequest(VM viewModel) {

        if (viewModel == null) {
            return;
        }

        try {
            TypeDefinition<? extends ViewModel> viewModelDefinition = TypeDefinition.getInstance(viewModel.getClass());

            for (Map.Entry<String, List<Field>> entry : viewModelDefinition.getAllSerializableFields().entrySet()) {

                Field field = entry.getValue().get(entry.getValue().size() - 1);
                String fieldName = field.getName();

                Object fieldValue = null;

                // check for annotation processors.
                for (Annotation viewModelAnnotation : field.getAnnotations()) {

                    Class<?> annotationClass = viewModelAnnotation.annotationType();

                    ServletViewRequestAnnotationProcessorClass annotation = annotationClass.getAnnotation(
                            ServletViewRequestAnnotationProcessorClass.class);

                    if (annotation != null) {

                        Class<? extends ServletViewRequestAnnotationProcessor<? extends Annotation>> annotationProcessorClass = annotation.value();

                        if (annotationProcessorClass != null) {

                            @SuppressWarnings("unchecked")
                            ServletViewRequestAnnotationProcessor<Annotation> annotationProcessor
                                    = (ServletViewRequestAnnotationProcessor<Annotation>) TypeDefinition.getInstance(annotationProcessorClass).newInstance();

                            fieldValue = annotationProcessor.getValue(request, fieldName, viewModelAnnotation);
                            break;
                        }
                    }
                }

                if (fieldValue != null) {
                    try {

                        // Handle the case where the field value is a collection but the field type is not.
                        if (fieldValue instanceof Collection && !Collection.class.isAssignableFrom(field.getType())) {
                            if (!((Collection<?>) fieldValue).isEmpty()) {
                                // get the first value from the collection
                                fieldValue = ((Collection<?>) fieldValue).iterator().next();
                            } else {
                                fieldValue = null;
                            }
                        }

                        field.set(viewModel, CONVERTER.convert(field.getGenericType(), fieldValue));
                    } catch (IllegalAccessException ex) {
                        throw new IllegalStateException(ex);
                    }
                }
            }

        } catch (RuntimeException e) {
            LoggerFactory.getLogger(ServletViewModelCreator.class)
                    .warn("Failed to update view model of type ["
                            + viewModel.getClass() + "] with all request data. Cause: " + e.getMessage(), e);
        }
    }
}
