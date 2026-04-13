package fr.insalyon.creatis.vip.core.server;

import jakarta.servlet.annotation.MultipartConfig;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Enables servlet multipart handling for Spring MVC endpoints using @RequestPart.
 */
@MultipartConfig
public class MultipartDispatcherServlet extends DispatcherServlet {

    private static final long serialVersionUID = 1L;
}
