# TennessineC
A small C compiler, written in pure Java, which is able to craft i386 Windows executables.

Currently, supports basic FFI (via `#pragma tenc import` directives), variables, expressions containing braces,
variables, method calls, constants and summation, subtraction operators. The sole data type currently supported is `int`
(32-bit word).

Note that at this moment I don't plan to strictly follow any of currently existing C standards, so this will probably
remain a toy compiler.

## Obtaining a pre-built binary
To be documented.

## Compiling `examples/test.c`

Use the following command: `java -jar TennessineC.jar -s examples/test.c -dp -e WinI386 -o test.exe`
(-dp stands for Debug Preprocessor)

The compiler should craft the `test.exe` file.

## Building
Step 1. Ensure you have JDK 8 (or above) installed;

Step 2. Clone the repository: `git clone https://github.com/minecraft8997/TennessineC`;

Step 3. Import the project into IntelliJ IDEA;

Step 4. Navigate to File -> Project Structure -> Artifacts -> Add -> JAR -> From modules with dependencies...

Step 5. Select the main class `ru.deewend.tennessinec.TennessineC`, press OK;

Step 6. Press Apply;

Step 7. Navigate to Build -> Build Artifacts... -> TennessineC:jar -> Build;

Step 8. Your jarfile will be located in the `out/artifacts/TennessineC_jar` folder.

## What I use for development

I was inspired to start this project after checking out the documentation which fits into a single PNG image –
[PE101](https://github.com/corkami/pics/tree/master/binary/pe101).

Currently, for development I use [Tiny C Compiler](https://bellard.org/tcc/) to compile test snippets and
[Ghidra](https://github.com/NationalSecurityAgency/ghidra) for researching the output. 
