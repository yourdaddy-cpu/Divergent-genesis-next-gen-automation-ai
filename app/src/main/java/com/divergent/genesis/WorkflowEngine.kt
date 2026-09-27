package com.divergent.genesis

import android.accessibilityservice.AccessibilityService
import kotlinx.coroutines.delay

object WorkflowEngine {
    suspend fun runWorkflow(workflowName: String, svc: AccessibilityService?) {
        if (svc == null) return
        
        when (workflowName) {
            "instagram_reel" -> {
                AgentState.currentTask.value = "Opening Instagram..."
                svc.openApp("com.instagram.android")
                delay(4000) // Wait for app to load

                AgentState.currentTask.value = "Tapping Create..."
                svc.findByText("Create")?.let { svc.tapNode(it) } ?: svc.findByText("+")?.let { svc.tapNode(it) }
                delay(2000)

                AgentState.currentTask.value = "Selecting Reel..."
                svc.findByText("Reel")?.let { svc.tapNode(it) }
                delay(2000)

                AgentState.currentTask.value = "Opening Gallery..."
                svc.findByText("Gallery")?.let { svc.tapNode(it) } ?: svc.findByText("Select")?.let { svc.tapNode(it) }
                delay(2000)

                // Note: This assumes the first video in the gallery is already selected. 
                // Tapping the first item is highly dependent on screen resolution, but this is a start.
                AgentState.currentTask.value = "Selecting recent video..."
                svc.tapAt(200f, 600f) // Taps roughly the first item in the grid
                delay(1500)

                AgentState.currentTask.value = "Proceeding..."
                svc.findByText("Next")?.let { svc.tapNode(it) }
                delay(2000)
                
                svc.findByText("Next")?.let { svc.tapNode(it) }
                delay(2000)

                AgentState.currentTask.value = "Typing caption..."
                svc.findByText("Write a caption")?.let { svc.tapNode(it) }
                delay(1000)
                svc.typeText("Posted by Divergent Genesis 🚀")
                delay(1000)

                AgentState.currentTask.value = "Sharing..."
                svc.findByText("Share")?.let { svc.tapNode(it) }
                delay(3000)
                
                AgentState.currentTask.value = null
            }

            "youtube_short" -> {
                AgentState.currentTask.value = "Opening YouTube..."
                svc.openApp("com.google.android.youtube")
                delay(4000)

                AgentState.currentTask.value = "Tapping Create..."
                svc.findByText("+")?.let { svc.tapNode(it) } ?: svc.findByText("Create")?.let { svc.tapNode(it) }
                delay(2000)

                AgentState.currentTask.value = "Selecting Short..."
                svc.findByText("Create a Short")?.let { svc.tapNode(it) }
                delay(2000)

                AgentState.currentTask.value = "Opening Gallery..."
                svc.findByText("Gallery")?.let { svc.tapNode(it) }
                delay(2000)

                AgentState.currentTask.value = "Selecting recent video..."
                svc.tapAt(200f, 600f) // Taps roughly the first item
                delay(1500)

                AgentState.currentTask.value = "Proceeding..."
                svc.findByText("Next")?.let { svc.tapNode(it) }
                delay(2000)
                
                svc.findByText("Next")?.let { svc.tapNode(it) }
                delay(2000)

                AgentState.currentTask.value = "Adding details..."
                svc.findByText("Add a title")?.let { svc.tapNode(it) }
                delay(1000)
                svc.typeText("Divergent Genesis Short")
                delay(1000)

                AgentState.currentTask.value = "Uploading..."
                svc.findByText("Upload Short")?.let { svc.tapNode(it) }
                delay(3000)

                AgentState.currentTask.value = null
            }
            else -> {
                AgentState.currentTask.value = "Unknown workflow."
                delay(2000)
                AgentState.currentTask.value = null
            }
        }
    }
}
