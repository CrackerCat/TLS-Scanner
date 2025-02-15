/**
 * TLS-Scanner-Core - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.core.vector.response;

import de.rub.nds.tlsattacker.core.constants.ProtocolMessageType;
import de.rub.nds.tlsattacker.core.protocol.ProtocolMessage;
import de.rub.nds.tlsattacker.core.record.AbstractRecord;
import de.rub.nds.tlsattacker.core.record.Record;
import de.rub.nds.tlsattacker.core.state.State;
import de.rub.nds.tlsattacker.core.workflow.action.ReceivingAction;
import de.rub.nds.tlsattacker.transport.socket.SocketState;
import de.rub.nds.tlsattacker.transport.tcp.ClientTcpTransportHandler;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResponseExtractor {

    private static final Logger LOGGER = LogManager.getLogger();

    public static ResponseFingerprint getFingerprint(State state, ReceivingAction action) {
        List<ProtocolMessage> messageList = action.getReceivedMessages();
        List<AbstractRecord> recordList = action.getReceivedRecords();
        SocketState socketState = extractSocketState(state);
        return new ResponseFingerprint(messageList, recordList, socketState);
    }

    public static ResponseFingerprint getFingerprint(State state) {
        ReceivingAction action = state.getWorkflowTrace().getLastReceivingAction();
        return getFingerprint(state, action);
    }

    private static SocketState extractSocketState(State state) {
        if (state.getTlsContext().getTransportHandler() instanceof ClientTcpTransportHandler) {
            SocketState socketState =
                (((ClientTcpTransportHandler) (state.getTlsContext().getTransportHandler())).getSocketState());
            return socketState;
        } else {
            return null;
        }
    }

    private static List<Class<AbstractRecord>> extractRecordClasses(ReceivingAction action) {
        List<Class<AbstractRecord>> classList = new LinkedList<>();
        if (action.getReceivedRecords() != null) {
            for (AbstractRecord record : action.getReceivedRecords()) {
                classList.add((Class<AbstractRecord>) record.getClass());
            }
        }
        return classList;
    }

    private static List<Class<ProtocolMessage>> extractMessageClasses(ReceivingAction action) {
        List<Class<ProtocolMessage>> classList = new LinkedList<>();
        if (action.getReceivedMessages() != null) {
            for (ProtocolMessage message : action.getReceivedMessages()) {
                classList.add((Class<ProtocolMessage>) message.getClass());
            }
        }
        return classList;
    }

    private static boolean didReceiveEncryptedAlert(ReceivingAction action) {
        if (action.getReceivedRecords() != null) {
            for (AbstractRecord abstractRecord : action.getReceivedRecords()) {
                if (abstractRecord instanceof Record) {
                    Record record = (Record) abstractRecord;
                    if (record.getContentMessageType() == ProtocolMessageType.ALERT) {
                        if (record.getLength().getValue() > 6) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private ResponseExtractor() {
    }
}
