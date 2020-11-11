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
    
    private class Pair {
        public KThread t;
        public long time;
        
        /*public int compareTo(Pair p2)
        {
            if(time > p2.time) return 1;
            else if(time < p2.time) return -1;
            else return 0;
        }*/
    }
    
    Queue<Pair> sleepingQueue = new LinkedList<>();
}
