package ubongo.machine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ubongo.common.networkUtils.SSHConnectionProperties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;

@XmlRootElement(name = "configuration")
@XmlAccessorType(XmlAccessType.FIELD)
public class Configuration {

    private static Logger logger = LogManager.getLogger(Configuration.class);

    @XmlElement(name = "ssh-connection")
    private SSHConnectionProperties sshConnectionProperties;

    public SSHConnectionProperties getSshConnectionProperties() {
        return sshConnectionProperties;
    }

    static public Configuration loadConfiguration(String path) throws UnmarshalException {
        Configuration configuration;
        File file = new File(path);
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Configuration.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            configuration = (Configuration) unmarshaller.unmarshal(file);
        } catch (JAXBException e) {
            logger.error("Failed to parse configuration file (file path: "
                    + file.getAbsolutePath() + ").", e);
            configuration = null;
        }
        if (configuration == null) {
            throw new UnmarshalException("Failed to load configuration. Make sure that " + file.getAbsolutePath() + " exists and is configured correctly");
        }
        return configuration;
    }
}
