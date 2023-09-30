package array.clientweb2

private var audioContext: dynamic = null

fun initAudio() {
    if (audioContext == null) {
        audioContext = js("new AudioContext()")
    }
}

fun playAudioBuffer(audioData: dynamic) {
    if (audioContext == null) {
        return
    }
    val conf = js("{}")
    conf.numberOfChannels = 1
    conf.length = audioData.length
    conf.sampleRate = audioContext.sampleRate
    val buf = js("new AudioBuffer(conf)")
    val channelData = buf.getChannelData(0)
    repeat(audioData.length) { i ->
        channelData[i] = audioData[i]
    }
    val src = audioContext.createBufferSource()
    src.buffer = buf
    src.connect(audioContext.destination)
    src.start()
}
