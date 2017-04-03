package org.votingsystem.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;
import org.votingsystem.util.SystemOperation;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.OperationType;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SystemOperationDeserializer extends StdDeserializer<SystemOperation> {

    private static final Logger log = Logger.getLogger(SystemOperationDeserializer.class.getName());

    public SystemOperationDeserializer() {
        this(null);
    }

    public SystemOperationDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SystemOperation deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        TextNode node = jsonParser.getCodec().readTree(jsonParser);
        String operationName = node.asText();
        for(CurrencyOperation currencyOperation : CurrencyOperation.values()) {
            if(currencyOperation.name().equals(operationName))
                return currencyOperation;
        }
        for(OperationType operationType : OperationType.values()) {
            if(operationType.name().equals(operationName))
                return operationType;
        }
        return null;
    }

}
