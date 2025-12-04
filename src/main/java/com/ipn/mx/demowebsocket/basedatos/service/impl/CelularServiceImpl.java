package com.ipn.mx.demowebsocket.basedatos.service.impl;

import com.ipn.mx.demowebsocket.basedatos.service.CelularService;
import org.springframework.stereotype.Service;

@Service
public class CelularServiceImpl implements CelularService {

    @Override
    public void guardarPuntaje(Integer juezId, Integer puntos, String color, Integer combateId) {

        if (puntos < 1 || puntos > 5) {
            System.out.println("❌ Puntaje inválido");
            return;
        }

        System.out.println(
                "✅ Puntaje guardado → Juez: " + juezId +
                        " | Puntos: " + puntos +
                        " | Color: " + color +
                        " | Combate: " + combateId
        );
    }

    @Override
    public void guardarIncidencia(Integer juezId, Integer combateId) {

        System.out.println(
                "🚨 Incidencia guardada → Juez: " + juezId +
                        " | Combate: " + combateId
        );
    }

    @Override
    public void registrarAdvertencia(Integer combateId) {

        System.out.println(
                "⚠️ Advertencia registrada → Combate: " + combateId
        );
    }

    @Override
    public void guardarPromedio(String color, Integer promedioFinal, Integer combateId) {

        System.out.println(
                "✅ Promedio guardado → Color: " + color +
                        " | Promedio: " + promedioFinal +
                        " | Combate: " + combateId
        );
    }
}
