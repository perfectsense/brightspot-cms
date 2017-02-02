package com.psddev.cms;

import java.io.IOException;
import java.io.InputStream;

import com.psddev.dari.util.AbstractStorageItem;

//TODO: Move to dari?
public class TestStorageItem extends AbstractStorageItem {

    @Override
    public boolean isInStorage() {
        return false;
    }

    @Override
    protected InputStream createData() throws IOException {
        return null;
    }

    @Override
    protected void saveData(InputStream data) throws IOException {

    }
}
