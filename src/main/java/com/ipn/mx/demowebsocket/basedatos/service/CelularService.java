package com.ipn.mx.demowebsocket.basedatos.service;

public interface CelularService {

    void guardarPuntaje(Integer juezId, Integer puntos, String color, Integer combateId);

    void guardarIncidencia(Integer juezId, Integer combateId);

    void registrarAdvertencia(Integer combateId);

    void guardarPromedio(String color, Integer promedioFinal, Integer combateId);
}
