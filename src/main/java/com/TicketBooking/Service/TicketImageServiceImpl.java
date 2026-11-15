package com.TicketBooking.Service;

import com.TicketBooking.Model.Booking;
import com.TicketBooking.Model.Show;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Renders the e-ticket to match the site's own visual language (see
 * src/App.css :root — white surfaces, near-black text, single cinema-red
 * accent, Inter type) rather than an unrelated look, so the emailed ticket
 * reads as the same product as the web UI.
 */
@Service
public class TicketImageServiceImpl implements TicketImageService {

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 420;
    private static final int STUB_WIDTH = 300; // right-hand section holding the QR code
    private static final int QR_SIZE = 200;

    // Colors lifted straight from the site's :root palette in App.css
    private static final Color BG = Color.WHITE;
    private static final Color BG_ALT = new Color(0xF6, 0xF7, 0xF9);   // --bg-alt (stub background)
    private static final Color BORDER = new Color(0xE7, 0xE8, 0xEC);   // --border
    private static final Color TEXT = new Color(0x14, 0x15, 0x1A);     // --text
    private static final Color TEXT_MUTED = new Color(0x6B, 0x6D, 0x78); // --text-muted
    private static final Color ACCENT = new Color(0xE1, 0x1D, 0x3C);   // --accent (cinema red)
    private static final Color ACCENT_DARK = new Color(0xB7, 0x13, 0x2D); // --accent-dark

    // "Inter" is the site's font; falls back to the platform sans-serif on
    // hosts where it isn't installed (AWT silently substitutes a default).
    private static final String FONT_FAMILY = "Inter";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    @Override
    public byte[] generateTicketImage(Booking booking, List<String> seatNumbers, String transactionId) {
        Show show = booking.getShow();
        String movieName = show.getMovie().getMovieName();
        String theatreName = show.getScreen().getTheatre().getTheatreName();
        String screenName = show.getScreen().getScreenName();
        String seatsJoined = String.join(", ", seatNumbers);

        int infoWidth = WIDTH - STUB_WIDTH;

        BufferedImage ticket = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = ticket.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // ---- card background (mirrors .booking-confirm-card: white surface,
            // 1px light border, rounded corners) ----
            g.setColor(BG);
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(BG_ALT);
            g.fillRect(infoWidth, 0, STUB_WIDTH, HEIGHT);
            g.setColor(BORDER);
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(1, 1, WIDTH - 3, HEIGHT - 3, 14, 14);

            // ---- perforation between info panel and QR stub, styled like
            // .ticket-btn--stub's punched-notch divider ----
            drawPerforation(g, infoWidth, HEIGHT);

            // ---- wordmark: "✦ Velvet" + "Reel" in accent, same as Navbar.jsx ----
            g.setFont(new Font(FONT_FAMILY, Font.BOLD, 22));
            FontMetrics wm = g.getFontMetrics();
            int wx = 40;
            g.setColor(ACCENT);
            g.drawString("\u2726", wx, 46);
            wx += wm.stringWidth("\u2726 ");
            g.setColor(TEXT);
            g.drawString("Velvet", wx, 46);
            wx += wm.stringWidth("Velvet");
            g.setColor(ACCENT);
            g.drawString("Reel", wx, 46);

            // ---- eyebrow: small red dash + uppercase label, like .hero-eyebrow ----
            g.setColor(ACCENT);
            g.fillRoundRect(40, 60, 22, 2, 2, 2);
            g.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
            g.setColor(ACCENT_DARK);
            g.drawString("E-TICKET  \u2022  BOOKING CONFIRMED", 68, 66);

            g.setColor(BORDER);
            g.fillRect(40, 84, infoWidth - 80, 1);

            // ---- movie title ----
            g.setColor(TEXT);
            g.setFont(new Font(FONT_FAMILY, Font.BOLD, 32));
            drawTruncated(g, movieName, 40, 132, infoWidth - 80);

            // ---- theatre / screen ----
            g.setColor(TEXT_MUTED);
            g.setFont(new Font(FONT_FAMILY, Font.PLAIN, 15));
            g.drawString(theatreName + "  \u2022  " + screenName, 40, 160);

            // ---- detail grid: date, time, seats, then booking/txn/amount ----
            int colWidth = (infoWidth - 80) / 3;
            int gridY = 218;
            drawField(g, "DATE", show.getShowDate().format(DATE_FMT), 40, gridY, colWidth);
            drawField(g, "TIME", show.getStartTime().format(TIME_FMT), 40 + colWidth, gridY, colWidth);
            drawField(g, "SEATS", seatsJoined.isEmpty() ? "-" : seatsJoined, 40 + 2 * colWidth, gridY, colWidth);

            g.setColor(BORDER);
            g.fillRect(40, gridY + 30, infoWidth - 80, 1);

            int gridY2 = gridY + 70;
            drawField(g, "BOOKING ID", "#" + booking.getId(), 40, gridY2, colWidth);
            drawField(g, "TRANSACTION ID", transactionId, 40 + colWidth, gridY2, colWidth);
            drawField(g, "AMOUNT PAID", "\u20B9" + formatAmount(booking.getTotalAmount()), 40 + 2 * colWidth, gridY2, colWidth);

            // ---- footer note ----
            g.setColor(TEXT_MUTED);
            g.setFont(new Font(FONT_FAMILY, Font.ITALIC, 12));
            g.drawString("Valid for a single entry \u2014 please arrive 15 minutes before showtime.", 40, HEIGHT - 26);

            // ---- QR stub ----
            String qrPayload = buildQrPayload(booking, seatNumbers, transactionId);
            BufferedImage qr = renderQrCode(qrPayload, QR_SIZE);
            int qrX = infoWidth + (STUB_WIDTH - QR_SIZE) / 2;
            int qrY = 66;

            g.setColor(BG);
            g.setStroke(new BasicStroke(1f));
            g.fill(new RoundRectangle2D.Double(qrX - 14, qrY - 14, QR_SIZE + 28, QR_SIZE + 28, 12, 12));
            g.setColor(BORDER);
            g.draw(new RoundRectangle2D.Double(qrX - 14, qrY - 14, QR_SIZE + 28, QR_SIZE + 28, 12, 12));
            g.drawImage(qr, qrX, qrY, null);

            g.setColor(ACCENT_DARK);
            g.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
            String scanText = "SCAN AT ENTRY";
            FontMetrics fm = g.getFontMetrics();
            int scanX = infoWidth + (STUB_WIDTH - fm.stringWidth(scanText)) / 2;
            g.drawString(scanText, scanX, qrY + QR_SIZE + 42);

            g.setColor(TEXT_MUTED);
            g.setFont(new Font(FONT_FAMILY, Font.PLAIN, 11));
            String bookingLabel = "Booking #" + booking.getId();
            fm = g.getFontMetrics();
            int labelX = infoWidth + (STUB_WIDTH - fm.stringWidth(bookingLabel)) / 2;
            g.drawString(bookingLabel, labelX, qrY + QR_SIZE + 60);

        } finally {
            g.dispose();
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(ticket, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to render ticket image", e);
        }
    }

    // ---- helpers ----

    private String formatAmount(Double amount) {
        if (amount == null) return "0";
        // Matches the site, which prints the raw total (no forced decimals)
        if (amount == Math.floor(amount)) {
            return String.valueOf(amount.longValue());
        }
        return String.format("%.2f", amount);
    }

    private String buildQrPayload(Booking booking, List<String> seatNumbers, String transactionId) {
        Show show = booking.getShow();
        return "VELVET-REEL-TICKET|" +
                "booking=" + booking.getId() +
                "|txn=" + transactionId +
                "|movie=" + show.getMovie().getMovieName() +
                "|date=" + show.getShowDate() +
                "|time=" + show.getStartTime() +
                "|seats=" + String.join(",", seatNumbers);
    }

    private BufferedImage renderQrCode(String payload, int size) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            // QR modules drawn in the site's near-black text color rather than
            // pure black, so it sits visually with the rest of the ticket.
            int dark = TEXT.getRGB();
            int light = BG.getRGB();
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? dark : light);
                }
            }
            return image;
        } catch (WriterException e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    private void drawField(Graphics2D g, String label, String value, int x, int y, int maxWidth) {
        g.setColor(TEXT_MUTED);
        g.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
        g.drawString(label, x, y);

        g.setColor(TEXT);
        g.setFont(new Font(FONT_FAMILY, Font.BOLD, 17));
        drawTruncated(g, value, x, y + 24, maxWidth - 12);
    }

    private void drawTruncated(Graphics2D g, String text, int x, int y, int maxWidth) {
        FontMetrics fm = g.getFontMetrics();
        String display = text;
        if (fm.stringWidth(display) > maxWidth) {
            while (display.length() > 1 && fm.stringWidth(display + "\u2026") > maxWidth) {
                display = display.substring(0, display.length() - 1);
            }
            display = display + "\u2026";
        }
        g.drawString(display, x, y);
    }

    /**
     * Punched-notch divider, the same visual device as .ticket-btn--stub /
     * the hero ticket cutouts: two semicircle "bites" out of the top and
     * bottom edges at the seam, plus a dashed line between them.
     */
    private void drawPerforation(Graphics2D g, int x, int height) {
        g.setColor(BG);
        g.fill(new Ellipse2D.Double(x - 13, -13, 26, 26));
        g.fill(new Ellipse2D.Double(x - 13, height - 13, 26, 26));

        g.setColor(BORDER);
        g.setStroke(new BasicStroke(1f));
        g.draw(new Ellipse2D.Double(x - 13, -13, 26, 26));
        g.draw(new Ellipse2D.Double(x - 13, height - 13, 26, 26));

        g.setColor(new Color(TEXT_MUTED.getRed(), TEXT_MUTED.getGreen(), TEXT_MUTED.getBlue(), 130));
        float[] dash = {5f, 5f};
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
        g.drawLine(x, 16, x, height - 16);
    }
}
