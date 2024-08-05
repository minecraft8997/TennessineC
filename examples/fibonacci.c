#pragma tenc subsystem cli
#pragma tenc minNTVersion 4
#pragma tenc import("msvcrt.dll", int, "printf", int, ...)

int fib(int n) {
    if (n - 1) {
        if (n - 2) {
            return fib(n - 1) + fib(n - 2);
        }
    }

    return 1;
}

int main() {
    printf("11-th Fibonacci number: %d\n", fib(11));
}
