package com.taller.gestion.usuario.repository;

import com.taller.gestion.usuario.entity.Rol;
import com.taller.gestion.usuario.entity.Usuario;
import jakarta.persistence.QueryHint;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    /**
     * Solo el id, sin cargar la entidad y sin flush: la usa la auditoría desde el listener
     * de JPA, en medio de un insert con IDENTITY. Un auto-flush ahí dejaría la transacción
     * marcada como rollback-only y se perdería la operación de negocio.
     */
    @QueryHints(@QueryHint(name = HibernateHints.HINT_FLUSH_MODE, value = "COMMIT"))
    @Query("SELECT u.id_usuario FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<Long> buscarIdPorEmailSinFlush(@Param("email") String email);

    boolean existsByEmailIgnoreCase(String email);

    // El guion bajo de id_usuario se interpretaría como ruta anidada, por eso va JPQL explícito.
    @Query("SELECT COUNT(u) FROM Usuario u "
            + "WHERE LOWER(u.email) = LOWER(:email) AND u.id_usuario <> :id_usuario")
    long countByEmailAndIdNot(@Param("email") String email,
                              @Param("id_usuario") Long id_usuario);

    long countByRolAndActivoTrue(Rol rol);

    @Query("SELECT u FROM Usuario u "
            + "WHERE (:incluirInactivos = true OR u.activo = true) "
            + "AND (:busqueda = '' "
            + "     OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) "
            + "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    Page<Usuario> buscar(@Param("busqueda") String busqueda,
                         @Param("incluirInactivos") boolean incluirInactivos,
                         Pageable pageable);
}
