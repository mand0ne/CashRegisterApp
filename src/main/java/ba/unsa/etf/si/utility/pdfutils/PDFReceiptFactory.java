package ba.unsa.etf.si.utility.pdfutils;

import ba.unsa.etf.si.App;
import ba.unsa.etf.si.models.Receipt;
import ba.unsa.etf.si.models.ReceiptItem;
import ba.unsa.etf.si.utility.date.DateUtils;
import com.itextpdf.kernel.color.Color;
import com.itextpdf.kernel.color.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfOutputIntent;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.border.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.zugferd.ZugferdConformanceLevel;
import com.itextpdf.zugferd.ZugferdDocument;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class PDFReceiptFactory {

    private static String DEST;
    private static final String ICC = "color/sRGB_CS_profile.icm";
    private static final String REGULAR = "fonts/OpenSans-Regular.ttf";
    private static final String BOLD = "fonts/OpenSans-Bold.ttf";
    private static final String NEWLINE = "\n";

    Receipt receipt;
    PdfFont bold = null;

    public PDFReceiptFactory(Receipt receipt) {
        this.receipt = receipt;
        DEST = App.cashRegister.getReceiptPath();
    }

    public void createPdf() throws IOException {
        ZugferdDocument pdfDocument = new ZugferdDocument(
                new PdfWriter(getDestination()), ZugferdConformanceLevel.ZUGFeRDBasic,
                new PdfOutputIntent("Custom", "", "https://www.color.org",
                        "sRGB IEC61966-2.1", App.class.getResourceAsStream(ICC)));

        Document document = new Document(pdfDocument);
        document.setFont(PdfFontFactory.createFont(App.class.getResourceAsStream(REGULAR).readAllBytes(), true))
                .setFontSize(12);

        this.bold = PdfFontFactory.createFont(App.class.getResourceAsStream(BOLD).readAllBytes(), true);
        Date dat = DateUtils.asDate(receipt.getDate());
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);
        String dateStr = formatter.format(dat);
        document.add(new Paragraph().setTextAlignment(TextAlignment.RIGHT).setMultipliedLeading(1)
                .add(new Text(String.format("RECEIPT %s\n", receipt.getTimestampID().substring(6))).setFont(bold).setFontSize(14))
                .add(dateStr).add(", " + receipt.getDate().toString().substring(11, 19)));

        document.add(getCashRegisterInfoTable());
        document.add(createReceiptItemTable());
        document.add(createTotalsTable());
        document.close();
    }

    private String getDestination() {
        Path path = Paths.get(DEST);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Paths.get(path.normalize().toString(), "receipt_" + receipt.getTimestampID() + ".pdf").normalize().toString();
    }

    private Table createTotalsTable() {
        Table table = new Table(new UnitValue[]{
                new UnitValue(UnitValue.PERCENT, 20f),
                new UnitValue(UnitValue.PERCENT, 20f),
                new UnitValue(UnitValue.PERCENT, 20f),
                new UnitValue(UnitValue.PERCENT, 15f),
                new UnitValue(UnitValue.PERCENT, 10f)})
                .setWidth(UnitValue.createPercentValue(100));
        table.addCell(createHeaderCell("Base amount "));
        table.addCell(createHeaderCell("Discount amount "));
        table.addCell(createHeaderCell("VAT "));
        table.addCell(createHeaderCell("Total"));
        table.addCell(createHeaderCell("Curr."));

        double totalDiscountValue = getTotalDiscountValue();
        String discountAmount = ((totalDiscountValue != 0) ? "-" : "") + totalDiscountValue;
        table.addCell(createCell(Double.toString(Math.round((receipt.getAmount() + totalDiscountValue - receipt.getVATPrice()) * 100.0) / 100.0))
                .setTextAlignment(TextAlignment.RIGHT));
        table.addCell(createCell(discountAmount)
                .setTextAlignment(TextAlignment.RIGHT));
        table.addCell(createCell(Double.toString(receipt.getVATPrice()))
                .setTextAlignment(TextAlignment.RIGHT));
        table.addCell(createBoldTextCell(Double.toString(receipt.getAmount()))
                .setTextAlignment(TextAlignment.RIGHT));
        table.addCell(createCell("KM")
                .setTextAlignment(TextAlignment.RIGHT));
        return table;
    }


    private double getTotalDiscountValue() {
        double d = 0;
        for (ReceiptItem item : receipt.getReceiptItems()) {
            d += item.getDiscountValue();
        }
        return BigDecimal.valueOf(d).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private Table createReceiptItemTable() {
        Table table = new Table(
                new UnitValue[]{
                        new UnitValue(UnitValue.PERCENT, 25.75f),
                        new UnitValue(UnitValue.PERCENT, 7.5f),
                        new UnitValue(UnitValue.PERCENT, 6.5f),
                        new UnitValue(UnitValue.PERCENT, 12.5f),
                        new UnitValue(UnitValue.PERCENT, 11.5f),
                        new UnitValue(UnitValue.PERCENT, 11.5f),
                        new UnitValue(UnitValue.PERCENT, 10.5f)})
                .setWidthPercent(100)
                .setMarginTop(10).setMarginBottom(10);
        table.addHeaderCell(createHeaderCell("Name of product"));
        table.addHeaderCell(createHeaderCell("Quantity"));
        table.addHeaderCell(createHeaderCell("Unit"));
        table.addHeaderCell(createHeaderCell("Discount (%)"));
        table.addHeaderCell(createHeaderCell("Disc. value"));
        table.addHeaderCell(createHeaderCell("VAT value"));
        table.addHeaderCell(createHeaderCell("Total"));

        for (ReceiptItem item : receipt.getReceiptItems()) {
            //name of product
            table.addCell(createCell(item.getName()));
            // quantity
            table.addCell(createCell(Double.toString(item.getQuantity())).setTextAlignment(TextAlignment.RIGHT));
            // unit
            table.addCell(createCell(item.getUnit()).setTextAlignment(TextAlignment.RIGHT));
            // discount
            table.addCell(createCell(Double.toString(item.getDiscount())).setTextAlignment(TextAlignment.RIGHT));
            // discounted value
            table.addCell(createCell(Double.toString(item.getDiscountValue())).setTextAlignment(TextAlignment.RIGHT));
            // VAT value
            table.addCell(createCell(Double.toString(item.getVATValue())).setTextAlignment(TextAlignment.RIGHT));
            //total
            table.addCell(createCell(Double.toString(BigDecimal.valueOf(item.getTotalPrice()).setScale(2, RoundingMode.HALF_UP).doubleValue())).setTextAlignment(TextAlignment.RIGHT));
        }
        return table;
    }


    private Cell createCell(String text) {

        return new Cell().setPadding(0.8f)
                .add(new Paragraph(text)
                        .setMultipliedLeading(1));
    }

    private Cell createBoldTextCell(String text) {

        return new Cell().setPadding(0.8f)
                .add(new Paragraph(text)
                        .setMultipliedLeading(1).setFont(bold));
    }

    private Cell createHeaderCell(String text) {
        Color color = new DeviceRgb(31, 107, 183);
        Color fColor = new DeviceRgb(255, 255, 255);

        return new Cell().setPadding(0.8f).setBackgroundColor(color)
                .add(new Paragraph(text)
                        .setMultipliedLeading(1).setFontColor(fColor).setFontSize(12));
    }

    private Table getCashRegisterInfoTable() {
        Table table = new Table(new UnitValue[]{
                new UnitValue(UnitValue.PERCENT, 50),
                new UnitValue(UnitValue.PERCENT, 50)})
                .setWidthPercent(100);
        try {
            table.addCell(getCashRegisterInfo("Receipt status: ",
                    (receipt.getReceiptStatus() != null) ? receipt.getReceiptStatus().name() : "N/A",
                    "Payment method: ",
                    (receipt.getPaymentMethod() != null) ? receipt.getPaymentMethod().name() : "N/A",
                    "Cashier: ",
                    receipt.getCashier(),
                    "Cash register ID: ",
                    Long.toString(App.cashRegister.getId())
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return table;
    }

    private Cell getCashRegisterInfo(String who, String name,
                                     String line1, String paymentMethod, String line2,
                                     String cashier, String line3, String cashregID) {
        Paragraph p = new Paragraph()
                .setMultipliedLeading(1.0f)
                .add(new Text(who).setFont(bold))
                .add(name).add(NEWLINE)
                .add(new Text(line1).setFont(bold))
                .add(paymentMethod).add(NEWLINE)
                .add(new Text(line2).setFont(bold))
                .add(cashier).add(NEWLINE)
                .add(new Text(line3).setFont(bold))
                .add(cashregID).add(NEWLINE)
                .add(new Text("Merchant ID: ").setFont(bold))
                .add(Long.toString(App.cashRegister.getMerchantID()));
        return new Cell().setBorder(Border.NO_BORDER).add(p);
    }


}
