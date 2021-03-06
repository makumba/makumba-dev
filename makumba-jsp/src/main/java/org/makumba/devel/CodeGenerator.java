///////////////////////////////
//  Makumba, Makumba tag library
//  Copyright (C) 2000-2003  http://www.makumba.org
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
//
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//
//  -------------
//  $Id$
//  $Name$
/////////////////////////////////////

package org.makumba.devel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.makumba.DataDefinition;
import org.makumba.DataDefinitionNotFoundError;
import org.makumba.FieldDefinition;
import org.makumba.analyser.MakumbaJspAnalyzer;
import org.makumba.commons.StringUtils;
import org.makumba.controller.Logic;
import org.makumba.providers.DataDefinitionProvider;

/**
 * This class generates code from a {@link DataDefinition}. Possible code types are:
 * <ul>
 * <li>JSP code: newForm, editForm, deleteLink, pages to list and view objects.</li>
 * <li>Java business logic code with needed handler methods.</li>
 * </ul>
 * 
 * @author Rudolf Mayer
 * @author Manuel Bernhardt <manuel@makumba.org>
 * @version $Id$
 */
public class CodeGenerator {
    /** Mapping from a action name to the action code */
    public static Hashtable<String, String> nameToTypeMapping = new Hashtable<String, String>();

    public static final String TYPE_ADDFORM = "Add";

    public static final String TYPE_BUSINESS_LOGICS = "Logic";

    public static final String TYPE_DELETE = "Delete";

    public static final String TYPE_EDITFORM = "Edit";

    public static final String TYPE_LIST = "List";

    public static final String TYPE_NEWFORM = "New";

    public static final String TYPE_OBJECT = "Object";

    /** Code generation types that can be processed. This array can be used for batch-creating code in iterations. */
    static final String[] ALL_PROCESSABLE_TYPES = { TYPE_NEWFORM, TYPE_OBJECT, TYPE_EDITFORM, TYPE_LIST, TYPE_DELETE,
            TYPE_BUSINESS_LOGICS };

    private static final String[] allCodeTypes;

    /** Default used access keys for Add, Save changes, Cancel & Reset */
    private static final List<String> DEFAULTUSED_ACCESS_KEYS = Arrays.asList(new String[] { "a", "s", "c", "r" });

    public static final String OQL_TLD_DEFINITION = "<%@ taglib uri=\"http://www.makumba.org/presentation\" prefix=\"mak\" %>";

    public static final String HQL_TLD_DEFINITION = "<%@ taglib uri=\"http://www.makumba.org/view-hql\" prefix=\"mak\" %>";

    static {
        allCodeTypes = new String[] { TYPE_NEWFORM, TYPE_ADDFORM, TYPE_EDITFORM, TYPE_LIST, TYPE_OBJECT, TYPE_DELETE,
                TYPE_BUSINESS_LOGICS };
        for (String allCodeType : allCodeTypes) {
            nameToTypeMapping.put(allCodeType, allCodeType);
        }
    }

    /** Constructs a fitting file name for the given DataDefinition and code generation type */
    public static String getFileNameFromObject(DataDefinition dd, String type) {
        if (type == TYPE_OBJECT) {
            type = "View";
        }
        return getLabelNameFromDataDefinition(dd) + type + ".jsp";
    }

    /**
     * Generates a fitting Business Logics name from a given DataDefinition. As an example, for a DataDefinition callled
     * <code>general.Person</code>, PersonLogic will be the return value.
     */
    public static String getLogicNameFromDataDefinition(DataDefinition dd) {
        return StringUtils.upperCaseBeginning(getLabelNameFromDataDefinition(dd)) + "Logic";
    }

    /** Returns the label name for a given DataDefintion. E.g. "general.Person" will produce a label <i>person</i>. */
    public static String getLabelNameFromDataDefinition(DataDefinition dd) {
        String objectName = dd.getName();
        if (objectName.indexOf('.') != -1) {
            objectName = objectName.substring(objectName.lastIndexOf('.') + 1);
        }
        return StringUtils.lowerCaseBeginning(objectName);
    }

    /** Main method for command line code generation. */
    public static void main(String[] args) throws IOException {
        String object = args[0];
        String path = "";
        if (args.length > 1) {
            path = args[1];
        }
        if (!path.equals("") && !path.endsWith(File.separator)) {
            path += File.separator;
        }
        File outputDir = new File(path);
        outputDir.mkdirs();

        CodeGeneratorTemplate template = new CodeGeneratorTemplate();

        // getting the data definition
        DataDefinition dd;

        try {
            // FIXME - this should get the datadefinition of the MDDs in a given context
            dd = DataDefinitionProvider.getInstance().getDataDefinition(object);
        } catch (Throwable t) {
            throw new DataDefinitionNotFoundError("Could not find such a data defintion");
        }

        for (String element : ALL_PROCESSABLE_TYPES) {
            String fileName = path + getFileNameFromObject(dd, element);
            File f = new File(fileName);
            FileWriter fw = new FileWriter(f);
            BufferedWriter out = new BufferedWriter(fw);
            StringBuffer sb = new StringBuffer();
            String action = getLabelNameFromDataDefinition(dd) + "View.jsp";
            new CodeGenerator().generateCode(sb, element, dd, action, template, MakumbaJspAnalyzer.QL_OQL);
            out.write(sb.toString());
            out.close();
        }
    }

    /** Contains lists of already used accessKeys for each DataDefinition. */
    private Hashtable<DataDefinition, ArrayList<String>> accessKeys = new Hashtable<DataDefinition, ArrayList<String>>();

    /**
     * Starts the code generation for the given code type and DataDefinition.
     * 
     * @param queryLanguage
     *            TODO
     */
    public void generateCode(StringBuffer sb, String type, DataDefinition dd, String action,
            CodeGeneratorTemplate template, String queryLanguage) {
        generateCode(sb, type, dd, action, DataServlet.separateFieldTypes(dd, true), template, 0, queryLanguage);
    }

    /** Starts the business logic code generation for the given DataDefinition. */
    public void generateJavaBusinessLogicCode(DataDefinition dd, String packageName, boolean hasSuperLogic,
            String[] types, StringBuffer sb) {
        int indent = 0;
        String directoryName = "";
        String className = directoryName + getLogicNameFromDataDefinition(dd);
        String userName = "yourName";
        if (packageName == null) {
            packageName = "";
        }

        String ddMethodName = Logic.upperCase(dd.getName());

        if (!packageName.equals("")) {
            appendLine(sb, "package org.eu.best.privatearea;");
            appendEmptyLine(sb);
        }

        appendLine(sb, "import java.util.Dictionary;");
        appendEmptyLine(sb);
        appendLine(sb, "import org.makumba.Attributes;");
        appendLine(sb, "import org.makumba.Transaction;");
        appendLine(sb, "import org.makumba.LogicException;");
        appendLine(sb, "import org.makumba.Pointer;");

        appendEmptyLine(sb);
        appendLine(sb, "/**");
        appendLine(sb, " * TODO: add javadoc comments!");
        appendLine(sb, " *");
        appendLine(sb, " * @author " + userName);
        appendLine(
            sb,
            " * @version $Id: " + className + ",v 1.1 "
                    + new SimpleDateFormat("yyyy/MM/dd hh:mm:ss").format(new Date()) + " " + userName + " Exp $");
        appendLine(sb, " */");
        append(sb, "public class " + className);
        if (hasSuperLogic) {
            append(sb, " extends Logic");
        }
        appendLine(sb, " {");
        appendEmptyLine(sb);
        indent++;
        appendJavaLine(sb, indent,
            "// add your specific authentication checking by uncommenting & adapting the code below");
        appendJavaLine(sb, indent, "public void checkAttributes(Attributes a, Transaction t) throws LogicException {");
        indent++;
        if (hasSuperLogic) {
            appendJavaLine(sb, indent, "super.checkAttributes(a, t);");
            appendEmptyLine(sb);
        }
        appendJavaLine(sb, indent, "// do authentication checking, e.g. check for existance of an attribute by");
        appendJavaLine(sb, indent, "a.getAttribute(\"username\");");
        appendEmptyLine(sb);
        appendJavaLine(sb, indent,
            "// then add your specific authorization checking here, e.g. check for being superuser by");
        appendJavaLine(sb, indent,
            "// if (!new Boolean((String) a.getAttribute(\"superUser\")).equals(Boolean.TRUE)) {");
        indent++;
        appendJavaLine(sb, indent, "// in case of not being authorized add:");
        appendJavaLine(sb, indent,
            "// throw new UnauthorizedException(\"You are not authorized to access this page!\");");

        indent--;
        appendJavaLine(sb, indent, "// }");
        indent--;
        appendJavaLine(sb, indent, "}");
        appendEmptyLine(sb);

        addOnNewHandler(sb, indent, ddMethodName);
        addOnEditHandler(sb, indent, ddMethodName);
        addOnDeleteHandler(sb, indent, ddMethodName);

        Vector<FieldDefinition> fields = extractSetComplex(dd);
        for (int i = 0; i < fields.size(); i++) {
            FieldDefinition fd = fields.elementAt(i);
            String handlerName = Logic.upperCase(dd.getName() + "->" + fd.getName());
            addOnAddHandler(sb, indent, handlerName);
            addOnEditHandler(sb, indent, handlerName);
            addOnDeleteHandler(sb, indent, handlerName);
        }

        appendLine(sb, "}");
    }

    public static void addOnAddHandler(StringBuffer sb, int indent, String ddMethodName) {
        writeHandler(sb, "add", "Pointer p, Dictionary d, Attributes a, Transaction t", indent, ddMethodName);
    }

    public static void addOnDeleteHandler(StringBuffer sb, int indent, String ddMethodName) {
        writeHandler(sb, "delete", "Pointer p, Attributes a, Transaction t", indent, ddMethodName);
    }

    public static void addOnEditHandler(StringBuffer sb, int indent, String ddMethodName) {
        writeHandler(sb, "edit", "Pointer p, Dictionary d, Attributes a, Transaction t", indent, ddMethodName);
    }

    public static void addOnNewHandler(StringBuffer sb, int indent, String ddMethodName) {
        writeHandler(sb, "new", "Dictionary d, Attributes a, Transaction t", indent, ddMethodName);
    }

    /** Writes a handler method of the given type. */
    public static void writeHandler(StringBuffer sb, String type, String params, int indent, String ddMethodName) {
        appendJavaLine(sb, indent, "public void on_" + type + ddMethodName + "(" + params + ") throws LogicException {");
        indent++;
        appendJavaLine(sb, indent, "");
        indent--;
        appendJavaLine(sb, indent, "}");
        appendLine(sb, "");
    }

    /** Appends a String to the given StringBuffer, if the String is not empty. */
    private void append(StringBuffer sb, String s) {
        if (s != null && !s.equals("")) {
            sb.append(s);
        }
    }

    /** Adds an empty line to the given StringBuffer */
    private void appendEmptyLine(StringBuffer sb) {
        sb.append("\n");
    }

    /** Adds a java-style indented line to the given StringBuffer and adds a line break. */
    private static void appendJavaLine(StringBuffer sb, int indent, String s) {
        sb.append(getJavaIndentation(indent)).append(s).append("\n");
    }

    /** Adds a jsp-style indented line to the given StringBuffer. */
    private void appendJSP(StringBuffer sb, int indent, String s) {
        sb.append(getJSPIndentation(indent)).append(s);
    }

    /** Adds a jsp-style indented line to the given StringBuffer and adds a line break. */
    private void appendJSPLine(StringBuffer sb, int indent, String s) {
        sb.append(getJSPIndentation(indent)).append(s).append("\n");
    }

    /** Appends the given String and a line break, if the String is not empty. */
    private static void appendLine(StringBuffer sb, String s) {
        if (s != null && !s.equals("")) {
            sb.append(s).append("\n");
        }
    }

    /** Generates a form-label with access key. */
    private String formatLabelName(String name, char key) {
        int index = name.toLowerCase().indexOf(key);
        if (index == -1) {
            return StringUtils.upperCaseBeginning(name);
        } else {
            String s = name.substring(0, index);
            s += "<span class=\"accessKey\">" + name.charAt(index) + "</span>";
            s += name.substring(index + 1);
            return StringUtils.upperCaseBeginning(s);
        }
    }

    private void generateCode(StringBuffer sb, String type, DataDefinition dd, String action,
            Vector<FieldDefinition>[] processData, CodeGeneratorTemplate template, int indent, String queryLanguage) {

        long beginTime = System.currentTimeMillis();
        Vector<FieldDefinition> fields = processData[0];
        Vector<FieldDefinition> sets = processData[1];
        String labelName = getLabelNameFromDataDefinition(dd);
        action += "?" + labelName + "=${" + labelName + "Pointer}";

        try {
            // Write page header, depending on type
            appendLine(sb, template.header);
            appendLine(sb, "<%-- Makumba Generator - START OF  *** " + type.toUpperCase() + " ***  PAGE FOR OBJECT "
                    + dd + " --%>");
            if (type == TYPE_LIST) {
                generateListCode(sb, dd, template, indent, labelName, queryLanguage);
            } else if (type == TYPE_DELETE) {
                generateDeleteCode(sb, dd, template, labelName, indent, queryLanguage);
            } else {
                indent++;
                if (type == TYPE_NEWFORM) {
                    appendLine(sb, template.beforePageHeader + "New " + StringUtils.upperCaseBeginning(labelName)
                            + template.afterPageHeader);
                    appendLine(sb, "<mak:newForm type=\"" + dd + "\" action=\"" + action + "\" name=\"" + labelName
                            + "\" method=\"post\" >");
                } else if (type == TYPE_OBJECT) {
                    appendLine(sb, "<mak:object from=\"" + dd + " " + labelName + "\" where=\"" + labelName
                            + (queryLanguage.equals(MakumbaJspAnalyzer.QL_OQL) ? "=$" : ".id=:") + labelName + "\">");
                    appendLine(sb, "<mak:value expr=\"" + labelName
                            + (queryLanguage.equals(MakumbaJspAnalyzer.QL_OQL) ? "" : ".id") + "\" printVar=\""
                            + labelName + "Pointer\" />");
                    appendJSPLine(sb, indent, template.beforePageHeader + StringUtils.upperCaseBeginning(labelName)
                            + " <i><mak:value expr=\"" + labelName + "." + dd.getTitleFieldName() + "\" /></i>"
                            + template.afterPageHeader);
                } else if (type == TYPE_EDITFORM) {
                    appendLine(sb, "<mak:object from=\"" + dd + " " + labelName + "\" where=\"" + labelName
                            + (queryLanguage.equals(MakumbaJspAnalyzer.QL_OQL) ? "=$" : ".id=:") + labelName + "\">");
                    appendLine(sb, "<mak:value expr=\"" + labelName
                            + (queryLanguage.equals(MakumbaJspAnalyzer.QL_OQL) ? "" : ".id") + "\" printVar=\""
                            + labelName + "Pointer\" />");
                    appendJSPLine(sb, indent,
                        template.beforePageHeader + "Edit " + StringUtils.upperCaseBeginning(labelName)
                                + " <i><mak:value expr=\"" + labelName + "." + dd.getTitleFieldName() + "\" /></i>"
                                + template.afterPageHeader);
                    appendJSPLine(sb, indent, "<mak:editForm object=\"" + labelName + "\" action=\"" + action
                            + "\" method=\"post\">");
                    indent++;
                }

                appendJSPLine(sb, indent, template.afterFormBegin);

                appendJSPLine(sb, indent, "<%-- Makumba Generator - START OF NORMAL FIELDS --%>");
                // iterating over the normal fields
                for (int i = 0; i < fields.size(); i++) {
                    FieldDefinition fd = fields.get(i);
                    if (fd.isFixed() && fd.isNotNull() && fd.getType().equals("ptr")) {
                        // TODO?: make inputs for that pointer too
                    } else {
                        processFieldDefinition(sb, type, template, labelName, fd, indent);
                    }
                }
                if (type == TYPE_OBJECT) {
                    appendJSPLine(sb, indent, template.beforeFormEnd);
                } else if (type == TYPE_EDITFORM) {
                    writeButtons(template, sb, "Save changes", indent);
                    appendJSPLine(sb, indent, template.beforeFormEnd);
                    indent--;
                    appendJSPLine(sb, indent, "</mak:editForm>");
                }
                appendJSPLine(sb, indent, "<%-- Makumba Generator - END OF NORMAL FIELDS --%>");

                // TODO?: for newForm generate addForms for internal sets?
                if (type != TYPE_NEWFORM) {
                    // iterating over the sets
                    appendEmptyLine(sb);
                    appendJSPLine(sb, indent, "<%-- Makumba Generator - START OF SETS --%>");

                    DataServlet.logger.finer("DEBUG INFO: Number of sets of MDD " + dd + " is " + sets.size());
                    for (int i = 0; i < sets.size(); i++) {
                        FieldDefinition fd = sets.get(i);
                        DataServlet.logger.finest("DEBUG INFO: Currently processing set with fieldname " + fd.getName()
                                + " and type " + fd.getType());

                        DataDefinition setDd = getDataDefinitionFromType(fd);

                        if (setDd == null) {
                            DataServlet.logger.warning("Problem generating code - did not find field definition for set '"
                                    + fd.getName() + "' in data definition '" + dd.getName() + "'.");
                        } else {
                            // sorting out only the normal fields, we don't care about generate sets inside sets.
                            Vector<FieldDefinition> innerFields = extractInnerFields(setDd);
                            DataServlet.logger.finer("DEBUG INFO: Number of inner fields of MDD " + dd + ", subset "
                                    + setDd.getName() + " is " + innerFields.size());

                            // generate the inner set code
                            generateSetCode(sb, type, fd, action, template, innerFields, indent, labelName);
                            if (type == TYPE_EDITFORM) {
                                // for edit forms we also make add forms of inner sets.
                                generateSetCode(sb, TYPE_ADDFORM, fd, action, template, innerFields, indent, labelName);
                            }
                        }

                    } // end iterating over the sets
                    appendEmptyLine(sb);
                    appendJSPLine(sb, indent, "<%-- Makumba Generator - END OF SETS --%>");
                    appendEmptyLine(sb);
                    indent--;
                }

                // closing forms & lists
                if (type == TYPE_NEWFORM) {
                    writeButtons(template, sb, "Add", indent);
                    appendJSPLine(sb, indent, template.beforeFormEnd);
                    appendLine(sb, "</mak:newForm>");
                }
                if (type == TYPE_ADDFORM || type == TYPE_OBJECT) {
                    appendJSPLine(sb, indent, template.beforeFormEnd);
                    appendLine(sb, "</mak:object>");
                }
                if (type == TYPE_EDITFORM) {
                    appendJSPLine(sb, indent, template.beforeFormEnd);
                    appendLine(sb, "</mak:object>");
                }
            }
            appendEmptyLine(sb);
            appendLine(sb, "<%-- Makumba Generator - END OF *** " + type.toUpperCase() + " ***  PAGE FOR OBJECT " + dd
                    + " --%>");
            appendLine(sb, template.footer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        DataServlet.logger.info("Generation of " + getFileNameFromObject(dd, type) + " took "
                + (System.currentTimeMillis() - beginTime) + " ms");
    }

    /**
     * Generates the code for a delete form.
     * 
     * @param queryLanguage
     *            TODO
     */
    private void generateDeleteCode(StringBuffer sb, DataDefinition dd, CodeGeneratorTemplate template,
            String labelName, int indent, String queryLanguage) {
        appendLine(sb, template.beforePageHeader + "Delete confirmation" + template.afterPageHeader);
        appendLine(
            sb,
            "<mak:object from=\"" + dd + " " + labelName + "\" where=\"" + labelName
                    + (queryLanguage.equals(MakumbaJspAnalyzer.QL_OQL) ? "=$" : ".id=:") + labelName + "\">");
        indent++;
        appendJSPLine(sb, indent,
            "Delete " + labelName + " '<mak:value expr=\"" + labelName + "." + dd.getTitleFieldName() + "\" />'?");
        appendJSPLine(sb, indent, "<a href=\"javascript:back();\">No</a> &nbsp;");
        appendJSPLine(sb, indent,
            "<mak:delete object=\"" + labelName + "\" action=\"" + getFileNameFromObject(dd, TYPE_LIST) + "\">");
        appendJSPLine(sb, indent + 1, "Delete");
        appendJSPLine(sb, indent, "</mak:delete>");
        indent--;
        appendLine(sb, "</mak:object>");
    }

    /** generate inner field code for mak:new/editForm. */
    private void generateInnerFieldFormCode(CodeGeneratorTemplate template, StringBuffer sb,
            Vector<FieldDefinition> innerFields, int indent) throws IOException {
        for (int i = 0; i < innerFields.size(); i++) {
            FieldDefinition innerFd = innerFields.get(i);
            appendJSPLine(sb, indent, template.beforeField);
            writeInput(sb, template, innerFd, indent);
        }
    }

    /**
     * Generate the code for listing objects.
     * 
     * @param queryLanguage
     *            TODO
     */
    private void generateListCode(StringBuffer sb, DataDefinition dd, CodeGeneratorTemplate template, int indent,
            String labelName, String queryLanguage) {

        appendJSPLine(sb, indent, template.beforePageHeader + "List " + StringUtils.upperCaseBeginning(labelName) + "s"
                + template.afterPageHeader);

        // links to this file for sort-by links
        String cgiParam = "?" + labelName + "=<mak:value expr=\"" + labelName
                + (queryLanguage.equals(MakumbaJspAnalyzer.QL_OQL) ? "" : ".id") + "\" />";
        String thisFile = getFileNameFromObject(dd, TYPE_LIST) + "?sortBy=";

        // converting parameters --> EL sort by values
        appendEmptyLine(sb);
        appendLine(sb, "<c:choose>");
        indent++;
        appendJSPLine(sb, indent, "<c:when test=\"${param.sortBy == 'created'}\">");
        appendJSPLine(sb, indent + 1, "<c:set var=\"sortBy\" value=\"" + labelName + ".TS_create\" />");
        appendJSPLine(sb, indent, "</c:when>");

        appendJSPLine(sb, indent, "<c:when test=\"${param.sortBy == 'modified'}\">");
        appendJSPLine(sb, indent + 1, "<c:set var=\"sortBy\" value=\"" + labelName + ".TS_modify\" />");
        appendJSPLine(sb, indent, "</c:when>");

        appendJSPLine(sb, indent, "<c:when test=\"${!empty param.sortBy}\">");
        appendJSPLine(sb, indent + 1, "<c:set var=\"sortBy\" value=\"" + labelName + ".${param.sortBy}\" />");
        appendJSPLine(sb, indent, "</c:when>");

        appendJSPLine(sb, indent, "<c:otherwise>");
        appendJSPLine(sb, indent + 1, "<c:set var=\"sortBy\" value=\"" + labelName + "." + dd.getTitleFieldName()
                + "\" />");
        appendJSPLine(sb, indent, "</c:otherwise>");
        indent--;
        appendLine(sb, "</c:choose>");
        appendEmptyLine(sb);

        // page header --> names of fields with sort-by links
        appendJSPLine(sb, indent, template.afterFormBegin);
        appendJSPLine(sb, indent, template.beforeField);
        appendJSPLine(sb, indent, template.beforeFieldName + "<a href=\"" + thisFile + "created\">#</a>"
                + template.afterFieldName);
        appendJSPLine(sb, indent, template.beforeFieldName + "<a href=\"" + thisFile + dd.getTitleFieldName() + "\">"
                + dd.getTitleFieldName() + "</a>" + template.afterFieldName);
        appendJSPLine(sb, indent, template.beforeFieldName + "<a href=\"" + thisFile + "created\">Created</a>"
                + template.afterFieldName);
        appendJSPLine(sb, indent, template.beforeFieldName + "<a href=\"" + thisFile + "modified\">Modified</a>"
                + template.afterFieldName);
        appendJSPLine(sb, indent, template.beforeFieldName + "Actions" + template.afterFieldName);
        appendJSPLine(sb, indent, template.afterField);

        // actual mak:list generation
        indent++;
        appendJSPLine(sb, indent, "<mak:list from=\"" + dd + " " + labelName + "\" orderBy=\"#{sortBy}\">");

        appendJSPLine(sb, indent, template.beforeField);
        appendJSPLine(sb, indent, template.beforeFieldTag + "${mak:count()}" + template.afterFieldTag);
        appendJSPLine(sb, indent,
            template.beforeFieldTag + "<mak:value expr=\"" + labelName + "." + dd.getTitleFieldName() + "\" />"
                    + template.afterFieldTag);
        appendJSPLine(sb, indent, template.beforeFieldTag + "<mak:value expr=\"" + labelName
                + ".TS_create\" format=\"yyyy-MM-dd hh:mm:ss\" />" + template.afterFieldTag);
        appendJSPLine(sb, indent, template.beforeFieldTag + "<mak:value expr=\"" + labelName
                + ".TS_modify\" format=\"yyyy-MM-dd hh:mm:ss\" />" + template.afterFieldTag);
        appendJSPLine(sb, indent, template.beforeFieldTag);
        append(sb, "<a href=\"" + getFileNameFromObject(dd, TYPE_OBJECT) + cgiParam + "\">[View]</a> ");
        append(sb, "<a href=\"" + getFileNameFromObject(dd, TYPE_EDITFORM) + cgiParam + "\">[Edit]</a> ");
        append(sb, "<a href=\"" + getFileNameFromObject(dd, TYPE_DELETE) + cgiParam + "\">[Delete]</a> ");
        append(sb, template.afterFieldTag);
        appendJSPLine(sb, indent, template.afterField);

        appendJSPLine(sb, indent, "</mak:list>");
        indent--;
        appendJSPLine(sb, indent, template.beforeFormEnd);
        appendLine(sb, "<a href=\"" + getFileNameFromObject(dd, TYPE_NEWFORM) + "\">[New]</a>");
    }

    /** Generate code for sets for addForm, editForm and object. */
    private void generateSetCode(StringBuffer sb, String type, FieldDefinition fd, String action,
            CodeGeneratorTemplate template, Vector<FieldDefinition> innerFields, int indent, String labelName)
            throws IOException {
        appendEmptyLine(sb);
        if (type == TYPE_ADDFORM) {
            appendJSPLine(sb, indent, "<%-- Makumba Generator - START ADDFORM FOR FIELD " + fd.getName() + " --%>");
            appendJSPLine(sb, indent, "<mak:addForm object=\"" + labelName + "\" field=\"" + fd.getName()
                    + "\" action=\"" + action + "\" method=\"post\" >");
            indent++;
            appendJSPLine(sb, indent, template.afterFormBegin);

            // launching generation of inner fields
            generateInnerFieldFormCode(template, sb, innerFields, indent);

            // closing form
            writeButtons(template, sb, "Add", indent);
            appendJSPLine(sb, indent, template.beforeFormEnd);
            indent--;
            appendJSPLine(sb, indent, "</mak:addForm>");
            appendJSPLine(sb, indent, "<%-- Makumba Generator - END ADDFORM FOR FIELD " + fd.getName() + " --%>");
        } else if (type == TYPE_EDITFORM) {
            appendJSPLine(sb, indent, "<%-- Makumba Generator - START LIST & EDITFORM FOR FIELD " + fd.getName()
                    + " --%>");
            appendJSPLine(sb, indent, template.beforePageHeaderLevel2 + StringUtils.upperCaseBeginning(fd.getName())
                    + template.afterPageHeaderLevel2);
            appendJSPLine(sb, indent, "<mak:list from=\"" + labelName + "." + fd.getName() + " " + fd.getName()
                    + "\" >");
            indent++;
            appendJSPLine(sb, indent, "<mak:editForm object=\"" + fd.getName() + "\" action=\"" + action
                    + "\" method=\"post\">");
            indent++;
            appendJSPLine(sb, indent, template.afterFormBegin);

            // launching generation of inner fields
            generateInnerFieldFormCode(template, sb, innerFields, indent);

            // closing form
            writeButtons(template, sb, "Save changes", indent);
            appendJSPLine(sb, indent, template.beforeFormEnd);
            indent--;
            appendJSPLine(sb, indent, "</mak:editForm>");
            indent--;
            appendJSPLine(sb, indent, "</mak:list>");

            // create add form for this set

            appendJSPLine(sb, indent, "<%-- Makumba Generator - END LIST & EDITFORM FOR FIELD " + fd.getName()
                    + " --%>");
        } else if (type == TYPE_OBJECT) {
            // we arrange fields horizontally, as inner sets should not have too many fields.

            appendJSPLine(sb, indent, "<%-- Makumba Generator - START LIST FIELD " + fd.getName() + " --%>");
            appendJSPLine(sb, indent, template.beforePageHeaderLevel2 + StringUtils.upperCaseBeginning(fd.getName())
                    + template.afterPageHeaderLevel2);
            appendJSPLine(sb, indent, template.afterFormBegin);
            appendJSPLine(sb, indent, template.beforeField);
            for (int i = 0; i < innerFields.size(); i++) {
                FieldDefinition innerFd = innerFields.get(i);
                appendJSPLine(sb, indent, template.beforeFieldName + innerFd.getName() + template.afterFieldName);
            }
            appendJSPLine(sb, indent, template.afterField);
            indent++;
            appendJSPLine(sb, indent, "<mak:list from=\"" + labelName + "." + fd.getName() + " " + fd.getName() + "\">");
            appendJSPLine(sb, indent, template.beforeField);

            // launching generation of inner fields
            for (int i = 0; i < innerFields.size(); i++) {
                appendJSPLine(sb, indent, template.beforeFieldTag + "<mak:value expr=\"" + fd.getName() + "."
                        + innerFields.get(i).getName() + "\"/>" + template.afterFieldTag);
            }

            appendJSPLine(sb, indent, template.afterField);

            // closing forms
            appendJSPLine(sb, indent, "</mak:list>");
            indent--;
            appendJSPLine(sb, indent, template.beforeFormEnd);
            appendJSPLine(sb, indent, "<%-- Makumba Generator - END LIST FOR FIELD " + fd.getName() + " --%>");
        }
    }

    /** Returns a valid, not-yet used access key for a field name. */
    private char getAccessKey(DataDefinition dd, String fieldName) {
        ArrayList<String> usedKeys = accessKeys.get(dd);
        if (usedKeys == null) {
            usedKeys = new ArrayList<String>(DEFAULTUSED_ACCESS_KEYS);
            accessKeys.put(dd, usedKeys);
        }
        fieldName = fieldName.toLowerCase();
        for (int i = 0; i < fieldName.length(); i++) {
            char key = fieldName.charAt(i);
            if (key != ' ') {
                if (!usedKeys.contains(String.valueOf(key))) {
                    usedKeys.add(String.valueOf(key));
                    return key;
                }
            }
        }
        return ' ';
    }

    /** Java-style indenting of 4 spaces. */
    private static StringBuffer getJavaIndentation(int indent) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < indent; i++) {
            sb.append("    ");
        }
        return sb;
    }

    /** JSP-style indenting of 2 spaces. */
    private StringBuffer getJSPIndentation(int indent) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
        return sb;
    }

    /**
     * Process all fields from a field definition, by adding either mak:value or mak:inputs, depending on the given
     * type.
     */
    private void processFieldDefinition(StringBuffer sb, String type, CodeGeneratorTemplate template, String labelName,
            FieldDefinition fd, int indent) throws IOException {
        appendJSPLine(sb, indent, template.beforeField);
        if (type == TYPE_NEWFORM || type == TYPE_EDITFORM) {
            writeInput(sb, template, fd, indent);
        } else if (type == TYPE_OBJECT) {

            String fieldName = fd.getName();
            if (fd.getDescription() != null && !fd.getDescription().equals("")) {
                fieldName = fd.getDescription();
            }

            appendJSPLine(sb, indent, template.beforeFieldName + fieldName + template.afterFieldName);
            appendJSP(sb, indent, template.beforeFieldTag);
            append(sb, "<mak:value expr=\"" + labelName + "." + fd.getName() + "\"/>");
        }
        appendLine(sb, template.afterFieldTag);
        appendJSPLine(sb, indent, template.afterField);
    }

    /** Writes add/cancel/rest buttons for a form. */
    private void writeButtons(CodeGeneratorTemplate template, StringBuffer sb, String buttonText, int indent) {
        appendJSPLine(sb, indent, template.beforeField);
        appendJSP(sb, indent, template.beforeFieldTag);
        writeSubmitButton(template, sb, buttonText, indent);
        writeResetButton(template, sb, indent);
        writeCancelButton(template, sb, indent);
        appendJSPLine(sb, indent, template.afterFieldTag);
        appendJSPLine(sb, indent, template.afterField);
    }

    private void writeCancelButton(CodeGeneratorTemplate template, StringBuffer sb, int indent) {
        appendJSP(sb, indent, "<input type=\"reset\" value=\"Cancel\" accessKey=\"" + 'C'
                + "\" onClick=\"javascript:back();\">");
    }

    private void writeResetButton(CodeGeneratorTemplate template, StringBuffer sb, int indent) {
        appendJSP(sb, indent, "<input type=\"reset\" accessKey=\"" + 'R' + "\">");
    }

    private void writeSubmitButton(CodeGeneratorTemplate template, StringBuffer sb, String buttonText, int indent) {
        appendJSP(sb, indent, "<input type=\"submit\" value=\"" + buttonText + "\" accessKey=\"" + buttonText.charAt(0)
                + "\">");
    }

    /** generates a form input field. */
    private void writeInput(StringBuffer sb, CodeGeneratorTemplate template, FieldDefinition fd, int indent) {
        String fieldName;
        if (fd.getDescription() != null && !fd.getDescription().equals("")) {
            fieldName = fd.getDescription();
        } else {
            fieldName = fd.getName();
        }
        fieldName = fieldName.trim();

        char key = getAccessKey(fd.getDataDefinition(), fieldName);

        appendJSPLine(sb, indent,
            template.beforeFieldName + "<label for=\"" + fd.getName() + "\">" + formatLabelName(fieldName, key)
                    + "</label>" + template.afterFieldName);
        appendJSP(sb, indent, template.beforeFieldTag);
        append(sb, "<mak:input field=\"" + fd.getName() + "\" styleId=\"" + fd.getName() + "\" accessKey=\"" + key
                + "\" />");
    }

    /** Gathers all fields that are not sets and ptrRel from a given DataDefinition. */
    private Vector<FieldDefinition> extractInnerFields(DataDefinition dd) {
        Vector<FieldDefinition> innerFields = new Vector<FieldDefinition>();
        if (dd != null) {
            for (int i = 0; i < dd.getFieldDefinitions().size(); i++) {
                FieldDefinition fd = dd.getFieldDefinition(i);
                if (!fd.isDefaultField() && fd.getIntegerType() != FieldDefinition._ptrRel
                        && fd.shouldEditBySingleInput()) {
                    innerFields.add(fd);
                }
            }
        }
        return innerFields;
    }

    /** Extracts all complex sets from a given DataDefinition. */
    public static Vector<FieldDefinition> extractSetComplex(DataDefinition dd) {
        Vector<FieldDefinition> sets = new Vector<FieldDefinition>();
        for (int i = 0; i < dd.getFieldDefinitions().size(); i++) {
            FieldDefinition fd = dd.getFieldDefinition(i);
            if (fd.getIntegerType() == FieldDefinition._setComplex) {
                sets.add(fd);
            }
        }
        return sets;
    }

    // FIXME: this should probably become part of FieldDefinition
    private DataDefinition getDataDefinitionFromType(FieldDefinition fd) {
        if (fd.isInternalSet()) {
            return fd.getSubtable();
        }
        if (fd.isExternalSet()) {
            return fd.getDataDefinition();
        }
        if (fd.isPointer()) {
            return fd.getForeignTable();
        }
        return null;
    }

}
