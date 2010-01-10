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

package org.makumba.forms.tags;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;

import org.makumba.CompositeValidationException;
import org.makumba.FieldDefinition;
import org.makumba.InvalidValueException;
import org.makumba.LogicException;
import org.makumba.MakumbaSystem;
import org.makumba.ProgrammerError;
import org.makumba.analyser.PageCache;
import org.makumba.commons.MakumbaJspAnalyzer;
import org.makumba.commons.MultipleKey;
import org.makumba.commons.StringUtils;
import org.makumba.forms.responder.ResponseControllerHandler;
import org.makumba.providers.Configuration;

/**
 * mak:input tag
 * 
 * @author Cristian Bogdan
 * @author Rudolf Mayer
 * @version $Id$
 */
public class InputTag extends BasicValueTag implements javax.servlet.jsp.tagext.BodyTag {

    private static final long serialVersionUID = 1L;

    protected String name = null;

    protected String display = null;

    protected String nameVar = null;

    protected String calendarEditorLink = null;

    protected String calendarEditor = Configuration.getCalendarEditorDefault();

    protected String nullOption;

    /** input whith body, used only for choosers as yet * */
    BodyContent bodyContent = null;

    org.makumba.forms.html.ChoiceSet choiceSet;

    // unused for now, set when we know at analysis that this input has
    // a body and will generate a choser (because it has <mak:option > inside)
    boolean isChoser;

    public String toString() {
        return "INPUT name=" + name + " value=" + valueExprOriginal + " dataType=" + dataType + "\n";
    }

    public void setDataType(String dt) {
        this.dataType = dt.trim();
    }

    public void setField(String field) {
        setName(field);
    }

    public void setName(String field) {
        this.name = field.trim();
    }

    public void setDisplay(String d) {
        this.display = d;
    }

    public void setNameVar(String var) {
        this.nameVar = var;
    }

    public void setNullOption(String s) {
        this.nullOption = s;
    }

    // Extra html formatting parameters
    public void setAccessKey(String s) {
        extraFormattingParams.put("accessKey", s);
    }

    public void setDisabled(String s) {
        extraFormattingParams.put("disabled", s);
    }

    public void setOnChange(String s) {
        extraFormattingParams.put("onChange", s);
    }

    public void setOnBlur(String s) {
        extraFormattingParams.put("onBlur", s);
    }

    public void setOnFocus(String s) {
        extraFormattingParams.put("onFocus", s);
    }

    public void setOnSelect(String s) {
        extraFormattingParams.put("onSelect", s);
    }

    public void setTabIndex(String s) {
        extraFormattingParams.put("tabIndex", s);
    }

    /**
     * {@inheritDoc}
     */
    public void setTagKey(PageCache pageCache) {
        expr = valueExprOriginal;
        // FIXME: this fix is rather a quick fix, it does not provide any information about the location of the error
        // it may appear e.g. if you put a mak:input inside a mak:object, but not inside a form
        if (getForm() == null) {
            throw new ProgrammerError("input tag must be enclosed by a form tag");
        }
        if (expr == null)
            expr = getForm().getDefaultExpr(name);
        Object[] keyComponents = { name, getForm().tagKey };
        tagKey = new MultipleKey(keyComponents);
    }

    FieldDefinition getTypeFromContext(PageCache pageCache) {
        return fdp.getInputTypeAtAnalysis(this, getForm().getDataTypeAtAnalysis(pageCache), name, pageCache);
    }

    /**
     * Determines the ValueComputer and associates it with the tagKey
     * 
     * @param pageCache
     *            the page cache of the current page
     */
    public void doStartAnalyze(PageCache pageCache) {
        if (name == null)
            throw new ProgrammerError("name attribute is required");
        if (getForm() == null) {
            throw new ProgrammerError("input tag must be enclosed by a form tag!");
        }
        if (isValue())
            fdp.onNonQueryStartAnalyze(this, isNull(), getForm().getTagKey(), pageCache, expr);
    }

    /**
     * Tells the ValueComputer to finish analysis, and sets the types for var and printVar
     * 
     * @param pageCache
     *            the page cache of the current page
     */
    public void doEndAnalyze(PageCache pageCache) {
        if (nameVar != null)
            setType(pageCache, nameVar, ddp.makeFieldOfType(nameVar, "char"));

        FieldDefinition contextType = getTypeFromContext(pageCache);
        boolean dataTypeIsDate = dataType != null && ddp.makeFieldDefinition("dummyName", dataType) != null
                && ddp.makeFieldDefinition("dummyName", dataType).isDateType();
        boolean contextTypeIsDate = contextType != null && contextType.isDateType();
        if ((dataTypeIsDate || contextTypeIsDate) && calendarEditor != null && calendarEditor.equals("true")) {
            pageCache.cacheSetValues(NEEDED_RESOURCES,
                MakumbaSystem.getCalendarProvider().getNeededJavaScriptFileNames());
        }

        super.doEndAnalyze(pageCache);
    }

    /**
     * {@inheritDoc}
     */
    public void initialiseState() {
        super.initialiseState();

        // if type is "file", make the form multipart (should this be here or in doMakumbaStartTag() ?)
        if ("file".equals(params.get("type"))) {
            getForm().setMultipart();
        }
    }

    public void setBodyContent(BodyContent bc) {
        bodyContent = bc;
        // for now, only chosers can have body
        choiceSet = new org.makumba.forms.html.ChoiceSet();
    }

    public void doInitBody() {
    }

    public int doAnalyzedStartTag(PageCache pageCache) {
        // we do everything in doMakumbaEndTag, to give a chance to the body to set more attributes, etc
        return EVAL_BODY_BUFFERED;
    }

    void checkBodyContentForNonWhitespace() throws JspException {
        // if we find non-whitespace text between two options, we insert it in the choices, as "text" (no actual choice)
        if (bodyContent != null && bodyContent.getString().trim().length() > 0) {
            choiceSet.add(null, bodyContent.getString().trim(), false, false);
            try {
                bodyContent.clear();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    /**
     * A value was computed, do what's needed with it, cleanup and return the result of doMakumbaEndTag()
     * 
     * @param val
     *            the computed value
     * @param type
     *            the type of the computed value
     * @throws JspException
     * @throws {@link LogicException}
     */
    int computedValue(Object val, FieldDefinition type) throws JspException, LogicException {
        checkBodyContentForNonWhitespace();

        if (choiceSet != null)
            params.put(org.makumba.forms.html.ChoiceSet.PARAMNAME, choiceSet);

        if ("false".equals(display))
            params.put("org.makumba.noDisplay", "dummy");

        if (nullOption != null) {
            // nullOption is only applicable for charEnum and intEnum types
            FieldDefinition fd = getTypeFromContext(getPageCache(pageContext, MakumbaJspAnalyzer.getInstance()));
            if (!fd.isEnumType() && !fd.isPointer()) {
                throw new ProgrammerError(
                        "Attribute 'nullOption' is only applicable for 'charEnum', 'intEnum' and 'ptr' types, but input '"
                                + fd.getName() + "' is of type '" + fd.getType() + "'!");
            }
            params.put("nullOption", nullOption);
        }
        // appending the ID to the extra formatting params seems like a bit of a hack here.. but it also the fastest..
        // and we don't do it for dates, cause there we need to do it differently (a date is 3 inputs, need _0, _1 & _2)
        if (!type.isDateType()) {
            extraFormatting.append("id=\"").append(name).append("\" ");
        } else { // but for dates we add info about calendarEditor
            if(calendarEditor!=null)
                params.put("calendarEditor", calendarEditor);
            if (calendarEditorLink != null) {
                params.put("calendarEditorLink", calendarEditorLink);
            }
        }

        String formatted = getForm().responder.format(name, type, val, params, extraFormatting.toString());
        String fieldName = name + getForm().responder.getSuffix();

        if (nameVar != null) {
            getPageContext().setAttribute(nameVar, fieldName);
        }

        if (display == null || !display.equals("false")) {
            try {
                // check for a possible composite validation error set, and do form annotation if needed
                CompositeValidationException errors = (CompositeValidationException) pageContext.getRequest().getAttribute(
                    ResponseControllerHandler.MAKUMBA_FORM_VALIDATION_ERRORS);
                Collection<InvalidValueException> exceptions = null;
                if (errors != null) { // get the exceptions for this field
                    exceptions = errors.getExceptions(fieldName);
                }
                // if requested, do annotation before the field
                if (StringUtils.equalsAny(getForm().annotation, new String[] { "before", "both" }) && exceptions != null) {
                    for (Iterator<InvalidValueException> iter = exceptions.iterator(); iter.hasNext();) {
                        printAnnotation(fieldName, (InvalidValueException) iter.next());
                        if (getForm().annotationSeparator != null) {// print the separator, if existing
                            pageContext.getOut().print(getForm().annotationSeparator);
                        }
                    }
                }
                // print the actual form value
                pageContext.getOut().print(formatted);
                // if requested, do annotation after the field
                if (StringUtils.equalsAny(getForm().annotation, new String[] { "after", "both" }) && exceptions != null) {
                    for (Iterator<InvalidValueException> iter = exceptions.iterator(); iter.hasNext();) {
                        if (getForm().annotationSeparator != null) {// print the separator, if existing
                            pageContext.getOut().print(getForm().annotationSeparator);
                        }
                        printAnnotation(fieldName, (InvalidValueException) iter.next());
                    }
                }
            } catch (java.io.IOException e) {
                throw new JspException(e.toString());
            }
        }

        name = valueExprOriginal = dataType = display = expr = nameVar = null;
        return EVAL_PAGE;
    }

    private void printAnnotation(String fieldName, InvalidValueException e) throws IOException {
        pageContext.getOut().print("<span class=\"LV_validation_message LV_invalid\">");
        // pageContext.getOut().print("<span class=\"formAnnotation\">");
        pageContext.getOut().print(e.getShortMessage());
        pageContext.getOut().print("</span>");
    }

    public void setCalendarEditorLink(String calendarEditorLink) {
        this.calendarEditorLink = calendarEditorLink;
    }

    public void setCalendarEditor(String calendarEditor) {
        this.calendarEditor = calendarEditor;
    }
    
    @Override
    protected void doAnalyzedCleanup() {
        super.doAnalyzedCleanup();
        bodyContent=null;
        choiceSet=null;
        name= nameVar= nullOption= display= calendarEditor= calendarEditorLink= null;
    }

}