package fr.insalyon.creatis.vip.core.server;

import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMappingJacksonResponseBodyAdvice;

import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.core.server.inter.DataViews;

@RestControllerAdvice
public class DataViewsHandler extends AbstractMappingJacksonResponseBodyAdvice {

    private final Supplier<User> supplier;

    @Autowired
    public DataViewsHandler(Supplier<User> supplier) {
        this.supplier = supplier;
    }

    @Override
    protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType, MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response) {
        final User currentUser = supplier.get();

        // currentUser can be null if unauthorized context
        if (currentUser != null) {
            switch (currentUser.getLevel()) {
                case Administrator:
                    bodyContainer.setSerializationView(DataViews.Admin.class);
                    break;
                case Developer:
                    bodyContainer.setSerializationView(DataViews.Developer.class);
                    break;
                default:
                    bodyContainer.setSerializationView(DataViews.User.class);
                    break;
            }
        }
    }
}
