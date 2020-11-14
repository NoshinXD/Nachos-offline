#include "syscall.h"
#include "stdio.h"

int main()
{
    printf("testing valid read,write,exec,join and invalid halt system call by creating a child process that prints, takes input, tries to call halt. Then we are  joining to this process\n");
    int pid = exec("echo.coff",0,0);
    int status1;
    if(pid==-1)
    {
        printf("process creation failed!\n");
        exit(-3);
    }
    int joinStatus = join(pid,&status1);
    if(joinStatus == 1)
    {
        printf("Joined successfully with child exit status %d\n",status1);

    }
    else
    {
        printf("Join failed of a valid subprocess\n");
        exit(-3);
    }

    printf("Now making a invalid join call by joining to previous process which is already finished\n");

    joinStatus = join(pid,&status1);
    if(joinStatus == 1 || joinStatus == 0)
    {
        printf("wrong join. It was not suppose to join\n");
        exit(-3);
    }
    else if(joinStatus == -1)
    {
        printf("couldn't join so our test was successfull\n");
    }

    printf("creating 2 child thread for concurrent userprocess simulation\n");
    char *args[1];
    args[0] = "hey from process 1";
    int pid1 = exec("printStringfromCommandLine.coff",1,args);
    args[0] = "hello from process 2";
    int pid2 = exec("printStringfromCommandLine.coff",1,args);

    if(pid1==-1 || pid2==-1)
    {
        printf("Process creation failed");
        exit(-3);
    }
    int joinStatus1 = join(pid1,&status1);
    int joinStatus2 = join(pid2,&status1);

    if(joinStatus1 == 1 && joinStatus2 == 1)
    {
        printf("Joined successfully from 2 process\n",status1);

    }
    else
    {
        printf("Join failed of a valid subprocess\n");
        exit(-3);
    }


    printf("finally testing valid halt call");
    halt();


}