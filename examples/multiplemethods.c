#pragma tenc subsystem cli
#pragma tenc minNTVersion 4
#pragma tenc import("msvcrt.dll", int, "printf", int, ...)

int test(int a, int b, int c) {
    int unused;

    printf("Received: %d, %d, %d\n", a, b, c);

    return a + (b - c);
}

void main() {
    int a = 1;
    int result = test(a + 1 - 1, 2, 3);

    // wrong results here :(
    printf("(%d, %d) but 1 + (1 + (2 - 3)) = %d or %d\n", result, a, a + test(a + 1 - 1, 2, 3), a + result);
}
