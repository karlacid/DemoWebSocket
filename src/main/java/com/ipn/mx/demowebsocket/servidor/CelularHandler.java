package com.ipn.mx.demowebsocket.servidor;

import com.ipn.mx.demowebsocket.basedatos.service.CelularService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CelularHandler extends TextWebSocketHandler {

    private final CelularService celularService;

    @Autowired
    public CelularHandler(CelularService celularService) {
        this.celularService = celularService;
    }

    private static final int MAX_JUECES = 3;

    private static final Map<WebSocketSession, Integer> sessionToIdMap = new ConcurrentHashMap<>();
    private static final Map<Integer, String> juecesSeleccionados = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> puntosTemp = new ConcurrentHashMap<>();
    private static final Map<Integer, String> colorTemp = new ConcurrentHashMap<>();
    private static final Set<Integer> juecesQueMarcaronIncidencia = Collections.synchronizedSet(new HashSet<>());

    private static int combateIdActual = 1;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionToIdMap.put(session, -1);
        enviarEstadoJueces();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer juezId = sessionToIdMap.remove(session);

        if (juezId != null && juezId != -1) {
            juecesSeleccionados.remove(juezId);
            puntosTemp.remove(juezId);
            colorTemp.remove(juezId);
            juecesQueMarcaronIncidencia.remove(juezId);
        }

        enviarEstadoJueces();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String msg = message.getPayload().trim();

        if (msg.startsWith("SELECCIONAR_JUEZ:")) {
            String[] partes = msg.replace("SELECCIONAR_JUEZ:", "").split(",");
            String nombre = partes[0].trim();
            int posicionDeseada = Integer.parseInt(partes[1].trim());

            if (posicionDeseada < 1 || posicionDeseada > MAX_JUECES) {
                enviarDirecto(session, "POSICION_INVALIDA");
                return;
            }

            if (juecesSeleccionados.containsKey(posicionDeseada)) {
                enviarDirecto(session, "JUEZ_OCUPADO");
                return;
            }

            juecesSeleccionados.put(posicionDeseada, nombre);
            sessionToIdMap.put(session, posicionDeseada);
            enviarEstadoJueces();
            return;
        }

        Integer juezId = sessionToIdMap.get(session);
        if (juezId == null || juezId == -1) return;

        if (msg.startsWith("PUNTUAR:")) {
            procesarPuntaje(msg, juezId);
            return;
        }

        if (msg.startsWith("INCIDENCIA:")) {
            procesarIncidencia(juezId);
            return;
        }

        if (msg.equals("RESET")) {
            resetCompleto();
            broadcast("RESET_COMPLETO");
        }
    }

    private void procesarPuntaje(String msg, Integer juezId) {
        String[] partes = msg.replace("PUNTUAR:", "").split(",");
        String valor = partes[0].trim();
        String color = partes[1].trim();

        if (valor.equalsIgnoreCase("NULL")) {
            puntosTemp.put(juezId, -1);
            colorTemp.put(juezId, color);
            broadcast("PUNTAJE:" + juezId + ",NULL," + color);
            celularService.registrarAdvertencia(combateIdActual);
        } else {
            int puntos = Integer.parseInt(valor);
            if (puntos < 1 || puntos > 5) return;
            puntosTemp.put(juezId, puntos);
            colorTemp.put(juezId, color);
            broadcast("PUNTAJE:" + juezId + "," + puntos + "," + color);
        }

        verificarPromedio();
    }

    private void procesarIncidencia(Integer juezId) {
        boolean esNueva = !juecesQueMarcaronIncidencia.contains(juezId);

        if (esNueva) {
            juecesQueMarcaronIncidencia.add(juezId);
            celularService.guardarIncidencia(juezId, combateIdActual);
            System.out.println(">>> Nueva incidencia del juez " + juezId);
        }

        System.out.println(">>> Total incidencias: " + juecesQueMarcaronIncidencia.size());

        // Siempre verificamos si ya hay 2 o más incidencias
        if (juecesQueMarcaronIncidencia.size() >= 2) {
            System.out.println(">>> ENVIANDO HABILITAR_PUNTOS (hay " + juecesQueMarcaronIncidencia.size() + " incidencias)");
            broadcast("HABILITAR_PUNTOS");
        } else {
            System.out.println(">>> NO se habilitan puntos aún (solo " + juecesQueMarcaronIncidencia.size() + " incidencia)");
        }

        // Confirmamos al juez que su incidencia fue registrada
        broadcast("INCIDENCIA_REGISTRADA:" + juezId);
    }

    private void verificarPromedio() {
        if (puntosTemp.size() != juecesSeleccionados.size()) {
            System.out.println(">>> Esperando más puntajes (" + puntosTemp.size() + "/" + juecesSeleccionados.size() + ")");
            return;
        }

        Set<String> colores = new HashSet<>(colorTemp.values());
        if (colores.size() != 1) {
            System.out.println(">>> Colores diferentes detectados, reseteo completo");
            // Limpiamos TODO incluyendo incidencias cuando hay colores diferentes
            resetCompleto();
            broadcast("RESET_COMPLETO");
            return;
        }

        int suma = 0;
        int contador = 0;

        for (Integer puntos : puntosTemp.values()) {
            if (puntos != -1) {
                suma += puntos;
                contador++;
            }
        }

        if (contador == 0) {
            System.out.println(">>> Solo hubo NULLs, reseteo completo");
            // Si todos marcaron NULL, también reset completo
            resetCompleto();
            broadcast("RESET_COMPLETO");
            return;
        }

        int promedioFinal = Math.round((float) suma / contador);
        String colorFinal = colores.iterator().next();

        System.out.println(">>> Promedio calculado: " + promedioFinal + " - " + colorFinal);
        celularService.guardarPromedio(colorFinal, promedioFinal, combateIdActual);

        resetCompleto();
        broadcast("RESET_COMPLETO");
    }

    private void resetCompleto() {
        puntosTemp.clear();
        colorTemp.clear();
        juecesQueMarcaronIncidencia.clear();
    }

    private void enviarEstadoJueces() {
        StringBuilder estado = new StringBuilder("ESTADO_JUECES:");
        juecesSeleccionados.forEach((id, nombre) -> estado.append(id).append("-").append(nombre).append(","));
        broadcast(estado.toString());
    }

    private void broadcast(String msg) {
        for (WebSocketSession sesion : sessionToIdMap.keySet()) {
            try {
                if (sesion.isOpen()) {
                    sesion.sendMessage(new TextMessage(msg));
                }
            } catch (Exception ignored) {}
        }
    }

    private void enviarDirecto(WebSocketSession sesion, String msg) {
        try {
            sesion.sendMessage(new TextMessage(msg));
        } catch (Exception ignored) {}
    }
}