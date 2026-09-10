package com.otilm.openapi.config.util;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassNameResolverTest {

    private static final List<String> INTERFACE_FQNS = List
            .of("com.otilm.api.interfaces.core.web.CertificateController",
                    "com.otilm.api.interfaces.connector.CertificateController",
                    "com.otilm.api.interfaces.connector.v2.CertificateController",
                    "com.otilm.api.interfaces.core.web.InfoController",
                    "com.otilm.api.interfaces.connector.InfoController",
                    "com.otilm.api.interfaces.connector.common.v2.InfoController",
                    "com.otilm.api.interfaces.connector.ComplianceController",
                    "com.otilm.api.interfaces.connector.v2.ComplianceController",
                    "com.otilm.api.interfaces.core.web.v2.ComplianceController",
                    "com.otilm.api.interfaces.core.client.ClientOperationController",
                    "com.otilm.api.interfaces.core.client.v2.ClientOperationController",
                    "com.otilm.api.interfaces.core.web.ConnectorController",
                    "com.otilm.api.interfaces.core.web.v2.ConnectorController",
                    "com.otilm.api.interfaces.connector.HealthController",
                    "com.otilm.api.interfaces.connector.common.v2.HealthController",
                    "com.otilm.api.interfaces.core.web.TokenInstanceController",
                    "com.otilm.api.interfaces.connector.cryptography.TokenInstanceController",
                    "com.otilm.api.interfaces.core.web.CryptographicOperationsController",
                    "com.otilm.api.interfaces.connector.cryptography.CryptographicOperationsController",
                    "com.otilm.api.interfaces.core.local.LocalController",
                    "com.otilm.api.interfaces.core.acme.AcmeController",
                    "com.otilm.api.interfaces.core.scep.ScepController",
                    "com.otilm.api.interfaces.connector.entity.EntityController",
                    "com.otilm.api.interfaces.connector.secrets.VaultController");

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("interfaceNameCases")
    void generatesExpectedImplementationName(String interfaceFqn, String expectedName) {
        String implClassName = ClassNameResolver.generateImplementationClassName(interfaceFqn);

        assertEquals(expectedName, implClassName);
        assertTrue(ClassNameResolver.isValidImplementationClassName(implClassName));
    }

    @Test
    void generatedNamesAreUnique() {
        Set<String> names = INTERFACE_FQNS
                .stream()
                .map(ClassNameResolver::generateImplementationClassName)
                .collect(Collectors.toSet());

        assertEquals(INTERFACE_FQNS.size(), names.size());
    }

    @Test
    void throwsOnSimpleNameWithoutPackage() {
        assertThrows(IllegalArgumentException.class,
                () -> ClassNameResolver.generateImplementationClassName("MyController"));
    }

    @Test
    void throwsOnEmptyString() {
        assertThrows(IllegalArgumentException.class, () -> ClassNameResolver.generateImplementationClassName(""));
    }

    @Test
    void throwsWhenNameStartsWithDot() {
        assertThrows(IllegalArgumentException.class,
                () -> ClassNameResolver.generateImplementationClassName(".MyController"));
    }

    private static Stream<Arguments> interfaceNameCases() {
        return Stream
                .of(Arguments
                        .of("com.otilm.api.interfaces.core.web.CertificateController",
                                "CoreWebCertificateControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.connector.CertificateController",
                                        "ConnectorCertificateControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.connector.v2.CertificateController",
                                        "ConnectorV2CertificateControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.core.web.InfoController",
                                        "CoreWebInfoControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.connector.InfoController",
                                        "ConnectorInfoControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.connector.common.v2.InfoController",
                                        "ConnectorCommonV2InfoControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.connector.ComplianceController",
                                        "ConnectorComplianceControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.connector.v2.ComplianceController",
                                        "ConnectorV2ComplianceControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.core.web.v2.ComplianceController",
                                        "CoreWebV2ComplianceControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.core.client.ClientOperationController",
                                        "CoreClientClientOperationControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.core.client.v2.ClientOperationController",
                                        "CoreClientV2ClientOperationControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.core.web.ConnectorController",
                                        "CoreWebConnectorControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.core.web.v2.ConnectorController",
                                        "CoreWebV2ConnectorControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.connector.HealthController",
                                        "ConnectorHealthControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.connector.common.v2.HealthController",
                                        "ConnectorCommonV2HealthControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.core.web.TokenInstanceController",
                                        "CoreWebTokenInstanceControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.connector.cryptography.TokenInstanceController",
                                        "ConnectorCryptographyTokenInstanceControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.core.web.CryptographicOperationsController",
                                        "CoreWebCryptographicOperationsControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.connector.cryptography.CryptographicOperationsController",
                                        "ConnectorCryptographyCryptographicOperationsControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.core.local.LocalController",
                                        "CoreLocalLocalControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.core.acme.AcmeController",
                                        "CoreAcmeAcmeControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.core.scep.ScepController",
                                        "CoreScepScepControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.connector.entity.EntityController",
                                        "ConnectorEntityEntityControllerDummyImpl"),
                        Arguments
                                .of("com.otilm.api.interfaces.connector.secrets.VaultController",
                                        "ConnectorSecretsVaultControllerDummyImpl"));
    }
}
