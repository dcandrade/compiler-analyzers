package analyzers.syntatical;

import model.error.SyntaxError;
import model.token.Token;
import model.token.TokenTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;


public class SyntacticalAnalyzer {
    private static boolean THROW_EXCEPTION = false;
    private static boolean VERBOSE = false;
    private static String NATIVE_TYPE_SYNC;

    private final List<Token> tokens;
    private int tokenIndex;
    private List<SyntaxError> errors;
    private Token currentToken = new Token("", "", 0);

    public SyntacticalAnalyzer(List<Token> tokens) {
        this.tokens = tokens;
        this.tokenIndex = 0;
        this.errors = new ArrayList<>();

        NATIVE_TYPE_SYNC = String.join("", TokenTypes.nativeTypes);
        this.updateToken();
    }

    public List<SyntaxError> getErrors() {
        return errors;
    }

    private void updateToken() throws IndexOutOfBoundsException {
        if (this.tokenIndex < this.tokens.size()) {
            this.currentToken = this.tokens.get(this.tokenIndex);
            this.tokenIndex++;
        } else {
            throw new IndexOutOfBoundsException("No more tokens");
        }
    }

    private boolean checkForTerminal(String terminal) {
        return this.checkForTerminal(this.currentToken, terminal);
    }

    private boolean checkForTerminal(Token token, String terminal) {
        return token.getValue().equals(terminal);
    }

    private boolean checkForType(Token token, String type) {
        return token.getType().equals(type);
    }

    private boolean checkForType(String type) {
        return this.checkForType(this.currentToken, type);
    }

    private boolean eatTerminal(String terminal, boolean throwException, String errorMsg, String sync) throws NoSuchElementException {
        if (!currentToken.getValue().equals(terminal)) {
            this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getValue(), terminal, errorMsg));
            String msg = "TerminalError -> Line: " + currentToken.getLine() + " -> " + "Expected " + terminal + " got " + currentToken.getValue();

            if (VERBOSE)
                System.err.println(msg + " ---> " + errorMsg);


            if (throwException) {
                throw new NoSuchElementException(msg);
            }

            if (sync != null) {
                panic(sync);
            }

            return false;
        }
        updateToken();

        return true;
    }

    private boolean eatTerminal(String terminal) throws NoSuchElementException {
        return this.eatTerminal(terminal, THROW_EXCEPTION, "Token inesperado", null);
    }

    private boolean eatTerminal(String terminal, String sync) throws NoSuchElementException {
        return this.eatTerminal(terminal, THROW_EXCEPTION, "Token inesperado", sync);
    }

    private void eatType(String type) throws NoSuchElementException {
        this.eatType(type, THROW_EXCEPTION, "Tipo inesperado", null);
    }

    private void eatType(String type, String errorMsg, String sync) throws NoSuchElementException {
        this.eatType(type, THROW_EXCEPTION, errorMsg, sync);
    }

    private void eatType(String type, boolean throwException, String errorMsg, String sync) throws NoSuchElementException {
        if (!currentToken.getType().equals(type)) {
            this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getType(), type, errorMsg));
            String msg = "TypeError -> Line: " + currentToken.getLine() + " -> " + "Expected type " + type + " got " + currentToken.getType() + " (" + currentToken.getValue() + ")";

            if (VERBOSE)
                System.err.println(msg + "  --->  " + errorMsg);

            if (throwException && sync == null) {
                throw new NoSuchElementException(msg);
            }

            if (sync != null) {
                panic(sync);
            }

            return;
        }

        updateToken();
    }

    private Token peekToken(int offset) {
        int index = this.tokenIndex + offset;
        if (index >= this.tokens.size()) {
            return Token.EMPTY_TOKEN;
        } else
            return this.tokens.get(this.tokenIndex + offset);
    }

    public void parseProgram() throws NoSuchElementException {
        try {
            parseConst();
            parseClasses();
            parseMain();
        } catch (IndexOutOfBoundsException ex) {
            this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getValue(), "Tokens", "Fim do arquivo inesperado"));
        }
    }

    private void parseConst() throws NoSuchElementException {
        boolean possiblyMistypedKeyword = checkForType(TokenTypes.IDENTIFIER);
        boolean hasBraces = checkForTerminal(peekToken(1), "{");
        boolean hasVarDecl = checkForType(peekToken(2), TokenTypes.RESERVED_WORD) || checkForType(peekToken(2), TokenTypes.IDENTIFIER);

        boolean isConst = checkForTerminal("const") || (possiblyMistypedKeyword && hasBraces && hasVarDecl);

        if (isConst) {
            eatTerminal("const", "{");
            eatTerminal("{");
            parseConstBody();
            eatTerminal("}", "class" + "main");
        }

    }

    private void parseConstBody() throws NoSuchElementException {
        if (TokenTypes.nativeTypes.contains(this.currentToken.getValue())) {
            parseType(false, "Tipo da constante ausente", null);
            parseConstAssignmentList();
            eatTerminal(";", NATIVE_TYPE_SYNC + TokenTypes.IDENTIFIER + "};");

            parseConstBody();
        } else if (checkForType(TokenTypes.IDENTIFIER)) {
            eatType(TokenTypes.IDENTIFIER);
            parseConstAssignmentList();
            eatTerminal(";", NATIVE_TYPE_SYNC + TokenTypes.IDENTIFIER + "};");

            if (checkForTerminal(";")) {
                updateToken();
            }

            parseConstBody();
        }
    }

    private void parseConstAssignmentList() throws NoSuchElementException {
        parseConstAssignment();
        parseOptionalAssignments();
    }

    private void parseOptionalAssignments() throws NoSuchElementException {
        boolean missingComma = checkForType(TokenTypes.IDENTIFIER);

        if (checkForTerminal(",") || missingComma) {
            eatTerminal(",");
            if (checkForType(TokenTypes.IDENTIFIER)) {
                parseConstAssignmentList();
            } else {
                this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getValue(), TokenTypes.DELIMITER, "Esperava ;"));
                panic(",;");
                if (checkForTerminal(",")) {
                    parseOptionalAssignments();
                }
            }
        }
    }

    private void parseConstAssignment() throws NoSuchElementException {
        parseGeneralIdentifier();
        eatTerminal("=");
        parseVectorDecl();
    }

    private void parseClasses() throws NoSuchElementException {
        boolean hasClassName = checkForType(peekToken(1), TokenTypes.IDENTIFIER);
        boolean missingClassKeywords = checkForType(TokenTypes.IDENTIFIER);

        if (checkForTerminal("main")) {
            return;
        }

        if (checkForTerminal("class") || hasClassName || missingClassKeywords) {

            if (!eatTerminal("class") && hasClassName) {
                updateToken();
            }

            eatType(TokenTypes.IDENTIFIER);
            parseExtends();
            eatTerminal("{");
            parseVariables();
            parseMethods();
            eatTerminal("}");

            parseClasses();
        }

    }

    private void parseExtends() throws NoSuchElementException {
        if (checkForTerminal("extends")) {
            eatTerminal("extends");
            eatType(TokenTypes.IDENTIFIER, "Herança ausente", "{variables");
        } else if (checkForType(TokenTypes.IDENTIFIER)) {
            this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getValue(), "extends", "Esperado extends"));
            this.updateToken();
        }
    }

    private void parseMethods() throws NoSuchElementException {
        boolean missingOnlyKeyword = (this.checkForType(peekToken(0), TokenTypes.IDENTIFIER) || TokenTypes.nativeTypes.contains(this.currentToken.getValue()) && this.checkForType(peekToken(1), TokenTypes.IDENTIFIER));
        boolean mistypedKeyword = checkForType(TokenTypes.IDENTIFIER) && (this.checkForType(peekToken(1), TokenTypes.IDENTIFIER) || TokenTypes.nativeTypes.contains(this.currentToken.getValue()) && this.checkForType(peekToken(2), TokenTypes.IDENTIFIER));
        boolean hasOnlyName = checkForType(TokenTypes.IDENTIFIER) && checkForTerminal(peekToken(1), "(");


        if (checkForTerminal("method") || missingOnlyKeyword || mistypedKeyword || hasOnlyName || checkForType(TokenTypes.IDENTIFIER)) {
            eatTerminal("method");

            if (mistypedKeyword) {
                updateToken();
            }

            parseType(THROW_EXCEPTION, "Erro na assinatura do método", TokenTypes.IDENTIFIER + NATIVE_TYPE_SYNC);

            eatType(TokenTypes.IDENTIFIER, "Erro na assinatura do método", "(");

            eatTerminal("(");
            parseParams();
            eatTerminal(")");
            parseFunctionBody();

            parseMethods();
        }

    }

    private void parseFunctionBody() throws NoSuchElementException {
        eatTerminal("{");
        parseVariables();
        parseStatements();
        parseReturn();
        eatTerminal("}");
    }

    private void parseReturn() throws NoSuchElementException {
        boolean mistypedReturn = checkForType(TokenTypes.IDENTIFIER) && checkForType(peekToken(1), TokenTypes.IDENTIFIER);

        if (mistypedReturn || eatTerminal("return", "}")) {
            if (mistypedReturn) {
                updateToken();
            }
            if (checkForTerminal("void")) {
                updateToken();
            } else {
                try {
                    parseExpression();
                } catch (IndexOutOfBoundsException e) {
                    this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getValue(), "Expressão", e.getMessage()));
                    panic(";");
                }
            }
            eatTerminal(";");
        }
    }

    private void parseExpression() throws NoSuchElementException {
        parseAddExp();

        if (checkForType(TokenTypes.RELATIONAL_OPERATOR) || checkForType(TokenTypes.LOGICAL_OPERATOR)) {
            updateToken();
            parseExpression();
        }
    }

    private void parseAddExp() throws NoSuchElementException {
        parseMultExp();

        if (checkForTerminal("+") || checkForTerminal("-")) {
            updateToken();
            parseAddExp();
        }
    }


    private void parseMultExp() throws NoSuchElementException {
        parseNegateExp();

        if (checkForTerminal("*") || checkForTerminal("/")) {
            updateToken();
            parseMultExp();
        }
    }

    private void parseNegateExp() throws NoSuchElementException {
        if (checkForTerminal("-")) {
            eatTerminal("-");
        }

        parseValue();
    }

    private void parseValue() throws NoSuchElementException {
        if (checkForTerminal("(")) {
            eatTerminal("(");
            parseExpression();

            if (checkForTerminal(")")) {
                eatTerminal(")");
            }

        } else {
            parseBaseValue();
        }
    }

    private void parseBaseValue() throws NoSuchElementException {
        if (checkForType(TokenTypes.STRING)) {
            eatType(TokenTypes.STRING);
        } else if (checkForTerminal("true") || checkForTerminal("false")) {
            updateToken();
        } else if (checkForTerminal("--") || checkForTerminal("++") || checkForType(TokenTypes.NUMBER) || checkForType(TokenTypes.IDENTIFIER)) {
            parseNumber();
        } else {
            throw new IndexOutOfBoundsException("Expressão Malformada na linha " + currentToken.getLine());
        }
    }

    private boolean checkBaseValue() {
        try {
            parseBaseValue();
            return true;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    private void parseNumber() throws NoSuchElementException {
        if (checkForTerminal("++") || checkForTerminal("--")) {
            updateToken();
        }

        parseNumberLiteral();

        if (checkForTerminal("++") || checkForTerminal("--")) {
            updateToken();
        }

    }

    private void parseNumberLiteral() throws NoSuchElementException {

        if (checkForType(TokenTypes.IDENTIFIER)) {
            parseMethodCall();
        } else if (checkForType(TokenTypes.NUMBER)) {
            updateToken();
        } else {
            throw new NoSuchElementException("expected number or identifier: " + this.currentToken);
        }

    }

    private void parseMethodCall() throws NoSuchElementException {
        parseGeneralIdentifier();
        parseFunctionParams();
    }

    private void parseFunctionParams(boolean mandatory) throws NoSuchElementException {
        if (checkForTerminal("(") || mandatory) {
            eatTerminal("(");
            parseArgList();
            eatTerminal(")", ";");
        }
    }

    private void parseFunctionParams() throws NoSuchElementException {
        parseFunctionParams(false);
    }

    private void parseArgList() throws NoSuchElementException {
        if (checkBaseValue()) {
            parseOptionalExtraArgs();
        }
    }

    private void parseOptionalExtraArgs() throws NoSuchElementException {
        if (checkForTerminal(",")) {
            eatTerminal(",");
            parseArgList();
        } else if (!checkForTerminal(")")) {
            this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getValue(), "Vírgula", "Parâmetro inesperado"));
            checkBaseValue();// base value parsed
        }
    }

    private void parseStatements() throws NoSuchElementException {
        // Check for expressions and assignments
        boolean comma = true;

        if (checkForType(TokenTypes.IDENTIFIER)) {
            parseGeneralIdentifier();
            Token keep = this.currentToken;

            // Parsing assigment

            if (checkForTerminal("=")) {
                eatTerminal("=");

                try {
                    parseExpression();
                    if (!checkForTerminal(";")) {
                        throw new IndexOutOfBoundsException();
                    }
                } catch (IndexOutOfBoundsException | NoSuchElementException e) {

                    this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getValue(), "Expressão", "Expressão Malformada"));
                    this.panic(";");
                }
            } else if (checkForTerminal("{")) {
                this.errors.add(new SyntaxError(keep.getLine(), keep.getValue(), "else", "Else malformado"));
                eatTerminal("{");
                parseStatements();
                eatTerminal("}");
                comma = false;
            } else if (checkForTerminal("(")) {

                parseFunctionParams();

                if (checkForTerminal("{")) {
                    eatTerminal("{");
                    parseStatements();
                    eatTerminal("}");

                    if (checkForTerminal("else")) {
                        this.errors.add(new SyntaxError(keep.getLine(), keep.getValue(), "If", "IF malformado"));

                        eatTerminal("else");
                        eatTerminal("{");
                        parseStatements();
                        eatTerminal("}");
                    } else {
                        this.errors.add(new SyntaxError(keep.getLine(), keep.getValue(), "If ou While", "Loop ou Condicional malformado"));

                    }
                }
            } else if (checkForTerminal("++") || checkForTerminal("--")) {
                //parseExpression();
                updateToken();
            }

            if (checkForType(TokenTypes.LOGICAL_OPERATOR) || checkForType(TokenTypes.RELATIONAL_OPERATOR) || checkForType(TokenTypes.ARITHMETICAL_OPERATOR)) {
                updateToken();
                try {
                    parseExpression();
                } catch (IndexOutOfBoundsException e) {
                    this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getValue(), "Expressão", "Expressão Malformada"));
                    this.panic(";");
                }
            }

            if (checkForType(TokenTypes.IDENTIFIER)) {

                int ref = this.tokenIndex;
                Token current = this.currentToken;
                panic(");.[");

                if (checkForTerminal(")")) {
                    eatTerminal(")");
                    this.errors.add(new SyntaxError(current.getLine(), current.getValue(), "(", "Chamada de método malformada"));
                } else if (checkForTerminal(";")) {
                    rollback(ref);
                    return; // Mistyped return
                } else {
                    this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getValue(), "Ponto ou =", "Atribuição Malformada"));
                    panic(";");
                }

            }

            if (comma)
                eatTerminal(";", ";" + TokenTypes.IDENTIFIER + "if" + "while" + "write" + "read");

            parseStatements();

        } else if (checkForTerminal("(")) {
            this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getValue(), "Chamada de Método", "Identificador Ausente"));

            parseFunctionParams();
            eatTerminal(";");
        } else {
            switch (currentToken.getValue()) {
                case "if":
                    parseIf();
                    parseStatements();
                    break;
                case "while":
                    parseWhile();
                    parseStatements();
                    break;
                case "write":
                    parseWrite();
                    eatTerminal(";");
                    parseStatements();
                    break;
                case "read":
                    parseRead();
                    eatTerminal(";");
                    parseStatements();
                    break;
            }
        }
    }

    private void rollback(int ref) {
        this.currentToken = this.tokens.get(ref);
        this.tokenIndex = ref;
    }


    private void parseRead() throws NoSuchElementException {
        eatTerminal("read");
        eatTerminal("(");
        parseGeneralIdentifierList();
        eatTerminal(")");
    }

    private void parseWrite() throws NoSuchElementException {
        eatTerminal("write");
        parseFunctionParams(true);
    }

    private void parseWhile() throws NoSuchElementException {
        eatTerminal("while");
        eatTerminal("(");
        try {
            parseExpression();

            if (!checkForTerminal(")") && !checkForTerminal("{")) {
                panic("){");
                throw new Exception();
            }
        } catch (Exception e) {
            this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getValue(), "Expressão", "Expressão Malformada"));
        }


        eatTerminal(")");
        eatTerminal("{");
        parseStatements();
        eatTerminal("}");
    }

    private void parseIf() throws NoSuchElementException {
        eatTerminal("if");

        eatTerminal("(");
        try {
            parseExpression();

            if (!checkForTerminal(")")) {
                panic(")");
                throw new Exception();
            }
        } catch (Exception e) {
            this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getValue(), "Expressão", "Expressão Malformada"));

        }

        eatTerminal(")", "{");
        eatTerminal("{");
        parseStatements();
        eatTerminal("}");
        parseElse();
    }


    private void parseGeneralIdentifierList() throws NoSuchElementException {
        parseGeneralIdentifier();
        parseOptionalExtraIds();
    }

    private void parseOptionalExtraIds() throws NoSuchElementException {
        boolean missingComma = this.checkForType(TokenTypes.IDENTIFIER);

        if (checkForTerminal(",") || missingComma) {
            eatTerminal(",");
            parseGeneralIdentifierList();
        } else if (checkForType(TokenTypes.IDENTIFIER)) { // First of General Identifier
            parseGeneralIdentifier();
        }
    }

    private void parseGeneralIdentifier() throws NoSuchElementException {
        parseOptVector();
        parseComposedIdentifier();
    }

    private void parseComposedIdentifier() throws NoSuchElementException {
        if (checkForTerminal(".")) {
            eatTerminal(".");
            if (checkForType(TokenTypes.IDENTIFIER)) {
                parseGeneralIdentifier();
            } else {
                this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getValue(), TokenTypes.IDENTIFIER, "Esperado identificador"));
                panic(".([" + TokenTypes.IDENTIFIER);

                if (checkForTerminal("(")) {
                    return;
                } else if (checkForTerminal("[")) {
                    parseVectorIndex();
                } else if (checkForType(TokenTypes.IDENTIFIER)) {
                    parseGeneralIdentifier();
                } else if (checkForTerminal(".")) {
                    eatTerminal(".");
                    parseGeneralIdentifier();
                }
            }
        }
    }

    private void parseOptVector() throws NoSuchElementException {
        eatType(TokenTypes.IDENTIFIER, true, "Sintaxe de variável ou parâmetro incorreta", "=,;)" + TokenTypes.IDENTIFIER + TokenTypes.RESERVED_WORD);

        parseVectorIndex();
    }

    private void parseVectorIndex() throws NoSuchElementException {
        if (checkForTerminal("[")) {
            eatTerminal("[");
            try {
                parseExpression();
            } catch (IndexOutOfBoundsException ex) {
                this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getValue(), "Indexação", "Indexação de vetor inválida"));
                panic("];,");
            }
            eatTerminal("]", ";,");

            parseVectorIndex();
        }

    }

    private void parseElse() throws NoSuchElementException {
        if (checkForTerminal("else")) {
            eatTerminal("else");
            eatTerminal("{");
            parseStatements();
            eatTerminal("}");
        }
    }

    private void parseParams() throws NoSuchElementException {
        if (TokenTypes.nativeTypes.contains(this.currentToken.getValue()) || this.checkForType(TokenTypes.IDENTIFIER)) {
            parseType(false, "Formato incorreto de parâmetro", null);
            parseOptVector();
            parseOptParams();

            parseParams();
        }
    }


    private void parseOptParams() throws NoSuchElementException {
        boolean missingComma = TokenTypes.nativeTypes.contains(this.currentToken.getValue()) || this.checkForType(TokenTypes.IDENTIFIER);

        if (checkForTerminal(",") || missingComma) {
            eatTerminal(",");
            parseParams();
        }
    }

    private void parseType(boolean throwException, String errorMsg, String sync) throws NoSuchElementException {
        if (TokenTypes.nativeTypes.contains(currentToken.getValue())) {
            eatTerminal(currentToken.getValue(), throwException, errorMsg, sync);
        } else {
            this.eatType(TokenTypes.IDENTIFIER, throwException, errorMsg, sync);
        }
    }

    private boolean attemptToParseType() throws NoSuchElementException {
        if (TokenTypes.nativeTypes.contains(this.currentToken.getValue())) {
            eatTerminal(this.currentToken.getValue());
            return true;
        } else if (this.checkForType(TokenTypes.IDENTIFIER)) {
            eatType(TokenTypes.IDENTIFIER);
            return true;
        }

        return false;
    }

    private void parseVariables() throws NoSuchElementException {
        boolean missingKeyword = checkForTerminal("{");

        if (checkForTerminal("variables") || missingKeyword) {
            eatTerminal("variables");
            eatTerminal("{");
            parseVariablesBody();
            eatTerminal("}", "method" + TokenTypes.IDENTIFIER);
        }

    }

    private void parseVariablesBody() throws NoSuchElementException {
        if (this.attemptToParseType()) {
            parseVarDeclList();
            eatTerminal(";", ";");

            parseVariablesBody();
        } else if (!checkForTerminal("}")) {
            this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getValue(), TokenTypes.IDENTIFIER, "Esperado declaração de variável"));
            panic(TokenTypes.IDENTIFIER + NATIVE_TYPE_SYNC);
            this.parseVariablesBody();

        }
    }

    private void parseVarDeclList() throws NoSuchElementException {
        parseVarDecl();
        parseOptionalDecls();
    }

    private void parseOptionalDecls() throws NoSuchElementException {
        boolean missingComma = checkForType(TokenTypes.IDENTIFIER);

        if (checkForTerminal(",") || missingComma) {
            eatTerminal(",");
            try {
                parseVarDeclList();
            } catch (Exception e) {
                this.panic(",");
                this.parseOptionalDecls();
            }
        } else if (checkForType(TokenTypes.IDENTIFIER)) {
            parseVarDecl();
        }
    }

    private void parseVarDecl() throws NoSuchElementException {
        parseGeneralIdentifier();
        parseVarAttribution();
    }

    private void parseVarAttribution() throws NoSuchElementException {
        if (checkForTerminal("=")) {
            eatTerminal("=");
            parseVectorDecl();
        }
    }

    private void parseVectorDecl() throws NoSuchElementException {
        if (checkForTerminal("[")) {
            parseVectorBody();
        } else {
            try {
                parseExpression();
            } catch (IndexOutOfBoundsException e) {
                this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getValue(), "Expressão", "Expressão Malformada"));

            }
        }
    }

    private void parseVectorBody() throws NoSuchElementException {
        eatTerminal("[");
        parseVectorValueList();
        eatTerminal("]");
    }

    private void parseVectorValueList() throws NoSuchElementException {
        parseVectorDecl();
        parseOptionalValue();
    }

    private void parseOptionalValue() throws NoSuchElementException {
        if (checkForTerminal(",")) {
            eatTerminal(",");
            parseVectorValueList();
        }
        List<String> t = Arrays.asList("-", "--", "(", "[", "++", "true", "false");//= List.of("-", "--", "(", "[", "++", "true", "false");
        List<String> v = Arrays.asList(TokenTypes.NUMBER, TokenTypes.STRING, TokenTypes.IDENTIFIER);//List.of(TokenTypes.NUMBER, TokenTypes.STRING, TokenTypes.IDENTIFIER);

        if (t.contains(currentToken.getValue()) || v.contains(currentToken.getType())) {
            parseVectorDecl();
        }
    }


    private void parseMain() throws NoSuchElementException {
        eatTerminal("main");
        eatTerminal("{");
        parseVariables();
        parseStatements();
        eatTerminal("}");

        if (this.tokenIndex != this.tokens.size()) {
            if (VERBOSE)
                System.err.println("unexpected extra tokens");
            this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getValue(), "Fim do arquivo", "Esperado fim do arquivo"));
        }
    }

    private void panic(String sync) throws NoSuchElementException {
        if (VERBOSE)
            System.err.println("---- Entering panic mode ---- (line " + this.currentToken.getLine() + ")");
        //System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));
        while (!sync.contains(this.currentToken.getValue()) && !sync.contains(this.currentToken.getType())) {
            if (VERBOSE)
                System.err.println("-> Skipping token " + this.currentToken);
            updateToken();
        }

        if (VERBOSE)
            System.err.println("-> Panic finished w/ token " + this.currentToken);

    }

}
