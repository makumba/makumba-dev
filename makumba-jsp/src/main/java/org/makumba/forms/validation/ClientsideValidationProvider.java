package org.makumba.forms.validation;

import org.makumba.FieldDefinition;
import org.makumba.analyser.MakumbaJspAnalyzer;
import org.makumba.forms.html.RecordEditor;
import org.makumba.forms.responder.FormResponder;

/**
 * Provides an interface to a client-side validation mechanism. For HTML forms, this can e.g. be java-script validation.
 * 
 * @author Rudolf Mayer
 * @version $Id: ClientsideValidationProvider.java,v 1.1 15.09.2007 13:28:28 Rudolf Mayer Exp $
 */
public interface ClientsideValidationProvider {

    /**
     * This method shall initialise the client side validations for the given field. After all fields are initialised,
     * {@link #getClientValidation(boolean)} and {@link #getOnSubmitValidation()} shall be able to provide their
     * validation mechanism, i.e. by having a java-script code prepared. <br>
     * This method is called from the {@link RecordEditor} and the {@link FormResponder}.
     */
    public void initField(String inputName, String formIdentifier, FieldDefinition fieldDefinition,
            boolean liveValidation);

    /** Shall provide the calls needed for doing validation on e.g. form submission. */
    public StringBuffer getOnSubmitValidation();

    /** Shall return all calls needed for client side validation, e.g. providing some java-script code. */
    public StringBuffer getClientValidation(boolean validateLive);

    /**
     * Return an array of file names to libraries that shall be included. Makumba could check via page analysis
     * {@link MakumbaJspAnalyzer} if the libraries are already included by the programmer, and add them if needed.
     */
    public String[] getNeededJavaScriptFileNames();

    /** Applies any formatting needed on the provided regular expression. */
    public String formatRegularExpression(String expression);
}
