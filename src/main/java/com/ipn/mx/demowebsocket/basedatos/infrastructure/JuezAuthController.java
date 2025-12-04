package com.ipn.mx.demowebsocket.basedatos.infrastructure;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/juez")
@CrossOrigin(origins = {"*"})
public class JuezAuthController {

    private final String PASSWORD_JUEZ_CORRECTA = "petotech"; // contrasña de la pp movil

    @PostMapping("/login")
    public ResponseEntity<?> loginJuez(@RequestBody Map<String, String> credenciales) {
        String password = credenciales.get("password");

        if (password != null && password.equals(PASSWORD_JUEZ_CORRECTA)) {
            System.out.println("Login de Juez HTTP exitoso.");
            return ResponseEntity.ok(Map.of("status", "ok", "message", "Login de Juez exitoso"));
        } else {
            System.out.println("Intento de login de Juez HTTP fallido.");
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Contraseña de Juez incorrecta"));
        }
    }
}
