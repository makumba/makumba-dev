package org.makumba.providers;

import java.io.Serializable;
import java.net.URL;
import java.util.Properties;

/**
 * This class knows how to read Makumba configuration and is used internally by different classes that need specifc
 * services. It can be seen as a service dispatcher in a way.
 * 
 * @author Manuel Gay
 * @version $Id: Configuration.java,v 1.1 28.09.2007 11:15:00 Manuel Exp $
 */
public class Configuration implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String defaultDataDefinitionProvider = "org.makumba.providers.datadefinition.makumba.MakumbaDataDefinitionFactory";

    private String defaultTransactionProvider = "org.makumba.db.makumba.MakumbaTransactionProvider";

    private static Properties controllerConfig;

    static {
        controllerConfig = new Properties();
        try {
            URL controllerURL = org.makumba.commons.ClassResource.get("MakumbaController.properties");
            controllerConfig.load(controllerURL.openStream());
        } catch (Exception e) {
            controllerConfig = null;
        }
    }

    public Configuration() {
        if (controllerConfig != null) {
            String defaultTransactionProvider = (String) controllerConfig.get("defaultTransactionProvider");
            if (defaultTransactionProvider != null) {
                this.defaultTransactionProvider = defaultTransactionProvider;
            }
        }
    }

    private String getDefaultDataDefinitionProviderClass() {
        return defaultDataDefinitionProvider;
    }

    private String dataDefinitionProvider = null;

    /**
     * Gives the data definition provider implementation to use
     * 
     * @return a String containing the class name of the data definition provider implementation
     */
    public String getDataDefinitionProviderClass() {

        // FIXME this should lookup a configuration file and return whatever is specified there
        return (dataDefinitionProvider == null) ? getDefaultDataDefinitionProviderClass() : dataDefinitionProvider;
    }

    /**
     * Gives the transaction provider implementation to use
     * 
     * @return a String containing the class name of the transaction provider implementation
     */
    public String getDefaultTransactionProviderClass() {
        return defaultTransactionProvider;
    }

    public void setDataDefinitionProvider(String ddp) {
        this.dataDefinitionProvider = ddp;
    }

    public void setDefaultTransactionProvider(String defaultTransactionProvider) {
        this.defaultTransactionProvider = defaultTransactionProvider;
    }

    public static String getClientSideValidationDefault() {
        // TODO: get this from some config file
        return "live";
    }

    public static boolean getReloadFormOnErrorDefault() {
        // TODO: get this from some config file
        return false;
    }

    public static String getCalendarEditorDefault() {
        // TODO: get this from some config file
        return "false";
    }

}