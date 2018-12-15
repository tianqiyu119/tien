import com.underwood.idgenerator.IDGenerator

fun main(args: Array<String>) {
    for (i in 1..1000) {
        val nextId = IDGenerator.nextId()
        println(nextId)
    }
}