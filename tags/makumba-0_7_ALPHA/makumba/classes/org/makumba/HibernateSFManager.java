package org.makumba;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import javassist.CannotCompileException;
import javassist.NotFoundException;

import javax.xml.transform.TransformerConfigurationException;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.makumba.commons.ClassResource;
import org.makumba.commons.NameResolver;
import org.makumba.commons.NamedResourceFactory;
import org.makumba.commons.NamedResources;
import org.makumba.db.hibernate.MddToClass;
import org.makumba.db.hibernate.MddToMapping;
import org.makumba.providers.TransactionProvider;
import org.xml.sax.SAXException;

/**
 * 
 * @author manu
 * @author rudi
 * @version $Id$
 */
public class HibernateSFManager {

    private static final String PREFIX = "makumbaGeneratedMappings";

    private static final String SEED = "SEED.txt";

    public static String findClassesRootFolder(String locatorSeed) {
        String rootFolder = "";
        try {
            rootFolder = new File(ClassResource.get(locatorSeed).getFile()).getParentFile().getCanonicalPath();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return rootFolder;
    }
    public static int sessionFactories = NamedResources.makeStaticCache("hibernate session factory", new NamedResourceFactory() {

        private static final long serialVersionUID = 1L;
    
        protected Object makeResource(Object nm, Object hashName) throws Exception {
            return makeSF((String)nm);
        }
    }, false);


    public static SessionFactory getSF(String cfgFilePath) {
        return (SessionFactory)NamedResources.getStaticCache(sessionFactories).getResource(cfgFilePath);
    }
    
    private static SessionFactory makeSF(String cfgFilePath){
            cfgFilePath+= ".cfg.xml";
            
            Configuration cfg = new Configuration().configure(cfgFilePath);
            String seed, prefix;
            if((seed = cfg.getProperty("makumba.seed")) == null)
                seed = SEED;
            String seedDir= findClassesRootFolder(seed);
            java.util.logging.Logger.getLogger("org.makumba." + "hibernate.sf").info("Generating classes under "+ seedDir);

            if((prefix = cfg.getProperty("makumba.prefix")) == null)
                prefix = PREFIX;

            java.util.logging.Logger.getLogger("org.makumba." + "hibernate.sf").info("Generating mappings under "+ seedDir+File.separator+prefix);
          
            String mddList;
            Vector dds;
            if((mddList = cfg.getProperty("makumba.mdd.list")) == null)
            {
                String mddRoot;
                if((mddRoot = cfg.getProperty("makumba.mdd.root")) == null)
                    mddRoot="dataDefinitions";
                java.util.logging.Logger.getLogger("org.makumba." + "hibernate.sf").info("Working with the MDDs under "+ mddRoot);
                dds= org.makumba.MakumbaSystem.mddsInDirectory(mddRoot);
            }else{
                dds= new Vector();
                java.util.logging.Logger.getLogger("org.makumba." + "hibernate.sf").info("Working with the MDDs "+ mddList);
                for(StringTokenizer st= new StringTokenizer(mddList, ","); st.hasMoreTokens();){
                    dds.addElement(st.nextToken().trim());
                }
            }
            
            java.util.logging.Logger.getLogger("org.makumba." + "hibernate.sf").info("Generating classes");

//          FIXME this is an ugly workaround for the current state of the code. there should be only ONE config file, not two
            String databaseProperties = cfgFilePath.substring(0, cfgFilePath.indexOf(".cfg.xml"))+".properties";
            Properties p= new Properties();
            try {
                p.load(org.makumba.commons.ClassResource.get(databaseProperties).openStream());
            } catch (Exception e) {
                throw new org.makumba.ConfigFileError(databaseProperties);
            }

            NameResolver nr = new NameResolver(p);
            
            try {
                MddToClass jot = new MddToClass(dds, seedDir, nr);
            } catch (CannotCompileException e) {
                e.printStackTrace();
            } catch (NotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            java.util.logging.Logger.getLogger("org.makumba." + "hibernate.sf").info("Generating mappings");
            
            try {
                MddToMapping xot = new MddToMapping(dds, cfg, org.makumba.HibernateSFManager
                        .findClassesRootFolder(seed), prefix, nr);
      
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
            java.util.logging.Logger.getLogger("org.makumba." + "hibernate.sf").info("building session factory");
            SessionFactory sessionFactory = cfg.buildSessionFactory();
            
            if("true".equals(cfg.getProperty("makumba.schemaUpdate"))){
//                if(!schemaUpd)
//                    throw new ProgrammerError("Hibernate schema update must be authorized, remove it from cfg.xml!");
                java.util.logging.Logger.getLogger("org.makumba." + "hibernate.sf").info("Peforming schema update");
                SchemaUpdate schemaUpdate = new SchemaUpdate(cfg);
                schemaUpdate.execute(true, true);
                java.util.logging.Logger.getLogger("org.makumba." + "hibernate.sf").info("Schema update finished");
            }else
                java.util.logging.Logger.getLogger("org.makumba." + "hibernate.sf").info("skipping schema update");
                
        
        return sessionFactory;
    }

    public static synchronized SessionFactory getSF() {
            String configFile;
            String defaultDataSourceName = new TransactionProvider().getDefaultDataSourceName();
            if(defaultDataSourceName == null) {
                configFile = "default";
            } else {
                configFile = defaultDataSourceName;
            }
            java.util.logging.Logger.getLogger("org.makumba." + "hibernate.sf").info("Initializing configuration from "+configFile);
            return getSF(configFile);
    }

    
    public static Configuration getConfiguration(String cfgFilePath) {
        Configuration cfg = new Configuration().configure(cfgFilePath);
        return cfg;
    }
    

}