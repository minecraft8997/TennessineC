#pragma tenc subsystem gui
#pragma tenc minNTVersion 4
#pragma tenc import("user32.dll", int, "MessageBoxA", char, int, int, char)

void main() {
    int a = 2;
    int pointer = " Hello World!" + 1;
    MessageBoxA(0, pointer, "[THIS SHOULD NOT APPEAR] TennessineC test" + a + 25 + ((0) - 0), 0);
}
