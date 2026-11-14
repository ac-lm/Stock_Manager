package com.taller.gestion.clientes.service;

import com.taller.gestion.clientes.dto.ClienteCreateDTO;
import com.taller.gestion.clientes.dto.ClienteResponseDTO;
import com.taller.gestion.clientes.dto.ClienteUpdateDTO;
import com.taller.gestion.clientes.entity.Cliente;
import com.taller.gestion.clientes.entity.TipoCliente;
import com.taller.gestion.clientes.repository.ClienteRepository;
import com.taller.gestion.clientes.util.CuitValidator;
import com.taller.gestion.common.exception.DatosInvalidosException;
import com.taller.gestion.common.exception.OperacionNoPermitidaException;
import com.taller.gestion.common.exception.RecursoDuplicadoException;
import com.taller.gestion.common.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClienteService {
    private final ClienteRepository clienteRepository;

    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> buscar(String busqueda, boolean incluirInactivos, Pageable pageable) {
        String texto = busqueda == null ? "" : busqueda.trim();
        String digitos = texto.replaceAll("\\D", "");
        if (digitos.isEmpty() && !texto.isEmpty()) {
            digitos = "#";
        }
        return clienteRepository
                .buscar(texto, digitos, incluirInactivos, pageable)
                .map(this::mapearAResponse);
    }

    @Transactional
    public ClienteResponseDTO save(ClienteCreateDTO dto) {
        validarTipoNoGenerico(dto.tipo_cliente());

        String cuit = normalizarCuit(dto.cuit());

        validarCuitYTipoCliente(cuit, dto.tipo_cliente(), null);

        Cliente cliente = new Cliente();
        cliente.setCuit(cuit);
        cliente.setNombre(dto.nombre());
        cliente.setEmail(dto.email());
        cliente.setTelefono(dto.telefono());
        cliente.setDireccion(dto.direccion());
        cliente.setTipo_cliente(dto.tipo_cliente());
        cliente.setObservaciones(dto.observaciones());

        return mapearAResponse(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO findById(Long id) {
        return mapearAResponse(obtenerCliente(id));
    }

    @Transactional
    public ClienteResponseDTO update(Long id, ClienteUpdateDTO dto) {
        Cliente cliente = obtenerClienteEditable(id);

        validarTipoNoGenerico(dto.tipo_cliente());
        String cuit = normalizarCuit(dto.cuit());

        validarCuitYTipoCliente(cuit, dto.tipo_cliente(), id);

        cliente.setCuit(cuit);
        cliente.setNombre(dto.nombre());
        cliente.setTelefono(dto.telefono());
        cliente.setEmail(dto.email());
        cliente.setDireccion(dto.direccion());
        cliente.setTipo_cliente(dto.tipo_cliente());
        cliente.setObservaciones(dto.observaciones());

        return mapearAResponse(cliente);
    }

    @Transactional
    public ClienteResponseDTO cambiarEstado(Long id, boolean activo) {
        Cliente cliente = obtenerClienteEditable(id);
        cliente.setActivo(activo);
        return mapearAResponse(cliente);
    }

    private Cliente obtenerCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado con id " + id));
    }

    private Cliente obtenerClienteEditable(Long id) {
        Cliente cliente = obtenerCliente(id);
        if (cliente.getTipo_cliente() == TipoCliente.GENERICO) {
            throw new OperacionNoPermitidaException("El cliente genérico no se puede modificar");
        }
        return cliente;
    }

    private void validarCuitYTipoCliente(String cuit, TipoCliente tipoCliente, Long idExcluir) {
        if (tipoCliente == TipoCliente.GRAN_CLIENTE && cuit == null) {
            throw new DatosInvalidosException("El CUIT es obligatorio para grandes clientes");
        }
        if (cuit != null) {
            if (!CuitValidator.esValido(cuit)) {
                throw new DatosInvalidosException("El CUIT no es válido");
            }
            boolean duplicado = (idExcluir == null)
                    ? clienteRepository.existsByCuit(cuit)
                    : clienteRepository.existsByCuitAndIdNot(cuit, idExcluir);
            if (duplicado) {
                throw new RecursoDuplicadoException("Ya existe un cliente con ese CUIT");
            }
        }
    }

    private String normalizarCuit(String cuit) {
        if (cuit == null || cuit.isBlank()) {
            return null;
        }
        String digitos = cuit.replaceAll("\\D", ""); // saca guiones, puntos y espacios
        if (digitos.length() != 11) {
            throw new DatosInvalidosException("El CUIT debe tener 11 dígitos");
        }
        return digitos.substring(0, 2) + "-" + digitos.substring(2, 10) + "-" + digitos.substring(10);
    }

    private ClienteResponseDTO mapearAResponse(Cliente cliente) {
        return new ClienteResponseDTO(
                cliente.getId_cliente(),
                cliente.getCuit(),
                cliente.getNombre(),
                cliente.getTelefono(),
                cliente.getEmail(),
                cliente.getDireccion(),
                cliente.getTipo_cliente(),
                cliente.getObservaciones(),
                cliente.isActivo()
        );
    }

    private void validarTipoNoGenerico(TipoCliente tipo) {
        if (tipo == TipoCliente.GENERICO) {
            throw new DatosInvalidosException("El tipo GENERICO está reservado para el sistema");
        }
    }
}
