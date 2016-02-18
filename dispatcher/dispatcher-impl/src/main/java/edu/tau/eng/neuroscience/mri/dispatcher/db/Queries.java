package edu.tau.eng.neuroscience.mri.dispatcher.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

public class Queries {

    private static final String propFileName = "queries.properties";
    private static Properties props;

    public static Properties getQueries() throws SQLException {
        String errorMsg = "Unable to load property file: " + propFileName;
        InputStream is = Queries.class.getClassLoader().getResourceAsStream(propFileName);
        if (is == null){
            throw new SQLException(errorMsg);
        }
        if (props == null) {
            props = new Properties();
            try {
                props.load(is);
            } catch (IOException e) {
                throw new SQLException(errorMsg + ". Details: " + e.getMessage());
            }
        }
        return props;
    }

    public static String getQuery(String query) throws SQLException{
        return getQueries().getProperty(query);
    }

}
