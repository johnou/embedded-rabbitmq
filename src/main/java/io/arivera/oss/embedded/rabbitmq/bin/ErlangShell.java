package io.arivera.oss.embedded.rabbitmq.bin;

import io.arivera.oss.embedded.rabbitmq.EmbeddedRabbitMqConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Level;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A wrapper for the command "<code>{@value ErlangShell#UNIX_ERL_COMMAND}</code>", used for checking/testing the Erlang version.
 */
public class ErlangShell {
  private static final String LOGGER_TEMPLATE = "%s.Process.%s";

  private static final String UNIX_ERL_COMMAND = "erl";

  private final EmbeddedRabbitMqConfig config;

  /**
   * Generic Constructor.
   */
  public ErlangShell(final EmbeddedRabbitMqConfig config) {
    this.config = config;

  }

  /**
   * @return a String representing the Erlang version, such as {@code "18.2.1"}
   * @throws ErlangShellException if the Erlang command can't be executed or if it exits unexpectedly.
   */
  public String getErlangVersion() throws ErlangShellException {
    String erlangShell = UNIX_ERL_COMMAND;

    Logger processOutputLogger = LoggerFactory.getLogger(
        String.format(LOGGER_TEMPLATE, this.getClass().getName(), erlangShell));

    Slf4jStream stream = Slf4jStream.of(processOutputLogger);

    final ProcessExecutor processExecutor = config.getProcessExecutorFactory().createInstance()
        .command(erlangShell, "-noshell", "-eval", "erlang:display(erlang:system_info(otp_release)), halt().")
        .timeout(config.getErlangCheckTimeoutInMillis(), TimeUnit.MILLISECONDS)
        .redirectError(stream.as(Level.WARN))
        .destroyOnExit()
        .readOutput(true);

    try {
      ProcessResult processResult = processExecutor.execute();
      int exitValue = processResult.getExitValue();
      if (exitValue == 0) {
        return processResult.outputUTF8().trim().replaceAll("[\"\\\\n]", ""); // "18.2.1\n" -> "18.2.1"
      } else {
        throw new ErlangShellException("Erlang exited with status " + exitValue);
      }
    } catch (IOException | InterruptedException | TimeoutException e) {
      throw new ErlangShellException("Exception executing Erlang shell command", e);
    }
  }
}
