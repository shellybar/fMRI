package edu.tau.eng.neuroscience.mri.dispatcher.db;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "db-connection")
@XmlAccessorType(XmlAccessType.FIELD)
public class DBConnectionProperties {

    @XmlElement private String host;
    @XmlElement private int port;
    @XmlElement private String schema;
    @XmlElement private String user;
    @XmlElement private String password;

    public DBConnectionProperties() {

    }

    public DBConnectionProperties(String host, int port, String schema, String user, String password) {
        this.host = host;
        this.port = port;
        this.schema = schema;
        this.user = user;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
