package com.certichain.server.web.servlet;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Plain HttpServlet (no Spring MVC) that renders a QR code PNG for the verification link
 * of a certificate: {@code /qr?certNo=CIT-2026-XXXX}. The link is built from the request,
 * so it works on whatever host/port the server is reached on.
 */
@WebServlet(name = "qrCodeServlet", urlPatterns = "/qr")
public class QrCodeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String certNo = req.getParameter("certNo");
        if (certNo == null || !certNo.matches("[A-Z0-9-]{4,30}")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "certNo is required");
            return;
        }
        int size = 220;
        try {
            size = Math.max(120, Math.min(600, Integer.parseInt(req.getParameter("size"))));
        } catch (NumberFormatException ignored) {
            // keep the default
        }
        String base = req.getScheme() + "://" + req.getServerName()
                + (isDefaultPort(req) ? "" : ":" + req.getServerPort()) + req.getContextPath();
        String url = base + "/verify/" + certNo + "?via=qr";
        try {
            BitMatrix matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, size, size,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 1));
            resp.setContentType("image/png");
            resp.setHeader("Cache-Control", "public, max-age=86400");
            MatrixToImageWriter.writeToStream(matrix, "PNG", resp.getOutputStream());
        } catch (WriterException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "QR encoding failed");
        }
    }

    private static boolean isDefaultPort(HttpServletRequest req) {
        return ("http".equals(req.getScheme()) && req.getServerPort() == 80)
                || ("https".equals(req.getScheme()) && req.getServerPort() == 443);
    }
}
