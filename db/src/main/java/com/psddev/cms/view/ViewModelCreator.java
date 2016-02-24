package com.psddev.cms.view;

/**
 * Creator of ViewModel objects.
 */
public interface ViewModelCreator {

    /**
     * Creates an instance of a ViewModel based on the ViewModel class,
     * associated model, and a ViewResponse.
     *
     * @param viewModelClass the class of the ViewModel to create.
     * @param model the model bound to the ViewModel.
     * @param viewResponse the current ViewResponse.
     * @param <M> the model type bound to the ViewModel.
     * @param <VM> the ViewModel type.
     * @return a newly created ViewModel of the specified {@code viewModelClass} type.
     */
    <M, VM extends ViewModel<? super M>> VM createViewModel(Class<VM> viewModelClass, M model, ViewResponse viewResponse);
}
