package blf.blockchains.ethereum.instructions;

import blf.blockchains.ethereum.state.EthereumProgramState;
import blf.core.instructions.Instruction;
import blf.core.state.ProgramState;

public class EthereumConnectInstruction extends Instruction {
    @Override
    public void execute(ProgramState state) {
        EthereumProgramState ethereumState = (EthereumProgramState) state;

        ethereumState.getReader().connect(ethereumState.getConnectionUrl());
    }

}
