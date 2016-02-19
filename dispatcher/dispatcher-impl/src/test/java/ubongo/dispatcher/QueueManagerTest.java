package ubongo.dispatcher;


import ubongo.common.datatypes.BaseUnit;
import ubongo.common.datatypes.Machine;
import ubongo.common.datatypes.Task;
import ubongo.common.datatypes.Unit;
import ubongo.common.exceptions.QueueManagementException;
import ubongo.dispatcher.db.DBProxy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({QueueManager.class, MachinesManager.class, ExecutionProxy.class})
@PowerMockIgnore("javax.management.*")
public class QueueManagerTest {

    private static final String URL = "dbUrl";
    private static final String USER = "dbUser";
    private static final int N_UNITS = 10;

    private QueueManager queueManager;
    private AtomicInteger executionCounter;
    private final Object executionCounterLock = new Object();
    private List<Unit> unitList;
    private List<Task> taskList = new ArrayList<>();

    @Before
    public void init() throws Exception {

        executionCounter = new AtomicInteger();

        // mock DBProxy
        DBProxy dbProxyMock = mock(DBProxy.class);
        when(dbProxyMock.getUrl()).thenReturn(URL);
        when(dbProxyMock.getUser()).thenReturn(USER);
        when(dbProxyMock.getNewTasks()).thenAnswer(new Answer<Object>() {
            private int count = 0;
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if (count >= N_UNITS) {
                    return new ArrayList<Task>();
                }
                for (Task task : taskList) {
                    task.setId(++count);
                }
                return taskList;
            }
        });
        doNothing().when(dbProxyMock).disconnect();
        doNothing().when(dbProxyMock).connect();
        doNothing().when(dbProxyMock).updateStatus(Matchers.any());
        doAnswer(invocation -> {
            taskList = (List<Task>) invocation.getArguments()[0];
            return null;
        }).when(dbProxyMock).add(Matchers.anyListOf(Task.class));

        // mock ExecutionProxy
        ExecutionProxy executionProxyMock = mock(ExecutionProxy.class);
        Whitebox.setInternalState(ExecutionProxy.class, "INSTANCE", executionProxyMock);
        doAnswer(invocation -> {
            synchronized (executionCounterLock) {
                executionCounter.getAndIncrement();
            }
            return null;
        }).when(executionProxyMock).execute(Matchers.any(Task.class), Matchers.any(QueueManager.class));

        // mock Machine and MachinesManager
        Machine machineMock = mock(Machine.class);
        when(machineMock.getId()).thenReturn(0);
        MachinesManager machinesManagerMock = mock(MachinesManager.class);
        PowerMockito.whenNew(MachinesManager.class).withAnyArguments().thenReturn(machinesManagerMock);
        doReturn(machineMock).when(machinesManagerMock).getAvailableMachine();

        unitList = new ArrayList<>();
        for (int i = 1; i <= N_UNITS; i++) {
            BaseUnit unit = new BaseUnit();
            unit.setId(i);
            unitList.add(unit);
        }
        queueManager = new QueueManager(dbProxyMock, machinesManagerMock);
        queueManager.start();
    }

    @Test
    public void testEnqueue() throws InterruptedException {
        queueManager.enqueue(unitList);
        int i;
        for (i = 0; i < N_UNITS && executionCounter.get() < N_UNITS; i++) {
            Thread.sleep(500);
        }
        assertTrue(N_UNITS == executionCounter.get());
    }

    @After
    public void clean() throws QueueManagementException {
        queueManager.shutdownNow();
    }

}
