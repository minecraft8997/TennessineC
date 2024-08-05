#include "dummy.h"

#pragma tenc subsystem cli
#pragma tenc minNTVersion 4
#pragma tenc import("msvcrt.dll", int, "printf", int, ...)
#pragma tenc import("user32.dll", int, "MessageBoxA", int, int, int, int)

int fib(int n) {
    if (n - 1) {
        if (n - 2) {
            return fib(n - 1) + fib(n - 2);
        }
    }

    return 1;
}

int test(int a, int b, int c) {
    int unused;

    printf("Received: %d, %d, %d\n", a, b, c);

    return a + (b - c);
}

#define HELLO_WORLD "Hello World!"
#ifndef HELLO_WORLD
#ifdef HELLO_WORLD
#error "This should never happen"
#endif
#endif

int main() {
    /*
     * From assign.c
     */
     int a = 1;
     int b = 2;
     int c = a + b;
     int d = c + a + b + (0x100 + 0b101010 - (1 + 2));
     printf("d=%d\n", d);
     d = 3 + 4;
     printf("new value of \"d\" is %d\n", d);

    /*
     * From fibonacci.c
     */
    printf("11-th Fibonacci number: %d\n", fib(11));

    /*
     * From ExpressionsInMethodParams.c
     */
    a = 2;
    int pointer = " Hello World!" + 1;
    MessageBoxA(0, pointer, "[THIS SHOULD NOT APPEAR] TennessineC test" + a + 23 + ((0) - 0), 0);

    /*
     * From looptest.c
     */
    a = 100;
    int counter = 0;
    while (a) {
        a = a - 1;
        counter = counter + 1;
    }
    if (a) {
        printf("This should never happen\n");
    }
    printf("counter=%d\n", counter);

    {
        int x = 100;
        int y = 50;
    }
    // x and y are not accessible anymore

    /*
     * From multiplefunctions.c
     */
    a = 1;

    printf("1 + (1 + (2 - 3)) = %d\n", a + test(a, 2, 3));

    /*
     * From test.c
     */
    a = 1;
    MessageBoxA(0, HELLO_WORLD, "TennessineC test (testing macros)", 0);
}
