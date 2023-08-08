package com.dhsdevelopments.kap.gui2

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException


class HtmlTransferable(
    private val plain: String,
    private val html: String
) : Transferable {
    override fun getTransferData(flavour: DataFlavor): Any {
        return when {
            flavour.equals(DataFlavor.fragmentHtmlFlavor) -> html
            flavour.equals(DataFlavor.stringFlavor) -> plain
            else -> throw UnsupportedFlavorException(flavour)
        }
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return SUPPORTED_FLAVOURS.copyOf()
    }

    override fun isDataFlavorSupported(flavour: DataFlavor): Boolean {
        return SUPPORTED_FLAVOURS.contains(flavour)
    }

    companion object {
        val SUPPORTED_FLAVOURS = arrayOf(
            DataFlavor.fragmentHtmlFlavor,
            DataFlavor.stringFlavor)
    }
}
