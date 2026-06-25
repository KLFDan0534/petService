package com.pet.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Servlet filter that sets UTF-8 character encoding on HTTP responses
 * and ensures proper charset is appended to the Content-Type header
 * for JavaScript, CSS, and HTML responses.
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Component
public class CharsetFilter implements Filter {
    /**
     * Sets the response character encoding to UTF-8 and appends charset=UTF-8
     * to the Content-Type for JavaScript, CSS, and HTML responses.
     * @param request  the servlet request
     * @param response the servlet response
     * @param chain    the filter chain
     * @throws IOException      if an I/O error occurs
     * @throws ServletException if a servlet error occurs
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            HttpServletResponse res = (HttpServletResponse) response;
            res.setCharacterEncoding("UTF-8");
            String ct = res.getContentType();
            if (ct != null && (ct.contains("javascript") || ct.contains("text/css") || ct.contains("text/html"))) {
                res.setContentType(ct + ";charset=UTF-8");
            }
        }
        chain.doFilter(request, response);
    }
}
