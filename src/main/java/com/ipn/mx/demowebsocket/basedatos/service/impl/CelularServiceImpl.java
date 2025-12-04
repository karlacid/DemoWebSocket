package com.ipn.mx.demowebsocket.basedatos.service.impl;


import com.ipn.mx.demowebsocket.basedatos.service.CelularService;
import org.springframework.stereotype.Service;

@Service
public class CelularServiceImpl implements CelularService {

    @Override
    public void guardarPuntaje(Integer juezId, Integer puntos, String color, Integer combateId) {

        if (puntos < 0 || puntos > 5) {
            System.out.println("Puntaje invalido");
            return;
        }

        System.out.println(
                "marco de puntos un juez" + puntos
        );
    }

    @Override
    public void guardarIncidencia(Integer juezId, Integer combateId) {

        System.out.println(
                "Alguien marco incidencia"
        );
    }

    @Override
    public void registrarAdvertencia(Integer combateId) {

        System.out.println(
                "aqui se detiene el tiempo"
        );
    }

    @Override
    public void guardarPromedio(String color, Integer promedioFinal, Integer combateId) {

        System.out.println(
                "puntaje, aqui se guarda el puntaje" + promedioFinal
        );
    }
}