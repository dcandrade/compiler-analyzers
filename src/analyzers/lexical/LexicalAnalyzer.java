package analyzers.lexical;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class LexicalAnalyzer {
    private final Map<Integer, String> symbolTable; // int to lexeme

    public LexicalAnalyzer() {
        this.symbolTable = new HashMap<>();
    }


    public static void main(String[] args) {
        String path = "tests/entrada_exemplo_teste_lexico.txt";

        try {
            Files.lines(Paths.get(path))
                    .forEach(System.out::println);
        } catch (IOException e) {
            System.err.println("File not found ("+path+")");
        }
    }
}
