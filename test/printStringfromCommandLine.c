#include "syscall.h"
#include "stdio.h"
int main(int argc, char **argv)
{
    int i;
    for(i=0;i<10;i++)
    {
        printf("%s\n",argv[0]);
    }
}