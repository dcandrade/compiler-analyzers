package main;

import analyzers.lexical.LexicalAnalyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Compiler {
    public static void main(String[] args) throws IOException {
        String path = "tests/entrada_exemplo_teste_lexico.txt";
        LexicalAnalyzer analyzer = new LexicalAnalyzer(path);

        analyzer.forEach(System.out::println);
        System.out.println("\n\n");
        analyzer.getErrors().forEach(System.out::println);
    }
}
