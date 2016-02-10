package edu.tau.eng.neuroscience.mri.common.exceptions;


public class ErrorCodes {

    public static final int GENERAL_DISPATCHER_EXCEPTION = 7000;
    public static final int UNIT_UNMARSHAL_EXCEPTION = 7001;

    public static final int GENERAL_QUEUE_MANAGEMENT_EXCEPTION = 8000;
    public static final int QUEUE_MANAGEMENT_CONNECTION_EXCEPTION = 8001;

    public static final int GENERAL_DB_PROXY_EXCEPTION = 9000;
    public static final int DB_CONNECTION_EXCEPTION = 9001;
    public static final int SSH_CONNECTION_EXCEPTION = 9002;
    public static final int DB_CONNECTION_PROPERTIES_UNMARSHAL_EXCEPTION = 9003;
    public static final int SSH_CONNECTION_PROPERTIES_UNMARSHAL_EXCEPTION = 9004;
    public static final int QUERY_FAILURE_EXCEPTION = 9005;

}
