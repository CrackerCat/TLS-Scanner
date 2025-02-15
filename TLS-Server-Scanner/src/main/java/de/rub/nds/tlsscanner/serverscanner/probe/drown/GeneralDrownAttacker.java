/**
 * TLS-Server-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner.probe.drown;

import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.RunningModeType;
import de.rub.nds.tlsattacker.core.constants.SSL2CipherSuite;
import de.rub.nds.tlsattacker.core.protocol.message.SSL2ClientMasterKeyMessage;
import de.rub.nds.tlsattacker.core.protocol.message.SSL2ServerHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.SSL2ServerVerifyMessage;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.ParallelExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowExecutorFactory;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowConfigurationFactory;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlsscanner.serverscanner.probe.drown.constans.DrownVulnerabilityType;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeneralDrownAttacker extends BaseDrownAttacker {

    private static final Logger LOGGER = LogManager.getLogger();

    public GeneralDrownAttacker(Config baseConfig, ParallelExecutor executor) {
        super(baseConfig, executor);
    }

    @Override
    public DrownVulnerabilityType getDrownVulnerabilityType() {
        WorkflowTrace trace = new WorkflowConfigurationFactory(tlsConfig)
            .createWorkflowTrace(WorkflowTraceType.SSL2_HELLO, RunningModeType.CLIENT);
        trace.addTlsAction(new SendAction(new SSL2ClientMasterKeyMessage()));
        trace.addTlsAction(new ReceiveAction(new SSL2ServerVerifyMessage()));
        State state = new State(tlsConfig, trace);
        WorkflowExecutor workflowExecutor =
            WorkflowExecutorFactory.createWorkflowExecutor(tlsConfig.getWorkflowExecutorType(), state);
        workflowExecutor.executeWorkflow();

        // See if the server talks SSLv2 at all
        if (!WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SSL2_SERVER_HELLO, trace)) {
            return DrownVulnerabilityType.NONE;
        }

        // See if export ciphers are announced
        SSL2ServerHelloMessage serverHello = (SSL2ServerHelloMessage) WorkflowTraceUtil
            .getFirstReceivedMessage(HandshakeMessageType.SSL2_SERVER_HELLO, trace);
        List<SSL2CipherSuite> serverCipherSuites =
            SSL2CipherSuite.getCipherSuites(serverHello.getCipherSuites().getValue());
        for (SSL2CipherSuite cipherSuite : serverCipherSuites) {
            if (cipherSuite.isWeak()) {
                LOGGER.debug("Declaring host as vulnerable based on weak cipher suite in ServerHello.");
                return DrownVulnerabilityType.GENERAL;
            }
        }

        // See if server supports export ciphers even though they have not
        // been announced (CVE-2015-3197)
        SSL2ServerVerifyMessage message = (SSL2ServerVerifyMessage) WorkflowTraceUtil
            .getFirstReceivedMessage(HandshakeMessageType.SSL2_SERVER_VERIFY, trace);
        if (message != null && ServerVerifyChecker.check(message, state.getTlsContext(), false)) {
            LOGGER.debug("Declaring host as vulnerable based on export cipher suite selection (CVE-2015-3197).");
            return DrownVulnerabilityType.GENERAL;
        }

        return DrownVulnerabilityType.SSL2;
    }
}
