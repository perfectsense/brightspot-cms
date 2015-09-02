package com.psddev.cms.tool;

import com.psddev.cms.db.Content;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;

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

    /**
     * {@link Modification} of SearchResultSelectionGeneratable classes to prevent
     * {@link com.psddev.dari.db.ValidationException ValidationException} from being
     * thrown on draft creation by the {@link com.psddev.cms.tool.page.CreateDraft} servlet.
     */
    public static class Data extends Modification<SearchResultSelectionGeneratable> {

        public static final String IGNORE_VALIDATION_EXTRA = "cms.ignoreValidation";

        @Override
        protected void onValidate() {

            State state = getState();

            if (ObjectUtils.to(boolean.class, state.getExtra(IGNORE_VALIDATION_EXTRA)) && state.as(Content.ObjectModification.class).isDraft()) {

                state.clearAllErrors();
            }
        }
    }
}
