package model;

import exceptions.TokenClassificationException;

import java.util.*;
import java.util.regex.Pattern;

public class LexemeClassifier {
    private final static String RESERVED_WORD = "PRE";
    private final static String IDENTIFIER = "IDE";
    private final static String NUMBER = "NRO";
    private final static String DELIMITER = "DEL";
    private final static String RELATIONAL_OPERATOR = "REL";
    private final static String LOGICAL_OPERATOR = "LOG";
    private final static String ARITIMETICAL_OPERATOR = "ART";
    private final static String LINE_COMMENT = "LCO";
    private final static String BLOCK_COMMENT = "BCO";


    private final String SYMBOL_REGEX = "";
    private final String SPACE_REGEX = " |\t";
    private final String DIGIT_REGEX = "[0-9]";
    private final String LETTER_REGEX = "[a-z]|[A-Z]";
    private final String ARITHMETICAL_OPERATOR_REGEX = "+|-|*|/|++|--";
    private final String RELATIONAL_OPERATOR_REGEX = "!=|==|<|<=|>|>=|=";
    private final String RESERVERD_WORD_REGEX = "class|const|variables|method|return|main|if|then|else|while|read|write|void|int|float|bool|string|true|false|extends";
    private final String IDENTIFIER_REGEX = "";
    private final String NUMBER_REGEX = "(-)?"+SPACE_REGEX+"*"+DIGIT_REGEX+"+"+"(."+DIGIT_REGEX +"+"+")";
    private final String LOGICAL_OPERATOR_REGEX = "!|&&|\\|\\|";
    //private final String COMMENT_REGEX = "//(.)*|";
    private final String DELIMITER_REGEX = ";|,|(|)|[|]|{|}|.";
    private final String STRING_REGEX = "\"(" + LETTER_REGEX + "\\|" + DIGIT_REGEX + "\\|" + SYMBOL_REGEX + ")*\"";
    private final String BLOCK_COMMENT_REGEX = "^/*";
    private final String LINE_COMMENT_REGEX = "^//";

    private final Map<String, String> categories2Regex;

    public LexemeClassifier(){
        this.categories2Regex = new LinkedHashMap<>(); // MUST be linked hash map to preserve order of insertion

        this.categories2Regex.put(LexemeClassifier.RESERVED_WORD, this.RESERVERD_WORD_REGEX);
        this.categories2Regex.put(LexemeClassifier.IDENTIFIER, this.IDENTIFIER_REGEX);
        this.categories2Regex.put(LexemeClassifier.NUMBER, this.NUMBER_REGEX);
        this.categories2Regex.put(LexemeClassifier.DELIMITER, this.DELIMITER_REGEX);
        this.categories2Regex.put(LexemeClassifier.RELATIONAL_OPERATOR, this.RELATIONAL_OPERATOR_REGEX);
        this.categories2Regex.put(LexemeClassifier.LOGICAL_OPERATOR, this.LOGICAL_OPERATOR_REGEX);
        this.categories2Regex.put(LexemeClassifier.ARITIMETICAL_OPERATOR, this.ARITHMETICAL_OPERATOR_REGEX);
        this.categories2Regex.put(LexemeClassifier.BLOCK_COMMENT, this.BLOCK_COMMENT_REGEX);
        this.categories2Regex.put(LexemeClassifier.LINE_COMMENT, this.LINE_COMMENT_REGEX);
    }

    public String classify(String token) throws TokenClassificationException {

        for (Map.Entry<String, String> entry : this.categories2Regex.entrySet()) {
            String category = entry.getKey();
            String regex = entry.getValue();

            if (Pattern.matches(token, regex)) {
                return category;
            }
        }

        throw new TokenClassificationException();
    }

}
