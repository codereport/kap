package com.dhsdevelopments.kap.gui2

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException


class HtmlTransferable(
    private val plain: String,
    private val html: String
) : Transferable {
    @Throws(UnsupportedFlavorException::class, IOException::class)
    override fun getTransferData(flavour: DataFlavor): Any {
        return when {
            flavour.equals(DataFlavor.fragmentHtmlFlavor) -> html
            flavour.equals(DataFlavor.stringFlavor) -> plain
            else -> throw UnsupportedFlavorException(flavour)
        }
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(
            DataFlavor.fragmentHtmlFlavor,
            DataFlavor.stringFlavor)
    }

    override fun isDataFlavorSupported(flavour: DataFlavor): Boolean {
        return flavour == DataFlavor.fragmentHtmlFlavor || flavour == DataFlavor.stringFlavor
    }
}
