/**
 * TLS-Scanner - A TLS configuration and analysis tool based on TLS-Attacker.
 *
 * Copyright 2017-2019 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsscanner.serverscanner.report.result;

import de.rub.nds.tlsscanner.serverscanner.constants.ProbeType;
import de.rub.nds.tlsscanner.serverscanner.rating.TestResult;
import de.rub.nds.tlsscanner.serverscanner.report.AnalyzedProperty;
import de.rub.nds.tlsscanner.serverscanner.report.SiteReport;

public class StarttlsConfigurationResult extends ProbeResult {

    private TestResult vulnerable;

    public StarttlsConfigurationResult(TestResult vulnerable) {
        super(ProbeType.STARTTLS_CONFIGURATION);
        this.vulnerable = vulnerable;
    }

    @Override
    protected void mergeData(SiteReport report) {
        report.putResult(AnalyzedProperty.OFFERS_PLAIN_LOGIN, vulnerable);
    }
}
