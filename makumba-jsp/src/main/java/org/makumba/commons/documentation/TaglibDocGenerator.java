package org.makumba.commons.documentation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.makumba.commons.tags.MakumbaTLDGenerator;

/**
 * This class generates the taglib documentation JSPWiki files based on the taglib-skeleton.tld file
 * 
 * @author Manuel Bernhardt <manuel@makumba.org>
 * @version $Id: TaglibDocGenerator.java,v 1.1 Nov 16, 2009 2:57:04 PM manu Exp $
 */
public class TaglibDocGenerator {

    private File outputDir;

    private HashMap<String, Element> processedElements = new HashMap<String, Element>();

    private static final String[][] genericAttributes = {
            { "Form-specific HTML tag attribute",
                    "The content is copied to the resulting <form...> tag. Careful with (escaping) quotes." },
            { "Generic HTML tag attribute",
                    "The content is copied to the resulting html tag. Careful with (escaping) quotes." },
            { "Input-specific HTML tag attribute",
                    "The content is copied to the resulting <input...> tag. Careful with (escaping) quotes." } };

    public static void main(String[] args) {

        String inputXMLPath = args[0] + File.separator + MakumbaTLDGenerator.TAGLIB_SKELETON;
        String outputPath = args[1];

        new TaglibDocGenerator(inputXMLPath, outputPath);
    }

    public TaglibDocGenerator(String inputXMLPath, String outputPath) {

        File xml = new File(inputXMLPath);
        if (!xml.exists()) {
            throw new RuntimeException("Could not find input file " + inputXMLPath);
        }

        outputDir = new File(new File(outputPath).getAbsolutePath());
        if (!outputDir.isDirectory() || !outputDir.exists()) {
            throw new RuntimeException("Output path " + outputPath + " does not exist or is not a directory");
        }
        System.out.println("Generating taglib documentation in " + outputPath + " based on file " + xml);

        SAXReader saxReader = new SAXReader();
        Document document = null;
        try {
            document = saxReader.read(xml);
        } catch (DocumentException e) {
            throw new RuntimeException("Could not read TLD XML file", e);
        }

        generateTaglibDocumentation(document);

    }

    private MultiValueMap tags = new MultiValueMap();

    private MultiValueMap functions = new MultiValueMap();

    /**
     * Generates all the taglib documentation based on the <tag> and <function> elements found in the tld-documented XML
     * 
     * @param document
     *            the DOM representation of the XML document
     */
    private void generateTaglibDocumentation(Document document) {

        Element root = document.getRootElement();

        // get all the tag and function elements
        for (Element e : getAllElements(root)) {
            boolean isTag = e.getName().equals("tag") && !e.elementText(getTagNameSyntax()).equals("rickroll");
            boolean isFunction = e.getName().equals("function");

            final String errorMsg = "Error processing element ";

            // keep names in separate places
            if (isTag) {
                tags.put(e.elementText(getTagNameSyntax()), null);
            }
            if (isFunction) {
                functions.put(e.elementText(getFunctionNameSyntax()), null);
            }

            // pre-processing for referred attributes: modify the document tree to include referenced attributes
            if (isTag) {
                for (Element tagContent : getAttributeList(e)) {
                    if (tagContent.getName().equals("attribute")) {
                        if (tagContent.attributeValue("name") != null
                                && tagContent.attributeValue("specifiedIn") != null) {
                            // have a referring attribute
                            MakumbaTLDGenerator.replaceReferencedAttribute(processedElements, errorMsg,
                                e.element(getElementNameSyntax(isTag)).getText(), tagContent);
                        }
                    }
                }
            }

            // generate tag doc
            try {
                if (isTag || isFunction) {
                    generateElementFile(e, isTag);
                }
            } catch (FileNotFoundException io) {
                System.err.println("Cannot find file: " + io.getMessage());
            } catch (IOException io2) {
                throw new RuntimeException("Cannot create generated file", io2);
            } finally {
                // we keep a reference to the processed attributes because we need this for the attribute referrer
                // resolution
                if (e.getName().equals("tag")) {
                    processedElements.put(e.elementText(getTagNameSyntax()), e);
                }
            }
        }

        @SuppressWarnings("unchecked")
        List<String> tagNames = new ArrayList<String>(tags.keySet());

        @SuppressWarnings("unchecked")
        List<String> functionNames = new ArrayList<String>(functions.keySet());

        Collections.sort(tagNames);
        Collections.sort(functionNames);

        generateIndexFile(tagNames, functionNames);

        generateMoveStatements();

    }

    private void generateMoveStatements() {
        final String svnRootPath = "https://makumba.svn.sourceforge.net/svnroot/makumba/trunk/makumba-documentation/wikidoc/";

        File f = new File(this.outputDir.getAbsolutePath() + File.separator + "renameAll.sh");
        try {
            f.createNewFile();
            PrintStream s = new PrintStream(new FileOutputStream(f));

            for (Object name : tags.keySet()) {
                String key = (String) name;
                name = StringUtils.capitalize((String) name);
                s.println("svn move " + svnRootPath + name + "Tag" + "Description" + ".txt" + " " + svnRootPath + "JSP"
                        + name + "Tag" + "Description" + ".txt" + " " + "-m \"Renaming generated documentation files\"");
                s.println("svn move " + svnRootPath + name + "Tag" + "Description" + ".properties" + " " + svnRootPath
                        + "JSP" + name + "Tag" + "Description" + ".properties" + " "
                        + "-m \"Renaming generated documentation files\"");
                s.println("svn move " + svnRootPath + name + "Tag" + "Example" + ".txt" + " " + svnRootPath + "JSP"
                        + name + "Tag" + "Example" + ".txt" + " " + "-m \"Renaming generated documentation files\"");
                s.println("svn move " + svnRootPath + name + "Tag" + "Example" + ".properties" + " " + svnRootPath
                        + "JSP" + name + "Tag" + "Example" + ".properties" + " "
                        + "-m \"Renaming generated documentation files\"");
                Collection<?> c = tags.getCollection(key);
                for (Object attribute : c) {
                    if (attribute == null) {
                        continue;
                    }
                    attribute = StringUtils.capitalize((String) attribute);
                    s.println("svn move " + svnRootPath + name + "Tag" + "Attribute" + attribute + "Attribute"
                            + "Comments" + ".txt" + " " + svnRootPath + "JSP" + name + "Tag" + "Attribute" + attribute
                            + "Attribute" + "Comments" + ".txt" + " " + "-m \"Renaming generated documentation files\"");
                    s.println("svn move " + svnRootPath + name + "Tag" + "Attribute" + attribute + "Attribute"
                            + "Comments" + ".properties" + " " + svnRootPath + "JSP" + name + "Tag" + "Attribute"
                            + attribute + "Attribute" + "Comments" + ".properties" + " "
                            + "-m \"Renaming generated documentation files\"");
                    s.println("svn move " + svnRootPath + name + "Tag" + "Attribute" + attribute + "Attribute"
                            + "Description" + ".txt" + " " + svnRootPath + "JSP" + name + "Tag" + "Attribute"
                            + attribute + "Attribute" + "Description" + ".txt" + " "
                            + "-m \"Renaming generated documentation files\"");
                    s.println("svn move " + svnRootPath + name + "Tag" + "Attribute" + attribute + "Attribute"
                            + "Description" + ".properties" + " " + svnRootPath + "JSP" + name + "Tag" + "Attribute"
                            + attribute + "Attribute" + "Description" + ".properties" + " "
                            + "-m \"Renaming generated documentation files\"");
                }
            }

            s.flush();
            s.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Element> getAttributeList(Element element) {
        return element.elements("attribute");
    }

    @SuppressWarnings("unchecked")
    private List<Element> getAllElements(Element root) {
        return root.elements();
    }

    private void generateIndexFile(List<String> tagNames, List<String> functionNames) {
        // generate index page for all tags
        File taglibIndexFile = new File(this.outputDir.getAbsolutePath() + File.separator + "TagIndex.txt");

        try {
            if (!taglibIndexFile.exists()) {
                taglibIndexFile.createNewFile();
            }

            PrintStream s = new PrintStream(new FileOutputStream(taglibIndexFile));
            s.println("!!!makumba tag library");
            s.println();

            for (String el : tagNames) {
                String tagName = processedElements.get(el).elementText(getTagNameSyntax());
                s.println("* [mak:" + tagName + "|" + getWikiTagName(tagName, true) + "]");
            }

            for (String f : functionNames) {
                s.println("* [mak:" + f + "()|" + getWikiTagName(f, false) + "]");
            }

            s.println();
            s.println("%%(display:none;) [Category Documentation]%%");
            s.close();

        } catch (IOException io) {
            throw new RuntimeException("Cannot create index file", io);
        }
    }

    /**
     * Generates one tag file
     * 
     * @param element
     *            the XML element representing this tag or function
     * @param isTag
     *            whether this is a tag or a function
     * @throws IOException
     *             if the generated file can't be created
     */
    private void generateElementFile(Element element, boolean isTag) throws IOException {

        String elementName = element.elementText(getElementNameSyntax(isTag));
        String generatedFileName = getWikiTagName(elementName, isTag);

        String path = this.outputDir.getAbsoluteFile() + File.separator + generatedFileName + ".txt";
        File generatedFile = new File(path);
        if (!generatedFile.exists()) {
            generatedFile = new File(generatedFile.getAbsolutePath());
            generatedFile.createNewFile();
        }

        // create properties file so JSPWiki is not confused about the author
        File props = new File(this.outputDir.getAbsoluteFile() + File.separator + generatedFileName + ".properties");
        if (!props.exists()) {
            props = new File(props.getAbsolutePath());
            props.createNewFile();
        }
        PrintWriter propWriter = new PrintWriter(new FileWriter(props, false));
        propWriter.append("#JSPWiki page properties for page " + generatedFileName + "\n");
        propWriter.append("#" + new Date().toString() + "\n");
        propWriter.append("author=TaglibReferenceGenerator");
        propWriter.flush();
        propWriter.close();

        // wtf?
        if (System.getProperty("os.name").startsWith("Mac OS X")) {
            System.setProperty("line.separator", "\r\n");
        }

        FileOutputStream fos = new FileOutputStream(generatedFile);
        BufferedWriter s = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));

        // generate the generated page warning
        s.newLine();
        s.append("%%(display:none) makumba hackers: this page is generated, don't edit it! Instead edit the text in the included pages. %%");
        s.newLine();
        s.newLine();

        // generate the header
        if (isTag) {
            s.append("!!!Taglib documentation for tag mak:" + elementName);
            s.newLine();
        } else {
            s.append("!!!Taglib documentation for EL function mak:" + elementName);
            s.newLine();

        }

        s.newLine();

        // generate the description
        generateDescription(element, s);
        s.newLine();

        // then the attributes
        generateAttributes(element, s, elementName, isTag);
        s.newLine();

        // then the "see also" section
        generateSeeAlso(element, s, isTag);
        s.newLine();

        // then the examples
        generateExamples(element, s, isTag);

        // finally append the category
        s.newLine();
        s.append("%%(display:none;) [Category Documentation]%%");
        s.flush();
        s.close();
        fos.close();

    }

    private String getWikiTagName(String elementName, boolean isTag) {
        return elementName.substring(0, 1).toUpperCase() + elementName.substring(1, elementName.length())
                + (isTag ? "Tag" : "Function");
    }

    private void generateExamples(Element element, BufferedWriter s, boolean isTag) throws IOException {
        s.append("!!Examples");
        s.newLine();
        s.newLine();

        Node examples = checkNodeExists(element, "example", isTag);
        StringTokenizer tk = new StringTokenizer(examples.getText(), ",");

        if (tk.countTokens() == 0) {
            s.append("%%(color:red) No example page provided for this tag!!%%");
            s.newLine();
        }

        while (tk.hasMoreElements()) {
            String e = tk.nextToken();
            s.append(getPageInsert(e));
            s.append("[.|edit:" + e + "]");
            s.newLine();
            s.newLine();
        }
    }

    private void generateSeeAlso(Element element, BufferedWriter s, boolean isTag) throws IOException {
        Node seeAlso = checkNodeExists(element, "see", isTag);
        StringTokenizer tk = new StringTokenizer(seeAlso.getText(), ",");
        if (tk.countTokens() > 0) {
            s.append("!!See also");
            s.newLine();
            s.newLine();
        }
        while (tk.hasMoreElements()) {

            String reference = tk.nextToken().trim();

            Object referredElement = element.getDocument().getRootElement().selectObject(
                "//taglib//" + (isTag ? "tag" : "function") + "['@" + getElementNameSyntax(isTag) + "=" + reference
                        + "']");
            if (referredElement == null) {
                throw new RuntimeException("Error: see also reference " + reference + " in element definition "
                        + element.elementText(getElementNameSyntax(isTag)) + " does not exist.");
            }

            String referenceWikiName = reference.substring(0, 1).toUpperCase()
                    + reference.substring(1, reference.length()) + (isTag ? "Tag" : "Function");

            s.append("[mak:" + reference + "|" + referenceWikiName + "]");
            if (tk.hasMoreTokens()) {
                s.append(", ");
            }
        }
        if (tk.countTokens() > 0) {
            s.newLine();
        }
    }

    private void generateAttributes(Element element, BufferedWriter s, String elementName, boolean isTag)
            throws IOException {
        s.append("!!Attributes");
        s.newLine();

        List<Element> attributes = getAttributeList(element);
        if (attributes.size() == 0) {
            s.append("This tag has no attributes");
            s.newLine();
        } else {
            // generate the table header
            s.append("[{Table");
            s.newLine();
            s.newLine(); // empty line necessary or the plugin doesn't work
            s.append("||Name||Required||Runtime expression||Description||Comments ");
            s.newLine();
            s.newLine();

            // in order not to generate the same content for generic attributes (HTML, form-specific, ...)
            // we keep the first attribute of the kind and only write out all the rows if another kind of attribute is
            // met
            GenericAttributeTuple genericAttributeTuple = new GenericAttributeTuple();
            for (Element a : attributes) {
                if (isTag) {
                    tags.put(elementName, a.elementText("name"));
                } else {
                    functions.put(elementName, a.elementText("name"));
                }
                generateAttributeRow(a, s, genericAttributeTuple, elementName + (isTag ? "Tag" : "Function"));
            }

            // in the end we flush the tuple
            if (genericAttributeTuple.getGenericAttributeName() != null) {
                genericAttributeTuple.print(s);
                genericAttributeTuple.reset();
            }

            s.newLine(); // empty line necessary or the plugin chomps one character of each cell
            s.append("}]");
            s.newLine();
        }
    }

    private void generateAttributeRow(Element attribute, BufferedWriter s, GenericAttributeTuple genericAttributeTuple,
            String typedElementName) throws IOException {

        String name = attribute.elementText("name");
        String required = attribute.elementText("required");
        String runtimeExpr = attribute.elementText("rtexprvalue");

        // was this one included?
        String inheritedFrom = attribute.attributeValue("inheritedFrom");

        String inherited = "";
        if (inheritedFrom != null) {
            inherited = "\\\\ %%(font-size:smaller;) (from [mak:" + inheritedFrom + "|"
                    + StringUtils.capitalize(inheritedFrom) + (typedElementName.endsWith("Tag") ? "Tag" : "Function")
                    + "])%%";
        }

        String attributePageSuffix = "Attribute" + StringUtils.capitalize(name) + "Attribute";
        String attributePageName = "";

        if (inheritedFrom != null) {
            attributePageName += StringUtils.capitalize(inheritedFrom)
                    + (typedElementName.endsWith("Tag") ? "Tag" : "Function") + attributePageSuffix;
        } else {
            attributePageName += StringUtils.capitalize(typedElementName) + attributePageSuffix;

        }

        String description = getPageInsert(attributePageName + "Description");
        String comments = getPageInsert(attributePageName + "Comments");

        File descriptionFile = new File(outputDir.getAbsolutePath() + File.separator + attributePageName
                + "Description.txt");
        String descriptionText;
        if (!descriptionFile.exists()) {
            descriptionText = "Document me please!";
            System.err.println("Description page " + attributePageName
                    + "Description not found, needs to be documented!");
        } else {
            descriptionText = MakumbaTLDGenerator.readFileAsString(descriptionFile.getAbsolutePath());
        }

        File commentsFile = new File(outputDir.getAbsolutePath() + File.separator + attributePageName + "Comments.txt");
        String commentsText;
        if (!commentsFile.exists()) {
            commentsText = "Document me please!";
            System.err.println("Comments page " + attributePageName + "Comments not found, needs to be documented!");
        } else {
            commentsText = MakumbaTLDGenerator.readFileAsString(commentsFile.getAbsolutePath());
        }

        String deprecated = attribute.elementText("deprecated");
        boolean isDeprecated = deprecated != null && deprecated.equals("true");
        String deprecatedStyle = isDeprecated ? "(deprecated) " : "";

        // check if this is a generic attribute
        boolean isGenericAttribute = false;

        for (String[] generic : genericAttributes) {

            // if this is a generic attribute
            if (descriptionText.startsWith(generic[0]) && commentsText.startsWith(generic[1])) {
                isGenericAttribute = true;

                if (genericAttributeTuple.getGenericAttributeName() == null) {
                    // this is the first occurrence of the generic attribute so we build it
                    genericAttributeTuple.setFirstGenericAttribute(name + inherited, required, runtimeExpr, description
                            + " (generic)", comments + " (generic)");
                    genericAttributeTuple.setGenericAttributeName(generic[0]);
                } else if (genericAttributeTuple.getGenericAttributeName().equals(generic[0])) {
                    genericAttributeTuple.addAttribute(name + inherited, required, runtimeExpr);
                } else {
                    // we have a different generic attribute, so we print the previous one, clear the tuple and set the
                    // new one
                    genericAttributeTuple.print(s);
                    genericAttributeTuple.reset();
                    genericAttributeTuple.setFirstGenericAttribute(name + inherited, required, runtimeExpr, description
                            + " (generic)", comments + " (generic)");
                }
            }
        }

        // if the attribute is no generic attribute process the generic attribute from before
        if (!isGenericAttribute && genericAttributeTuple.getGenericAttributeName() != null) {
            genericAttributeTuple.print(s);
            genericAttributeTuple.reset();
        } else if (!isGenericAttribute) {

            if (isDeprecated) {
                s.append("|" + deprecatedStyle + name + " (deprecated) " + inherited + " ");
            } else {
                s.append("|" + deprecatedStyle + name + inherited + " ");
            }
            s.newLine();
            s.append("|" + deprecatedStyle + required + " ");
            s.newLine();
            s.append("|" + deprecatedStyle + runtimeExpr + " ");
            s.newLine();
            s.append("|" + deprecatedStyle + description + "[.|edit:" + attributePageName + "Description]" + " ");
            s.newLine();
            s.append("|" + deprecatedStyle + comments + "[.|edit:" + attributePageName + "Comments]" + " ");
            s.newLine();
            s.newLine(); // empty line, row is over
        }

    }

    /**
     * Generates the description for this document. Checks if the <description> element in taglib-skeleton exists
     * 
     * @throws IOException
     */
    private void generateDescription(Element element, BufferedWriter s) throws IOException {
        s.append("!!Description");
        s.newLine();
        s.newLine();
        s.append(getPageInsert(element.elementText("descriptionPage")));
        s.append("[.|edit:" + element.elementText("descriptionPage") + "]");
        s.newLine();

    }

    private Node checkNodeExists(Element element, String nodeName, boolean isTag) {
        Node n = element.element(nodeName);
        if (n == null) {
            throw new RuntimeException("No <" + nodeName + "> element found in taglib-skeleton.tld for element "
                    + element.elementText(getElementNameSyntax(isTag)));
        }
        return n;
    }

    private String getPageInsert(String pageName) {
        return "[{InsertPage style='display: inline;' page=" + pageName + "}]";

    }

    protected String getTagNameSyntax() {
        return "name";
    }

    protected String getFunctionNameSyntax() {
        return "name";
    }

    protected String getElementNameSyntax(boolean isTag) {
        return isTag ? getTagNameSyntax() : getFunctionNameSyntax();
    }

    class GenericAttributeTuple {

        private String[] firstGenericAttribute;

        private String genericAttributeName;

        private List<String[]> attributes = new ArrayList<String[]>();

        public void addAttribute(String name, String required, String runtimeExpr) {
            String[] att = new String[3];
            att[0] = name;
            att[1] = required;
            att[2] = runtimeExpr;
            attributes.add(att);
        }

        public void print(BufferedWriter s) throws IOException {
            for (String str : firstGenericAttribute) {
                s.append("|" + str + " ");
                s.newLine();
            }

            s.newLine(); // first row is over, new line

            for (String[] att : attributes) {
                s.append("|" + att[0] + " ");
                s.newLine();
                s.append("|" + att[1] + " ");
                s.newLine();
                s.append("|" + att[2] + " ");
                s.newLine();
                s.append("|^ ");
                s.newLine();
                s.append("|^ ");
                s.newLine();

                s.newLine();
            }
        }

        public void reset() {
            firstGenericAttribute = null;
            genericAttributeName = null;
            attributes = new ArrayList<String[]>();
        }

        public void setFirstGenericAttribute(String name, String required, String runtimeExpr, String description,
                String comments) {
            this.firstGenericAttribute = new String[5];
            this.firstGenericAttribute[0] = name;
            this.firstGenericAttribute[1] = required;
            this.firstGenericAttribute[2] = runtimeExpr;
            this.firstGenericAttribute[3] = description;
            this.firstGenericAttribute[4] = comments;

        }

        public String getGenericAttributeName() {
            return genericAttributeName;
        }

        public void setGenericAttributeName(String genericAttributeName) {
            this.genericAttributeName = genericAttributeName;
        }
    }

}
