package com.psddev.cms.image;

import com.psddev.cms.view.ViewModel;
import com.psddev.dari.util.ClassEnhancer;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.asm.ClassReader;
import com.psddev.dari.util.asm.MethodVisitor;
import com.psddev.dari.util.asm.Opcodes;
import com.psddev.dari.util.asm.Type;

import java.lang.reflect.Method;
import java.util.Locale;

public class ImageSizeEnhancer extends ClassEnhancer {

    private static final String CLASS_INTERNAL_NAME;
    private static final String GET_URL_FOR_FIELD_METHOD_NAME;
    private static final String GET_URL_FOR_FIELD_METHOD_DESC;
    private static final String GET_URL_METHOD_NAME;
    private static final String GET_URL_METHOD_DESC;

    static {
        Class<?> imageSizeClass = ImageSize.class;
        Method getUrlMethod;
        Method getUrlAutomaticallyMethod;

        try {
            getUrlMethod = imageSizeClass.getMethod("getUrlForField", StorageItem.class, String.class);
            getUrlAutomaticallyMethod = imageSizeClass.getMethod("getUrl", StorageItem.class);

        } catch (NoSuchMethodException error) {
            throw new IllegalStateException(error);
        }

        CLASS_INTERNAL_NAME = Type.getInternalName(imageSizeClass);
        GET_URL_FOR_FIELD_METHOD_NAME = getUrlMethod.getName();
        GET_URL_FOR_FIELD_METHOD_DESC = Type.getMethodDescriptor(getUrlMethod);
        GET_URL_METHOD_NAME = getUrlAutomaticallyMethod.getName();
        GET_URL_METHOD_DESC = Type.getMethodDescriptor(getUrlAutomaticallyMethod);
    }

    public static String toField(String methodName) {
        if (methodName != null && methodName.startsWith("get")) {
            if (methodName.length() > 3) {
                methodName = methodName.substring(3);
                String field = methodName.substring(0, 1).toLowerCase(Locale.ENGLISH);

                if (methodName.length() > 1) {
                    field += methodName.substring(1);
                }

                return field;
            }
        }

        return null;
    }

    @Override
    public boolean canEnhance(ClassReader reader) {
        Class<?> c = ObjectUtils.getClassByName(reader.getClassName().replace('/', '.'));
        return c != null && ViewModel.class.isAssignableFrom(c);
    }

    @Override
    public MethodVisitor visitMethod(int access, String viewModelMethodName, String desc, String signature, String[] exceptions) {
        MethodVisitor visitor = super.visitMethod(access, viewModelMethodName, desc, signature, exceptions);

        return new MethodVisitor(Opcodes.ASM5, visitor) {

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (opcode == Opcodes.INVOKESTATIC
                        && CLASS_INTERNAL_NAME.equals(owner)
                        && GET_URL_METHOD_NAME.equals(name)
                        && GET_URL_METHOD_DESC.equals(desc)) {

                    String field = toField(viewModelMethodName);

                    if (field != null) {
                        super.visitLdcInsn(field);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, CLASS_INTERNAL_NAME, GET_URL_FOR_FIELD_METHOD_NAME, GET_URL_FOR_FIELD_METHOD_DESC, false);
                        return;
                    }
                }

                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        };
    }
}
