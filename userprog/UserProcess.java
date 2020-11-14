package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
	int numPhysPages = Machine.processor().getNumPhysPages();
	pageTable = new TranslationEntry[numPhysPages];
	for (int i=0; i<numPhysPages; i++)
	    pageTable[i] = new TranslationEntry(i,0, false,false,false,false);
        
        boolean s = Machine.interrupt().disable();
      
        staticLock.acquire();
        processId = totalProcess++;
        staticLock.release();
        
        files = new OpenFile[TOTALFILESIZE];
        files[0] = UserKernel.console.openForReading();
        files[1] = UserKernel.console.openForWriting();
        Machine.interrupt().restore(s);
        child = new LinkedList<>();
        childStatus = new HashMap<>();
        parent = null;
        
        
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
        staticLock.acquire();
        currentlyRunning++;
        staticLock.release();
        
	myThread = (UThread) new UThread(this).setName(name);
        myThread.fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <=data.length);
        int totalread = 0;
        int endVaddr = vaddr+ length - 1;
        
        byte[] memory = Machine.processor().getMemory();

        int lastAllowableAddress = Machine.processor().makeAddress(numPages - 1, pageSize - 1);
        
        if(vaddr<0 || endVaddr>lastAllowableAddress || endVaddr<vaddr || numPages == 0){
            return 0;
        }
        
        int firstVPN = Machine.processor().pageFromAddress(vaddr);
        int lastVPN = Machine.processor().pageFromAddress(endVaddr);
        
        for(int i=firstVPN; i<=lastVPN; i++){
            if( i<0 || i > pageTable.length || pageTable[i] == null || pageTable[i].valid == false){
                break;
            }
            
//            int physicalAddr = pageTable[i].ppn;
//            int end

//            
           int pageStart = Machine.processor().makeAddress(i, 0);
           int pageEnd = Machine.processor().makeAddress(i, pageSize-1);
           int readSize = 0;
           
           if(endVaddr > pageEnd){
               readSize = pageEnd - vaddr+1;
           }
           
           else{
               readSize = endVaddr - vaddr +1;
           }
           
           int readOffset = vaddr - pageStart;
           int physicalStartAddr = Machine.processor().makeAddress(pageTable[i].ppn, readOffset);
           
           for(int j = 0;j<readSize;j++){
               data[totalread+offset+j]= memory[physicalStartAddr+j];
           }
           
           totalread = totalread+readSize;
           vaddr = vaddr+readSize;
           
        }

//	if (vaddr < 0 || vaddr >= memory.length)
//	    return 0;

	//int amount = Math.min(length, memory.length-vaddr);
	//System.arraycopy(memory, vaddr, data, offset, amount);

	//return amount;
        return totalread;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
        
        // System.out.println("offset: " + offset);
        // System.out.println("length: " + length);
        // System.out.println("offset+length: " + (offset+length));
        // System.out.println("data.length: " + data.length);
        
        Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <=data.length);
        int totalwrite = 0;
        int endVaddr = vaddr+ length - 1;
        
        byte[] memory = Machine.processor().getMemory();

        int allowableLastAddress = Machine.processor().makeAddress(numPages - 1, pageSize - 1);
        
        if(vaddr<0 || endVaddr>allowableLastAddress || endVaddr < vaddr || numPages >= pageTable.length){
            return 0;
        }
        
        int firstVPN = Machine.processor().pageFromAddress(vaddr);
        int lastVPN = Machine.processor().pageFromAddress(endVaddr);
        
        for(int i=firstVPN; i<=lastVPN; i++){
            if(i<0 || i> pageTable.length ||  pageTable[i] == null || pageTable[i].readOnly || pageTable[i].valid == false){
                break;
            }
            
//            int physicalAddr = pageTable[i].ppn;
//            int end

//            
           int pageStart = Machine.processor().makeAddress(i, 0);
           int pageEnd = Machine.processor().makeAddress(i, pageSize-1);
           int writeSize = 0;
           
           if(endVaddr > pageEnd){
               writeSize = pageEnd - vaddr+1;
           }
           
           else{
               writeSize = endVaddr - vaddr +1;
           }
           
           int writeOffset = vaddr - pageStart;
           int physicalStartAddr = Machine.processor().makeAddress(pageTable[i].ppn, writeOffset);
           
           for(int j = 0;j<writeSize;j++){
               memory[physicalStartAddr+j]= data[totalwrite+offset+j];
           }
           
           totalwrite = totalwrite+writeSize;
           vaddr = vaddr+writeSize;
           
        }
        return totalwrite;

//	if (vaddr < 0 || vaddr >= memory.length)
//	    return 0;

	//int amount = Math.min(length, memory.length-vaddr);
	//System.arraycopy(memory, vaddr, data, offset, amount);

	//return amount;
       
//	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
//
//	byte[] memory = Machine.processor().getMemory();
//	
//	// for now, just assume that virtual addresses equal physical addresses
//	if (vaddr < 0 || vaddr >= memory.length)
//	    return 0;
//
//	int amount = Math.min(length, memory.length-vaddr);
//	System.arraycopy(data, offset, memory, vaddr, amount);
//
//	return amount;
    }
    
    private boolean allocate(int startingVPN, int totalPage, boolean readOnly)
    {
        ArrayList<TranslationEntry> pages = new ArrayList<>();
        
        for(int i=0; i<totalPage; i++)
        {
            if(startingVPN + i >= pageTable.length)
            {
                return false;
            }
            
            int ppn = UserKernel.allocatePage();
            if(ppn == -1)
            {
                for(int j=0; j<pages.size(); j++)
                {
                    numPages--;
                    TranslationEntry t = pages.get(j);
                    pageTable[t.vpn] = new TranslationEntry(t.vpn, 0, false, false, false, false);
                    UserKernel.reclaimPage(t.ppn);
                    
                }
                return false;
            }
            
            TranslationEntry newEntry = new TranslationEntry(startingVPN + i, ppn, true, readOnly, false, false);
            pages.add(newEntry);
            pageTable[startingVPN + i] = newEntry;
            numPages++;
        }
        return true;
    }
    
    private void returnAllPages()
    {
        for(int i=0; i<pageTable.length; i++)
        {
            TranslationEntry t = pageTable[i];
            if(t.valid)
            {
                UserKernel.reclaimPage(t.ppn);
                pageTable[t.vpn] = new TranslationEntry(t.vpn, 0, false, false, false, false);
                    
                
            }
        }
    }
    

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    //numPages += section.getLength();
            boolean allocated = allocate(numPages, section.getLength(), section.isReadOnly());
            if(!allocated)
            {
                returnAllPages();
                numPages = 0;
                return false;
            }
            
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

        
	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	boolean stackAllocated = allocate(numPages,stackPages,false);
        if(!stackAllocated)
        {
            returnAllPages();
            numPages = 0;
            return false;
        }




	initialSP = (numPages*pageSize);

	// and finally reserve 1 page for arguments
	//numPages++;
        boolean allocatedArg = allocate(numPages, 1, false);
        
        if(!allocatedArg)
        {
            returnAllPages();
            numPages = 0;
            return false;
        }
        

        if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
	if (numPages > Machine.processor().getNumPhysPages()) {
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}

	// load sections
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
		int vpn = section.getFirstVPN()+i;
            TranslationEntry entry = pageTable[vpn];

            if(entry==null || entry.valid== false )
            {
                return false;
            }



            // for now, just assume virtual addresses=physical addresses
		    section.loadPage(i, entry.ppn);
	    }
	}
	
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        returnAllPages();
        numPages = 0;

        for(int i=0; i<files.length; i++)
        {
            if(files[i] != null)
            {
                files[i].close();
                files[i] = null;
            }
        }

        coff.close();
        
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {

        
        if(processId!=0){
            return -1;
        }
	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }
    
    private int handleRead(int fileDescriptor,int virtualAddr,int size){
        
        if(fileDescriptor<0||fileDescriptor>=TOTALFILESIZE||files[fileDescriptor]==null||size<0 || virtualAddr<0){
            return -1;
            
        }
        
        byte[] buf = new byte[size];
        int totalRead = 0;
        if(fileDescriptor==0) {
            nachos.machine.OpenFile console = files[fileDescriptor];
            synchronized (console)
            {
                totalRead = console.read(buf, 0, size);
            }

            
        }
        if(totalRead== -1){
            return -1;
        }
        

        int written = writeVirtualMemory(virtualAddr,buf,0,totalRead);
        
        // totalRead = Math.min(totalRead,memory.length-virtualAddr);
        // if(totalRead<=0){
        //     return -1;
        // }
        // for(int i =0;i<totalRead;i++){
        //     memory[virtualAddr+i]= buf[i];
        // }
        
        return written;

        
    }
    
    
    
    private int handleWrite(int fileDescriptor,int virtualAddr,int size){
        
        //System.out.println("inside handle writeqqqqq"
                //+ "");
        if(fileDescriptor<0||fileDescriptor>=TOTALFILESIZE||files[fileDescriptor]==null||size<0 || virtualAddr<0){
            return -1;
            
        }
        
        byte[] buf = new byte[size];
        //byte[] memory = Machine.processor().getMemory();
        int totalRead = readVirtualMemory(virtualAddr,buf,0,size);
//        for(int i = 0;i<size;i++){
//            if((virtualAddr+i) >= memory.length)
//                break;
//            buf[i] = memory[virtualAddr+i];
//            totalRead++;
//        }
//
        int totalWrite = 0;
        if(fileDescriptor==1){

            nachos.machine.OpenFile console = files[fileDescriptor];
            synchronized (console)
            {
                totalRead = console.write(buf, 0, totalRead);
            }

            //totalWrite = files[fileDescriptor].write(buf, 0, totalRead);
            
        }
        
        //System.out.println("returning from write"+ totalWrite);
        return totalWrite;
    }
    
    private int handleExec(int fileVaddr, int argLength, int argVaddr)
    {
        int MAX_STRSIZE = 256;
        int lastVaddr = (pageTable.length * pageSize) - 1;
        
        if(fileVaddr < 0 || fileVaddr > lastVaddr || argVaddr < 0 || argVaddr > lastVaddr || argLength < 0)
        {
            return -1;
        }
        
        String fileName = readVirtualMemoryString(fileVaddr, MAX_STRSIZE);
        
        if(fileName == null || !fileName.contains(".coff"))
        {
            return -1;
        }
        
        String[] args = new String[argLength];
        
        for(int i=0; i<argLength; i++)
        {
            byte[] buf = new byte[4];
            int jumpOffset = i * 4;
            int readSize = readVirtualMemory(argVaddr + jumpOffset, buf);
            if(readSize != 4)
            {
                return -1;
            }
            int vAddrToRead = Lib.bytesToInt(buf, 0);
            String argument = readVirtualMemoryString(vAddrToRead, MAX_STRSIZE);
            
            if(argument == null)
            {
                return -1;
            }
            args[i] = argument;
        }
        
        UserProcess childProcess = UserProcess.newUserProcess();
        
        boolean executed = childProcess.execute(fileName, args);
        
        if(!executed)
        {
            return -1;
        }

        child.add(childProcess);
        childProcess.parent = this;
        return childProcess.processId;
    }
    
    private int handleJoin(int processID, int statVaddr)
    {
        if(processID < 0 || statVaddr < 0 || statVaddr >= (pageTable.length * pageSize))
        {
            return -1;
        }
        
        UserProcess joinChild = null;
        
        // int joinChildIndex = -1; // doing this thing later


        for (UserProcess c: child)
        {
            if(c.processId== processID)
            {
                joinChild = c;
                break;
            }
        }
        

        
        if(joinChild == null)
        {
            return -1;
        }
        
        joinChild.myThread.join();
        joinChild.parent = null;
        
        parentAccessLock.acquire();
         
         // doing this here
        

        

        
        Integer joinChildStatus = childStatus.get(joinChild.processId);
        child.remove(joinChild);
        
        parentAccessLock.release();
        
        if(joinChildStatus == null)
        {
            return 0;
        }
        else
        {
            byte[] buf = new byte[4];
            buf = Lib.bytesFromInt(joinChildStatus);
            
            int writeSize = writeVirtualMemory(statVaddr, buf);
            if(writeSize == 4)
            {
                return 1;
            }
            else
            {
                return 0;
            }
        }
    }
    
    private int handleExit(int exitStatus)
    {
        
        
        

        
        //System.out.println(" ekhane error thakte pare");
        if(parent != null)
        {
            parentAccessLock.acquire();
            parent.childStatus.put(processId,exitStatus);
            
            parentAccessLock.release();
        }

        unloadSections();
        
        for(UserProcess c: child)
        {
            c.parent = null;
            
        }

        staticLock.acquire();
        currentlyRunning--;
        staticLock.release();
        //System.out.println("currentlyRunning: "+currentlyRunning);
//        UThread.finish();
        if(currentlyRunning == 0)
        {
            Kernel.kernel.terminate();
        }
        else
        {
            UThread.finish();
        }
        
        return 0;
    }

    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
            
        case syscallRead:
	    return handleRead(a0,a1,a2);
        
         case syscallWrite:
	    return handleWrite(a0,a1,a2);
            
         case syscallExec:
             return handleExec(a0, a1, a2);
            
         case syscallJoin:
             return handleJoin(a0, a1);
             
         case syscallExit:
             return handleExit(a0);

	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
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
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
        System.out.println("Unexpected exception: " +
                Processor.exceptionNames[cause]);
	    handleExit(-1);

//	    Lib.debug(dbgProcess, "Unexpected exception: " +
//		      Processor.exceptionNames[cause]);
//	    Lib.assertNotReached("Unexpected exception");
	}
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    // our defined datastructure
    
    private  static Lock staticLock = new Lock();
    private static Lock parentAccessLock = new Lock();
    
    private static int totalProcess = 0;
    private static final int TOTALFILESIZE=10;
    private static int currentlyRunning = 0;
    
    private int processId;
        
    private OpenFile[] files;
    private UserProcess parent;
    private LinkedList<UserProcess> child ;
    private HashMap<Integer,Integer> childStatus;
    private UThread myThread;
}
