package TCP;

import java.net.*;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean; // ← añadido
import java.util.function.Consumer;
import java.util.logging.Logger;

public class MonitorHeartbeat {

    private static final Logger log =
            Logger.getLogger(MonitorHeartbeat.class.getName());

    private final String host;
    private final int puerto;
    private final int timeoutMs;
    private final int intervaloMs;
    private final Consumer<Boolean> callbackEstado;

    private final AtomicBoolean ultimoEstado = new AtomicBoolean(true);

    private volatile Instant ultimoHeartbeat = Instant.now();
    private ScheduledFuture<?> tarea;

    public MonitorHeartbeat(String host, int puerto, int timeoutMs,
                            int intervaloMs, Consumer<Boolean> callback) {
        this.host           = host;
        this.puerto         = puerto;
        this.timeoutMs      = timeoutMs;
        this.intervaloMs    = intervaloMs;
        this.callbackEstado = callback;
    }

    public void iniciar(ScheduledExecutorService scheduler) {
        tarea = scheduler.scheduleAtFixedRate(
                this::verificarNodo, 0, intervaloMs, TimeUnit.MILLISECONDS);
        log.info(String.format("Monitor iniciado para %s:%d (timeout=%dms, intervalo=%dms)",
                host, puerto, timeoutMs, intervaloMs));
    }

    public void detener() {
        if (tarea != null) tarea.cancel(false);
    }

    private void verificarNodo() {
        boolean activo = false;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, puerto), timeoutMs);
            activo = true;
            ultimoHeartbeat = Instant.now();
        } catch (Exception e) {
            log.warning(String.format("Heartbeat fallido para %s:%d - %s",
                    host, puerto, e.getMessage()));
        }

        boolean estadoAnterior = ultimoEstado.getAndSet(activo);
        if (activo != estadoAnterior) {
            String estado = activo ? "RECUPERADO" : "CAIDO";
            System.out.printf("[%s] Nodo %s:%d -> %s%n",
                    LocalTime.now(), host, puerto, estado);
            callbackEstado.accept(activo);
        }
    }

    public boolean estaActivo()          { return ultimoEstado.get(); }
    public Instant getUltimoHeartbeat() { return ultimoHeartbeat; }

    /** Demo con dos nodos: uno real (servidor TCP) y uno falso. */
    public static void main(String[] args) throws InterruptedException {
        ScheduledExecutorService scheduler =
                Executors.newScheduledThreadPool(4);

        MonitorHeartbeat monitorReal = new MonitorHeartbeat(
                "localhost", 9000, 1000, 2000,
                activo -> {
                    if (!activo) {
                        System.out.println("[FAILOVER] Activando nodo de respaldo...");
                    }
                }
        );

        MonitorHeartbeat monitorFalso = new MonitorHeartbeat(
                "192.168.99.99", 9000, 500, 3000,
                activo -> System.out.println("[FAILOVER] Nodo 99.99: " + activo)
        );

        monitorReal.iniciar(scheduler);
        monitorFalso.iniciar(scheduler);

        Thread.sleep(15_000);
        monitorReal.detener();
        monitorFalso.detener();
        scheduler.shutdown();

        System.out.println("Estado final nodo real: " + monitorReal.estaActivo());
    }
}
