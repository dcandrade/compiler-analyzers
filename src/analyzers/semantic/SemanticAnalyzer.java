package analyzers.semantic;

import model.semantic.SymbolTable;
import model.semantic.entries.VariableEntry;
import model.semantic.entries.ClassEntry;
import model.token.Token;
import model.token.TokenTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SemanticAnalyzer {

    private final List<Token> tokens;
    private SymbolTable symbolTable;
    private int tokenIndex;

    private Token currentToken, lastToken;
    private VariableEntry currentVariableEntry;
    private ClassEntry currentClass;
    private String currentType, getCurrentTypeMethod;
    private List<VariableEntry> currentVariableEntryList;
    private List<VariableEntry> currentConstantList;
    private boolean isConst = false;

    private final List<String> nativeTypes;


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

    private void analyzer() throws Exception {
        checkConst();
        checkClass();
    }

    private boolean checkConst() throws Exception {
        if(checkToken("const")) {
            isConst = true;
            if (checkToken("{")) {
                analyzerVariable();
                return true;
            }
        }
        return false;
    }

    private boolean checkVariable() throws Exception {
        if (checkToken("variables")) {
            isConst = false;
            if (checkToken("{")) {
                analyzerVariable();
                return true;
            }
        }
        return false;
    }

    private boolean analyzerVariable() throws Exception {
        checkDeclaration();
        return true;
    }

    private boolean checkToken(String type) {
        if(currentToken.getValue().equals(type) || currentToken.getType().equals(type)) {
            System.out.println(currentToken.getValue());
            this.updateToken();
            return true;
        }
        return false;
    }

    private boolean checkType(int hierarchy) {
        if(nativeTypes.contains(currentToken.getValue())) {
            if(hierarchy == 1) {
                currentType = currentToken.getValue();
            } else if (hierarchy == 2) {
                getCurrentTypeMethod = currentToken.getValue();
            }

            //System.out.println(currentToken.getValue());
            this.updateToken();
            return true;
        }
        return false;
    }

    private boolean checkDeclaration() throws Exception {
        checkType(1);

        if(checkToken(TokenTypes.IDENTIFIER)) {
            currentVariableEntry = new VariableEntry(lastToken.getValue(), currentType, isConst);


            if (checkToken(";")) {
                if(checkToken("}")) {
                    System.out.println("Finishe");
                    return true;
                } else {
                    if (currentVariableEntry.isConst()) {
                        //System.out.println("Constante: " + currentVariableEntry.getName());
                    } else {
                        //System.out.println("Variavel: " + currentVariableEntry.getName());
                        currentVariableEntryList.add(currentVariableEntry);
                    }
                    checkDeclaration();
                }

            } else {
                checkToken(TokenTypes.RELATIONAL_OPERATOR);
                checkAssignment();
                if (currentVariableEntry.isConst()) {
                    //System.out.println("Constante: " + currentVariableEntry.getName());
                } else {
                    //System.out.println("Variavel: " + currentVariableEntry.getName());
                    currentVariableEntryList.add(currentVariableEntry);
                }
            }
        }

        //Sair da recursão
        if (checkToken(";")) {
            if(checkToken("}")) {
                System.out.println("Finishe");
                return true;
            } else {
                analyzerVariable();
            }
        } else if(checkToken(",")) {
            //verificar se pode haver uma variável seguida de outra na mesma linha
            currentVariableEntry = new VariableEntry(null, null);
            currentVariableEntry.setConst(false);
            checkDeclaration();
            if (currentVariableEntry.isConst()) {
                //System.out.println("Constante: " + currentVariableEntry.getName());
            } else {
                //System.out.println("Variavel: " + currentVariableEntry.getName());
                currentVariableEntryList.add(currentVariableEntry);
            }
        }

        return false;
    }

    private boolean checkAssignment() throws Exception {
        switch (currentType) {

            case "string":
                if (currentToken.getType().equals(TokenTypes.STRING)) {
                    //currentVariableEntry.setValue(currentToken.getValue());
                    this.updateToken();
                } else {
                    System.err.println("Erro");
                }
                break;

            case "boolean":
                if (currentToken.getValue().equals("true") || currentToken.getValue().equals("false")) {
                    //currentVariableEntry.setValue(currentToken.getValue());
                    this.updateToken();
                } else {
                    System.err.println("Erro");
                }
                break;

            case "int":
            case "float":

                if (currentToken.getType().equals(TokenTypes.NUMBER)) {
                    //currentVariableEntry.setValue(currentToken.getValue());
                    this.updateToken();
                } else {
                    System.err.println("Erro");
                }
                break;
        }
        symbolTable.addConst(currentVariableEntry);
        return true;
    }

    private boolean checkClass() throws Exception {
        if(checkToken("class")) {
            if(checkToken(TokenTypes.IDENTIFIER)) {
                if (checkToken("{")) {
                    checkVariable();
                    currentClass.addVariableList(currentVariableEntryList);
                    checkMethod();
                } else if(checkToken(currentToken.getValue())) {
                    if (checkToken(TokenTypes.IDENTIFIER)) {//Verificar se a classe herdade existe
                        if (checkToken("{")) {
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
        if (checkToken("method")) {
            if(nativeTypes.contains(currentToken.getValue())) {
                checkType(2);
                if (checkToken(TokenTypes.IDENTIFIER)) {
                    checkParams();
                    if (checkToken("{")) {
                        checkFunctionBody();
                        checkReturn();
                        if(checkToken(";")) {
                            if (checkToken("}")) {
                                checkMethod();
                            }else {
                                System.err.println("Não podem haver comandos após o return");
                            }
                        }
                        return true;
                    }
                }
            }
        } else  if (checkToken("}")) {
            checkClass();
        }
        return false;
    }

    private boolean checkFunctionBody() {
        return true;
    }

    private boolean checkReturn() throws Exception {
        if(checkToken("return")) {
            System.out.println("valor do return para análise: " + currentToken.getValue());
            if(currentClass.checkVariableType(currentToken.getValue(), getCurrentTypeMethod)) {
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
        if(checkToken("(")) {
            if(nativeTypes.contains(currentToken.getValue())){
                updateToken();
                if(checkToken(TokenTypes.IDENTIFIER)) {
                    if (checkToken(")")) {
                        return true;
                    }
                }
            }
        }

        return true;
    }
}