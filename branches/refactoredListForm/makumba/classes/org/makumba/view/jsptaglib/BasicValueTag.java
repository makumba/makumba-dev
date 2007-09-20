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
//  $Id: BasicValueTag.java 1529 2007-09-13 23:33:10Z rosso_nero $
//  $Name$
/////////////////////////////////////

package org.makumba.view.jsptaglib;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.makumba.FieldDefinition;
import org.makumba.LogicException;
import org.makumba.MakumbaSystem;
import org.makumba.ProgrammerError;
import org.makumba.analyser.PageCache;
import org.makumba.commons.PageAttributes;
import org.makumba.controller.html.dateEditor;
import org.makumba.controller.http.ControllerFilter;
import org.makumba.controller.http.RequestAttributes;
import org.makumba.list.ListFormDataProvider;
import org.makumba.list.tags.MakumbaTag;
import org.makumba.util.MultipleKey;
import org.makumba.util.StringUtils;

/**
 * This is a a base class for InputTag and OptionTag but may be used for other tags that need to compute a value in
 * similar manner (value="$attribute" or value="OQL expr").
 * 
 * @author Cristian Bogdan
 * @version $Id: BasicValueTag.java 1529 2007-09-13 23:33:10Z rosso_nero $
 */
public abstract class BasicValueTag extends FormTag {
    
    protected static final String INPUT_TYPES = "org.makumba.inputtypes";

    String valueExprOriginal = null;

    /* Cannot be set here, subclasses who need it will set it */
    String dataType = null;

    public String expr = null;
    
    protected ListFormDataProvider fdp = new ListFormDataProvider();

    public void setValue(String value) {
        this.valueExprOriginal = value.trim();
    }

    public FormTagBase getForm() {
        return (FormTagBase) TagSupport.findAncestorWithClass(this, FormTagBase.class);
    }

    /**
     * Indicates if the expression is null
     * @return <code>true</code> if the expression is null, <code>false</code> otherwise
     */
    public boolean isNull() {
        return expr == null || expr.trim().length() == 0 || expr.trim().equals("nil");
    }

    /**
     * Indicates if the expression is a value
     * @return <code>true</code> if the expression doesn't start with '$' and is not null, <code>false</code> otherwise
     */
    boolean isValue() {
        return expr != null && !expr.startsWith("$") && !isNull();
    }

    /**
     * Indicates if the expression is an attribute
     * @return <code>true</code> if the expression starts with '$', <code>false</code> otherwise
     */
    boolean isAttribute() {
        return expr != null && expr.startsWith("$");
    }

    /** 
     * Determines the ValueComputer and associates it with the tagKey
     * @param pageCache the page cache of the current page
     */
    public void doStartAnalyze(PageCache pageCache) {
        if (isValue()) {
            fdp.onBasicValueStartAnalyze(this, pageCache, expr);
        }
    }

    public String checkPtrExpr(String expr2, PageCache pageCache) {
        return expr2;
    }

    abstract FieldDefinition getTypeFromContext(PageCache pageCache);

    /** 
     * Tells the ValueComputer to finish analysis, and sets the types for var and printVar.
     * @param pageCache the page cache of the current page
     */
    public void doEndAnalyze(PageCache pageCache) {
        FieldDefinition contextType = getTypeFromContext(pageCache);

        FieldDefinition dataTypeInfo = null;
        FieldDefinition type = null;

        if (dataType != null) {
            dataTypeInfo = MakumbaSystem.makeFieldDefinition("dummyName", dataType);
            if (contextType != null && !contextType.isAssignableFrom(dataTypeInfo))
                throw new ProgrammerError("declared data type '" + dataType
                        + "' not compatible with the type computed from context '" + contextType + "'");
        }

        // FIXME shouldn't this be in FormDataProvider - and should this class know about Values and Attributes?
        
        if (isValue()) {
            type = fdp.onBasicValueEndAnalyze(getTagKey(), pageCache);
        }
        if (isAttribute())
            type = (FieldDefinition) pageCache.retrieve(MakumbaTag.TYPES, expr.substring(1));

        String fieldName = "";
        if (this instanceof InputTag) {
            fieldName = "Field <" + ((InputTag) this).name + ">: ";
        }
        
        if (type != null && dataTypeInfo != null && !dataTypeInfo.isAssignableFrom(type))
            throw new ProgrammerError(
                    fieldName
                            + "computed type for INPUT is different from the indicated dataType. The dataType is indicated to '"
                            + dataType + "' type computed is '" + type + "'");

        if (type != null && contextType != null && !contextType.isAssignableFrom(type))
            throw new ProgrammerError(
                    fieldName
                            + "computed type is different from the type resulting from form analysis. The context type was determined to '"
                            + contextType + "', type computed is '" + type + "'");

        if (type == null && contextType == null && dataTypeInfo == null)
            throw new ProgrammerError(fieldName
                    + "cannot determine input type. Please specify the type using dataType=...");

        // we give priority to the type as computed from the form
        if (contextType == null)
            contextType = dataTypeInfo != null ? dataTypeInfo : type;

        pageCache.cache(INPUT_TYPES, tagKey, contextType);
    }

    public int doMakumbaEndTag(PageCache pageCache) throws JspException, LogicException {
        FieldDefinition type = (FieldDefinition) pageCache.retrieve(INPUT_TYPES, tagKey);
        Object val = null;
        
        // if we are reloading the form page on validation errors, fill form inputs as in the request
        if (this instanceof InputTag
                && StringUtils.equals(pageContext.getRequest().getAttribute(ControllerFilter.MAKUMBA_FORM_RELOAD), "true")) {
            String tagName = ((InputTag) this).name + getForm().responder.getSuffix();
            if (type.getIntegerType() == FieldDefinition._date) { 
                // we need a special treatment for date fields, as they do not come in a single input, but several ones
                val = dateEditor.readFrom(tagName, RequestAttributes.getParameters((HttpServletRequest) pageContext.getRequest()));
            } else { // other types can be handled normally
                val = pageContext.getRequest().getParameter(tagName);
            }
            return computedValue(val, type);
        }

        if (isValue())
            val = fdp.getValue(this, pageCache);
            
        if (isAttribute()) {
            val = PageAttributes.getAttributes(pageContext).getAttribute(expr.substring(1));
        }

        if (val != null)
            val = type.checkValue(val);

        return computedValue(val, type);
    }

    /** 
     * A value was computed, do what's needed with it, cleanup and return the result of doMakumbaEndTag()
     * @param o the value
     * @param type the type of the data
     */
    abstract int computedValue(Object o, FieldDefinition type) throws JspException, LogicException;
}
