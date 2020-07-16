package com.ryuta46.evalspecmaker

import com.ryuta46.evalspecmaker.util.Logger
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class XlsxCreator {
    val logger = Logger(this.javaClass.simpleName)

    companion object {
        // 列幅最大値
        private val MAX_WIDTH = 10000
        // 推定折り返し文字数
        private val WRAP_CHAR_LENGTH = 20
        // ヘッダ行のインデックス
        private val HEADER_INDEX = 1
    }

    private val book: Workbook

    // デフォルトのスタイル
    private val cellStyle: CellStyle
    // 上の枠のみ
    private val cellStyleTopBorder: CellStyle
    // 下の枠なし
    private val cellStyleNoBottomBorder: CellStyle
    // 上下の枠なし
    private val cellStyleNoVerticalBorder: CellStyle
    // 中央寄せ
    private val cellStyleCenter: CellStyle
    // ヘッダのスタイル
    private val cellStyleHeader: CellStyle

    private enum class Column(val title: String) {
        NUMBER("#"),
        MAJOR("大項目"),
        MIDDLE("中項目"),
        MINOR("小項目"),
        CLIENT("試験クライアント"),
        LANG("ブラウザ言語設定"),
        METHOD("確認手順"),
        CONFIRM("確認項目"),
        COMMENT("補足"),
        TESTER("試験者"),
        DATE("試験日"),
        RESULT("結果"),
        RESULTDETAIL("結果詳細");

        val index: Int
            get() = ordinal + 1
    }



    init {
        book = XSSFWorkbook()
        cellStyle = book.createCellStyle().apply {
            alignment = CellStyle.ALIGN_LEFT
            verticalAlignment = CellStyle.VERTICAL_TOP
            wrapText = true
            borderTop = 1.toShort()
            borderLeft = 1.toShort()
            borderRight = 1.toShort()
            borderBottom = 1.toShort()
        }

        cellStyleTopBorder = book.createCellStyle().apply {
            cloneStyleFrom(cellStyle)
            borderTop = 1.toShort()
            borderLeft = 0.toShort()
            borderRight = 0.toShort()
            borderBottom = 0.toShort()
        }

        cellStyleNoBottomBorder = book.createCellStyle().apply {
            cloneStyleFrom(cellStyle)
            borderBottom = 0.toShort()
        }

        cellStyleNoVerticalBorder = book.createCellStyle().apply {
            cloneStyleFrom(cellStyleNoBottomBorder)
            borderTop = 0.toShort()
        }

        cellStyleCenter = book.createCellStyle().apply {
            cloneStyleFrom(cellStyle)
            alignment = CellStyle.ALIGN_CENTER
            verticalAlignment = CellStyle.VERTICAL_CENTER
            // 何故か以下行を呼び出さないと中央揃えにならない
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.getIndex()
        }


        cellStyleHeader = book.createCellStyle().apply {
            cloneStyleFrom(cellStyle)
            alignment = CellStyle.ALIGN_CENTER
            verticalAlignment = CellStyle.VERTICAL_CENTER
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.getIndex()
            fillPattern = CellStyle.SOLID_FOREGROUND
        }
    }


    @Throws(java.io.IOException::class)
    internal fun createXlsx(file: String, rootItem: TestItem) {
        logger.trace {
            rootItem.children.forEach { createCategory(it) }

            java.io.FileOutputStream(file).use {
                book.write(it)
            }
        }
    }

    private fun selectAssignee(major: TestItem, middle: TestItem, minor: TestItem, client: String): String? {
        if (minor.assignee.containsKey(client)) {
            return minor.assignee[client]
        }
        if (minor.assignee.containsKey("any")) {
            return minor.assignee["any"]
        }
        if (middle.assignee.containsKey(client)) {
            return middle.assignee[client]
        }
        if (middle.assignee.containsKey("any")) {
            return middle.assignee["any"]
        }
        if (major.assignee.containsKey(client)) {
            return major.assignee[client]
        }
        if (major.assignee.containsKey("any")) {
            return major.assignee["any"]
        }
        return null
    }

    private fun setRow(sheet: Sheet, rowIndex: Int, major: TestItem, majorIndex: Int, middle: TestItem, middleIndex: Int, minor: TestItem, minorIndex: Int, client: String, lang: String, matrixIndex: Int) {
        val majorBody = major.bodies
        var majorLineCount = estimateLineCount(majorBody)
        setCellValue(sheet, Column.MAJOR.index, rowIndex, majorBody).cellStyle = cellStyleNoBottomBorder

        val middleBody = middle.bodies
        var middleLineCount = estimateLineCount(middleBody)
        setCellValue(sheet, Column.MIDDLE.index, rowIndex, middleBody).cellStyle = cellStyleNoBottomBorder

        // 中項目、小項目のみが進んだ場合は大項目の縦線なし
        if (middleIndex > 0 || minorIndex > 0 || matrixIndex > 0) {
            setCellValue(sheet, Column.MAJOR.index, rowIndex, "").cellStyle = cellStyleNoVerticalBorder
            majorLineCount = 1
        }
        // 小項目のみが進んだ場合は中項目の縦線なし
        if (minorIndex > 0 || matrixIndex > 0) {
            setCellValue(sheet, Column.MIDDLE.index, rowIndex, "").cellStyle = cellStyleNoVerticalBorder
            middleLineCount = 1
        }

        // 項目番号
        setCellValue(sheet, Column.NUMBER.index, rowIndex, "%d-%d-%d-%d".format(majorIndex+1, middleIndex+1, minorIndex+1, matrixIndex+1))

        val minorBody = minor.bodies
        val method = minor.methods
        val confirm = minor.confirms
        val comment = minor.comments

        setCellValue(sheet, Column.MINOR.index, rowIndex, minorBody)

        // クライアント
        setCellValue(sheet, Column.CLIENT.index, rowIndex, client)
        // 言語
        setCellValue(sheet, Column.LANG.index, rowIndex, lang)

        // 手順
        setCellValue(sheet, Column.METHOD.index, rowIndex, method)
        // 確認点
        setCellValue(sheet, Column.CONFIRM.index, rowIndex, confirm)
        // 補足
        setCellValue(sheet, Column.COMMENT.index, rowIndex, comment)

        // 試験者
        val assignee = selectAssignee(major, middle, minor, client) ?: ""
        setCellValue(sheet, Column.TESTER.index, rowIndex, assignee).cellStyle = cellStyleCenter

        // 各列の行数の推測値から最大のものを列の高さに設定
        var maxRowHeightUnit = Math.max(majorLineCount, middleLineCount)
        maxRowHeightUnit = Math.max(maxRowHeightUnit, estimateLineCount(minorBody))
        maxRowHeightUnit = Math.max(maxRowHeightUnit, estimateLineCount(method))
        maxRowHeightUnit = Math.max(maxRowHeightUnit, estimateLineCount(confirm))

        // 高さを反映
        val row = sheet.getRow(rowIndex)
        row.height = (row.height * maxRowHeightUnit).toShort()

        // スタイルのみ設定する項目を設定
        setCellValue(sheet, Column.DATE.index, rowIndex, "")
        setCellValue(sheet, Column.RESULT.index, rowIndex, "").cellStyle = cellStyleCenter
        setCellValue(sheet, Column.RESULTDETAIL.index, rowIndex, "")
    }

    private fun createCategory(categoryItem: TestItem) {
        val sheet = book.createSheet(categoryItem.bodies)

        // グリッド表示を消す.
        sheet.isDisplayGridlines = false

        setColumnHeader(sheet)

        var rowIndex = HEADER_INDEX + 1

        categoryItem.children.forEachIndexed { majorIndex, major ->

            major.children.forEachIndexed { middleIndex, middle ->

                middle.children.forEachIndexed { minorIndex, minor ->

                    if (minor.containsComment("・テスト対象外")) {
                        setRow(sheet, rowIndex, major, majorIndex, middle, middleIndex, minor, minorIndex, "", "", 0)
                        rowIndex++
                    } else {
                        var clients = listOf("Chrome", "iPad")
                        if (minor.containsComment("・no-chrome")) {
                            minor.removeComment("・no-chrome")
                            clients = listOf("iPad")
                        } else if (minor.containsComment("・no-ipad")) {
                            minor.removeComment("・no-ipad")
                            clients = listOf("Chrome")
                        }
                        var langs = listOf("日本語", "英語")
                        if (minor.containsComment("・no-lang")) {
                            minor.removeComment("・no-lang")
                            langs = listOf("")
                        }
                        val matrix = product(clients, langs)

                        matrix.forEachIndexed { matrixIndex, clientLang ->
                            val client = clientLang.first
                            val lang = clientLang.second
                            setRow(sheet, rowIndex, major, majorIndex, middle, middleIndex, minor, minorIndex, client, lang, matrixIndex)
                            rowIndex++
                        }
                    }

                }
            }
        }

        // 表の下端の横線を設定
        for (i in Column.NUMBER.index..Column.RESULTDETAIL.index) {
            getCell(sheet, i, rowIndex).cellStyle = cellStyleTopBorder
        }

        // Resize all column
        Column.values().map { it.index }.forEach { index ->
            sheet.autoSizeColumn(index)
            if (Companion.MAX_WIDTH < sheet.getColumnWidth(index)) {
                sheet.setColumnWidth(index, Companion.MAX_WIDTH)
            }
        }
    }

    private fun getCell(sheet: Sheet, columnIndex: Int, rowIndex: Int): Cell {
        val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
        val cell = row.getCell(columnIndex) ?: row.createCell(columnIndex)
        return cell
    }

    private fun setCellValue(sheet: Sheet, columnIndex: Int, rowIndex: Int, text: String): Cell {
        val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
        val cell = row.getCell(columnIndex) ?: row.createCell(columnIndex)

        cell.setCellValue(text)
        cell.cellStyle = cellStyle

        return cell
    }

    private fun estimateLineCount(text: String): Int {
        val lines = text.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

        val lineCount = lines
                .map { it.codePointCount(0, it.length) }
                .sumBy { (it + Companion.WRAP_CHAR_LENGTH - 1) / Companion.WRAP_CHAR_LENGTH }

        return lineCount

    }

    private fun setColumnHeader(sheet: Sheet) {
        Column.values().forEach { column ->
            setCellValue(sheet, column.index, Companion.HEADER_INDEX, column.title).cellStyle = cellStyleHeader
        }
    }

    private fun product(a: List<String>, b: List<String>): List<Pair<String, String>> {
        val ret = mutableListOf<Pair<String, String>>()
        for (aElem in a) {
            for (bElem in b) {
                ret.add(Pair(aElem, bElem))
            }
        }
        return ret
    }

}
