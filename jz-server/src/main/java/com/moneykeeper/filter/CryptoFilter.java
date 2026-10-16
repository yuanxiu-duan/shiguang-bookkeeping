package com.moneykeeper.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneykeeper.util.AesUtil;
import com.moneykeeper.util.RsaUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 加解密过滤器
 *
 * 请求解密流程：
 *   POST/PUT/PATCH/DELETE → body 含 encKey + encData → RSA 解密 AES 密钥 → AES 解密 body
 *   GET                   → header X-Enc-Key          → RSA 解密 AES 密钥 → 仅加密响应
 *
 * 响应加密流程：
 *   若请求携带了加密标记，则将响应体 AES 加密后返回 { encData: "..." }
 *   若请求未加密，则原样返回（向后兼容）
 */
public class CryptoFilter implements Filter {

    /** 跳过加解密的路径（文件上传/下载、公钥接口、公开检查更新） */
    private static final Set<String> EXCLUDED_PREFIXES = Set.of(
            "/api/auth/public-key",
            "/api/auth/avatar",
            "/api/transactions/template",
            "/api/transactions/export",
            "/api/transactions/import",
            "/api/admin/app-releases/upload-file",
            "/api/admin/app-releases/check-update"
    );

    private final RsaUtil rsaUtil;
    private final ObjectMapper objectMapper;

    public CryptoFilter(RsaUtil rsaUtil, ObjectMapper objectMapper) {
        this.rsaUtil = rsaUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String uri = req.getRequestURI();

        // 跳过排除路径
        if (isExcluded(uri)) {
            chain.doFilter(request, response);
            return;
        }

        String method = req.getMethod();
        String encKey = null;
        String encData = null;

        if ("GET".equals(method)) {
            // GET 请求：从 header 获取加密密钥
            encKey = req.getHeader("X-Enc-Key");
            if (encKey == null) {
                // 未加密，直接透传
                chain.doFilter(request, response);
                return;
            }
        } else if ("POST".equals(method) || "PUT".equals(method)
                || "PATCH".equals(method) || "DELETE".equals(method)) {
            // 读取请求体
            String body = readBody(req);
            if (body == null || body.isEmpty()) {
                chain.doFilter(request, response);
                return;
            }

            try {
                JsonNode node = objectMapper.readTree(body);
                if (node.has("encKey") && node.has("encData")) {
                    encKey = node.get("encKey").asText();
                    encData = node.get("encData").asText();
                } else {
                    // 非加密请求，用原始 body 透传
                    chain.doFilter(new BodyCachingRequestWrapper(req, body), response);
                    return;
                }
            } catch (Exception e) {
                // 非 JSON 或非加密格式，透传
                chain.doFilter(new BodyCachingRequestWrapper(req, body), response);
                return;
            }
        } else {
            // OPTIONS / HEAD 等，直接透传
            chain.doFilter(request, response);
            return;
        }

        // ---- 解密 AES 密钥 ----
        String aesKeyBase64;
        String aesIvBase64;
        try {
            String decryptedKeyInfo = rsaUtil.decrypt(encKey);
            String[] parts = decryptedKeyInfo.split("\\.");
            if (parts.length != 2) {
                writeError(res, 4001, "公钥已过期，请重新获取");
                return;
            }
            aesKeyBase64 = parts[0];
            aesIvBase64 = parts[1];
        } catch (Exception e) {
            // 公钥不匹配（通常是服务端重启后旧公钥失效）
            writeError(res, 4001, "公钥已过期，请重新获取");
            return;
        }

        // ---- 解密请求体 ----
        HttpServletRequest wrappedRequest;
        if (encData != null) {
            String decryptedBody;
            try {
                decryptedBody = AesUtil.decrypt(encData, aesKeyBase64, aesIvBase64);
            } catch (Exception e) {
                writeError(res, 4001, "公钥已过期，请重新获取");
                return;
            }
            wrappedRequest = new BodyCachingRequestWrapper(req, decryptedBody);
        } else {
            wrappedRequest = req;
        }

        // 将 AES 密钥存入 request attribute，供后续使用
        wrappedRequest.setAttribute("_cryptoAesKey", aesKeyBase64);
        wrappedRequest.setAttribute("_cryptoAesIv", aesIvBase64);

        // ---- 包装响应以捕获输出 ----
        CachedResponseWrapper responseWrapper = new CachedResponseWrapper(res);

        chain.doFilter(wrappedRequest, responseWrapper);

        // ---- 加密响应 ----
        String responseBody = responseWrapper.getContent();
        if (responseBody != null && !responseBody.isEmpty()) {
            String encryptedResponse = AesUtil.encrypt(responseBody, aesKeyBase64, aesIvBase64);
            res.setStatus(responseWrapper.getStatus());
            res.setContentType("application/json");
            res.setCharacterEncoding("UTF-8");
            res.getWriter().write("{\"encData\":\"" + encryptedResponse + "\"}");
        }
    }

    private boolean isExcluded(String uri) {
        for (String path : EXCLUDED_PREFIXES) {
            if (uri.equals(path)) return true;
        }
        return false;
    }

    private String readBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            char[] buffer = new char[4096];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
        }
        return sb.toString();
    }

    private void writeError(HttpServletResponse res, int code, String msg) throws IOException {
        // 公钥失效场景统一返回 HTTP 400 + 业务码 4001，便于前端识别重试
        int httpStatus = code == 4001 ? 400 : code;
        res.setStatus(httpStatus);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"code\":" + code + ",\"msg\":\"" + msg + "\"}");
    }

    // ==================== 内部包装类 ====================

    /**
     * 请求包装类：用缓存的 body 替换原始输入流
     */
    static class BodyCachingRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;

        BodyCachingRequestWrapper(HttpServletRequest request, String body) {
            super(request);
            this.body = body.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public boolean isFinished() { return bais.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {}
                @Override public int read() { return bais.read(); }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override public int getContentLength() { return body.length; }
        @Override public long getContentLengthLong() { return body.length; }
    }

    /**
     * 响应包装类：捕获写入的输出，供加密后写回原始响应
     */
    static class CachedResponseWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
        private int status = 200;

        CachedResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return new ServletOutputStream() {
                @Override public boolean isReady() { return true; }
                @Override public void setWriteListener(WriteListener listener) {}
                @Override public void write(int b) { baos.write(b); }
                @Override public void write(byte[] b, int off, int len) { baos.write(b, off, len); }
            };
        }

        @Override
        public PrintWriter getWriter() {
            return new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8), true);
        }

        @Override public void setStatus(int sc) { this.status = sc; super.setStatus(sc); }
        @Override public void sendError(int sc) throws IOException { this.status = sc; super.sendError(sc); }
        @Override public void sendError(int sc, String msg) throws IOException { this.status = sc; super.sendError(sc, msg); }

        public int getStatusValue() { return status; }
        public String getContent() { return baos.toString(StandardCharsets.UTF_8); }
    }
}
