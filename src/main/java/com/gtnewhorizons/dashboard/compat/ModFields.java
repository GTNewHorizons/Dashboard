package com.gtnewhorizons.dashboard.compat;

import com.gtnewhorizon.gtnhlib.reflect.Fields;

/**
 * Other mods' fields, found by name so Dashboard does not depend on the mods. A field that is gone throws like a gone
 * class, so the caller's warning names it. Fields not read before that read as a fallback.
 */
final class ModFields {

    private ModFields() {}

    static Fields.ClassFields<Object> of(String className) throws ClassNotFoundException {
        return of(Class.forName(className));
    }

    @SuppressWarnings("unchecked")
    static Fields.ClassFields<Object> of(Class<?> type) {
        return Fields.ofClass((Class<Object>) type);
    }

    static Fields.ClassFields<Object>.Field<Boolean> booleanField(Fields.ClassFields<Object> fields, String name)
        throws NoSuchFieldException {
        return require(fields.getBooleanField(Fields.LookupType.DECLARED, name), name);
    }

    static Fields.ClassFields<Object>.Field<Integer> intField(Fields.ClassFields<Object> fields, String name)
        throws NoSuchFieldException {
        return require(fields.getIntField(Fields.LookupType.DECLARED, name), name);
    }

    static Fields.ClassFields<Object>.Field<Float> floatField(Fields.ClassFields<Object> fields, String name)
        throws NoSuchFieldException {
        return require(fields.getFloatField(Fields.LookupType.DECLARED, name), name);
    }

    static boolean readBoolean(Fields.ClassFields<Object>.Field<Boolean> field, boolean fallback) {
        return field == null ? fallback : field.getValue(null);
    }

    static int readInt(Fields.ClassFields<Object>.Field<Integer> field) {
        return readInt(field, 0);
    }

    static int readInt(Fields.ClassFields<Object>.Field<Integer> field, int fallback) {
        return field == null ? fallback : field.getValue(null);
    }

    private static <F> F require(F field, String name) throws NoSuchFieldException {
        if (field == null) {
            throw new NoSuchFieldException(name);
        }
        return field;
    }
}
