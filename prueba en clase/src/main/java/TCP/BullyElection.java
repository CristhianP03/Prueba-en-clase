package TCP;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class BullyElection {

    enum TipoMensaje { ELECTION, OK, COORDINATOR }

    record MensajeBully(TipoMensaje tipo, int emisor, int destino) {}

    static class Proceso implements Runnable {
        final int id;
        final int totalProcesos;
        final BlockingQueue<MensajeBully>[] buzones;
        final Proceso[] todosLosProcesos; // ← para filtrar activos al enviar
        volatile boolean activo;
        volatile int liderActual = -1;

        final AtomicBoolean enEleccion = new AtomicBoolean(false);
        static final AtomicInteger liderId = new AtomicInteger(-1);

        @SuppressWarnings("unchecked")
        Proceso(int id, int total, BlockingQueue<MensajeBully>[] buzones,
                boolean activo) {
            this.id              = id;
            this.totalProcesos   = total;
            this.buzones         = buzones;
            this.activo          = activo;
            this.todosLosProcesos = new Proceso[0];
        }

        @Override
        public void run() {
            Thread.currentThread().setName("Proceso-" + id);
            System.out.printf("[P%d] Iniciado. Activo=%b%n", id, activo);

            while (activo) {
                try {
                    MensajeBully msg = buzones[id].poll(500, TimeUnit.MILLISECONDS);
                    if (msg != null) procesarMensaje(msg);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.printf("[P%d] Finalizado.%n", id);
        }

        void procesarMensaje(MensajeBully msg) {
            switch (msg.tipo()) {
                case ELECTION -> {
                    System.out.printf("[P%d] Recibe elección de P%d%n", id, msg.emisor());
                    enviar(TipoMensaje.OK, msg.emisor());
                    if (enEleccion.compareAndSet(false, true)) iniciarEleccion();
                }
                case OK -> {
                    System.out.printf("[P%d] Recibe OK de P%d -> alguien mas grande existe%n",
                            id, msg.emisor());
                    enEleccion.set(false);
                }
                case COORDINATOR -> {
                    liderActual = msg.emisor();
                    liderId.set(liderActual);
                    enEleccion.set(false);
                    System.out.printf("[P%d] El lider es: P%d%n", id, liderActual);
                }
            }
        }

        void iniciarEleccion() {
            System.out.printf("[P%d] Iniciando elección...%n", id);
            boolean hayMasGrande = false;

            // ── Solo enviar a procesos con mayor ID que estén activos ─────────
            for (int i = id + 1; i < totalProcesos; i++) {
                if (procs != null && procs[i] != null && procs[i].activo) {
                    enviar(TipoMensaje.ELECTION, i);
                    hayMasGrande = true;
                }
            }

            if (!hayMasGrande) {
                proclamarLider();
            } else {
                CompletableFuture.delayedExecutor(1500, TimeUnit.MILLISECONDS)
                        .execute(() -> {
                            if (enEleccion.get()) {
                                proclamarLider();
                            }
                        });
            }
        }

        void proclamarLider() {
            liderActual = id;
            liderId.set(id);
            enEleccion.set(false);
            System.out.printf("[P%d] Yo soy el líder! Enviando como coordinador a todos.%n", id);
            for (int i = 0; i < totalProcesos; i++) {
                if (i != id) enviar(TipoMensaje.COORDINATOR, i);
            }
        }

        void enviar(TipoMensaje tipo, int destino) {
            if (destino < totalProcesos) {
                try {
                    buzones[destino].put(new MensajeBully(tipo, id, destino));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        void simularFallo() {
            System.out.printf("[P%d] *** SIMULANDO FALLO ***%n", id);
            activo = false;
        }
    }

    static Proceso[] procs;

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        final int N = 5;
        BlockingQueue<MensajeBully>[] buzones = new BlockingQueue[N];
        for (int i = 0; i < N; i++) {
            buzones[i] = new LinkedBlockingQueue<>();
        }

        procs = new Proceso[N];
        for (int i = 0; i < N; i++) {
            procs[i] = new Proceso(i, N, buzones, true);
        }

        ExecutorService exec = Executors.newFixedThreadPool(N);
        for (Proceso p : procs) exec.submit(p);

        procs[4].proclamarLider();
        Thread.sleep(1000);

        System.out.println("\n--- Simulando fallo del lider P4 ---\n");
        procs[4].simularFallo();

        Thread.sleep(500);
        procs[1].enEleccion.set(true);
        procs[1].iniciarEleccion();

        Thread.sleep(3000);
        System.out.printf("%nEl lider es: P%d%n", Proceso.liderId.get());

        exec.shutdownNow();
    }
}
