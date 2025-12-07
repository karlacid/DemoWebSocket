package com.ipn.mx.demowebsocket.basedatos.service.impl;

import com.ipn.mx.demowebsocket.basedatos.domain.entity.Administrador;
import com.ipn.mx.demowebsocket.basedatos.domain.repository.AdministradorRepository;
import com.ipn.mx.demowebsocket.basedatos.service.AdministradorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AdministradorServiceImpl implements AdministradorService {

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<Administrador> readAll() {
        return administradorRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Administrador read(Integer id) {
        return administradorRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Administrador save(Administrador administrador) {
        // Validación de unicidad de correo
        if (administrador.getCorreoAdministrador() != null) {
            Optional<Administrador> existingByCorreo = administradorRepository
                    .findByCorreoAdministrador(administrador.getCorreoAdministrador());

            if (existingByCorreo.isPresent()) {
                // Permitir si es el mismo administrador (UPDATE)
                if (administrador.getIdAdministrador() == null ||
                        !existingByCorreo.get().getIdAdministrador().equals(administrador.getIdAdministrador())) {
                    throw new IllegalArgumentException("El correo ya está registrado");
                }
            }
        }

        // Validación de unicidad de usuario
        if (administrador.getUsuarioAdministrador() != null) {
            Optional<Administrador> existingByUsuario = administradorRepository
                    .findByUsuarioAdministrador(administrador.getUsuarioAdministrador());

            if (existingByUsuario.isPresent()) {
                // Permitir si es el mismo administrador (UPDATE)
                if (administrador.getIdAdministrador() == null ||
                        !existingByUsuario.get().getIdAdministrador().equals(administrador.getIdAdministrador())) {
                    throw new IllegalArgumentException("El usuario ya está registrado");
                }
            }
        }

        String raw = administrador.getContraseniaAdministrador();
        if (raw != null && !raw.isBlank()) {
            // Heurística simple: si no parece BCrypt, hasheamos
            if (!raw.startsWith("$2a$") && !raw.startsWith("$2b$") && !raw.startsWith("$2y$")) {
                administrador.setContraseniaAdministrador(passwordEncoder.encode(raw));
            }
        }

        return administradorRepository.save(administrador);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        administradorRepository.deleteById(id);
    }
}