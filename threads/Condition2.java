package nachos.threads;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	this.conditionLock = conditionLock;
        waitingQueue = ThreadedKernel.scheduler.newThreadQueue(false);
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean s = Machine.interrupt().disable();
	conditionLock.release();
        waitingQueue.waitForAccess(KThread.currentThread());
        KThread.sleep();
	conditionLock.acquire();
        Machine.interrupt().restore(s);
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        boolean s = Machine.interrupt().disable();
        KThread wakingThread = waitingQueue.nextThread();
        if(wakingThread != null)
        {
            wakingThread.ready();
        }
        Machine.interrupt().restore(s);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        boolean s = Machine.interrupt().disable();
        
        while(true)
        {
            KThread wakingThread = waitingQueue.nextThread();
            if(wakingThread == null) break;
            wakingThread.ready();
        }
        
        Machine.interrupt().restore(s);
    }
    
    private static class Listener implements Runnable {
	Listener(int which, Communicator com) {
	    this.which = which;
            this.com = com;
	}
	
	public void run() {
	    for(int i=0; i<4; i++)
            {
                KThread.yield();
                com.listen();
                System.out.println("*** " + KThread.currentThread().getName() + " looped "
				   + i + " times");
                KThread.yield();
            }
	}

	private int which;
        private Communicator com;
    }
    
    private static class Speaker implements Runnable {
	Speaker(int which, Communicator com) {
	    this.which = which;
            this.com = com;
	}
	
	public void run() {
	    for(int i=0; i<6; i++)
            {
                KThread.yield();
                com.speak(i);
                System.out.println("*** " + KThread.currentThread().getName() + " looped "
				   + i + " times");
                KThread.yield();
               
            }
	}

	private int which;
        private Communicator com;
    }
   
    
    private static class Condition2Test implements Runnable {
	Condition2Test(int which) {
	    this.which = which;
            com = new Communicator();
	}
	
	public void run() {
	    //System.out.println("Testing for task 2 & 4 initiated...");
            System.out.println("******** testing condition variable and synchronous send and receive  **********");
            KThread l1 = new KThread(new Listener(1, com)).setName("listener thread 1");
            KThread l2 = new KThread(new Listener(2, com)).setName("listener thread 2");
            KThread l3 = new KThread(new Listener(3, com)).setName("listener thread 3");
            
            KThread s1 = new KThread(new Speaker(1, com)).setName("speaker thread 1");
            KThread s2 = new KThread(new Speaker(2, com)).setName("speaker thread 2");
            
            l1.fork();
            l2.fork();
            l3.fork();
            
            s1.fork();
            s2.fork();
            
            l1.join();
            l2.join();
            l3.join();
            
            s1.join();
            s2.join();
        System.out.println("******** testing of condition variable and synchronous send and receive  completed **********");
            
            //System.out.println("Testing for task 2 & 4 finished!");
	}
        
    
	private int which;
        private Communicator com;
        
    }
    
    public static void selfTest() {
	//Lib.debug(dbgThread, "Enter KThread.selfTest");
	
	//new KThread(new PingTest(1)).setName("forked thread").fork();
        //System.out.println("in Kthread_selfTest after fork");
	//new PingTest(0).run();
        //new KThread(new PingTest(1)).setName("forked thread1").fork();
        //new KThread(new PingTest(2)).setName("forked thread2").fork();
        //new KThread(new PingTest(3)).setName("forked thread3").fork();
        KThread t1 = new KThread(new Condition2Test(1)).setName("forked thread1");
        t1.fork();
        t1.join();
    }

    private Lock conditionLock;
    
    private ThreadQueue waitingQueue = null;
}
