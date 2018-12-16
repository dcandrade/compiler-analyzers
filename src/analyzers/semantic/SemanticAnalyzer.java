package analyzers.semantic;

import model.semantic.SymbleTable;
import model.semantic.Variable;
import model.semantic.Class;
import model.token.Token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SemanticAnalyzer {

    private final List<Token> tokens;
    private SymbleTable symbleTable;
    private int tokenIndex;

    private Token currentToken, lastToken;
    private Variable currentVariable;
    private Class currenteClasss;
    private String currentType;

    private final List<String> nativeTypes;


    public SemanticAnalyzer(List<Token> tokens) {
        currentVariable = new Variable();
        currenteClasss = new Class();
        symbleTable = new SymbleTable();
        this.tokens = tokens;
        this.tokenIndex = 0;
        this.updateToken();
        this.nativeTypes = Arrays.asList("int", "float", "string", "bool", "void");
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

    public void analyzer() {
        Const();
        chechClass();
    }

    public void Const() {
        if(currentToken.getValue().equals("const")) {
            this.updateToken();
            checkToken("{");
            analyzerConst();
        }
    }

    public boolean analyzerConst() {
        currentVariable.setConst(true);
        checkDeclaration();
        return false;
    }

    public boolean checkToken(String type) {
        if(currentToken.getValue().equals(type) || currentToken.getType().equals(type)) {
            System.out.println(currentToken.getValue());
            this.updateToken();
            return true;
        }
        return false;
    }

    public boolean checkType() {
        if(nativeTypes.contains(currentToken.getValue())) {
            currentType = currentToken.getValue();
            System.out.println(currentToken.getValue());
            this.updateToken();
            return true;
        }
        return false;
    }

    public boolean checkDeclaration() {
        checkType();
        if(checkToken("IDE")) {
            currentVariable.setName(lastToken.getValue());
            if (checkToken(";")) {
                if(checkToken("}")) {
                    System.out.println("Finishe");
                    return true;
                } else {
                    //analyzerConst(); //Verificar possivel erro
                    checkDeclaration();
                }

            } else {
                checkToken("REL");
                checkAssignment();
            }
        }
        if(checkToken(";")) {
            if(checkToken("}")) {
                System.out.println("Finishe");
                return true;
            } else {
                analyzerConst();
            }
        } else if(checkToken(",")) {
            checkDeclaration();
            //return true;
        }
        return false;
    }

    public boolean checkAssignment() {

        if(currentType.equals("string")) {

            if(currentToken.getType().equals("CDC")){
                currentVariable.setType(currentToken.getValue());
                System.out.println(currentToken.getValue());
                this.updateToken();
            } else {
                System.err.println("Erro");
            }
        } else if (currentType.equals("boolean")){

            if(currentToken.getValue().equals("true") || currentToken.getValue().equals("false")) {
                currentVariable.setType(currentToken.getValue());
                System.out.println(currentToken.getValue());
                this.updateToken();
            } else {
                System.err.println("Erro");
            }
        } else if (currentType.equals("int") || currentType.equals("float")) {

            if(currentToken.getType().equals("NRO")) {
                currentVariable.setType(currentToken.getValue());
                System.out.println(currentToken.getValue());
                this.updateToken();
            } else {
                System.err.println("Erro");
            }
        }
        symbleTable.addConst(currentVariable);
        return true;
    }

    public boolean chechClass() {
        if(checkToken("class")) {
            if(checkToken("IDE")) {
                if (checkToken("{")) {
                    checkVariable();
                }
            }
        }

        return true;
    }

    public boolean checkVariable() {
        if (checkToken("variables")) {
            if (checkToken("{")) {
                checkDeclaration();
                return true;
            }
        }
        return false;
    }
}