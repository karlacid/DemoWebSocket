package com.ipn.mx.demowebsocket.basedatos.service.impl;

import com.ipn.mx.demowebsocket.basedatos.service.CelularService;
import org.springframework.stereotype.Service;

@Service
public class CelularServiceImpl implements CelularService {

    @Override
    public void guardarIncidencia(Integer juezId, Integer combateId) {
        System.out.println("INCIDENCIA registrada por al menos 2 jueces");
    }

    @Override
    public void registrarAdvertencia(Integer combateId) {
        System.out.println("Advertencia registrada");
    }

    @Override
    public void guardarPromedio(String color, Integer promedioFinal, Integer combateId) {
        System.out.println("Promedio calculado: " + color + " " + promedioFinal);
    }
}
