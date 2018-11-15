package me.zeroeightsix.jmm;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.*;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

@CommandLine.Command(name = "jmm", mixinStandardHelpOptions = true, version = "jmm v0.1")
public class JMM implements Callable<Boolean> {

    @CommandLine.Option(names = { "-v", "--verbose" }, description = "Verbose mode.")
    private boolean verbose = false;

    @CommandLine.Option(names = { "-fv", "--fineverbose" }, description = "Fine verbose mode.")
    private boolean fine = false;

    @CommandLine.Option(names = { "-r", "--recursive" }, description = "Recursively check directories")
    private boolean recursive = false;

    @CommandLine.Option(names = {"-n", "--noact"}, description = "Do everything but act on files")
    private boolean noAct = false;

    @CommandLine.Option(names = {"-c", "--confirm"}, description = "Ask for confirmation upon acting on a file")
    private boolean confirmationRequired = false;

    @CommandLine.Option(names = {"-o", "--no-overwrite"}, description = "Do not overwrite files")
    private boolean noOverwrite = false;

    @CommandLine.Option(names = {"-s", "--stacks"}, description = "Print stack traces")
    private boolean stacks = false;

    @CommandLine.Option(names = {"-d", "--directory"}, description = "Defines the working directory of JMM")
    private String directory = "";

    @CommandLine.Parameters(index = "0", arity = "1", paramLabel = "SELECTOR", description = "Regex to find targeted files")
    private String selector;

    @CommandLine.Parameters(index = "1", arity = "1", paramLabel = "TARGET", description = "Regex to rename files to")
    private String target;

    @CommandLine.Parameters(index = "2", arity = "1..*", paramLabel = "PROCESSORS", description = "Reference modifiers")
    private ReferenceProcessor[] processors;

    private static final Pattern REFERENCE_PATTERN = Pattern.compile("\\$(\\d)+");

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        int exitCode = 1;
        try {
            Boolean result = CommandLine.call(new JMM(), args);
            exitCode = (result == null || result) ? 0 : exitCode;
        } finally {
            AnsiConsole.systemUninstall();
        }
        System.exit(exitCode);
    }

    /**
     * @return true if execution went well
     */
    @Override
    public Boolean call() {
        final Path here = Paths.get(directory);

        UnaryOperator<String> processor;
        if (processors == null || processors.length == 0) processor = s -> s;
        else {
            processor = s -> {
                for (ReferenceProcessor referenceProcessor : processors) s = referenceProcessor.unaryOperator.apply(s);
                return s;
            };
        }

        final BiConsumer<Path, Path> actor = (from, to) -> {
            Path relative = here;
            from = relative.relativize(from);
            to = relative.relativize(to);

            if (confirmationRequired &&
                    !ask(ansi()
                            .fg(WHITE).a("Move ")
                            .fg(CYAN).a(from)
                            .fg(WHITE).a(" to ")
                            .fg(CYAN).a(to)
                            .fg(WHITE).a("?")
                            .reset())) {
                return;
            }
            if (Files.exists(to) && noOverwrite) {
                if (verbose) System.out.println(ansi().fg(RED).a("Skipping " + to + ": file exists"));
                return;
            }
            if (noAct || verbose)
                System.out.println(ansi().fg(CYAN).a(from).fg(DEFAULT).a(" -> ").fg(CYAN).a(to).reset());
            if (noAct)
                return;
            try {
                Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                if (verbose) {
                    if (stacks)
                        e.printStackTrace();
                    else
                        System.err.println("Failed to move (" + e.getMessage() + ")");
                }
            }
        };

        if (fine) {
            verbose = true;
            System.out.println("Fine logging is ON.");
        }

        if (!Files.isDirectory(here)) {
            System.err.println("No such directory: " + directory);
            return false;
        } else if (verbose) {
            System.out.println("Working directory: " + here.toAbsolutePath());
        }

        final AtomicInteger acted = new AtomicInteger(0);

        try (Stream<Path> paths = Files.walk(here, recursive ? Integer.MAX_VALUE : 1)) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        boolean match = path.getFileName().toString().matches(selector);
                        if (fine) {
                            System.out.println((match ? "matches:  " : "no match: ") + path.toAbsolutePath());
                        }
                        return match;
                    })
                    .distinct()
                    .forEach(path -> {
                        String to = replaceAllWithMethod(path.getFileName().toString(), selector, target, processor);
//                        String to = path.getFileName().toString().replaceAll(selector, target);
                        if (fine) {
                            System.out.println(path.getFileName() + ": " + to);
                        }
                        actor.accept(path, path.resolveSibling(to));
                        acted.addAndGet(1);
                    });
        } catch (Exception e) {
            if (verbose) {
                if (stacks) {
                    e.printStackTrace();
                } else {
                    System.err.println("Exception traversing path: " + e.getMessage());
                }
            }
        }

        final int a = acted.get();
        if (verbose) {
            if (a == 0) {
                System.err.println("No files were affected.");
            } else {
                System.out.println(ansi().fg(WHITE).a(a).fg(DEFAULT).a(" file(s) affected.").reset());
            }
        }

        return true;
    }

    public static boolean ask(Ansi question) {
        return ask(question.fg(YELLOW).a(" (y/n) ").reset().toString(), "y", "n");
    }

    public static boolean ask(String question, String positive, String negative) {
        Scanner input = new Scanner(System.in);
        positive = positive.toLowerCase();
        negative = negative.toLowerCase();
        String def = "";
        String answer;
        do {
            System.out.print(question);
            answer = input.nextLine().trim().toLowerCase();
        } while (!answer.matches(positive) && !answer.matches(negative) && !answer.matches(def));
        return answer.matches(def) || answer.matches(positive);
    }

    static String replaceAllWithMethod(String data, String regex, String replacement, UnaryOperator<String> operator) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(data);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            // Find backreferences (e.g. $1)
            Matcher second = REFERENCE_PATTERN.matcher(replacement);
            StringBuffer sb2 = new StringBuffer();
            while (second.find()) {
                // Find the group number of the reference (e.g. $1 -> 1), parse it
                int groupNumber = Integer.parseInt(second.group(1));
                // Replace the reference with the group with the same number from our current group in the given string
                // e.g.
                //  data = "a b b c d"
                //  regex = "(b) b"
                //  replacement = "$1"
                //  operator = s -> s.toUpperCase()
                // then:
                //  current group: b b
                //  groupNumber: 1 (from replacement: $1)
                //  group #1: b
                //      -> with operator: B
                // -> replacement: "$1" -> "B"
                second.appendReplacement(sb2, operator.apply(matcher.group(groupNumber)));
            }
            second.appendTail(sb2);

            matcher.appendReplacement(sb, sb2.toString());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

}
