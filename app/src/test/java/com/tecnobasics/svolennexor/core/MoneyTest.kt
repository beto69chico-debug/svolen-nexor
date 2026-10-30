package com.tecnobasics.svolennexor.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun formatea_pesos_con_miles() {
        assertEquals("$12,500", Money.format(1_250_000))
        assertEquals("$12,500.50", Money.format(1_250_050))
        assertEquals("$0", Money.format(0))
        assertEquals("$999", Money.format(99_900))
        assertEquals("$1,000,000.05", Money.format(100_000_005))
        assertEquals("-$150", Money.format(-15_000))
        assertEquals("$1,500.00", Money.format(150_000, alwaysDecimals = true))
    }

    @Test
    fun lee_lo_que_se_escribe() {
        assertEquals(1_250_000L, Money.parse("12500"))
        assertEquals(1_250_050L, Money.parse("12,500.50"))
        assertEquals(1_250_000L, Money.parse("$ 12 500"))
        assertEquals(1_250_050L, Money.parse("12500.5"))
        assertEquals(50L, Money.parse(".5"))
        assertEquals(1_250_000L, Money.parse("12500 MXN"))
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("12.345"))
        assertNull(Money.parse("1.2.3"))
    }

    @Test
    fun texto_para_editar() {
        assertEquals("12500", Money.plain(1_250_000))
        assertEquals("12500.05", Money.plain(1_250_005))
        assertEquals("", Money.plain(0))
        assertEquals(1_250_005L, Money.parse(Money.plain(1_250_005)))
    }
}
