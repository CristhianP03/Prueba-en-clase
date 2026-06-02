package comun;

public class Mensaje {

    private String tipo;
    private String token;
    private String payload;
    private long lamportTime;
    private int nodoId;

    public Mensaje() {
    }

    public Mensaje(String tipo, String token, String payload,
            long lamportTime, int nodoId) {
        this.tipo = tipo;
        this.token = token;
        this.payload = payload;
        this.lamportTime = lamportTime;
        this.nodoId = nodoId;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getTipo() {
        return tipo;
    }

    public String getToken() {
        return token;
    }

    public String getPayload() {
        return payload;
    }

    public long getLamportTime() {
        return lamportTime;
    }

    public int getNodoId() {
        return nodoId;
    }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public void setLamportTime(long lamportTime) {
        this.lamportTime = lamportTime;
    }

    public void setNodoId(int nodoId) {
        this.nodoId = nodoId;
    }

    @Override
    public String toString() {
        return "Message{tipo='" + tipo + "', nodoId=" + nodoId
                + ", lamport=" + lamportTime + ", payload='" + payload + "'}";
    }
}
