package com.ryuta46.evalspecmaker

internal class TestItem {
    internal enum class TextContainer {
        // 項目本文
        BODY,
        // 試験手順
        METHOD,
        // 確認点
        CONFIRM,
        // 補足
        COMMENT,
    }

    private val _children = mutableListOf<TestItem>()
    val children: List<TestItem>
        get() = _children

    private val bodyList = mutableListOf<String>()
    private val methodList = mutableListOf<String>()
    private val confirmList = mutableListOf<String>()
    private val commentList = mutableListOf<String>()

    private var mAddTarget = TextContainer.BODY

    var level = 0


    val bodies: String
        get() = textListToString(bodyList)
    val methods: String
        get() = textListToString(methodList)
    val confirms: String
        get() = textListToString(confirmList)
    val comments: String
        get() = textListToString(commentList)

    var assignee = mutableMapOf<String, String>()

    fun setAssignee(text: String) {
        val sp = text.split(":")
        if (sp.size == 1) {
            assignee["any"] = text
        }
        if (sp.size >= 2) {
            assignee[sp[0]] = sp[1]
        }
    }

    private fun textListToString(list: List<String>): String {
        if (list.isEmpty()) {
            return ""
        }
        val builder = StringBuilder(list[0])
        for (i in 1..list.size - 1) {
            builder.append("\n")
            builder.append(list[i])
        }
        return builder.toString()
    }

    fun addChild(child: TestItem) {
        _children.add(child)
    }

    fun setAddTarget(target: TextContainer) {
        mAddTarget = target
    }

    fun addText(text: String) {
        when (mAddTarget) {
            TextContainer.BODY -> bodyList.add(text)
            TextContainer.METHOD -> methodList.add((methodList.size + 1).toString() + ". " + text)
            TextContainer.CONFIRM -> confirmList.add("・" + text)
            TextContainer.COMMENT -> commentList.add("・" + text)
        }
    }

    fun inheritComment(parent: TestItem) {
        for (comment in parent.getCommentList()) {
            commentList.add(comment)
        }
    }

    fun getCommentList(): List<String> {
        return commentList
    }

    fun containsComment(text: String): Boolean {
        for (comment in commentList) {
            if (comment.equals(text)) {
                return true
            }
        }
        return false
    }

    fun removeComment(text: String) {
        commentList.removeAll { it.equals(text) }
    }


    fun printInformation(level: Int) {
        val builder = StringBuilder()
        for (i in 0..level - 1) {
            builder.append("-")
        }
        val prefix = builder.toString()


        println(String.format("%sL:%d", prefix, level))
        for (text in bodyList) {
            println(String.format("%sT:%s", prefix, text))
        }
        for (text in methodList) {
            println(String.format("%sM:%s", prefix, text))
        }
        for (text in confirmList) {
            println(String.format("%sC:%s", prefix, text))
        }

        for (childNode in _children) {
            childNode.printInformation(level + 1)
        }
    }


}
