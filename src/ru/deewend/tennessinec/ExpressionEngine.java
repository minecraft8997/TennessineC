package ru.deewend.tennessinec;

import ru.deewend.tennessinec.exporter.Exporter;

import java.util.ArrayList;
import java.util.List;

public class ExpressionEngine {
    public interface InstructionEncoder {
        void encode(int firstRegister, int secondRegister);
    }

    public enum OperationType {
        SUMMATION((firstRegister, secondRegister) -> { throw new UnsupportedOperationException(); }),
        SUBTRACTION((firstRegister, secondRegister) -> { throw new UnsupportedOperationException(); });

        private final InstructionEncoder encoder;

        OperationType(InstructionEncoder encoder) {
            this.encoder = encoder;
        }

        public void encode(int firstRegister, int secondRegister) {
            encoder.encode(firstRegister, secondRegister);
        }
    }

    public interface ValueGenerator {
        int value(Exporter exporter, String unparsedValue);
    }

    public enum OperandType {
        REGISTER((exporter, unparsedValue) -> {
            return 0; // todo fixme
        }),
        // VARIABLE_NAME,
        CONSTANT((exporter, unparsedValue) -> Integer.parseInt(unparsedValue));

        private final ValueGenerator valueGenerator;

        OperandType(ValueGenerator valueGenerator) {
            this.valueGenerator = valueGenerator;
        }

        public int value(Exporter exporter, String unparsedValue) {
            return valueGenerator.value(exporter, unparsedValue);
        }
    }

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
        while (!theExpressionTokens.isEmpty()) {
            String currentToken = theExpressionTokens.get(0);
            if (stage == STAGE_EXPECTING_OPERATOR) {

            } else { // expecting either first or second operand
                if (currentToken.equals("(")) {
                    int stack = 1;
                    List<String> tokensInside = new ArrayList<>();
                    for (int i = 1; i < theExpressionTokens.size(); i++) {
                        String nextToken = theExpressionTokens.get(i);
                        tokensInside.add(nextToken);
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
                    int tokenCount = tokensInside.size();
                    // probably a subject to removal; checking the brace just to be sure
                    int idx = tokenCount - 1;
                    if (!tokensInside.get(idx).equals(")")) {
                        throw new IllegalArgumentException("Invalid expression: " +
                                "expected a closing brace as the last token");
                    }
                    // removing the last brace from the temporary list is required though
                    tokensInside.remove(idx);

                    parseExpression(exporter, tokensInside, false);

                    theExpressionTokens.subList(0, tokenCount).clear();
                } else if (TokenizedCode.TokenType.LITERAL_INTEGER.detect(currentToken)) {

                }
            }


            stage++;
            if (stage == STAGE_EXPECTING_SECOND_OPERAND) {
                // encodeSimpleExpression(summation, ); // todo fixme
            }
        }
    }

    /*
     * Accepts an expression against EAX register that contains one operand and one operation.
     * For instance, EAX+
     * At this time, the operation can be either summation or subtraction.
     * Each operand can be either:
     * 1) EAX, EBX, ECX, EDX register;
     * 2) Variable name;
     * 3) Constant value.
     *
     * Moves the result to EAX register.
     */
    private static void encodeSimpleExpression(
            boolean summation, String secondOperand, OperandType secondOperandType
    ) {

    }
}
