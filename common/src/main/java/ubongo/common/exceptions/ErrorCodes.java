package ubongo.common.exceptions;


public class ErrorCodes {

    public static final int GENERAL_DISPATCHER_EXCEPTION = 100;
    public static final int UNIT_UNMARSHAL_EXCEPTION = 101;

    public static final int GENERAL_QUEUE_MANAGEMENT_EXCEPTION = 200;
    public static final int QUEUE_MANAGEMENT_CONNECTION_EXCEPTION = 201;

    public static final int GENERAL_DB_PROXY_EXCEPTION = 300;
    public static final int DB_CONNECTION_EXCEPTION = 301;
    public static final int SSH_CONNECTION_EXCEPTION = 302;
    public static final int DB_CONNECTION_PROPERTIES_UNMARSHAL_EXCEPTION = 303;
    public static final int SSH_CONNECTION_PROPERTIES_UNMARSHAL_EXCEPTION = 304;
    public static final int QUERY_FAILURE_EXCEPTION = 305;
    public static final int EMPTY_RESULT_SET = 306;

    public static final int GENERAL_MACHINES_MANAGEMENT_EXCEPTION = 400;
    public static final int FAILED_TO_LOAD_MACHINES_EXCEPTION = 401;
    public static final int NO_AVAILABLE_MACHINE_EXCEPTION = 402;

    public static final int GENERAL_SYNCHRONIZATION_EXCEPTION = 500;
    public static final int MACHINES_SYNCHRONIZATION_EXCEPTION = 501;
    public static final int UNITS_SYNCHRONIZATION_EXCEPTION = 502;

}
