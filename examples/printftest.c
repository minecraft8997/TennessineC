#pragma tenc subsystem cli
#pragma tenc minNTVersion 4
#pragma tenc import("msvcrt.dll", int, "printf", int, ...)

void main() {
    printf("Hello ");
    printf("world\n");
}
