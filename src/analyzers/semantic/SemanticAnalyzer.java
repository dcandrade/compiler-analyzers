package analyzers.semantic;

import model.error.SemanticError;
import model.semantic.SymbolTable;
import model.semantic.entries.ClassEntry;
import model.semantic.entries.MethodEntry;
import model.semantic.entries.VariableEntry;
import model.token.Token;
import model.token.TokenTypes;

import javax.management.InstanceAlreadyExistsException;
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

            if (!this.symbolTable.isValidType(currentType)) {
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
                    this.errors.add(new SemanticError(line, "Indexador Inválido (" + dimensions + ")", "Número/identificador Inteiro", "Dimensão de vetor inválida"));
                    currentVariableEntry = new VariableEntry(varName, currentType, isConst, Collections.EMPTY_LIST, line);
                }
            } else {
                varName = fullIdentifier;
                currentVariableEntry = new VariableEntry(varName, currentType, isConst, line);
            }

            //this.currentVariableEntryList.add(currentVariableEntry);
            VariableEntry var = context.get(currentVariableEntry.getName());
            if (var != null) {
                String msg;

                // TODO: especificar escopo da variável (classe etc)
                if (var.isConst()) {
                    msg = "Identificador já utilizado com constante";
                } else {
                    msg = "Indentificador já foi definido na classe ou na classe mãe";
                }

                this.errors.add(new SemanticError(currentVariableEntry.getLine(), currentVariableEntry.getName(), "Identificador novo", msg));
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

    //TODO: diferenciar retorn de vetor indexado e não indexado
    private String getExpressionType(List<Token> expression, Map<String, VariableEntry> context) {
        String operators = TokenTypes.DELIMITER + TokenTypes.ARITHMETICAL_OPERATOR + TokenTypes.RELATIONAL_OPERATOR + TokenTypes.LOGICAL_OPERATOR;
        String lastType = null;
        String tokenType;

        if (expression.size() == 1 && expression.get(0).getValue().equals("void")) {
            return "void";
        }

        if (expression.isEmpty()) {
            return TokenTypes.UNDEFINED;
        }

        int skipStart = -1, skipEnd = -1;

        for (int i = 0; i < expression.size(); i++) {
            Token token = expression.get(i);
            tokenType = convertType(token, context);

            if (token.getValue().equals("[")) {
                skipStart = ++i;

                while (!expression.get(i).getValue().equals("]")) {
                    i++;
                }

                skipEnd = i;

                String arrayIndexType = this.getExpressionType(expression.subList(skipStart, skipEnd), context);

                if (!arrayIndexType.equals(TokenTypes.NUMBER_INT) && !arrayIndexType.equals(TokenTypes.UNDEFINED)) {
                    this.errors.add(new SemanticError(token.getLine(), arrayIndexType, "NIN", "Indexador de vetor inválido"));
                }

            }


            if (!operators.contains(token.getType())) {
                if (lastType == null) {
                    lastType = tokenType;
                } else if (lastType.equals(TokenTypes.STRING)) {
                    this.errors.add(new SemanticError(token.getLine(), "", "", "Não podem ser realizadas operações com strings"));

                    return TokenTypes.UNDEFINED;
                } else if (!lastType.equals(tokenType)) {
                    // Conversão de tipos dentro de uma expressão, logo o tipo da expressão é indefinido
                    if (!tokenType.equals(TokenTypes.UNDEFINED))
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
                        //this.errors.add(new SemanticError(line, expressionType, currentType, "Aviso de conversão"));
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

    // TODO: add contexto das outras classes
    private void checkClass() {
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

            ClassEntry superclass = null;
            try {
                superclass = this.symbolTable.getClass(superclassName);
            } catch (ClassNotFoundException e) {
                this.errors.add(new SemanticError(line, superclassName, "Classe mãe previamente declarada", "Classe mãe inexistente"));
            }

            ClassEntry classEntry = null;
            try {
                classEntry = this.symbolTable.addClass(className, superclass);
            } catch (InstanceAlreadyExistsException e) {
                this.errors.add(new SemanticError(line, className, "Novo nome para classe", "Classe com nome repetido"));
            }

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

            if (!this.symbolTable.isValidType(returnType)) {
                this.errors.add(new SemanticError(line, returnType, "Classe válida ou tipo nativo", "Tipo de variável inválida"));
            }

            updateToken();
            String name = this.currentToken.getValue();
            updateToken();

            eatTerminal("(");
            Map<String, VariableEntry> params = new TreeMap<>(); // Deve ser TreeMap para manter a ordem
            checkDeclaration(false, true, params, true);

            String methodSymbol = name + params.entrySet().stream().map(s -> s.getValue().getType()).reduce("", (s, s2) -> s + s2);

            MethodEntry method = new MethodEntry(methodSymbol, returnType, params);
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

    private VariableEntry getChainedExpressionType(int line, List<Token> call, Map<String, VariableEntry> context, Map<String, ClassEntry> classes) {
        int i = 0;
        String next;
        VariableEntry variableEntry = context.get(call.get(0).getValue());

        while (true) {
            if (i > call.size() - 2) {
                return variableEntry;
            }
            ClassEntry classEntry = classes.get(variableEntry.getType());
            next = call.get(++i).getValue();

            if (next.equals(".")) {
                next = call.get(++i).getValue();
                //continue;
            }

            if (i + 1 < call.size() && call.get(i + 1).getValue().equals("(")) {
                String baseName = next;
                int start = i + 1;

                while (!call.get(i).getValue().equals(")")) {
                    i++;
                }
                i++;

                List<Token> params = call.subList(start + 1, i - 1); // remove brackets

                String methodSymbol = baseName + params.stream()
                        .filter(t -> !t.getValue().equals(","))
                        .map(t -> convertType(t, context))
                        .reduce("", (s, s2) -> s + s2);

                String methodType;

                try {
                    methodType = classes.get(variableEntry.getType()).getMethodType(methodSymbol);
                } catch (Exception e) {
                    this.errors.add(new SemanticError(line, "", "", String.format("A classe %s não tem método %s com essa assinatura", classEntry.getName(), baseName)));
                    methodType = TokenTypes.UNDEFINED;
                }
                variableEntry = new VariableEntry("", methodType, line);

            } else { // isn't a method call
                try {
                    variableEntry = classEntry.getVariable(next);
                } catch (Exception e) {
                    this.errors.add(new SemanticError(line, "", "", String.format("A classe %s não tem atributo %s", classEntry.getName(), next)));
                    return new VariableEntry("", TokenTypes.UNDEFINED, line);
                }
            }

            if (i >= call.size()) {
                return variableEntry;
            }
        }

    }

    private void checkStatements(Map<String, VariableEntry> context, Map<String, ClassEntry> classes) throws Exception {
        int line = currentToken.getLine();
        VariableEntry var;

        if (checkForType(TokenTypes.IDENTIFIER)) {
            String opValue  = this.tokens.get(tokenIndex).getValue();
            if(opValue.equals("++") || opValue.equals("--")){
                var = context.get(currentToken.getValue());
                System.out.println("---"+var);

                if(var.getType().equals(TokenTypes.BOOLEAN) || var.getType().equals(TokenTypes.STRING) || var.isConst()){
                    this.errors.add(new SemanticError(line, "String ou Booleano", "Número", "Somente variáveis numéricas podem ser incrementados ou decrementados"));
                }

                updateToken();
                updateToken();
                eatTerminal(";");
                checkStatements(context, classes);
            }else {

                List<Token> leftExp = this.bufferize("=;");
                String fullIdentifier = leftExp.stream().map(Token::getValue).reduce("", (a, b) -> a + b);

                fullIdentifier = removeBrackets(fullIdentifier);

                if (fullIdentifier.contains(".")) {
                    var = getChainedExpressionType(line, leftExp, context, classes);
                } else {
                    var = context.get(fullIdentifier);
                }

                if (checkForTerminal("=")) {

                    updateToken();

                    List<Token> expression = bufferize(";");
                    eatTerminal(";");

                    String expressionType = getExpressionType(expression, context);
                    String ri = expression.stream().map(Token::getValue).reduce("", (a, b) -> a + b);

                    //System.out.println("Right "+ri);
                    if (var.isConst()) {
                        this.errors.add(new SemanticError(line, "", "", "Atribuição de novo valor a uma constante (" + var.getName() + ")"));
                    } else if (!var.getType().equals(expressionType) && !expressionType.equals(TokenTypes.UNDEFINED)) {
                        this.errors.add(new SemanticError(line, expressionType, var.getType(), "Erro de conversão"));
                    }

                }

                eatTerminal(";");
                checkStatements(context, classes);
            }
        }else {
            switch (currentToken.getValue()) {
                case "if":
                    eatTerminal("if");
                    eatTerminal("(");
                    bufferize(")");
                    eatTerminal(")");
                    eatTerminal("{");
                    checkStatements(context, classes);
                    eatTerminal("}");

                    break;
                case "while":
                    eatTerminal("while");
                    eatTerminal("(");
                    bufferize(")");
                    eatTerminal(")");
                    eatTerminal("{");
                    checkStatements(context, classes);
                    eatTerminal("}");
                    break;
                case "write":
                    //parseWrite();
                    eatTerminal(";");
                    checkStatements(context, classes);
                    break;
                case "read":
                    //parseRead();
                    eatTerminal(";");
                    checkStatements(context, classes);
                    break;
            }
        }

    }

    private String removeBrackets(String fullIdentifier) {
        while (fullIdentifier.contains("[")) {
            String prefix = fullIdentifier.substring(0, fullIdentifier.indexOf("["));
            String post = fullIdentifier.substring(fullIdentifier.indexOf("]") + 1, fullIdentifier.length());

            fullIdentifier = prefix + post;
        }
        return fullIdentifier;
    }

    private void checkMain() throws Exception {
        ClassEntry mainClass = this.symbolTable.addClass("main", null);

        eatTerminal("main");
        eatTerminal("{");
        checkVariable(mainClass);

        Map<String, VariableEntry> context = this.symbolTable.getFullVariablesContext();

        checkStatements(context, this.symbolTable.getClasses());

        eatTerminal("}");

    }


}