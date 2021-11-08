/**
 * TLS-Server-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2021 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner.probe;

import de.rub.nds.modifiablevariable.bytearray.ByteArrayModificationFactory;
import de.rub.nds.modifiablevariable.bytearray.ModifiableByteArray;
import de.rub.nds.modifiablevariable.util.ArrayConverter;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.CompressionMethod;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.protocol.message.ClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.HelloVerifyRequestMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloDoneMessage;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.ParallelExecutor;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceUtil;
import de.rub.nds.tlsattacker.core.workflow.action.ChangeConnectionTimeoutAction;
import de.rub.nds.tlsattacker.core.workflow.action.GenericReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveTillAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowConfigurationFactory;
import de.rub.nds.tlsattacker.core.workflow.factory.WorkflowTraceType;
import de.rub.nds.tlsscanner.serverscanner.config.ScannerConfig;
import de.rub.nds.tlsscanner.serverscanner.constants.ProbeType;
import static de.rub.nds.tlsscanner.serverscanner.probe.TlsProbe.LOGGER;
import de.rub.nds.tlsscanner.serverscanner.rating.TestResult;
import de.rub.nds.tlsscanner.serverscanner.report.SiteReport;
import de.rub.nds.tlsscanner.serverscanner.report.result.DtlsHelloVerifyRequestResult;
import de.rub.nds.tlsscanner.serverscanner.report.result.ProbeResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Nurullah Erinola - nurullah.erinola@rub.de
 */
public class DtlsHelloVerifyRequestProbe extends TlsProbe {

    private Integer cookieLength;

    public DtlsHelloVerifyRequestProbe(ScannerConfig scannerConfig, ParallelExecutor parallelExecutor) {
        super(parallelExecutor, ProbeType.DTLS_HELLO_VERIFY_REQUEST, scannerConfig);
    }

    @Override
    public ProbeResult executeTest() {
        try {
            return new DtlsHelloVerifyRequestResult(hasHvrRetransmissions(), checksCookie(), cookieLength,
                usesVersion(), usesRandom(), usesSessionId(), usesCiphersuites(), usesCompressions());
        } catch (Exception E) {
            LOGGER.error("Could not scan for " + getProbeName(), E);
            return new DtlsHelloVerifyRequestResult(TestResult.ERROR_DURING_TEST, TestResult.ERROR_DURING_TEST, -1,
                TestResult.ERROR_DURING_TEST, TestResult.ERROR_DURING_TEST, TestResult.ERROR_DURING_TEST,
                TestResult.ERROR_DURING_TEST, TestResult.ERROR_DURING_TEST);
        }
    }

    private TestResult hasHvrRetransmissions() {
        Config config = getConfig();
        config.setAddRetransmissionsToWorkflowTraceInDtls(true);
        WorkflowTrace trace =
            new WorkflowConfigurationFactory(config).createTlsEntryWorkflowTrace(config.getDefaultClientConnection());
        trace.addTlsAction(new SendAction(new ClientHelloMessage(config)));
        trace.addTlsAction(new ReceiveAction(new HelloVerifyRequestMessage()));
        trace.addTlsAction(new ChangeConnectionTimeoutAction(3000));
        trace.addTlsAction(new GenericReceiveAction());
        State state = new State(config, trace);
        executeState(state);
        if (WorkflowTraceUtil
            .getLastReceivedMessage(HandshakeMessageType.HELLO_VERIFY_REQUEST, state.getWorkflowTrace())
            .isRetransmission()) {
            return TestResult.TRUE;
        } else {
            return TestResult.FALSE;
        }
    }

    private TestResult checksCookie() {
        Config config = getConfig();
        config.setWorkflowTraceType(WorkflowTraceType.DYNAMIC_HELLO);
        State state = new State(config);
        executeState(state);
        if (WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO, state.getWorkflowTrace())) {
            if (state.getTlsContext().getDtlsCookie() != null) {
                cookieLength = state.getTlsContext().getDtlsCookie().length;
                if (cookieLength == 0) {
                    return TestResult.CANNOT_BE_TESTED;
                }
            }
        } else {
            return TestResult.ERROR_DURING_TEST;
        }
        int[] testPositions = new int[] { 0, cookieLength / 2, cookieLength - 1 };
        for (int totest : testPositions) {
            config = getConfig();
            WorkflowTrace trace = new WorkflowConfigurationFactory(config)
                .createTlsEntryWorkflowTrace(config.getDefaultClientConnection());
            trace.addTlsAction(new SendAction(new ClientHelloMessage(config)));
            trace.addTlsAction(new ReceiveAction(new HelloVerifyRequestMessage()));
            ClientHelloMessage clientHelloMessage = new ClientHelloMessage(config);
            ModifiableByteArray cookie = new ModifiableByteArray();
            cookie.setModification(ByteArrayModificationFactory.xor(ArrayConverter.hexStringToByteArray("FF"), totest));
            clientHelloMessage.setCookie(cookie);
            trace.addTlsAction(new SendAction(clientHelloMessage));
            trace.addTlsAction(new ReceiveTillAction(new ServerHelloDoneMessage(config)));
            state = new State(config, trace);
            if (getResult(state) == TestResult.FALSE) {
                return TestResult.FALSE;
            }
        }
        return TestResult.TRUE;
    }

    // problematic case: if server supports one version
    private TestResult usesVersion() {
        Config config = getConfig();
        WorkflowTrace trace =
            new WorkflowConfigurationFactory(config).createTlsEntryWorkflowTrace(config.getDefaultClientConnection());
        trace.addTlsAction(new SendAction(new ClientHelloMessage(config)));
        trace.addTlsAction(new ReceiveAction(new HelloVerifyRequestMessage()));
        ClientHelloMessage clientHelloMessage = new ClientHelloMessage(config);
        ModifiableByteArray protocolVersion = new ModifiableByteArray();
        protocolVersion.setModification(ByteArrayModificationFactory.explicitValue(ProtocolVersion.DTLS10.getValue()));
        clientHelloMessage.setProtocolVersion(protocolVersion);
        trace.addTlsAction(new SendAction(clientHelloMessage));
        trace.addTlsAction(new ReceiveTillAction(new ServerHelloDoneMessage(config)));
        State state = new State(config, trace);
        return getResult(state);
    }

    private TestResult usesRandom() {
        Config config = getConfig();
        WorkflowTrace trace =
            new WorkflowConfigurationFactory(config).createTlsEntryWorkflowTrace(config.getDefaultClientConnection());
        trace.addTlsAction(new SendAction(new ClientHelloMessage(config)));
        trace.addTlsAction(new ReceiveAction(new HelloVerifyRequestMessage(config)));
        ClientHelloMessage clientHelloMessage = new ClientHelloMessage(config);
        ModifiableByteArray random = new ModifiableByteArray();
        random.setModification(ByteArrayModificationFactory.xor(ArrayConverter.hexStringToByteArray("FFFF"), -2));
        clientHelloMessage.setRandom(random);
        trace.addTlsAction(new SendAction(clientHelloMessage));
        trace.addTlsAction(new ReceiveTillAction(new ServerHelloDoneMessage(config)));
        State state = new State(config, trace);
        return getResult(state);
    }

    private TestResult usesSessionId() {
        Config config = getConfig();
        WorkflowTrace trace =
            new WorkflowConfigurationFactory(config).createTlsEntryWorkflowTrace(config.getDefaultClientConnection());
        trace.addTlsAction(new SendAction(new ClientHelloMessage(config)));
        trace.addTlsAction(new ReceiveAction(new HelloVerifyRequestMessage(config)));
        ClientHelloMessage clientHelloMessage = new ClientHelloMessage(config);
        ModifiableByteArray sessionId = new ModifiableByteArray();
        sessionId
            .setModification(ByteArrayModificationFactory.explicitValue(ArrayConverter.hexStringToByteArray("FFFF")));
        clientHelloMessage.setSessionId(sessionId);
        trace.addTlsAction(new SendAction(clientHelloMessage));
        trace.addTlsAction(new ReceiveTillAction(new ServerHelloDoneMessage(config)));
        State state = new State(config, trace);
        return getResult(state);
    }

    private TestResult usesCiphersuites() {
        Config config = getConfig();
        WorkflowTrace trace =
            new WorkflowConfigurationFactory(config).createTlsEntryWorkflowTrace(config.getDefaultClientConnection());
        trace.addTlsAction(new SendAction(new ClientHelloMessage(config)));
        trace.addTlsAction(new ReceiveAction(new HelloVerifyRequestMessage(config)));
        ClientHelloMessage clientHelloMessage = new ClientHelloMessage(config);
        ModifiableByteArray ciphersuites = new ModifiableByteArray();
        ciphersuites.setModification(ByteArrayModificationFactory.delete(1, 2));
        clientHelloMessage.setCipherSuites(ciphersuites);
        trace.addTlsAction(new SendAction(clientHelloMessage));
        trace.addTlsAction(new ReceiveTillAction(new ServerHelloDoneMessage(config)));
        State state = new State(config, trace);
        return getResult(state);
    }

    private TestResult usesCompressions() {
        Config config = getConfig();
        WorkflowTrace trace =
            new WorkflowConfigurationFactory(config).createTlsEntryWorkflowTrace(config.getDefaultClientConnection());
        trace.addTlsAction(new SendAction(new ClientHelloMessage(config)));
        trace.addTlsAction(new ReceiveAction(new HelloVerifyRequestMessage(config)));
        ClientHelloMessage clientHelloMessage = new ClientHelloMessage(config);
        ModifiableByteArray compressions = new ModifiableByteArray();
        compressions.setModification(ByteArrayModificationFactory.delete(-1, 1));
        clientHelloMessage.setCompressions(compressions);
        trace.addTlsAction(new SendAction(clientHelloMessage));
        trace.addTlsAction(new ReceiveTillAction(new ServerHelloDoneMessage(config)));
        State state = new State(config, trace);
        return getResult(state);
    }

    private TestResult getResult(State state) {
        executeState(state);
        if (WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.HELLO_VERIFY_REQUEST, state.getWorkflowTrace())) {
            if (WorkflowTraceUtil.didReceiveMessage(HandshakeMessageType.SERVER_HELLO, state.getWorkflowTrace())) {
                return TestResult.FALSE;
            } else {
                return TestResult.TRUE;
            }
        } else {
            return TestResult.CANNOT_BE_TESTED;
        }
    }

    private Config getConfig() {
        Config config = getScannerConfig().createConfig();
        config.setHighestProtocolVersion(ProtocolVersion.DTLS12);
        List<CipherSuite> ciphersuites = new LinkedList<>();
        ciphersuites.addAll(Arrays.asList(CipherSuite.values()));
        ciphersuites.remove(CipherSuite.TLS_FALLBACK_SCSV);
        ciphersuites.remove(CipherSuite.TLS_EMPTY_RENEGOTIATION_INFO_SCSV);
        config.setDefaultClientSupportedCipherSuites(ciphersuites);
        List<CompressionMethod> compressionList = new ArrayList<>(Arrays.asList(CompressionMethod.values()));
        config.setDefaultClientSupportedCompressionMethods(compressionList);
        config.setEnforceSettings(false);
        config.setQuickReceive(true);
        config.setEarlyStop(true);
        config.setStopReceivingAfterFatal(true);
        config.setStopActionsAfterFatal(true);
        config.setStopActionsAfterIOException(true);
        config.setAddECPointFormatExtension(true);
        config.setAddEllipticCurveExtension(true);
        config.setAddServerNameIndicationExtension(true);
        config.setAddSignatureAndHashAlgorithmsExtension(true);
        return config;
    }

    @Override
    public boolean canBeExecuted(SiteReport report) {
        return true;
    }

    @Override
    public ProbeResult getCouldNotExecuteResult() {
        return new DtlsHelloVerifyRequestResult(TestResult.COULD_NOT_TEST, TestResult.COULD_NOT_TEST, -1,
            TestResult.COULD_NOT_TEST, TestResult.COULD_NOT_TEST, TestResult.COULD_NOT_TEST, TestResult.COULD_NOT_TEST,
            TestResult.COULD_NOT_TEST);
    }

    @Override
    public void adjustConfig(SiteReport report) {
    }

}
