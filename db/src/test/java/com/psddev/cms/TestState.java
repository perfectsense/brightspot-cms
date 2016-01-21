package com.psddev.cms;

import com.psddev.dari.db.State;

//TODO: move to dari?
public class TestState extends State {

    @Override
    public Object get(Object key) {
        return getRawValue(key.toString());
    }
}
