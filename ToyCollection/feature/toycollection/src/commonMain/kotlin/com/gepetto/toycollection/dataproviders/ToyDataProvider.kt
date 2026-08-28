package com.gepetto.toycollection.dataproviders

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.gepetto.toycollection.models.Toy

data class ToyProvider(val toy: Toy)

class ToyDataProvider : PreviewParameterProvider<ToyProvider> {
    override val values: Sequence<ToyProvider>
        get() = sequenceOf(
            ToyProvider(toy1),
            ToyProvider(toy2),
            ToyProvider(toy3),
            ToyProvider(toy4),
            ToyProvider(toy5),
            ToyProvider(toy6),
            ToyProvider(toy7),
        )

    companion object {
        val toy1 = Toy(
            refNum = "1",
            makerCombo = "Monogram",
            condition = "C10",
            scale = "1/24",
            description = "1965 Chaparral 2 - 1st Issue - Racer",
            comments = "",
            bodyMaker = "Monogram",
            chassisMaker = "Monogram",
            chassisType = "in line T",
            motorMaker = "Monogram",
            motorDetails = "16D EBD",
            repro = "y",
            boxed = "y",
            color = "White",
            catalogNumber = "",
            majorWork = "",
            minorWork = "",
            soundFile = "",
            detail = "",
            toMake = "",
            buy = "",
            traded = "",
            value = "200.00",
            amountPaid = "100.00",
            amountSold = "",
            bitmaps =  "monogram.jpg",
            bitmapsTimeStamp =  "",
            picture =  "",
            pictureTimeStamp =  "",
            hasPicture =  "",
            factoryToy = "y",
            acquired =  "",
        )

        val toy2 = Toy(
            refNum = "2",
            makerCombo = "Monogram",
            condition = "C10",
            scale = "1/32",
            description = "1965 Lola T70",
            comments = "",
            bodyMaker = "Monogram",
            chassisMaker = "Monogram",
            chassisType = "in line T",
            motorMaker = "Monogram",
            motorDetails = "16D EBD",
            repro = "",
            boxed = "y",
            color = "Blue",
            catalogNumber = "",
            majorWork = "",
            minorWork = "",
            soundFile = "",
            detail = "",
            toMake = "",
            buy = "",
            traded = "",
            value = "200.00",
            amountPaid = "100.00",
            amountSold = "",
            bitmaps =  "",
            bitmapsTimeStamp =  "",
            picture =  "",
            pictureTimeStamp =  "",
            hasPicture =  "y",
            factoryToy = "y",
            acquired =  "",
        )

        val toy3 = Toy(
            refNum = "3",
            makerCombo = "Cox",
            condition = "C10",
            scale = "1/32",
            description = "1965 Cheetah",
            comments = "",
            bodyMaker = "Cox",
            chassisMaker = "Cox",
            chassisType = "in line M",
            motorMaker = "Cox",
            motorDetails = "16D EBD",
            repro = "",
            boxed = "y",
            color = "Black",
            catalogNumber = "",
            majorWork = "",
            minorWork = "",
            soundFile = "",
            detail = "",
            toMake = "",
            buy = "",
            traded = "",
            value = "150.00",
            amountPaid = "50.00",
            amountSold = "",
            bitmaps =  "",
            bitmapsTimeStamp =  "",
            picture =  "",
            pictureTimeStamp =  "",
            hasPicture =  "y",
            factoryToy = "y",
            acquired =  "",
        )

        val toy4 = Toy(
            refNum = "4",
            makerCombo = "Cox",
            condition = "C10",
            scale = "1/32",
            description = "1965 Ford GT used in the 24 Hours of Lemans in 1965",
            comments = "",
            bodyMaker = "Cox",
            chassisMaker = "Cox",
            chassisType = "in line M",
            motorMaker = "Cox",
            motorDetails = "16D EBD",
            repro = "",
            boxed = "y",
            color = "Blue",
            catalogNumber = "",
            majorWork = "",
            minorWork = "",
            soundFile = "",
            detail = "",
            toMake = "",
            buy = "",
            traded = "",
            value = "150.00",
            amountPaid = "50.00",
            amountSold = "",
            bitmaps =  "",
            bitmapsTimeStamp =  "",
            picture =  "",
            pictureTimeStamp =  "",
            hasPicture =  "y",
            factoryToy = "y",
            acquired =  "",
        )

        val toy5 = Toy(
            refNum = "5",
            makerCombo = "Estrela",
            condition = "C10",
            scale = "1/32",
            description = "1966 Ford J",
            comments = "",
            bodyMaker = "Estrela",
            chassisMaker = "Estrela",
            chassisType = "in line IF",
            motorMaker = "Estrela",
            motorDetails = "16D REB Oxford",
            repro = "",
            boxed = "y",
            color = "Blue",
            catalogNumber = "",
            majorWork = "",
            minorWork = "",
            soundFile = "",
            detail = "",
            toMake = "",
            buy = "",
            traded = "",
            value = "150.00",
            amountPaid = "50.00",
            amountSold = "",
            bitmaps =  "",
            bitmapsTimeStamp =  "",
            picture =  "",
            pictureTimeStamp =  "",
            hasPicture =  "y",
            factoryToy = "y",
            acquired =  "",
        )

        val toy6 = Toy(
            refNum = "6",
            makerCombo = "Estrela",
            condition = "C10",
            scale = "1/32",
            description = "1965 Chaparral 2",
            comments = "",
            bodyMaker = "Estrela",
            chassisMaker = "Estrela",
            chassisType = "in line IF",
            motorMaker = "Estrela",
            motorDetails = "16D REB Oxford",
            repro = "",
            boxed = "y",
            color = "Blue",
            catalogNumber = "",
            majorWork = "",
            minorWork = "",
            soundFile = "",
            detail = "",
            toMake = "",
            buy = "",
            traded = "lost",
            value = "150.00",
            amountPaid = "50.00",
            amountSold = "",
            bitmaps =  "",
            bitmapsTimeStamp =  "",
            picture =  "",
            pictureTimeStamp =  "",
            hasPicture =  "y",
            factoryToy = "y",
            acquired =  "",
        )

        val toy7 = toy6.copy(
            refNum = "7",
            description = "1965 Cheetah with very long description to force a second line"
        )

        var toy8 = toy3.copy(
            refNum = "8",
            description = "1965 Cheetah with very long description to force a second line"
        )

        var toy9 = toy4.copy(
            refNum = "9",
            description = "1966 Chaparral 2D"
        )

        val allToys = listOf(toy1, toy2, toy3, toy4, toy5, toy6 ,toy7, toy8, toy9)
        val coxToys = listOf(toy3, toy4, toy8, toy9)
        val monogramToys = listOf(toy1, toy2)
        val estrelaToys = listOf(toy5, toy6)

    }
}
