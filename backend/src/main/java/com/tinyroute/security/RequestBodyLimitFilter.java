package com.tinyroute.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ReadListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestBodyLimitFilter extends OncePerRequestFilter {

    static final long MAX_REQUEST_BODY_BYTES = 16 * 1024;

    private final SecurityErrorResponseWriter securityErrors;

    public RequestBodyLimitFilter(SecurityErrorResponseWriter securityErrors) {
        this.securityErrors = securityErrors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_REQUEST_BODY_BYTES) {
            securityErrors.requestBodyTooLarge(response);
            return;
        }

        try {
            filterChain.doFilter(new LimitedBodyRequest(request), response);
        } catch (RequestBodyTooLargeException exception) {
            if (!response.isCommitted()) {
                securityErrors.requestBodyTooLarge(response);
            }
        }
    }

    private static final class LimitedBodyRequest extends HttpServletRequestWrapper {

        private ServletInputStream inputStream;

        private LimitedBodyRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (inputStream == null) {
                inputStream = new LimitedServletInputStream(super.getInputStream());
            }
            return inputStream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            return new BufferedReader(new InputStreamReader(
                    getInputStream(),
                    encoding == null ? StandardCharsets.UTF_8 : java.nio.charset.Charset.forName(encoding)
            ));
        }
    }

    private static final class LimitedServletInputStream extends ServletInputStream {

        private final ServletInputStream delegate;
        private long bytesRead;

        private LimitedServletInputStream(ServletInputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            recordBytes(value == -1 ? 0 : 1);
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (length == 0) {
                return 0;
            }
            int permittedLength = (int) Math.min(length, MAX_REQUEST_BODY_BYTES + 1 - bytesRead);
            if (permittedLength <= 0) {
                throw new RequestBodyTooLargeException();
            }
            int read = delegate.read(bytes, offset, permittedLength);
            recordBytes(Math.max(read, 0));
            return read;
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }

        private void recordBytes(long count) throws RequestBodyTooLargeException {
            bytesRead += count;
            if (bytesRead > MAX_REQUEST_BODY_BYTES) {
                throw new RequestBodyTooLargeException();
            }
        }
    }
}
