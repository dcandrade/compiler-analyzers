package main;

import analyzers.lexical.LexicalAnalyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Compiler {
    public static void main(String[] args) throws IOException {
        LexicalAnalyzer analyzer = new LexicalAnalyzer();
        String path = "tests/entrada_exemplo_teste_lexico.txt";

        Files.lines(Paths.get(path))
                .forEach(analyzer::processLine);
    }
}
