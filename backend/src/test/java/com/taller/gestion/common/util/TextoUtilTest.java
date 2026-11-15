package com.taller.gestion.common.util;

import com.taller.gestion.util.TextoUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextoUtilTest {

    @Test
    @DisplayName("limpiar: saca espacios de los bordes y junta los repetidos")
    void limpiar_espacios() {
        assertThat(TextoUtil.limpiar("  Juan    Perez ")).isEqualTo("Juan Perez");
    }

    @Test
    @DisplayName("limpiar: vacío, solo espacios y null pasan a null")
    void limpiar_vacioANull() {
        assertThat(TextoUtil.limpiar("   ")).isNull();
        assertThat(TextoUtil.limpiar("")).isNull();
        assertThat(TextoUtil.limpiar(null)).isNull();
    }

    @Test
    @DisplayName("limpiarEmail: minúsculas y sin espacios")
    void limpiarEmail() {
        assertThat(TextoUtil.limpiarEmail("  Juan@Mail.COM ")).isEqualTo("juan@mail.com");
    }

    @Test
    @DisplayName("normalizarParaBusqueda: sin acentos, minúsculas y espacios únicos")
    void normalizar_acentosMayusculasEspacios() {
        assertThat(TextoUtil.normalizarParaBusqueda("  JOSÉ   Pérez ")).isEqualTo("jose perez");
    }

    @Test
    @DisplayName("normalizarParaBusqueda: elimina los comodines de LIKE")
    void normalizar_sacaComodines() {
        assertThat(TextoUtil.normalizarParaBusqueda("Perez_%")).isEqualTo("perez");
    }

    @Test
    @DisplayName("normalizarParaBusqueda: null devuelve cadena vacía")
    void normalizar_null() {
        assertThat(TextoUtil.normalizarParaBusqueda(null)).isEmpty();
    }
}