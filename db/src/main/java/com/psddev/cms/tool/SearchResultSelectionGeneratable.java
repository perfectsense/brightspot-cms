package com.psddev.cms.tool;

import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Recordable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

public interface SearchResultSelectionGeneratable extends Recordable {

    public void fromSelection(SearchResultSelection selection);

    @ObjectType.AnnotationProcessorClass(ItemTypeProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ItemTypes {
        Class<? extends Recordable>[] value();
    }

    static class ItemTypeProcessor implements ObjectType.AnnotationProcessor<ItemTypes> {

        @Override
        public void process(ObjectType type, ItemTypes annotation) {

            List<ObjectType> itemTypes = new ArrayList<ObjectType>();

            for (Class<? extends Recordable> clazz : annotation.value()) {

                ObjectType itemType = ObjectType.getInstance(clazz);
                if (itemType != null) {
                    itemTypes.add(itemType);
                }
            }

            type.as(TypeData.class).setItemTypes(itemTypes);
        }
    }

    public static class TypeData extends Modification<ObjectType> {

        private List<ObjectType> itemTypes;

        public List<ObjectType> getItemTypes() {
            if (itemTypes == null) {
                itemTypes = new ArrayList<ObjectType>();
            }
            return itemTypes;
        }

        public void setItemTypes(List<ObjectType> itemTypes) {
            this.itemTypes = itemTypes;
        }
    }
}
