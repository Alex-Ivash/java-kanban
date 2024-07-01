package service.server.handler;

import java.io.IOException;

public interface EndpointHandler<E, T> {
    void accept(E e, T t) throws IOException;
}
