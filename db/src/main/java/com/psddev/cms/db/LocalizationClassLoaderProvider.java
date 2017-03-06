package com.psddev.cms.db;

/**
 * Implementation of this class provides a class loader that
 * {@link LocalizationContext} uses the find resource bundles.
 */
public interface LocalizationClassLoaderProvider {

    /**
     * @return Nullable.
     */
    ClassLoader getClassLoader();
}
