package ru.deewend.tennessinec;

import ru.deewend.tennessinec.exporter.Exporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExpressionEngine {
    private static final byte STAGE_EXPECTING_OPERATOR = 1;
    private static final byte STAGE_EXPECTING_SECOND_OPERAND = 2;

    private ExpressionEngine() {
    }

    /*
     * Generates instructions calculating the given expression value.
     * Moves the result to EAX register.
     */
    public static void parseExpression(
            Exporter exporter, List<String> theExpressionTokens, boolean shouldInstantiateNewList
    ) {
        if (theExpressionTokens.isEmpty()) {
            throw new IllegalArgumentException("Empty expression");
        }

        if (shouldInstantiateNewList) {
            theExpressionTokens = new ArrayList<>(theExpressionTokens);
        }

        byte stage = 0;
        boolean summation = true;
        boolean shouldMovEBXEAX = true;
        boolean canExitLoop = true;
        while (!theExpressionTokens.isEmpty()) {
            try {

            String currentToken = theExpressionTokens.get(0);
            if (stage == STAGE_EXPECTING_OPERATOR) {
                stage++;
                summation = currentToken.equals("+");
                canExitLoop = false;

                continue;
            }
            // expecting either first or second operand

            if (stage == STAGE_EXPECTING_SECOND_OPERAND && shouldMovEBXEAX) {
                /*
                 * TODO Something is wrong here.
                 *
                 * ModRM class was designed in a way that if in the context of a Mov instruction you put
                 * .setMod(ModRM.MOD_REGISTER_TO_REGISTER)
                 * .setReg(REG1)
                 * .setRm(REG2)
                 *
                 * it means you would like to perform MOV REG1,REG2 operation. It works as intended for example
                 * in I386DefineMethod Instruction (where we perform MOV EBP,ESP), however, to perform
                 * ADD/SUB/MOV EBX,EAX (and vice-versa) we have to put the registers in a different order.
                 * Should probably research the reason eventually.
                 */
                exporter.putInstruction("Mov", Triple.of(Helper.MovType.REG_TO_REG_OR_REG_TO_MEM, ModRM.builder()
                        .setMod(ModRM.MOD_REGISTER_TO_REGISTER)
                        .setReg(ModRM.REG_EAX)
                        .setRm(ModRM.REG_EBX)
                        .value(), Helper.SKIP_PARAMETER)); // MOV EBX,EAX

                shouldMovEBXEAX = false;
            }
            while (currentToken.equals("(")) {
                revealBraces(theExpressionTokens, summation, 1);

                theExpressionTokens.remove(0);
                if (theExpressionTokens.isEmpty()) {
                    throw new IllegalArgumentException("Encountered an empty expression");
                }
                currentToken = theExpressionTokens.get(0);
            }

            if (TokenizedCode.TokenType.LITERAL_INTEGER.detect(currentToken)) {
                int base = 10;
                if (currentToken.startsWith("0x")) {
                    base = 16;
                } else if (currentToken.startsWith("0b")) {
                    base = 2;
                }
                if (base != 10) { // removing notation
                    currentToken = currentToken.substring(2);
                }
                exporter.putInstruction("MovEAX", Integer.parseInt(currentToken, base));
            } else if (TokenizedCode.TokenType.SYMBOL.detect(currentToken)) {
                if (theExpressionTokens.size() > 1 && theExpressionTokens.get(1).equals("(")) {
                    // this is a method call
                    Pair<Integer, Integer> result =
                            parseMethodParameters(exporter, theExpressionTokens, 2);

                    int idx = result.getFirst();
                    int parameterCount = result.getSecond();

                    TMethod.putCallMethod(exporter, currentToken, parameterCount);
                    // the return value of the method will be located in the EAX register

                    theExpressionTokens.subList(1, idx + 1).clear();
                } else {
                    // this is a variable name

                    VariableData data = Scope.findVariable(currentToken);
                    Helper.moveFromMemToEAX(exporter, data);
                }
            } else {
                throw new IllegalArgumentException("Invalid expression: unexpected token: " + currentToken);
            }

            if (stage == STAGE_EXPECTING_SECOND_OPERAND) {
                exporter.putInstruction(summation ? "Add" : "Sub", Pair.of(ModRM.builder()
                        .setMod(ModRM.MOD_REGISTER_TO_REGISTER)
                        .setReg(ModRM.REG_EAX)
                        .setRm(ModRM.REG_EBX)
                        .value(), Helper.SKIP_PARAMETER)); // ADD/SUB EBX,EAX

                exporter.putInstruction("Mov", Triple.of(Helper.MovType.REG_TO_REG_OR_REG_TO_MEM, ModRM.builder()
                        .setMod(ModRM.MOD_REGISTER_TO_REGISTER)
                        .setReg(ModRM.REG_EBX)
                        .setRm(ModRM.REG_EAX)
                        .value(), Helper.SKIP_PARAMETER)); // MOV EAX,EBX
                // (since we're required to store the result in the EAX register)

                stage = STAGE_EXPECTING_OPERATOR - 1; // it will be incremented immediately
                canExitLoop = true;
            }
            stage++;

            } finally {
                theExpressionTokens.remove(0);
            }
        }
        if (!canExitLoop) {
            throw new IllegalArgumentException("Invalid expression: expected an operand");
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static void revealBraces(List<String> theExpressionTokens, boolean summation, int startingFrom) {
        int stack = 1;
        for (int i = startingFrom; i < theExpressionTokens.size(); i++) {
            String currentToken = theExpressionTokens.get(i);
            if (currentToken.equals("(")) {
                stack++;
            } else if (currentToken.equals(")")) {
                stack--;
                if (stack == 0) {
                    theExpressionTokens.remove(i);

                    return;
                }
            }
            if (stack != 1) continue;

            if (summation) continue;

            if (currentToken.equals("+")) {
                theExpressionTokens.set(i, "-");
            } else if (currentToken.equals("-")) {
                theExpressionTokens.set(i, "+");
            }
        }
        if (stack != 0) {
            throw new IllegalArgumentException("Invalid expression: expected " + stack + " closing brace(s)");
        }
    }

    /*
     * Treats each method parameter as an expression and attempts to parse it.
     * After the parsing is done, pushes the value of EAX register for each parameter (from right to left).
     *
     * The method assumes that theExpressionTokens size is greater than or equal to "startingFrom",
     * theExpressionTokens.get(startingFrom - 2) is the function name (if presented) and that
     * theExpressionTokens.get(startingFrom - 1) is "(" -- an opening brace (if presented).
     */
    public static Pair<Integer, Integer> parseMethodParameters(
            Exporter exporter, List<String> theExpressionTokens, int startingFrom
    ) {
        int parameterCount = 0;
        int idx = findClosingBraceIdx(theExpressionTokens, null, startingFrom);
        for (int i = idx - 1; i >= startingFrom; i--) {
            int j;
            int stack = 0;
            List<String> tokensInside = new ArrayList<>();
            for (j = i; j >= startingFrom; j--) {
                String token = theExpressionTokens.get(j);
                if (token.equals(")")) {
                    stack++;
                } else if (token.equals("(")) {
                    stack--;
                    if (stack < 0) throw new IllegalArgumentException("Invalid expression: unexpected opening brace");
                }
                if (token.equals(",") && stack == 0) break;

                tokensInside.add(token);
            }
            if (stack != 0) {
                throw new IllegalArgumentException("Invalid expression: expected " + stack + " opening brace(s)");
            }
            Collections.reverse(tokensInside);

            // the result should be located in the EAX register
            parseExpression(exporter, tokensInside, false);

            exporter.putInstruction("PushEAX", Helper.NOTHING);
            parameterCount++;

            i = j; // it will be decremented later
        }

        return Pair.of(idx, parameterCount);
    }

    @SuppressWarnings("SameParameterValue")
    private static int findClosingBraceIdx(
            List<String> theExpressionTokens, List<String> tokensInside, int startingFrom
    ) {
        int i;
        int stack = 1;
        for (i = startingFrom; i < theExpressionTokens.size(); i++) {
            String nextToken = theExpressionTokens.get(i);
            if (tokensInside != null) tokensInside.add(nextToken);
            if (nextToken.equals("(")) {
                stack++;
            } else if (nextToken.equals(")")) {
                stack--;
                if (stack < 0) {
                    throw new IllegalArgumentException("Invalid expression: " +
                            "encountered a redundant closing brace");
                }
                if (stack == 0) break;
            }
        }
        if (stack != 0) {
            throw new IllegalArgumentException("Invalid expression: expected " +
                    stack + " closing brace(s)");
        }
        if (tokensInside != null) {
            // probably a subject to removal; checking the brace just to be sure
            int idx = tokensInside.size() - 1;
            if (!tokensInside.get(idx).equals(")")) {
                throw new IllegalArgumentException("Invalid expression: " +
                        "expected a closing brace as the last token");
            }
            // removing the last brace from the temporary list is required though
            tokensInside.remove(idx);
        }

        return i;
    }
}
