package com.memdiag.cli;

import com.memdiag.cli.commands.HistogramCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "memdiag",
         subcommands = {HistogramCommand.class},
         description = "JVM Memory Diagnosis Tool")
public class MemDiagCli {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new MemDiagCli()).execute(args);
        System.exit(exitCode);
    }
}
