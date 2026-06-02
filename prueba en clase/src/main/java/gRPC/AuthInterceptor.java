package gRPC;

import io.grpc.*;

import java.util.Set;

public class AuthInterceptor implements ServerInterceptor {

    static final Metadata.Key<String> TOKEN_KEY =
            Metadata.Key.of("x-token", Metadata.ASCII_STRING_MARSHALLER);

    private static final Set<String> TOKENS_VALIDOS =
            Set.of("token-cliente-001", "token-cliente-002");

    @Override
    public <Q, R> ServerCall.Listener<Q> interceptCall(
            ServerCall<Q, R> call,
            Metadata headers,
            ServerCallHandler<Q, R> next) {

        String token = headers.get(TOKEN_KEY);

        if (token == null || !TOKENS_VALIDOS.contains(token)) {
            call.close(Status.UNAUTHENTICATED.withDescription("Token invalido o ausente"), headers);
            return new ServerCall.Listener<>() {}; // listener vacío, rechaza
        }

        return next.startCall(call, headers);
    }
}
