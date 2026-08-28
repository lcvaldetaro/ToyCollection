package com.gepetto.toycollection.models

data class ToyCounts (
    var cost : Float = 0F,
    var value: Float = 0F,
    var total : Int = 0,
    var totalN : Int = 0,
    var totalHo : Int = 0,
    var totalO: Int = 0,
    var total132: Int = 0,
    var total124: Int = 0,
    var totalRepro: Int = 0,
    var totalFactory: Int = 0,
    var totalMakers: Int = 0,
) {

    fun add (toyCounts : ToyCounts) {
        cost += toyCounts.cost
        value += toyCounts.value
        total += toyCounts.total
        totalN += toyCounts.totalN
        totalHo += toyCounts.totalHo
        totalO += toyCounts.totalO
        total132 += toyCounts.total132
        total124 += toyCounts.total124
        totalRepro += toyCounts.totalRepro
        totalFactory += toyCounts.totalFactory
        totalMakers += toyCounts.totalMakers
    }

    override fun toString () : String {
        return (
            "\nTotal models = ${total}" +
            "\nTotal manufacturers = ${totalMakers}" +
            "\nFactory models = ${totalFactory}" +
            "\nReproduction models = ${totalRepro}" +
            "\nValue = ${value}" +
            "\nCost  = ${cost}" +
            "\n1/24 scale or bigger models = ${total124}" +
            "\n1/32 scale models = ${total132}" +
            "\nO scale models = ${totalO}" +
            "\nHO scale models = ${totalHo}" +
            "\nN scale models = ${totalN}"
        )
    }

    companion object {
        fun countToys(makers: List<Maker>): ToyCounts {
            val toyCounts = ToyCounts(totalMakers = makers.size)

            for (maker in makers) {
                maker.normalizeData()
                toyCounts.add(countSingleMakerToys(maker))
            }

            return toyCounts
        }

        fun countSingleMakerToys(maker: Maker) : ToyCounts {
            var cost = 0.0F
            var value = 0.0F
            var count = 0
            var count132 = 0
            var count124 = 0
            var countHo = 0
            var countN = 0
            var countO = 0
            var countRepro = 0
            var countFactory = 0

            for (toy in maker.toysList) {
                if (toy.traded.isEmpty()) {
                    toy.normalizeData()
                    try { cost += toy.amountPaid.toFloat() } catch (_: Exception) { }
                    try { value += toy.value.toFloat() } catch (_: Exception) { }

                    if(toy.normalizedScale == "1/32")
                        count132++

                    if (toy.normalizedScale == "1/24" || toy.normalizedScale == "1/18")
                        count124++

                    if (toy.normalizedScale == "1/44")
                        countO++

                    if (toy.normalizedScale == "1/88")
                        countHo++

                    if (toy.normalizedScale == "1/144")
                        countN++

                    if (toy.repro.isYes())
                        countRepro++
                    else
                        if (toy.bodyMaker != "Valdetaro" && toy.chassisMaker != "Scratch" && toy.bodyMaker == toy.chassisMaker)
                            countFactory++

                    count++
                }
            }

            return ToyCounts (
                cost = cost,
                value = value,
                total = count,
                totalN = countN,
                totalHo = countHo,
                totalO = countO,
                total132 = count132,
                total124 = count124,
                totalRepro = countRepro,
                totalFactory = countFactory
            )
        }
    }
}
