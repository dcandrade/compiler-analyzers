package model.token;

public class TokenTypes {
    public final static String SPACE = "ESP";
    public final static String RESERVED_WORD = "PRE";
    public final static String IDENTIFIER = "IDE";
    public final static String NUMBER = "NRO";
    public final static String DELIMITER = "DEL";
    public final static String RELATIONAL_OPERATOR = "REL";
    public final static String LOGICAL_OPERATOR = "LOG";
    public final static String ARITHMETICAL_OPERATOR = "ART";
    public final static String LINE_COMMENT = "LCO";
    public final static String STRING = "CDC";
    public final static String BLOCK_COMMENT_START = "CBC";
    public final static String BLOCK_COMMENT_END = "FBC";
    public final static String INVALID_TOKEN = "ITO";
    public final static String NO_MORE_TOKENS = "EOF";
    public final static String BOOLEAN = "BOL";
    public final static String NUMBER_FLOAT = "NFL";
    public final static String NUMBER_INT = "NIN";
    public final static String UNDEFINED = "UNDEFINED";


    public static final String[] PRIMITIVE_TYPES =
            {RESERVED_WORD, NUMBER, ARITHMETICAL_OPERATOR, LOGICAL_OPERATOR, RELATIONAL_OPERATOR, DELIMITER};
}
