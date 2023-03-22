package array

private fun bindingsBeyondIndex(env: Environment, level: Int): List<EnvironmentBinding> {
    val l = env.localBindings().filter { b -> b.frameIndex > level }.toMutableList()
//        val l = ArrayList<EnvironmentBinding>()
//        val loc = localBindings()
//        for(b in loc) {
//            if(b.frameIndex > level) {
//                l.add(b)
//            }
//        }
    env.childEnvironments.forEach { c ->
        val inner = bindingsBeyondIndex(c, level + 1)
        l += inner
    }
    return l
}

fun depthOfEnv(baseEnv: Environment, env: Environment): Int {
    var curr = baseEnv
    var i = 0
    while (true) {
        if (curr === env) {
            return i
        }
        i++
        curr = curr.parent ?: throw IllegalStateException("Can't find env in parent list")
    }
}

private fun Environment.rewriteForEscape() {
    val a = bindingsBeyondIndex(this, 0)
    val b = a.groupBy { it.storage }
    b.forEach { (k, v) ->
        val copiedStorage = ExternalStorageRef(depthOfEnv(this, k.env), k.index)
        val storageDescriptor = StackStorageDescriptor(this, storageList.size + externalStorageList.size, "copied from: ${k.comment}")
        externalStorageList.add(copiedStorage)
        v.forEach { b ->
            b.updateStorage(storageDescriptor)
        }
    }
}

//private fun Environment.checkEscapeReturns() {
//    returnTargets.forEach { target ->
//        var curr: Environment? = this
//        while (curr != null && curr !== target.env) {
//            if (curr.canEscape()) {
//                throw ParseException("Cannot return across escaped stack frames", target.pos)
//            }
//            curr = curr.parent
//        }
//    }
//}

fun Environment.escapeAnalysis() {
    fun recurse(env: Environment) {
        if (env.canEscape()) {
            env.rewriteForEscape()
        }
        env.childEnvironments.forEach(::recurse)
    }

    recurse(this)
}

fun Environment.freeVariableRefs(): List<EnvironmentBinding> {
    return bindingsBeyondIndex(this, 0)
}
