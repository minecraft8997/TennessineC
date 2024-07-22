#pragma tenc subsystem cli
#pragma tenc minNTVersion 4

void main() {
    int a = 1;
    int b = 2;
    int c = a + b;
    int d = c + a + b + (0x100 + 0b101010 - (1 + 2));
}
