package com.defi.lending.loader;

import com.defi.lending.exception.DataLoadException;
import com.defi.lending.model.Operation;

import java.util.List;

/**
 * Reads {@code operations.csv} in file order. The list is the execution order; this loader does not sort it.
 */
public final class OperationLoader {

    /**
     * Columns, in order: timestamp, accountId, action, symbol, amount.
     * timestamp is an ISO-8601 local date-time. action is a name from {@code OperationType}.
     */
    public List<Operation> load(String resourceName) throws DataLoadException {
        // TODO: read the classpath resource with the rules in PLAN.md.
        // Map each data row to an Operation. Throw DataLoadException, with the file name and line,
        // on a missing file, a bad timestamp or number, an unknown action, or a non-positive amount.
        // Keep the file order, including equal timestamps.
        throw new UnsupportedOperationException("OperationLoader.load");
    }
}
