package com.memdiag.cli;

import com.memdiag.cli.commands.AgentCommand;
import com.memdiag.cli.commands.DiagnoseCommand;
import com.memdiag.cli.commands.DiffCommand;
import com.memdiag.cli.commands.GcRootsCommand;
import com.memdiag.cli.commands.HistogramCommand;
import com.memdiag.cli.commands.NativeCommand;
import com.memdiag.cli.commands.NmtCommand;
import com.memdiag.cli.commands.ReportCommand;
import com.memdiag.cli.commands.SnapshotCommand;
import com.memdiag.cli.commands.ThreadsCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "memdiag",
         subcommands = {HistogramCommand.class, ThreadsCommand.class, DiagnoseCommand.class,
                        NativeCommand.class, ReportCommand.class, NmtCommand.class,
                        SnapshotCommand.class, DiffCommand.class, GcRootsCommand.class,
                        AgentCommand.class},
         description = "JVM Memory Diagnosis Tool",
         mixinStandardHelpOptions = true)
public class MemDiagCli {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new MemDiagCli()).execute(args);
        System.exit(exitCode);
    }
}
