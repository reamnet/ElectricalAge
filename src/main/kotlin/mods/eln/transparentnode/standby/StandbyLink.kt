package mods.eln.transparentnode.standby
import java.util.WeakHashMap

/** A weakly registered loaded generator; no world or unit references are stored in map values. */
interface GeneratorPeer { val unit: StandbyGeneratorUnit; fun publish() }

/** Weak owners, dimension-scoped requests and a five-second lease prevent stale remote starts. */
object StandbyLink {
    private data class Request(val channel: String,val expiry: Long)
    private val worlds=WeakHashMap<Any,WeakHashMap<Any,Request>>()
    // Values contain no owner/unit references, so unloaded generators remain collectable.
    private val generators=WeakHashMap<Any,WeakHashMap<GeneratorPeer,Long>>()
    data class ResetResult(val linked: Int,val reset: Int)
    fun updateGenerator(world: Any,owner: GeneratorPeer,tick: Long) {
        generators.getOrPut(world) { WeakHashMap() }[owner]=tick+100
    }
    fun removeGenerator(world: Any,owner: GeneratorPeer) { generators[world]?.remove(owner) }
    /** Explicit one-shot command to presently loaded peers; never queues a future restart. */
    fun resetGenerators(world: Any,value: String,tick: Long): ResetResult {
        val channel=channel(value)
        if(channel.isBlank())return ResetResult(0,0)
        val entries=generators[world] ?: return ResetResult(0,0)
        entries.entries.removeIf { it.value<tick }
        val linked=entries.keys.toList().filter { channel(it.unit.channel)==channel }
        var reset=0
        for(generator in linked)if(generator.unit.fault.isNotEmpty()) {
            generator.unit.reset();generator.publish();reset++
        }
        return ResetResult(linked.size,reset)
    }
    fun channel(value: String)=value.trim().take(64)
    fun update(world: Any,owner: Any,channel: String,tick: Long,wanted: Boolean) {
        val entries=worlds.getOrPut(world) { WeakHashMap() }
        if(!wanted || channel.isBlank())entries.remove(owner) else entries[owner]=Request(channel,tick+100)
        entries.entries.removeIf { it.value.expiry<tick }
    }
    fun remove(world: Any,owner: Any) { worlds[world]?.remove(owner) }
    fun requested(world: Any,channel: String,tick: Long): Boolean = channel.isNotBlank() &&
        worlds[world]?.values?.any { it.channel==channel && it.expiry>=tick }==true
}
