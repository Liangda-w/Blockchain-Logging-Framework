package blf.configuration;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import blf.core.parameters.Parameter;
import blf.blockchains.ethereum.classes.EthereumPublicMemberQueryEthereum;
import blf.blockchains.ethereum.classes.EthereumSmartContractParameter;
import blf.blockchains.ethereum.classes.EthereumSmartContractQuery;
import io.reactivex.annotations.NonNull;

/**
 * SmartContractFilter
 */
public class SmartContractQuerySpecification {
    private final EthereumSmartContractQuery query;

    private SmartContractQuerySpecification(EthereumSmartContractQuery query) {
        assert query != null;
        this.query = query;
    }

    public EthereumSmartContractQuery getQuery() {
        return this.query;
    }

    public static SmartContractQuerySpecification ofMemberVariable(@NonNull ParameterSpecification variable) {
        final EthereumSmartContractQuery query = new EthereumPublicMemberQueryEthereum(
            variable.getParameter().getName(),
            Collections.emptyList(),
            Collections.singletonList(variable.getParameter())
        );
        return new SmartContractQuerySpecification(query);
    }

    public static SmartContractQuerySpecification ofMemberFunction(
        @NonNull String functionName,
        @NonNull List<TypedValueAccessorSpecification> inputParameters,
        @NonNull List<ParameterSpecification> outputParameters
    ) {

        final List<EthereumSmartContractParameter> inputs = inputParameters.stream()
            .map(SmartContractQuerySpecification::createSmartContractParameter)
            .collect(Collectors.toList());

        final List<Parameter> outputs = outputParameters.stream().map(ParameterSpecification::getParameter).collect(Collectors.toList());

        return new SmartContractQuerySpecification(new EthereumPublicMemberQueryEthereum(functionName, inputs, outputs));
    }

    private static EthereumSmartContractParameter createSmartContractParameter(TypedValueAccessorSpecification param) {
        final String name = createParameterName();
        return new EthereumSmartContractParameter(param.getType(), name, param.getAccessor());
    }

    private static long counter = 0;

    private static String createParameterName() {
        return String.format("param%s", counter++);
    }
}
