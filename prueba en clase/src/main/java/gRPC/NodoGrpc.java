package gRPC;

import comun.LamportClock;
import io.grpc.*;

import java.io.IOException;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class NodoGrpc extends NodoServiceGrpc.NodoServiceImplBase {

    private static final Logger log = Logger.getLogger(NodoGrpc.class.getName());

    private final int              nodoId;
    private final LamportClock     reloj;
    private final AtomicInteger    coordinadorId = new AtomicInteger(-1);

    public NodoGrpc(int nodoId) {
        this.nodoId = nodoId;
        this.reloj  = new LamportClock("Nodo-" + nodoId);
    }

    @Override
    public void operar(OperacionRequest req,
                       io.grpc.stub.StreamObserver<OperacionResponse> obs) {

        long t = reloj.recibir("cliente", req.getLamportTime(), req.getPayload());

        String payload = req.getPayload().trim().toUpperCase();
        String resultado;

        if (payload.equals("HORA")) {
            resultado = "HORA_ACTUAL:" + LocalTime.now();

        } else if (payload.startsWith("ECO ")) {
            resultado = "ECO:" + req.getPayload().substring(4);

        } else {
            resultado = "ERROR:comando desconocido -> " + req.getPayload();
        }

        log.info("[Nodo-" + nodoId + "] Operar: " + req.getPayload()
                + " -> " + resultado + " (Lamport=" + t + ")");

        long tResp = reloj.eventoInterno("responder");
        obs.onNext(OperacionResponse.newBuilder()
                .setResultado(resultado)
                .setLamportTime(tResp)
                .setNodoId(nodoId)
                .setExito(true)
                .build());
        obs.onCompleted();
    }

    @Override
    public void heartbeat(HeartbeatRequest req,
                          io.grpc.stub.StreamObserver<HeartbeatResponse> obs) {

        long t = reloj.recibir("Nodo-" + req.getNodoId(),
                req.getLamportTime(), "HB");
        obs.onNext(HeartbeatResponse.newBuilder()
                .setNodoId(nodoId)
                .setLamportTime(t)
                .build());
        obs.onCompleted();
    }

    @Override
    public void eleccion(EleccionRequest req,
                         io.grpc.stub.StreamObserver<EleccionResponse> obs) {

        reloj.recibir("Nodo-" + req.getNodoIdEmisor(),
                req.getLamportTime(), "ELECTION");
        log.info("[Nodo-" + nodoId + "] ELECTION de Nodo-" + req.getNodoIdEmisor());

        obs.onNext(EleccionResponse.newBuilder().setOk(true).build());
        obs.onCompleted();
    }

    @Override
    public void coordinador(CoordinadorRequest req,
                            io.grpc.stub.StreamObserver<CoordinadorResponse> obs) {

        reloj.recibir("Nodo-" + req.getNodoIdCoordinador(),
                req.getLamportTime(), "COORDINATOR");
        coordinadorId.set(req.getNodoIdCoordinador());
        log.info("[Nodo-" + nodoId + "] Nuevo coordinador: Nodo-"
                + req.getNodoIdCoordinador());

        obs.onNext(CoordinadorResponse.newBuilder().setRecibido(true).build());
        obs.onCompleted();
    }

    // ── Main: levantar el servidor ───────────────────────────────────────────
    public static void main(String[] args) throws IOException, InterruptedException {
        int id     = args.length > 0 ? Integer.parseInt(args[0]) : 1;
        int puerto = 9000 + id; // Nodo 1 → 9001, Nodo 2 → 9002, etc.

        Server server = ServerBuilder.forPort(puerto)
                .intercept(new AuthInterceptor()) // Parte E
                .addService(new NodoGrpc(id))
                .build()
                .start();

        log.info("[Nodo-" + id + "] Escuchando en :" + puerto);
        server.awaitTermination();
    }
}
