package org.votingsystem.util;


import org.votingsystem.dto.currency.IncomesDto;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class MapUtils {

    public static Map<String, IncomesDto> getTagMapForIncomes(String tag, BigDecimal total, BigDecimal timeLimited) {
        Map<String, IncomesDto> result = new HashMap<>();
        result.put(tag, new IncomesDto(total, timeLimited));
        return result;
    }

    public static Map<String, BigDecimal> getTagMapForExpenses(String tag, BigDecimal amount) {
        Map<String, BigDecimal> result = new HashMap<>();
        result.put(tag, amount);
        return result;
    }

}
