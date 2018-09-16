package model;

import exceptions.TokenClassificationException;

import java.util.*;
import java.util.regex.Pattern;

public class LexemeClassifier {
    public final static String RESERVED_WORD = "PRE";
    public final static String IDENTIFIER = "IDE";
    public final static String NUMBER = "NRO";
    public final static String DELIMITER = "DEL";
    public final static String RELATIONAL_OPERATOR = "REL";
    public final static String LOGICAL_OPERATOR = "LOG";
    public final static String ARITHMETICAL_OPERATOR = "ART";
    public final static String LINE_COMMENT = "LCO";
    public final static String BLOCK_COMMENT = "BCO";
    public final static String STRING = "CDC";

    private final static String[] PRIMITIVE_TYPES = {RESERVED_WORD, NUMBER, ARITHMETICAL_OPERATOR, LOGICAL_OPERATOR,
            RELATIONAL_OPERATOR, DELIMITER};


    private String SYMBOL_REGEX;
    private String STRING_REGEX;
    private static final String SPACE_REGEX = "[ \t]";
    private static final String DIGIT_REGEX = "[0-9]";
    private static final String LETTER_REGEX = "[a-z]|[A-Z]";
    private static final String ARITHMETICAL_OPERATOR_REGEX = "\\+|-|\\*|/|\\+\\+|--";
    private static final String RELATIONAL_OPERATOR_REGEX = "!=|==|\\<|\\<=|\\>|\\>=|=";
    private static final String RESERVERD_WORD_REGEX = "class|const|variables|method|return|main|if|then|else|while|read|write|void|int|float|bool|string|true|false|extends";
    private static final String IDENTIFIER_REGEX = LETTER_REGEX + "(" + LETTER_REGEX + "|" + DIGIT_REGEX + "|_)*";
    private static final String NUMBER_REGEX = "(-)?(" + SPACE_REGEX + ")*" + DIGIT_REGEX + "+" + "(." + DIGIT_REGEX + "+" + ")";
    private static final String LOGICAL_OPERATOR_REGEX = "!|&&|\\|\\|";
    private static final String DELIMITER_REGEX = ";|,|\\(|\\)|\\[|\\]|\\{|\\}|\\.";
    private static final String BLOCK_COMMENT_REGEX = "^/\\*";
    private static final String LINE_COMMENT_REGEX = "^//";

    private final Map<String, String> categories2Regex;

    public LexemeClassifier() {
        this.generatePendingRegexes();
        this.categories2Regex = new LinkedHashMap<>(); // MUST be linked hash map to preserve order of insertion
        this.populateClassificationMap();
    }

    public String classify(String token) throws TokenClassificationException {

        for (Map.Entry<String, String> entry : this.categories2Regex.entrySet()) {
            String category = entry.getKey();
            String regex = entry.getValue();

            if (Pattern.matches(regex, token)) {
                return category;
            }
        }

        throw new TokenClassificationException();
    }

    public Optional<String> checkForPrimitiveTypes(String token) {
        for (String type : LexemeClassifier.PRIMITIVE_TYPES) {
            if (this.checkTokenType(token, type)) {
                return Optional.of(type);
            }
        }

        return Optional.empty();
    }

    public boolean checkTokenType(String token, String type) {
        String regex = this.categories2Regex.get(type);

        return regex != null && Pattern.matches(regex, token);
    }

    private void populateClassificationMap() {
        this.categories2Regex.put(LexemeClassifier.RESERVED_WORD, LexemeClassifier.RESERVERD_WORD_REGEX);
        this.categories2Regex.put(LexemeClassifier.IDENTIFIER, LexemeClassifier.IDENTIFIER_REGEX);
        this.categories2Regex.put(LexemeClassifier.NUMBER, LexemeClassifier.NUMBER_REGEX);
        this.categories2Regex.put(LexemeClassifier.DELIMITER, LexemeClassifier.DELIMITER_REGEX);
        this.categories2Regex.put(LexemeClassifier.RELATIONAL_OPERATOR, LexemeClassifier.RELATIONAL_OPERATOR_REGEX);
        this.categories2Regex.put(LexemeClassifier.LOGICAL_OPERATOR, LexemeClassifier.LOGICAL_OPERATOR_REGEX);
        this.categories2Regex.put(LexemeClassifier.ARITHMETICAL_OPERATOR, LexemeClassifier.ARITHMETICAL_OPERATOR_REGEX);
        this.categories2Regex.put(LexemeClassifier.BLOCK_COMMENT, LexemeClassifier.BLOCK_COMMENT_REGEX);
        this.categories2Regex.put(LexemeClassifier.LINE_COMMENT, LexemeClassifier.LINE_COMMENT_REGEX);
    }

    private void generatePendingRegexes() {
        this.SYMBOL_REGEX = this.generateSymbolRegex();
        this.STRING_REGEX = "\"(" + LETTER_REGEX + "\\|" + DIGIT_REGEX + "\\|" + SYMBOL_REGEX + ")*\"";
    }

    private String generateSymbolRegex() {
        StringBuilder symbolRegexBuilder = new StringBuilder();
        for (int ascii_index = 32; ascii_index <= 126; ascii_index++) {
            if (ascii_index != 34) {
                symbolRegexBuilder.append((char) ascii_index);
                symbolRegexBuilder.append("|");
            }
        }
        symbolRegexBuilder.deleteCharAt(symbolRegexBuilder.length() - 1); //deleting last |

        return symbolRegexBuilder.toString();
    }

    public static void main(String[] args) {
        LexemeClassifier classifier = new LexemeClassifier();
        System.out.println(LexemeClassifier.NUMBER_REGEX);
        System.out.println(Pattern.matches(LexemeClassifier.NUMBER_REGEX, "a12.344444"));
    }

}
