package com.sivacrackers.billing;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sivacrackers.common.ShopTime;
import com.sivacrackers.settings.BusinessSettings;
import com.sivacrackers.settings.BusinessSettingsRepository;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * PDF documents: invoices for in-store bills and simple report tables.
 * Business details come only from the saved business settings – empty fields are left out, never invented.
 * No tax, GST or licence lines are added.
 */
@Component
public class PdfDocuments {

    private static final Color NAVY = new Color(11, 22, 54);
    private static final Color LIGHT = new Color(241, 245, 249);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, NAVY);
    private static final Font H2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, NAVY);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 9.5f);
    private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f);
    private static final Font HEAD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font VOID = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(185, 28, 28));

    private final BusinessSettingsRepository settings;

    public PdfDocuments(BusinessSettingsRepository settings) {
        this.settings = settings;
    }

    /** "Rs. 1,250.50" – the standard PDF fonts have no ₹ sign. */
    static String rs(BigDecimal v) {
        NumberFormat f = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"));
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return "Rs. " + f.format(v == null ? BigDecimal.ZERO : v);
    }

    static String when(Instant i) {
        return i == null ? "" : LocalDateTime.ofInstant(i, ShopTime.ZONE).format(WHEN);
    }

    @Transactional(readOnly = true)
    public byte[] invoice(Bill bill) {
        BusinessSettings s = settings.findSingleton().orElse(null);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A5, 28, 28, 28, 28);
            PdfWriter.getInstance(doc, out);
            doc.addTitle("Bill " + bill.getBillNumber());
            doc.open();

            doc.add(new Paragraph(s == null || s.getShopName() == null ? "SIVA Crackers" : s.getShopName(), TITLE));
            for (String line : businessLines(s)) {
                doc.add(new Paragraph(line, SMALL));
            }
            doc.add(Chunk.NEWLINE);

            PdfPTable head = new PdfPTable(new float[] {1, 1});
            head.setWidthPercentage(100);
            head.addCell(plain(new Phrase("BILL / INVOICE\n", H2), Element.ALIGN_LEFT));
            head.addCell(plain(new Phrase(bill.getBillNumber(), H2), Element.ALIGN_RIGHT));
            head.addCell(plain(new Phrase("Date: " + when(bill.getCreatedAt()), BODY), Element.ALIGN_LEFT));
            head.addCell(plain(new Phrase("Billed by: " + nz(bill.getCreatedByName()), BODY), Element.ALIGN_RIGHT));
            String customer = bill.getCustomerName() == null ? "Walk-in customer"
                    : bill.getCustomerName() + (bill.getCustomerPhone() == null ? "" : " · " + bill.getCustomerPhone());
            PdfPCell c = plain(new Phrase("Customer: " + customer, BODY), Element.ALIGN_LEFT);
            c.setColspan(2);
            head.addCell(c);
            doc.add(head);

            if (bill.getStatus() == Bill.Status.VOIDED) {
                Paragraph v = new Paragraph("VOIDED – " + when(bill.getVoidedAt()) + " – " + nz(bill.getVoidReason()), VOID);
                v.setSpacingBefore(6);
                doc.add(v);
            }

            PdfPTable t = new PdfPTable(new float[] {0.5f, 4, 1.2f, 1.6f, 1.8f});
            t.setWidthPercentage(100);
            t.setSpacingBefore(10);
            for (String h : new String[] {"#", "Item", "Qty", "Price", "Amount"}) {
                PdfPCell hc = new PdfPCell(new Phrase(h, HEAD));
                hc.setBackgroundColor(NAVY);
                hc.setPadding(5);
                hc.setHorizontalAlignment(h.equals("Item") || h.equals("#") ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
                t.addCell(hc);
            }
            t.setHeaderRows(1);
            for (BillItem i : bill.getItems()) {
                t.addCell(cell(String.valueOf(i.getLineNo()), Element.ALIGN_LEFT));
                t.addCell(cell(i.getProductName() + "\n" + i.getProductSku() + " · " + i.getPackSize(), Element.ALIGN_LEFT));
                t.addCell(cell(String.valueOf(i.getQuantity()), Element.ALIGN_RIGHT));
                t.addCell(cell(rs(i.getUnitPrice()), Element.ALIGN_RIGHT));
                t.addCell(cell(rs(i.getLineTotal()), Element.ALIGN_RIGHT));
            }
            doc.add(t);

            PdfPTable sum = new PdfPTable(new float[] {3, 1.6f});
            sum.setWidthPercentage(60);
            sum.setHorizontalAlignment(Element.ALIGN_RIGHT);
            sum.setSpacingBefore(6);
            sumRow(sum, "Subtotal", rs(bill.getSubtotal()), BODY);
            if (bill.getDiscountAmount().signum() > 0) {
                sumRow(sum, "Discount", "- " + rs(bill.getDiscountAmount()), BODY);
            }
            sumRow(sum, "Total", rs(bill.getTotal()), BOLD);
            sumRow(sum, "Payment", bill.getPaymentMethod().getLabel(), BODY);
            sumRow(sum, "Status", bill.getPaymentStatus() == Bill.PaymentStatus.PAID ? "Paid" : "Payment pending", BODY);
            if (bill.getAmountReceived() != null && bill.getPaymentStatus() == Bill.PaymentStatus.PAID) {
                sumRow(sum, "Received", rs(bill.getAmountReceived()), BODY);
                sumRow(sum, "Change", rs(bill.getAmountReceived().subtract(bill.getTotal()).max(BigDecimal.ZERO)), BODY);
            }
            doc.add(sum);

            if (bill.getNotes() != null) {
                Paragraph n = new Paragraph("Note: " + bill.getNotes(), SMALL);
                n.setSpacingBefore(8);
                doc.add(n);
            }
            Paragraph foot = new Paragraph("Sold in person at the shop counter. Thank you for visiting.", SMALL);
            foot.setSpacingBefore(14);
            foot.setAlignment(Element.ALIGN_CENTER);
            doc.add(foot);
            doc.close();
            return out.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new IllegalStateException("Could not create the invoice PDF", e);
        }
    }

    /** A titled table (reports). */
    public byte[] table(String title, String subtitle, String[] headers, List<Object[]> rows) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(headers.length > 5 ? PageSize.A4.rotate() : PageSize.A4, 28, 28, 28, 28);
            PdfWriter.getInstance(doc, out);
            doc.addTitle(title);
            doc.open();
            BusinessSettings s = settings.findSingleton().orElse(null);
            doc.add(new Paragraph((s == null || s.getShopName() == null ? "SIVA Crackers" : s.getShopName()) + " – " + title, TITLE));
            if (subtitle != null) {
                doc.add(new Paragraph(subtitle, SMALL));
            }
            PdfPTable t = new PdfPTable(headers.length);
            t.setWidthPercentage(100);
            t.setSpacingBefore(10);
            for (String h : headers) {
                PdfPCell hc = new PdfPCell(new Phrase(h, HEAD));
                hc.setBackgroundColor(NAVY);
                hc.setPadding(4);
                t.addCell(hc);
            }
            t.setHeaderRows(1);
            boolean shade = false;
            for (Object[] r : rows) {
                for (Object v : r) {
                    PdfPCell pc = cell(text(v), v instanceof Number ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                    if (shade) {
                        pc.setBackgroundColor(LIGHT);
                    }
                    t.addCell(pc);
                }
                shade = !shade;
            }
            if (rows.isEmpty()) {
                PdfPCell empty = cell("No data for this period.", Element.ALIGN_LEFT);
                empty.setColspan(headers.length);
                t.addCell(empty);
            }
            doc.add(t);
            Paragraph foot = new Paragraph("Generated " + when(Instant.now()) + " (Indian time). Figures come only from records saved in the system.", SMALL);
            foot.setSpacingBefore(10);
            doc.add(foot);
            doc.close();
            return out.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new IllegalStateException("Could not create the report PDF", e);
        }
    }

    private static String text(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof BigDecimal d) {
            return rs(d);
        }
        if (v instanceof Instant i) {
            return when(i);
        }
        return v.toString();
    }

    private static List<String> businessLines(BusinessSettings s) {
        List<String> lines = new ArrayList<>();
        if (s == null) {
            return lines;
        }
        List<String> address = new ArrayList<>();
        for (String part : new String[] {s.getAddressLine1(), s.getArea(), s.getCity(), s.getState(), s.getPincode()}) {
            if (part != null && !part.isBlank()) {
                address.add(part.trim());
            }
        }
        if (!address.isEmpty()) {
            lines.add(String.join(", ", address));
        }
        List<String> contact = new ArrayList<>();
        if (s.getPhone() != null) {
            contact.add("Phone " + s.getPhone());
        }
        if (s.getEmail() != null) {
            contact.add(s.getEmail());
        }
        if (!contact.isEmpty()) {
            lines.add(String.join(" · ", contact));
        }
        return lines;
    }

    private static PdfPCell plain(Phrase p, int align) {
        PdfPCell c = new PdfPCell(p);
        c.setBorder(PdfPCell.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setPaddingBottom(3);
        return c;
    }

    private static PdfPCell cell(String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, BODY));
        c.setHorizontalAlignment(align);
        c.setPadding(4);
        c.setBorderColor(new Color(226, 232, 240));
        return c;
    }

    private static void sumRow(PdfPTable t, String label, String value, Font f) {
        t.addCell(plain(new Phrase(label, f), Element.ALIGN_LEFT));
        t.addCell(plain(new Phrase(value, f), Element.ALIGN_RIGHT));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
