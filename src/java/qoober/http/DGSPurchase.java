/*
 * Copyright © 2013-2016 The Qoober Core Developers.
 * Copyright © 2016-2020 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Qoober software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package qoober.http;

import qoober.*;
import qoober.Qoober;
import qoober.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static qoober.http.JSONResponses.INCORRECT_DELIVERY_DEADLINE_TIMESTAMP;
import static qoober.http.JSONResponses.INCORRECT_PURCHASE_PRICE;
import static qoober.http.JSONResponses.INCORRECT_PURCHASE_QUANTITY;
import static qoober.http.JSONResponses.MISSING_DELIVERY_DEADLINE_TIMESTAMP;
import static qoober.http.JSONResponses.UNKNOWN_GOODS;

public final class DGSPurchase extends CreateTransaction {

    static final DGSPurchase instance = new DGSPurchase();

    private DGSPurchase() {
        super(new APITag[] {APITag.DGS, APITag.CREATE_TRANSACTION},
                "goods", "priceNQT", "quantity", "deliveryDeadlineTimestamp");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws QooberException {

        DigitalGoodsStore.Goods goods = ParameterParser.getGoods(req);
        if (goods.isDelisted()) {
            return UNKNOWN_GOODS;
        }

        int quantity = ParameterParser.getGoodsQuantity(req);
        if (quantity > goods.getQuantity()) {
            return INCORRECT_PURCHASE_QUANTITY;
        }

        long priceNQT = ParameterParser.getPriceNQT(req);
        if (priceNQT != goods.getPriceNQT()) {
            return INCORRECT_PURCHASE_PRICE;
        }

        String deliveryDeadlineString = Convert.emptyToNull(req.getParameter("deliveryDeadlineTimestamp"));
        if (deliveryDeadlineString == null) {
            return MISSING_DELIVERY_DEADLINE_TIMESTAMP;
        }
        int deliveryDeadline;
        try {
            deliveryDeadline = Integer.parseInt(deliveryDeadlineString);
            if (deliveryDeadline <= Qoober.getEpochTime()) {
                return INCORRECT_DELIVERY_DEADLINE_TIMESTAMP;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_DELIVERY_DEADLINE_TIMESTAMP;
        }

        Account buyerAccount = ParameterParser.getSenderAccount(req);
        Account sellerAccount = Account.getAccount(goods.getSellerId());

        Attachment attachment = new Attachment.DigitalGoodsPurchase(goods.getId(), quantity, priceNQT,
                deliveryDeadline);
        try {
            return createTransaction(req, buyerAccount, sellerAccount.getId(), 0, attachment);
        } catch (QooberException.InsufficientBalanceException e) {
            return JSONResponses.NOT_ENOUGH_FUNDS;
        }

    }

}