package com.dbstudio

import com.dbstudio.ui.components.ManualCategory
import com.dbstudio.ui.components.RibbonTab
import com.dbstudio.ui.components.manualTopics
import com.dbstudio.viewmodel.AppViewModel
import com.dbstudio.viewmodel.Screen
import com.dbstudio.viewmodel.TableDesignerViewModel
import kotlin.test.*

class RibbonNavigationTest {

    @Test
    fun testRibbonTabsAreDefined() {
        val tabNames = RibbonTab.entries.map { it.name }
        assertTrue(tabNames.contains("ARCHIVO"))
        assertTrue(tabNames.contains("INICIO"))
        assertTrue(tabNames.contains("INSERTAR"))
        assertTrue(tabNames.contains("DATOS"))
        assertTrue(tabNames.contains("VISTA"))

        assertEquals("Archivo", RibbonTab.ARCHIVO.displayName)
        assertEquals("Inicio", RibbonTab.INICIO.displayName)
        assertEquals("Insertar", RibbonTab.INSERTAR.displayName)
        assertEquals("Datos", RibbonTab.DATOS.displayName)
        assertEquals("Vista", RibbonTab.VISTA.displayName)
    }

    @Test
    fun testAppViewModelNavigationCoversAllRibbonScreens() {
        val vm = AppViewModel()

        // Default screen is OVERVIEW
        assertEquals(Screen.OVERVIEW, vm.currentScreen)

        // Navigate to each Ribbon screen
        val screens = listOf(
            Screen.OVERVIEW,
            Screen.TABLE_DESIGNER,
            Screen.DATA_VIEW,
            Screen.SQL_QUERY,
            Screen.DIAGRAM,
            Screen.JOIN_TABLES,
            Screen.ETL_QUERY
        )

        for (screen in screens) {
            vm.navigateTo(screen)
            assertEquals(screen, vm.currentScreen, "Should navigate correctly to $screen")
        }
    }

    @Test
    fun testTableSelectionAndSelectionClearing() {
        val vm = AppViewModel()
        assertNull(vm.selectedTable)

        vm.selectTable("clientes")
        assertEquals("clientes", vm.selectedTable)

        vm.selectTable("pedidos")
        assertEquals("pedidos", vm.selectedTable)

        vm.selectTable(null)
        assertNull(vm.selectedTable)
    }

    @Test
    fun testTableDesignerShowAddColumnDialogState() {
        val vm = TableDesignerViewModel()
        assertFalse(vm.showAddColumnDialog)

        vm.showAddColumnDialog = true
        assertTrue(vm.showAddColumnDialog)

        vm.showAddColumnDialog = false
        assertFalse(vm.showAddColumnDialog)
    }

    @Test
    fun testUserManualIncludesRibbonAndCustomFieldTopics() {
        assertTrue(ManualCategory.entries.any { it.name == "RIBBON" })
        assertTrue(manualTopics.any { it.title.contains("Cinta de Opciones") })
        assertTrue(manualTopics.any { it.title.contains("Campo Personalizado") })
        assertTrue(manualTopics.any { it.title.contains("Cerebro") })
        assertTrue(manualTopics.any { it.title.contains("Excel") })
    }
}
