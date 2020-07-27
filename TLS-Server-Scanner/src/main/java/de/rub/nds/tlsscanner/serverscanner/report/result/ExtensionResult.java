/**
 * TLS-Scanner - A TLS configuration and analysis tool based on TLS-Attacker.
 *
 * Copyright 2017-2019 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsscanner.report.result;

import de.rub.nds.tlsattacker.core.constants.ExtensionType;
import de.rub.nds.tlsscanner.constants.ProbeType;
import de.rub.nds.tlsscanner.rating.TestResult;
import de.rub.nds.tlsscanner.report.AnalyzedProperty;
import de.rub.nds.tlsscanner.report.SiteReport;
import java.util.List;

/**
 *
 * @author Robert Merget <robert.merget@rub.de>
 */
public class ExtensionResult extends ProbeResult {

    private List<ExtensionType> allSupportedExtensions;
    private TestResult extendedMasterSecret = TestResult.FALSE;
    private TestResult encryptThenMac = TestResult.FALSE;
    private TestResult secureRenegotiation = TestResult.FALSE;
    private TestResult sessionTickets = TestResult.FALSE;

    public ExtensionResult(List<ExtensionType> allSupportedExtensions) {
        super(ProbeType.EXTENSIONS);
        this.allSupportedExtensions = allSupportedExtensions;
    }

    @Override
    public void mergeData(SiteReport report) {
        if (report.getSupportedExtensions() == null) {
            report.setSupportedExtensions(allSupportedExtensions);
        } else {
            report.getSupportedExtensions().addAll(allSupportedExtensions);
        }
        if (allSupportedExtensions != null) {
            for (ExtensionType type : allSupportedExtensions) {
                if (type == ExtensionType.ENCRYPT_THEN_MAC) {
                    encryptThenMac = TestResult.TRUE;
                }
                if (type == ExtensionType.EXTENDED_MASTER_SECRET) {
                    extendedMasterSecret = TestResult.TRUE;
                }
                if (type == ExtensionType.RENEGOTIATION_INFO) {
                    secureRenegotiation = TestResult.TRUE;
                }
                if (type == ExtensionType.SESSION_TICKET) {
                    sessionTickets = TestResult.TRUE;
                }
            }
        } else {
            encryptThenMac = TestResult.COULD_NOT_TEST;
            extendedMasterSecret = TestResult.COULD_NOT_TEST;
            secureRenegotiation = TestResult.COULD_NOT_TEST;
            sessionTickets = TestResult.COULD_NOT_TEST;
        }
        report.putResult(AnalyzedProperty.SUPPORTS_EXTENDED_MASTER_SECRET, extendedMasterSecret);
        report.putResult(AnalyzedProperty.SUPPORTS_ENCRYPT_THEN_MAC, encryptThenMac);
        report.putResult(AnalyzedProperty.SUPPORTS_SECURE_RENEGOTIATION_EXTENSION, secureRenegotiation);
        report.putResult(AnalyzedProperty.SUPPORTS_SESSION_TICKETS, sessionTickets);
    }

}
