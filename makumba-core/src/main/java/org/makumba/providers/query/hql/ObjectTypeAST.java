/*
 * Created on 21-Jul-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.makumba.providers.query.hql;

import java.util.Map;

import antlr.RecognitionException;
import antlr.SemanticException;
import antlr.collections.AST;

public class ObjectTypeAST extends ExprTypeAST {
    private static final long serialVersionUID = 1L;

    private String objectType;

    public ObjectTypeAST(AST lhs, AST rhs, Map<String, String> aliasTypes, ObjectType typeComputer)
            throws RecognitionException {
        super(-2);
        String type = null;
        if (lhs instanceof ObjectTypeAST) {
            type = ((ObjectTypeAST) lhs).getObjectType();
            if (type == null) {
                throw new SemanticException("unknown alias: " + lhs + " in property reference: " + "of " + rhs);
            }
        } else {
            type = aliasTypes.get(lhs.getText());
            if (type == null) {
                throw new SemanticException("unknown alias: " + lhs.getText() + " in property reference: "
                        + lhs.getText() + "." + rhs.getText());
            }
        }

        Object computedType = "";
        if (rhs == null) {
            computedType = typeComputer.determineType(type, null);
        } else {
            setDescription(rhs.getText());
            computedType = typeComputer.determineType(type, rhs.getText());

        }

        // System.out.println("GOT TYPE: " + computedType);

        if (computedType instanceof String) {
            setObjectType(computedType.toString());
        } else {
            setExtraTypeInfo(computedType);
            setDataType(typeComputer.getTypeOf(computedType));
        }
    }

    public ObjectTypeAST(AST pointer, Map<String, String> aliasTypes) throws SemanticException {
        String type = aliasTypes.get(pointer.getText());
        if (type == null) {
            throw new SemanticException("unknown alias: " + pointer.getText());
        }

        setObjectType(type);
        setDescription(pointer.getText());
    }

    @Override
    public String getObjectType() {
        return objectType;
    }

    void setObjectType(String objectType) {
        this.objectType = objectType;
    }

}
