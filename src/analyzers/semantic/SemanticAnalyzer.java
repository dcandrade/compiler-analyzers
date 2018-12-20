package analyzers.semantic;

import model.error.SemanticError;
import model.semantic.SymbolTable;
import model.semantic.entries.ClassEntry;
import model.semantic.entries.VariableEntry;
import model.token.Token;
import model.token.TokenTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SemanticAnalyzer {
    private static final int RESERVED_WORD_SCOPE = -1;
    private static final int CONST_SCOPE = 0;
    private static final int CLASS_SCOPE = 1;
    private static final int MAIN_SCOPE = 2;

    private final List<Token> tokens;
    private final List<String> nativeTypes;
    private SymbolTable symbolTable;
    private int tokenIndex;
    private Token currentToken, lastToken;
    private VariableEntry currentVariableEntry;
    private ClassEntry currentClass;
    private String currentType, getCurrentTypeMethod;
    private List<VariableEntry> currentVariableEntryList;
    private List<VariableEntry> currentConstantList;
    private int currentScope;
    private List<SemanticError> errors;


    public SemanticAnalyzer(List<Token> tokens) throws Exception {
        currentVariableEntry = new VariableEntry(null, null);
        currentClass = new ClassEntry(null, null);
        currentVariableEntryList = new ArrayList<>();
        currentConstantList = new ArrayList<>();

        symbolTable = new SymbolTable();

        this.tokens = tokens;
        this.tokenIndex = 0;
        this.updateToken();
        this.nativeTypes = Arrays.asList("int", "float", "string", "bool", "void");
        this.currentScope = CONST_SCOPE;
        this.errors = new ArrayList<>();
        analyzer();
    }

    private void updateToken() throws IndexOutOfBoundsException {
        if (this.tokenIndex < this.tokens.size()) {
            lastToken = currentToken;
            this.currentToken = this.tokens.get(this.tokenIndex);
            this.tokenIndex++;
        } else {
            throw new IndexOutOfBoundsException("No more tokens");
        }
    }

    public List<SemanticError> getErrors() {
        return errors;
    }

    private void analyzer() throws Exception {
        checkConst();
        checkClass();
    }

    private boolean checkConst() throws Exception {
        if (checkTokenValue("const")) {
            if (checkTokenValue("{")) {
                checkDeclaration(true);
                return true;
            }
        }
        return false;
    }

    private boolean checkVariable() throws Exception {
        if (checkTokenValue("variables")) {
            if (checkTokenValue("{")) {
                checkDeclaration(false);
                return true;
            }
        }
        return false;
    }


    private boolean checkTokenType(String type) {
        if (currentToken.getType().equals(type)) {
            this.updateToken();
            return true;
        }
        return false;
    }

    private boolean checkTokenValue(String tokenValue) {
        if (currentToken.getValue().equals(tokenValue)) {
            this.updateToken();
            return true;
        }
        return false;
    }


    private boolean checkType(int hierarchy) {
        if (nativeTypes.contains(currentToken.getValue())) {
            if (hierarchy == 1) {
                currentType = currentToken.getValue();
            } else if (hierarchy == 2) {
                getCurrentTypeMethod = currentToken.getValue();
            }

            this.updateToken();
            return true;
        }
        return false;
    }

    private boolean checkDeclaration(boolean isConst) throws Exception {
        return this.checkDeclaration(isConst, true); // Update só é false quando é lista de declarações
    }

    private boolean checkDeclaration(boolean isConst, boolean updateType) throws Exception {

        if (updateType) {
            currentType = convertType(currentToken.getValue());
            this.updateToken();
        }

        if (checkTokenType(TokenTypes.IDENTIFIER)) {
            currentVariableEntry = new VariableEntry(lastToken.getValue(), currentType, isConst);

            System.out.println("Current var: " + currentVariableEntry);

            if (checkTokenValue(";")) {
                if (checkTokenValue("}")) {
                    return true;
                } else {
                    if (isConst) {
                        currentConstantList.add(currentVariableEntry);
                    } else {
                        currentVariableEntryList.add(currentVariableEntry);
                    }
                    checkDeclaration(isConst);
                }

            } else {
                checkTokenValue("=");
                checkAssignment();

                if (currentVariableEntry.isConst()) {
                    currentConstantList.add(currentVariableEntry);

                } else {
                    currentVariableEntryList.add(currentVariableEntry);
                }
            }
        }

        //System.out.println(currentToken);
        //Sair da recursão
        if (checkTokenValue(";")) {
            if (checkTokenValue("}")) {
                System.out.println("Finished");
                return true;
            } else {
                checkDeclaration(isConst, true);
            }
        } else if (checkTokenValue(",")) {
            //verificar se pode haver uma variável seguida de outra na mesma linha

            checkDeclaration(isConst, false);
        }

        return false;
    }

    private List<Token> bufferize(String sync) {
        List<Token> buffer = new ArrayList<>();

        while (!sync.contains(this.currentToken.getValue())) {
            buffer.add(this.currentToken);
            this.updateToken();
        }

        return buffer;
    }

    private String getExpressionType(List<Token> expression, int scope) throws Exception {
        String operators = TokenTypes.DELIMITER + TokenTypes.ARITHMETICAL_OPERATOR + TokenTypes.RELATIONAL_OPERATOR + TokenTypes.LOGICAL_OPERATOR;
        String lastType = null;
        String tokenType;

        for (Token token : expression) {
            tokenType = convertType(token, scope);
            if (!operators.contains(token.getType())) {
                if (lastType == null) {
                    lastType = tokenType;
                } else if (!lastType.equals(tokenType)) {
                    //this.errors.add(new SemanticError(token.getLine(), tokenType, lastType, "Erro de conversão"));
                    //System.out.println("->>> Conversão de "+tokenType+ " para "+lastType +" token "+token);
                }
            } else if (tokenType.equals(TokenTypes.LOGICAL_OPERATOR) || token.getType().equals(TokenTypes.RELATIONAL_OPERATOR)) {
                return TokenTypes.BOOLEAN;
            }
        }

        return lastType;
    }

    private String convertType(String type) {
        switch (type) {
            case "int":
                return TokenTypes.NUMBER_INT;
            case "float":
                return TokenTypes.NUMBER_FLOAT;
            case "string":
                return TokenTypes.STRING;
            default:
                return type;
        }
    }

    private String convertType(Token token, int scope) {
        if (scope == CONST_SCOPE) {
            if (token.getType().equals(TokenTypes.IDENTIFIER)) {
                String type = this.symbolTable.getConstType(token.getValue());

                if (type == null) {
                    type = TokenTypes.UNDEFINED;
                    this.errors.add(new SemanticError(token.getLine(), token.getValue(), "Valor ou Identificador válido", "Constante indefinida"));
                }

                return type;
            } else if (token.getType().equals(TokenTypes.NUMBER)) {
                if (token.getValue().contains("."))
                    return TokenTypes.NUMBER_FLOAT;
                else
                    return TokenTypes.NUMBER_INT;
            }

            return token.getType();
        }

        return null; //TODO: resto
    }

    private boolean checkAssignment() throws Exception {
        List<Token> expression = this.bufferize(",;");
        int line = expression.get(0).getLine();
        String expressionToken = expression.stream().map(Token::getValue).reduce("", (a, b) -> a + b);

        String expressionType = getExpressionType(expression, currentScope);

        if (!expressionType.equals(TokenTypes.UNDEFINED)) {
            switch (currentType) {
                case TokenTypes.STRING:
                    if (!expressionType.equals(TokenTypes.STRING)) {
                        //System.err.println("Erro: Variável CDC atribuída como " + expressionType);
                        this.errors.add(new SemanticError(line, expressionType, currentType, "Erro de conversão"));
                    }
                    break;

                case TokenTypes.BOOLEAN:
                    if (!expressionType.equals(TokenTypes.BOOLEAN)) {
                        //System.err.println("Erro: Variável booleana atribuída como " + expressionType);
                        this.errors.add(new SemanticError(line, expressionType, currentType, "Erro de conversão"));
                    }
                    break;
                case TokenTypes.NUMBER_INT:
                    if (!expressionType.equals(TokenTypes.NUMBER_INT)) {
                        //System.err.println("Erro: Variável float atribuída como " + expressionType);
                        this.errors.add(new SemanticError(line, expressionType, currentType, "Erro de conversão"));
                    }
                    break;
                case TokenTypes.NUMBER_FLOAT:
                    if (!expressionType.equals(TokenTypes.NUMBER_FLOAT)) {
                        //System.err.println("Erro: Variável float atribuída como " + expressionType);
                        this.errors.add(new SemanticError(line, expressionType, currentType, "Erro de conversão"));

                    }
                    break;
                default:
                    // System.out.println("Erro: Instâncias de classe não podem ser constantes " + expressionType);
                    this.errors.add(new SemanticError(line, expressionType, currentType, "Erro de conversão"));
                    break;
            }
        }

        symbolTable.addConst(currentVariableEntry);

        return true;
    }

    private boolean checkClass() throws Exception {
        if (checkTokenValue("class")) {
            if (checkTokenValue(TokenTypes.IDENTIFIER)) {
                if (checkTokenValue("{")) {
                    checkVariable();
                    currentClass.addVariableList(currentVariableEntryList);
                    checkMethod();
                } else if (checkTokenValue(currentToken.getValue())) {
                    if (checkTokenType(TokenTypes.IDENTIFIER)) {//Verificar se a classe herdade existe
                        if (checkTokenValue("{")) {
                            checkVariable();
                            currentClass.addVariableList(currentVariableEntryList);
                            checkMethod();
                        }
                    }
                }
            }
        } else {
            checkMain();
        }

        return true;
    }

    private boolean checkMethod() throws Exception {
        if (checkTokenValue("method")) {
            if (nativeTypes.contains(currentToken.getValue())) {
                checkType(2);
                if (checkTokenType(TokenTypes.IDENTIFIER)) {
                    checkParams();
                    if (checkTokenValue("{")) {
                        checkFunctionBody();
                        checkReturn();
                        if (checkTokenValue(";")) {
                            if (checkTokenValue("}")) {
                                checkMethod();
                            } else {
                                System.err.println("Não podem haver comandos após o return");
                            }
                        }
                        return true;
                    }
                }
            }
        } else if (checkTokenValue("}")) {
            checkClass();
        }
        return false;
    }

    private boolean checkFunctionBody() {
        return true;
    }

    private boolean checkReturn() throws Exception {
        if (checkTokenValue("return")) {
            System.out.println("valor do return para análise: " + currentToken.getValue());
            if (currentClass.checkVariableType(currentToken.getValue(), getCurrentTypeMethod)) {
                updateToken();
                System.out.println("Return correto");
                return true;
            } else {
                System.out.println("Valor de retorn não é o mesmo do tipo da função");
            }
        }
        return false;
    }

    private boolean checkMain() {
        return true;
    }

    private boolean checkParams() {
        if (checkTokenValue("(")) {
            if (nativeTypes.contains(currentToken.getValue())) {
                updateToken();
                if (checkTokenType(TokenTypes.IDENTIFIER)) {
                    if (checkTokenValue(")")) {
                        return true;
                    }
                }
            }
        }

        return true;
    }
}