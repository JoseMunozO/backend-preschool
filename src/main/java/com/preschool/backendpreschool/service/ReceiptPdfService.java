package com.preschool.backendpreschool.service;

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
import com.preschool.backendpreschool.model.Parent;
import com.preschool.backendpreschool.model.Payment;
import com.preschool.backendpreschool.model.PaymentAllocation;
import com.preschool.backendpreschool.model.PaymentMethod;
import com.preschool.backendpreschool.model.Staff;
import com.preschool.backendpreschool.model.StudentCharge;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReceiptPdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String institutionName;

    public ReceiptPdfService(@Value("${app.receipt.institution-name}") String institutionName) {
        this.institutionName = institutionName;
    }

    public byte[] generateReceipt(Payment payment, List<PaymentAllocation> allocations) {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            document.add(new Paragraph(institutionName, titleFont));
            document.add(new Paragraph("Recibo de pago (documento informativo, no es un comprobante fiscal)", headingFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Recibo N.: " + formatReceiptNumber(payment.getPaymentId()), normalFont));
            document.add(new Paragraph("Fecha de pago: " + payment.getPaymentDate().format(DATE_FORMAT), normalFont));

            Parent parent = payment.getParent();
            if (parent != null) {
                document.add(new Paragraph("Padre/tutor: " + parent.getFirstName() + " " + parent.getLastName(), normalFont));
            }

            Staff staff = payment.getReceivedByStaff();
            if (staff != null) {
                document.add(new Paragraph("Recibido por: " + staff.getFirstName() + " " + staff.getLastName(), normalFont));
            }

            document.add(new Paragraph("Metodo de pago: " + translatePaymentMethod(payment.getPaymentMethod()), normalFont));
            if (payment.getReferenceNumber() != null) {
                document.add(new Paragraph("Referencia: " + payment.getReferenceNumber(), normalFont));
            }
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 3f, 1.5f});

            addHeaderCell(table, "Estudiante", tableHeaderFont);
            addHeaderCell(table, "Concepto", tableHeaderFont);
            addHeaderCell(table, "Monto", tableHeaderFont);

            for (PaymentAllocation allocation : allocations) {
                StudentCharge charge = allocation.getStudentCharge();
                table.addCell(new Phrase(
                        charge.getStudent().getFirstName() + " " + charge.getStudent().getLastName(), normalFont));
                table.addCell(new Phrase(charge.getChargeType().getName(), normalFont));

                PdfPCell amountCell = new PdfPCell(new Phrase(formatAmount(allocation.getAmountAllocated()), normalFont));
                amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(amountCell);
            }

            document.add(table);
            document.add(new Paragraph(" "));

            Paragraph total = new Paragraph("Total pagado: " + formatAmount(payment.getTotalAmount()), headingFont);
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            if (payment.getNotes() != null && !payment.getNotes().isBlank()) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Notas: " + payment.getNotes(), normalFont));
            }

            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el recibo PDF", e);
        }

        return outputStream.toByteArray();
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        table.addCell(new PdfPCell(new Phrase(text, font)));
    }

    private String formatReceiptNumber(Long paymentId) {
        return String.format("REC-%06d", paymentId);
    }

    private String formatAmount(BigDecimal amount) {
        return "RD$ " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String translatePaymentMethod(PaymentMethod method) {
        return switch (method) {
            case CASH -> "Efectivo";
            case CARD -> "Tarjeta";
            case TRANSFER -> "Transferencia bancaria";
            case SWISH -> "Swish";
            case OTHER -> "Otro";
        };
    }
}
