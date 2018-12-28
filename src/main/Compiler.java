package main;

import analyzers.lexical.LexicalAnalyzer;
import analyzers.semantic.SemanticAnalyzer;
import analyzers.syntatical.SyntacticalAnalyzer;
import model.error.LexicalError;
import model.error.SemanticError;
import model.error.SyntaxError;
import model.token.Token;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Compiler {
    public static void main(String[] args) throws Exception {
        List<Path> inputs = Files.list(Paths.get("tests")).filter(x -> !x.getFileName().toString().startsWith("_")).collect(Collectors.toList());
        for (Path file : inputs) {

            Files.createDirectories(Paths.get("output", "lexico"));
            Files.createDirectories(Paths.get("output", "sintatico"));
            Files.createDirectories(Paths.get("output", "semantico"));

            Path lexerOutputFile = Paths.get("output", "lexico", file.getFileName().toString());
            System.out.println(" -->> Processando arquivo " + file);

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

            if (lexer.getLexicalErrors().size() > 0) {
                System.err.println("-- Erros Léxicos. Processo de compilação interrompido.");
                return;
            }
            System.out.println("Léxico OK");


            Path parserOutputFile = Paths.get("output", "sintatico", file.getFileName().toString());
            BufferedWriter parserOutput = Files.newBufferedWriter(parserOutputFile);

            SyntacticalAnalyzer parser = new SyntacticalAnalyzer(lexer.getTokens());
            parser.parseProgram();

            for (SyntaxError error : parser.getErrors()) {
                parserOutput.write(error + "\n");
            }
            parserOutput.close();

            if (parser.getErrors().size() > 0) {
                System.err.println("-- Erros Sintáticos. Processo de compilação interrompido.");
                return;
            }

            System.out.println("Sintático OK ");

            Path semanticOutputFile = Paths.get("output", "semantico", file.getFileName().toString());
            BufferedWriter semanticOutput = Files.newBufferedWriter(semanticOutputFile);

            SemanticAnalyzer semantic = new SemanticAnalyzer(lexer.getTokens());

            for (SemanticError error : semantic.getErrors()) {
                semanticOutput.write(error + "\n");
            }

            semanticOutput.close();

            System.out.println("Semântico OK ");


        }
    }
}
