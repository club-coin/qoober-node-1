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

import qoober.Exchange;
import qoober.QooberException;
import qoober.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllExchanges extends APIServlet.APIRequestHandler {

    static final GetAllExchanges instance = new GetAllExchanges();

    private GetAllExchanges() {
        super(new APITag[] {APITag.MS}, "timestamp", "firstIndex", "lastIndex", "includeCurrencyInfo");
    }
    
    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws QooberException {
        final int timestamp = ParameterParser.getTimestamp(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeCurrencyInfo = "true".equalsIgnoreCase(req.getParameter("includeCurrencyInfo"));

        JSONObject response = new JSONObject();
        JSONArray exchanges = new JSONArray();
        try (DbIterator<Exchange> exchangeIterator = Exchange.getAllExchanges(firstIndex, lastIndex)) {
            while (exchangeIterator.hasNext()) {
                Exchange exchange = exchangeIterator.next();
                if (exchange.getTimestamp() < timestamp) {
                    break;
                }
                exchanges.add(JSONData.exchange(exchange, includeCurrencyInfo));
            }
        }
        response.put("exchanges", exchanges);
        return response;
    }

}
