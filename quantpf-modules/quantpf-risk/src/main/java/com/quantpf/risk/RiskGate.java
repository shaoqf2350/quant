package com.quantpf.risk;
public interface RiskGate { RiskDecision evaluate(RiskContext context); void haltGlobal(String reason); void resumeGlobal(); }