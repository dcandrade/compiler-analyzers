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
        if (eatTerminal("const")) {
            if (eatTerminal("{")) {
                checkDeclaration(true);
                return true;
            }
        }
        return false;
    }

    private boolean checkVariable() throws Exception {
        if (eatTerminal("variables")) {
            if (eatTerminal("{")) {
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

    private boolean checkForType(String type) {
        return currentToken.getType().equals(type);
    }

    private boolean eatTerminal(String tokenValue) {
        if (currentToken.getValue().equals(tokenValue)) {
            this.updateToken();
            return true;
        }
        return false;
    }

    private boolean checkForTerminal(String terminal) {
        return this.currentToken.getValue().equals(terminal);
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

        if (checkForType(TokenTypes.IDENTIFIER)) {
            List<Token> currentVar;
            int line = currentToken.getLine();

            if (isConst) {
                currentVar = this.bufferize("=");
            } else {
                currentVar = this.bufferize(",;");
            }

            String fullIdentifier = currentVar.stream().map(Token::getValue).reduce("", (a, b) -> a + b);
            String varName;
            boolean isVector = fullIdentifier.contains("[");

            if (isVector) {
                varName = fullIdentifier.substring(0, fullIdentifier.indexOf("["));
                String dimensions = fullIdentifier.substring(fullIdentifier.indexOf("["));
                try {
                    currentVariableEntry = new VariableEntry(varName, currentType, isConst, dimensions);
                } catch (NumberFormatException ex) {
                    this.errors.add(new SemanticError(line, "Indexador Inválido", "Número Inteiro", "Dimensão de vetor inválido"));
                    currentVariableEntry = new VariableEntry(varName, currentType, isConst, "");
                }
            } else {
                varName = fullIdentifier;
                currentVariableEntry = new VariableEntry(varName, currentType, isConst);
            }

            this.updateToken();
            System.out.println("Current var: " + currentVariableEntry);

            if (eatTerminal(";")) {
                if (eatTerminal("}")) {
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
                eatTerminal("=");
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
        if (eatTerminal(";")) {
            if (eatTerminal("}")) {
                System.out.println("Finished");
                return true;
            } else {
                checkDeclaration(isConst, true);
            }
        } else if (eatTerminal(",")) {
            //verificar se pode haver uma variável seguida de outra na mesma linha

            checkDeclaration(isConst, false);
        }

        return false;
    }

    private List<Token> bufferize(String sync) {
        int brackets = 0;

        List<Token> buffer = new ArrayList<>();

        while (!sync.contains(this.currentToken.getValue()) || brackets > 0) {
            if (checkForTerminal("["))
                brackets++;
            else if (checkForTerminal("]"))
                brackets--;

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
                    this.errors.add(new SemanticError(token.getLine(), tokenType, lastType, "Erro de conversão"));

                    return TokenTypes.UNDEFINED;
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
            case "boolean":
                return TokenTypes.BOOLEAN;
            default:
                return type;
        }
    }

    private String convertType(Token token, int scope) {
        if (token.isBolean()) {
            return TokenTypes.BOOLEAN;
        }

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

    private void checkAssignment() throws Exception {
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
                        // TODO: exibir warning?
                        this.errors.add(new SemanticError(line, expressionType, currentType, "Aviso de conversão"));
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

        if (currentVariableEntry.isVector()) {


            List<Long> dimensions = new ArrayList<>();
            System.out.println(expressionToken);
            boolean done = false;

            while (!done) {
                expressionToken = expression.stream().map(Token::getValue).reduce("", (a, b) -> a + b);
                System.out.println("Current exp token "+expressionToken);

                long size = 0;

                if (expression.get(0).getValue().equals("[")) {
                    int brackets = 0;

                    for (Token t : expression) {
                        if (t.getValue().equals("[")) {
                            brackets++;
                            if (brackets == 0) {
                                size++;
                            }
                        } else if (t.getValue().equals("]")) {
                            brackets--;
                            if (brackets == 0) {
                                size++;
                            }
                        }

                    }
                    expression.remove(0);
                    expression.remove(expression.size() - 1);


                } else if (expressionToken.contains("]")) {
                    System.out.println("middle : " + expressionToken);
                    int innerDim = 0;
                    int currentSize = 0;

                    for (Token t : expression) {
                        String tokenType = convertType(t, currentScope);
                        if (t.getValue().equals("]")) {
                            if (innerDim == 0) {
                                innerDim = currentSize;
                            } else if (currentSize != innerDim) {
                                System.err.println("erro dimensão");
                            }
                            currentSize = 0;
                        } else if (tokenType.equals(currentVariableEntry.getType())) {
                            currentSize++;
                        }
                    }

                    size = innerDim;
                    done = true;
                } else {
                    expressionToken = expression.stream().map(Token::getValue).reduce("", (a, b) -> a + b);
                    System.out.println("--> size corrido " + expressionToken);
                    size = expression.stream().filter(t -> !t.getType().equals(TokenTypes.DELIMITER)).count();
                    done = true;
                }

                dimensions.add(size);
            }

            dimensions.remove(0);
            System.out.print("Expected dimensions ");
            currentVariableEntry.getDimensions().forEach(x -> System.out.print(x + " "));
            System.out.println("\n");
            System.out.print("Dimensions: ");
            dimensions.forEach(x -> System.out.print(" " + x));
            System.out.println("\n");
        }
        try {
            symbolTable.addConst(currentVariableEntry);
        } catch (Exception e) {
            this.errors.add(new SemanticError(line, currentVariableEntry.getName(), "Identificador novo", "Identificador já utilizado"));
        }
    }

    private boolean checkClass() throws Exception {
        if (eatTerminal("class")) {
            if (eatTerminal(TokenTypes.IDENTIFIER)) {
                if (eatTerminal("{")) {
                    checkVariable();
                    currentClass.addVariableList(currentVariableEntryList);
                    checkMethod();
                } else if (eatTerminal(currentToken.getValue())) {
                    if (checkTokenType(TokenTypes.IDENTIFIER)) {//Verificar se a classe herdade existe
                        if (eatTerminal("{")) {
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
        if (eatTerminal("method")) {
            if (nativeTypes.contains(currentToken.getValue())) {
                checkType(2);
                if (checkTokenType(TokenTypes.IDENTIFIER)) {
                    checkParams();
                    if (eatTerminal("{")) {
                        checkFunctionBody();
                        checkReturn();
                        if (eatTerminal(";")) {
                            if (eatTerminal("}")) {
                                checkMethod();
                            } else {
                                System.err.println("Não podem haver comandos após o return");
                            }
                        }
                        return true;
                    }
                }
            }
        } else if (eatTerminal("}")) {
            checkClass();
        }
        return false;
    }

    private boolean checkFunctionBody() {
        return true;
    }

    private boolean checkReturn() throws Exception {
        if (eatTerminal("return")) {
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
        if (eatTerminal("(")) {
            if (nativeTypes.contains(currentToken.getValue())) {
                updateToken();
                if (checkTokenType(TokenTypes.IDENTIFIER)) {
                    if (eatTerminal(")")) {
                        return true;
                    }
                }
            }
        }

        return true;
    }
}