/**
 * TLS-Server-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.serverscanner.probe.result;

import de.rub.nds.scanner.core.constants.TestResult;
import de.rub.nds.scanner.core.probe.result.ProbeResult;
import de.rub.nds.tlsscanner.core.constants.TlsAnalyzedProperty;
import de.rub.nds.tlsscanner.core.constants.TlsProbeType;
import de.rub.nds.tlsscanner.serverscanner.report.ServerReport;

public class CcaSupportResult extends ProbeResult<ServerReport> {

    private final TestResult supportsCca;

    public CcaSupportResult(TestResult supportsCca) {
        super(TlsProbeType.CCA_SUPPORT);
        this.supportsCca = supportsCca;
    }

    @Override
    public void mergeData(ServerReport report) {
        report.putResult(TlsAnalyzedProperty.SUPPORTS_CCA, supportsCca);
    }
}
