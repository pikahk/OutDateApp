package ru.pikahk.outdateapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeTest {

    @Test
    fun `обычный штрихкод EAN-13 остаётся как есть`() {
        assertEquals("4607001771555", normalizeBarcode("4607001771555"))
    }

    @Test
    fun `короткий EAN-8 остаётся как есть`() {
        assertEquals("96385074", normalizeBarcode("96385074"))
    }

    @Test
    fun `американский UPC-A дополняется нулём до EAN-13`() {
        assertEquals("0036000291452", normalizeBarcode("036000291452"))
    }

    @Test
    fun `из кода Честного знака достаётся номер товара`() {
        val dataMatrix = "\u001D0104607001771555215Abc12\u001D93dGVz"
        assertEquals("4607001771555", normalizeBarcode(dataMatrix))
    }

    @Test
    fun `код с префиксом сканера тоже разбирается`() {
        assertEquals("4607001771555", normalizeBarcode("]d20104607001771555215Abc12"))
    }

    @Test
    fun `неверная контрольная цифра отбрасывается`() {
        assertNull(normalizeBarcode("4607001771556"))
    }

    @Test
    fun `текст и QR-ссылки не считаются штрихкодом`() {
        assertNull(normalizeBarcode("https://example.com"))
        assertNull(normalizeBarcode("12345"))
    }
}
