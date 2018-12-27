package analyzers.semantic;

import model.error.SemanticError;
import model.semantic.SymbolTable;
import model.semantic.entries.ClassEntry;
import model.semantic.entries.MethodEntry;
import model.semantic.entries.VariableEntry;
import model.token.Token;
import model.token.TokenTypes;

import java.util.*;
import java.util.stream.Collectors;

public class SemanticAnalyzer {
    private final List<Token> tokens;
    private SymbolTable symbolTable;
    private int tokenIndex;
    private Token currentToken;
    private VariableEntry currentVariableEntry;
    private String currentType;
    private List<SemanticError> errors;


    public SemanticAnalyzer(List<Token> tokens) throws Exception {
        currentVariableEntry = new VariableEntry(null, null, -1);

        symbolTable = new SymbolTable();

        this.tokens = tokens;
        this.tokenIndex = 0;
        this.updateToken();
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
        checkMain();
    }

    private void checkConst() {
        if (eatTerminal("const")) {
            if (eatTerminal("{")) {
                checkDeclaration(true, this.symbolTable.getConstContext());
            }
        }
    }

    private void checkVariable(ClassEntry context) {
        if (eatTerminal("variables")) {
            if (eatTerminal("{")) {
                Map<String, VariableEntry> fullContext = context.getVariables();
                fullContext.putAll(this.symbolTable.getConstContext());
                checkDeclaration(false, fullContext);
            }
        }
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

    private void checkDeclaration(boolean isConst, Map<String, VariableEntry> context) {
        checkDeclaration(isConst, true, context, false);
    }

    private void checkDeclaration(boolean isConst, boolean updateType, Map<String, VariableEntry> context, boolean isParam) {
        if (updateType && !checkForTerminal(")")) {
            currentType = TokenTypes.convertType(currentToken.getValue());
            if(!this.symbolTable.isValidType(currentType)){
                this.errors.add(new SemanticError(this.currentToken.getLine(), currentType, "Classe válida ou tipo nativo", "Tipo de variável inválida"));
            }
            this.updateToken();
        }

        if (checkForType(TokenTypes.IDENTIFIER)) {
            List<Token> currentVar;
            int line = currentToken.getLine();

            if (isConst) {
                currentVar = this.bufferize("=");
            } else if (isParam) {
                currentVar = this.bufferize(",)");
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
            if (context.get(currentVariableEntry.getName()) != null) {
                this.errors.add(new SemanticError(currentVariableEntry.getLine(), currentVariableEntry.getName(), "Identificador novo", "Identificador já utilizado"));
            } else {
                context.put(currentVariableEntry.getName(), currentVariableEntry);
            }

            //this.updateToken();
            //System.out.println("Current var: " + currentVariableEntry);

            if (eatTerminal(";")) {
                if (eatTerminal("}")) {
                    return;
                } else {
                    checkDeclaration(isConst, true, context, isParam);
                }

            } else if (eatTerminal("=")) {
                checkAssignment(context);
            }
        }

        //System.out.println(currentToken);
        //Sair da recursão
        if (eatTerminal(";")) {
            if (eatTerminal("}")) {
                //System.out.println("Finished");
            } else {
                checkDeclaration(isConst, true, context, isParam);
            }
        } else if (eatTerminal(",")) {
            //verificar se pode haver uma variável seguida de outra na mesma linha
            checkDeclaration(isConst, isParam, context, isParam);
        } else if (eatTerminal(")") && isParam) {
            //System.out.println("Finished params");
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

    private String getExpressionType(List<Token> expression, Map<String, VariableEntry> context) {
        String operators = TokenTypes.DELIMITER + TokenTypes.ARITHMETICAL_OPERATOR + TokenTypes.RELATIONAL_OPERATOR + TokenTypes.LOGICAL_OPERATOR;
        String lastType = null;
        String tokenType;

        if (expression.size() == 1 && expression.get(0).getValue().equals("void")) {
            return "void";
        }

        for (Token token : expression) {
            tokenType = convertType(token, context);
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

    private String convertType(Token token, Map<String, VariableEntry> context) {
        return convertType(token, context, true);
    }

    private String convertType(Token token, Map<String, VariableEntry> context, boolean error) {
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

            VariableEntry var = context.get(token.getValue());
            String type;

            if (var == null) {
                type = TokenTypes.UNDEFINED;
                if (error)
                    this.errors.add(new SemanticError(token.getLine(), token.getValue(), "Valor ou Identificador válido", "Constante indefinida"));
            } else {
                type = var.getType();
            }

            return type;

        }


        return token.getType(); // TODO: remaining scopes
    }

    private void checkAssignment(Map<String, VariableEntry> context) {
        List<Token> expression = this.bufferize(",;");
        int line = expression.get(0).getLine();
        String expressionToken = expression.stream().map(Token::getValue).reduce("", (a, b) -> a + b);
        String expressionType = getExpressionType(expression, context);


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
        if (currentVariableEntry.isConst()) {
            currentVariableEntry.setValue(expressionToken);
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
                        String tokenType = convertType(t, context);

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
            checkMethod(classEntry);
            eatTerminal("}"); // Class end
            checkClass();

        }
    }

    // TODO: diferenciar retorno de vetores e variaveis do mesmo tipo
    private void checkMethod(ClassEntry classEntry) {
        if (eatTerminal("method")) {
            int line = this.currentToken.getLine();

            String returnType = translatePRE(this.currentToken.getValue());

            if(!this.symbolTable.isValidType(returnType)){
                this.errors.add(new SemanticError(line, returnType, "Classe válida ou tipo nativo", "Tipo de variável inválida"));
            }

            updateToken();
            String name = this.currentToken.getValue();
            updateToken();

            eatTerminal("(");
            Map<String, VariableEntry> params = new TreeMap<>(); // Deve ser TreeMap para manter a ordem
            checkDeclaration(false, true, params, true);

            MethodEntry method = new MethodEntry(name + params.toString(), returnType, params);

            eatTerminal("{");

            checkFunctionBody(); // TODO: implementar

            eatTerminal("return");
            List<Token> returnExpression = bufferize(";");

            Map<String, VariableEntry> context = this.symbolTable.getConstContext();
            context.putAll(classEntry.getVariables());
            context.putAll(method.getParams());

            int returnLine = this.currentToken.getLine();
            String expressionType = getExpressionType(returnExpression, context);

            try {
                classEntry.addMethod(method);
            } catch (Exception e) {
                this.errors.add(new SemanticError(line, method.getName(), "Assinatura única", "Já existe um método com essa assinatura"));

            }

            if (!expressionType.equals(returnType)) {
                this.errors.add(new SemanticError(returnLine, expressionType, returnType, "Tipo do retorno diferente do declarado"));

            }

            eatTerminal(";");
            eatTerminal("}");

            checkMethod(classEntry);
        }
    }

    private String translatePRE(String PRE) {
        switch (PRE) {
            case "float":
                return TokenTypes.NUMBER_FLOAT;
            case "int":
                return TokenTypes.NUMBER_INT;
            case "string":
                return TokenTypes.STRING;
            default:
                return PRE;
        }
    }

    private void checkFunctionBody() {
    }

    private void checkStatements(){

    }

    private void checkMain() {
    }

}