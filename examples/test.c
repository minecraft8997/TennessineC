#include "dummy.h"

#pragma tenc subsystem gui
#pragma tenc minNTVersion 4
#pragma tenc import("user32.dll", int, "MessageBoxA", int, int, int, int)

// #pragma tenc bundleFile("./myImage.png", 0) // to be implemented

#define HELLO_WORLD "Hello World!"
#ifndef HELLO_WORLD
#ifdef HELLO_WORLD
#error "This should never happen"
#endif
#endif

void main() {
    int a = 1;
    MessageBoxA(0, HELLO_WORLD, "TennessineC test", 0);
}
