package analyzers.semantic;

import model.error.SemanticError;
import model.semantic.SymbolTable;
import model.semantic.entries.ClassEntry;
import model.semantic.entries.VariableEntry;
import model.token.Token;
import model.token.TokenTypes;

import java.util.*;
import java.util.stream.Collectors;

public class SemanticAnalyzer {
    private static final int RESERVED_WORD_SCOPE = -1;
    private static final int CONST_SCOPE = 0;
    private static final int CLASS_SCOPE = 1;
    private static final int MAIN_SCOPE = 2;

    private final List<Token> tokens;
    private SymbolTable symbolTable;
    private int tokenIndex;
    private Token currentToken;
    private VariableEntry currentVariableEntry;
    private ClassEntry currentClass;
    private String currentType, getCurrentTypeMethod;
    private int currentScope;
    private List<SemanticError> errors;


    public SemanticAnalyzer(List<Token> tokens) throws Exception {
        currentVariableEntry = new VariableEntry(null, null, -1);
        currentClass = new ClassEntry(null, null);

        symbolTable = new SymbolTable();

        this.tokens = tokens;
        this.tokenIndex = 0;
        this.updateToken();
        this.currentScope = CONST_SCOPE;
        this.errors = new ArrayList<>();
        analyzer();
    }

    private void updateToken() throws IndexOutOfBoundsException {
        if (this.tokenIndex < this.tokens.size()) {
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

    private void checkConst() {
        if (eatTerminal("const")) {
            if (eatTerminal("{")) {
                checkDeclaration(true, true, this.symbolTable.getConstContext());
            }
        }
    }

    private void checkVariable(ClassEntry context) {
        if (eatTerminal("variables")) {
            if (eatTerminal("{")) {
                checkDeclaration(false, true, context.getVariables());
            }
        }
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

    private void checkType(int hierarchy) {
        if (TokenTypes.nativeTypes.contains(currentToken.getValue())) {
            if (hierarchy == 1) {
                currentType = currentToken.getValue();
            } else if (hierarchy == 2) {
                getCurrentTypeMethod = currentToken.getValue();
            }

            this.updateToken();
        }
    }

    private void checkDeclaration(boolean isConst, boolean updateType, Map<String, VariableEntry> context) {
        if (updateType) {
            currentType = TokenTypes.convertType(currentToken.getValue());
            this.updateToken();
        }

        if (checkForType(TokenTypes.IDENTIFIER)) {
            List<Token> currentVar;
            int line = currentToken.getLine();

            if (isConst) {
                currentVar = this.bufferize("=");
            } else {
                currentVar = this.bufferize("=,;");
            }


            String fullIdentifier = currentVar.stream().map(Token::getValue).reduce("", (a, b) -> a + b);
            String varName;
            boolean isVector = fullIdentifier.contains("[");

            if (isVector) {
                varName = fullIdentifier.substring(0, fullIdentifier.indexOf("["));
                String dimensions = fullIdentifier.substring(fullIdentifier.indexOf("["));
                try {
                    List<Integer> dims = Arrays.stream(dimensions.replace("[", " ")
                            .replace("]", " ")
                            .split(" "))
                            .filter(s -> !s.isEmpty())
                            .map(this::translateConst)
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());

                    currentVariableEntry = new VariableEntry(varName, currentType, isConst, dims, line);
                } catch (NumberFormatException ex) {
                    this.errors.add(new SemanticError(line, "Indexador Inválido", "Número/identificador Inteiro", "Dimensão de vetor inválida"));
                    currentVariableEntry = new VariableEntry(varName, currentType, isConst, Collections.EMPTY_LIST, line);
                }
            } else {
                varName = fullIdentifier;
                currentVariableEntry = new VariableEntry(varName, currentType, isConst, line);
            }

            //this.currentVariableEntryList.add(currentVariableEntry);
            if (!isConst && this.symbolTable.isConst(currentVariableEntry)) {
                this.errors.add(new SemanticError(currentVariableEntry.getLine(), currentVariableEntry.getName(), "Identificador novo", "Identificador já utilizado como constante"));

            } else if (context.get(currentVariableEntry.getName()) != null) {
                this.errors.add(new SemanticError(currentVariableEntry.getLine(), currentVariableEntry.getName(), "Identificador novo", "Identificador já utilizado"));
            } else {
                context.put(currentVariableEntry.getName(), currentVariableEntry);
            }

            //this.updateToken();
            System.out.println("Current var: " + currentVariableEntry);

            if (eatTerminal(";")) {
                if (eatTerminal("}")) {
                    return;
                } else {
                    checkDeclaration(isConst, true, context);
                }

            } else if (eatTerminal("=")) {
                checkAssignment();
            }
        }

        //System.out.println(currentToken);
        //Sair da recursão
        if (eatTerminal(";")) {
            if (eatTerminal("}")) {
                System.out.println("Finished");
            } else {
                checkDeclaration(isConst, true, context);
            }
        } else if (eatTerminal(",")) {
            //verificar se pode haver uma variável seguida de outra na mesma linha
            checkDeclaration(isConst, false, context);
        }

    }

    private String translateConst(String varName) {
        VariableEntry var = this.symbolTable.getConst(varName);

        if (var == null) {
            return varName;
        } else
            return var.getValue();
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

    private String getExpressionType(List<Token> expression, int scope) {
        String operators = TokenTypes.DELIMITER + TokenTypes.ARITHMETICAL_OPERATOR + TokenTypes.RELATIONAL_OPERATOR + TokenTypes.LOGICAL_OPERATOR;
        String lastType = null;
        String tokenType;

        for (Token token : expression) {
            tokenType = convertType(token, scope);
            if (!operators.contains(token.getType())) {
                if (lastType == null) {
                    lastType = tokenType;
                } else if (!lastType.equals(tokenType)) {
                    // Conversão de tipos dentro de uma expressão, logo o tipo da expressão é indefinido
                    this.errors.add(new SemanticError(token.getLine(), tokenType, lastType, "Erro de conversão"));

                    return TokenTypes.UNDEFINED;
                }
            } else if (tokenType.equals(TokenTypes.LOGICAL_OPERATOR) || token.getType().equals(TokenTypes.RELATIONAL_OPERATOR)) {
                return TokenTypes.BOOLEAN;
            }
        }

        return lastType;
    }

    private String convertType(Token token, int scope) {
        return convertType(token, scope, true);
    }

    private String convertType(Token token, int scope, boolean error) {
        if (token.isBolean()) {
            return TokenTypes.BOOLEAN;
        }

        if (token.getType().equals(TokenTypes.NUMBER)) {
            if (token.getValue().contains("."))
                return TokenTypes.NUMBER_FLOAT;
            else
                return TokenTypes.NUMBER_INT;
        }

        if (token.getType().equals(TokenTypes.IDENTIFIER)) {

            if (scope == CONST_SCOPE) {
                String type = this.symbolTable.getConstType(token.getValue());

                if (type == null) {
                    type = TokenTypes.UNDEFINED;
                    if (error)
                        this.errors.add(new SemanticError(token.getLine(), token.getValue(), "Valor ou Identificador válido", "Constante indefinida"));
                }

                return type;

            }

            if (scope == CLASS_SCOPE) {
                String constType = convertType(token, CONST_SCOPE, false);
                String type;

                if (constType == null) {
                    ClassEntry classEntry = this.symbolTable.checkClassVariable(token.getValue());

                    if (classEntry != null) {
                        type = classEntry.getVariableType(token.getValue());
                        if (type == null) {
                            type = TokenTypes.UNDEFINED;
                            if (error)
                                this.errors.add(new SemanticError(token.getLine(), token.getValue(), "Valor ou Identificador válido", "Constante indefinida"));
                        }
                        return type;
                    }
                }

            }
        }


        return token.getType(); // TODO: remaining scopes
    }

    private void checkAssignment() {
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
            List<Integer> dimensions = new ArrayList<>();
            boolean done = false;
            boolean error = false;

            while (!done) {
                expressionToken = expression.stream().map(Token::getValue).reduce("", (a, b) -> a + b);

                int size = 0;

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
                    int innerDim = 0;
                    int currentSize = 0;

                    for (Token t : expression) {
                        String tokenType = convertType(t, currentScope);

                        if (t.getValue().equals("]")) {
                            if (innerDim == 0) {
                                innerDim = currentSize;
                            } else if (currentSize != innerDim) {
                                error = true;
                                this.errors.add(new SemanticError(line, currentVariableEntry.getName(), "Vetor de dimensões corretas", "Vetor com dimensões diferentes da declarada"));
                            }

                            currentSize = 0;
                        } else if (tokenType.equals(currentVariableEntry.getType())) {
                            currentSize++;
                        }
                    }

                    if (currentSize != innerDim) {
                        error = true;
                        this.errors.add(new SemanticError(line, currentVariableEntry.getName(), "Vetor de dimensões corretas", "Vetor com dimensões diferentes da declarada"));
                    }

                    size = innerDim;
                    done = true;
                } else {
                    size = (int) expression.stream().filter(t -> !t.getType().equals(TokenTypes.DELIMITER)).count();
                    done = true;
                }

                if (error) {
                    break;
                }

                dimensions.add(size);
            }

            if (!error) {
                dimensions.remove(0);

                boolean pass = true;

                if (dimensions.size() == this.currentVariableEntry.getDimensions().size()) {
                    for (int i = 0; i < dimensions.size(); i++) {
                        if (!dimensions.get(i).equals(this.currentVariableEntry.getDimensions().get(i))) {
                            pass = false;
                            break;
                        }
                    }
                } else {
                    pass = false;
                }

                if (!pass) {
                    this.errors.add(new SemanticError(line, currentVariableEntry.getName(), "Vetor de dimensões corretas", "Vetor com dimensões diferentes da declarada"));
                }
            }

        }
    }

    private void checkClass() throws Exception {
        if (eatTerminal("class")) {
            this.currentScope = CLASS_SCOPE;

            String className, superclassName = null;

            className = currentToken.getValue();
            int line = currentToken.getLine();
            updateToken();

            if (checkForTerminal("extends")) {
                updateToken();
                superclassName = currentToken.getValue();
                updateToken();
            }

            // Não pode herdar de si mesma
            if (superclassName != null && className.equals(superclassName)) {
                this.errors.add(new SemanticError(line, className, "Classe mãe diferente da classe filha", "Classe herdando de si mesma"));
                superclassName = null;
            }

            ClassEntry classEntry = this.symbolTable.addClass(className, superclassName);

            eatTerminal("{");

            checkVariable(classEntry);
            //checkMethod();
            eatTerminal("}"); // Class end
            checkClass();

        } else {
            checkMain();
        }

    }

    private void checkMethod() throws Exception {
        if (eatTerminal("method")) {
            String returnType = this.currentToken.getValue();
            updateToken();
            String name = this.currentToken.getValue();
            updateToken();
            System.out.println("--- Metodo "+ name + "   retorno "+returnType);
            eatTerminal("(");
            List<Token> params = bufferize(")");
            System.out.println(" --- Parametros "+ params);
            eatTerminal(")");
            eatTerminal("{");
            System.out.println("--->>>>" + this.currentToken);

        }
    }

    private void checkFunctionBody() {
    }

    private void checkReturn() throws Exception {
        if (eatTerminal("return")) {
            System.out.println("valor do return para análise: " + currentToken.getValue());
            if (currentClass.checkVariableType(currentToken.getValue(), getCurrentTypeMethod)) {
                updateToken();
                System.out.println("Return correto");
            } else {
                System.out.println("Valor de retorn não é o mesmo do tipo da função");
            }
        }
    }

    private void checkMain() {
    }

    private void checkParams() {
        if (eatTerminal("(")) {
            if (TokenTypes.nativeTypes.contains(currentToken.getValue())) {
                updateToken();
                if (checkTokenType(TokenTypes.IDENTIFIER)) {
                    if (eatTerminal(")")) {
                    }
                }
            }
        }

    }
}