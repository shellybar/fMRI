package edu.tau.eng.neuroscience.mri.dispatcher;

import com.jcraft.jsch.JSchException;

import java.sql.SQLException;

public class DBProxyTest {

    public static void main(String[] args) throws SQLException, ClassNotFoundException, JSchException {

        String mySqlHost = "mysqlsrv.cs.tau.ac.il";
        String schema = "brain_tau_db";
        int mySqlServerPort = 3306;
        String user = "brain_tau_db";
        String password = "brain_tau_db";

        DBProxy dbProxy = new DBProxy(mySqlHost, mySqlServerPort, schema, user, password);
        dbProxy.connect();

    }

}
