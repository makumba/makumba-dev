package org.makumba.providers.query.mql;

import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.collections.CollectionUtils;
import org.makumba.FieldDefinition;
import org.makumba.Pointer;
import org.makumba.commons.NameResolver;
import org.makumba.commons.NameResolver.TextList;

import antlr.CommonAST;
import antlr.SemanticException;
import antlr.collections.AST;

/**
 * The root of Mql analysis tree nodes. It performs analysis as the tree is built. It should know how to determine its
 * Makumba type, if it is not a parameter.
 * 
 * @author Cristian Bogdan
 * @version $Id: MqlNode.java,v 1.1 Aug 5, 2008 5:38:16 PM cristi Exp $
 */

public class MqlNode extends CommonAST {
    NameResolver.TextList text;

    MqlSqlWalker walker;

    private MqlNode father;

    private FieldDefinition makType;

    private String originalText;

    protected MqlSqlWalker getWalker() {
        return walker;
    }

    protected void setWalker(MqlSqlWalker walker) {
        this.walker = walker;
    }

    public MqlNode() {
    }

    /** we analyze the tree as it is built */
    public void setFather(MqlNode node) {
        father = node;
    }

    /** we analyze the tree as it is built, we call oneMoreChild() to see whether we are ready for analysis */
    @Override
    public void setFirstChild(AST a) {
        super.setFirstChild(a);
        if (a != null) {
            ((MqlNode) a).setFather(this);
            oneMoreChild((MqlNode) a);
        }
    }

    /** we analyze the tree as it is built, we call oneMoreChild() of the father to see whether it is ready for analysis */
    @Override
    public void setNextSibling(AST a) {
        super.setNextSibling(a);
        if (father != null)
            father.oneMoreChild((MqlNode) a);
    }

    protected void oneMoreChild(MqlNode child) {
        FieldDefinition tp = findMakType(child);
        if (tp != null)
            makType = tp;
        if ((getType() == HqlSqlTokenTypes.IN || getType() == HqlSqlTokenTypes.NOT_IN)
                && child.getType() == HqlSqlTokenTypes.IN_LIST) {
            // getFirstChild() is the left side of the IN expression, child.getFirstChild() is the right side, where we
            // expect a parameter list
            //FIXME: we assume that the left operand of the IN  expression is not a parameter and therefore has a type assigned.
            // if it is a parameter, we have to make sure that all things in the right operand (IN_LIST) are of the same type and assign that type to the parameter
            MqlNode inListMember = (MqlNode) child.getFirstChild();
            do {
                if (inListMember.getType() == HqlSqlTokenTypes.NAMED_PARAM)
                    walker.setParameterType(inListMember, (MqlNode) getFirstChild());
                else
                    try {
                        checkAndRewriteOperand((MqlNode) getFirstChild(), inListMember);
                    } catch (SemanticException e) {
                        walker.error=e;
                        return;
                    }
                inListMember = (MqlNode) inListMember.getNextSibling();
            } while (inListMember != null);
        }
    }

    protected FieldDefinition findMakType(MqlNode child) {
        switch (getType()) {
            // TODO: type of functions
            // if their args are parameters, set parameter types

            // TODO: type of ANY and ALL is the type of the (only) projection of the subquery
            case HqlSqlTokenTypes.METHOD_CALL:
                return getFunctionType(child);
            case HqlSqlTokenTypes.AGGREGATE:
            case HqlSqlTokenTypes.UNARY_MINUS:
            case HqlSqlTokenTypes.UNARY_PLUS:
                return child.getMakType();
            case HqlSqlTokenTypes.CASE:
                // TODO: maybe WHEN, THEN or ELSE are parameters
                // set the WHEN parameter type to boolean, and THEN has the type of ELSE
                // if both THEN and ELSE are parameters, we're in trouble
                // TODO: this is untested
                if (child.getType() == HqlSqlTokenTypes.THEN)
                    return ((MqlNode) child.getFirstChild()).getMakType();
        }
        return null;
    };

    private FieldDefinition getFunctionType(MqlNode child) {
        String type = null;
        String name = child.getText();
        if (dateFunctions.contains(name))
            type = "date";
        if (intFunctions.contains(name))
            type = "int";
        if (stringFunctions.contains(name))
            type = "char[255]";
        if (type != null) {
            child.setType(HqlSqlTokenTypes.METHOD_NAME);
            return walker.currentContext.ddp.makeFieldDefinition("x", type);
        }
        return null;
    }

    protected void setMakType(FieldDefinition fd) {
        if(fd.getType().equals("ptrIndex")){
            fd=walker.currentContext.ddp.makeFieldDefinition("x", "ptr " + fd.getPointedType().getName());
        }
        makType = fd;
    }

    protected FieldDefinition getMakType() {
        return makType;
    }

    public void setText(String text) {
        super.setText(text);
        if (originalText == null && text.length() > 0)
            originalText = text;
    }

    public void setType(int type) {
        super.setType(type);
        String def = knownType();
        if (def != null)
            setMakType(walker.currentContext.ddp.makeFieldDefinition("x", def));
    }

    String knownType() {

        // FIXME: this is not actually checking for correctness: e.g. AND should have 2 boolean operators, etc.
        // to fix that, the MqlBinaryOperator can be used / subclassed
        switch (getType()) {
            case HqlSqlTokenTypes.NUM_INT:
            case HqlSqlTokenTypes.NUM_LONG:
            case HqlSqlTokenTypes.COUNT:
                return "int";
            case HqlSqlTokenTypes.NUM_FLOAT:
            case HqlSqlTokenTypes.NUM_DOUBLE:
                return "real";
            case HqlSqlTokenTypes.FALSE:
            case HqlSqlTokenTypes.TRUE:
            case HqlSqlTokenTypes.EXISTS:
            case HqlSqlTokenTypes.NOT:
            case HqlSqlTokenTypes.AND:
            case HqlSqlTokenTypes.OR:
            case HqlSqlTokenTypes.IS_NULL:
            case HqlSqlTokenTypes.IS_NOT_NULL:
            case HqlSqlTokenTypes.IN:
                return "boolean";
            case HqlSqlTokenTypes.QUOTED_STRING:
                return "char[255]";
        }
        return null;
    }

    protected void checkForOperandType(MqlNode ast) {
        if (!ast.isParam() && ast.getMakType() == null)
            throw new IllegalStateException("No makumba type computed for "
                    + MqlSqlWalker.printer.showAsString(ast, ""));
    }

    boolean isParam() {
        return getType() == HqlSqlTokenTypes.NAMED_PARAM || getType() == HqlSqlTokenTypes.PARAM;
    }

    void checkOperandTypes(MqlNode left, MqlNode right) throws SemanticException {
        if (!(left.isParam() && left.getMakType()==null) // 
                && !right.getMakType().isAssignableFrom(left.getMakType()) //
                && !(right.getMakType().isNumberType() && left.getMakType().isNumberType()))
            throw new SemanticException("incompatible operands " + left.getText() + "("+left.getMakType()+") and " + right.getText()+" ("+right.getMakType()+")");
    }
    
    public String getOriginalText() {
        return originalText;
    }

    public void writeTo(TextList t) {
        if (text == null)
            t.append(getText());
        else
            t.append(text);
    }

    public String getText() {
        if (text != null)
            return text.toString();
        else
            return super.getText();
    }

    public void setTextList(TextList tl) {
        text = tl;
    }

    protected boolean checkAndRewriteOperand(MqlNode left, MqlNode right) throws SemanticException {
        if (right.getType() == HqlSqlTokenTypes.QUOTED_STRING && !left.isParam()) {
    
            String s = right.getText();
            String arg1 = s.substring(1, s.length() - 1);
            if (arg1.length() > 0) {
                // OQL accepts empty strings even for intEnums...
                // here we accept it also for pointers, that's a bit too much
                Object o = null;
                try {
                    o = ((FieldDefinition) left.getMakType()).checkValue(arg1);
                } catch (org.makumba.InvalidValueException e) {
                    // walker.printer.showAst(right, walker.pw);
                    throw new SemanticException(e.getMessage());
                }
                if (o instanceof Pointer) {
                    o = new Long(((Pointer) o).longValue());
                }
                if (o instanceof Number) {
                    right.setText(o.toString());
                } else
                    right.setText("\'" + o + "\'");
            }
            return true;
    
        } else {
            checkOperandTypes(left, right);
            return false;
        }
    }

    // ------ function part, ported from OQL, might move somewhere else------
    /** Simple string-to-string functions with one argument. */
    public static String[] simpleStringFunctions = { "lower", "upper", "trim", "rtrim", "ltrim" };

    /** int-to-string functions. */
    public static String[] intToStringFunctions = { "char" };

    /** string-to-int functions. */
    public static String[] stringToIntFunctions = { "ascii", "character_length" };

    /** date-to-int functions. */
    public static String[] dateToIntFunctions = { "dayOfMonth", "dayOfWeek", "dayOfYear", "month", "hour", "minute(",
            "second(" };

    /** date-to-String functions. */
    public static String[] dateToStringFunctions = { "monthName", "dayName" };

    public static String[] nonParametricDateFunctions = { "current_date", "current_time", "current_timestamp" };

    public static HashSet<String> stringFunctions = new HashSet<String>();

    public static HashSet<String> intFunctions = new HashSet<String>();

    public static HashSet<String> dateFunctions = new HashSet<String>();

    static {
        CollectionUtils.addAll(stringFunctions, simpleStringFunctions);
        CollectionUtils.addAll(stringFunctions, intToStringFunctions);
        CollectionUtils.addAll(stringFunctions, dateToStringFunctions);

        CollectionUtils.addAll(intFunctions, stringToIntFunctions);
        CollectionUtils.addAll(intFunctions, dateToIntFunctions);

        CollectionUtils.addAll(dateFunctions, nonParametricDateFunctions);
    }

}