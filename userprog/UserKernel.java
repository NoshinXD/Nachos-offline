package nachos.userprog;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import nachos.machine.*;
import nachos.machine.Processor;
import nachos.threads.*;
import nachos.userprog.*;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    //public static Lock pageSychLock;
    //private static LinkedList<Integer> pagetable = new LinkedList<>();
    
    public void initialize(String[] args) {
	super.initialize(args);
       // pageSychLock = new Lock();

	console = new SynchConsole(Machine.console());

	swapFile= nachos.threads.ThreadedKernel.fileSystem.open("swapFile.txt",true);
//	swapFile.write(0,"hi".getBytes(),0,"hi".getBytes().length);
        currentPosition = 0;


	
	Machine.processor().setExceptionHandler(new Runnable() {
		public void run() { exceptionHandler(); }
	    });
        
        for(int i=0;i<Machine.processor().getNumPhysPages();i++){
            pagetable.add(i);
        }
    }

    /**
     * Test the console device.
     */	
    public void selfTest() {
	super.selfTest();

	System.out.println("Testing the console device. Typed characters");
	System.out.println("will be echoed until q is typed.");

	char c;

	do {
	    c = (char) console.readByte(true);
	    console.writeByte(c);
	}
	while (c != 'q');

	System.out.println("");
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;
	
	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
	super.run();

	UserProcess process = UserProcess.newUserProcess();
	
	String shellProgram = Machine.getShellProgramName();	
	Lib.assertTrue(process.execute(shellProgram, new String[] { }));

	KThread.currentThread().finish();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }
    
    synchronized public static int allocatePage(){
        int returnPage = -1;
        
        //boolean s = Machine.interrupt().disable();
        //pageSychLock.acquire();
        if(!pagetable.isEmpty()){
            returnPage = pagetable.removeFirst();
        }
        else
        {
            int randomPPN = (int) Math.floor(Math.random()*pagetable.size());
            Hashtable<nachos.userprog.UserProcess.InvertedPageTableIndex, nachos.machine.TranslationEntry> invertedPageTable = nachos.userprog.UserProcess.invertedPageTable;
            int i = 0;
            nachos.machine.TranslationEntry swapEntry = null;
            nachos.userprog.UserProcess.InvertedPageTableIndex swapIndex = null;
            for(nachos.userprog.UserProcess.InvertedPageTableIndex t :invertedPageTable.keySet())
            {
                if(invertedPageTable.get(t).ppn==randomPPN)
                {
                    swapEntry = invertedPageTable.get(t);
                    swapIndex = t;
                    break;
                }
            }
            if(swapEntry==null || swapIndex == null)
            {
                System.out.println("shouldn't happen");
            }
            if(swapFilePosition.containsKey(swapIndex)==true)
            {
                if(swapEntry.dirty==true)
                {
//                    swapFile.write(0,"hi".getBytes(),0,"hi".getBytes().length);
                    byte[] memory = Machine.processor().getMemory();
                    int physicalStartAddr = Machine.processor().makeAddress(randomPPN, 0);
                    byte[] data = new byte[Processor.pageSize];
                    for(int j = 0;j<data.length;j++){
                        data[j]= memory[physicalStartAddr+j];
                    }
                    int pagePosition = swapFilePosition.get(swapIndex);
                    swapFile.write(pagePosition,data,0,data.length);

                }
            }
            else
            {
                byte[] memory = Machine.processor().getMemory();
                int physicalStartAddr = Machine.processor().makeAddress(randomPPN, 0);
                byte[] data = new byte[Processor.pageSize];
                for(int j = 0;j<data.length;j++){
                    data[j]= memory[physicalStartAddr+j];
                }
                swapFile.write(currentPosition,data,0,data.length);
                swapFilePosition.put(swapIndex,currentPosition);
                currentPosition+=data.length;
            }
            returnPage = randomPPN;
            invertedPageTable.remove(swapIndex);
//            System.out.println("hi");
        }

        //pageSychLock.release();
        //Machine.interrupt().restore(s);
        
        return returnPage;
        
    }
    
    synchronized public static void reclaimPage(int pagenumber){
        
       // pageSychLock.acquire();
        pagetable.addFirst(pagenumber);
       // pageSychLock.release();
        
    }
    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
    private static LinkedList<Integer> pagetable = new LinkedList<>();
    //private static Lock pageSychLock = new Lock();
    public static nachos.machine.OpenFile swapFile;
    public static int currentPosition = 0;
    public static HashMap<nachos.userprog.UserProcess.InvertedPageTableIndex, Integer> swapFilePosition;
    
}
