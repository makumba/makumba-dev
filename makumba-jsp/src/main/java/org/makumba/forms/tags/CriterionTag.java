package org.makumba.forms.tags;

import java.io.IOException;
import java.util.Hashtable;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.StringUtils;
import org.makumba.FieldDefinition;
import org.makumba.LogicException;
import org.makumba.ProgrammerError;
import org.makumba.analyser.PageCache;
import org.makumba.commons.MultipleKey;
import org.makumba.commons.tags.FormDataProvider;
import org.makumba.commons.tags.GenericMakumbaTag;

/**
 * @author Rudolf Mayer
 * @version $Id: CriterionTag.java,v 1.1 Oct 21, 2007 1:37:37 PM rudi Exp $
 */
public class CriterionTag extends GenericMakumbaTag implements BodyTag {
    private static final long serialVersionUID = 1L;

    private static final String[] allowedRanges = ATTRIBUTE_VALUES_TRUE_FALSE;

    private String isRange;

    private String fields;

    private FieldDefinition fieldDef;

    private BodyContent bodyContent;

    private FormDataProvider fdp;

    private String matchMode;

    private boolean hasMatchModeTag = false;

    public CriterionTag() {
        // TODO move this somewhere else
        try {
            this.fdp = (FormDataProvider) Class.forName("org.makumba.list.tags.ListFormDataProvider").newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public SearchTag getForm() {
        return (SearchTag) TagSupport.findAncestorWithClass(this, FormTagBase.class);
    }

    /**
     * Determines the ValueComputer and associates it with the tagKey
     * 
     * @param pageCache
     *            the page cache of the current page
     */
    @Override
    public void doStartAnalyze(PageCache pageCache) {
        if (fields == null) {
            throw new ProgrammerError("fields attribute is required");
        }
        parseFieldList(pageCache);
        // FIXME: we can't have the same field as the primary criterion yet => it would lead to two inputs with
        // the same name.
        // The fix should be to give a different name to the input, and store a mapping input-name => field
        // name, so we can construct the query correctly.
        // For now, we just prevent errors by checking that this field was not yet used in this search form
        if (getForm().containsInput(getInputName())) {
            throw new ProgrammerError(
                    "This search form already contains one critireon tag with the same field as the first criterion. For now, this is not supported in Makumba!");
        }
        super.doStartAnalyze(pageCache);
    }

    private void parseFieldList(PageCache pageCache) throws ProgrammerError {
        String[] fieldsSplit = getFieldsSplit();
        for (String element : fieldsSplit) {
            FieldDefinition fd = getForm().fdp.getInputTypeAtAnalysis(this, getForm().getDataTypeAtAnalysis(pageCache),
                element, pageCache);

            // if the fd is not found, the field is not known
            if (fd == null) {
                throw new ProgrammerError("Field '" + element + "' in field list '" + fields + "' is not known.");
            }
            // compare it to the already checked one if the types are equivalent
            // FIXME: this should be more relaxed, ideally we allow searching on all types
            if (fieldDef != null) {
                if (fieldDef.getIntegerType() != fd.getIntegerType()) {
                    // as a quick-fix, we allow using char & text equally
                    if (!(fieldDef.isStringType() && fd.isStringType())) {
                        throw new ProgrammerError("All fields in the field list must be of the same type! Field '"
                                + element + "' with type '" + fd.getType() + "' differs from the previous field '"
                                + fieldDef + "' of type '" + fieldDef.getType() + "'!");
                    }
                }
            } else {
                fieldDef = fd;
            }

            // multi-field search is only possible for string, number and date types, not for sets, pointers & enums
            if (fieldsSplit.length > 1 && !(fd.isStringType() || fd.isNumberType() || fd.isDateType())) {
                throw new ProgrammerError(
                        "Multi-field search is only possible for 'char'/'text', 'int'/'real' and 'date' types, given field '"
                                + element + "' is of type '" + fd.getType() + "'!");
            }

        }
    }

    @Override
    public int doAnalyzedEndTag(PageCache pageCache) throws JspException, LogicException {
        getForm().responder.addMultiFieldSearchMapping(getInputName(), getFieldsSplit());

        // if matchMode is null, it was not set in the criterionTag
        // thus if we have no matchMode tag for this criterionTag
        // get a default mode depending on the type, and whether we do a range comparison
        if (matchMode == null && !hasMatchModeTag) {
            if (isRange()) { // for range, we always use between-inclusive
                matchMode = SearchTag.MATCH_BETWEEN_INCLUSIVE;
            } else { // for others, use equals as the most logical one
                matchMode = SearchTag.MATCH_EQUALS;
            }
            // need to register it again in the responser
            getForm().responder.setDefaultMatchMode(getInputName(), matchMode);
        }

        if (bodyContent != null) {
            try {
                bodyContent.getEnclosingWriter().print(bodyContent.getString());
            } catch (IOException e) {
                throw new JspException(e.toString());
            }
        }
        return super.doAnalyzedEndTag(pageCache);
    }

    /**
     * This always returns EVAL_BODY_TAG so we make sure {@link #doInitBody()} is called
     * 
     * @param pageCache
     *            the page cache of the current page
     */
    @Override
    public int doAnalyzedStartTag(PageCache pageCache) {
        if (fieldDef == null) {
            parseFieldList(pageCache);
            if (matchMode != null) {
                // check for valid match-mode
                Hashtable<String, String> matchModes = MatchModeTag.getValidMatchmodes(false,
                    getFieldDefinition(pageCache));
                if (!matchModes.containsKey(matchMode)) {
                    throw new ProgrammerError("Unknown match mode '" + matchMode + "'. Valid options are: "
                            + org.makumba.commons.StringUtils.toString(matchModes.keySet()));
                }

                getForm().responder.setDefaultMatchMode(getInputName(), matchMode);
            }
        }
        return EVAL_BODY_BUFFERED;
    }

    public void setHasMatchMode(boolean hasMatchMode) {
        this.hasMatchModeTag = hasMatchMode;
    }

    @Override
    public void doEndAnalyze(PageCache pageCache) {
        super.doEndAnalyze(pageCache);
    }

    public void setFields(String s) {
        this.fields = s;
    }

    public String[] getFieldsSplit() {
        String[] split = fields.split(",");
        for (int i = 0; i < split.length; i++) {
            split[i] = split[i].trim();
        }
        return split;
    }

    @Override
    public void doInitBody() throws JspException {
    }

    @Override
    public void setBodyContent(BodyContent b) {
        bodyContent = b;
    }

    public FieldDefinition getTypeFromContext(PageCache pageCache) {
        if (fieldDef == null) {
            parseFieldList(pageCache);
        }
        return fdp.getInputTypeAtAnalysis(this, getForm().getDataTypeAtAnalysis(pageCache), getInputName(), pageCache);
    }

    @Override
    public void setTagKey(PageCache pageCache) {
        tagKey = new MultipleKey(new Object[] { getForm().tagKey, id, fields, matchMode });
    }

    @Override
    public boolean allowsIdenticalKey() {
        return false;
    }

    public String getInputName() {
        if (fields.indexOf(",") == -1) {
            return fields.trim();
        } else {
            return fields.substring(0, fields.indexOf(",")).trim();
        }

    }

    public FieldDefinition getFieldDefinition(PageCache pageCache) {
        parseFieldList(pageCache);
        return fieldDef;
    }

    public boolean isRange() {
        return StringUtils.equals(isRange, "true");
    }

    public void setIsRange(String isRange) {
        this.isRange = isRange;
    }

    public void setMatchMode(String matchMode) {
        this.matchMode = matchMode;
    }

    @Override
    protected void registerPossibleAttributeValues() {
        registerAttributeValues("isRange", allowedRanges);
    }

    @Override
    protected void doAnalyzedCleanup() {
        super.doAnalyzedCleanup();
        bodyContent = null;
        fieldDef = null;
        matchMode = null;
        hasMatchModeTag = false;
    }

    public String getMatchMode() {
        return matchMode;
    }
}
