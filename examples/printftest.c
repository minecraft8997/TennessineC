#pragma tenc subsystem cli
#pragma tenc minNTVersion 4
#pragma tenc import("msvcrt.dll", void, "printf", pointer, ...)

void main() {
    printf("Hello ");
    printf("world\n");
}
