/**
 * TLS-Scanner - A TLS configuration and analysis tool based on TLS-Attacker.
 *
 * Copyright 2017-2019 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsscanner.rating;

import de.rub.nds.tlsscanner.report.AnalyzedProperty;
import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "recommendations")
public class Recommendations implements Serializable {

    /**
     * The default Config file to load.
     */
    static final String DEFAULT_RECOMMENDATIONS_FILE = "rating/recommendations.xml";

    private List<Recommendation> recommendations;

    private Recommendations() {

    }

    public Recommendations(List<Recommendation> recommendations) {
        this.recommendations = recommendations;
    }

    public static Recommendations createRecommendations() {
        InputStream stream = Recommendations.class.getResourceAsStream(DEFAULT_RECOMMENDATIONS_FILE);
        return RatingIO.readRecommendations(stream);
    }

    public static Recommendations createRecommendations(File f) {
        return RatingIO.readRecommendations(f);
    }

    public static Recommendations createRecommendations(InputStream stream) {
        return RatingIO.readRecommendations(stream);
    }

    @XmlElement(name = "recommendation")
    public List<Recommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<Recommendation> recommendations) {
        this.recommendations = recommendations;
    }

    public PropertyResultRecommendation getPropertyRecommendation(AnalyzedProperty property, TestResult result) {
        for (Recommendation r : recommendations) {
            if (r.getAnalyzedProperty() == property) {
                return r.getPropertyResultRecommendation(result);
            }
        }
        return new PropertyResultRecommendation(result, Recommendation.NO_RECOMMENDATION_FOUND,
                Recommendation.NO_RECOMMENDATION_FOUND);
    }

    public Recommendation getRecommendation(AnalyzedProperty property) {
        for (Recommendation r : recommendations) {
            if (r.getAnalyzedProperty() == property) {
                return r;
            }
        }
        return new Recommendation(property, property.toString());
    }
}
