/* fibonacci.c
 *	prints nth fibonacci number
 */

#include "syscall.h"
#include "stdio.h"

int stringToInt(char *s) {
    int ans = 0;
    while (*s != 0) {
        ans = ans * 10 + (*s-'0');
        s++;
    }
    return ans;
}

char a[100], b[100];

void intToString (int x, char * s) {
    int sz = 0;
    while (x > 0) {
        int d = x%10;
        x/=10;
        s[sz] = d+'0';
        sz++;
    }

    s[sz] = 0;
    int i, j;
    for (i=0, j=sz-1; i<j; i++, j--) {
        char temp = s[i];
        s[i] = s[j];
        s[j] = temp;
    }
}

char * args[2];

void outOfMemory() {
    printf("Machine ran out of memory\n");
    exit(-1);
}

int main(int argc, char **argv)
{
    int n, pid, A, B;
    n = stringToInt(argv[1]);

    if (n == 0)     return 1;
    if (n == 1)     return 2;

    intToString(n-2, a);
    intToString(n-1, b);

    args[0] = "fibonacci_cmd.coff";
    args[1] = a;
    pid = exec("fibonacci_cmd.coff", 2, args);
    if (pid == -1) outOfMemory();
    join(pid, &A);
    if (A == -1)   outOfMemory();

    args[0] = "fibonacci_cmd.coff";
    args[1] = b;
    pid = exec("fibonacci_cmd.coff", 2, args);
    if (pid == -1)  outOfMemory();
    join(pid, &B);
    if (B == -1)    outOfMemory();

    printf("fibonacci(%d) = %d\n", n, A+B);
    return A+B;
}
