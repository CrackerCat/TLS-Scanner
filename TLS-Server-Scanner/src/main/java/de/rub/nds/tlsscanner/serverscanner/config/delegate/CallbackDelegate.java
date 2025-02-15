/**
 * TLS-Server-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner.config.delegate;

import com.beust.jcommander.Parameter;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.config.delegate.Delegate;
import de.rub.nds.tlsattacker.core.exceptions.ConfigurationException;
import de.rub.nds.tlsattacker.core.state.State;
import java.io.IOException;
import java.util.function.Function;

public class CallbackDelegate extends Delegate {

    @Parameter(names = "-beforeTransportPreInitCb", required = false,
        description = "The shell command the scanner should run before the pre initialization of the transport handler.")
    private String beforeTransportPreInitCommand = null;

    @Parameter(names = "-beforeTransportInitCb", required = false,
        description = "The shell command the scanner should run before the initialization of the transport handler.")
    private String beforeTransportInitCommand = null;

    @Parameter(names = "-afterTransportInitCb", required = false,
        description = "The shell command the scanner should run after the initialization of the transport handler.")
    private String afterTransportInitCommand = null;

    @Parameter(names = "-afterExecutionCb", required = false,
        description = "The shell command the scanner should run after the worklfow execution.")
    private String afterExecutionCommand = null;

    public CallbackDelegate() {
    }

    public String getBeforeTransportPreInitCommand() {
        return beforeTransportPreInitCommand;
    }

    public String getBeforeTransportInitCommand() {
        return beforeTransportInitCommand;
    }

    public String getAfterTransportInitCommand() {
        return afterTransportInitCommand;
    }

    public String getAfterExecutionCommand() {
        return afterExecutionCommand;
    }

    @Override
    public void applyDelegate(Config config) throws ConfigurationException {
    }

    public Function<State, Integer> getBeforeTransportPreInitCallback() {
        return getCallback(beforeTransportPreInitCommand);
    }

    public Function<State, Integer> getBeforeTransportInitCallback() {
        return getCallback(beforeTransportInitCommand);
    }

    public Function<State, Integer> getAfterTransportInitCallback() {
        return getCallback(afterTransportInitCommand);
    }

    public Function<State, Integer> getAfterExecutionCallback() {
        return getCallback(afterExecutionCommand);
    }

    private Function<State, Integer> getCallback(String command) {
        if (command == null) {
            return null;
        }
        return (State state) -> {
            try {
                Runtime.getRuntime().exec(command.split(" "));
                LOGGER.debug("Running command: {}", command);
            } catch (IOException E) {
                LOGGER.error("Error during command execution", E);
            }
            return 0;
        };
    }
}
