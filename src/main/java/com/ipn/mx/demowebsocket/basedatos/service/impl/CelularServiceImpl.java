package com.ipn.mx.demowebsocket.basedatos.service.impl;

import com.ipn.mx.demowebsocket.basedatos.service.CelularService;
import org.springframework.stereotype.Service;

@Service
public class CelularServiceImpl implements CelularService {

    @Override
    public void guardarIncidencia(Integer juezId, Integer combateId) {
        System.out.println("INCIDENCIA parar tiempo, ya esta la regla para que sea cuando al menos dos jueces marcaron incidencia");
    }

    @Override
    public void registrarAdvertencia(Integer combateId) {
        System.out.println("solo nulls");
    }

    @Override
    public void guardarPromedio(String color, Integer promedioFinal, Integer combateId) {
        System.out.println("Puntaje: " + color + " " + promedioFinal);
    }
}
