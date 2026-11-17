package dev.logplatform;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class Limits extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest q, HttpServletResponse r, FilterChain c) throws ServletException, IOException {
        if (q.getHeader("Content-Encoding") != null) {
            r.sendError(415);
            return;
        }
        if (q.getContentLengthLong() > 1048576) {
            r.sendError(413);
            return;
        }
        HttpServletRequestWrapper bounded = new HttpServletRequestWrapper(q) {
            public ServletInputStream getInputStream() throws IOException {
                var in = q.getInputStream();
                return new ServletInputStream() {
                    int count;

                    public boolean isFinished() {
                        return in.isFinished();
                    }

                    public boolean isReady() {
                        return in.isReady();
                    }

                    public void setReadListener(ReadListener l) {
                        in.setReadListener(l);
                    }

                    public int read() throws IOException {
                        int v = in.read();
                        if (v >= 0 && ++count > 1048576) throw new IOException("body limit exceeded");
                        return v;
                    }
                };
            }
        };
        c.doFilter(bounded, r);
    }
}
