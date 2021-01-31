
package hu.kag.yavoter;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

public class Config {
    private static File configFile   = null;
    private static Properties config = null;

    private Config() { }

    /**
     Loads the global properties from a file.
     @param path A {@link File} from which to load configuration data
     @throws FileNotFoundException if the config file couldn't be found
     @throws IOException if an I/O error occurs
     */
    public static void load( String path ) {
        configFile = new File(path);
        reload();
    }

    /**
     Reloads the global properties from a file.
     @throws FileNotFoundException if the config file couldn't be found
     @throws IOException if an I/O error occurs
     */
    public static synchronized void reload()    {
        if (configFile!=null) {
            Properties nconfig = new Properties();
            try {
                nconfig.putAll(System.getProperties());
                nconfig.load(new FileInputStream(configFile));
                replacePropReferences(nconfig);
                config=nconfig;
            } catch (Exception e) {
                throw new RuntimeException("Config load error",e);
            }
        }
    }

    /**
     Gets a configuration value.
     @param key The key to retrieve
     @return The value for that key, or <code>null</code>
     */
    public static String get( String key )  {
        return get( key, null );
    }

    /**
     Gets a configuration value, with default.
     @param key The key to retrieve
     @param defaultValue The default value if the key is not found
     @return The value for that key
     */
    public static synchronized String get( String key, String defaultValue )    {
        return config.getProperty( key, defaultValue );
    }

    public static int getInt(String key,int def) {
        String sk = get(key,null);
        return sk==null?def:Integer.parseInt(sk);
    }

    public static synchronized Collection<?> keySet() {
        return Collections.unmodifiableCollection((config.keySet()));
    }

    public static Properties getPropertiesWithPrefix(String prefix,boolean chop) {
        Properties prop = new Properties();
        Iterator<?> it = config.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<?, ?> p = (Entry<?, ?>) it.next();
            if (p.getKey().toString().startsWith(prefix)) {
                if (chop) {
                    prop.put(((String)p.getKey()).substring(prefix.length()+1),p.getValue());
                } else {
                    prop.put(p.getKey(),p.getValue());
                }
            }
        }
        return prop;
    }

    protected static void replacePropReferences(Properties props) {
        Object valueObj;

        Set<Entry<Object, Object>> pset = props.entrySet();
        Iterator<Entry<Object, Object>> it = pset.iterator();
        while (it.hasNext()) {
            Entry<Object, Object> entry = it.next();
            valueObj = entry.getValue();
            if (valueObj instanceof String) {
                valueObj = changeValue(valueObj.toString(),props);
            } else {
                continue;
            }

            if (valueObj!=null) {
                entry.setValue(valueObj);
            }
        }

    }

    protected static String changeValue(String value, Properties props) {
        boolean valueChanged = false;
        int idx, endIdx;
        String refdKey, refdValue;

        if (value != null) {
            idx = value.indexOf('{');
            while (idx >= 0) {
                endIdx = value.indexOf('}', idx + 1);
                if (endIdx >= 0) {
                    refdKey = value.substring(idx + 1, endIdx);
                    refdValue = props.getProperty(refdKey);
                    if (refdValue != null) {
                        // Ha van hivatkozott ertek, cserelunk
                        value =
                                value.substring(0, idx)
                                        + refdValue
                                        + value.substring(endIdx + 1);
                        valueChanged = true;
                        idx = value.indexOf('{', idx);
                    } else {
                        // Ha nincs ertek, nem cserelunk
                        idx = value.indexOf('{', idx + 1);
                    }
                } else {
                    // Ha nincs bezarva, akkor vege
                    idx = -1;
                }
            }
        }
        return valueChanged ? value : null;
    }

}