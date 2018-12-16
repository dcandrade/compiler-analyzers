package main;

import analyzers.lexical.LexicalAnalyzer;
import analyzers.semantic.SemanticAnalyzer;
import analyzers.syntatical.SyntacticalAnalyzer;
import model.error.LexicalError;
import model.error.SyntaxError;
import model.token.Token;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Compiler {
    public static void main(String[] args) throws Exception {
        List<Path> inputs = Files.list(Paths.get("tests")).collect(Collectors.toList());
        for (Path file : inputs) {

            Files.createDirectories(Paths.get("output", "lexico"));
            Files.createDirectories(Paths.get("output", "sintatico"));

            Path lexerOutputFile = Paths.get("output",  "lexico", file.getFileName().toString());

            BufferedWriter lexerOutput = Files.newBufferedWriter(lexerOutputFile);
            LexicalAnalyzer lexer = new LexicalAnalyzer(file);

            for (Token token : lexer.getTokens()) {
                lexerOutput.write(token + "\n");
            }

            lexerOutput.write("\n\n");

            for (LexicalError lexicalError : lexer.getLexicalErrors()) {
                lexerOutput.write(lexicalError + "\n");
            }

            lexerOutput.close();

            Path parserOutputFile = Paths.get("output",  "sintatico", file.getFileName().toString());
            BufferedWriter parserOutput = Files.newBufferedWriter(parserOutputFile);

            SyntacticalAnalyzer parser = new SyntacticalAnalyzer(lexer.getTokens());
            parser.parseProgram();

            for (SyntaxError error : parser.getErrors()) {
                parserOutput.write(error + "\n");
            }

            System.out.println(" -- Arquivo " + lexerOutputFile + " processado");

            parserOutput.close();

            SemanticAnalyzer semantic = new SemanticAnalyzer(lexer.getTokens());
        }
    }
}
