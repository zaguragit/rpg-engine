package io.posidon.uranium.debug

class MainLogger(verbose: Boolean) : Logger() {

    val verbose = if (verbose) Logger() else null
}