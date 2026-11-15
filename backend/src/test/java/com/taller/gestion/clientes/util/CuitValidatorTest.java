package com.taller.gestion.clientes.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CuitValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"20-12345678-6", "20123456786", "30-71234567-1"})
    @DisplayName("CUIT válidos: con y sin guiones")
    void cuitsValidos(String cuit) {
        assertThat(CuitValidator.esValido(cuit)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "20-12345678-9",   // dígito verificador incorrecto
            "20-12345678",     // 10 dígitos
            "20-123456789-6",  // 12 dígitos
            ""                 // vacío
    })
    @DisplayName("CUIT inválidos: dígito verificador o cantidad de dígitos incorrecta")
    void cuitsInvalidos(String cuit) {
        assertThat(CuitValidator.esValido(cuit)).isFalse();
    }
}