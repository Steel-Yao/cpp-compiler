package org.yyds.optimizer;

import org.yyds.ir.Quadruple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record BasicBlock(String name, List<Quadruple> quadruples) {
    public BasicBlock(String name, List<Quadruple> quadruples) {
        this.name = name;
        this.quadruples = new ArrayList<>(quadruples);
    }

    @Override
    public List<Quadruple> quadruples() {
        return Collections.unmodifiableList(quadruples);
    }
}
