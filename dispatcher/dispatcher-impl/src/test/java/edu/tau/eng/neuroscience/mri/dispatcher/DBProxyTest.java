package edu.tau.eng.neuroscience.mri.dispatcher;

import edu.tau.eng.neuroscience.mri.common.constants.SystemConstants;
import edu.tau.eng.neuroscience.mri.dispatcher.db.DBProxy;
import edu.tau.eng.neuroscience.mri.dispatcher.db.DBProxyException;

public class DBProxyTest {

    public static void main(String[] args) throws DBProxyException {

        DBProxy dbProxy = new DBProxy(SystemConstants.BASE_DIR + "/configs/db_connection.xml");
        dbProxy.connect();
        dbProxy.disconnect();
    }

}
