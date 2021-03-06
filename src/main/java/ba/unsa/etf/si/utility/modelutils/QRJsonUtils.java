package ba.unsa.etf.si.utility.modelutils;

import ba.unsa.etf.si.App;
import ba.unsa.etf.si.controllers.PrimaryController;
import ba.unsa.etf.si.models.Receipt;
import ba.unsa.etf.si.models.ReceiptItem;
import ba.unsa.etf.si.routes.CashRegisterRoutes;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class QRJsonUtils {

    private QRJsonUtils() {
    }

    private static String receiptItemsString(List<ReceiptItem> list) {
        StringBuilder receiptItems = new StringBuilder();
        for (ReceiptItem receiptItem : list) {
            int itemQuantity = (int) receiptItem.getQuantity();
            receiptItems.append(receiptItem.getName()).append(" (").append(itemQuantity).append(")");
            receiptItems.append(",");
        }
        return StringUtils.chop(receiptItems.toString());
    }

    public static String getDynamicQRCode(Receipt receipt) {
        return "{\n" +
                "\"cashRegisterId\": " + App.cashRegister.getId() + ",\n" +
                "\"officeId\": " + App.cashRegister.getOfficeID() + ",\n" +
                "\"businessName\": \"" + App.cashRegister.getMerchantName() + "\",\n" +
                "\"receiptId\": \"" + receipt.getTimestampID() + "\",\n" +
                "\"service\": \"" + receiptItemsString(receipt.getReceiptItems()) + "\",\n" +
                "\"totalPrice\": " + receipt.getAmount() + "\n" +
                "}";
    }

    public static String getStaticQRCode() {
        App.cashRegister.setUuid(CashRegisterRoutes.getCashRegisterUUID(PrimaryController.currentUser.getToken()));
        return "{\n" +
                "\"cashRegisterId\": " + App.cashRegister.getId() + ",\n" +
                "\"officeId\": " + App.cashRegister.getOfficeID() + ",\n" +
                "\"businessName\": \"" + App.cashRegister.getMerchantName() + "\",\n" +
                "\"uuid\": \"" + App.cashRegister.getUuid() + "\"\n" +
                "}";
    }
}
