package org.makumba.providers.datadefinition;

import java.lang.reflect.Member;

import org.apache.commons.lang.StringUtils;
import org.makumba.FieldDefinition;
import org.makumba.FieldMetadata;
import org.makumba.MakumbaError;
import org.makumba.providers.DataDefinitionProvider;

public class FieldMetadataImpl implements FieldMetadata {

    private final DataDefinitionProvider ddp;

    private Member member;

    private FieldDefinition fd;

    private final static Class<?>[] emptyClassArray = new Class<?>[] {};

    public Member getField() {
        return this.member;
    }

    public FieldDefinition getFieldDefinition() {
        return this.fd;
    }

    public FieldMetadataImpl() {
        this.ddp = DataDefinitionProvider.getInstance();
    }

    public FieldMetadataImpl(String typeName, String fieldName) {
        this();
        this.fd = ddp.getDataDefinition(typeName).getFieldDefinition(fieldName);

        try {

            // FIXME we need to re-load the class since it just got generated
            // but java does not allow that in the case of a static classloader
            // this.getClass().getClassLoader().loadClass(typeName);

            Class<?> clazz = Class.forName(typeName);

            // FIXME try fields as well
            member = clazz.getMethod("get" + StringUtils.capitalize(fieldName), emptyClassArray);

        } catch (ClassNotFoundException e) {
            throw new MakumbaError(e, "Could not find class of type '" + typeName + "'");
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            throw new MakumbaError(e, "Could not find getter of field " + fieldName);
        }

    }
    // public FieldMetadata(String scalarType){...}

    // public FieldMetadata(TypeMetadata pointTo){...}

}
