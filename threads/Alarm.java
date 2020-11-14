package nachos.threads;

import java.util.LinkedList; 
import java.util.Queue;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another threadCauses the current
     * thread to yield, forcing a context switch if there is
     * that should be run.
     */
    public void timerInterrupt() {
        boolean s = Machine.interrupt().disable();
        long current_time = Machine.timer().getTime();
        int len = sleepingQueue.size();
        
        for(int i=0; i<len; i++)
        {
            Pair p = sleepingQueue.remove();
            if(p.time < current_time)
            {
                p.t.ready();
            }
            else
            {
                sleepingQueue.add(p);
            }
        }
        KThread.currentThread().yield();
        Machine.interrupt().restore(s);
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
        long wakeTime = Machine.timer().getTime() + x;
        KThread ct = KThread.currentThread();
        
        if(ct != null)
        {
            boolean s = Machine.interrupt().disable();
            Pair newEntry = new Pair();
            newEntry.t = ct;
            newEntry.time = wakeTime;
            sleepingQueue.add(newEntry);
            KThread.sleep();
            Machine.interrupt().restore(s);
        }
       
	/*while (wakeTime > Machine.timer().getTime())
	    KThread.yield();*/
    }
    
    private static class AlarmTest implements Runnable {
	AlarmTest(long time, Alarm alarm) {
	    this.time = time;
            this.alarm = alarm;
	}
	
	public void run() {
	    /*for (int i=0; i<5; i++) {
		System.out.println("*** thread " + which + " looped "
				   + i + " times");
		currentThread.yield();
	    }*/
            
            /*KThread k1 = new KThread(new PingTest(which + 1)).setName("forked thread " + (which + 1));
            k1.fork();
            k1.join();*/
            System.out.println(KThread.currentThread().getName() + " sleeps at " + Machine.timer().getTime());
            alarm.waitUntil(time);
            System.out.println(KThread.currentThread().getName() + " wakes at " + Machine.timer().getTime());
	}
        
    
	private long time;
        private Alarm alarm;
    }
    
    public static void selfTest() {
        System.out.println("******** testing alarm and waitUntil  **********");
	Alarm alarm = ThreadedKernel.alarm;
        long time1 = 10000;
        long time2 = 20000;
        long time3 = 30000;
        KThread k1 = new KThread(new AlarmTest(time1, alarm)).setName("alarmed thread1");
        k1.fork();
        KThread k2 = new KThread(new AlarmTest(time2, alarm)).setName("alarmed thread2");
        k2.fork();
        KThread k3 = new KThread(new AlarmTest(time3, alarm)).setName("alarmed thread3");
        k3.fork();
        
        k1.join();
        k2.join();
        k3.join();

        System.out.println("******** testing of alarm and waitUntil completed **********");
    }
    
    private class Pair {
        public KThread t;
        public long time;
    }
    
    Queue<Pair> sleepingQueue = new LinkedList<>();
}
