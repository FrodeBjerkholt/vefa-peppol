/*
 * Copyright 2016 Direktoratet for forvaltning og IKT
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/community/eupl/og_page/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package no.difi.vefa.peppol.security;

import no.difi.certvalidator.ValidatorBuilder;
import no.difi.certvalidator.api.CertificateBucket;
import no.difi.certvalidator.api.CertificateBucketException;
import no.difi.certvalidator.api.CrlCache;
import no.difi.certvalidator.rule.*;
import no.difi.certvalidator.util.KeyStoreCertificateBucket;
import no.difi.certvalidator.util.SimpleCrlCache;
import no.difi.certvalidator.util.SimplePrincipalNameProvider;
import no.difi.vefa.peppol.common.code.Service;
import no.difi.vefa.peppol.common.lang.PeppolRuntimeException;
import no.difi.vefa.peppol.security.api.CertificateValidator;
import no.difi.vefa.peppol.security.api.ModeDescription;
import no.difi.vefa.peppol.security.api.PeppolSecurityException;
import no.difi.vefa.peppol.security.mode.ProductionMode;
import no.difi.vefa.peppol.security.mode.TestMode;
import no.difi.vefa.peppol.security.util.DifiCertificateValidator;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class Mode {

    public static final String PRODUCTION = "PRODUCTION";

    public static final String TEST = "TEST";

    private static Map<String, ModeDescription> modeDescriptions = new HashMap<>();

    private CrlCache crlCache = new SimpleCrlCache();

    private KeyStoreCertificateBucket keyStore;

    private Map<Service, CertificateBucket> certificateBuckets = new HashMap<>();

    private ModeDescription mode;

    static {
        add(new ProductionMode());
        add(new TestMode());
    }

    /**
     * Register new modes if identifier is unique.
     *
     * @param modeDescription Implementation of mode.
     */
    public static void add(ModeDescription modeDescription) {
        if (!modeDescriptions.containsKey(modeDescription.getIdentifier()))
            modeDescriptions.put(modeDescription.getIdentifier(), modeDescription);
    }

    public static void remove(String identifer) {
        modeDescriptions.remove(identifer);
    }

    public static Mode valueOf(String identifier) {
        if (modeDescriptions.containsKey(identifier))
            return new Mode(modeDescriptions.get(identifier));

        throw new IllegalStateException(String.format("Mode '%s' not found.", identifier));
    }

    public static String detect(X509Certificate certificate) throws PeppolSecurityException {
        for (String identifier : modeDescriptions.keySet()) {
            try {
                valueOf(identifier)
                        .validator(Service.ALL)
                        .validate(certificate);
                return identifier;
            } catch (PeppolSecurityException e) {
                // No action.
            }
        }

        throw new PeppolSecurityException(
                String.format("Unable to detect mode for certificate '%s'.", certificate.getSubjectDN().toString()));
    }

    private Mode(ModeDescription mode) {
        this.mode = mode;

        try {
            keyStore = new KeyStoreCertificateBucket(mode.getKeystore(), "peppol");

            certificateBuckets.put(null, keyStore.startsWith("root-"));
            certificateBuckets.put(Service.ALL, keyStore.startsWith("ap-", "smp-", "sts-"));
            certificateBuckets.put(Service.AP, keyStore.startsWith("ap-"));
            certificateBuckets.put(Service.SMP, keyStore.startsWith("smp-"));
        } catch (CertificateBucketException e) {
            throw new PeppolRuntimeException(e.getMessage(), e);
        }
    }

    public KeyStoreCertificateBucket getKeyStoreBucket() {
        return keyStore;
    }

    public CertificateValidator validator(Service service) {
        ValidatorBuilder validatorBuilder = ValidatorBuilder.newInstance();
        validatorBuilder.addRule(new ExpirationRule());
        validatorBuilder.addRule(SigningRule.PublicSignedOnly());
        validatorBuilder.addRule(new PrincipalNameRule("CN",
                new SimplePrincipalNameProvider(mode.getIssuers(service)), PrincipalNameRule.Principal.ISSUER));
        validatorBuilder.addRule(new ChainRule(certificateBuckets.get(null), certificateBuckets.get(service)));
        validatorBuilder.addRule(new CRLRule(crlCache));
        validatorBuilder.addRule(new OCSPRule(certificateBuckets.get(service)));

        return new DifiCertificateValidator(validatorBuilder.build());
    }
}
