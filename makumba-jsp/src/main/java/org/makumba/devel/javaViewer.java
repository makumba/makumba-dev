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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.makumba.DataDefinition;
import org.makumba.DataDefinitionNotFoundError;
import org.makumba.analyser.engine.JavaParseData;
import org.makumba.analyser.engine.SourceSyntaxPoints;
import org.makumba.analyser.engine.SyntaxPoint;
import org.makumba.analyser.engine.TomcatJsp;
import org.makumba.commons.StringUtils;
import org.makumba.commons.tags.MakumbaJspConfiguration;
import org.makumba.providers.DataDefinitionProvider;

/**
 * the java viewer. It should be a filter from another (mb third-party) viewer that links known .java and .mdd sources.
 * See SourceViewServlet for the filter architecture
 * 
 * @version $Id$
 * @author Stefan Baebler
 * @author Rudolf Mayer
 */
public class javaViewer extends LineViewer {
    public static Map<String, String> javaSyntaxProperties = MakumbaJspConfiguration.getJavaViewerSyntaxStyles();

    private static DataDefinitionProvider ddp = DataDefinitionProvider.getInstance();

    private boolean compiledJSP = false;

    private boolean haveFile = false;

    private SourceSyntaxPoints syntaxPoints;

    private SyntaxPoint[] sourceSyntaxPoints;

    private JavaParseData javaParseData;

    public javaViewer(HttpServletRequest req) throws Exception {
        super(true, req);
        jspClasspath = TomcatJsp.getContextCompiledJSPDir(request.getSession().getServletContext());

        viewerName = "Java Viewer";
        contextPath = req.getContextPath();
        virtualPath = DevelUtils.getVirtualPath(req, MakumbaJspConfiguration.getToolLocation(DeveloperTool.JAVA_VIEWER));
        if (virtualPath == null) {
            virtualPath = "/";
        } else {
            virtualPath = virtualPath.substring(1);
        }

        URL url;
        if (virtualPath.endsWith(".java")) {
            url = org.makumba.commons.ClassResource.get(virtualPath);
        } else {
            url = org.makumba.commons.ClassResource.get(virtualPath.replace('.', '/') + ".java");
        }
        if (url != null) {
            setSearchLevels(false, false, false, true);
            haveFile = true;
        } else {
            String filePath = jspClasspath + "/" + virtualPath.replace('.', '/') + ".java";
            File jspClassFile = new File(filePath);
            if (jspClassFile.exists()) {
                url = new URL("file://" + filePath);
                setSearchLevels(false, false, false, true); // for compiled JSP files, we search only for MDDs.
                compiledJSP = true;
                haveFile = true;
            } else {
                String s = virtualPath;
                if (s.startsWith("/")) {
                    s = s.substring(1);
                }
                url = org.makumba.commons.ClassResource.get(s.replace('.', '/'));
            }
        }

        if (haveFile) { // we actually read a file
            // uncomment this for testing purposes to clean the analyzer cache.
            // NamedResources.cleanStaticCache(JavaParseData.analyzedPages);
            javaParseData = JavaParseData.getParseData("/", url.getFile(), JavaSourceAnalyzer.getInstance());
            javaParseData.getAnalysisResult(null);
            syntaxPoints = javaParseData.getSyntaxPoints();
            sourceSyntaxPoints = javaParseData.getSyntaxPoints().getSyntaxPoints();
            addImportedPackages(javaParseData.getImportedPackages());
            importedClasses = javaParseData.getImportedClasses();
        }
        readFromURL(url);
    }

    /**
     * Utilises the super-class' method, and performs additionally syntax highlighting for java keywords.
     * 
     * @see org.makumba.devel.LineViewer#parseLine(java.lang.String)
     */
    @Override
    public String parseLine(String s) {
        if (s.length() == 0 || s.trim().length() == 0) {
            return s;
        }
        String result = super.parseLine(s);
        if (compiledJSP) {
            return result;
        }
        for (String keyWord : JavaParseData.AllHighlightableTokens) {
            // we highlight the word if we have a style defined for this syntax point type
            final String substitute = keyWord + " ";
            if (JavaParseData.shallHighlight(keyWord) && result.contains(substitute)) {
                result = result.replaceAll(substitute, "<span class=\"" + keyWord + "\">" + keyWord + "</span> ");
            }
        }
        return result;
    }

    /** parse the text and write the output */
    @Override
    public void parseText(PrintWriter writer) throws IOException {
        long begin = System.currentTimeMillis();
        printPageBegin(writer);

        SyntaxPoint lastSyntaxPoint = null;
        int insideComment = 0;

        for (int j = 0; sourceSyntaxPoints != null && j < sourceSyntaxPoints.length; j++) {
            SyntaxPoint currentSyntaxPoint = sourceSyntaxPoints[j];
            String type = currentSyntaxPoint.getType();
            int currentLine = currentSyntaxPoint.getLine();

            if (type.equals("TextLine") && currentSyntaxPoint.isBegin()) { // begin of line found -> print line numbers
                if (printLineNumbers) {
                    writer.print("\n");
                    writeLineNumber(writer, currentLine, !hideLineNumbers);
                }
            } else {
                String beforeSyntaxPoint = syntaxPoints.getLineText(currentLine).substring(
                    lastSyntaxPoint.getColumn() - 1, currentSyntaxPoint.getColumn() - 1);
                if (type.equals("TextLine") && !currentSyntaxPoint.isBegin()) { // end of line found
                    writer.print(parseLine(htmlEscape(beforeSyntaxPoint)));
                } else { // we are in a syntax point
                    if (currentSyntaxPoint.isBegin()) { // we are at the beginning of the point
                        if (JavaParseData.isCommentSyntaxPoint(currentSyntaxPoint.getType())) {
                            insideComment++;
                        }
                        writer.print(parseLine(htmlEscape(beforeSyntaxPoint)));
                        // we treat class imports at the end of the syntax point
                        if (!JavaParseData.isClassUsageSyntaxPoint(currentSyntaxPoint.getType())) {
                            // we don't highlight literals inside comments
                            if (!(insideComment > 0 && type.equals(JavaParseData.JAVA_STRING_LITERAL) && JavaParseData.shallHighlight(type))) {
                                writer.print("<span class=\"" + type + "\">");
                            }
                        }

                    } else { // we have the end of the point
                        if (JavaParseData.isCommentSyntaxPoint(currentSyntaxPoint.getType())) {
                            insideComment--;
                        }
                        if (JavaParseData.isClassUsageSyntaxPoint(currentSyntaxPoint.getType())) {
                            // generate links to used classes
                            Class<?> webappClass = findClass(beforeSyntaxPoint);
                            String classLink = null;
                            if (webappClass == null) {
                                webappClass = findClassSimple(beforeSyntaxPoint);
                            }
                            if (webappClass != null) {
                                classLink = formatClassLink(webappClass, null, beforeSyntaxPoint);
                            }
                            if (classLink != null) {
                                writer.print(classLink);
                            } else {
                                writer.print(beforeSyntaxPoint);
                            }
                            writer.print("</a>");
                        } else if (currentSyntaxPoint.getType().equals("JavaMethodInvocation")) {
                            String object = null;
                            String method = null;
                            String[] parts = null;
                            if (beforeSyntaxPoint.indexOf(".") != -1) {
                                // we actually do have a "." inside the syntax point
                                parts = beforeSyntaxPoint.split("\\.");
                            } else { // we need to go back one more syntax point
                                parts = syntaxPoints.getLineText(currentLine).substring(
                                    sourceSyntaxPoints[j - 2].getColumn() - 1, currentSyntaxPoint.getColumn() - 1).split(
                                    "\\.");
                            }

                            if (parts.length > 1) {
                                object = parts[0];
                                method = parts[1];
                            }
                            Class<?> variableClass = null;
                            String classLink = null;
                            if (object.equals("super")) { // provide link to super class
                                variableClass = findClass(javaParseData.getSuperClass());
                                classLink = formatClassLink(variableClass, method, beforeSyntaxPoint);
                            } else if (!object.equals("this")) { // don't care about usage of this
                                String className = javaParseData.getDefinedObjectClassName(object,
                                    currentSyntaxPoint.getPosition());
                                if (className == null) { // try to check if we use a static field
                                    className = object;
                                }
                                if (className != null) {
                                    variableClass = findClass(className);
                                    if (variableClass != null) {
                                        classLink = object + "." + formatClassLink(variableClass, method, method);
                                    } else {
                                        classLink = object + "." + method;
                                    }
                                }
                            }
                            if (classLink != null) {
                                writer.print(classLink);
                            } else {
                                writer.print(beforeSyntaxPoint);
                            }
                            javaParseData.getViewedClass();
                        } else if (currentSyntaxPoint.getType().equals("MakumbaFormHandler")) {
                            // add links to makumba form handlers, e.g. on_editMdd(..)
                            String[] parts = splitHandlerMethodName(beforeSyntaxPoint);
                            DataDefinition dd = null;
                            if (parts != null) {
                                String mddName = findMddNameFromHandler(parts[1]);
                                try {
                                    dd = ddp.getDataDefinition(mddName);
                                    writer.print(parts[0] + "<a href=\"" + contextPath
                                            + MakumbaJspConfiguration.getToolLocation(DeveloperTool.MDD_VIEWER)
                                            + dd.getName() + "\" title=\"'" + parts[2] + "'-handler for "
                                            + dd.getName() + "\" class=\"classLink\">" + parts[1] + "</a>");
                                } catch (DataDefinitionNotFoundError e) {
                                    mddName = findMddNameFromHandler(parts[1], true);
                                    try {
                                        dd = ddp.getDataDefinition(mddName);
                                        DataDefinition parentDd = dd.getParentField().getDataDefinition();
                                        writer.print(parts[0] + "<a href=\"" + contextPath
                                                + MakumbaJspConfiguration.getToolLocation(DeveloperTool.MDD_VIEWER)
                                                + "/" + parentDd.getName() + "\" title=\"'" + parts[2]
                                                + "'-handler for " + dd.getName() + "\" class=\"classLink\">"
                                                + parts[1] + "</a>");
                                    } catch (DataDefinitionNotFoundError e1) {
                                        // do nothing, just don't use this possible MDD
                                    } catch (NullPointerException e1) {
                                        // do nothing, just don't use this possible MDD
                                    }
                                }
                            }
                            if (dd == null) { // did not find the mdd --> just continue without displaying a link
                                writer.print(parseLine(htmlEscape(beforeSyntaxPoint)));
                            }
                        } else {
                            writer.print(parseLine(htmlEscape(beforeSyntaxPoint)));
                            // we don't highlight literals inside comments
                            if (!(insideComment > 0 && currentSyntaxPoint.getType().equals(
                                JavaParseData.JAVA_STRING_LITERAL))
                                    && JavaParseData.shallHighlight(type)) {
                                writer.print("</span>");
                            }
                        }
                    }
                }
            }
            lastSyntaxPoint = currentSyntaxPoint; // move pointer to last syntax Point
        }

        printPageEnd(writer);
        double timeTaken = System.currentTimeMillis() - begin;
        java.util.logging.Logger.getLogger("org.makumba.org.makumba.devel.sourceViewer").info(
            "Java sourcecode viewer took :" + timeTaken / 1000.0 + " seconds");
    }

    private String findMddNameFromHandler(String encodedMddName) {
        return findMddNameFromHandler(encodedMddName, false);
    }

    /**
     * Discovers the name of an MDD from an encoded MDD name. Used for discovering the name of the MDD from a handler
     * method. e.g. general.address.City becomes on_editGeneralAddressCity in an edit handler.
     */
    private String findMddNameFromHandler(String encodedMddName, boolean upperCaseSecondButLast) {
        String mddName = "";
        ArrayList<String> partList = new ArrayList<String>();

        // Pattern camelCase = Pattern.compile("[^A-Z*|A-Z^A-Z*]*");
        // Pattern camelCase = Pattern.compile("[A-Z(^A-Z)*]*");
        Pattern camelCase = Pattern.compile("[A-Z]");
        Matcher m = camelCase.matcher(encodedMddName);
        int currentPos = 0;
        while (m.find()) {
            if (m.start() > currentPos) {
                String s = encodedMddName.substring(currentPos, m.start());
                partList.add(s);
                currentPos = m.start();
            }
        }

        partList.add(encodedMddName.substring(currentPos));
        String[] parts = partList.toArray(new String[partList.size()]);
        for (int i = 0; i < parts.length; i++) {
            if (upperCaseSecondButLast) {
                if (i + 2 < parts.length) {
                    mddName += StringUtils.lowerCaseBeginning(parts[i]) + ".";
                } else if (i + 2 == parts.length) {
                    mddName += parts[i] + ".";
                } else {
                    mddName += StringUtils.lowerCaseBeginning(parts[i]);
                }

            } else {
                if (i + 1 < parts.length) {
                    mddName += StringUtils.lowerCaseBeginning(parts[i]) + ".";
                } else {
                    mddName += parts[i];
                }
            }
        }
        return mddName;
    }

    /**
     * splits the name of a handler method into different parts. For e.g. on_editGeneralAddressCity, the return values
     * are as follows:<br>
     * index [0]: handler type (e.g. <i>on_edit</i>).<br>
     * index [1]: encoded MDD name (e.g. <i>GeneralAddressCity</i>).<br>
     * index [2]: printable name of the handler action (e.g. <i>Edit</i>):
     */
    private String[] splitHandlerMethodName(String handlerName) {
        String encodedMddName = null;
        String[] formTypes = { "on_new", "on_edit", "on_add", "on_delete" };
        for (int i = 0; encodedMddName == null && i < formTypes.length; i++) {
            if (handlerName.indexOf(formTypes[i]) != -1) {
                encodedMddName = handlerName.substring(formTypes[i].length());
            }
        }
        String type = handlerName.substring(handlerName.indexOf("on_") + "on_".length(),
            handlerName.indexOf(encodedMddName));
        return new String[] { handlerName.substring(0, handlerName.indexOf(encodedMddName)), encodedMddName,
                type.substring(0, 1).toUpperCase() + type.substring(1) };
    }

    @Override
    public void navigation(PrintWriter w) {

        printFileRelations(w);
        DevelUtils.printNavigationButton(w, "Java", "#", "", 1);
        String p = virtualPath;
        if (p.endsWith(".java")) {
            p = p.substring(0, p.indexOf(".java"));
        }
        p = p.replaceAll("\\.", "/");
        String path = contextPath + MakumbaJspConfiguration.getToolLocation(DeveloperTool.JAVA_VIEWER)
                + p.substring(0, p.lastIndexOf('/') + 1);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        DevelUtils.printNavigationButton(w, "browse", path, "", 0);
        DevelUtils.writeDevelUtilLinks(w, DeveloperTool.JAVA_VIEWER.getKey(), contextPath);
    }

}
