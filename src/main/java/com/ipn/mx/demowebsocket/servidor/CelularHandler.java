package com.ipn.mx.demowebsocket.servidor;

import com.ipn.mx.demowebsocket.basedatos.service.CelularService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CelularHandler extends TextWebSocketHandler {

    private final CelularService celularService;

    @Autowired
    public CelularHandler(CelularService celularService) {
        this.celularService = celularService;
    }

    private static final Map<WebSocketSession, Integer> sessionToIdMap = new ConcurrentHashMap<>();
    private static final Map<Integer, WebSocketSession> idToSessionMap = new ConcurrentHashMap<>();
    private static final Set<Integer> idsActivos =
            Collections.synchronizedSet(new HashSet<>());

    private static final int MAX_JUECES = 3;

    private static final Set<Integer> juecesSeleccionados =
            Collections.synchronizedSet(new HashSet<>());

    private static final Map<Integer, Integer> puntosTemp = new ConcurrentHashMap<>();
    private static final Map<Integer, String> colorTemp = new ConcurrentHashMap<>();

    private static int incidenciasTemp = 0;
    private static final Set<Integer> juecesQueMarcaronIncidencia =
            Collections.synchronizedSet(new HashSet<>());

    private static int combateIdActual = 1;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {

        int juezIdAsignado = -1;

        synchronized (idsActivos) {

            if (idsActivos.size() >= MAX_JUECES) {
                session.close(CloseStatus.SERVICE_OVERLOAD.withReason("Sala llena"));
                return;
            }

            for (int i = 1; i <= MAX_JUECES; i++) {
                if (!idsActivos.contains(i)) {
                    juezIdAsignado = i;
                    break;
                }
            }

            if (juezIdAsignado != -1) {
                idsActivos.add(juezIdAsignado);
                sessionToIdMap.put(session, juezIdAsignado);
                idToSessionMap.put(juezIdAsignado, session);

                System.out.println("Juez conectado con ID: " + juezIdAsignado);
                send(session, "ESTADO_JUECES:" + juecesSeleccionados.toString());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        Integer juezId = sessionToIdMap.get(session);

        if (juezId != null) {

            idsActivos.remove(juezId);
            sessionToIdMap.remove(session);
            idToSessionMap.remove(juezId);

            puntosTemp.remove(juezId);
            colorTemp.remove(juezId);
            juecesQueMarcaronIncidencia.remove(juezId);
            juecesSeleccionados.remove(juezId);
            broadcast("ESTADO_JUECES:" + juecesSeleccionados.toString());

            System.out.println("Juez desconectado: " + juezId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        String msg = message.getPayload().trim();
        Integer juezId = sessionToIdMap.get(session);
        if (juezId == null) return;

        if (msg.startsWith("SELECCIONAR_JUEZ:")) {

            int juezSeleccionado = Integer.parseInt(
                    msg.replace("SELECCIONAR_JUEZ:", "").trim()
            );

            synchronized (juecesSeleccionados) {

                if (juecesSeleccionados.contains(juezSeleccionado)) {
                    send(session, "JUEZ_OCUPADO");
                    System.out.println("Juez " + juezSeleccionado + " ya ocupado");
                    return;
                }

                juecesSeleccionados.add(juezSeleccionado);
                System.out.println("Juez " + juezSeleccionado + " seleccionado por sesión " + juezId);
            }

            broadcast("ESTADO_JUECES:" + juecesSeleccionados.toString());
            return;
        }

        if (msg.startsWith("PUNTUAR:")) {

            String[] partes = msg.replace("PUNTUAR:", "").split(",");
            int puntos = Integer.parseInt(partes[0]);
            String color = partes[1];
            if (puntos < 0 || puntos > 5) {
                send(session, "ERROR:PUNTOS_INVALIDOS");
                return;
            }

            puntosTemp.put(juezId, puntos);
            colorTemp.put(juezId, color);

            broadcast("PUNTAJE:" + juezId + "," + puntos + "," + color);
            celularService.guardarPuntaje(juezId, puntos, color, combateIdActual);

            System.out.println("Puntaje: Juez " + juezId + " → " + puntos + " pts " + color);

            calcularYEnviarPromedio(color);

            return;
        }

        if (msg.equals("INCIDENCIA")) {

            synchronized (juecesQueMarcaronIncidencia) {
                if (juecesQueMarcaronIncidencia.contains(juezId)) return;

                juecesQueMarcaronIncidencia.add(juezId);
                incidenciasTemp++;

                broadcast("INCIDENCIAS:" + incidenciasTemp);
                celularService.guardarIncidencia(juezId, combateIdActual);

                System.out.println("Incidencia: Juez " + juezId + " (Total: " + incidenciasTemp + ")");

                if (juecesQueMarcaronIncidencia.size() >= 2) {
                    broadcast("HABILITAR_PUNTOS");
                    celularService.registrarAdvertencia(combateIdActual);
                    System.out.println("Habilitando botones de puntos (2+ incidencias)");
                }
            }
            return;
        }

        if (msg.equals("RESET")) {

            puntosTemp.clear();
            colorTemp.clear();
            juecesQueMarcaronIncidencia.clear();
            incidenciasTemp = 0;

            broadcast("RESET_COMPLETO");
            System.out.println("Reset completado");
        }
    }

    private void broadcast(String msg) {
        for (WebSocketSession sesion : idToSessionMap.values()) {
            try {
                if (sesion.isOpen()) {
                    sesion.sendMessage(new TextMessage(msg));
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void send(WebSocketSession session, String msg) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(msg));
            }
        } catch (Exception ignored) {
        }
    }

    private void calcularYEnviarPromedio(String color) {
        int juecesQuePuntuaron = 0;
        int sumaPuntos = 0;

        synchronized (puntosTemp) {
            for (Integer juezId : idsActivos) {
                if (puntosTemp.containsKey(juezId) &&
                        colorTemp.containsKey(juezId) &&
                        colorTemp.get(juezId).equalsIgnoreCase(color)) {

                    sumaPuntos += puntosTemp.get(juezId);
                    juecesQuePuntuaron++;
                }
            }
        }

        System.out.println("Jueces que puntuaron " + color + ": " + juecesQuePuntuaron + "/" + idsActivos.size());

        if (juecesQuePuntuaron == idsActivos.size() && juecesQuePuntuaron > 0) {

            double promedio = (double) sumaPuntos / juecesQuePuntuaron;

            double parteDecimal = promedio - Math.floor(promedio);
            int promedioFinal;

            if (parteDecimal < 0.5) {
                promedioFinal = (int) Math.floor(promedio);
            } else if (parteDecimal >= 0.6) {
                promedioFinal = (int) Math.ceil(promedio);
            } else {
                promedioFinal = (int) Math.round(promedio);
            }

            System.out.println("Puntos: " + promedioFinal);

            celularService.guardarPromedio(color, promedioFinal, combateIdActual);

            puntosTemp.clear();
            colorTemp.clear();
            juecesQueMarcaronIncidencia.clear();
            incidenciasTemp = 0;

            broadcast("RESET_COMPLETO");
        }
    }
}