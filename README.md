# TennessineC
To be documented.

## Building
Step 1. Ensure you have JDK 8 (or above) installed;

Step 2. Clone the repository: `git clone https://github.com/minecraft8997/TennessineC`;

Step 3. Import the project into IntelliJ IDEA;

Step 4. Navigate to File -> Project Structure -> Artifacts -> Add -> JAR -> From modules with dependencies...

Step 5. Select the main class `ru.deewend.tennessinec.TennessineC`, press OK;

Step 6. Press Apply;

Step 7. Navigate to Build -> Build Artifacts... -> TennessineC:jar -> Build;

Step 8. Your jarfile will be located in the `out/artifacts/TennessineC_jar` folder.

## Compiling `examples/test.c`

Use the following command: `java -jar TennessineC.jar -s examples/test.c -dp -e WinI386 -o test.exe` (-dp stands for Debug Preprocessor)

The compiler should craft the `test.exe` file.