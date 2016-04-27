package ubongo.persistence.db;

public class DBConstants {

    public final static String DEBUG_PREFIX = "zz_debug_";

    public final static String QUERY_GET_FLOW_TASKS = "get_flow_tasks";
    public final static String QUERY_GET_NEW_TASKS = "get_new_tasks";
    public final static String QUERY_GET_TASK_BY_ID = "get_task_by_id";
    public final static String QUERY_UPDATE_TASK_STATUS = "update_task_status";
    public final static String QUERY_CREATE_FLOW = "create_flow";
    public final static String QUERY_START_FLOW = "start_flow";
    public final static String QUERY_CLEAR_TABLES = "clear_tables";
    public final static String QUERY_CREATE_ANALYSIS = "create_analysis";

    public final static String TASKS_TABLE_NAME = "tasks";
    public final static String TASKS_TASK_ID = "task_id";
    public final static String TASKS_FLOW_ID = "flow_id";
    public final static String TASKS_SERIAL_NUM = "serial_in_flow";
    public final static String TASKS_TASK_STATUS = "status";
    public final static String TASKS_UNIT_ID = "unit_id";
    public final static String TASKS_UNIT_PARAMS = "unit_params";
    public final static String TASKS_STUDY = "study";
    public final static String TASKS_SUBJECT = "subject";
    public final static String TASKS_RUN = "run";
    public final static String TASKS_MACHINE_ID = "machine_id";

    public final static String FLOWS_TABLE_NAME = "flows";

    public final static String UNITS_TABLE_NAME = "units";

}
