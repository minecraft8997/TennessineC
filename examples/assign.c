#pragma tenc subsystem cli
#pragma tenc minNTVersion 4
#pragma tenc import("msvcrt.dll", int, "printf", int, ...)

void main() {
    int a = 1;
    int b = 2;
    int c = a + b;
    int d = c + a + b + (0x100 + 0b101010 - (1 + 2));
    printf("d=%d\n", d);
    d = 3 + 4;
    printf("new value of \"d\" is %d\n", d);
}
