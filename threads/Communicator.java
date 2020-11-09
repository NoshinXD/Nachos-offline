package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
   
    
    public Communicator() {
         conditionVariablelock = new Lock();
         speakerVariable = new Condition2((conditionVariablelock));
         listenerVariable = new Condition2((conditionVariablelock));
         sleepingListenerCount = 0;
         sleepingSpeakerCount = 0;
         DataVariable = 0;
        
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        conditionVariablelock.acquire();
        
        if(sleepingListenerCount>0){
            listenerVariable.wake();
            DataVariable = word;
            sleepingListenerCount--;
        }
        
        else{
            sleepingSpeakerCount++;
            speakerVariable.sleep();
            DataVariable = word;
            listenerVariable.wake();
            sleepingListenerCount--;
            
        }
        
        
        
        conditionVariablelock.release();
        
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    
    
    public int listen() {
        int ret = 0;
        conditionVariablelock.acquire();
        
        if(sleepingSpeakerCount>0){
            speakerVariable.wake();
            sleepingListenerCount++;
            listenerVariable.sleep();
            ret = DataVariable;
            
            
            
        }
        else{
            sleepingListenerCount++;
            listenerVariable.sleep();
            ret = DataVariable;
            
        }
        
        conditionVariablelock.release();
        
        
	return ret;
    }
    
     private Lock conditionVariablelock;
     private Condition2 speakerVariable;
     private Condition2 listenerVariable;
     private int DataVariable;
     private int sleepingSpeakerCount;
     private int sleepingListenerCount;
     
     
}
