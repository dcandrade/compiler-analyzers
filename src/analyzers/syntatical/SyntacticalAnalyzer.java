package analyzers.syntatical;

import model.error.SyntaxError;
import model.token.Token;
import model.token.TokenTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public class SyntacticalAnalyzer {

    private final Iterator<Token> tokens;
    private final List<String> nativeTypes;
    private List<SyntaxError> errors;
    private Token currentToken = null;

    public SyntacticalAnalyzer(Iterator<Token> tokens) {
        this.tokens = tokens;
        this.errors = new ArrayList<>();
        this.nativeTypes = Arrays.asList(new String[]{"int", "float", "string", "bool", "void"});//List.of("int", "float", "string", "bool", "void");
    }

    private void updateToken() throws Exception {
        if (tokens.hasNext()) {
            this.currentToken = this.tokens.next();
        } else {
            throw new Exception("No more tokens");
        }
    }

    private boolean checkForTerminal(String terminal) {
        return currentToken.getValue().equals(terminal);
    }

    private boolean checkForType(String type) {
        return currentToken.getType().equals(type);
    }

    private void eatTerminal(String terminal, boolean updateOnError) throws Exception {
        if (!currentToken.getValue().equals(terminal)) {
            this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getValue(), terminal, SyntaxError.TERMINAL_MISMATCH));

            if(updateOnError){
                updateToken();
            }else{
                throw new Exception("Line: " + currentToken.getLine() + " -> " + "Expected " + terminal + " got " + currentToken.getValue());

            }
        }
        updateToken();
    }


    private void eatTerminal(String terminal) throws Exception {
        this.eatTerminal(terminal, false);
    }

    private void eatType(String type) throws Exception {
        this.eatType(type, true);
    }

    private void eatType(String type, boolean updateOnError) throws Exception {
        if (!currentToken.getType().equals(type)) {
            this.errors.add(new SyntaxError(currentToken.getLine(), currentToken.getType(), type, SyntaxError.IDENTIFIER_MISMATCH));

            if(updateOnError) {
                updateToken();
            }else {
                throw new Exception("Line: " + currentToken.getLine() + " -> " + "Expected type " + type + " got " + currentToken.getType() + " (" + currentToken.getValue() + ")");
            }
        }
        updateToken();
    }

    public void parseProgram() throws Exception {
        this.currentToken = tokens.next();
        parseConst();
        parseClasses();
        parseMain();
    }

    private void parseConst() throws Exception {
        if (checkForTerminal("const")) {
            eatTerminal("const");
            eatTerminal("{");
            parseConstBody();
            eatTerminal("}");
        }

    }

    private void parseConstBody() throws Exception {
        if (this.nativeTypes.contains(this.currentToken.getValue())) {
            parseType(true);
            parseConstAssignmentList();
            eatTerminal(";");

            parseConstBody();
        } else if (checkForType(TokenTypes.IDENTIFIER)) {
            eatType(TokenTypes.IDENTIFIER);
            parseConstAssignmentList();
            eatTerminal(";");

            parseConstBody();
        }
    }

    private void parseConstAssignmentList() throws Exception {
        parseConstAssignment();
        parseOptionalAssignments();
    }

    private void parseOptionalAssignments() throws Exception {
        if (checkForTerminal(",")) {
            eatTerminal(",");
            parseConstAssignmentList();
        } else if (checkForType(TokenTypes.IDENTIFIER)) {
            parseConstAssignment();
        }
    }

    private void parseConstAssignment() throws Exception {
        parseGeneralIdentifier();
        eatTerminal("=");
        parseVectorDecl();
    }

    private void parseClasses() throws Exception {
        if (checkForTerminal("class")) {
            eatTerminal("class");
            eatType(TokenTypes.IDENTIFIER);
            parseExtends();
            eatTerminal("{");
            parseVariables();
            parseMethods();
            eatTerminal("}");

            parseClasses();
        }

    }

    private void parseExtends() throws Exception {
        if (checkForTerminal("extends")) {
            eatTerminal("extends");
            eatType(TokenTypes.IDENTIFIER);
        }
    }

    private void parseMethods() throws Exception {
        if (checkForTerminal("method")) {
            eatTerminal("method");
            parseType(true);
            eatType(TokenTypes.IDENTIFIER);
            eatTerminal("(");
            parseParams();
            eatTerminal(")");
            parseFunctionBody();

            parseMethods();
        }

    }

    private void parseFunctionBody() throws Exception {
        eatTerminal("{");
        parseVariables();
        parseStatements();
        parseReturn();
        eatTerminal("}");
    }

    private void parseReturn() throws Exception {
        if (checkForTerminal("return")) {
            eatTerminal("return");
            if (checkForTerminal("void")) {
                updateToken();
            } else {
                parseExpression();
            }
            eatTerminal(";");
        }
    }

    private void parseExpression() throws Exception {
        parseAddExp();

        if (checkForType(TokenTypes.RELATIONAL_OPERATOR) || checkForType(TokenTypes.LOGICAL_OPERATOR)) {
            updateToken();
            parseExpression();
        }
    }

    private void parseAddExp() throws Exception {
        parseMultExp();

        if (checkForTerminal("+") || checkForTerminal("-")) {
            updateToken();
            parseAddExp();
        }
    }


    private void parseMultExp() throws Exception {
        parseNegateExp();

        if (checkForTerminal("*") || checkForTerminal("/")) {
            updateToken();
            parseMultExp();
        }
    }

    private void parseNegateExp() throws Exception {
        if (checkForTerminal("-")) {
            eatTerminal("-");
        }

        parseValue();
    }

    private void parseValue() throws Exception {
        if (checkForTerminal("(")) {
            eatTerminal("(");
            parseExpression();
            eatTerminal(")");
        } else {
            parseBaseValue();
        }
    }

    private void parseBaseValue() throws Exception {
        if (checkForType(TokenTypes.STRING)) {
            eatType(TokenTypes.STRING);
        } else if (checkForTerminal("true") || checkForTerminal("false")) {
            updateToken();
        } else if (checkForTerminal("--") || checkForTerminal("++") || checkForType(TokenTypes.NUMBER) || checkForType(TokenTypes.IDENTIFIER)) {
            parseNumber();
        } else {
            throw new Exception("base value error: " + this.currentToken);
        }
    }

    private boolean checkBaseValue() {
        try {
            parseBaseValue();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void parseNumber() throws Exception {
        if (checkForTerminal("++") || checkForTerminal("--")) {
            updateToken();
        }

        parseNumberLiteral();

        if (checkForTerminal("++") || checkForTerminal("--")) {
            updateToken();
        }

    }

    private void parseNumberLiteral() throws Exception {

        if (checkForType(TokenTypes.IDENTIFIER)) {
            parseMethodCall();
        } else if (checkForType(TokenTypes.NUMBER)) {
            updateToken();
        } else {
            throw new Exception("expected number or identifier: "+this.currentToken);
        }

    }

    private void parseMethodCall() throws Exception {
        parseGeneralIdentifier();
        parseFunctionParams();
    }

    private void parseFunctionParams() throws Exception {
        if (checkForTerminal("(")) {
            eatTerminal("(");
            parseArgList();
            eatTerminal(")");
        }
    }

    private void parseArgList() throws Exception {
        if (checkBaseValue()) {
            parseOptionalExtraArgs();
        }
    }

    private void parseOptionalExtraArgs() throws Exception {
        if (checkForTerminal(",")) {
            eatTerminal(",");
            parseArgList();
        } else {
            checkBaseValue();// base value parsed
        }
    }

    private void parseStatements() throws Exception {
        // Check for expressions and assignments

        if (checkForType(TokenTypes.IDENTIFIER)) {
            parseGeneralIdentifier();

            // Parsing assigment
            if (checkForTerminal("=")) {
                eatTerminal("=");
                parseExpression();
            } else if (checkForTerminal("(")) {
                parseFunctionParams();
            } else if(checkForTerminal("++") || checkForTerminal("--")) {
                //parseExpression();
                updateToken();
            }

            if(checkForType(TokenTypes.LOGICAL_OPERATOR) || checkForType(TokenTypes.RELATIONAL_OPERATOR) || checkForType(TokenTypes.ARITHMETICAL_OPERATOR)){
                updateToken();
                parseExpression();
            }

            eatTerminal(";");

            parseStatements();

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


    private void parseRead() throws Exception {
        eatTerminal("read");
        eatTerminal("(");
        parseGeneralIdentifierList();
        eatTerminal(")");
    }

    private void parseWrite() throws Exception {
        eatTerminal("write");
        parseFunctionParams();
    }

    private void parseWhile() throws Exception {
        eatTerminal("while");
        eatTerminal("(");
        parseExpression();
        eatTerminal(")");
        eatTerminal("{");
        parseStatements();
        eatTerminal("}");
    }

    private void parseIf() throws Exception {
        eatTerminal("if");
        eatTerminal("(");
        parseExpression();
        eatTerminal(")");
        eatTerminal("{");
        parseStatements();
        eatTerminal("}");
        parseElse();
    }

    private void parseGeneralIdentifierList() throws Exception {
        parseGeneralIdentifier();
        parseOptionalExtraIds();
    }

    private void parseOptionalExtraIds() throws Exception {

        if (checkForTerminal(",")) {
            eatTerminal(",");
            parseGeneralIdentifierList();
        } else if (checkForType(TokenTypes.IDENTIFIER)) { // First of General Identifier
            parseGeneralIdentifier();
        }
    }

    private void parseGeneralIdentifier() throws Exception {
        parseOptVector();
        parseComposedIdentifier();
    }

    private void parseComposedIdentifier() throws Exception {
        if (checkForTerminal(".")) {
            eatTerminal(".");
            parseGeneralIdentifier();
        }
    }

    private void parseOptVector() throws Exception {
        eatType(TokenTypes.IDENTIFIER);
        parseVectorIndex();
    }

    private void parseVectorIndex() throws Exception {
        if (checkForTerminal("[")) {
            eatTerminal("[");
            parseExpression();
            eatTerminal("]");

            parseVectorIndex();
        }

    }

    private void parseElse() throws Exception {
        if (checkForTerminal("else")) {
            eatTerminal("else");
            eatTerminal("{");
            parseStatements();
            eatTerminal("}");
        }
    }

    private void parseParams() throws Exception {
        if (this.nativeTypes.contains(this.currentToken.getValue()) || this.checkForType(TokenTypes.IDENTIFIER)) {
            parseType(true);
            parseOptVector();
            parseOptParams();

            parseParams();
        }
    }


    private void parseOptParams() throws Exception {
        if (checkForTerminal(",")) {
            eatTerminal(",");
            parseParams();
        }
    }

    private void parseType(boolean updateOnError) throws Exception {
        if (this.nativeTypes.contains(currentToken.getValue())) {
            eatTerminal(currentToken.getValue(), updateOnError);
        } else {
            this.eatType(TokenTypes.IDENTIFIER, updateOnError);
        }
    }

    private boolean attemptToParseType() throws Exception {
        if (this.nativeTypes.contains(this.currentToken.getValue())) {
            eatTerminal(this.currentToken.getValue());
            return true;
        } else if (this.checkForType(TokenTypes.IDENTIFIER)) {
            eatType(TokenTypes.IDENTIFIER);
            return true;
        }

        return false;
    }

    private void parseVariables() throws Exception {
        if (checkForTerminal("variables")) {
            eatTerminal("variables");
            eatTerminal("{");
            parseVariablesBody();
            eatTerminal("}");

        }

    }

    private void parseVariablesBody() throws Exception {
        if (this.attemptToParseType()) {
            parseVarDeclList();
            eatTerminal(";");

            parseVariablesBody();
        }
    }

    private void parseVarDeclList() throws Exception {
        parseVarDecl();
        parseOptionalDecls();
    }

    private void parseOptionalDecls() throws Exception {
        if (checkForTerminal(",")) {
            eatTerminal(",");
            parseVarDeclList();
        } else if (checkForType(TokenTypes.IDENTIFIER)) {
            parseVarDecl();
        }
    }

    private void parseVarDecl() throws Exception {
        parseGeneralIdentifier();
        parseVarAttribution();
    }

    private void parseVarAttribution() throws Exception {
        if (checkForTerminal("=")) {
            eatTerminal("=");
            parseVectorDecl();
        }
    }

    private void parseVectorDecl() throws Exception {
        if (checkForTerminal("[")) {
            parseVectorBody();
        } else {
            parseExpression();
        }
    }

    private void parseVectorBody() throws Exception {
        eatTerminal("[");
        parseVectorValueList();
        eatTerminal("]");
    }

    private void parseVectorValueList() throws Exception {
        parseVectorDecl();
        parseOptionalValue();
    }

    private void parseOptionalValue() throws Exception {
        if (checkForTerminal(",")) {
            eatTerminal(",");
            parseVectorValueList();
        }
        List<String> t  = Arrays.asList(new String[]{"-", "--", "(", "[", "++", "true", "false"});//= List.of("-", "--", "(", "[", "++", "true", "false");
        List<String> v = Arrays.asList(new String[]{TokenTypes.NUMBER, TokenTypes.STRING, TokenTypes.IDENTIFIER});//List.of(TokenTypes.NUMBER, TokenTypes.STRING, TokenTypes.IDENTIFIER);

        if (t.contains(currentToken.getValue()) || v.contains(currentToken.getType())) {
            parseVectorDecl();
        }
    }


    private void parseMain() throws Exception {
        eatTerminal("main");
        eatTerminal("{");
        parseVariables();
        parseStatements();
        eatTerminal("}");

        if(this.tokens.hasNext()){
            throw new Exception("unexpected extra tokens");
        }
    }
}
