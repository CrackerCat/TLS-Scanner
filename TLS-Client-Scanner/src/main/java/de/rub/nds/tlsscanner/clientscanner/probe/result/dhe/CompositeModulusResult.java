/**
 * TLS-Client-Scanner - A TLS configuration and analysis tool based on TLS-Attacker
 *
 * Copyright 2017-2022 Ruhr University Bochum, Paderborn University, Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package de.rub.nds.tlsscanner.clientscanner.probe.result.dhe;

import de.rub.nds.scanner.core.constants.TestResult;
import de.rub.nds.tlsscanner.clientscanner.constants.CompositeModulusType;

public class CompositeModulusResult {

    private final TestResult result;
    private final CompositeModulusType type;

    public CompositeModulusResult(TestResult result, CompositeModulusType type) {
        this.result = result;
        this.type = type;
    }

    public TestResult getResult() {
        return result;
    }

    public CompositeModulusType getType() {
        return type;
    }
}
