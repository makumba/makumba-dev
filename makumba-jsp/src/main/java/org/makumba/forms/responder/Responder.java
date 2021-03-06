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

package org.makumba.forms.responder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.makumba.CompositeValidationException;
import org.makumba.DataDefinition;
import org.makumba.InvalidValueException;
import org.makumba.LogicException;
import org.makumba.MakumbaError;
import org.makumba.Pointer;
import org.makumba.commons.attributes.RequestAttributes;
import org.makumba.controller.http.ControllerFilter;

/**
 * A responder is created for each form and stored internally, to respond when the form is submitted. To reduce memory
 * space, identical responders are stored only once
 */
public abstract class Responder implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * the name of the CGI parameter that passes the responder key, so the responder can be retrieved from the cache,
     * "__makumba__responder__"
     */
    public final static String responderName = "__makumba__responder__";

    /**
     * prevents multiple submission of the same form (bug #190), computes as responder+sessionID,
     * "__makumba__formSession__"
     */
    protected final static String formSessionName = "__makumba__formSession__";

    protected static final String FORM_RESULTS = "org.makumba.formResults";

    /** the default label used to store the add and new result, "___mak___edited___" */
    private static final String anonymousResult = "___mak___edited___";

    /** the default response message, "changes done" */
    private static final String defaultMessage = "changes done";

    /** the default response message for search forms, "Search done!" */
    public static final String defaultMessageSearchForm = "Search done!";

    /** the name of the CGI parameter that passes the base pointer, see {@link #basePointerType}, "__makumba__base__" */
    public final static String basePointerName = "__makumba__base__";

    protected transient ResponderFactory factory;

    private static final Logger logger = Logger.getLogger("org.makumba.controller");

    /** the responder key, as computed from the other fields */
    protected int identity;

    /** the controller object, on which the handler method that performs the operation is invoked */
    protected transient Object controller;

    /** store the name of the controller class */
    protected String controllerClassname;

    /** the database in which the operation takes place */
    protected String database;

    /** a response message to be shown in the response page */
    protected String message = defaultMessage;

    /** a response message to be shown when multiple submit occur */
    protected String multipleSubmitErrorMsg;

    /** Stores whether we shall do client-side validation, i.e. with javascript for a {@link FormResponder} */
    protected String clientSideValidation;

    /**
     * Stores whether we shall reload this form on a validation error or not. Used by {@link ControllerFilter} to decide
     * on the action.
     */
    protected boolean reloadFormOnError;

    protected String originatingPageName;

    /**
     * Stores whether the form shall be annotated with the validation errors, or not. Used by {@link ControllerFilter}
     * to decide if the error messages shall be shown in the form response or not.
     */
    private boolean showFormAnnotated;

    /** new and add responders set their result to a result attribute */
    protected String resultAttribute = anonymousResult;

    /** the name of the resultLabel used in the resultList of a search form */
    protected String resultLabel;

    /** HTML ID of the form **/
    private String formId;

    /** the business logic handler, for all types of forms */
    protected String handler;

    /** the business logic after handler, for all types of forms */
    protected String afterHandler;

    /**
     * edit, add and delete makumba operations have a special pointer called the base pointer
     */
    protected String basePointerType;

    /** the type where the search operation is made */
    protected String searchType;

    /** the name of the form we operate on (only needed for search forms). */
    protected String formName;

    /** the type where the new operation is made */
    protected String newType;

    /** the field on which the add operation is made */
    protected String addField;

    /** the operation name: add, edit, delete, new, simple */
    protected String operation;

    /** the event to be fired when the responder responds **/
    protected String triggerEvent;

    /** the operation handler, computed from the operation */
    protected ResponderOperation op;

    /** order of the forms in the page * */
    protected ArrayList<String> formOrder;

    protected String recordChangesIn;

    protected HashMap<String, String> defaultMatchModes = new HashMap<String, String>();

    public String getDefaultMatchMode(String inputName) {
        return defaultMatchModes.get(inputName);
    }

    public void setDefaultMatchMode(String inputName, String matchMode) {
        defaultMatchModes.put(inputName, matchMode);
    }

    public String getHandler() {
        return handler;
    }

    public String getAfterHandler() {
        return afterHandler;
    }

    public String getAddField() {
        return addField;
    }

    public String getBasePointerType() {
        return basePointerType;
    }

    public Object getController() {
        return controller;
    }

    public String getDatabase() {
        return database;
    }

    public String getNewType() {
        return newType;
    }

    public String getSearchType() {
        return searchType;
    }

    public String getResultLabel() {
        return resultLabel;
    }

    public String getFormName() {
        return formName;
    }

    public ArrayList<String> getFormOrder() {
        return formOrder;
    }

    // --------------- form time, responder preparation -------------------
    /** pass the http request, so the responder computes its default controller and database */
    public void setHttpRequest(HttpServletRequest req) throws LogicException {
        controller = RequestAttributes.getAttributes(req).getRequestController();
        database = RequestAttributes.getAttributes(req).getRequestDatabase();
    }

    /** pass the operation * */
    public void setOperation(String operation, ResponderOperation op) {
        this.operation = operation;
        this.op = op;
    }

    /** pass the form response message */
    public void setMessage(String message) {
        this.message = message;
    }

    /** pass the event to be fired after form submission */
    public void setTriggerEvent(String e) {
        this.triggerEvent = e;
    }

    /** pass the multiple submit response message */
    public void setMultipleSubmitErrorMsg(String multipleSubmitErrorMsg) {
        this.multipleSubmitErrorMsg = multipleSubmitErrorMsg;
    }

    public void setReloadFormOnError(boolean reloadFormOnError) {
        this.reloadFormOnError = reloadFormOnError;
    }

    public boolean getReloadFormOnError() {
        return reloadFormOnError;
    }

    public void setOriginatingPageName(String originatingPageName) {
        this.originatingPageName = originatingPageName;
    }

    public String getOriginatingPageName() {
        return originatingPageName;
    }

    public void setShowFormAnnotated(boolean showFormAnnotated) {
        this.showFormAnnotated = showFormAnnotated;
    }

    public boolean getShowFormAnnotated() {
        return showFormAnnotated;
    }

    public void setRecordChangesIn(String recordChangesIn) {
        this.recordChangesIn = recordChangesIn;
    }

    public String getRecordChangesIn() {
        return recordChangesIn;
    }

    public String getTriggerEvent() {
        return triggerEvent;
    }

    public void setClientSideValidation(String clientSideValidation) {
        this.clientSideValidation = clientSideValidation;
    }

    /** pass the response handler, if other than the default one */
    public void setHandler(String handler) {
        this.handler = handler;
    }

    /** pass the response afterHandler, if other than the default one */
    public void setAfterHandler(String afterHandler) {
        this.afterHandler = afterHandler;
    }

    /** pass the base pointer type, needed for the response */
    public void setBasePointerType(String basePointerType) {
        this.basePointerType = basePointerType;
    }

    /** pass the name of the result attribute */
    public void setResultAttribute(String resultAttribute) {
        this.resultAttribute = resultAttribute;
    }

    /** set the name of the resultLabel used in the resultList of a search form */
    public void setResultLabel(String resultLabel) {
        this.resultLabel = resultLabel;
    }

    /** passes the HTML ID of the form, either generated or provided by the user, enriched with form iteration **/
    public void setFormId(String id) {
        this.formId = id;
    }

    /** pass the field to which the add operation is made */
    public void setAddField(String s) {
        addField = s;
    }

    /** pass the type on which the new operation is made */
    public void setNewType(DataDefinition dd) {
        newType = dd.getName();
    }

    /** pass the type on which the new operation is made */
    public void setSearchType(DataDefinition dd) {
        searchType = dd.getName();
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public void setResponderOrder(ArrayList<String> formOrder) {
        this.formOrder = formOrder;
    }

    abstract protected void postDeserializaton();

    /** a key that should identify this responder among all */
    public String responderKey() {
        // we strip the makumba responder from the originating page, to not modify the responder value recursively...
        String originatingPageNameWithoutResponder = originatingPageName;
        try {
            if (originatingPageName.contains(responderName)) {
                int beginIndex = originatingPageName.indexOf(responderName);
                if (originatingPageName.indexOf("&", beginIndex) > 0) {
                    originatingPageNameWithoutResponder = originatingPageName.substring(0, beginIndex - 1)
                            + originatingPageName.substring(originatingPageName.indexOf("&", beginIndex));
                } else {
                    originatingPageNameWithoutResponder = originatingPageName.substring(0, beginIndex - 1);
                }
            }
        } catch (Exception e) {
            logger.warning("Couldn't remove '" + responderName + "' part from the originating URL '"
                    + originatingPageName + "': " + e.getMessage());
        }
        // hack for the makumba tests to work
        // since there are rarely more than one db in the webapp, this doesn't really matter
        String db = database;
        if (database.equals("testDatabaseHibernate")) {
            db = "testDatabase";
        }

        return formId + basePointerType + message + multipleSubmitErrorMsg + resultAttribute + db + operation
                + controller.getClass().getName() + handler + addField + newType + reloadFormOnError
                + originatingPageNameWithoutResponder + showFormAnnotated + clientSideValidation + defaultMatchModes
                + resultLabel + triggerEvent + (StringUtils.isNotBlank(recordChangesIn) ? recordChangesIn : "");
    }

    /** get the integer key of this form, and register it if not already registered */
    public int getPrototype() {
        // at this point we should have all data set, so we should be able to verify the responder
        String s = op.verify(this);
        if (s != null) {
            throw new MakumbaError("Bad responder configuration " + s);
        }
        return factory.getResponderIdentity(this);
    }

    // ------------------ multiple form section -------------------
    /** the form counter, 0 for the root form, one increment for each subform of this form */
    // NOTE: transient data here is not used during response, only at form building time
    transient int groupCounter = 0;

    /** the form suffix, "" for the root form, _+increment for subforms */
    protected transient String storedSuffix = "";

    protected transient String storedParentSuffix = "";

    /**
     * Used in search forms to store a mapping of a input name to the list of (one or more) fields in the mdd the input
     * should be matched against.
     */
    private Hashtable<String, String[]> multiFieldSearchMapping = new Hashtable<String, String[]>();

    protected static final char suffixSeparator = '_';

    /** pass the parent responder */
    public void setParentResponder(Responder resp, Responder root) {
        storedSuffix = "" + suffixSeparator + ++root.groupCounter;
        storedParentSuffix = resp.storedSuffix;
    }

    public String getSuffix() {
        return storedSuffix;
    }

    // ----------------- response section ------------------

    /** formats an error message */
    public static String errorMessage(Throwable t) {
        return t.getMessage();
    }

    /** formats an error message */
    public static String errorMessageFormatter(String message) {
        return "<span class=\"makumbaResponder makumbaError\">" + message + "</span>";
    }

    /** formats a successful message */
    public static String successFulMessageFormatter(String message) {
        return "<span class=\"makumbaResponder makumbaSuccess\">" + message + "</span>";
    }

    /** reads the HTTP base pointer */
    public Pointer getHttpBasePointer(HttpServletRequest req, String suffix) {
        // for add forms, the result of the enclosing new form may be used
        String basePointer = (String) RequestAttributes.getParameters(req).getParameter(basePointerName + suffix);
        if (basePointer.startsWith("valueOf_")) {
            // a base pointer that was not determined on form creation time
            // it contains the name of the attribute to get the value from -> evaluate it now
            String attributeName = basePointer.substring("valueOf_".length());
            return new Pointer(basePointerType, (String) RequestAttributes.getParameters(req).getParameter(
                attributeName));
        }
        return new Pointer(basePointerType, basePointer);
    }

    /**
     * Reads the data needed for the logic operation, from the http request. {@link FormResponder} provides an
     * implementation
     * 
     * @param req
     *            the request corresponding to the current page
     * @param suffix
     *            the responder / form suffix
     * @return a Dictionary holding the data read by the logic
     */
    public abstract Dictionary<String, Object> getHttpData(HttpServletRequest req, String suffix);

    public abstract ArrayList<InvalidValueException> getUnassignedExceptions(CompositeValidationException e,
            ArrayList<InvalidValueException> unassignedExceptions, String suffix);

    public ResponderFactory getFactory() {
        return factory;
    }

    public void setFactory(ResponderFactory factory) {
        this.factory = factory;
    }

    public void addMultiFieldSearchMapping(String name, String[] allFieldNames) {
        multiFieldSearchMapping.put(name, allFieldNames);
    }

    public String[] getMultiFieldSearchCriterion(String name) {
        return multiFieldSearchMapping.get(name);
    }

    /**
     * Save this responder instance as a binary object to the disc; persist it so it can be read from the disc into the
     * cache on a server reload / restart.
     */
    public void saveResponderToDisc() {
        String fileName = ResponderCacheManager.validResponderFilename(identity);
        File file = new File(fileName);
        try {
            controllerClassname = controller.getClass().getName();
            ObjectOutputStream objectOut = new ObjectOutputStream(new FileOutputStream(file));
            objectOut.writeObject(this); // we write the responder to disk
            objectOut.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while writing responder to HDD: '" + e.getMessage() + "', deleting file "
                    + fileName, e);
            e.printStackTrace();
            file.delete();
        }
    }

    public String getFormId() {
        return formId;
    }
}