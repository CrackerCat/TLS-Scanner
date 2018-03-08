/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.rub.nds.tlsscanner.report.result;

import de.rub.nds.tlsscanner.constants.ProbeType;
import de.rub.nds.tlsscanner.report.SiteReport;

public class StarttlsResult extends ProbeResult {
    
    private Boolean supported;
    
    public StarttlsResult(Boolean supported) {
        super(ProbeType.STARTTLS);
        this.supported = supported;
    }

    @Override
    public void merge(SiteReport report) {
        report.setSupportsStarttls(supported);
    }
    
    
}
