package ru.deewend.tennessinec;

import ru.deewend.tennessinec.exporter.Exporter;

import java.util.ArrayList;
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
        boolean summation = false;
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

            if (stage == STAGE_EXPECTING_SECOND_OPERAND) {
                exporter.putInstruction("Mov", Triple.of(Helper.MovType.REG_TO_REG_OR_REG_TO_MEM, ModRM.builder()
                        .setMod(ModRM.MOD_REGISTER_TO_REGISTER)
                        .setReg(ModRM.REG_EBX)
                        .setRm(ModRM.REG_EAX)
                        .value(), Helper.SKIP_PARAMETER)); // MOV EBX,EAX
            }
            if (currentToken.equals("(")) {
                List<String> tokensInside = new ArrayList<>();
                int idx = findClosingBraceIdx(theExpressionTokens, tokensInside, 1);
                int tokenCount = idx - 1;

                parseExpression(exporter, tokensInside, false);

                theExpressionTokens.subList(0, tokenCount).clear();
            } else if (TokenizedCode.TokenType.LITERAL_INTEGER.detect(currentToken)) {
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

                    int idx = findClosingBraceIdx(theExpressionTokens, null, 2);
                    for (int i = idx - 1; i >= 2; i--) {
                        int j;
                        List<String> tokensInside = new ArrayList<>();
                        for (j = i; j >= 2; j--) {
                            String token = theExpressionTokens.get(i);
                            if (token.equals(",")) break;

                            tokensInside.add(token);
                        }
                        // the result should be located in the EAX register
                        parseExpression(exporter, tokensInside, false);

                        exporter.putInstruction("PushEAX", Helper.NOTHING);

                        i = j; // it will be decremented later
                    }
                    exporter.putInstruction("CallMethod", currentToken);
                    // the return value of the method will be located in the EAX register
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
                        .setReg(ModRM.REG_EBX)
                        .setRm(ModRM.REG_EAX)
                        .value(), Helper.SKIP_PARAMETER)); // ADD/SUB EBX,EAX

                exporter.putInstruction("Mov", Triple.of(Helper.MovType.REG_TO_REG_OR_REG_TO_MEM, ModRM.builder()
                        .setMod(ModRM.MOD_REGISTER_TO_REGISTER)
                        .setReg(ModRM.REG_EAX)
                        .setRm(ModRM.REG_EBX)
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
