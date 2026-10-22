package com.minimemo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/memo")
@CrossOrigin(origins = "*") // Allows the Chrome Extension to talk to localhost
public class MeetingController {

    private final ChatClient chatClient;

    public MeetingController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateMemo(@RequestBody Map<String, String> payload) {
        try {
            String transcript = payload.get("transcript");

            if (transcript == null || transcript.trim().length() < 10) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Transcript is too short or empty. Make sure CC is turned ON and people are speaking."
                ));
            }

            // Updated strict prompt to ignore UI noise
            String prompt = """
                You are MiniMemo, an intelligent real-time meeting assistant.
                Analyze the following live transcript collected so far and generate a structured summary.
                
                STRICT INSTRUCTIONS:
                1. Focus ONLY on actual human conversation, business topics, and tasks.
                2. Completely IGNORE all automated system text (e.g., "cameras are on", "microphone is on", "joined the meeting", "dial-in information").
                3. If the transcript ONLY contains system text and no actual human discussion, reply EXACTLY with: "Waiting for actual conversation to begin..."

                Format requirements:
                # Executive Overview
                A 2-3 sentence overview of what has been discussed so far.

                # Key Highlights & Decisions
                - Bullet points covering major points and decisions made.

                # Current Action Items
                - List action items with assignees and explicit or implied deadlines (Mark TBD if unknown).

                Transcript:
                """ + transcript;

            // Call local Ollama model
            String result = chatClient.prompt(prompt).call().content();

            return ResponseEntity.ok(Map.of("memo", result));

        } catch (Exception e) {
            // Catch connection errors if Ollama isn't running or times out
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to connect to local AI. Ensure 'ollama serve' is running. Details: " + e.getMessage()
            ));
        }
    }
}