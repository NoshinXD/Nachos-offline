package nachos.vm;

import nachos.machine.*;
import nachos.machine.Processor;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.io.File;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public VMProcess() {
	super();
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
        super.saveState();
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        int tlbSize = nachos.machine.Machine.processor().getTLBSize();
        //System.out.println(tlbSize);
        for(int i=0;i<tlbSize;i++)
        {
            nachos.machine.TranslationEntry t = nachos.machine.Machine.processor().readTLBEntry(i);
            t.valid = false;
            nachos.machine.Machine.processor().writeTLBEntry(i,t);

        }
	//super.restoreState();
    }

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return	<tt>true</tt> if successful.
     */
    protected boolean loadSections() {
	return super.loadSections();
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
	super.unloadSections();
    }    

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();




	switch (cause) {

	    case Processor.exceptionTLBMiss:
            int badAddr = processor.readRegister(Processor.regBadVAddr);
            handleTLBMiss(badAddr);
            break;
	default:
	    super.handleException(cause);
	    break;
	}
    }


    // my functions
    public void handleTLBMiss(int badAddr) {
        //System.out.println("bad adress: "+badAddr);
        int vpn = Machine.processor().pageFromAddress(badAddr);
//        File f = new File("killme.txt");
//        try {
//            f.createNewFile();
//        } catch (Exception e)
//        {
//            System.out.println(e);
//        }

        //System.out.println("vpn: "+vpn);
        nachos.machine.TranslationEntry t = null;
        t = invertedPageTable.get(new InvertedPageTableIndex(processId,vpn));
        if(t==null)
        {
            System.out.println("null sorry!");
            for(int i=0;i<pageTable.length;i++)
            {
                if(pageTable[i].vpn == vpn)
                {
                    t = pageTable[i];
                    break;
                }
            }
        }
        if(t==null)
        {
            System.out.println("no entry found");
        }
        else
        {
            //System.out.println(t.vpn+" "+t.ppn+" "+t.valid+" "+t.dirty+" "+t.readOnly+" "+t.used);
        }
        int index = -5;
        Processor p= nachos.machine.Machine.processor();
        for(int i=0;i<p.getTLBSize();i++)
        {
            if(p.readTLBEntry(i).valid == false)
            {
                index = i;
                break;
            }
        }

        if(index == -5)
        {
            int temp = (int) Math.floor((Math.random()*p.getTLBSize()));
            index = temp%p.getTLBSize();
        }

        //System.out.println(index);

        p.writeTLBEntry(index,t);

    }
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';
}
