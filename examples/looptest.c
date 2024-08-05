#pragma tenc subsystem cli
#pragma tenc minNTVersion 4
#pragma tenc import("msvcrt.dll", int, "printf", int, ...)

void main() {
    int a = 100;
    int counter = 0;
    while (a) {
        a = a - 1;
        counter = counter + 1;
    }
    if (a) {
        printf("This should never happen\n");
    }
    printf("counter=%d\n", counter);
}
