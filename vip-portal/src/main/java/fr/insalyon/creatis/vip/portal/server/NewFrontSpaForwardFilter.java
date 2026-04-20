package fr.insalyon.creatis.vip.portal.server;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * For SPA routes under /new_front, avoid 404 for other routes than /new_front
 */
public class NewFrontSpaForwardFilter implements Filter {

    @Override
    public void init(final FilterConfig filterConfig) {
        // No init
    }

    @Override
    public void doFilter(
            final ServletRequest request,
            final ServletResponse response,
            final FilterChain chain
    ) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final String method = httpRequest.getMethod();

        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        final String contextPath = httpRequest.getContextPath();
        final String requestUri = httpRequest.getRequestURI();
        final String path = requestUri.substring(contextPath.length());

        if (isSpaRoute(path)) {
            httpRequest.getRequestDispatcher("/new_front/index.html").forward(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No rsc to delete
    }

    private boolean isSpaRoute(final String path) {
        if (!path.startsWith("/new_front")) {
            return false;
        }

        if ("/new_front".equals(path) || "/new_front/".equals(path) || "/new_front/index.html".equals(path)) {
            return false;
        }

        final String normalized = path.substring("/new_front/".length());
        return !normalized.isEmpty() && !normalized.contains(".");
    }
}
