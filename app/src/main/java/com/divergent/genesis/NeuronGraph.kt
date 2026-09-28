package com.divergent.genesis

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

enum class NodeType {
    CORE, MONO_MAIN, BI_MAIN, MONO_SUB, BI_SUB, MONO_MINI
}

data class NeuronNode(
    val id: String = UUID.randomUUID().toString(),
    val parentId: String? = null,
    var label: String,
    var url: String? = null,
    var type: NodeType,
    var x: Float = 0f,
    var y: Float = 0f,
    var angle: Float = 0f,
    var depth: Int = 0
)

object BrainGraphManager {
    val nodes = mutableStateListOf<NeuronNode>()

    fun clear() { nodes.clear() }

    fun addMainNode(label: String, url: String? = null): NeuronNode {
        val angle = (0..360).random().toFloat()
        val radius = 420f + (0..120).random()
        val node = NeuronNode(
            label = label,
            url = url,
            type = NodeType.MONO_MAIN,
            angle = angle,
            depth = 1,
            x = cos(Math.toRadians(angle.toDouble())).toFloat() * radius,
            y = sin(Math.toRadians(angle.toDouble())).toFloat() * radius
        )
        nodes.add(node)
        return node
    }

    fun addChild(parentId: String, label: String, url: String? = null): NeuronNode? {
        val parent = nodes.find { it.id == parentId } ?: return null
        val spread = 70f
        val childAngle = parent.angle + (-spread..spread).random()
        val childRadius = when (parent.type) {
            NodeType.MONO_MAIN -> 180f
            NodeType.BI_MAIN -> 150f
            NodeType.MONO_SUB -> 110f
            NodeType.BI_SUB -> 90f
            else -> 80f
        }
        val childType = when (parent.type) {
            NodeType.MONO_MAIN, NodeType.BI_MAIN -> NodeType.MONO_SUB
            NodeType.MONO_SUB, NodeType.BI_SUB -> NodeType.MONO_MINI
            else -> NodeType.MONO_MINI
        }
        val rad = Math.toRadians(childAngle.toDouble())
        val node = NeuronNode(
            parentId = parentId,
            label = label,
            url = url,
            type = childType,
            angle = childAngle,
            depth = parent.depth + 1,
            x = parent.x + cos(rad).toFloat() * childRadius,
            y = parent.y + sin(rad).toFloat() * childRadius
        )
        nodes.add(node)
        autoPromote(parentId)
        return node
    }

    /** Rule: If a node gains too many children, it grows up. */
    private fun autoPromote(parentId: String) {
        val parent = nodes.find { it.id == parentId } ?: return
        val childCount = nodes.count { it.parentId == parentId }

        when (parent.type) {
            NodeType.MONO_MINI -> {
                if (childCount > 4) {
                    val idx = nodes.indexOf(parent)
                    nodes[idx] = parent.copy(type = NodeType.BI_SUB)
                }
            }
            NodeType.BI_SUB -> {
                if (childCount > 5) {
                    val idx = nodes.indexOf(parent)
                    nodes[idx] = parent.copy(type = NodeType.BI_MAIN)
                }
            }
            NodeType.MONO_SUB -> {
                if (childCount > 5) {
                    val idx = nodes.indexOf(parent)
                    nodes[idx] = parent.copy(type = NodeType.BI_MAIN)
                }
            }
            else -> { /* Main nodes stay main */ }
        }
    }

    /** Recursive delete: kill all descendants first, then self. */
    fun deleteNode(nodeId: String) {
        val node = nodes.find { it.id == nodeId } ?: return
        val children = nodes.filter { it.parentId == nodeId }.toList()
        children.forEach { deleteNode(it.id) }
        nodes.remove(node)
    }

    fun findNodeByUrl(url: String): NeuronNode? =
        nodes.find { it.url != null && it.url.equals(url, ignoreCase = true) }

    fun findNodeByLabel(label: String): NeuronNode? =
        nodes.find { it.label.equals(label, ignoreCase = true) }

    fun getColor(type: NodeType): Color = when (type) {
        NodeType.CORE      -> Color(0xFF8B0000) // Dark Deep Red
        NodeType.MONO_MAIN -> Color(0xFF1E90FF) // Blue
        NodeType.BI_MAIN   -> Color(0xFF00FFFF) // Cyan
        NodeType.MONO_SUB  -> Color(0xFF32CD32) // Green
        NodeType.BI_SUB    -> Color(0xFFFFD700) // Yellow (Suggested)
        NodeType.MONO_MINI -> Color(0xFFFFB6C1) // Light Pink
    }

    fun getRadius(type: NodeType): Float = when (type) {
        NodeType.CORE      -> 45f
        NodeType.MONO_MAIN -> 30f
        NodeType.BI_MAIN   -> 24f
        NodeType.MONO_SUB  -> 18f
        NodeType.BI_SUB    -> 14f
        NodeType.MONO_MINI -> 9f
    }

    // Seed the initial brain
    init {
        val google = addMainNode("Google", "https://google.com")
        addChild(google.id, "Search", "https://google.com/search")
        addChild(google.id, "Images", "https://images.google.com")

        val yt = addMainNode("YouTube", "https://youtube.com")
        addChild(yt.id, "Search", "https://youtube.com/results")
        addChild(yt.id, "Subscribe")
        addChild(yt.id, "Upload Short")

        val ig = addMainNode("Instagram", "https://instagram.com")
        addChild(ig.id, "Reels")
        addChild(ig.id, "Post Reel")
    }
}
