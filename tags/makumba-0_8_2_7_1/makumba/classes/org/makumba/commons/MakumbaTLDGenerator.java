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

package org.makumba.commons;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.makumba.MakumbaSystem;

/**
 * This class generates the Makumba TLD files based on the documented TLD XML file.
 * 
 * @author Manuel Gay
 * @version $Id$
 */
public class MakumbaTLDGenerator {

    private static final String TAGLIB_MAK = "taglib.tld";

    private static final String TAGLIB_HIBERNATE = "taglib-hibernate.tld";

    private static final String HIBERNATE_TLD_URI = "http://www.makumba.org/view-hql";

    private static final String TAGLIB_DOCUMENTED_XML = "taglib-documented.xml";

    public static void main(String[] args) {
        HashMap<String, Element> processedTags = new HashMap<String, Element>();

        // read the documented XML files
        SAXReader saxReader = new SAXReader();
        Document document = null;
        final String sourcePath = new File(args[0]) + File.separator + TAGLIB_DOCUMENTED_XML;
        try {
            document = saxReader.read(sourcePath);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        final String errorMsg = "Error processing '" + sourcePath + "': ";

        Element root = document.getRootElement();
        for (Element tag : (List<Element>) root.elements()) {
            if (tag.getName().equals("description")) {
                // update makumba version place-holder
                tag.setText(tag.getText().replace("@version@", MakumbaSystem.getVersion()));
            }
            if (tag.getName().equals("tag") || tag.getName().equals("function")) {

                Element name = tag.element("name");
                String tagName = name.getText();

                for (Element tagContent : (List<Element>) tag.elements()) {
                    
                    if (tagContent.getName().equals("attribute")) {
                        if (tagContent.attributeValue("name") != null
                                || tagContent.attributeValue("specifiedIn") != null) { // have a referring attribute
                            Element newTag = getReferencedAttributes(processedTags, errorMsg, tag, tagName, tagContent);
                            tagContent.setAttributes(newTag.attributes());
                            final List<Element> elements = newTag.elements();
                            for (Element element : elements) {
                                tagContent.add((Element) element.clone());
                            }
                        } else { // normal attribute
                            for (Element attributeContent : (List<Element>) tagContent.elements()) {
                                // remove all the <comments> tags inside <attribute> elements
                                if (StringUtils.equalsAny(attributeContent.getName(), "comments", "deprecated", "descriptionPage", "commentsPage")) {
                                    attributeContent.getParent().remove(attributeContent);
                                }
                            }
                        }
                    }

                    // remove the invalid tags
                    if (StringUtils.equalsAny(tagContent.getName(), "see", "example")) {
                        tagContent.getParent().remove(tagContent);
                    }
                    
                    // if we have a descriptionPage instead of a raw text, make the description tag, or the TLD is invalid
                    if(tagContent.getName().equals("descriptionPage")) {
                        Element e = tagContent.getParent().addElement("description");
                        e.setText("See " + tagContent.getText());
                        tagContent.getParent().remove(tagContent);
                    }
                }
                processedTags.put(tagName, tag);
            }
        }

        // generate the clean TLD
        String tldPath = args[0] + File.separator + TAGLIB_MAK;
        System.out.println("Writing general Makumba TLD at path " + tldPath);
        try {
            XMLWriter output = new XMLWriter(new FileWriter(new File(tldPath)), new OutputFormat("", false));
            output.write(document);
            output.close();
        } catch (IOException e1) {
            System.out.println(e1.getMessage());
        }

        // generate hibernate TLD

        Document hibernateTLD = document;
        hibernateTLD.getRootElement().element("uri").setText(HIBERNATE_TLD_URI);

        String hibernateTldPath = args[0] + File.separator + TAGLIB_HIBERNATE;
        System.out.println("Writing hibernate Makumba TLD at path " + hibernateTldPath);
        try {
            XMLWriter output = new XMLWriter(new FileWriter(new File(hibernateTldPath)), new OutputFormat("", false));
            output.write(document);
            output.close();
        } catch (IOException e1) {
            System.out.println(e1.getMessage());
        }

        // here we could now also generate the list-hql, forms-hql, ... TLDs
        // this would be easily done by defining an array of tags that should be in each of them, iterate over all the
        // tags in the doc
        // and take only the right ones
        // and then write this with the right URL

    }

    public static Element getReferencedAttributes(HashMap<String, Element> processedTags, final String errorMsg,
            Element tag, String tagName, Element tagContent) {
        String specifiedIn = tagContent.attributeValue("specifiedIn");
        String attribute = tagContent.attributeValue("name");
        Element copyFrom = processedTags.get(specifiedIn);
        if (copyFrom == null) {
            System.out.println(errorMsg + "tag '" + tagName
                    + "' specifies an invalid source tag to copy attributes from: '" + specifiedIn
                    + "'. Make sure that the source tag exists and is defined BEFORE the tag copying!");
            System.exit(-1);
        }
        Element element = getAttributeFromTag(copyFrom, attribute);
        if (element == null) {
            System.out.println(errorMsg + "tag '" + tagName + "' specifies an not-existing source attribute '"
                    + attribute + "' to copy from tag '" + specifiedIn + "'!");
            System.exit(-1);
        }
        return element;
    }

    private static Element getAttributeFromTag(Element tag, String attribute) {
        for (Element tagContent : (List<Element>) tag.elements()) {
            if (tagContent.getName().equals("attribute")) {
                if (tagContent.element("name") != null && tagContent.element("name").getText().equals(attribute)) {
                    return (Element) tagContent.clone();
                }
            }
        }
        return null;
    }

}