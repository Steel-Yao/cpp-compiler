package org.yyds.optimizer;

import org.yyds.ir.Quadruple;

import java.util.List;

public interface OptimizationPass {
    List<Quadruple> apply(List<Quadruple> input);
}
