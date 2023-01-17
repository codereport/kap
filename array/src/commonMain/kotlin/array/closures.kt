package array

fun Instruction.escapeAnalysis() {
    if (this.canEscape()) {

    }
}

fun Instruction.canEscape(): Boolean {
    return this is APLParser.EvalLambdaFnx
}
