package blf.blockchains.ethereum.variables;

import blf.blockchains.ethereum.state.EthereumProgramState;
import blf.core.exceptions.ExceptionHandler;
import blf.core.values.BlockchainVariables;
import blf.core.values.ValueAccessor;
import blf.core.values.Variable;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DataSourceAccessors
 */
public class EthereumVariables implements BlockchainVariables {

    public ValueAccessor currentBlockNumberAccessor() {
        return state -> {
            try {
                return ((EthereumProgramState) state).getReader().getCurrentBlock().getNumber();
            } catch (Exception e) {
                ExceptionHandler.getInstance().handleException("Error when retrieving the current block number.", e);
            }

            return null;
        };
    }

    @Override
    public Map<String, String> getBlockVariableNamesAndTypes() {
        return getVariableNamesAndType(EthereumBlockVariables.BLOCK_VARIABLES);
    }

    @Override
    public Map<String, String> getTransactionVariableNamesAndTypes() {
        return getVariableNamesAndType(EthereumTransactionVariables.TRANSACTION_VARIABLES);
    }

    @Override
    public Map<String, String> getLogEntryVariableNamesAndTypes() {
        return getVariableNamesAndType(EthereumLogEntryVariables.LOG_ENTRY_VARIABLES);
    }

    private Map<String, String> getVariableNamesAndType(Set<Variable> variables) {
        return variables.stream().collect(Collectors.toMap(Variable::getName, Variable::getType));
    }

    @Override
    public ValueAccessor getValueAccessor(String name) {
        return findVariable(name, Variable::getAccessor);
    }

    private static <T> T findVariable(final String name, final Function<Variable, T> mapper) {
        return variableStream().filter(variable -> variable.hasName(name)).map(mapper).findFirst().orElse(null);
    }

    private static Stream<Variable> variableStream() {
        return Stream.concat(
            EthereumBlockVariables.BLOCK_VARIABLES.stream(),
            Stream.concat(
                EthereumTransactionVariables.TRANSACTION_VARIABLES.stream(),
                EthereumLogEntryVariables.LOG_ENTRY_VARIABLES.stream()
            )
        );
    }

}
