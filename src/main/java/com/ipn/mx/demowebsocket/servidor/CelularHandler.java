package com.ipn.mx.demowebsocket.servidor;

import com.ipn.mx.demowebsocket.basedatos.service.CelularService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.*;

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
    private static final Map<Integer, Long> tiempoRegistro = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

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
            tiempoRegistro.remove(juezId);
        }
        enviarEstadoJueces();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String msg = message.getPayload().trim();

        if (msg.startsWith("SELECCIONAR_JUEZ:")) {
            String[] partes = msg.replace("SELECCIONAR_JUEZ:", "").split(",");
            String nombre = partes[0].trim();
            int posicion = Integer.parseInt(partes[1].trim());

            if (posicion < 1 || posicion > MAX_JUECES) {
                enviarDirecto(session, "POSICION_INVALIDA");
                return;
            }
            if (juecesSeleccionados.containsKey(posicion)) {
                enviarDirecto(session, "JUEZ_OCUPADO");
                return;
            }

            juecesSeleccionados.put(posicion, nombre);
            sessionToIdMap.put(session, posicion);
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

        tiempoRegistro.put(juezId, System.currentTimeMillis());
        iniciarTemporizador(juezId);

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
        tiempoRegistro.put(juezId, System.currentTimeMillis());
        iniciarTemporizador(juezId);

        boolean nueva = !juecesQueMarcaronIncidencia.contains(juezId);
        if (nueva) juecesQueMarcaronIncidencia.add(juezId);

        if (juecesQueMarcaronIncidencia.size() >= 2) {
            celularService.guardarIncidencia(juezId, combateIdActual);
        }

        broadcast("INCIDENCIA_REGISTRADA:" + juezId);
    }

    private void iniciarTemporizador(int juezId) {
        scheduler.schedule(() -> {
            if (!tiempoRegistro.containsKey(juezId)) return;
            long tiempo = tiempoRegistro.get(juezId);

            if (System.currentTimeMillis() - tiempo >= 20000) {
                puntosTemp.remove(juezId);
                colorTemp.remove(juezId);
                juecesQueMarcaronIncidencia.remove(juezId);
                tiempoRegistro.remove(juezId);
                broadcast("RESET_TIEMPO_EXCEDIDO:" + juezId);
            }

        }, 20, TimeUnit.SECONDS);
    }

    private void verificarPromedio() {
        if (puntosTemp.size() != juecesSeleccionados.size()) return;

        Set<String> colores = new HashSet<>(colorTemp.values());
        if (colores.size() != 1) {
            resetCompleto();
            broadcast("RESET_COMPLETO");
            return;
        }

        int suma = 0, contador = 0;
        for (Integer puntos : puntosTemp.values()) {
            if (puntos != -1) {
                suma += puntos;
                contador++;
            }
        }

        if (contador == 0) {
            celularService.guardarPromedio("NULL", 0, combateIdActual);
            resetCompleto();
            broadcast("RESET_COMPLETO");
            return;
        }

        int promedio = Math.round((float) suma / contador);
        String color = colores.iterator().next();
        celularService.guardarPromedio(color, promedio, combateIdActual);

        resetCompleto();
        broadcast("RESET_COMPLETO");
    }

    private void resetCompleto() {
        puntosTemp.clear();
        colorTemp.clear();
        juecesQueMarcaronIncidencia.clear();
        tiempoRegistro.clear();
    }

    private void enviarEstadoJueces() {
        StringBuilder estado = new StringBuilder("ESTADO_JUECES:");
        juecesSeleccionados.forEach((id, nombre) -> estado.append(id).append("-").append(nombre).append(","));
        broadcast(estado.toString());
    }

    private void broadcast(String msg) {
        for (WebSocketSession sesion : sessionToIdMap.keySet()) {
            try {
                if (sesion.isOpen()) sesion.sendMessage(new TextMessage(msg));
            } catch (Exception ignored) {}
        }
    }

    private void enviarDirecto(WebSocketSession sesion, String msg) {
        try {
            sesion.sendMessage(new TextMessage(msg));
        } catch (Exception ignored) {}
    }
}
